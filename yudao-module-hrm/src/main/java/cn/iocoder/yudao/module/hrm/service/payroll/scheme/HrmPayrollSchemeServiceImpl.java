package cn.iocoder.yudao.module.hrm.service.payroll.scheme;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.scheme.*;
import cn.iocoder.yudao.module.hrm.controller.admin.salary.vo.taxrule.HrmSalaryTaxRuleSaveReqVO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.HrmPayrollReviewDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.scheme.HrmPayrollSchemeDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.salary.config.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.HrmPayrollReviewMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.scheme.HrmPayrollSchemeMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.salary.config.*;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import javax.annotation.Resource;
import javax.validation.Validator;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;

@Service
public class HrmPayrollSchemeServiceImpl implements HrmPayrollSchemeService {
    @Resource private HrmPayrollSchemeMapper mapper;
    @Resource private HrmSalaryGroupMapper groupMapper;
    @Resource private HrmSalaryTaxRuleMapper taxMapper;
    @Resource private HrmSalaryOptionMapper optionMapper;
    @Resource private HrmPayrollReviewMapper reviewMapper;
    @Resource private PermissionApi permissionApi;
    @Resource private AdminUserApi adminUserApi;
    @Resource private Validator validator;
    private Long tenant() { return TenantContextHolder.getRequiredTenantId(); }
    private void guard() {
        for(String permission:Arrays.asList("hrm:salary:group:query","hrm:salary:option:query","hrm:salary:tax-rule:query"))
            if(!permissionApi.hasAnyPermissions(getLoginUserId(),permission))throw exception(PAYROLL_SCHEME_PERMISSION);
    }
    private LambdaQueryWrapperX<HrmPayrollSchemeDO> query() { return new LambdaQueryWrapperX<HrmPayrollSchemeDO>().eq(HrmPayrollSchemeDO::getTenantId,tenant()); }
    private void check(boolean valid,String reason) { if(!valid)throw exception(PAYROLL_SCHEME_INVALID,reason); }
    private void date(LocalDate value) { check(value==null||(value.getYear()>=1000&&value.getYear()<=9999),"日期超过数据库支持范围"); }
    private void revision(HrmPayrollSchemeDO row,Integer revision) { if(!Objects.equals(row.getRevision(),revision))throw exception(PAYROLL_SCHEME_STALE); }
    private HrmPayrollSchemeDO require(Long id,boolean lock) {
        LambdaQueryWrapperX<HrmPayrollSchemeDO> q=query().eq(HrmPayrollSchemeDO::getId,id);if(lock)q.last("FOR UPDATE");
        HrmPayrollSchemeDO row=id==null?null:mapper.selectOne(q);if(row==null)throw exception(PAYROLL_SCHEME_NOT_EXISTS);return row;
    }
    private String decimal(BigDecimal value) { return value==null?null:value.toPlainString(); }
    private String hash(HrmPayrollSchemeSnapshotVO snapshot) {
        try { byte[] bytes=MessageDigest.getInstance("SHA-256").digest(JsonUtils.toJsonString(snapshot).getBytes(StandardCharsets.UTF_8));
            StringBuilder result=new StringBuilder();for(byte b:bytes)result.append(String.format("%02x",b&255));return result.toString();
        }catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}
    }
    @Override public List<HrmPayrollSchemeSnapshotVO.Group> groups(String search) {
        guard();check(search==null||search.length()<=64,"薪资组检索最多 64 字符");
        QueryWrapper<HrmSalaryGroupDO> q=new QueryWrapper<HrmSalaryGroupDO>().eq("tenant_id",tenant()).select("id","name").orderByDesc("id");
        if(StrUtil.isNotBlank(search))q.like("name",search.trim());
        List<HrmSalaryGroupDO> rows=groupMapper.selectList(q.last("LIMIT 201"));check(rows.size()<=200,"薪资组超过 200 个，请输入名称缩小检索");
        return rows.stream().map(g->new HrmPayrollSchemeSnapshotVO.Group().setId(g.getId()).setName(g.getName())).collect(Collectors.toList());
    }
    private HrmPayrollSchemeSnapshotVO snapshot(Long groupId,boolean lock) {
        QueryWrapper<HrmSalaryGroupDO> gq=new QueryWrapper<HrmSalaryGroupDO>().eq("tenant_id",tenant()).eq("id",groupId)
                .select("id","name","salary_standard","change_rule","tax_rule_id");
        if(lock)gq.last("FOR UPDATE");
        HrmSalaryGroupDO group=groupId==null?null:groupMapper.selectOne(gq);check(group!=null,"源薪资组不存在或不可访问");
        HrmPayrollSchemeSnapshotVO result=new HrmPayrollSchemeSnapshotVO().setGroup(new HrmPayrollSchemeSnapshotVO.Group()
                .setId(group.getId()).setName(group.getName()).setSalaryStandard(decimal(group.getSalaryStandard())).setChangeRule(group.getChangeRule()).setTaxRuleId(group.getTaxRuleId()));
        if(group.getTaxRuleId()!=null) {
            QueryWrapper<HrmSalaryTaxRuleDO> tq=new QueryWrapper<HrmSalaryTaxRuleDO>().eq("tenant_id",tenant()).eq("id",group.getTaxRuleId())
                    .select("id","name","type","tax_enabled","threshold","decimal_scale","cycle_type");if(lock)tq.last("FOR UPDATE");
            HrmSalaryTaxRuleDO tax=taxMapper.selectOne(tq);
            if(tax!=null)result.setTaxRule(new HrmPayrollSchemeSnapshotVO.TaxRule().setId(tax.getId()).setName(tax.getName()).setType(tax.getType())
                    .setTaxEnabled(tax.getTaxEnabled()).setThreshold(decimal(tax.getThreshold())).setDecimalScale(tax.getDecimalScale()).setCycleType(tax.getCycleType()));
        }
        QueryWrapper<HrmSalaryOptionDO> oq=new QueryWrapper<HrmSalaryOptionDO>().eq("tenant_id",tenant())
                .select("id","code","parent_code","template_id","name","remark","system_flag","type","visible","enabled","tax_enabled","calculate_enabled");
        // Count first to avoid materializing a pathological catalogue; the locked result is also bounded below.
        check(optionMapper.selectCount(new QueryWrapper<HrmSalaryOptionDO>().eq("tenant_id",tenant()))<=1000,"租户薪资项目录超过 1000 项，请先核对目录规模");
        // The current tenant SQL parser reorders FOR UPDATE before ORDER BY. Lock without ORDER BY,
        // then canonicalize the bounded catalogue in Java for stable fingerprints.
        if(lock)oq.last("FOR UPDATE");else oq.orderByAsc("code","id").last("LIMIT 1001");
        List<HrmSalaryOptionDO> options=optionMapper.selectList(oq);check(options.size()<=1000,"租户薪资项目录超过 1000 项");
        options.sort(Comparator.comparing((HrmSalaryOptionDO o)->o.getCode(),Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(HrmSalaryOptionDO::getId));
        result.setOptions(options.stream().map(o->BeanUtils.toBean(o,HrmPayrollSchemeSnapshotVO.Option.class)).collect(Collectors.toList()));
        validateSnapshot(result);return result;
    }
    private void validateSnapshot(HrmPayrollSchemeSnapshotVO s) {
        if(StrUtil.isBlank(s.getGroup().getName()))s.getIssues().add("薪资组名称缺失");
        if(s.getGroup().getSalaryStandard()==null||new BigDecimal(s.getGroup().getSalaryStandard()).signum()<=0)s.getIssues().add("月计薪标准缺失或不大于零");
        if(StrUtil.isBlank(s.getGroup().getChangeRule()))s.getIssues().add("薪资变更规则缺失");
        if(s.getTaxRule()==null)s.getIssues().add("源薪资组引用的计税规则不存在于当前租户");
        else {
            HrmSalaryTaxRuleSaveReqVO tax=BeanUtils.toBean(s.getTaxRule(),HrmSalaryTaxRuleSaveReqVO.class);
            tax.setThreshold(s.getTaxRule().getThreshold()==null?null:new BigDecimal(s.getTaxRule().getThreshold()));
            validator.validate(tax).stream().map(v->"计税规则："+v.getMessage()).sorted().distinct().forEach(s.getIssues()::add);
        }
        if(s.getOptions().stream().noneMatch(o->Boolean.TRUE.equals(o.getEnabled())))s.getIssues().add("没有启用的薪资项");
        Map<Integer,HrmPayrollSchemeSnapshotVO.Option> options=new HashMap<>();
        for(HrmPayrollSchemeSnapshotVO.Option o:s.getOptions()) {
            if(o.getCode()==null||o.getCode()<=0||options.put(o.getCode(),o)!=null)s.getIssues().add("薪资项编码缺失、无效或重复");
            if(StrUtil.isBlank(o.getName())||o.getType()==null||o.getType()<0||o.getType()>2)s.getIssues().add("薪资项 "+o.getCode()+" 名称或类型不合法");
            if(o.getEnabled()==null||o.getTaxEnabled()==null||o.getCalculateEnabled()==null||o.getVisible()==null)s.getIssues().add("薪资项 "+o.getCode()+" 配置标志缺失");
        }
        for(HrmPayrollSchemeSnapshotVO.Option o:s.getOptions()) {
            Set<Integer> visited=new HashSet<>();HrmPayrollSchemeSnapshotVO.Option node=o;
            while(node!=null&&node.getParentCode()!=null&&node.getParentCode()!=0) {
                if(!visited.add(node.getCode())) { s.getIssues().add("薪资项 "+o.getCode()+" 父级存在循环");break; }
                node=options.get(node.getParentCode());if(node==null)s.getIssues().add("薪资项 "+o.getCode()+" 父级缺失");
            }
            if(node!=null&&node.getParentCode()==null)s.getIssues().add("薪资项 "+o.getCode()+" 父编码缺失");
        }
    }
    @Override @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public HrmPayrollSchemeRespVO capture(Long groupId) {
        guard();HrmPayrollSchemeSnapshotVO s=snapshot(groupId,false);
        return new HrmPayrollSchemeRespVO().setGroupId(groupId).setGroupName(s.getGroup().getName()).setSnapshot(s).setSourceHash(hash(s))
                .setCapturedAt(LocalDateTime.now()).setOptionCount(s.getOptions().size());
    }
    private void validate(HrmPayrollSchemeSaveReqVO req,boolean confirm) {
        check(req!=null&&StrUtil.isNotBlank(req.getTitle()),"方案名称不能为空");date(req.getEffectiveFrom());date(req.getEffectiveTo());
        check(req.getEffectiveTo()==null||(req.getEffectiveFrom()!=null&&!req.getEffectiveFrom().isAfter(req.getEffectiveTo())),"有效期结束不能早于开始，且须先填写开始日期");
        if(confirm) { check(StrUtil.isNotBlank(req.getOwnerName()),"确认必须填写负责人");check(StrUtil.isNotBlank(req.getReference()),"确认必须填写配置依据");check(req.getEffectiveFrom()!=null,"确认必须填写有效期开始日期"); }
    }
    private HrmPayrollSchemeDO editable(HrmPayrollSchemeSaveReqVO req) {
        validate(req,false);HrmPayrollSchemeSnapshotVO s=snapshot(req.getGroupId(),true);String hash=hash(s);
        if(req.getExpectedSourceHash()!=null&&!req.getExpectedSourceHash().equals(hash))throw exception(PAYROLL_SCHEME_SOURCE_CHANGED);
        HrmPayrollSchemeDO row=new HrmPayrollSchemeDO().setId(req.getId()).setGroupId(req.getGroupId()).setGroupName(s.getGroup().getName()).setTitle(req.getTitle().trim())
                .setOwnerName(StrUtil.trimToNull(req.getOwnerName())).setReference(StrUtil.trimToNull(req.getReference())).setEffectiveFrom(req.getEffectiveFrom()).setEffectiveTo(req.getEffectiveTo())
                .setOptionCount(s.getOptions().size()).setSnapshotJson(JsonUtils.toJsonString(s)).setSourceHash(hash).setCapturedAt(LocalDateTime.now());row.setTenantId(tenant());return row;
    }
    @Override @Transactional(rollbackFor=Exception.class,isolation=Isolation.READ_COMMITTED)
    public Long create(HrmPayrollSchemeSaveReqVO req) {
        guard();validate(req,false);mapper.lockGroup(req.getGroupId(),tenant());
        if(mapper.selectCount(query().eq(HrmPayrollSchemeDO::getGroupId,req.getGroupId()))>0)throw exception(PAYROLL_SCHEME_DUPLICATE);
        HrmPayrollSchemeDO row=editable(req).setId(null).setSchemeVersion(1).setRevision(1).setStatus(0);
        try { mapper.insert(row); }catch(DuplicateKeyException e){throw exception(PAYROLL_SCHEME_DUPLICATE);}
        audit("create",null,row,"抓取既有配置为草稿，尚未确认");return row.getId();
    }
    @Override @Transactional(rollbackFor=Exception.class,isolation=Isolation.READ_COMMITTED)
    public void update(HrmPayrollSchemeSaveReqVO req) {
        guard();HrmPayrollSchemeDO before=require(req.getId(),false);mapper.lockGroup(before.getGroupId(),tenant());before=require(req.getId(),true);revision(before,req.getRevision());
        if(before.getStatus()!=0)throw exception(PAYROLL_SCHEME_IMMUTABLE);
        check(Objects.equals(before.getGroupId(),req.getGroupId()),"源薪资组固定，不能修改");
        HrmPayrollSchemeDO after=editable(req).setSchemeVersion(before.getSchemeVersion()).setRevision(before.getRevision()+1).setStatus(0);
        mapper.updateById(after);audit("update",before,after,"维护方案草稿并重新抓取现有配置");
    }
    @Override @Transactional(rollbackFor=Exception.class,isolation=Isolation.READ_COMMITTED)
    public Long newVersion(Long id,Integer expected) {
        guard();HrmPayrollSchemeDO selected=require(id,false);mapper.lockGroup(selected.getGroupId(),tenant());selected=require(id,true);revision(selected,expected);
        HrmPayrollSchemeDO latest=mapper.selectOne(query().eq(HrmPayrollSchemeDO::getGroupId,selected.getGroupId()).orderByDesc(HrmPayrollSchemeDO::getSchemeVersion).last("LIMIT 1"));
        HrmPayrollSchemeDO row=editable(BeanUtils.toBean(selected,HrmPayrollSchemeSaveReqVO.class)).setId(null).setSchemeVersion(latest.getSchemeVersion()+1).setRevision(1).setStatus(0);
        mapper.insert(row);audit("new-version",null,row,"从 V"+selected.getSchemeVersion()+" 新建草稿，抓取当前配置且不继承确认结论");return row.getId();
    }
    @Override @Transactional(rollbackFor=Exception.class,isolation=Isolation.READ_COMMITTED)
    public void review(HrmPayrollSchemeReviewReqVO req) {
        guard();check(req!=null&&StrUtil.isNotBlank(req.getEvidence()),"必须填写评审依据");check("confirm".equals(req.getAction())||"retire".equals(req.getAction()),"评审操作不合法");
        HrmPayrollSchemeDO before=require(req.getId(),false);mapper.lockGroup(before.getGroupId(),tenant());before=require(req.getId(),true);revision(before,req.getRevision());
        boolean confirm="confirm".equals(req.getAction());
        if(confirm) {
            check(before.getStatus()==0,"只有草稿可确认");validate(BeanUtils.toBean(before,HrmPayrollSchemeSaveReqVO.class),true);
            HrmPayrollSchemeSnapshotVO current=snapshot(before.getGroupId(),true);
            if(!Objects.equals(before.getSourceHash(),hash(current)))throw exception(PAYROLL_SCHEME_SOURCE_CHANGED);
            check(current.getIssues().isEmpty(),"配置检查未通过："+String.join("；",current.getIssues()));
            LambdaQueryWrapperX<HrmPayrollSchemeDO> q=query().eq(HrmPayrollSchemeDO::getGroupId,before.getGroupId()).eq(HrmPayrollSchemeDO::getStatus,1);
            if(before.getEffectiveTo()!=null)q.le(HrmPayrollSchemeDO::getEffectiveFrom,before.getEffectiveTo());
            LocalDate start=before.getEffectiveFrom();q.and(w->w.isNull(HrmPayrollSchemeDO::getEffectiveTo).or().ge(HrmPayrollSchemeDO::getEffectiveTo,start));
            if(mapper.selectCount(q)>0)throw exception(PAYROLL_SCHEME_OVERLAP);
        }else check(before.getStatus()==1,"只有已确认方案可停用");
        HrmPayrollSchemeDO after=BeanUtils.toBean(before,HrmPayrollSchemeDO.class).setStatus(confirm?1:2).setRevision(before.getRevision()+1).setReviewedBy(getLoginUserId())
                .setReviewedByName(actorName()).setReviewedTime(LocalDateTime.now()).setEvidence(req.getEvidence().trim());
        mapper.updateById(after);audit(req.getAction(),before,after,req.getEvidence());
    }
    private HrmPayrollSchemeRespVO response(HrmPayrollSchemeDO row,boolean detail) {
        HrmPayrollSchemeRespVO result=BeanUtils.toBean(row,HrmPayrollSchemeRespVO.class);
        if(detail)result.setSnapshot(JsonUtils.parseObject(row.getSnapshotJson(),HrmPayrollSchemeSnapshotVO.class));return result;
    }
    @Override public HrmPayrollSchemeRespVO get(Long id) { guard();return response(require(id,false),true); }
    @Override public PageResult<HrmPayrollSchemeRespVO> page(HrmPayrollSchemePageReqVO req) {
        guard();LambdaQueryWrapperX<HrmPayrollSchemeDO> q=query().eqIfPresent(HrmPayrollSchemeDO::getGroupId,req.getGroupId()).eqIfPresent(HrmPayrollSchemeDO::getStatus,req.getStatus())
                .likeIfPresent(HrmPayrollSchemeDO::getTitle,req.getSearch()).orderByDesc(HrmPayrollSchemeDO::getId);
        q.select(HrmPayrollSchemeDO.class,f->!"snapshotJson".equals(f.getProperty())&&!"reference".equals(f.getProperty())&&!"evidence".equals(f.getProperty()));
        PageResult<HrmPayrollSchemeDO> rows=mapper.selectPage(req,q);return new PageResult<>(rows.getList().stream().map(r->response(r,false)).collect(Collectors.toList()),rows.getTotal());
    }
    @Override public List<HrmPayrollReviewDO> history(Long id) {
        guard();require(id,false);return reviewMapper.selectList(new LambdaQueryWrapperX<HrmPayrollReviewDO>().eq(HrmPayrollReviewDO::getTenantId,tenant())
                .eq(HrmPayrollReviewDO::getObjectType,"scheme").eq(HrmPayrollReviewDO::getObjectId,id).orderByDesc(HrmPayrollReviewDO::getId));
    }
    @Override @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public HrmPayrollSchemeCompareVO compare(Long leftId,Long rightId) {
        guard();HrmPayrollSchemeDO left=require(leftId,false),right=require(rightId,false);check(left.getGroupId().equals(right.getGroupId()),"只能比较同一薪资组的配置版本");
        HrmPayrollSchemeRespVO l=response(left,true),r=response(right,true);Map<String,String> lv=flatten(l.getSnapshot()),rv=flatten(r.getSnapshot());
        Set<String> paths=new LinkedHashSet<>(lv.keySet());paths.addAll(rv.keySet());List<HrmPayrollSchemeCompareVO.Change> changes=new ArrayList<>();
        for(String path:paths)if(!Objects.equals(lv.get(path),rv.get(path))||lv.containsKey(path)!=rv.containsKey(path))changes.add(new HrmPayrollSchemeCompareVO.Change().setPath(path).setLabel(label(path))
                .setLeft(lv.get(path)).setRight(rv.get(path)).setKind(!lv.containsKey(path)?"ADDED":!rv.containsKey(path)?"REMOVED":"CHANGED"));
        return new HrmPayrollSchemeCompareVO().setLeft(l).setRight(r).setChanges(changes);
    }
    private Map<String,String> flatten(HrmPayrollSchemeSnapshotVO s) {
        Map<String,String> result=new LinkedHashMap<>();fields(result,"group",s.getGroup());fields(result,"taxRule",s.getTaxRule());
        for(HrmPayrollSchemeSnapshotVO.Option option:s.getOptions())fields(result,"options."+option.getCode(),option);return result;
    }
    private void fields(Map<String,String> out,String prefix,Object value) {
        if(value==null)return;com.fasterxml.jackson.databind.JsonNode node=JsonUtils.parseTree(JsonUtils.toJsonString(value));
        node.fields().forEachRemaining(e->out.put(prefix+"."+e.getKey(),e.getValue().isNull()?null:e.getValue().asText()));
    }
    private String label(String path) {
        String key=path.substring(path.lastIndexOf('.')+1);Map<String,String> names=new HashMap<>();
        names.put("id","记录编号");names.put("name","名称");names.put("salaryStandard","月计薪标准");names.put("changeRule","薪资变更规则");names.put("taxRuleId","引用计税规则");
        names.put("type","类型");names.put("taxEnabled","是否计税");names.put("threshold","起征阈值");names.put("decimalScale","小数位数");names.put("cycleType","计税周期");
        names.put("code","编码");names.put("parentCode","父编码");names.put("templateId","目录模板编号");names.put("remark","备注");names.put("systemFlag","系统项");
        names.put("visible","是否显示");names.put("enabled","是否启用");names.put("calculateEnabled","是否参与计算");
        String prefix=path.startsWith("group.")?"薪资组":path.startsWith("taxRule.")?"计税规则":"薪资项 "+path.split("\\.")[1];return prefix+" · "+names.getOrDefault(key,key);
    }
    @Override @Transactional(readOnly=true)
    public HrmPayrollSchemeRespVO resolve(Long groupId,LocalDate start,LocalDate end) {
        guard();date(start);date(end);check(start!=null&&end!=null&&!start.isAfter(end),"请声明合法核对期间");
        LambdaQueryWrapperX<HrmPayrollSchemeDO> q=query().eq(HrmPayrollSchemeDO::getGroupId,groupId).eq(HrmPayrollSchemeDO::getStatus,1);
        q.le(HrmPayrollSchemeDO::getEffectiveFrom,start);
        q.and(w->w.isNull(HrmPayrollSchemeDO::getEffectiveTo).or().ge(HrmPayrollSchemeDO::getEffectiveTo,end));
        List<HrmPayrollSchemeDO> choices=mapper.selectList(q.last("LIMIT 2"));check(choices.size()==1,"期间内未找到单个完整覆盖的确认方案，不自动拼接版本");
        return response(choices.get(0),true);
    }
    private String actorName() { return Optional.ofNullable(adminUserApi.getUser(getLoginUserId())).map(u->u.getNickname()).orElse("当前登录人员"); }
    private void audit(String action,HrmPayrollSchemeDO before,HrmPayrollSchemeDO after,String reason) {
        HrmPayrollReviewDO row=new HrmPayrollReviewDO().setObjectType("scheme").setObjectId(after.getId()).setAction(action).setFromVersion(before==null?null:before.getRevision())
                .setToVersion(after.getRevision()).setActorId(getLoginUserId()).setActorName(actorName()).setReason(reason)
                .setBeforeSnapshot(before==null?null:JsonUtils.toJsonString(before)).setAfterSnapshot(JsonUtils.toJsonString(after));row.setTenantId(tenant());reviewMapper.insert(row);
    }
}
