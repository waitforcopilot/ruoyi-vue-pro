package cn.iocoder.yudao.module.hrm.service.payroll.trial;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.calculation.*;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.eligibility.*;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.identity.HrmPayrollMappingRespVO;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.trial.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.HrmPayrollReviewDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.calculation.HrmPayrollCalculationDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.eligibility.HrmPayrollEligibilityDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.trial.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.HrmPayrollReviewMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.calculation.HrmPayrollCalculationMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.eligibility.HrmPayrollEligibilityMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.trial.*;
import cn.iocoder.yudao.module.hrm.service.payroll.calculation.*;
import cn.iocoder.yudao.module.hrm.service.payroll.eligibility.HrmPayrollEligibilityService;
import cn.iocoder.yudao.module.hrm.service.payroll.identity.*;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import javax.annotation.Resource;
import javax.validation.Validator;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;

/** An auditable batch execution layer. No mutable legacy wage ledger is written. */
@Service
public class HrmPayrollTrialServiceImpl implements HrmPayrollTrialService {
    @Resource private HrmPayrollTrialBatchMapper batches;
    @Resource private HrmPayrollTrialPersonMapper people;
    @Resource private HrmPayrollTrialRunMapper runs;
    @Resource private HrmPayrollReviewMapper reviews;
    @Resource private HrmPayrollCalculationMapper definitions;
    @Resource private HrmPayrollEligibilityMapper qualifications;
    @Resource private HrmPayrollCalculationService calculation;
    @Resource private HrmPayrollCalculationEngine engine;
    @Resource private HrmPayrollEligibilityService eligibility;
    @Resource private HrmPayrollEmployeeAccess access;
    @Resource private HrmPayrollEmployeeMappingService personnel;
    @Resource private PermissionApi permissionApi;
    @Resource private AdminUserApi adminUserApi;
    @Resource private Validator validator;

    private Long tenant() { return TenantContextHolder.getRequiredTenantId(); }
    private void guard() {
        for (String permission : Arrays.asList("hrm:payroll:trial:query", "hrm:payroll:calculation:query", "hrm:payroll:eligibility:query", "hrm:employee:query"))
            if (!permissionApi.hasAnyPermissions(getLoginUserId(), permission)) throw exception(PAYROLL_TRIAL_PERMISSION);
    }
    private void action(String permission) {
        guard(); if (!permissionApi.hasAnyPermissions(getLoginUserId(), permission)) throw exception(PAYROLL_TRIAL_PERMISSION);
    }
    private void valid(boolean condition, String message) { if (!condition) throw exception(PAYROLL_TRIAL_INVALID, message); }
    private void validate(Object value) {
        valid(value != null, "请求不能为空");
        String errors = validator.validate(value).stream().map(v -> v.getPropertyPath() + "：" + v.getMessage()).sorted().collect(Collectors.joining("；"));
        valid(errors.isEmpty(), errors);
    }
    private LambdaQueryWrapperX<HrmPayrollTrialBatchDO> batchQuery() { return new LambdaQueryWrapperX<HrmPayrollTrialBatchDO>().eq(HrmPayrollTrialBatchDO::getTenantId, tenant()); }
    private LambdaQueryWrapperX<HrmPayrollTrialRunDO> runQuery() { return new LambdaQueryWrapperX<HrmPayrollTrialRunDO>().eq(HrmPayrollTrialRunDO::getTenantId, tenant()); }
    private LambdaQueryWrapperX<HrmPayrollTrialPersonDO> personQuery(Long batchId) {
        return new LambdaQueryWrapperX<HrmPayrollTrialPersonDO>().eq(HrmPayrollTrialPersonDO::getTenantId, tenant()).eq(HrmPayrollTrialPersonDO::getBatchId, batchId);
    }
    private HrmPayrollTrialBatchDO require(Long id, boolean lock) {
        guard(); LambdaQueryWrapperX<HrmPayrollTrialBatchDO> q = batchQuery().eq(HrmPayrollTrialBatchDO::getId, id); if (lock) q.last("FOR UPDATE");
        HrmPayrollTrialBatchDO row = id == null ? null : batches.selectOne(q);
        if (row == null) throw exception(PAYROLL_TRIAL_NOT_EXISTS);
        List<HrmPayrollTrialPersonDO> footprint = people.selectList(personQuery(id));
        if (footprint.isEmpty()) throw exception(PAYROLL_TRIAL_NOT_EXISTS);
        for (HrmPayrollTrialPersonDO person : footprint) {
            try { access.require(person.getEmployeeId(), person.getSnapshotDeptId(), person.getSnapshotUserId()); }
            catch (ServiceException error) { if (Objects.equals(error.getCode(), PAYROLL_MAPPING_NOT_EXISTS.getCode())) throw exception(PAYROLL_TRIAL_NOT_EXISTS); throw error; }
        }
        return row;
    }
    private HrmPayrollTrialStoredConfigVO stored(HrmPayrollTrialBatchDO row) { return JsonUtils.parseObject(row.getConfigurationJson(), HrmPayrollTrialStoredConfigVO.class); }
    private HrmPayrollTrialRespVO response(HrmPayrollTrialBatchDO row, boolean detail) {
        HrmPayrollTrialRespVO vo = BeanUtils.toBean(row, HrmPayrollTrialRespVO.class);
        if (detail) vo.setConfiguration(stored(row));
        return vo;
    }
    private HrmPayrollTrialBatchDO editable(HrmPayrollTrialSaveReqVO req) {
        validate(req); valid(req.getPeriodStart().getYear() >= 1000 && req.getPeriodEnd().getYear() <= 9999, "期间超过数据库范围");
        long days = ChronoUnit.DAYS.between(req.getPeriodStart(), req.getPeriodEnd()); valid(days >= 0 && days <= 365, "期间须合法且最多 366 天");
        if ("MONTHLY".equals(req.getPeriodType())) valid(req.getPeriodStart().getDayOfMonth() == 1 && req.getPeriodEnd().equals(YearMonth.from(req.getPeriodStart()).atEndOfMonth()), "自然月期间须从月初至该月月末");
        valid(req.getConfiguration().getPeople().stream().map(HrmPayrollTrialConfigVO.PersonInput::getEmployeeId).distinct().count() == req.getConfiguration().getPeople().size(), "同一批次不能重复登记人员");
        calculation.get(req.getDefinitionId());
        HrmPayrollTrialStoredConfigVO config = new HrmPayrollTrialStoredConfigVO().setRoles(req.getConfiguration().getRoles());
        for (Long employeeId : req.getConfiguration().getPeople().stream().map(HrmPayrollTrialConfigVO.PersonInput::getEmployeeId).sorted().collect(Collectors.toList())) access.employee(employeeId, true);
        for (HrmPayrollTrialConfigVO.PersonInput input : req.getConfiguration().getPeople()) { HrmPayrollMappingRespVO person = personnel.employee(input.getEmployeeId());
            valid(input.getEmployeeFingerprint() == null || input.getEmployeeFingerprint().equals(person.getEmployeeFingerprint()), "人员档案已变化，请重新核对 HRM #" + input.getEmployeeId());
            HrmPayrollTrialStoredConfigVO.Person snapshot = BeanUtils.toBean(input, HrmPayrollTrialStoredConfigVO.Person.class)
                    .setSnapshotName(person.getSnapshotName()).setSnapshotJobNumber(person.getSnapshotJobNumber())
                    .setSnapshotDeptId(person.getSnapshotDeptId()).setSnapshotUserId(person.getSnapshotUserId()).setSnapshotCapturedAt(person.getSnapshotCapturedAt());
            snapshot.setEmployeeFingerprint(person.getEmployeeFingerprint()); snapshot.setInputReference(StrUtil.trimToNull(input.getInputReference())); config.getPeople().add(snapshot);
        }
        String json = JsonUtils.toJsonString(config); valid(json.getBytes(StandardCharsets.UTF_8).length <= 512 * 1024, "批次输入资料过大，请分批登记");
        HrmPayrollTrialBatchDO row = BeanUtils.toBean(req, HrmPayrollTrialBatchDO.class).setTitle(req.getTitle().trim()).setEntityName(req.getEntityName().trim())
                .setOwnerName(StrUtil.trimToNull(req.getOwnerName())).setReference(StrUtil.trimToNull(req.getReference()))
                .setConfigurationJson(json).setPersonCount(config.getPeople().size()).setStatus(0).setCurrentRunId(null);
        row.setTenantId(tenant()); return row;
    }
    private void footprint(HrmPayrollTrialBatchDO batch) {
        for (HrmPayrollTrialStoredConfigVO.Person person : stored(batch).getPeople()) {
            if (people.selectCount(personQuery(batch.getId()).eq(HrmPayrollTrialPersonDO::getEmployeeId, person.getEmployeeId())
                    .eq(HrmPayrollTrialPersonDO::getEmployeeFingerprint, person.getEmployeeFingerprint())) > 0) continue;
            HrmPayrollTrialPersonDO row = new HrmPayrollTrialPersonDO().setBatchId(batch.getId()).setEmployeeId(person.getEmployeeId())
                    .setEmployeeFingerprint(person.getEmployeeFingerprint()).setSnapshotDeptId(person.getSnapshotDeptId()).setSnapshotUserId(person.getSnapshotUserId());
            row.setTenantId(tenant()); people.insert(row);
        }
    }
    @Override @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public Long create(HrmPayrollTrialSaveReqVO req) {
        action("hrm:payroll:trial:maintain"); HrmPayrollTrialBatchDO row = editable(req).setId(null).setRevision(1).setLatestRunId(null);
        try { batches.insert(row); } catch (DuplicateKeyException error) { throw exception(PAYROLL_TRIAL_DUPLICATE); }
        footprint(row); audit("create", null, row, "登记明确主体、期间和人员的试算草稿"); return row.getId();
    }
    @Override @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public void update(HrmPayrollTrialSaveReqVO req) {
        action("hrm:payroll:trial:maintain"); validate(req); HrmPayrollTrialBatchDO before = require(req.getId(), true); revision(before, req.getRevision());
        valid(before.getStatus() <= 1, "已复核或冻结的批次不能修改");
        valid(Objects.equals(before.getCode(), req.getCode()) && Objects.equals(before.getEntityCode(), req.getEntityCode())
                && Objects.equals(before.getPeriodType(), req.getPeriodType()) && Objects.equals(before.getPeriodStart(), req.getPeriodStart())
                && Objects.equals(before.getPeriodEnd(), req.getPeriodEnd()), "批次编号、主体编号和期间固定，不能修改");
        HrmPayrollTrialBatchDO after = editable(req).setRevision(before.getRevision() + 1).setLatestRunId(before.getLatestRunId());
        batches.updateById(after); footprint(after); audit("update", before, after, "维护批次输入，当前试算失效，历史版本保留");
    }
    private void revision(HrmPayrollTrialBatchDO batch, Integer expected) { if (!Objects.equals(batch.getRevision(), expected)) throw exception(PAYROLL_TRIAL_STALE); }
    @Override public HrmPayrollTrialRespVO get(Long id) { return response(require(id, false), true); }
    @Override public PageResult<HrmPayrollTrialRespVO> page(HrmPayrollTrialPageReqVO req) {
        guard(); validate(req);
        return BeanUtils.toBean(batches.selectPage(req, access.filterTrialBatches(batchQuery())
                .eqIfPresent(HrmPayrollTrialBatchDO::getEntityCode, StrUtil.trimToNull(req.getEntityCode())).likeIfPresent(HrmPayrollTrialBatchDO::getTitle, req.getSearch())
                .eqIfPresent(HrmPayrollTrialBatchDO::getStatus, req.getStatus()).select(HrmPayrollTrialBatchDO.class, f -> !Arrays.asList("configurationJson", "reference").contains(f.getProperty()))
                .orderByDesc(HrmPayrollTrialBatchDO::getId)), HrmPayrollTrialRespVO.class);
    }
    private HrmPayrollTrialCheckVO.Issue issue(String code, String message) { return new HrmPayrollTrialCheckVO.Issue().setCode(code).setMessage(message); }
    private Map<String, String> roles(HrmPayrollTrialConfigVO.Roles binding) {
        Map<String, String> keys = new LinkedHashMap<>(); keys.put("gross", binding.getGross()); keys.put("deductions", binding.getDeductions()); keys.put("tax", binding.getTax()); keys.put("net", binding.getNet()); return keys;
    }
    private HrmPayrollTrialResultVO evaluate(HrmPayrollTrialBatchDO batch) {
        HrmPayrollTrialCheckVO report = new HrmPayrollTrialCheckVO().setBatchId(batch.getId()).setRevision(batch.getRevision()).setCheckedAt(LocalDateTime.now());
        HrmPayrollTrialResultVO result = new HrmPayrollTrialResultVO().setBatch(response(batch, true)).setCheck(report);
        HrmPayrollTrialStoredConfigVO config = stored(batch); Map<String, String> roleKeys = roles(config.getRoles());
        if (StrUtil.isBlank(batch.getOwnerName()) || StrUtil.isBlank(batch.getReference())) report.getIssues().add(issue("BATCH_BASIS_MISSING", "须填写负责人和批次口径依据"));
        HrmPayrollCalculationEngine.Compiled compiled = null;
        try {
            HrmPayrollCalculationRespVO definition = calculation.get(batch.getDefinitionId()); result.setDefinition(definition);
            if (definition.getStatus() != 1 || definition.getEffectiveFrom() == null || definition.getEffectiveFrom().isAfter(batch.getPeriodStart())
                    || definition.getEffectiveTo() != null && definition.getEffectiveTo().isBefore(batch.getPeriodEnd())) {
                report.getIssues().add(issue("DEFINITION_NOT_EFFECTIVE", "规则须已确认且完整覆盖批次期间"));
            } else {
                compiled = engine.compile(definition.getProgram()); result.setProgramHash(compiled.getProgramHash());
                if (!Boolean.TRUE.equals(compiled.verifyCases().getAllPassed())) { report.getIssues().add(issue("DEFINITION_CASES_FAILED", "所选规则的样例未全部通过")); compiled = null; }
                Map<String, HrmPayrollCalculationSpecVO.Item> items = definition.getProgram().getItems().stream().collect(Collectors.toMap(HrmPayrollCalculationSpecVO.Item::getKey, item -> item));
                boolean mapping = new HashSet<>(roleKeys.values()).size() == 4 && roleKeys.values().stream().allMatch(key -> items.containsKey(key)
                        && "CNY".equals(items.get(key).getUnit()) && Objects.equals(items.get(key).getAmountScale(), 2));
                if (!mapping) { report.getIssues().add(issue("OUTPUT_BINDING_INVALID", "应发、扣款、个税和实发须分别绑定四个不同的 CNY/2 位结果")); compiled = null; }
            }
        } catch (ServiceException error) { report.getIssues().add(issue("DEFINITION_UNAVAILABLE", "规则不存在、不可访问或定义不合法")); }
        int included = 0, excluded = 0, blocked = 0;
        Map<String, BigDecimal> totals = new LinkedHashMap<>(); roleKeys.keySet().forEach(role -> totals.put(role, new BigDecimal("0.00")));
        for (HrmPayrollTrialStoredConfigVO.Person input : config.getPeople()) {
            HrmPayrollTrialCheckVO.Person checked = new HrmPayrollTrialCheckVO.Person().setEmployeeId(input.getEmployeeId()).setName(input.getSnapshotName()); report.getPeople().add(checked);
            HrmPayrollTrialResultVO.Person person = new HrmPayrollTrialResultVO.Person().setInput(input); result.getPeople().add(person);
            HrmPayrollEligibilityLookupVO qualification = eligibility.lookup(new HrmPayrollEligibilityLookupReqVO().setEntityCode(batch.getEntityCode())
                    .setEmployeeId(input.getEmployeeId()).setStart(batch.getPeriodStart()).setEnd(batch.getPeriodEnd()));
            person.setEligibility(qualification.getEligibility());
            if (!Boolean.TRUE.equals(qualification.getMatched())) checked.getIssues().add(issue(qualification.getIssueCode(), qualification.getExplanation()));
            else {
                checked.setEligibilityId(qualification.getEligibility().getId()).setEligibilityVersion(qualification.getEligibility().getEligibilityVersion());
                if ("EXCLUDED".equals(qualification.getEligibility().getQualification())) {
                    checked.setState("EXCLUDED"); person.setState("EXCLUDED"); excluded++; continue;
                }
                if (!Objects.equals(input.getEmployeeFingerprint(), qualification.getEligibility().getEmployeeFingerprint())) checked.getIssues().add(issue("DRAFT_PERSON_CHANGED", "批次人员快照与资格快照不同，请刷新批次草稿"));
                if (StrUtil.isBlank(input.getInputReference())) checked.getIssues().add(issue("INPUT_BASIS_MISSING", "须填写个人金额输入的来源依据"));
                if (compiled == null) checked.getIssues().add(issue("DEFINITION_BLOCKED", "所选规则未通过核验"));
                if (checked.getIssues().isEmpty()) {
                    try {
                        HrmPayrollCalculationResultVO calculationResult = compiled.execute(input.getInputs()); person.setCalculation(calculationResult);
                        valid(calculationResult.getInputs().values().stream().allMatch(v -> new BigDecimal(v).signum() >= 0), "常规工资批次不接受负数输入，追溯调整应使用后续调整流程");
                        Map<String, String> amounts = calculationResult.getItems().stream().collect(Collectors.toMap(HrmPayrollCalculationResultVO.Result::getKey, HrmPayrollCalculationResultVO.Result::getAmount));
                        for (Map.Entry<String, String> role : roleKeys.entrySet()) person.getAmounts().put(role.getKey(), amounts.get(role.getValue()));
                        valid(person.getAmounts().values().stream().allMatch(v -> new BigDecimal(v).signum() >= 0), "常规工资结果不能为负数");
                        BigDecimal net = new BigDecimal(person.getAmounts().get("gross")).subtract(new BigDecimal(person.getAmounts().get("deductions"))).subtract(new BigDecimal(person.getAmounts().get("tax")));
                        valid(net.compareTo(new BigDecimal(person.getAmounts().get("net"))) == 0, "实发须精确等于应发减扣款减个税");
                    } catch (ServiceException error) { person.setCalculation(null); person.getAmounts().clear(); checked.getIssues().add(issue("INPUT_OR_AMOUNT_INVALID", error.getMessage())); }
                }
            }
            if (checked.getIssues().isEmpty()) { checked.setState("READY"); person.setState("CALCULATED"); included++; person.getAmounts().forEach((role, value) -> totals.put(role, totals.get(role).add(new BigDecimal(value)))); }
            else { checked.setState("BLOCKED"); person.setState("BLOCKED"); blocked++; }
        }
        if (included == 0 && blocked == 0) report.getIssues().add(issue("NO_INCLUDED_PEOPLE", "批次没有纳入计薪的人员，不能生成空工资版本"));
        report.setIncludedCount(included).setExcludedCount(excluded).setBlockedCount(blocked).setReady(blocked == 0 && report.getIssues().isEmpty());
        totals.forEach((role, value) -> result.getTotals().put(role, value.setScale(2).toPlainString()));
        LocalDateTime time = report.getCheckedAt(); report.setCheckedAt(null); report.setSourceHash(digest(result)); report.setCheckedAt(time); return result;
    }
    private String digest(Object value) {
        try { byte[] bytes = MessageDigest.getInstance("SHA-256").digest(JsonUtils.toJsonString(value).getBytes(StandardCharsets.UTF_8)); StringBuilder hash = new StringBuilder(); for (byte b : bytes) hash.append(String.format("%02x", b & 255)); return hash.toString(); }
        catch (NoSuchAlgorithmException error) { throw new IllegalStateException(error); }
    }
    @Override @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public HrmPayrollTrialCheckVO check(Long id) { return evaluate(require(id, false)).getCheck(); }
    @Override @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public HrmPayrollTrialInspectionVO inspect(Long id) {
        HrmPayrollTrialBatchDO batch = require(id, false);
        HrmPayrollTrialResultVO current = evaluate(batch);
        HrmPayrollTrialInspectionVO out = new HrmPayrollTrialInspectionVO().setBatch(current.getBatch()).setCheck(current.getCheck()).setAvailability("NONE");
        Long runId = batch.getCurrentRunId() == null ? batch.getLatestRunId() : batch.getCurrentRunId();
        if (runId == null) return out;
        HrmPayrollTrialRunDO saved = runs.selectOne(runQuery().eq(HrmPayrollTrialRunDO::getId, runId).eq(HrmPayrollTrialRunDO::getBatchId, id));
        if (saved == null) return out.setAvailability("UNAVAILABLE");
        HrmPayrollTrialRunRespVO snapshot = runResponse(saved, true); out.setRun(snapshot);
        if (batch.getCurrentRunId() == null) return out.setAvailability("INVALIDATED");
        return out.setAvailability(Boolean.TRUE.equals(current.getCheck().getReady()) && manifest(current).equals(manifest(snapshot.getResult())) ? "CURRENT" : "SOURCE_CHANGED");
    }
    private void lockSources(HrmPayrollTrialBatchDO batch) {
        HrmPayrollCalculationRespVO definition = calculation.get(batch.getDefinitionId());
        definitions.selectOne(new LambdaQueryWrapperX<HrmPayrollCalculationDO>().eq(HrmPayrollCalculationDO::getTenantId, tenant())
                .eq(HrmPayrollCalculationDO::getCode, definition.getCode()).eq(HrmPayrollCalculationDO::getDefinitionVersion, 1).last("FOR UPDATE"));
        definitions.selectOne(new LambdaQueryWrapperX<HrmPayrollCalculationDO>().eq(HrmPayrollCalculationDO::getTenantId, tenant()).eq(HrmPayrollCalculationDO::getId, definition.getId()).last("FOR UPDATE"));
        List<Long> ids = stored(batch).getPeople().stream().map(HrmPayrollTrialStoredConfigVO.Person::getEmployeeId).sorted().collect(Collectors.toList());
        for (Long id : ids) qualifications.selectOne(new LambdaQueryWrapperX<HrmPayrollEligibilityDO>().eq(HrmPayrollEligibilityDO::getTenantId, tenant())
                .eq(HrmPayrollEligibilityDO::getEntityCode, batch.getEntityCode()).eq(HrmPayrollEligibilityDO::getEmployeeId, id).eq(HrmPayrollEligibilityDO::getEligibilityVersion, 1).last("FOR UPDATE"));
        for (Long id : ids) {
            try { access.employee(id, true); }
            catch (ServiceException error) { if (!Objects.equals(error.getCode(), PAYROLL_MAPPING_NOT_EXISTS.getCode())) throw error; }
        }
    }
    @Override @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public HrmPayrollTrialRunRespVO execute(HrmPayrollTrialExecuteReqVO req) {
        action("hrm:payroll:trial:execute"); validate(req); HrmPayrollTrialBatchDO batch = require(req.getBatchId(), true);
        HrmPayrollTrialRunDO replay = runs.selectOne(runQuery().eq(HrmPayrollTrialRunDO::getBatchId, batch.getId()).eq(HrmPayrollTrialRunDO::getRequestKey, req.getRequestKey()));
        if (replay != null) {
            if (!Objects.equals(replay.getExpectedRevision(), req.getRevision()) || !Objects.equals(replay.getSourceHash(), req.getSourceHash())) throw exception(PAYROLL_TRIAL_REQUEST_CONFLICT);
            return runResponse(replay, true);
        }
        revision(batch, req.getRevision()); valid(batch.getStatus() <= 1, "已复核或冻结的批次不能重新试算"); lockSources(batch);
        HrmPayrollTrialResultVO result = evaluate(batch);
        if (!Boolean.TRUE.equals(result.getCheck().getReady())) throw exception(PAYROLL_TRIAL_BLOCKED);
        if (!result.getCheck().getSourceHash().equals(req.getSourceHash())) throw exception(PAYROLL_TRIAL_STALE);
        String json = JsonUtils.toJsonString(result); valid(json.getBytes(StandardCharsets.UTF_8).length <= 4 * 1024 * 1024, "试算解释资料过大，请减少批次人数或规则项目");
        HrmPayrollTrialRunDO latest = runs.selectOne(runQuery().eq(HrmPayrollTrialRunDO::getBatchId, batch.getId()).orderByDesc(HrmPayrollTrialRunDO::getRunVersion).last("LIMIT 1"));
        HrmPayrollTrialRunDO row = new HrmPayrollTrialRunDO().setBatchId(batch.getId()).setRunVersion(latest == null ? 1 : latest.getRunVersion() + 1)
                .setRequestKey(req.getRequestKey()).setExpectedRevision(req.getRevision()).setSourceHash(req.getSourceHash()).setResultJson(json)
                .setIncludedCount(result.getCheck().getIncludedCount()).setExcludedCount(result.getCheck().getExcludedCount()).setExecutedBy(getLoginUserId())
                .setExecutedByName(actorName()).setExecutedAt(LocalDateTime.now()); row.setTenantId(tenant()); runs.insert(row);
        HrmPayrollTrialBatchDO after = BeanUtils.toBean(batch, HrmPayrollTrialBatchDO.class).setRevision(batch.getRevision() + 1).setStatus(1).setCurrentRunId(row.getId()).setLatestRunId(row.getId()).setActiveReviewId(null).setFrozenRunId(null);
        batches.updateById(after); audit("execute", batch, after, "保存试算 V" + row.getRunVersion() + "；未执行发放、审批或工资条发布"); return runResponse(row, true);
    }
    @Override public HrmPayrollTrialBatchDO lockBatch(Long id) { return require(id, true); }
    @Override public String validateCurrentRun(Long batchId, Long runId) {
        HrmPayrollTrialBatchDO batch = require(batchId, true);
        HrmPayrollTrialRunDO run = runs.selectOne(runQuery().eq(HrmPayrollTrialRunDO::getId, runId).eq(HrmPayrollTrialRunDO::getBatchId, batchId));
        if (run == null || !Objects.equals(batch.getCurrentRunId(), runId)) throw exception(PAYROLL_REVIEW_STALE);
        lockSources(batch); HrmPayrollTrialResultVO current = evaluate(batch), saved = JsonUtils.parseObject(run.getResultJson(), HrmPayrollTrialResultVO.class);
        if (!Boolean.TRUE.equals(current.getCheck().getReady()) || !manifest(current).equals(manifest(saved))) throw exception(PAYROLL_REVIEW_SOURCE_CHANGED);
        return manifest(saved);
    }
    private String manifest(HrmPayrollTrialResultVO result) {
        Map<String, Object> source = new LinkedHashMap<>();
        HrmPayrollTrialRespVO b = result.getBatch();
        Map<String, Object> identity = new LinkedHashMap<>();
        identity.put("code", b.getCode()); identity.put("title", b.getTitle()); identity.put("entityCode", b.getEntityCode()); identity.put("entityName", b.getEntityName());
        identity.put("periodType", b.getPeriodType()); identity.put("periodStart", b.getPeriodStart()); identity.put("periodEnd", b.getPeriodEnd());
        identity.put("definitionId", b.getDefinitionId()); identity.put("ownerName", b.getOwnerName()); identity.put("reference", b.getReference()); identity.put("configuration", b.getConfiguration());
        source.put("batch", identity); source.put("definition", result.getDefinition()); source.put("programHash", result.getProgramHash()); source.put("people", result.getPeople()); source.put("totals", result.getTotals());
        return digest(source);
    }
    private HrmPayrollTrialRunRespVO runResponse(HrmPayrollTrialRunDO row, boolean detail) {
        HrmPayrollTrialRunRespVO vo = BeanUtils.toBean(row, HrmPayrollTrialRunRespVO.class);
        if (detail) vo.setResult(JsonUtils.parseObject(row.getResultJson(), HrmPayrollTrialResultVO.class)); return vo;
    }
    private HrmPayrollTrialRunDO requireRun(Long id) {
        guard(); HrmPayrollTrialRunDO row = id == null ? null : runs.selectOne(runQuery().eq(HrmPayrollTrialRunDO::getId, id));
        if (row == null) throw exception(PAYROLL_TRIAL_NOT_EXISTS); require(row.getBatchId(), false); return row;
    }
    @Override public HrmPayrollTrialRunRespVO run(Long id) { return runResponse(requireRun(id), true); }
    @Override public List<HrmPayrollTrialRunRespVO> runs(Long id) {
        require(id, false); return runs.selectList(runQuery().eq(HrmPayrollTrialRunDO::getBatchId, id).orderByDesc(HrmPayrollTrialRunDO::getRunVersion))
                .stream().map(row -> runResponse(row, false)).collect(Collectors.toList());
    }
    @Override public HrmPayrollTrialCompareVO compare(Long leftId, Long rightId) {
        HrmPayrollTrialRunRespVO left = run(leftId), right = run(rightId); valid(left.getBatchId().equals(right.getBatchId()), "只能对比同一批次的试算版本");
        HrmPayrollTrialCompareVO diff = new HrmPayrollTrialCompareVO().setLeft(left).setRight(right)
                .setRuleChanged(!Objects.equals(left.getResult().getProgramHash(), right.getResult().getProgramHash())
                        || !Objects.equals(JsonUtils.toJsonString(left.getResult().getBatch().getConfiguration().getRoles()), JsonUtils.toJsonString(right.getResult().getBatch().getConfiguration().getRoles())));
        for (String role : left.getResult().getTotals().keySet()) diff.getTotalDifferences().put(role, subtract(right.getResult().getTotals().get(role), left.getResult().getTotals().get(role)));
        Map<Long, HrmPayrollTrialResultVO.Person> l = index(left), r = index(right); Set<Long> ids = new TreeSet<>(l.keySet()); ids.addAll(r.keySet());
        for (Long id : ids) {
            HrmPayrollTrialResultVO.Person a = l.get(id), b = r.get(id);
            HrmPayrollTrialCompareVO.Person person = new HrmPayrollTrialCompareVO.Person().setEmployeeId(id).setName((b == null ? a : b).getInput().getSnapshotName())
                    .setLeftAmounts(a == null ? null : a.getAmounts()).setRightAmounts(b == null ? null : b.getAmounts())
                    .setLeftInputs(a == null ? null : a.getInput().getInputs()).setRightInputs(b == null ? null : b.getInput().getInputs());
            if (a == null) person.setChange("ADDED"); else if (b == null) person.setChange("REMOVED"); else if (!Objects.equals(a.getState(), b.getState())) person.setChange("QUALIFICATION_CHANGED");
            else {
                Map<String, String> amounts = new LinkedHashMap<>(); for (String role : a.getAmounts().keySet()) amounts.put(role, subtract(b.getAmounts().get(role), a.getAmounts().get(role))); person.setDifferences(amounts);
                person.setChange(!a.getAmounts().equals(b.getAmounts()) ? "AMOUNTS_CHANGED" : !a.getInput().getInputs().equals(b.getInput().getInputs()) ? "INPUTS_CHANGED" : !Objects.equals(a.getEligibility().getId(), b.getEligibility().getId()) || !Objects.equals(a.getInput().getEmployeeFingerprint(), b.getInput().getEmployeeFingerprint()) || !Objects.equals(a.getInput().getInputReference(), b.getInput().getInputReference()) ? "SOURCE_CHANGED" : "UNCHANGED");
            }
            diff.getPeople().add(person);
        }
        return diff;
    }
    private Map<Long, HrmPayrollTrialResultVO.Person> index(HrmPayrollTrialRunRespVO run) { return run.getResult().getPeople().stream().collect(Collectors.toMap(p -> p.getInput().getEmployeeId(), p -> p)); }
    private String subtract(String right, String left) { return new BigDecimal(right).subtract(new BigDecimal(left)).setScale(2).toPlainString(); }
    @Override public List<HrmPayrollReviewDO> history(Long id) {
        require(id, false); return reviews.selectList(new LambdaQueryWrapperX<HrmPayrollReviewDO>().eq(HrmPayrollReviewDO::getTenantId, tenant())
                .eq(HrmPayrollReviewDO::getObjectType, "trial-batch").eq(HrmPayrollReviewDO::getObjectId, id).orderByDesc(HrmPayrollReviewDO::getId));
    }
    private String actorName() { return Optional.ofNullable(adminUserApi.getUser(getLoginUserId())).map(user -> user.getNickname()).orElse("当前登录人员"); }
    private void audit(String action, HrmPayrollTrialBatchDO before, HrmPayrollTrialBatchDO after, String reason) {
        HrmPayrollReviewDO row = new HrmPayrollReviewDO().setObjectType("trial-batch").setObjectId(after.getId()).setAction(action)
                .setFromVersion(before == null ? null : before.getRevision()).setToVersion(after.getRevision()).setActorId(getLoginUserId()).setActorName(actorName()).setReason(reason)
                .setBeforeSnapshot(before == null ? null : JsonUtils.toJsonString(before)).setAfterSnapshot(JsonUtils.toJsonString(after)); row.setTenantId(tenant()); reviews.insert(row);
    }
}
