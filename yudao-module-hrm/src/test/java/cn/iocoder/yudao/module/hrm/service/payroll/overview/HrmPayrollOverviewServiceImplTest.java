package cn.iocoder.yudao.module.hrm.service.payroll.overview;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.calculation.*;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.eligibility.*;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.overview.*;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.trial.*;
import cn.iocoder.yudao.module.hrm.service.payroll.calculation.*;
import cn.iocoder.yudao.module.hrm.service.payroll.eligibility.*;
import cn.iocoder.yudao.module.hrm.service.payroll.identity.*;
import cn.iocoder.yudao.module.hrm.service.payroll.trial.*;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import javax.sql.DataSource;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@Import({
    HrmPayrollOverviewServiceImpl.class,
    cn.iocoder.yudao.module.hrm.service.payroll.trial.HrmPayrollTrialSchemeBinding.class,
    cn.iocoder.yudao.module.hrm.service.payroll.scheme.HrmPayrollSchemeServiceImpl.class,
    HrmPayrollTrialServiceImpl.class,
    HrmPayrollCalculationServiceImpl.class,
    HrmPayrollCalculationEngine.class,
    HrmPayrollEligibilityServiceImpl.class,
    HrmPayrollEmployeeMappingServiceImpl.class,
    HrmPayrollEmployeeAccess.class,
    ValidationAutoConfiguration.class
})
class HrmPayrollOverviewServiceImplTest extends BaseDbUnitTest {
    @Resource private HrmPayrollOverviewService service;
    @Resource private HrmPayrollTrialService trial;
    @Resource private HrmPayrollCalculationService calculation;
    @Resource private HrmPayrollEligibilityService eligibility;
    @Resource private DataSource dataSource;
    @MockBean private PermissionApi permissions;
    @MockBean private AdminUserApi users;
    @MockBean private TaskService tasks;
    private JdbcTemplate jdbc;
    private Long definition;
    private final List<Task> nativeTasks = new ArrayList<>();
    private final LocalDate start = LocalDate.of(2026, 10, 1), end = LocalDate.of(2026, 10, 31);
    private final List<String> core =
            Arrays.asList(
                    "hrm:payroll:overview:query",
                    "hrm:payroll:trial:query",
                    "hrm:payroll:calculation:query",
                    "hrm:payroll:eligibility:query",
                    "hrm:employee:query");

    @BeforeEach
    void fixture() {
        jdbc = new JdbcTemplate(dataSource);
        context(1L, 10L);
        nativeTasks.clear();
        for (Long actor : Arrays.asList(10L, 30L, 40L)) {
            for (String p : core) when(permissions.hasAnyPermissions(actor, p)).thenReturn(true);
            for (String p :
                    Arrays.asList("hrm:payroll:trial:maintain", "hrm:payroll:trial:execute"))
                when(permissions.hasAnyPermissions(actor, p)).thenReturn(true);
            when(permissions.getDeptDataPermission(actor))
                    .thenReturn(new DeptDataPermissionRespDTO().setAll(true));
            when(users.getUser(actor))
                    .thenReturn(new AdminUserRespDTO().setId(actor).setNickname("合成人员" + actor));
        }
        when(tasks.createTaskQuery())
                .thenAnswer(
                        invocation -> {
                            TaskQuery q = mock(TaskQuery.class, RETURNS_SELF);
                            String[] assigned = {null};
                            Collection<?>[] selected = {null};
                            when(q.taskAssignee(anyString()))
                                    .thenAnswer(
                                            i -> {
                                                assigned[0] = i.getArgument(0);
                                                return q;
                                            });
                            when(q.processInstanceIdIn(anyCollection()))
                                    .thenAnswer(
                                            i -> {
                                                selected[0] = i.getArgument(0);
                                                return q;
                                            });
                            when(q.list())
                                    .thenAnswer(
                                            i ->
                                                    nativeTasks.stream()
                                                            .filter(
                                                                    t ->
                                                                            assigned[0] == null
                                                                                    || assigned[0]
                                                                                            .equals(
                                                                                                    t
                                                                                                            .getAssignee()))
                                                            .filter(
                                                                    t ->
                                                                            selected[0] == null
                                                                                    || selected[0]
                                                                                            .contains(
                                                                                                    t
                                                                                                            .getProcessInstanceId()))
                                                            .collect(Collectors.toList()));
                            return q;
                        });
        jdbc.update(
                "INSERT INTO hrm_employee(id,name,job_number,dept_id,user_id,status,tenant_id) VALUES(101,'合成 A','001',11,10,20,1),(102,'合成 B','002',22,20,30,1)");
        definition =
                calculation.create(
                        new HrmPayrollCalculationSaveReqVO()
                                .setCode("RULE")
                                .setTitle("合成工资")
                                .setScopeCode("QA")
                                .setApplicableScope("合成主体")
                                .setOwnerName("QA")
                                .setReference("合成口径")
                                .setEffectiveFrom(start)
                                .setEffectiveTo(end)
                                .setProgram(HrmPayrollWageTemplate.program()));
        calculation.review(
                new HrmPayrollCalculationReviewReqVO()
                        .setId(definition)
                        .setRevision(1)
                        .setAction("confirm")
                        .setEvidence("合成确认"));
        qualify(101L, "INCLUDED");
        qualify(102L, "EXCLUDED");
    }

    private void context(Long tenant, Long actor) {
        TenantContextHolder.setTenantId(tenant);
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                new LoginUser().setId(actor).setTenantId(tenant).setUserType(2),
                                null,
                                Collections.emptyList()));
    }

    @AfterEach
    void clear() {
        TenantContextHolder.clear();
        SecurityContextHolder.clearContext();
    }

    private void qualify(Long employee, String decision) {
        Long id =
                eligibility.create(
                        new HrmPayrollEligibilitySaveReqVO()
                                .setEntityCode("QA")
                                .setEntityName("合成主体")
                                .setEmployeeId(employee)
                                .setQualification(decision)
                                .setOwnerName("QA")
                                .setReference("合成资格依据")
                                .setReason("合成资格")
                                .setEffectiveFrom(start)
                                .setEffectiveTo(end));
        eligibility.review(
                new HrmPayrollEligibilityReviewReqVO()
                        .setId(id)
                        .setRevision(1)
                        .setAction("confirm")
                        .setEvidence("合成确认"));
    }

    private Long batch(String code, boolean run, boolean mixed) {
        List<HrmPayrollTrialConfigVO.PersonInput> people = new ArrayList<>();
        people.add(
                new HrmPayrollTrialConfigVO.PersonInput()
                        .setEmployeeId(101L)
                        .setInputReference("合成金额依据")
                        .setInputs(
                                new LinkedHashMap<>(
                                        HrmPayrollWageTemplate.program()
                                                .getCases()
                                                .get(0)
                                                .getInputs())));
        if (mixed)
            people.add(
                    new HrmPayrollTrialConfigVO.PersonInput()
                            .setEmployeeId(102L)
                            .setInputs(new LinkedHashMap<>())
                            .setInputReference("明确排除"));
        Long id =
                trial.create(
                        new HrmPayrollTrialSaveReqVO()
                                .setCode(code)
                                .setTitle(code)
                                .setEntityCode("QA")
                                .setEntityName("合成主体")
                                .setPeriodStart(start)
                                .setPeriodEnd(end)
                                .setDefinitionId(definition)
                                .setOwnerName("QA")
                                .setReference("合成工资")
                                .setConfiguration(new HrmPayrollTrialConfigVO().setPeople(people)));
        if (run) execute(id);
        return id;
    }

    private void execute(Long id) {
        HrmPayrollTrialCheckVO c = trial.check(id);
        assertTrue(c.getReady());
        trial.execute(
                new HrmPayrollTrialExecuteReqVO()
                        .setBatchId(id)
                        .setRevision(c.getRevision())
                        .setSourceHash(c.getSourceHash())
                        .setRequestKey("exec-" + UUID.randomUUID()));
    }

    private HrmPayrollOverviewPageReqVO query() {
        HrmPayrollOverviewPageReqVO q = new HrmPayrollOverviewPageReqVO();
        q.setEntityCode("QA");
        return q;
    }

    private HrmPayrollTrialSaveReqVO edit(Long id) {
        HrmPayrollTrialRespVO b = trial.get(id);
        return BeanUtils.toBean(b, HrmPayrollTrialSaveReqVO.class)
                .setConfiguration(
                        JsonUtils.parseObject(
                                JsonUtils.toJsonString(b.getConfiguration()),
                                HrmPayrollTrialConfigVO.class));
    }

    private String state(Long id) {
        return jdbc.queryForList(
                                "SELECT revision,status,current_run_id,latest_run_id,active_review_id,frozen_run_id FROM hrm_payroll_trial_batch WHERE id=?",
                                id)
                        .toString()
                + jdbc.queryForObject("SELECT COUNT(*) FROM hrm_payroll_review", Long.class)
                + jdbc.queryForObject("SELECT COUNT(*) FROM hrm_payroll_trial_run", Long.class);
    }

    private Task nativeTask(String pid, String key, String actor, String tenant) {
        Task t = mock(Task.class);
        when(t.getProcessInstanceId()).thenReturn(pid);
        when(t.getTaskDefinitionKey()).thenReturn(key);
        when(t.getAssignee()).thenReturn(actor);
        when(t.getTenantId()).thenReturn(tenant);
        nativeTasks.add(t);
        return t;
    }

    private String cycle(Long batch, Long cycle, String stage) {
        Long run = trial.get(batch).getCurrentRunId();
        String pid = "p-" + cycle;
        jdbc.update(
                "INSERT INTO hrm_payroll_review_cycle(id,batch_id,run_id,cycle_version,status,source_hash,process_instance_id,process_definition_id,started_by,submit_evidence,started_at,hr_reviewer_id,finance_reviewer_id,tenant_id) VALUES(?,?,?,1,0,? ,?, 'hrm_payroll_trial_review:1:qa',10,'合成复核',CURRENT_TIMESTAMP,30,40,1)",
                cycle,
                batch,
                run,
                String.join("", Collections.nCopies(64, "a")),
                pid);
        jdbc.update(
                "UPDATE hrm_payroll_trial_batch SET status=2,active_review_id=? WHERE id=?",
                cycle,
                batch);
        nativeTask(pid, stage, "hrReview".equals(stage) ? "30" : "40", "1");
        return pid;
    }

    @Test
    void emptyQueryHasZeroStateCountsAndNoAmounts() {
        assertEquals(0L, service.page(query()).getTotal());
        assertEquals(0L, service.page(query()).getAssignedHr());
        assertEquals(0L, service.page(query()).getAssignedFinance());
        assertTrue(service.page(query()).getBatches().getList().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "hrm:payroll:overview:query",
                "hrm:payroll:trial:query",
                "hrm:payroll:calculation:query",
                "hrm:payroll:eligibility:query",
                "hrm:employee:query"
            })
    void everyCombinedPermissionIsRequired(String p) {
        Long id = batch("PERMS", true, false);
        when(permissions.hasAnyPermissions(10L, p)).thenReturn(false);
        assertServiceException(() -> service.page(query()), PAYROLL_OVERVIEW_PERMISSION);
        assertServiceException(() -> service.detail(id), PAYROLL_OVERVIEW_PERMISSION);
    }

    @Test
    void totalsAreIndependentOfPaginationAndStatusFilterIsConsistent() {
        Long a = batch("DRAFT", false, false),
                b = batch("TRIAL", true, false),
                c = batch("REVIEW", true, false),
                d = batch("APPROVED", true, false),
                e = batch("FROZEN", true, false);
        cycle(c, 901L, "hrReview");
        jdbc.update("UPDATE hrm_payroll_trial_batch SET status=3 WHERE id=?", d);
        jdbc.update(
                "UPDATE hrm_payroll_trial_batch SET status=4,frozen_run_id=current_run_id WHERE id=?",
                e);
        HrmPayrollOverviewPageReqVO q = query();
        q.setPageSize(1);
        HrmPayrollOverviewRespVO r = service.page(q);
        assertEquals(5L, r.getTotal());
        assertEquals(5L, r.getBatches().getTotal());
        assertEquals(1, r.getBatches().getList().size());
        for (int s = 0; s < 5; s++) assertEquals(1L, r.getStates().get(s));
        q.setStatus(2);
        r = service.page(q);
        assertEquals(1L, r.getTotal());
        assertEquals(c, r.getBatches().getList().get(0).getId());
        assertEquals(0L, r.getStates().get(1));
    }

    @Test
    void entityNameAndExactCompletePeriodFiltersAgreeAcrossCountsAndRows() {
        Long id = batch("MATCH", true, false);
        HrmPayrollOverviewPageReqVO q = query();
        q.setSearch("MAT");
        q.setPeriodStart(start);
        q.setPeriodEnd(end);
        assertEquals(id, service.page(q).getBatches().getList().get(0).getId());
        q.setPeriodEnd(end.minusDays(1));
        assertEquals(0L, service.page(q).getTotal());
        q.setPeriodEnd(end);
        q.setEntityCode("OTHER");
        assertEquals(0L, service.page(q).getTotal());
    }

    @Test
    void invalidRangesAndUnboundedPagesAreRejected() {
        HrmPayrollOverviewPageReqVO q = query();
        q.setPeriodStart(start);
        assertServiceException(() -> service.page(q), PAYROLL_OVERVIEW_INVALID, "须同时选择期间起止日");
        q.setPeriodEnd(start.minusDays(1));
        assertServiceException(() -> service.page(q), PAYROLL_OVERVIEW_INVALID, "期间起日不能晚于止日");
        q.setPeriodStart(null);
        q.setPeriodEnd(null);
        q.setPageSize(-1);
        assertServiceException(() -> service.page(q), PAYROLL_OVERVIEW_INVALID, "查询字段或分页参数无效");
        q.setPageSize(201);
        assertServiceException(() -> service.page(q), PAYROLL_OVERVIEW_INVALID, "查询字段或分页参数无效");
    }

    @Test
    void sqlLookingQueryIsBoundAsLiteralData() {
        batch("SAFE", true, false);
        HrmPayrollOverviewPageReqVO q = query();
        q.setEntityCode("QA' OR 1=1 --");
        assertEquals(0L, service.page(q).getTotal());
    }

    @Test
    void singleSavedVersionIsShownWithoutAddingOverlappingBatches() {
        Long a = batch("ONE", true, false);
        batch("DUPLICATE-PERIOD", true, false);
        HrmPayrollOverviewRespVO.Detail d = service.detail(a);
        assertEquals("CURRENT", d.getAvailability());
        assertEquals(
                trial.run(trial.get(a).getCurrentRunId()).getResult().getTotals(), d.getAmounts());
        assertEquals(1, d.getRunVersion());
        assertEquals(2L, service.page(query()).getTotal());
    }

    @Test
    void missingVersionHasNoInventedZeroMoney() {
        Long id = batch("NO-VERSION", false, false);
        HrmPayrollOverviewRespVO.Detail d = service.detail(id);
        assertEquals("NONE", d.getAvailability());
        assertNull(d.getAmounts());
        assertNull(d.getRunId());
        assertTrue(d.getCheck().getReady());
    }

    @Test
    void invalidatedVersionIsHistoricalAndNewTrialRestoresCurrentState() {
        Long id = batch("INVALIDATE", true, false);
        Long old = trial.get(id).getCurrentRunId();
        trial.update(edit(id).setTitle("新草稿"));
        assertEquals("INVALIDATED", service.detail(id).getAvailability());
        assertEquals(old, service.detail(id).getRunId());
        execute(id);
        assertEquals("CURRENT", service.detail(id).getAvailability());
        assertEquals(2, service.detail(id).getRunVersion());
    }

    @Test
    void sourceBasisDriftIsDetectedEvenWhenAmountsAreUnchanged() {
        Long id = batch("DRIFT", true, false);
        Map<String, String> amounts = service.detail(id).getAmounts();
        jdbc.update(
                "UPDATE hrm_payroll_employee_eligibility SET reference='新的合成依据' WHERE employee_id=101 AND tenant_id=1");
        HrmPayrollOverviewRespVO.Detail d = service.detail(id);
        assertTrue(d.getCheck().getReady());
        assertEquals("SOURCE_CHANGED", d.getAvailability());
        assertEquals(amounts, d.getAmounts());
    }

    @Test
    void changedPersonnelShowsBlockersAndKeepsOriginalMoney() {
        Long id = batch("PERSON", true, false);
        Map<String, String> saved = service.detail(id).getAmounts();
        jdbc.update("UPDATE hrm_employee SET status=30 WHERE id=101");
        HrmPayrollOverviewRespVO.Detail d = service.detail(id);
        assertEquals("SOURCE_CHANGED", d.getAvailability());
        assertFalse(d.getCheck().getReady());
        assertEquals(1, d.getCheck().getBlockedCount());
        assertEquals(saved, d.getAmounts());
    }

    @Test
    void workflowOnlyStateChangesDoNotInvalidateSources() {
        Long id = batch("FLOW", true, false);
        jdbc.update(
                "UPDATE hrm_payroll_trial_batch SET status=4,revision=revision+4,frozen_run_id=current_run_id WHERE id=?",
                id);
        assertEquals("CURRENT", service.detail(id).getAvailability());
        assertEquals("FROZEN", service.detail(id).getBatch().getStage());
    }

    @Test
    void getNeverChangesRevisionHistoryOrRunCount() {
        Long id = batch("READ", true, false);
        cycle(id, 901L, "financeReview");
        String before = state(id);
        service.page(query());
        service.detail(id);
        service.detail(id);
        assertEquals(before, state(id));
    }

    @Test
    void referenceToAnotherBatchRunCannotMixMoney() {
        Long a = batch("BAD-POINTER", false, false), b = batch("OTHER", true, false);
        jdbc.update(
                "UPDATE hrm_payroll_trial_batch SET current_run_id=?,latest_run_id=? WHERE id=?",
                trial.get(b).getCurrentRunId(),
                trial.get(b).getCurrentRunId(),
                a);
        assertEquals("UNAVAILABLE", service.detail(a).getAvailability());
        assertNull(service.detail(a).getAmounts());
    }

    @Test
    void crossTenantCountsAreEmptyAndDetailIsNotReadable() {
        Long id = batch("TENANT", true, false);
        context(999L, 10L);
        assertEquals(0L, service.page(query()).getTotal());
        assertServiceException(() -> service.detail(id), PAYROLL_TRIAL_NOT_EXISTS);
    }

    @Test
    void departmentsAndSelfCannotCountMixedOrExcludedPersonnelOutsideScope() {
        Long a = batch("ALLOWED", true, false), b = batch("MIXED", true, true);
        for (DeptDataPermissionRespDTO scope :
                Arrays.asList(
                        new DeptDataPermissionRespDTO()
                                .setDeptIds(new HashSet<>(Collections.singletonList(11L))),
                        new DeptDataPermissionRespDTO().setSelf(true))) {
            when(permissions.getDeptDataPermission(10L)).thenReturn(scope);
            assertEquals(1L, service.page(query()).getTotal());
            assertEquals(a, service.page(query()).getBatches().getList().get(0).getId());
            assertServiceException(() -> service.detail(b), PAYROLL_TRIAL_NOT_EXISTS);
        }
    }

    @Test
    void bothCapturedAndCurrentDepartmentsMustRemainVisible() {
        Long id = batch("MOVED", true, false);
        jdbc.update("UPDATE hrm_employee SET dept_id=22 WHERE id=101");
        when(permissions.getDeptDataPermission(10L))
                .thenReturn(
                        new DeptDataPermissionRespDTO()
                                .setDeptIds(new HashSet<>(Collections.singletonList(11L))));
        assertEquals(0L, service.page(query()).getTotal());
        when(permissions.getDeptDataPermission(10L))
                .thenReturn(
                        new DeptDataPermissionRespDTO()
                                .setDeptIds(new HashSet<>(Collections.singletonList(22L))));
        assertEquals(0L, service.page(query()).getTotal());
    }

    @Test
    void removedPersonRemainsInHistoricalScopeGate() {
        Long id = batch("HISTORY", true, true);
        HrmPayrollTrialSaveReqVO e = edit(id);
        e.getConfiguration().getPeople().remove(1);
        trial.update(e);
        when(permissions.getDeptDataPermission(10L))
                .thenReturn(
                        new DeptDataPermissionRespDTO()
                                .setDeptIds(new HashSet<>(Collections.singletonList(11L))));
        assertEquals(0L, service.page(query()).getTotal());
        assertServiceException(() -> service.detail(id), PAYROLL_TRIAL_NOT_EXISTS);
    }

    @Test
    void nativeAssignedTasksCountBeyondPageAndIdentifyCurrentStage() {
        Long a = batch("HR-1", true, false),
                b = batch("HR-2", true, false),
                c = batch("FINANCE", true, false);
        cycle(a, 901L, "hrReview");
        cycle(b, 902L, "hrReview");
        cycle(c, 903L, "financeReview");
        context(1L, 30L);
        HrmPayrollOverviewPageReqVO q = query();
        q.setPageSize(1);
        HrmPayrollOverviewRespVO r = service.page(q);
        assertEquals(2L, r.getAssignedHr());
        assertEquals(0L, r.getAssignedFinance());
        assertEquals("FINANCE_REVIEW", r.getBatches().getList().get(0).getStage());
        assertFalse(r.getBatches().getList().get(0).getAssignedToMe());
        q.setSearch("HR-1");
        assertTrue(service.page(q).getBatches().getList().get(0).getAssignedToMe());
        context(1L, 40L);
        assertEquals(1L, service.page(query()).getAssignedFinance());
    }

    @Test
    void staleWrongVersionAndOtherTenantTasksCannotEnterCounts() {
        Long a = batch("OLD", true, false),
                b = batch("WRONG-RUN", true, false),
                c = batch("TENANT-TASK", true, false);
        cycle(a, 901L, "hrReview");
        cycle(b, 902L, "hrReview");
        String pid = cycle(c, 903L, "hrReview");
        jdbc.update("UPDATE hrm_payroll_review_cycle SET status=3 WHERE id=901");
        jdbc.update("UPDATE hrm_payroll_review_cycle SET run_id=run_id+999 WHERE id=902");
        nativeTasks.removeIf(t -> pid.equals(t.getProcessInstanceId()));
        nativeTask(pid, "hrReview", "30", "999");
        context(1L, 30L);
        assertEquals(0L, service.page(query()).getAssignedHr());
        assertTrue(
                service.page(query()).getBatches().getList().stream()
                        .allMatch(r -> r.getStage().equals("CHECK_BPM")));
    }

    @Test
    void assignedTaskStillCannotCountHiddenMixedBatch() {
        Long id = batch("HIDDEN-TASK", true, true);
        cycle(id, 901L, "hrReview");
        context(1L, 30L);
        when(permissions.getDeptDataPermission(30L))
                .thenReturn(
                        new DeptDataPermissionRespDTO()
                                .setDeptIds(new HashSet<>(Collections.singletonList(11L))));
        assertEquals(0L, service.page(query()).getAssignedHr());
        assertEquals(0L, service.page(query()).getTotal());
    }

    @Test
    void finishedEngineWithoutBusinessSyncIsReportedAndNotMutated() {
        Long id = batch("UNSYNCED", true, false);
        cycle(id, 901L, "financeReview");
        nativeTasks.clear();
        String before = state(id);
        assertEquals("CHECK_BPM", service.detail(id).getBatch().getStage());
        assertEquals(before, state(id));
    }
}
