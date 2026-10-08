package cn.iocoder.yudao.module.hrm.service.payroll.overview;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.overview.*;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.trial.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.review.HrmPayrollReviewCycleDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.trial.HrmPayrollTrialBatchDO;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.review.HrmPayrollReviewCycleMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.trial.HrmPayrollTrialBatchMapper;
import cn.iocoder.yudao.module.hrm.service.payroll.identity.HrmPayrollEmployeeAccess;
import cn.iocoder.yudao.module.hrm.service.payroll.review.PayrollBpmContext;
import cn.iocoder.yudao.module.hrm.service.payroll.trial.HrmPayrollTrialService;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import javax.validation.Validator;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** Scoped state counts and native tasks; financial amounts always belong to one saved version. */
@Service
public class HrmPayrollOverviewServiceImpl implements HrmPayrollOverviewService {
    @Resource private HrmPayrollTrialBatchMapper batches;
    @Resource private HrmPayrollReviewCycleMapper cycles;
    @Resource private HrmPayrollTrialService trial;
    @Resource private HrmPayrollEmployeeAccess access;
    @Resource private PermissionApi permissions;
    @Resource private TaskService tasks;
    @Resource private Validator validator;

    private Long tenant() {
        return TenantContextHolder.getRequiredTenantId();
    }

    private void guard() {
        for (String p :
                Arrays.asList(
                        "hrm:payroll:overview:query",
                        "hrm:payroll:trial:query",
                        "hrm:payroll:calculation:query",
                        "hrm:payroll:eligibility:query",
                        "hrm:employee:query"))
            if (!permissions.hasAnyPermissions(getLoginUserId(), p))
                throw exception(PAYROLL_OVERVIEW_PERMISSION);
    }

    private void valid(boolean ok, String message) {
        if (!ok) throw exception(PAYROLL_OVERVIEW_INVALID, message);
    }

    private LambdaQueryWrapperX<HrmPayrollTrialBatchDO> query(HrmPayrollOverviewPageReqVO r) {
        return access.filterTrialBatches(
                        new LambdaQueryWrapperX<HrmPayrollTrialBatchDO>()
                                .eq(HrmPayrollTrialBatchDO::getTenantId, tenant()))
                .eqIfPresent(
                        HrmPayrollTrialBatchDO::getEntityCode,
                        StrUtil.trimToNull(r.getEntityCode()))
                .likeIfPresent(HrmPayrollTrialBatchDO::getTitle, StrUtil.trimToNull(r.getSearch()))
                .eqIfPresent(HrmPayrollTrialBatchDO::getStatus, r.getStatus())
                .eqIfPresent(HrmPayrollTrialBatchDO::getPeriodStart, r.getPeriodStart())
                .eqIfPresent(HrmPayrollTrialBatchDO::getPeriodEnd, r.getPeriodEnd());
    }

    private List<HrmPayrollReviewCycleDO> cycleList(Collection<Long> ids, Collection<String> pids) {
        if ((ids != null && ids.isEmpty()) || (pids != null && pids.isEmpty()))
            return Collections.emptyList();
        LambdaQueryWrapperX<HrmPayrollReviewCycleDO> q =
                new LambdaQueryWrapperX<HrmPayrollReviewCycleDO>()
                        .eq(HrmPayrollReviewCycleDO::getTenantId, tenant())
                        .eq(HrmPayrollReviewCycleDO::getStatus, 0);
        q.select(
                HrmPayrollReviewCycleDO::getId,
                HrmPayrollReviewCycleDO::getBatchId,
                HrmPayrollReviewCycleDO::getRunId,
                HrmPayrollReviewCycleDO::getProcessInstanceId);
        if (ids != null) q.in(HrmPayrollReviewCycleDO::getId, ids);
        if (pids != null) q.in(HrmPayrollReviewCycleDO::getProcessInstanceId, pids);
        return cycles.selectList(q);
    }

    private boolean bound(HrmPayrollReviewCycleDO c, HrmPayrollOverviewRespVO.Row b) {
        return c != null
                && Objects.equals(b.getStatus(), 2)
                && Objects.equals(c.getId(), b.getActiveReviewId())
                && Objects.equals(c.getBatchId(), b.getId())
                && Objects.equals(c.getRunId(), b.getCurrentRunId());
    }

    private List<Task> taskList(Collection<String> pids, boolean mine) {
        if (pids != null && pids.isEmpty()) return Collections.emptyList();
        org.flowable.task.api.TaskQuery q =
                tasks.createTaskQuery()
                        .taskTenantId(String.valueOf(tenant()))
                        .processDefinitionKey(PayrollBpmContext.KEY)
                        .active();
        if (mine) q.taskAssignee(String.valueOf(getLoginUserId()));
        if (pids != null) q.processInstanceIdIn(pids);
        return q.list().stream()
                .filter(t -> String.valueOf(tenant()).equals(t.getTenantId()))
                .filter(t -> !mine || String.valueOf(getLoginUserId()).equals(t.getAssignee()))
                .filter(
                        t ->
                                "hrReview".equals(t.getTaskDefinitionKey())
                                        || "financeReview".equals(t.getTaskDefinitionKey()))
                .collect(Collectors.toList());
    }

    private void stages(List<HrmPayrollOverviewRespVO.Row> rows) {
        List<Long> ids =
                rows.stream()
                        .filter(b -> Objects.equals(b.getStatus(), 2))
                        .map(HrmPayrollOverviewRespVO.Row::getActiveReviewId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
        Map<Long, HrmPayrollReviewCycleDO> cycleMap =
                cycleList(ids, null).stream()
                        .collect(Collectors.toMap(HrmPayrollReviewCycleDO::getId, c -> c));
        List<String> pids =
                rows.stream()
                        .filter(b -> bound(cycleMap.get(b.getActiveReviewId()), b))
                        .map(b -> cycleMap.get(b.getActiveReviewId()).getProcessInstanceId())
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
        Map<String, List<Task>> byPid =
                taskList(pids, false).stream()
                        .collect(Collectors.groupingBy(Task::getProcessInstanceId));
        for (HrmPayrollOverviewRespVO.Row b : rows) {
            String[] labels = {
                "CHECK_INPUTS", "SUBMIT_REVIEW", "CHECK_BPM", "CHECK_FREEZE", "FROZEN"
            };
            b.setStage(
                    b.getStatus() != null && b.getStatus() >= 0 && b.getStatus() < labels.length
                            ? labels[b.getStatus()]
                            : "CHECK_BPM");
            HrmPayrollReviewCycleDO c = cycleMap.get(b.getActiveReviewId());
            if (!bound(c, b)) continue;
            List<Task> active =
                    byPid.getOrDefault(c.getProcessInstanceId(), Collections.emptyList());
            if (active.size() != 1) continue;
            Task t = active.get(0);
            b.setStage("hrReview".equals(t.getTaskDefinitionKey()) ? "HR_REVIEW" : "FINANCE_REVIEW")
                    .setAssignedToMe(String.valueOf(getLoginUserId()).equals(t.getAssignee()));
        }
    }

    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public HrmPayrollOverviewRespVO page(HrmPayrollOverviewPageReqVO r) {
        guard();
        valid(r != null, "查询不能为空");
        valid(validator.validate(r).isEmpty(), "查询字段或分页参数无效");
        valid((r.getPeriodStart() == null) == (r.getPeriodEnd() == null), "须同时选择期间起止日");
        valid(
                r.getPeriodStart() == null || !r.getPeriodStart().isAfter(r.getPeriodEnd()),
                "期间起日不能晚于止日");
        HrmPayrollOverviewRespVO out =
                new HrmPayrollOverviewRespVO()
                        .setGeneratedAt(LocalDateTime.now())
                        .setTotal(batches.selectCount(query(r)));
        for (int s = 0; s <= 4; s++)
            out.getStates()
                    .put(s, batches.selectCount(query(r).eq(HrmPayrollTrialBatchDO::getStatus, s)));
        PageResult<HrmPayrollTrialBatchDO> page =
                batches.selectPage(
                        r,
                        query(r).select(
                                        HrmPayrollTrialBatchDO.class,
                                        f ->
                                                !Arrays.asList("configurationJson", "reference")
                                                        .contains(f.getProperty()))
                                .orderByDesc(HrmPayrollTrialBatchDO::getId));
        List<HrmPayrollOverviewRespVO.Row> rows =
                BeanUtils.toBean(page.getList(), HrmPayrollOverviewRespVO.Row.class);
        stages(rows);
        out.setBatches(new PageResult<>(rows, page.getTotal()));
        List<Task> mine = taskList(null, true);
        List<HrmPayrollReviewCycleDO> mineCycles =
                cycleList(
                        null,
                        mine.stream()
                                .map(Task::getProcessInstanceId)
                                .distinct()
                                .collect(Collectors.toList()));
        Map<Long, HrmPayrollReviewCycleDO> byId =
                mineCycles.stream()
                        .collect(Collectors.toMap(HrmPayrollReviewCycleDO::getId, c -> c));
        Set<String> visiblePids = new HashSet<>();
        if (!byId.isEmpty())
            for (HrmPayrollTrialBatchDO b :
                    batches.selectList(
                            query(r).eq(HrmPayrollTrialBatchDO::getStatus, 2)
                                    .in(HrmPayrollTrialBatchDO::getActiveReviewId, byId.keySet())
                                    .select(
                                            HrmPayrollTrialBatchDO::getId,
                                            HrmPayrollTrialBatchDO::getStatus,
                                            HrmPayrollTrialBatchDO::getCurrentRunId,
                                            HrmPayrollTrialBatchDO::getActiveReviewId))) {
                HrmPayrollReviewCycleDO c = byId.get(b.getActiveReviewId());
                if (bound(c, BeanUtils.toBean(b, HrmPayrollOverviewRespVO.Row.class)))
                    visiblePids.add(c.getProcessInstanceId());
            }
        out.setAssignedHr(
                mine.stream()
                        .filter(
                                t ->
                                        visiblePids.contains(t.getProcessInstanceId())
                                                && "hrReview".equals(t.getTaskDefinitionKey()))
                        .map(Task::getProcessInstanceId)
                        .distinct()
                        .count());
        out.setAssignedFinance(
                mine.stream()
                        .filter(
                                t ->
                                        visiblePids.contains(t.getProcessInstanceId())
                                                && "financeReview".equals(t.getTaskDefinitionKey()))
                        .map(Task::getProcessInstanceId)
                        .distinct()
                        .count());
        return out;
    }

    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public HrmPayrollOverviewRespVO.Detail detail(Long batchId) {
        guard();
        HrmPayrollTrialInspectionVO view = trial.inspect(batchId);
        HrmPayrollOverviewRespVO.Row row =
                BeanUtils.toBean(view.getBatch(), HrmPayrollOverviewRespVO.Row.class);
        stages(Collections.singletonList(row));
        HrmPayrollOverviewRespVO.Detail out =
                new HrmPayrollOverviewRespVO.Detail()
                        .setBatch(row)
                        .setCheck(view.getCheck())
                        .setAvailability(view.getAvailability());
        if (view.getRun() != null) {
            HrmPayrollTrialRunRespVO r = view.getRun();
            out.setRunId(r.getId())
                    .setRunVersion(r.getRunVersion())
                    .setExecutedByName(r.getExecutedByName())
                    .setExecutedAt(r.getExecutedAt())
                    .setIncludedCount(r.getIncludedCount())
                    .setExcludedCount(r.getExcludedCount())
                    .setAmounts(new LinkedHashMap<>(r.getResult().getTotals()));
        }
        return out;
    }
}
