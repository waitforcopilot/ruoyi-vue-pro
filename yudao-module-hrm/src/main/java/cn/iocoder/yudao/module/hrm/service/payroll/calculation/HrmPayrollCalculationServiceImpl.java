package cn.iocoder.yudao.module.hrm.service.payroll.calculation;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.calculation.*;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.scheme.HrmPayrollSchemeCompareVO.Change;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.HrmPayrollReviewDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.calculation.HrmPayrollCalculationDO;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.HrmPayrollReviewMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.calculation.HrmPayrollCalculationMapper;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import javax.annotation.Resource;
import javax.validation.Validator;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;

@Service
public class HrmPayrollCalculationServiceImpl implements HrmPayrollCalculationService {
    @Resource private HrmPayrollCalculationMapper mapper;
    @Resource private HrmPayrollReviewMapper reviewMapper;
    @Resource private HrmPayrollCalculationEngine engine;
    @Resource private AdminUserApi adminUserApi;
    @Resource private Validator validator;
    private void check(boolean valid, String reason) { if (!valid) throw exception(PAYROLL_CALCULATION_INVALID, reason); }
    private void validate(Object value) {
        check(value != null, "请求资料缺失");
        String issues = validator.validate(value).stream().map(v -> v.getPropertyPath() + "：" + v.getMessage()).sorted().collect(Collectors.joining("；"));
        check(issues.isEmpty(), issues);
    }
    private LambdaQueryWrapperX<HrmPayrollCalculationDO> query() {
        return new LambdaQueryWrapperX<HrmPayrollCalculationDO>().eq(HrmPayrollCalculationDO::getTenantId, TenantContextHolder.getRequiredTenantId());
    }
    private HrmPayrollCalculationDO require(Long id, boolean lock) {
        LambdaQueryWrapperX<HrmPayrollCalculationDO> q = query().eq(HrmPayrollCalculationDO::getId, id);
        if (lock) q.last("FOR UPDATE");
        HrmPayrollCalculationDO row = id == null ? null : mapper.selectOne(q);
        if (row == null) throw exception(PAYROLL_CALCULATION_NOT_EXISTS);
        return row;
    }
    private void lockSeries(HrmPayrollCalculationDO row) {
        HrmPayrollCalculationDO first = mapper.selectOne(query().eq(HrmPayrollCalculationDO::getCode, row.getCode())
                .eq(HrmPayrollCalculationDO::getDefinitionVersion, 1).last("FOR UPDATE"));
        check(first != null, "计算规则首版本缺失，无法维护");
    }
    private void revision(HrmPayrollCalculationDO row, Integer revision) {
        if (!Objects.equals(row.getRevision(), revision)) throw exception(PAYROLL_CALCULATION_STALE);
    }
    private void date(LocalDate value) { check(value == null || value.getYear() >= 1000 && value.getYear() <= 9999, "日期超过数据库范围"); }
    private HrmPayrollCalculationEngine.Compiled compiled(HrmPayrollCalculationDO row) {
        check(Objects.equals(row.getSchemaVersion(), 1), "当前计算定义结构版本不支持执行");
        return engine.compile(JsonUtils.parseObject(row.getProgramJson(), HrmPayrollCalculationSpecVO.class));
    }
    private HrmPayrollCalculationDO editable(HrmPayrollCalculationSaveReqVO req) {
        validate(req); date(req.getEffectiveFrom()); date(req.getEffectiveTo());
        check(req.getEffectiveTo() == null || req.getEffectiveFrom() != null && !req.getEffectiveFrom().isAfter(req.getEffectiveTo()), "有效期结束不能早于开始，须先填写开始日期");
        HrmPayrollCalculationEngine.Compiled program = engine.compile(req.getProgram());
        HrmPayrollCalculationDO result = BeanUtils.toBean(req, HrmPayrollCalculationDO.class)
                .setTitle(req.getTitle().trim()).setOwnerName(StrUtil.trimToNull(req.getOwnerName()))
                .setApplicableScope(StrUtil.trimToNull(req.getApplicableScope())).setDescription(StrUtil.trimToNull(req.getDescription()))
                .setReference(StrUtil.trimToNull(req.getReference())).setSchemaVersion(1).setProgramJson(program.getProgramJson())
                .setInputCount(program.getInputCount()).setItemCount(program.getItemCount()).setCaseCount(program.getCaseCount())
                .setVerificationJson(JsonUtils.toJsonString(program.verifyCases()));
        result.setTenantId(TenantContextHolder.getRequiredTenantId()); return result;
    }
    private HrmPayrollCalculationRespVO response(HrmPayrollCalculationDO row, boolean detail) {
        HrmPayrollCalculationRespVO result = BeanUtils.toBean(row, HrmPayrollCalculationRespVO.class);
        if (detail) {
            result.setProgram(JsonUtils.parseObject(row.getProgramJson(), HrmPayrollCalculationSpecVO.class));
            result.setVerifiedCases(JsonUtils.parseObject(row.getVerificationJson(), HrmPayrollCalculationCasesVO.class));
        }
        return result;
    }
    @Override @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public Long create(HrmPayrollCalculationSaveReqVO req) {
        HrmPayrollCalculationDO row = editable(req).setId(null).setDefinitionVersion(1).setRevision(1).setStatus(0);
        if (mapper.selectCount(query().eq(HrmPayrollCalculationDO::getCode, row.getCode())) > 0) throw exception(PAYROLL_CALCULATION_DUPLICATE);
        try { mapper.insert(row); } catch (DuplicateKeyException error) { throw exception(PAYROLL_CALCULATION_DUPLICATE); }
        audit("create", null, row, "登记明确表达式、单位和精度的计算草稿"); return row.getId();
    }
    @Override @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public void update(HrmPayrollCalculationSaveReqVO req) {
        HrmPayrollCalculationDO before = require(req.getId(), false); lockSeries(before); before = require(req.getId(), true); revision(before, req.getRevision());
        if (before.getStatus() != 0) throw exception(PAYROLL_CALCULATION_IMMUTABLE);
        HrmPayrollCalculationDO after = editable(req);
        check(before.getCode().equals(after.getCode()) && before.getScopeCode().equals(after.getScopeCode()), "规则编号和范围编号固定，不能修改");
        after.setDefinitionVersion(before.getDefinitionVersion()).setRevision(before.getRevision() + 1).setStatus(0);
        mapper.updateById(after); audit("update", before, after, "维护计算草稿并重新核对业务样例");
    }
    @Override @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public Long newVersion(Long id, Integer expected) {
        HrmPayrollCalculationDO selected = require(id, false); lockSeries(selected); selected = require(id, true); revision(selected, expected);
        HrmPayrollCalculationDO latest = mapper.selectOne(query().eq(HrmPayrollCalculationDO::getCode, selected.getCode())
                .orderByDesc(HrmPayrollCalculationDO::getDefinitionVersion).last("LIMIT 1"));
        HrmPayrollCalculationDO row = BeanUtils.toBean(selected, HrmPayrollCalculationDO.class).setId(null)
                .setDefinitionVersion(latest.getDefinitionVersion() + 1).setRevision(1).setStatus(0)
                .setReviewedBy(null).setReviewedByName(null).setReviewedTime(null).setEvidence(null);
        row.clean(); mapper.insert(row); audit("new-version", null, row, "从 V" + selected.getDefinitionVersion() + " 复制定义，独立评审"); return row.getId();
    }
    @Override @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public void review(HrmPayrollCalculationReviewReqVO req) {
        validate(req); HrmPayrollCalculationDO before = require(req.getId(), false); lockSeries(before); before = require(req.getId(), true); revision(before, req.getRevision());
        boolean confirm = "confirm".equals(req.getAction()); HrmPayrollCalculationCasesVO verified = null;
        if (confirm) {
            check(before.getStatus() == 0, "只有草稿可以确认");
            check(StrUtil.isNotBlank(before.getOwnerName()) && StrUtil.isNotBlank(before.getApplicableScope()) && StrUtil.isNotBlank(before.getReference()),
                    "确认须填写负责人、适用范围说明和规则依据");
            check(before.getEffectiveFrom() != null, "确认须填写有效期开始日期");
            verified = compiled(before).verifyCases();
            if (!Boolean.TRUE.equals(verified.getAllPassed())) throw exception(PAYROLL_CALCULATION_CASES_FAILED);
            LambdaQueryWrapperX<HrmPayrollCalculationDO> q = query().eq(HrmPayrollCalculationDO::getCode, before.getCode())
                    .eq(HrmPayrollCalculationDO::getScopeCode, before.getScopeCode()).eq(HrmPayrollCalculationDO::getStatus, 1);
            if (before.getEffectiveTo() != null) q.le(HrmPayrollCalculationDO::getEffectiveFrom, before.getEffectiveTo());
            LocalDate start = before.getEffectiveFrom();
            q.and(w -> w.isNull(HrmPayrollCalculationDO::getEffectiveTo).or().ge(HrmPayrollCalculationDO::getEffectiveTo, start));
            if (mapper.selectCount(q) > 0) throw exception(PAYROLL_CALCULATION_OVERLAP);
        } else check(before.getStatus() == 1, "只有已确认版本可以停用");
        HrmPayrollCalculationDO after = BeanUtils.toBean(before, HrmPayrollCalculationDO.class).setStatus(confirm ? 1 : 2)
                .setRevision(before.getRevision() + 1).setReviewedBy(getLoginUserId()).setReviewedByName(actorName())
                .setReviewedTime(LocalDateTime.now()).setEvidence(req.getEvidence().trim());
        if (verified != null) after.setVerificationJson(JsonUtils.toJsonString(verified));
        mapper.updateById(after); audit(req.getAction(), before, after, req.getEvidence());
    }
    @Override public HrmPayrollCalculationRespVO get(Long id) { return response(require(id, false), true); }
    @Override public PageResult<HrmPayrollCalculationRespVO> page(HrmPayrollCalculationPageReqVO req) {
        validate(req);
        LambdaQueryWrapperX<HrmPayrollCalculationDO> q = query().likeIfPresent(HrmPayrollCalculationDO::getTitle, req.getSearch())
                .eqIfPresent(HrmPayrollCalculationDO::getCode, req.getCode()).eqIfPresent(HrmPayrollCalculationDO::getScopeCode, req.getScopeCode())
                .eqIfPresent(HrmPayrollCalculationDO::getStatus, req.getStatus()).orderByDesc(HrmPayrollCalculationDO::getId);
        q.select(HrmPayrollCalculationDO.class, f -> !Arrays.asList("programJson", "verificationJson", "reference", "description", "evidence").contains(f.getProperty()));
        PageResult<HrmPayrollCalculationDO> rows = mapper.selectPage(req, q);
        return new PageResult<>(rows.getList().stream().map(row -> response(row, false)).collect(Collectors.toList()), rows.getTotal());
    }
    @Override public List<HrmPayrollReviewDO> history(Long id) {
        require(id, false);
        return reviewMapper.selectList(new LambdaQueryWrapperX<HrmPayrollReviewDO>().eq(HrmPayrollReviewDO::getTenantId, TenantContextHolder.getRequiredTenantId())
                .eq(HrmPayrollReviewDO::getObjectType, "calculation-definition").eq(HrmPayrollReviewDO::getObjectId, id).orderByDesc(HrmPayrollReviewDO::getId));
    }
    @Override public HrmPayrollCalculationCasesVO cases(Long id) { return compiled(require(id, false)).verifyCases(); }
    @Override @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public HrmPayrollCalculationPreviewVO preview(HrmPayrollCalculationPreviewReqVO req) {
        validate(req); date(req.getStart()); date(req.getEnd()); check(!req.getStart().isAfter(req.getEnd()), "核对结束不能早于开始");
        HrmPayrollCalculationDO row = require(req.getDefinitionId(), false);
        check(row.getStatus() == 1, "只有已确认版本可核对输入");
        check(row.getEffectiveFrom() != null && !row.getEffectiveFrom().isAfter(req.getStart())
                && (row.getEffectiveTo() == null || !row.getEffectiveTo().isBefore(req.getEnd())), "所选版本须完整覆盖声明期间，不自动拼接版本");
        HrmPayrollCalculationEngine.Compiled program = compiled(row);
        return new HrmPayrollCalculationPreviewVO().setDefinition(response(row, true)).setProgramHash(program.getProgramHash())
                .setStart(req.getStart()).setEnd(req.getEnd()).setResult(program.execute(req.getInputs()))
                .setExplanation("按所选版本和明确输入核对规则。后续项目引用前项舍入金额；单位为声明，不自动换算或按期间分摊。结果不生成工资月表、个人扣款或发放记录。");
    }
    @Override @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public HrmPayrollCalculationCompareVO compare(Long leftId, Long rightId) {
        HrmPayrollCalculationDO left = require(leftId, false), right = require(rightId, false);
        check(left.getCode().equals(right.getCode()) && left.getScopeCode().equals(right.getScopeCode()), "只能对比同规则编号及范围的版本");
        Map<String, String> l = fields(left), r = fields(right); List<Change> changes = new ArrayList<>();
        Set<String> keys = new LinkedHashSet<>(l.keySet()); keys.addAll(r.keySet());
        for (String key : keys) if (!Objects.equals(l.get(key), r.get(key))) changes.add(new Change().setPath(key).setLabel(label(key))
                .setLeft(l.get(key)).setRight(r.get(key)).setKind("CHANGED"));
        return new HrmPayrollCalculationCompareVO().setLeft(response(left, true)).setRight(response(right, true)).setChanges(changes);
    }
    private Map<String, String> fields(HrmPayrollCalculationDO row) {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("title", row.getTitle()); result.put("description", row.getDescription()); result.put("reference", row.getReference());
        result.put("applicableScope", row.getApplicableScope()); result.put("ownerName", row.getOwnerName());
        result.put("effectiveFrom", Objects.toString(row.getEffectiveFrom(), null)); result.put("effectiveTo", Objects.toString(row.getEffectiveTo(), null));
        result.put("program", compiled(row).getProgramJson()); return result;
    }
    private String label(String key) {
        Map<String, String> labels = new HashMap<>(); labels.put("title", "规则名称"); labels.put("description", "说明");
        labels.put("reference", "规则依据"); labels.put("applicableScope", "适用范围说明"); labels.put("ownerName", "负责人");
        labels.put("effectiveFrom", "有效期开始"); labels.put("effectiveTo", "有效期结束"); labels.put("program", "输入、表达式、精度及样例");
        return labels.getOrDefault(key, key);
    }
    private String actorName() { return Optional.ofNullable(adminUserApi.getUser(getLoginUserId())).map(u -> u.getNickname()).orElse("当前登录人员"); }
    private void audit(String action, HrmPayrollCalculationDO before, HrmPayrollCalculationDO after, String reason) {
        HrmPayrollReviewDO row = new HrmPayrollReviewDO().setObjectType("calculation-definition").setObjectId(after.getId()).setAction(action)
                .setFromVersion(before == null ? null : before.getRevision()).setToVersion(after.getRevision()).setActorId(getLoginUserId())
                .setActorName(actorName()).setReason(reason.trim()).setBeforeSnapshot(before == null ? null : JsonUtils.toJsonString(before))
                .setAfterSnapshot(JsonUtils.toJsonString(after));
        row.setTenantId(TenantContextHolder.getRequiredTenantId()); reviewMapper.insert(row);
    }
}
