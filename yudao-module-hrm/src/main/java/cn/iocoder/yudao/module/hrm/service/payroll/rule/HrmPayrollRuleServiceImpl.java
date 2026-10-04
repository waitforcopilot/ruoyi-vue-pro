package cn.iocoder.yudao.module.hrm.service.payroll.rule;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.rule.*;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.rule.HrmPayrollRuleSaveReqVO.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.HrmPayrollReviewDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.rule.HrmPayrollRuleDO;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.HrmPayrollReviewMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.rule.HrmPayrollRuleMapper;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import javax.annotation.Resource;
import java.io.InputStream;
import java.math.*;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;

/** Reviewed rule evidence only; nothing here changes the existing payroll/tax calculation configuration. */
@Service
@Validated
public class HrmPayrollRuleServiceImpl implements HrmPayrollRuleService {
    @Resource private HrmPayrollRuleMapper ruleMapper;
    @Resource private HrmPayrollReviewMapper reviewMapper;
    @Resource private AdminUserApi adminUserApi;
    private Long tenant() { return TenantContextHolder.getRequiredTenantId(); }

    @Override
    @Transactional(rollbackFor=Exception.class)
    public Integer initialize() {
        JsonNode catalog;
        try (InputStream input=new ClassPathResource("payroll/rule-catalog.json").getInputStream()) {
            catalog=JsonUtils.getObjectMapper().readTree(input);
        } catch (java.io.IOException e) { throw new IllegalStateException("Cannot read the business question catalog",e); }
        int count=0;
        for(JsonNode item:catalog) {
            if(ruleMapper.selectRoot(item.get("code").asText(),tenant(),false)!=null) continue;
            HrmPayrollRuleDO row=JsonUtils.parseObject(item.toString(),HrmPayrollRuleDO.class)
                    .setRuleVersion(1).setRevision(1).setStatus(0).setBuiltIn(true)
                    .setParametersJson("[]").setCasesJson("[]").setParameterCount(0).setCaseCount(0);
            row.setTenantId(tenant());
            try { ruleMapper.insert(row); audit("initialize",null,row,"登记 PRD 待决口径，不预置实际规则"); count++; }
            catch(DuplicateKeyException ignored) { /* Concurrent initialization keeps the first draft. */ }
        }
        return count;
    }

    @Override
    @Transactional(rollbackFor=Exception.class)
    public Long create(HrmPayrollRuleSaveReqVO request) {
        check(request.getCode().startsWith("RULE-CUSTOM-") && request.getCode().length()>12,"补充规则编号须以 RULE-CUSTOM- 开头并填写后缀");
        if(ruleMapper.selectRoot(request.getCode(),tenant(),false)!=null) throw exception(PAYROLL_RULE_CODE_DUPLICATE);
        validate(request,false);
        HrmPayrollRuleDO row=editable(request).setId(null).setRuleVersion(1).setRevision(1).setStatus(0).setBuiltIn(false);
        try { ruleMapper.insert(row); } catch(DuplicateKeyException e) { throw exception(PAYROLL_RULE_CODE_DUPLICATE); }
        audit("create",null,row,"补充规则草稿");
        return row.getId();
    }

    @Override
    @Transactional(rollbackFor=Exception.class,isolation=Isolation.READ_COMMITTED)
    public Long newVersion(Long id,Integer expected) {
        HrmPayrollRuleDO selected=require(id,false);
        if(ruleMapper.selectRoot(selected.getCode(),tenant(),true)==null) throw exception(PAYROLL_RULE_NOT_EXISTS);
        selected=require(id,true); revision(selected,expected);
        HrmPayrollRuleDO latest=ruleMapper.selectOne(new LambdaQueryWrapperX<HrmPayrollRuleDO>()
                .eq(HrmPayrollRuleDO::getTenantId,tenant()).eq(HrmPayrollRuleDO::getCode,selected.getCode())
                .orderByDesc(HrmPayrollRuleDO::getRuleVersion).last("LIMIT 1"));
        HrmPayrollRuleDO row=editable(response(selected)).setId(null).setRuleVersion(latest.getRuleVersion()+1)
                .setRevision(1).setStatus(0).setBuiltIn(selected.getBuiltIn());
        ruleMapper.insert(row);
        audit("new-version",null,row,"从版本 V"+selected.getRuleVersion()+" 另建草稿，原结论不继承");
        return row.getId();
    }

    @Override
    @Transactional(rollbackFor=Exception.class)
    public void update(HrmPayrollRuleSaveReqVO request) {
        HrmPayrollRuleDO before=require(request.getId(),true); revision(before,request.getRevision());
        if(before.getStatus()!=0) throw exception(PAYROLL_RULE_IMMUTABLE);
        check(before.getCode().equals(request.getCode()),"稳定规则编号不能修改");
        check(before.getCategory().equals(request.getCategory()),"同编号的规则分类不能修改");
        validate(request,false);
        HrmPayrollRuleDO after=editable(request).setRuleVersion(before.getRuleVersion()).setRevision(before.getRevision()+1)
                .setStatus(0).setBuiltIn(before.getBuiltIn());
        ruleMapper.updateById(after); audit("update",before,after,"修订规则草稿，需重新评审");
    }

    @Override
    @Transactional(rollbackFor=Exception.class,isolation=Isolation.READ_COMMITTED)
    public void review(HrmPayrollRuleReviewReqVO request) {
        HrmPayrollRuleDO selected=require(request.getId(),false);
        // All confirmations for one code serialize on the first version, including different draft IDs.
        if(ruleMapper.selectRoot(selected.getCode(),tenant(),true)==null) throw exception(PAYROLL_RULE_NOT_EXISTS);
        HrmPayrollRuleDO before=require(request.getId(),true); revision(before,request.getRevision());
        boolean confirming="confirm".equals(request.getAction());
        HrmPayrollRuleDO after=BeanUtils.toBean(before,HrmPayrollRuleDO.class);
        if(confirming) {
            check(before.getStatus()==0,"只有草稿可确认");
            HrmPayrollRuleSaveReqVO normalized=response(before); validate(normalized,true);
            LambdaQueryWrapperX<HrmPayrollRuleDO> query=new LambdaQueryWrapperX<HrmPayrollRuleDO>()
                    .eq(HrmPayrollRuleDO::getTenantId,tenant()).eq(HrmPayrollRuleDO::getCode,before.getCode())
                    .eq(HrmPayrollRuleDO::getScopeCode,normalized.getScopeCode()).eq(HrmPayrollRuleDO::getStatus,1);
            if(normalized.getEffectiveTo()!=null) query.le(HrmPayrollRuleDO::getEffectiveFrom,normalized.getEffectiveTo());
            query.and(w->w.isNull(HrmPayrollRuleDO::getEffectiveTo).or().ge(HrmPayrollRuleDO::getEffectiveTo,normalized.getEffectiveFrom()));
            if(ruleMapper.selectCount(query)>0) throw exception(PAYROLL_RULE_PERIOD_OVERLAP);
            after.setParametersJson(JsonUtils.toJsonString(normalized.getParameters())).setCasesJson(JsonUtils.toJsonString(normalized.getCases()));
        } else check(before.getStatus()==1,"只有已确认规则可停用");
        after.setStatus(confirming?1:2).setRevision(before.getRevision()+1).setEvidence(request.getEvidence())
                .setReviewedBy(getLoginUserId()).setReviewedByName(actorName()).setReviewedTime(LocalDateTime.now());
        ruleMapper.updateById(after); audit(request.getAction(),before,after,request.getEvidence());
    }

    private HrmPayrollRuleDO editable(HrmPayrollRuleSaveReqVO request) {
        HrmPayrollRuleDO row=BeanUtils.toBean(request,HrmPayrollRuleDO.class)
                .setParametersJson(JsonUtils.toJsonString(request.getParameters())).setCasesJson(JsonUtils.toJsonString(request.getCases()))
                .setParameterCount(request.getParameters().size()).setCaseCount(request.getCases().size())
                .setReviewedBy(null).setReviewedByName(null).setReviewedTime(null).setEvidence(null);
        row.setTenantId(tenant()); return row;
    }

    private void validate(HrmPayrollRuleSaveReqVO request,boolean confirming) {
        if(request.getEffectiveFrom()!=null) check(request.getEffectiveFrom().getYear()>=1000 && request.getEffectiveFrom().getYear()<=9999,"生效日期超过数据库支持范围");
        if(request.getEffectiveTo()!=null) check(request.getEffectiveTo().getYear()>=1000 && request.getEffectiveTo().getYear()<=9999,"截止日期超过数据库支持范围");
        check(request.getEffectiveTo()==null || (request.getEffectiveFrom()!=null && !request.getEffectiveFrom().isAfter(request.getEffectiveTo())),"截止日期不能早于生效日期，且须先填写生效日期");
        Set<String> keys=new HashSet<>();
        for(Parameter parameter:request.getParameters()) {
            check(parameter!=null,"参数不能为 null");
            check(keys.add(parameter.getKey()),"参数标识不能重复");
            if(confirming) normalize(parameter);
        }
        for(BusinessCase sample:request.getCases()) {
            check(sample!=null,"业务样例不能为 null");
            JsonNode input;
            try { input=JsonUtils.getObjectMapper().readTree(sample.getInputJson()); }
            catch(Exception e) { throw exception(PAYROLL_RULE_INVALID,"业务样例输入必须是合法 JSON 对象"); }
            check(input!=null && input.isObject(),"业务样例输入必须是合法 JSON 对象");
            depth(input,0);
        }
        if(confirming) {
            check(StrUtil.isNotBlank(request.getOwnerName()),"确认必须填写规则负责人");
            check(StrUtil.isNotBlank(request.getScopeCode()),"确认必须填写稳定范围编号");
            check(StrUtil.isNotBlank(request.getApplicableScope()),"确认必须填写适用范围");
            check(request.getEffectiveFrom()!=null,"确认必须填写生效日期");
            check(StrUtil.isNotBlank(request.getDefinition()),"确认必须填写完整规则口径");
            check(StrUtil.isNotBlank(request.getReference()),"确认必须填写制度或政策出处");
            check(!request.getCases().isEmpty(),"确认必须提供业务输入及预期结果样例");
        }
    }

    private void normalize(Parameter p) {
        check(StrUtil.isNotBlank(p.getValue()),"确认时所有参数必须明确值，零值应填 0");
        String value=p.getValue().trim();
        if("DECIMAL".equals(p.getType()) || "INTEGER".equals(p.getType())) check(StrUtil.isNotBlank(p.getUnit()),"数值参数必须明确单位");
        if("DECIMAL".equals(p.getType())) check(p.getScale()!=null,"小数参数必须明确小数位");
        try {
            switch(p.getType()) {
                case "INTEGER":
                    if(!value.matches("[+-]?[0-9]+")) throw new IllegalArgumentException();
                    BigInteger integer=new BigInteger(value);
                    if(integer.abs().toString().length()>18) throw new IllegalArgumentException();
                    value=integer.toString(); break;
                case "DECIMAL":
                    if(!value.matches("[+-]?[0-9]+(\\.[0-9]+)?")) throw new IllegalArgumentException();
                    BigDecimal decimal=new BigDecimal(value);
                    if(decimal.precision()>18 || decimal.scale()>p.getScale()) throw new IllegalArgumentException();
                    value=decimal.stripTrailingZeros().toPlainString(); break;
                case "BOOLEAN": if(!"true".equals(value) && !"false".equals(value)) throw new IllegalArgumentException(); break;
                case "DATE": if(!value.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) throw new IllegalArgumentException(); value=LocalDate.parse(value).toString(); break;
                case "TEXT": break;
                default: throw new IllegalArgumentException();
            }
        } catch(IllegalArgumentException | DateTimeException e) { throw exception(PAYROLL_RULE_INVALID,"参数值不符合类型或精度约束"); }
        p.setValue(value);
    }

    private void depth(JsonNode node,int level) {
        check(level<=16,"业务样例 JSON 嵌套超过 16 层");
        if(node.isContainerNode()) for(JsonNode child:node) depth(child,level+1);
    }

    private HrmPayrollRuleDO require(Long id,boolean lock) {
        HrmPayrollRuleDO row=id==null?null:ruleMapper.selectTenantById(id,tenant(),lock);
        if(row==null) throw exception(PAYROLL_RULE_NOT_EXISTS); return row;
    }
    private HrmPayrollRuleRespVO response(HrmPayrollRuleDO row) {
        HrmPayrollRuleRespVO value=BeanUtils.toBean(row,HrmPayrollRuleRespVO.class);
        if(row.getParametersJson()!=null) value.setParameters(JsonUtils.parseArray(row.getParametersJson(),Parameter.class));
        if(row.getCasesJson()!=null) value.setCases(JsonUtils.parseArray(row.getCasesJson(),BusinessCase.class));
        return value;
    }
    @Override public HrmPayrollRuleRespVO get(Long id) { return response(require(id,false)); }
    @Override public PageResult<HrmPayrollRuleRespVO> page(HrmPayrollRulePageReqVO query) {
        LambdaQueryWrapperX<HrmPayrollRuleDO> wrapper=new LambdaQueryWrapperX<HrmPayrollRuleDO>()
                .eq(HrmPayrollRuleDO::getTenantId,tenant()).eqIfPresent(HrmPayrollRuleDO::getCategory,query.getCategory())
                .eqIfPresent(HrmPayrollRuleDO::getStatus,query.getStatus()).eqIfPresent(HrmPayrollRuleDO::getScopeCode,query.getScopeCode());
        if(StrUtil.isNotBlank(query.getSearch())) wrapper.and(w->w.like(HrmPayrollRuleDO::getCode,query.getSearch()).or().like(HrmPayrollRuleDO::getTitle,query.getSearch()));
        wrapper.select(HrmPayrollRuleDO.class,field->!Arrays.asList("parametersJson","casesJson","definition").contains(field.getProperty()));
        PageResult<HrmPayrollRuleDO> page=ruleMapper.selectPage(query,wrapper.orderByDesc(HrmPayrollRuleDO::getId));
        return new PageResult<>(page.getList().stream().map(this::response).collect(Collectors.toList()),page.getTotal());
    }
    @Override public List<HrmPayrollReviewDO> history(Long id) {
        require(id,false);
        return reviewMapper.selectList(new LambdaQueryWrapperX<HrmPayrollReviewDO>().eq(HrmPayrollReviewDO::getTenantId,tenant())
                .eq(HrmPayrollReviewDO::getObjectType,"rule").eq(HrmPayrollReviewDO::getObjectId,id).orderByDesc(HrmPayrollReviewDO::getId));
    }
    private void revision(HrmPayrollRuleDO row,Integer expected) { if(!Objects.equals(row.getRevision(),expected)) throw exception(PAYROLL_RULE_STALE); }
    private void check(boolean valid,String message) { if(!valid) throw exception(PAYROLL_RULE_INVALID,message); }
    private String actorName() { AdminUserRespDTO user=adminUserApi.getUser(getLoginUserId()); return user==null?String.valueOf(getLoginUserId()):user.getNickname(); }
    private void audit(String action,HrmPayrollRuleDO before,HrmPayrollRuleDO after,String reason) {
        HrmPayrollReviewDO row=new HrmPayrollReviewDO().setObjectType("rule").setObjectId(after.getId()).setAction(action)
                .setFromVersion(before==null?null:before.getRevision()).setToVersion(after.getRevision()).setActorId(getLoginUserId())
                .setActorName(actorName()).setReason(reason).setBeforeSnapshot(before==null?null:JsonUtils.toJsonString(response(before)))
                .setAfterSnapshot(JsonUtils.toJsonString(response(after)));
        row.setTenantId(tenant()); reviewMapper.insert(row);
    }
}
