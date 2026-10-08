package cn.iocoder.yudao.module.hrm.service.payroll.review;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;

import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.review.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.HrmPayrollReviewDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.review.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.trial.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.employee.info.HrmEmployeeMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.HrmPayrollReviewMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.review.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.trial.*;
import cn.iocoder.yudao.module.hrm.service.payroll.trial.HrmPayrollTrialService;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import javax.validation.Validator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/**
 * BPM-backed two-person review; approvals and freezes never modify trial amounts or legacy ledgers.
 */
@Service
public class HrmPayrollReviewServiceImpl implements HrmPayrollReviewService {
    @Resource private HrmPayrollTrialService trial;
    @Resource private HrmPayrollTrialBatchMapper batches;
    @Resource private HrmPayrollTrialRunMapper runs;
    @Resource private HrmPayrollTrialPersonMapper footprint;
    @Resource private HrmEmployeeMapper employees;
    @Resource private HrmPayrollReviewCycleMapper cycles;
    @Resource private HrmPayrollReviewCommandMapper commands;
    @Resource private HrmPayrollReviewMapper audits;
    @Resource private HrmPayrollReviewBpmGateway bpm;
    @Resource private PermissionApi permissions;
    @Resource private AdminUserApi users;
    @Resource private Validator validator;

    private Long tenant() {
        return TenantContextHolder.getRequiredTenantId();
    }

    private Long actor() {
        return getLoginUserId();
    }

    private void valid(boolean ok, String msg) {
        if (!ok) throw exception(PAYROLL_REVIEW_INVALID, msg);
    }

    private void allow(Long id, String permission) {
        if (!permissions.hasAnyPermissions(id, permission))
            throw exception(PAYROLL_REVIEW_PERMISSION);
    }

    private LambdaQueryWrapperX<HrmPayrollReviewCycleDO> query(Long batchId) {
        return new LambdaQueryWrapperX<HrmPayrollReviewCycleDO>()
                .eq(HrmPayrollReviewCycleDO::getTenantId, tenant())
                .eq(HrmPayrollReviewCycleDO::getBatchId, batchId);
    }

    private HrmPayrollReviewCycleDO cycle(Long id, Long batchId) {
        HrmPayrollReviewCycleDO c =
                id == null
                        ? null
                        : cycles.selectOne(query(batchId).eq(HrmPayrollReviewCycleDO::getId, id));
        if (c == null) throw exception(PAYROLL_REVIEW_STALE);
        return c;
    }

    private HrmPayrollTrialRunDO run(HrmPayrollTrialBatchDO b, Long id) {
        HrmPayrollTrialRunDO r =
                runs.selectOne(
                        new LambdaQueryWrapperX<HrmPayrollTrialRunDO>()
                                .eq(HrmPayrollTrialRunDO::getTenantId, tenant())
                                .eq(HrmPayrollTrialRunDO::getBatchId, b.getId())
                                .eq(HrmPayrollTrialRunDO::getId, id));
        if (r == null) throw exception(PAYROLL_REVIEW_STALE);
        return r;
    }

    private boolean visible(DeptDataPermissionRespDTO scope, Long userId, Long dept, Long user) {
        return Boolean.TRUE.equals(scope.getAll())
                || (scope.getDeptIds() != null && dept != null && scope.getDeptIds().contains(dept))
                || (Boolean.TRUE.equals(scope.getSelf()) && userId.equals(user));
    }

    private AdminUserRespDTO reviewer(Long id, Long batchId, String permission) {
        AdminUserRespDTO user = id == null ? null : users.getUser(id);
        valid(user != null && Objects.equals(user.getStatus(), 0), "复核人不存在、跨租户或未启用");
        for (String p :
                Arrays.asList(
                        "hrm:payroll:trial:query",
                        "hrm:payroll:calculation:query",
                        "hrm:payroll:eligibility:query",
                        "hrm:employee:query",
                        permission)) allow(id, p);
        DeptDataPermissionRespDTO scope = permissions.getDeptDataPermission(id);
        if (scope == null) throw exception(PAYROLL_REVIEW_PERMISSION);
        if (!Boolean.TRUE.equals(scope.getAll()))
            for (HrmPayrollTrialPersonDO person :
                    footprint.selectList(
                            new LambdaQueryWrapperX<HrmPayrollTrialPersonDO>()
                                    .eq(HrmPayrollTrialPersonDO::getTenantId, tenant())
                                    .eq(HrmPayrollTrialPersonDO::getBatchId, batchId))) {
                HrmEmployeeDO current =
                        employees.selectOne(
                                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<
                                                HrmEmployeeDO>()
                                        .eq("tenant_id", tenant())
                                        .eq("id", person.getEmployeeId())
                                        .select("id", "dept_id", "user_id"));
                if (current == null
                        || !visible(
                                scope, id, person.getSnapshotDeptId(), person.getSnapshotUserId())
                        || !visible(scope, id, current.getDeptId(), current.getUserId()))
                    throw exception(PAYROLL_REVIEW_PERMISSION);
            }
        return user;
    }

    private String name(Long id) {
        return Optional.ofNullable(users.getUser(id))
                .map(AdminUserRespDTO::getNickname)
                .orElse("当前登录人员");
    }

    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public HrmPayrollReviewRespVO get(Long batchId) {
        trial.get(batchId);
        HrmPayrollTrialBatchDO b =
                batches.selectOne(
                        new LambdaQueryWrapperX<HrmPayrollTrialBatchDO>()
                                .eq(HrmPayrollTrialBatchDO::getTenantId, tenant())
                                .eq(HrmPayrollTrialBatchDO::getId, batchId));
        return response(b);
    }

    private HrmPayrollReviewRespVO response(HrmPayrollTrialBatchDO b) {
        HrmPayrollReviewRespVO out =
                new HrmPayrollReviewRespVO()
                        .setBatchId(b.getId())
                        .setRevision(b.getRevision())
                        .setBatchStatus(b.getStatus())
                        .setActiveReviewId(b.getActiveReviewId())
                        .setFrozenRunId(b.getFrozenRunId());
        List<HrmPayrollReviewCycleDO> list =
                cycles.selectList(
                        query(b.getId()).orderByDesc(HrmPayrollReviewCycleDO::getCycleVersion));
        out.setCycles(BeanUtils.toBean(list, HrmPayrollReviewRespVO.Cycle.class));
        if (b.getActiveReviewId() != null) {
            HrmPayrollReviewCycleDO c = cycle(b.getActiveReviewId(), b.getId());
            out.setCycle(BeanUtils.toBean(c, HrmPayrollReviewRespVO.Cycle.class));
            if (c.getStatus() == 0) out.setTasks(bpm.tasks(c.getProcessInstanceId()));
        }
        return out;
    }

    private void permission(HrmPayrollReviewActionReqVO r, HrmPayrollReviewCycleDO c) {
        String action = r.getAction();
        if ("approve".equals(action) || "reject".equals(action)) {
            if (c == null) throw exception(PAYROLL_REVIEW_STALE);
            if (Objects.equals(actor(), c.getHrReviewerId()))
                allow(actor(), "hrm:payroll:trial:hr-review");
            else if (Objects.equals(actor(), c.getFinanceReviewerId()))
                allow(actor(), "hrm:payroll:trial:finance-review");
            else throw exception(PAYROLL_REVIEW_PERMISSION);
        } else
            allow(
                    actor(),
                    "hrm:payroll:trial:"
                            + ("cancel".equals(action)
                                    ? "review-submit"
                                    : action.equals("submit") ? "review-submit" : action));
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public HrmPayrollReviewRespVO action(HrmPayrollReviewActionReqVO r) {
        valid(r != null, "请求不能为空");
        String errors =
                validator.validate(r).stream()
                        .map(e -> e.getPropertyPath() + "：" + e.getMessage())
                        .sorted()
                        .collect(Collectors.joining("；"));
        valid(errors.isEmpty(), errors);
        HrmPayrollTrialBatchDO b = trial.lockBatch(r.getBatchId());
        HrmPayrollReviewCycleDO c =
                r.getCycleId() == null ? null : cycle(r.getCycleId(), b.getId());
        permission(r, c);
        String hash = hash(r);
        HrmPayrollReviewCommandDO replay =
                commands.selectOne(
                        new LambdaQueryWrapperX<HrmPayrollReviewCommandDO>()
                                .eq(HrmPayrollReviewCommandDO::getTenantId, tenant())
                                .eq(HrmPayrollReviewCommandDO::getBatchId, b.getId())
                                .eq(HrmPayrollReviewCommandDO::getRequestKey, r.getRequestKey()));
        if (replay != null) {
            if (!Objects.equals(replay.getActorId(), actor())
                    || !Objects.equals(replay.getRequestHash(), hash))
                throw exception(PAYROLL_REVIEW_CONFLICT);
            return JsonUtils.parseObject(replay.getResponseJson(), HrmPayrollReviewRespVO.class);
        }
        if (!Objects.equals(b.getRevision(), r.getRevision())
                || !Objects.equals(b.getCurrentRunId(), r.getRunId()))
            throw exception(PAYROLL_REVIEW_STALE);
        HrmPayrollTrialRunDO wage = run(b, r.getRunId());
        HrmPayrollTrialBatchDO before = BeanUtils.toBean(b, HrmPayrollTrialBatchDO.class);
        if ("submit".equals(r.getAction())) {
            valid(b.getStatus() == 1 && c == null, "须选择尚未进入复核的当前试算");
            valid(
                    r.getHrReviewerId() != null && r.getFinanceReviewerId() != null,
                    "必须明确 HR 和财务复核人");
            valid(
                    !r.getHrReviewerId().equals(r.getFinanceReviewerId())
                            && !Arrays.asList(wage.getExecutedBy(), actor())
                                    .contains(r.getHrReviewerId())
                            && !Arrays.asList(wage.getExecutedBy(), actor())
                                    .contains(r.getFinanceReviewerId()),
                    "两名复核人须不同且不能是核算人或发起人");
            AdminUserRespDTO
                    hr = reviewer(r.getHrReviewerId(), b.getId(), "hrm:payroll:trial:hr-review"),
                    finance =
                            reviewer(
                                    r.getFinanceReviewerId(),
                                    b.getId(),
                                    "hrm:payroll:trial:finance-review");
            String source = trial.validateCurrentRun(b.getId(), wage.getId()),
                    definition = bpm.definition();
            HrmPayrollReviewCycleDO last =
                    cycles.selectOne(
                            query(b.getId())
                                    .orderByDesc(HrmPayrollReviewCycleDO::getCycleVersion)
                                    .last("LIMIT 1"));
            c =
                    new HrmPayrollReviewCycleDO()
                            .setBatchId(b.getId())
                            .setRunId(wage.getId())
                            .setCycleVersion(last == null ? 1 : last.getCycleVersion() + 1)
                            .setStatus(0)
                            .setSourceHash(source)
                            .setProcessDefinitionId(definition)
                            .setStartedBy(actor())
                            .setStartedByName(name(actor()))
                            .setStartedAt(LocalDateTime.now())
                            .setSubmitEvidence(r.getEvidence().trim())
                            .setHrReviewerId(hr.getId())
                            .setHrReviewerName(hr.getNickname())
                            .setFinanceReviewerId(finance.getId())
                            .setFinanceReviewerName(finance.getNickname());
            c.setTenantId(tenant());
            cycles.insert(c);
            c.setProcessInstanceId(bpm.start(c));
            cycles.updateById(c);
            b.setStatus(2)
                    .setActiveReviewId(c.getId())
                    .setFrozenRunId(null)
                    .setRevision(b.getRevision() + 1);
            batches.updateById(b);
        } else {
            if (c == null
                    || !Objects.equals(b.getActiveReviewId(), c.getId())
                    || !Objects.equals(c.getRunId(), wage.getId()))
                throw exception(PAYROLL_REVIEW_STALE);
            String action = r.getAction();
            if ("approve".equals(action) || "reject".equals(action)) {
                valid(b.getStatus() == 2 && c.getStatus() == 0, "当前轮次不在复核中");
                List<HrmPayrollReviewRespVO.Task> tasks = bpm.tasks(c.getProcessInstanceId());
                HrmPayrollReviewRespVO.Task t =
                        tasks.stream()
                                .filter(x -> Objects.equals(x.getId(), r.getTaskId()))
                                .findFirst()
                                .orElseThrow(() -> exception(PAYROLL_REVIEW_STALE));
                String key = c.getHrReviewedAt() == null ? "hrReview" : "financeReview";
                Long expected =
                        key.equals("hrReview") ? c.getHrReviewerId() : c.getFinanceReviewerId();
                if (!key.equals(t.getKey())
                        || !Objects.equals(expected, actor())
                        || !Objects.equals(t.getAssigneeId(), actor())
                        || Arrays.asList(wage.getExecutedBy(), c.getStartedBy()).contains(actor()))
                    throw exception(PAYROLL_REVIEW_PERMISSION);
                reviewer(
                        actor(),
                        b.getId(),
                        "hrm:payroll:trial:"
                                + (key.equals("hrReview") ? "hr-review" : "finance-review"));
                if ("approve".equals(action)) {
                    String source = trial.validateCurrentRun(b.getId(), wage.getId());
                    if (!Objects.equals(source, c.getSourceHash()))
                        throw exception(PAYROLL_REVIEW_SOURCE_CHANGED);
                }
                if (key.equals("hrReview"))
                    c.setHrEvidence(r.getEvidence().trim()).setHrReviewedAt(LocalDateTime.now());
                else
                    c.setFinanceEvidence(r.getEvidence().trim())
                            .setFinanceReviewedAt(LocalDateTime.now());
                cycles.updateById(c);
                b.setRevision(b.getRevision() + 1);
                batches.updateById(b);
                bpm.decide(c.getProcessInstanceId(), t.getId(), actor(), action);
                // The Flowable completion listener may run before historic rows flush.
                // Reconcile again after the engine command returns using actual persisted status.
                sync(b.getId());
            } else if ("cancel".equals(action) || "admin-cancel".equals(action)) {
                valid(b.getStatus() == 2 && c.getStatus() == 0, "只能撤销运行中的本轮复核");
                if ("cancel".equals(action) && !Objects.equals(c.getStartedBy(), actor()))
                    throw exception(PAYROLL_REVIEW_PERMISSION);
                bpm.cancel(c.getProcessInstanceId(), actor(), "admin-cancel".equals(action));
                sync(b.getId());
            } else if ("freeze".equals(action)) {
                valid(
                        b.getStatus() == 3
                                && c.getStatus() == 1
                                && c.getHrReviewedAt() != null
                                && c.getFinanceReviewedAt() != null,
                        "只能冻结两级复核通过的当前试算");
                if (Arrays.asList(wage.getExecutedBy(), c.getStartedBy()).contains(actor()))
                    throw exception(PAYROLL_REVIEW_PERMISSION);
                if (!Objects.equals(
                        trial.validateCurrentRun(b.getId(), wage.getId()), c.getSourceHash()))
                    throw exception(PAYROLL_REVIEW_SOURCE_CHANGED);
                c.setStatus(4)
                        .setFrozenBy(actor())
                        .setFrozenByName(name(actor()))
                        .setFrozenAt(LocalDateTime.now())
                        .setFreezeEvidence(r.getEvidence().trim());
                cycles.updateById(c);
                b.setStatus(4).setFrozenRunId(wage.getId()).setRevision(b.getRevision() + 1);
                batches.updateById(b);
            } else if ("unfreeze".equals(action)) {
                valid(
                        b.getStatus() == 4
                                && c.getStatus() == 4
                                && Objects.equals(b.getFrozenRunId(), wage.getId()),
                        "只能解冻本轮冻结的版本");
                c.setStatus(5)
                        .setUnfrozenBy(actor())
                        .setUnfrozenByName(name(actor()))
                        .setUnfrozenAt(LocalDateTime.now())
                        .setUnfreezeEvidence(r.getEvidence().trim());
                cycles.updateById(c);
                b.setStatus(1)
                        .setActiveReviewId(null)
                        .setFrozenRunId(null)
                        .setRevision(b.getRevision() + 1);
                batches.updateById(b);
            }
        }
        audit(r.getAction(), before, c, r.getEvidence());
        HrmPayrollTrialBatchDO latest =
                batches.selectOne(
                        new LambdaQueryWrapperX<HrmPayrollTrialBatchDO>()
                                .eq(HrmPayrollTrialBatchDO::getTenantId, tenant())
                                .eq(HrmPayrollTrialBatchDO::getId, b.getId()));
        HrmPayrollReviewRespVO out = response(latest);
        HrmPayrollReviewCommandDO receipt =
                new HrmPayrollReviewCommandDO()
                        .setBatchId(b.getId())
                        .setActorId(actor())
                        .setRequestKey(r.getRequestKey())
                        .setRequestHash(hash)
                        .setResponseJson(JsonUtils.toJsonString(out));
        receipt.setTenantId(tenant());
        commands.insert(receipt);
        return out;
    }
    /**
     * Explicit recovery uses only the server's own cycle and actual BPM status, never client event
     * fields.
     */
    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public HrmPayrollReviewRespVO sync(Long batchId) {
        HrmPayrollTrialBatchDO b = trial.lockBatch(batchId);
        if (b.getStatus() == 2 && b.getActiveReviewId() != null) {
            HrmPayrollReviewCycleDO c = cycle(b.getActiveReviewId(), batchId);
            Integer status = bpm.actualStatus(c.getProcessInstanceId(), String.valueOf(c.getId()));
            if (Arrays.asList(2, 3, 4).contains(status))
                processEvent(
                        new BpmProcessInstanceStatusEvent(this)
                                .setId(c.getProcessInstanceId())
                                .setProcessDefinitionKey(PayrollBpmContext.KEY)
                                .setBusinessKey(String.valueOf(c.getId()))
                                .setStatus(status));
        }
        return response(
                batches.selectOne(
                        new LambdaQueryWrapperX<HrmPayrollTrialBatchDO>()
                                .eq(HrmPayrollTrialBatchDO::getTenantId, tenant())
                                .eq(HrmPayrollTrialBatchDO::getId, batchId)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public void processEvent(BpmProcessInstanceStatusEvent event) {
        if (event == null
                || !PayrollBpmContext.KEY.equals(event.getProcessDefinitionKey())
                || event.getBusinessKey() == null
                || !event.getBusinessKey().matches("[1-9][0-9]{0,18}")
                || !Arrays.asList(2, 3, 4).contains(event.getStatus())) return;
        Long id;
        try {
            id = Long.valueOf(event.getBusinessKey());
        } catch (NumberFormatException e) {
            return;
        }
        HrmPayrollReviewCycleDO hint =
                cycles.selectOne(
                        new LambdaQueryWrapperX<HrmPayrollReviewCycleDO>()
                                .eq(HrmPayrollReviewCycleDO::getTenantId, tenant())
                                .eq(HrmPayrollReviewCycleDO::getId, id));
        if (hint == null) return;
        HrmPayrollTrialBatchDO b =
                batches.selectOne(
                        new LambdaQueryWrapperX<HrmPayrollTrialBatchDO>()
                                .eq(HrmPayrollTrialBatchDO::getTenantId, tenant())
                                .eq(HrmPayrollTrialBatchDO::getId, hint.getBatchId())
                                .last("FOR UPDATE"));
        HrmPayrollReviewCycleDO c = cycle(id, hint.getBatchId());
        if (b == null
                || b.getStatus() != 2
                || c.getStatus() != 0
                || !Objects.equals(b.getActiveReviewId(), id)
                || !Objects.equals(b.getCurrentRunId(), c.getRunId())
                || !Objects.equals(event.getId(), c.getProcessInstanceId())
                || !Objects.equals(
                        event.getStatus(), bpm.actualStatus(event.getId(), event.getBusinessKey())))
            return;
        boolean approved = event.getStatus() == 2;
        if (approved
                && (c.getHrReviewedAt() == null
                        || c.getFinanceReviewedAt() == null
                        || c.getHrReviewerId().equals(c.getFinanceReviewerId()))) return;
        HrmPayrollTrialBatchDO before = BeanUtils.toBean(b, HrmPayrollTrialBatchDO.class);
        c.setStatus(approved ? 1 : event.getStatus() == 3 ? 2 : 3)
                .setOutcome(
                        approved ? "APPROVED" : event.getStatus() == 3 ? "REJECTED" : "CANCELLED")
                .setFinishedAt(LocalDateTime.now());
        cycles.updateById(c);
        b.setStatus(approved ? 3 : 0)
                .setCurrentRunId(approved ? c.getRunId() : null)
                .setFrozenRunId(null)
                .setRevision(b.getRevision() + 1);
        batches.updateById(b);
        audit(
                "bpm-" + c.getOutcome().toLowerCase(Locale.ROOT),
                before,
                c,
                "核对实际 BPM 终态、复核轮次和试算版本后同步业务状态");
    }

    private void audit(
            String action,
            HrmPayrollTrialBatchDO before,
            HrmPayrollReviewCycleDO c,
            String reason) {
        HrmPayrollReviewDO log =
                new HrmPayrollReviewDO()
                        .setObjectType("trial-batch")
                        .setObjectId(before.getId())
                        .setAction(action)
                        .setActorId(actor())
                        .setActorName(actor() == null ? "BPM 状态同步" : name(actor()))
                        .setFromVersion(before.getRevision())
                        .setToVersion(batches.selectById(before.getId()).getRevision())
                        .setReason(reason)
                        .setBeforeSnapshot(JsonUtils.toJsonString(before))
                        .setAfterSnapshot(JsonUtils.toJsonString(c == null ? null : cycle(c.getId(), before.getId())));
        log.setTenantId(tenant());
        audits.insert(log);
    }

    private String hash(Object value) {
        try {
            byte[] bytes =
                    MessageDigest.getInstance("SHA-256")
                            .digest(JsonUtils.toJsonString(value).getBytes(StandardCharsets.UTF_8));
            StringBuilder s = new StringBuilder();
            for (byte b : bytes) s.append(String.format("%02x", b & 255));
            return s.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
