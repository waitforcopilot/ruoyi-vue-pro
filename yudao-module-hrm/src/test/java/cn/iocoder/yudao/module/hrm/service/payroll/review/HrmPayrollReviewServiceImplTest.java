package cn.iocoder.yudao.module.hrm.service.payroll.review;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.calculation.*;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.eligibility.*;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.review.*;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.trial.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.review.HrmPayrollReviewCycleDO;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.review.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.trial.*;
import cn.iocoder.yudao.module.hrm.service.payroll.calculation.*;
import cn.iocoder.yudao.module.hrm.service.payroll.eligibility.*;
import cn.iocoder.yudao.module.hrm.service.payroll.identity.*;
import cn.iocoder.yudao.module.hrm.service.payroll.trial.*;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import javax.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@Import({
    HrmPayrollReviewServiceImpl.class,
    HrmPayrollTrialServiceImpl.class,
    HrmPayrollCalculationServiceImpl.class,
    HrmPayrollCalculationEngine.class,
    HrmPayrollEligibilityServiceImpl.class,
    HrmPayrollEmployeeMappingServiceImpl.class,
    HrmPayrollEmployeeAccess.class,
    ValidationAutoConfiguration.class
})
class HrmPayrollReviewServiceImplTest extends BaseDbUnitTest {
    @Resource private HrmPayrollReviewService service;
    @Resource private HrmPayrollTrialService trial;
    @Resource private HrmPayrollCalculationService calculation;
    @Resource private HrmPayrollEligibilityService eligibility;
    @Resource private HrmPayrollReviewCycleMapper cycles;
    @Resource private HrmPayrollReviewCommandMapper commands;
    @Resource private HrmPayrollTrialRunMapper runs;
    @Resource private HrmPayrollTrialBatchMapper batches;
    @Resource private javax.sql.DataSource dataSource;
    @MockBean private HrmPayrollReviewBpmGateway bpm;
    @MockBean private PermissionApi permissions;
    @MockBean private AdminUserApi users;
    private JdbcTemplate jdbc;
    private Long batchId, runId;
    private Map<String, Integer> nativeStatus;
    private final LocalDate start = LocalDate.of(2026, 10, 1), end = LocalDate.of(2026, 10, 31);

    @BeforeEach
    void fixture() {
        jdbc = new JdbcTemplate(dataSource);
        nativeStatus = new ConcurrentHashMap<>();
        context(10L, 1L);
        for (Long id : Arrays.asList(10L, 11L, 12L, 13L)) {
            when(users.getUser(id))
                    .thenReturn(
                            new AdminUserRespDTO()
                                    .setId(id)
                                    .setStatus(0)
                                    .setNickname("合成角色 #" + id));
            when(permissions.getDeptDataPermission(id))
                    .thenReturn(new DeptDataPermissionRespDTO().setAll(true));
            for (String p :
                    Arrays.asList(
                            "hrm:employee:query",
                            "hrm:payroll:trial:query",
                            "hrm:payroll:calculation:query",
                            "hrm:payroll:eligibility:query"))
                when(permissions.hasAnyPermissions(id, p)).thenReturn(true);
        }
        for (String p : Arrays.asList("maintain", "execute", "review-submit"))
            when(permissions.hasAnyPermissions(10L, "hrm:payroll:trial:" + p)).thenReturn(true);
        when(permissions.hasAnyPermissions(11L, "hrm:payroll:trial:hr-review")).thenReturn(true);
        for (String p : Arrays.asList("finance-review", "freeze", "unfreeze"))
            when(permissions.hasAnyPermissions(12L, "hrm:payroll:trial:" + p)).thenReturn(true);
        when(bpm.definition()).thenReturn(PayrollBpmContext.KEY + ":1:qa");
        when(bpm.start(any()))
                .thenAnswer(
                        a -> {
                            HrmPayrollReviewCycleDO c = a.getArgument(0);
                            String pid = "pid-" + c.getId();
                            nativeStatus.put(pid, 1);
                            return pid;
                        });
        when(bpm.tasks(anyString()))
                .thenAnswer(
                        a -> {
                            String pid = a.getArgument(0);
                            HrmPayrollReviewCycleDO c = byPid(pid);
                            if (c == null || nativeStatus.get(pid) != 1)
                                return Collections.emptyList();
                            boolean hr = c.getHrReviewedAt() == null;
                            return Collections.singletonList(
                                    new HrmPayrollReviewRespVO.Task()
                                            .setId(pid + (hr ? "-hr" : "-finance"))
                                            .setKey(hr ? "hrReview" : "financeReview")
                                            .setName(hr ? "HR 复核" : "财务复核")
                                            .setAssigneeId(
                                                    hr
                                                            ? c.getHrReviewerId()
                                                            : c.getFinanceReviewerId()));
                        });
        when(bpm.actualStatus(anyString(), anyString()))
                .thenAnswer(a -> nativeStatus.get((String) a.getArgument(0)));
        doAnswer(
                        a -> {
                            String pid = a.getArgument(0), action = a.getArgument(3);
                            HrmPayrollReviewCycleDO c = byPid(pid);
                            if ("reject".equals(action)
                                    || ((String) a.getArgument(1)).endsWith("-finance")) {
                                int status = "reject".equals(action) ? 3 : 2;
                                nativeStatus.put(pid, status);
                                service.processEvent(event(c, status));
                            }
                            return null;
                        })
                .when(bpm)
                .decide(anyString(), anyString(), anyLong(), anyString());
        doAnswer(
                        a -> {
                            String pid = a.getArgument(0);
                            nativeStatus.put(pid, 4);
                            service.processEvent(event(byPid(pid), 4));
                            return null;
                        })
                .when(bpm)
                .cancel(anyString(), anyLong(), anyBoolean());
        jdbc.update(
                "INSERT INTO hrm_employee(id,name,job_number,dept_id,user_id,status,tenant_id) VALUES(101,'复核合成人员','001',11,10,20,1)");
        Long definition =
                calculation.create(
                        new HrmPayrollCalculationSaveReqVO()
                                .setCode("QA-REVIEW")
                                .setTitle("合成复核工资")
                                .setScopeCode("QA")
                                .setApplicableScope("合成范围")
                                .setOwnerName("QA")
                                .setReference("合成依据")
                                .setEffectiveFrom(start)
                                .setEffectiveTo(end)
                                .setProgram(HrmPayrollWageTemplate.program()));
        calculation.review(
                new HrmPayrollCalculationReviewReqVO()
                        .setId(definition)
                        .setRevision(1)
                        .setAction("confirm")
                        .setEvidence("合成核对"));
        Long qual =
                eligibility.create(
                        new HrmPayrollEligibilitySaveReqVO()
                                .setEntityCode("QA")
                                .setEntityName("合成主体")
                                .setEmployeeId(101L)
                                .setQualification("INCLUDED")
                                .setOwnerName("QA")
                                .setReference("合成依据")
                                .setReason("合成纳入")
                                .setEffectiveFrom(start)
                                .setEffectiveTo(end));
        eligibility.review(
                new HrmPayrollEligibilityReviewReqVO()
                        .setId(qual)
                        .setRevision(1)
                        .setAction("confirm")
                        .setEvidence("合成核对"));
        batchId =
                trial.create(
                        new HrmPayrollTrialSaveReqVO()
                                .setCode("QA-BATCH")
                                .setTitle("合成复核批次")
                                .setEntityCode("QA")
                                .setEntityName("合成主体")
                                .setPeriodStart(start)
                                .setPeriodEnd(end)
                                .setDefinitionId(definition)
                                .setOwnerName("QA")
                                .setReference("合成口径")
                                .setConfiguration(
                                        new HrmPayrollTrialConfigVO()
                                                .setPeople(
                                                        Collections.singletonList(
                                                                new HrmPayrollTrialConfigVO
                                                                                .PersonInput()
                                                                        .setEmployeeId(101L)
                                                                        .setInputReference("合成输入依据")
                                                                        .setInputs(
                                                                                HrmPayrollWageTemplate
                                                                                        .program()
                                                                                        .getCases()
                                                                                        .get(0)
                                                                                        .getInputs())))));
        HrmPayrollTrialCheckVO check = trial.check(batchId);
        runId =
                trial.execute(
                                new HrmPayrollTrialExecuteReqVO()
                                        .setBatchId(batchId)
                                        .setRevision(check.getRevision())
                                        .setSourceHash(check.getSourceHash())
                                        .setRequestKey("initial-trial"))
                        .getId();
    }

    private void context(Long user, Long tenant) {
        TenantContextHolder.setTenantId(tenant);
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                new LoginUser().setId(user).setTenantId(tenant).setUserType(2),
                                null,
                                Collections.emptyList()));
    }

    @AfterEach
    void clear() {
        TenantContextHolder.clear();
        SecurityContextHolder.clearContext();
    }

    private HrmPayrollReviewCycleDO byPid(String pid) {
        return cycles.selectOne(
                new cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX<
                                HrmPayrollReviewCycleDO>()
                        .eq(HrmPayrollReviewCycleDO::getTenantId, 1L)
                        .eq(HrmPayrollReviewCycleDO::getProcessInstanceId, pid));
    }

    private BpmProcessInstanceStatusEvent event(HrmPayrollReviewCycleDO c, int status) {
        BpmProcessInstanceStatusEvent e = new BpmProcessInstanceStatusEvent(this);
        e.setId(c.getProcessInstanceId());
        e.setBusinessKey(String.valueOf(c.getId()));
        e.setProcessDefinitionKey(PayrollBpmContext.KEY);
        e.setStatus(status);
        return e;
    }

    private HrmPayrollReviewActionReqVO request(String action) {
        HrmPayrollReviewRespVO view = service.get(batchId);
        return new HrmPayrollReviewActionReqVO()
                .setAction(action)
                .setBatchId(batchId)
                .setRunId(runId)
                .setRevision(view.getRevision())
                .setCycleId(view.getActiveReviewId())
                .setRequestKey("cmd-" + UUID.randomUUID())
                .setEvidence("合成" + action + "证据");
    }

    private HrmPayrollReviewRespVO submit() {
        return service.action(request("submit").setHrReviewerId(11L).setFinanceReviewerId(12L));
    }

    private HrmPayrollReviewRespVO decision(Long actor, String action) {
        context(actor, 1L);
        HrmPayrollReviewRespVO view = service.get(batchId);
        return service.action(request(action).setTaskId(view.getTasks().get(0).getId()));
    }

    private HrmPayrollReviewRespVO approve() {
        submit();
        decision(11L, "approve");
        return decision(12L, "approve");
    }

    private HrmPayrollReviewRespVO freeze() {
        approve();
        return service.action(request("freeze"));
    }

    private void invalid(Runnable work) {
        assertEquals(
                PAYROLL_REVIEW_INVALID.getCode(),
                assertThrows(ServiceException.class, work::run).getCode());
    }

    @Test
    void sourceManifestSurvivesStoredIsoTimestampsAndWorkflowOnlyRevisionChanges() {
        HrmPayrollTrialRunRespVO saved = trial.run(runId);
        assertEquals(
                java.time.LocalDate.now(),
                saved.getResult()
                        .getPeople()
                        .get(0)
                        .getEligibility()
                        .getReviewedTime()
                        .toLocalDate());
        assertEquals(
                JsonUtils.toJsonString(saved),
                JsonUtils.toJsonString(
                        JsonUtils.parseObject(
                                JsonUtils.toJsonString(saved), HrmPayrollTrialRunRespVO.class)));
        String manifest = trial.validateCurrentRun(batchId, runId);
        submit();
        assertEquals(manifest, trial.validateCurrentRun(batchId, runId));
    }

    @Test
    void submissionPinsRunSourceDefinitionReviewersAndBpmIdentity() {
        HrmPayrollReviewRespVO v = submit();
        assertEquals(2, v.getBatchStatus());
        assertEquals(runId, v.getCycle().getRunId());
        assertEquals(11L, v.getCycle().getHrReviewerId());
        assertEquals(12L, v.getCycle().getFinanceReviewerId());
        assertEquals(PayrollBpmContext.KEY + ":1:qa", v.getCycle().getProcessDefinitionId());
        assertNotNull(v.getCycle().getSourceHash());
        assertEquals("hrReview", v.getTasks().get(0).getKey());
        assertEquals(1, commands.selectCount());
    }

    @Test
    void twoStepReviewMustCompleteBeforeFreezingAndAmountsStayImmutable() {
        String result = JsonUtils.toJsonString(trial.run(runId).getResult());
        HrmPayrollReviewRespVO v = submit();
        context(12L, 1L);
        invalid(() -> service.action(request("freeze")));
        decision(11L, "approve");
        assertEquals(2, service.get(batchId).getBatchStatus());
        v = decision(12L, "approve");
        assertEquals(3, v.getBatchStatus());
        assertEquals("APPROVED", v.getCycle().getOutcome());
        assertEquals(1, JsonUtils.parseObject(trial.history(batchId).stream().filter(a -> "approve".equals(a.getAction()) && Long.valueOf(12L).equals(a.getActorId())).findFirst().get().getAfterSnapshot(), HrmPayrollReviewCycleDO.class).getStatus());
        assertNotNull(v.getCycle().getHrReviewedAt());
        assertNotNull(v.getCycle().getFinanceReviewedAt());
        v = service.action(request("freeze"));
        assertEquals(4, v.getBatchStatus());
        assertEquals(runId, v.getFrozenRunId());
        assertEquals(result, JsonUtils.toJsonString(trial.run(runId).getResult()));
        assertEquals(1, runs.selectCount());
    }

    @Test
    void activeReviewApprovedAndFrozenBatchesBlockEditingAndRecalculation() {
        submit();
        blockedEdits();
        decision(11L, "approve");
        decision(12L, "approve");
        blockedEdits();
        service.action(request("freeze"));
        blockedEdits();
    }

    private void blockedEdits() {
        HrmPayrollTrialSaveReqVO edit =
                BeanUtils.toBean(trial.get(batchId), HrmPayrollTrialSaveReqVO.class)
                        .setConfiguration(
                                JsonUtils.parseObject(
                                        JsonUtils.toJsonString(
                                                trial.get(batchId).getConfiguration()),
                                        HrmPayrollTrialConfigVO.class));
        when(permissions.hasAnyPermissions(getUser(), "hrm:payroll:trial:maintain"))
                .thenReturn(true);
        when(permissions.hasAnyPermissions(getUser(), "hrm:payroll:trial:execute"))
                .thenReturn(true);
        assertEquals(
                PAYROLL_TRIAL_INVALID.getCode(),
                assertThrows(ServiceException.class, () -> trial.update(edit)).getCode());
        HrmPayrollTrialCheckVO c = trial.check(batchId);
        assertEquals(
                PAYROLL_TRIAL_INVALID.getCode(),
                assertThrows(
                                ServiceException.class,
                                () ->
                                        trial.execute(
                                                new HrmPayrollTrialExecuteReqVO()
                                                        .setBatchId(batchId)
                                                        .setRevision(c.getRevision())
                                                        .setSourceHash(c.getSourceHash())
                                                        .setRequestKey(
                                                                "blocked-" + UUID.randomUUID())))
                        .getCode());
    }

    private Long getUser() {
        return cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils
                .getLoginUserId();
    }

    @Test
    void hrAndFinanceReviewersMustDifferFromEachOtherMakerAndStarter() {
        for (Long id : Arrays.asList(10L, 11L)) {
            HrmPayrollReviewActionReqVO r =
                    request("submit").setHrReviewerId(11L).setFinanceReviewerId(id);
            invalid(() -> service.action(r));
        }
        assertEquals(0, cycles.selectCount());
        verify(bpm, never()).start(any());
    }

    @Test
    void missingInactiveCrossTenantOrUnprivilegedReviewersCannotStart() {
        invalid(
                () ->
                        service.action(
                                request("submit").setHrReviewerId(11L).setFinanceReviewerId(999L)));
        when(users.getUser(12L)).thenReturn(new AdminUserRespDTO().setId(12L).setStatus(1));
        invalid(() -> submit());
        when(users.getUser(12L)).thenReturn(new AdminUserRespDTO().setId(12L).setStatus(0));
        when(permissions.hasAnyPermissions(12L, "hrm:payroll:eligibility:query")).thenReturn(false);
        assertServiceException(this::submit, PAYROLL_REVIEW_PERMISSION);
    }

    @Test
    void reviewersNeedVisibilityOverEveryHistoricalFootprint() {
        when(permissions.getDeptDataPermission(11L))
                .thenReturn(
                        new DeptDataPermissionRespDTO()
                                .setDeptIds(new HashSet<>(Collections.singletonList(22L))));
        assertServiceException(this::submit, PAYROLL_REVIEW_PERMISSION);
    }

    @Test
    void selfScopeMustUseReviewersUserIdRatherThanTheStartersId() {
        when(permissions.getDeptDataPermission(11L))
                .thenReturn(new DeptDataPermissionRespDTO().setSelf(true));
        assertServiceException(this::submit, PAYROLL_REVIEW_PERMISSION);
        jdbc.update("UPDATE hrm_employee SET user_id=11 WHERE id=101");
        assertServiceException(this::submit, PAYROLL_REVIEW_PERMISSION);
    }

    @Test
    void missingEvidenceOrUnknownActionCreatesNoWorkflow() {
        invalid(
                () ->
                        service.action(
                                request("submit")
                                        .setHrReviewerId(11L)
                                        .setFinanceReviewerId(12L)
                                        .setEvidence(" ")));
        invalid(() -> service.action(request("publish")));
        assertEquals(0, cycles.selectCount());
    }

    @Test
    void unsupportedOrFailedBpmDeploymentRollsBackCycleAndBatch() {
        when(bpm.definition())
                .thenThrow(
                        cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil
                                .exception(PAYROLL_REVIEW_BPM_UNSUPPORTED));
        assertServiceException(this::submit, PAYROLL_REVIEW_BPM_UNSUPPORTED);
        assertEquals(0, cycles.selectCount());
        assertEquals(1, trial.get(batchId).getStatus());
    }

    @Test
    void engineStartFailureRollsBackBusinessWritesAndCommandReceipt() {
        doThrow(new IllegalStateException("synthetic engine failure")).when(bpm).start(any());
        assertThrows(IllegalStateException.class, this::submit);
        assertEquals(0, cycles.selectCount());
        assertEquals(0, commands.selectCount());
        assertEquals(1, trial.get(batchId).getStatus());
    }

    @Test
    void approvalOrderAssigneeAndCurrentTaskIdCannotBeForged() {
        HrmPayrollReviewRespVO v = submit();
        context(12L, 1L);
        assertServiceException(
                () -> service.action(request("approve").setTaskId(v.getTasks().get(0).getId())),
                PAYROLL_REVIEW_PERMISSION);
        context(11L, 1L);
        assertServiceException(
                () -> service.action(request("approve").setTaskId("forged-task")),
                PAYROLL_REVIEW_STALE);
        assertNull(service.get(batchId).getCycle().getHrReviewedAt());
    }

    @Test
    void changedPersonnelOrRuleSourceBlocksApprovals() {
        submit();
        jdbc.update("UPDATE hrm_employee SET status=30 WHERE id=101");
        assertServiceException(() -> decision(11L, "approve"), PAYROLL_REVIEW_SOURCE_CHANGED);
        assertNull(service.get(batchId).getCycle().getHrReviewedAt());
    }

    @Test
    void lossOfRoleOrDepartmentScopeAfterSubmissionBlocksApproval() {
        submit();
        when(permissions.hasAnyPermissions(11L, "hrm:payroll:trial:hr-review")).thenReturn(false);
        assertServiceException(() -> decision(11L, "approve"), PAYROLL_REVIEW_PERMISSION);
        when(permissions.hasAnyPermissions(11L, "hrm:payroll:trial:hr-review")).thenReturn(true);
        when(permissions.getDeptDataPermission(11L)).thenReturn(new DeptDataPermissionRespDTO());
        assertServiceException(() -> decision(11L, "approve"), PAYROLL_TRIAL_NOT_EXISTS);
    }

    @Test
    void rejectionInvalidatesCurrentRunRetainsReviewAndRequiresNewTrial() {
        submit();
        HrmPayrollReviewRespVO v = decision(11L, "reject");
        assertEquals(0, v.getBatchStatus());
        assertEquals("REJECTED", v.getCycle().getOutcome());
        assertNull(trial.get(batchId).getCurrentRunId());
        assertEquals(runId, trial.get(batchId).getLatestRunId());
        assertNotNull(trial.run(runId).getResult());
        context(10L, 1L);
        assertServiceException(
                () ->
                        service.action(
                                request("submit").setHrReviewerId(11L).setFinanceReviewerId(12L)),
                PAYROLL_REVIEW_STALE);
    }

    @Test
    void onlyStarterCanCancelAndCancellationRetainsCompletedHrEvidence() {
        submit();
        decision(11L, "approve");
        assertServiceException(() -> service.action(request("cancel")), PAYROLL_REVIEW_PERMISSION);
        context(10L, 1L);
        HrmPayrollReviewRespVO v = service.action(request("cancel"));
        assertEquals(0, v.getBatchStatus());
        assertEquals("CANCELLED", v.getCycle().getOutcome());
        assertNotNull(v.getCycle().getHrEvidence());
        assertEquals(1, runs.selectCount());
    }

    @Test
    void unfreezePreservesResultAndRequiresFreshReviewCycle() {
        HrmPayrollReviewRespVO frozen = freeze();
        HrmPayrollReviewRespVO v = service.action(request("unfreeze"));
        assertEquals(1, v.getBatchStatus());
        assertNull(v.getActiveReviewId());
        assertNull(v.getFrozenRunId());
        assertEquals(5, v.getCycles().get(0).getStatus());
        assertNotNull(v.getCycles().get(0).getFreezeEvidence());
        assertNotNull(v.getCycles().get(0).getUnfreezeEvidence());
        assertServiceException(
                () -> service.action(request("freeze").setCycleId(frozen.getActiveReviewId())),
                PAYROLL_REVIEW_STALE);
        context(10L, 1L);
        HrmPayrollReviewRespVO next = submit();
        assertEquals(2, next.getCycle().getCycleVersion());
        assertNull(next.getCycle().getHrEvidence());
        assertEquals(1, runs.selectCount());
    }

    @Test
    void sourceDriftAfterReviewBlocksFreezeWithoutErasingApprovals() {
        approve();
        jdbc.update("UPDATE hrm_employee SET status=30 WHERE id=101");
        assertServiceException(
                () -> service.action(request("freeze")), PAYROLL_REVIEW_SOURCE_CHANGED);
        assertEquals(3, service.get(batchId).getBatchStatus());
        assertNotNull(service.get(batchId).getCycle().getFinanceEvidence());
    }

    @Test
    void makerCannotFreezeOwnReviewedResultEvenWithFreezePermission() {
        approve();
        context(10L, 1L);
        when(permissions.hasAnyPermissions(10L, "hrm:payroll:trial:freeze")).thenReturn(true);
        assertServiceException(() -> service.action(request("freeze")), PAYROLL_REVIEW_PERMISSION);
    }

    @Test
    void idempotentSubmissionAndApprovalRetryReturnOriginalReceipt() {
        HrmPayrollReviewActionReqVO cmd =
                request("submit").setHrReviewerId(11L).setFinanceReviewerId(12L);
        HrmPayrollReviewRespVO v = service.action(cmd);
        assertEquals(JsonUtils.toJsonString(v), JsonUtils.toJsonString(service.action(cmd)));
        assertEquals(1, cycles.selectCount());
        verify(bpm, times(1)).start(any());
        context(11L, 1L);
        HrmPayrollReviewActionReqVO decision =
                request("approve").setTaskId(v.getTasks().get(0).getId());
        HrmPayrollReviewRespVO approved = service.action(decision);
        assertEquals(
                JsonUtils.toJsonString(approved), JsonUtils.toJsonString(service.action(decision)));
        verify(bpm, times(1)).decide(anyString(), anyString(), anyLong(), eq("approve"));
    }

    @Test
    void reusedKeyWithChangedEvidenceOrActorIsRejected() {
        HrmPayrollReviewActionReqVO cmd =
                request("submit").setHrReviewerId(11L).setFinanceReviewerId(12L);
        service.action(cmd);
        cmd.setEvidence("改变证据");
        assertServiceException(() -> service.action(cmd), PAYROLL_REVIEW_CONFLICT);
    }

    @Test
    void staleRevisionCycleAndRunCannotOperateOnNewPhase() {
        HrmPayrollReviewActionReqVO stale =
                request("submit").setHrReviewerId(11L).setFinanceReviewerId(12L);
        submit();
        assertServiceException(() -> service.action(stale), PAYROLL_REVIEW_STALE);
        context(11L, 1L);
        assertServiceException(
                () -> service.action(request("approve").setCycleId(999L).setTaskId("missing")),
                PAYROLL_REVIEW_STALE);
    }

    @Test
    void callbackNeedsMatchingActualEngineStatusCycleRunAndInstance() {
        HrmPayrollReviewRespVO v = submit();
        HrmPayrollReviewCycleDO c = cycles.selectById(v.getActiveReviewId());
        BpmProcessInstanceStatusEvent e = event(c, 2);
        service.processEvent(e);
        assertEquals(2, trial.get(batchId).getStatus());
        nativeStatus.put(c.getProcessInstanceId(), 2);
        service.processEvent(e);
        assertEquals(2, trial.get(batchId).getStatus());
        e.setId("other-instance");
        service.processEvent(e);
        assertEquals(2, trial.get(batchId).getStatus());
    }

    @Test
    void duplicateApprovedEventDoesNotIncreaseRevisionOrAuditAgain() {
        HrmPayrollReviewRespVO v = approve();
        HrmPayrollReviewCycleDO c = cycles.selectById(v.getActiveReviewId());
        int before = trial.history(batchId).size();
        service.processEvent(event(c, 2));
        assertEquals(v.getRevision(), trial.get(batchId).getRevision());
        assertEquals(before, trial.history(batchId).size());
    }

    @Test
    void oldCancelledCycleCannotApproveNewCycleOrFrozenVersion() {
        HrmPayrollReviewRespVO old = submit();
        service.action(request("cancel"));
        HrmPayrollTrialCheckVO q = trial.check(batchId);
        runId =
                trial.execute(
                                new HrmPayrollTrialExecuteReqVO()
                                        .setBatchId(batchId)
                                        .setRevision(q.getRevision())
                                        .setSourceHash(q.getSourceHash())
                                        .setRequestKey("second-trial"))
                        .getId();
        HrmPayrollReviewRespVO next = submit();
        HrmPayrollReviewCycleDO prior = cycles.selectById(old.getActiveReviewId());
        nativeStatus.put(prior.getProcessInstanceId(), 2);
        service.processEvent(event(prior, 2));
        assertEquals(next.getActiveReviewId(), trial.get(batchId).getActiveReviewId());
        assertEquals(2, trial.get(batchId).getStatus());
    }

    @Test
    void crossTenantViewsCommandsAndCallbacksCannotChangeTenantABatch() {
        HrmPayrollReviewRespVO v = submit();
        HrmPayrollReviewCycleDO c = cycles.selectById(v.getActiveReviewId());
        context(10L, 999L);
        assertServiceException(() -> service.get(batchId), PAYROLL_TRIAL_NOT_EXISTS);
        service.processEvent(event(c, 3));
        context(10L, 1L);
        assertEquals(2, trial.get(batchId).getStatus());
    }

    @Test
    void departmentReadCannotAccessOtherBatchOrHistory() {
        submit();
        when(permissions.getDeptDataPermission(13L))
                .thenReturn(
                        new DeptDataPermissionRespDTO()
                                .setDeptIds(new HashSet<>(Collections.singletonList(22L))));
        context(13L, 1L);
        assertServiceException(() -> service.get(batchId), PAYROLL_TRIAL_NOT_EXISTS);
    }

    @Test
    void freezeAndUnfreezePermissionsAreSeparateFromReview() {
        approve();
        when(permissions.hasAnyPermissions(12L, "hrm:payroll:trial:freeze")).thenReturn(false);
        assertServiceException(() -> service.action(request("freeze")), PAYROLL_REVIEW_PERMISSION);
        when(permissions.hasAnyPermissions(12L, "hrm:payroll:trial:freeze")).thenReturn(true);
        service.action(request("freeze"));
        when(permissions.hasAnyPermissions(12L, "hrm:payroll:trial:unfreeze")).thenReturn(false);
        assertServiceException(
                () -> service.action(request("unfreeze")), PAYROLL_REVIEW_PERMISSION);
    }

    @Test
    void managementCancellationNeedsSeparateRoleAndRetainsReason() {
        submit();
        context(12L, 1L);
        assertServiceException(
                () -> service.action(request("admin-cancel")), PAYROLL_REVIEW_PERMISSION);
        when(permissions.hasAnyPermissions(12L, "hrm:payroll:trial:admin-cancel")).thenReturn(true);
        HrmPayrollReviewRespVO v = service.action(request("admin-cancel"));
        assertEquals(0, v.getBatchStatus());
        assertEquals("CANCELLED", v.getCycle().getOutcome());
        verify(bpm).cancel(v.getCycle().getProcessInstanceId(), 12L, true);
    }

    @Test
    void reconciliationUsesActualBpmStatusAndIsIdempotent() {
        HrmPayrollReviewRespVO v = submit();
        HrmPayrollReviewCycleDO c = cycles.selectById(v.getActiveReviewId());
        nativeStatus.put(c.getProcessInstanceId(), 4);
        v = service.sync(batchId);
        assertEquals(0, v.getBatchStatus());
        assertEquals("CANCELLED", v.getCycle().getOutcome());
        assertEquals(v.getRevision(), service.sync(batchId).getRevision());
    }

    @Test
    void reconciliationCannotApproveWithoutBothIndependentReviews() {
        HrmPayrollReviewRespVO v = submit();
        nativeStatus.put(v.getCycle().getProcessInstanceId(), 2);
        assertEquals(2, service.sync(batchId).getBatchStatus());
        assertNull(service.get(batchId).getCycle().getFinanceReviewedAt());
    }

    @Test
    void simultaneousSubmitRetryCreatesOneCycleAndOneBpmInstance() throws Exception {
        HrmPayrollReviewActionReqVO cmd =
                request("submit").setHrReviewerId(11L).setFinanceReviewerId(12L);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch go = new CountDownLatch(1);
        Callable<Long> task =
                () -> {
                    context(10L, 1L);
                    try {
                        go.await();
                        return service.action(cmd).getActiveReviewId();
                    } finally {
                        clear();
                    }
                };
        try {
            Future<Long> a = pool.submit(task), b = pool.submit(task);
            go.countDown();
            assertEquals(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS));
            assertEquals(1, cycles.selectCount());
            assertEquals(1, commands.selectCount());
            verify(bpm, times(1)).start(any());
        } finally {
            pool.shutdownNow();
        }
    }
}
