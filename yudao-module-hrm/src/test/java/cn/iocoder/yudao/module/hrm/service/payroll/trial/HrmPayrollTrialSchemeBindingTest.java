package cn.iocoder.yudao.module.hrm.service.payroll.trial;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.calculation.*;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.eligibility.*;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.overview.*;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.review.*;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.scheme.*;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.trial.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.trial.*;
import cn.iocoder.yudao.module.hrm.service.payroll.calculation.*;
import cn.iocoder.yudao.module.hrm.service.payroll.eligibility.*;
import cn.iocoder.yudao.module.hrm.service.payroll.identity.*;
import cn.iocoder.yudao.module.hrm.service.payroll.overview.*;
import cn.iocoder.yudao.module.hrm.service.payroll.review.*;
import cn.iocoder.yudao.module.hrm.service.payroll.scheme.*;
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
    HrmPayrollTrialSchemeBinding.class,
    HrmPayrollSchemeServiceImpl.class,
    HrmPayrollOverviewServiceImpl.class,
    HrmPayrollReviewServiceImpl.class,
    HrmPayrollTrialServiceImpl.class,
    HrmPayrollCalculationServiceImpl.class,
    HrmPayrollCalculationEngine.class,
    HrmPayrollEligibilityServiceImpl.class,
    HrmPayrollEmployeeMappingServiceImpl.class,
    HrmPayrollEmployeeAccess.class,
    ValidationAutoConfiguration.class
})
class HrmPayrollTrialSchemeBindingTest extends BaseDbUnitTest {
    @Resource private HrmPayrollTrialService service;
    @Resource private HrmPayrollSchemeService schemes;
    @Resource private HrmPayrollOverviewService overview;
    @Resource private HrmPayrollReviewService review;
    @MockBean private HrmPayrollReviewBpmGateway bpm;

    @MockBean(answer = org.mockito.Answers.RETURNS_DEEP_STUBS)
    private org.flowable.engine.TaskService tasks;

    private Long scheme;
    private final List<String> policyPermissions =
            Arrays.asList(
                    "hrm:payroll:scheme:query",
                    "hrm:salary:group:query",
                    "hrm:salary:option:query",
                    "hrm:salary:tax-rule:query");
    @Resource private HrmPayrollCalculationService calculation;
    @Resource private HrmPayrollEligibilityService eligibility;
    @Resource private HrmPayrollTrialBatchMapper batches;
    @Resource private HrmPayrollTrialRunMapper runs;
    @Resource private HrmPayrollTrialPersonMapper people;
    @Resource private javax.sql.DataSource dataSource;
    @MockBean private PermissionApi permissions;
    @MockBean private AdminUserApi users;
    private JdbcTemplate jdbc;
    private Long definition;
    private final LocalDate start = LocalDate.of(2026, 10, 1), end = LocalDate.of(2026, 10, 31);

    @BeforeEach
    void fixture() {
        jdbc = new JdbcTemplate(dataSource);
        org.flowable.task.api.TaskQuery nativeQuery =
                org.mockito.Mockito.mock(
                        org.flowable.task.api.TaskQuery.class, org.mockito.Answers.RETURNS_SELF);
        when(tasks.createTaskQuery()).thenReturn(nativeQuery);
        when(nativeQuery.list()).thenReturn(Collections.emptyList());
        context(1L);
        for (String p :
                Arrays.asList(
                        "hrm:employee:query",
                        "hrm:payroll:trial:query",
                        "hrm:payroll:calculation:query",
                        "hrm:payroll:eligibility:query",
                        "hrm:payroll:trial:maintain",
                        "hrm:payroll:trial:execute"))
            when(permissions.hasAnyPermissions(10L, p)).thenReturn(true);
        when(users.getUser(10L))
                .thenReturn(new AdminUserRespDTO().setId(10L).setNickname("合成试算人员"));
        scope(new DeptDataPermissionRespDTO().setAll(true));
        jdbc.update(
                "INSERT INTO hrm_employee(id,name,job_number,dept_id,user_id,status,tenant_id) VALUES(101,'合成人员 A','001',11,10,20,1),(102,'合成人员 B','001',22,20,30,1),(103,'无归属人员','003',NULL,NULL,20,1),(999,'其他租户','001',11,10,20,999)");
        definition = definition("RULE");
        qualify(101L, "INCLUDED");
        for (String p : policyPermissions)
            when(permissions.hasAnyPermissions(10L, p)).thenReturn(true);
        when(permissions.hasAnyPermissions(10L, "hrm:payroll:overview:query")).thenReturn(true);
        jdbc.update(
                "INSERT INTO hrm_salary_tax_rule(id,name,type,tax_enabled,tenant_id) VALUES(11,'合成不计税配置',3,FALSE,1)");
        jdbc.update(
                "INSERT INTO hrm_salary_group(id,name,salary_standard,change_rule,tax_rule_id,employee_ids,dept_ids,tenant_id) VALUES(1,'合成方案组',10,'合成规则',11,'[888]','[999]',1)");
        jdbc.update(
                "INSERT INTO hrm_salary_option(id,code,parent_code,name,type,enabled,visible,tax_enabled,calculate_enabled,tenant_id) VALUES(201,10,0,'合成目录',2,TRUE,TRUE,FALSE,FALSE,1),(202,1000000001,10,'基本工资示例',1,TRUE,TRUE,FALSE,TRUE,1),(203,1000000002,10,'停用项示例',0,FALSE,TRUE,FALSE,TRUE,1)");
        scheme =
                schemes.create(
                        new HrmPayrollSchemeSaveReqVO()
                                .setGroupId(1L)
                                .setTitle("合成方案配置")
                                .setOwnerName("QA")
                                .setReference("方案技术依据")
                                .setEffectiveFrom(start)
                                .setEffectiveTo(end));
        schemeReview(scheme, "confirm");
    }

    private void context(Long tenant) {
        TenantContextHolder.setTenantId(tenant);
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                new LoginUser().setId(10L).setTenantId(tenant).setUserType(2),
                                null,
                                Collections.emptyList()));
    }

    @AfterEach
    void clear() {
        TenantContextHolder.clear();
        SecurityContextHolder.clearContext();
    }

    private void scope(DeptDataPermissionRespDTO value) {
        when(permissions.getDeptDataPermission(10L)).thenReturn(value);
    }

    private Long definition(String code) {
        Long id =
                calculation.create(
                        new HrmPayrollCalculationSaveReqVO()
                                .setCode(code)
                                .setTitle("合成常规工资")
                                .setScopeCode("QA")
                                .setApplicableScope("合成声明主体")
                                .setOwnerName("QA")
                                .setReference("合成技术依据")
                                .setEffectiveFrom(start)
                                .setEffectiveTo(end)
                                .setProgram(HrmPayrollWageTemplate.program()));
        calculation.review(
                new HrmPayrollCalculationReviewReqVO()
                        .setId(id)
                        .setRevision(1)
                        .setAction("confirm")
                        .setEvidence("合成核对"));
        return id;
    }

    private Long qualify(Long employee, String qualification) {
        Long id =
                eligibility.create(
                        new HrmPayrollEligibilitySaveReqVO()
                                .setEntityCode("QA")
                                .setEntityName("合成主体")
                                .setEmployeeId(employee)
                                .setQualification(qualification)
                                .setOwnerName("QA")
                                .setReference("合成资格依据")
                                .setReason("合成决策")
                                .setEffectiveFrom(start)
                                .setEffectiveTo(end));
        eligibility.review(
                new HrmPayrollEligibilityReviewReqVO()
                        .setId(id)
                        .setRevision(1)
                        .setAction("confirm")
                        .setEvidence("合成核对"));
        return id;
    }

    private HrmPayrollTrialConfigVO.PersonInput person(Long id) {
        return new HrmPayrollTrialConfigVO.PersonInput()
                .setEmployeeId(id)
                .setInputReference("合成金额来源")
                .setInputs(
                        new LinkedHashMap<>(
                                HrmPayrollWageTemplate.program().getCases().get(0).getInputs()));
    }

    private HrmPayrollTrialSaveReqVO draft(String code) {
        return new HrmPayrollTrialSaveReqVO()
                .setCode(code)
                .setTitle("合成工资试算")
                .setEntityCode("QA")
                .setEntityName("合成主体")
                .setPeriodStart(start)
                .setPeriodEnd(end)
                .setDefinitionId(definition)
                .setOwnerName("QA")
                .setReference("合成工资口径")
                .setConfiguration(
                        new HrmPayrollTrialConfigVO()
                                .setPeople(
                                        new ArrayList<>(Collections.singletonList(person(101L)))));
    }

    private HrmPayrollTrialSaveReqVO edit(Long id) {
        HrmPayrollTrialRespVO row = service.get(id);
        return BeanUtils.toBean(row, HrmPayrollTrialSaveReqVO.class)
                .setConfiguration(
                        JsonUtils.parseObject(
                                JsonUtils.toJsonString(row.getConfiguration()),
                                HrmPayrollTrialConfigVO.class));
    }

    private HrmPayrollTrialExecuteReqVO command(Long id, String key) {
        HrmPayrollTrialCheckVO check = service.check(id);
        assertTrue(check.getReady(), JsonUtils.toJsonString(check));
        return new HrmPayrollTrialExecuteReqVO()
                .setBatchId(id)
                .setRevision(check.getRevision())
                .setSourceHash(check.getSourceHash())
                .setRequestKey(key);
    }

    private HrmPayrollTrialRunRespVO execute(Long id, String key) {
        return service.execute(command(id, key));
    }

    private HrmPayrollTrialPageReqVO page() {
        HrmPayrollTrialPageReqVO q = new HrmPayrollTrialPageReqVO();
        q.setPageSize(100);
        return q;
    }

    private void invalid(Runnable work) {
        assertEquals(
                PAYROLL_TRIAL_INVALID.getCode(),
                assertThrows(ServiceException.class, work::run).getCode());
    }

    private void issue(Long id, String code) {
        HrmPayrollTrialCheckVO c = service.check(id);
        assertFalse(c.getReady());
        assertTrue(
                c.getIssues().stream().anyMatch(i -> code.equals(i.getCode()))
                        || c.getPeople().stream()
                                .flatMap(p -> p.getIssues().stream())
                                .anyMatch(i -> code.equals(i.getCode())),
                JsonUtils.toJsonString(c));
    }

    private void schemeReview(Long id, String action) {
        schemes.review(
                new HrmPayrollSchemeReviewReqVO()
                        .setId(id)
                        .setRevision(schemes.get(id).getRevision())
                        .setAction(action)
                        .setEvidence("合成方案评审"));
    }

    private HrmPayrollTrialSaveReqVO bound(String code) {
        HrmPayrollTrialSaveReqVO req = draft(code).setSchemeId(scheme);
        List<HrmPayrollTrialConfigVO.SourceBinding> bindings = new ArrayList<>();
        for (HrmPayrollCalculationSpecVO.Input input : HrmPayrollWageTemplate.program().getInputs())
            bindings.add(
                    new HrmPayrollTrialConfigVO.SourceBinding()
                            .setInputKey(input.getKey())
                            .setUnit(input.getUnit())
                            .setSourceType("MANUAL")
                            .setReference("合成明确来源 " + input.getKey()));
        bindings.get(0).setSourceType("SCHEME_ITEM").setOptionId(202L);
        req.getConfiguration().setSourceBindings(bindings);
        return req;
    }

    private HrmPayrollTrialConfigVO.SourceBinding first(HrmPayrollTrialSaveReqVO req) {
        return req.getConfiguration().getSourceBindings().get(0);
    }

    private HrmPayrollOverviewPageReqVO overviewPage() {
        HrmPayrollOverviewPageReqVO r = new HrmPayrollOverviewPageReqVO();
        r.setPageSize(100);
        return r;
    }

    @Test
    void selectedVersionAndAllExplicitSourcesAreCapturedWithStableIsoDates() {
        Long id = service.create(bound("SNAPSHOT"));
        HrmPayrollTrialRunRespVO r = execute(id, "binding-first");
        assertEquals(scheme, r.getResult().getScheme().getId());
        assertEquals(9, r.getResult().getBatch().getConfiguration().getSourceBindings().size());
        assertEquals("6200.00", r.getResult().getTotals().get("net"));
        assertEquals(
                "基本工资示例", r.getResult().getScheme().getSnapshot().getOptions().get(1).getName());
        assertEquals(
                JsonUtils.toJsonString(r.getResult()),
                JsonUtils.toJsonString(service.run(r.getId()).getResult()));
        assertTrue(r.getResult().getScheme().getCapturedAt().getYear() >= 2026);
        assertEquals("CURRENT", service.inspect(id).getAvailability());
        assertNotNull(service.validateCurrentRun(id, r.getId()));
    }

    @Test
    void eachPolicyPermissionProtectsDetailsListsCountsAndAllHistoricalRuns() {
        Long legacy = service.create(draft("LEGACY")),
                id = service.create(bound("PROTECTED")),
                run = execute(id, "protected-first").getId();
        for (String p : policyPermissions) {
            when(permissions.hasAnyPermissions(10L, p)).thenReturn(false);
            assertServiceException(() -> service.get(id), PAYROLL_TRIAL_PERMISSION);
            assertServiceException(() -> service.run(run), PAYROLL_TRIAL_PERMISSION);
            assertServiceException(() -> service.runs(id), PAYROLL_TRIAL_PERMISSION);
            assertServiceException(() -> service.history(id), PAYROLL_TRIAL_PERMISSION);
            assertEquals(1, service.page(page()).getTotal());
            assertEquals(legacy, service.page(page()).getList().get(0).getId());
            assertEquals(1, overview.page(overviewPage()).getTotal());
            assertServiceException(() -> overview.detail(id), PAYROLL_TRIAL_PERMISSION);
            assertServiceException(() -> service.create(bound("DENIED")), PAYROLL_TRIAL_PERMISSION);
            when(permissions.hasAnyPermissions(10L, p)).thenReturn(true);
        }
    }

    @Test
    void legacyUnboundExecutionAndManifestRequireNoAdditionalPolicyAccess() {
        for (String p : policyPermissions)
            when(permissions.hasAnyPermissions(10L, p)).thenReturn(false);
        Long id = service.create(draft("UNBOUND"));
        HrmPayrollTrialRunRespVO r = execute(id, "unbound-first");
        assertNull(r.getResult().getScheme());
        assertNull(service.get(id).getSchemeId());
        assertNotNull(service.validateCurrentRun(id, r.getId()));
        assertEquals("CURRENT", service.inspect(id).getAvailability());
        assertEquals(1, overview.page(overviewPage()).getTotal());
    }

    @Test
    void unknownMissingExtraAndDuplicateInputBindingsCannotBeSaved() {
        HrmPayrollTrialSaveReqVO missing = bound("MISSING");
        missing.getConfiguration().setSourceBindings(null);
        invalid(() -> service.create(missing));
        HrmPayrollTrialSaveReqVO empty = bound("EMPTY");
        empty.getConfiguration().setSourceBindings(Collections.emptyList());
        invalid(() -> service.create(empty));
        HrmPayrollTrialSaveReqVO duplicate = bound("DUPLICATE");
        duplicate.getConfiguration().getSourceBindings().get(1).setInputKey("baseSalary");
        invalid(() -> service.create(duplicate));
        HrmPayrollTrialSaveReqVO unknown = bound("UNKNOWN");
        first(unknown).setInputKey("unknown");
        invalid(() -> service.create(unknown));
        HrmPayrollTrialSaveReqVO extra = bound("EXTRA");
        extra.getConfiguration()
                .getSourceBindings()
                .add(
                        new HrmPayrollTrialConfigVO.SourceBinding()
                                .setInputKey("extra")
                                .setUnit("CNY")
                                .setSourceType("MANUAL")
                                .setReference("合成"));
        invalid(() -> service.create(extra));
    }

    @Test
    void foreignMissingOrDisabledSnapshotItemCannotBind() {
        for (Long option : Arrays.asList(203L, 999L)) {
            HrmPayrollTrialSaveReqVO req = bound("BAD-ITEM" + option);
            first(req).setOptionId(option);
            invalid(() -> service.create(req));
        }
        HrmPayrollTrialSaveReqVO req = bound("NULL-ITEM");
        first(req).setOptionId(null);
        invalid(() -> service.create(req));
    }

    @Test
    void manualSourcesCannotPretendToBeSchemeItemsAndAtLeastOneItemIsExplicit() {
        HrmPayrollTrialSaveReqVO req = bound("ALL-MANUAL");
        first(req).setSourceType("MANUAL").setOptionId(null);
        invalid(() -> service.create(req));
        HrmPayrollTrialSaveReqVO wrong = bound("MANUAL-ID");
        wrong.getConfiguration().getSourceBindings().get(1).setOptionId(202L);
        invalid(() -> service.create(wrong));
        HrmPayrollTrialSaveReqVO detached = bound("NO-SCHEME").setSchemeId(null);
        invalid(() -> service.create(detached));
    }

    @Test
    void sameItemCannotFeedMultipleInputs() {
        HrmPayrollTrialSaveReqVO req = bound("DOUBLE-ITEM");
        req.getConfiguration()
                .getSourceBindings()
                .get(1)
                .setSourceType("SCHEME_ITEM")
                .setOptionId(202L);
        invalid(() -> service.create(req));
    }

    @Test
    void inputUnitsAndExplicitBasisAreValidatedBeforeSaving() {
        HrmPayrollTrialSaveReqVO req = bound("UNIT");
        first(req).setUnit("USD");
        invalid(() -> service.create(req));
        HrmPayrollTrialSaveReqVO blank = bound("BASIS");
        first(blank).setReference(" ");
        invalid(() -> service.create(blank));
    }

    @Test
    void schemeItemsCannotBindAnUntypedCountOrForeignCurrency() {
        for (String change : Arrays.asList("INTEGER", "USD", "SCALE")) {
            HrmPayrollCalculationSaveReqVO req =
                    BeanUtils.toBean(
                                    calculation.get(definition),
                                    HrmPayrollCalculationSaveReqVO.class)
                            .setCode("TYPE-" + change);
            HrmPayrollCalculationSpecVO.Input in = req.getProgram().getInputs().get(0);
            if ("INTEGER".equals(change)) {
                in.setType(change).setScale(0);
                for (HrmPayrollCalculationSpecVO.BusinessCase c : req.getProgram().getCases())
                    c.getInputs()
                            .put(
                                    "baseSalary",
                                    new java.math.BigDecimal(c.getInputs().get("baseSalary"))
                                            .toBigIntegerExact()
                                            .toString());
            } else if ("USD".equals(change)) in.setUnit(change);
            else in.setScale(3);
            Long rule = calculation.create(req);
            calculation.review(
                    new HrmPayrollCalculationReviewReqVO()
                            .setId(rule)
                            .setRevision(1)
                            .setAction("confirm")
                            .setEvidence("合成反例"));
            HrmPayrollTrialSaveReqVO batch = bound("UNITS-" + change).setDefinitionId(rule);
            first(batch).setUnit(in.getUnit());
            invalid(() -> service.create(batch));
        }
    }

    @Test
    void pendingRetiredAndPartialPeriodSchemeVersionsAreRejected() {
        jdbc.update("UPDATE hrm_payroll_scheme_version SET status=0 WHERE id=?", scheme);
        invalid(() -> service.create(bound("PENDING")));
        jdbc.update("UPDATE hrm_payroll_scheme_version SET status=2 WHERE id=?", scheme);
        invalid(() -> service.create(bound("RETIRED")));
        jdbc.update(
                "UPDATE hrm_payroll_scheme_version SET status=1,effective_to=? WHERE id=?",
                end.minusDays(1),
                scheme);
        invalid(() -> service.create(bound("PARTIAL")));
    }

    @Test
    void boundRuleMustBeConfirmedCoverPeriodAndDeclareSameEntity() {
        jdbc.update(
                "UPDATE hrm_payroll_calculation_definition SET scope_code='OTHER' WHERE id=?",
                definition);
        invalid(() -> service.create(bound("SCOPE")));
        jdbc.update(
                "UPDATE hrm_payroll_calculation_definition SET scope_code='QA',status=0 WHERE id=?",
                definition);
        invalid(() -> service.create(bound("DRAFT-RULE")));
        jdbc.update(
                "UPDATE hrm_payroll_calculation_definition SET status=1,effective_from=? WHERE id=?",
                start.plusDays(1),
                definition);
        invalid(() -> service.create(bound("RULE-PERIOD")));
    }

    @Test
    void fourOutputRolesAreConfirmedWithTheSourceAssociation() {
        HrmPayrollTrialSaveReqVO req = bound("OUTPUTS");
        req.getConfiguration().getRoles().setNet("tax");
        invalid(() -> service.create(req));
    }

    @Test
    void laterMutableSourceEditsDoNotOverwriteConfirmedSnapshotOrAutoFillAmounts() {
        Long id = service.create(bound("RAW-CHANGE"));
        HrmPayrollTrialRunRespVO r = execute(id, "raw-first");
        jdbc.update("UPDATE hrm_salary_option SET name='后来源项目',enabled=FALSE,type=0 WHERE id=202");
        jdbc.update("UPDATE hrm_salary_group SET salary_standard=99,employee_ids='[]' WHERE id=1");
        assertEquals("CURRENT", service.inspect(id).getAvailability());
        assertNotNull(service.validateCurrentRun(id, r.getId()));
        assertEquals(
                "基本工资示例",
                service.run(r.getId())
                        .getResult()
                        .getScheme()
                        .getSnapshot()
                        .getOptions()
                        .get(1)
                        .getName());
        assertEquals(
                "6000.00",
                service.run(r.getId())
                        .getResult()
                        .getPeople()
                        .get(0)
                        .getInput()
                        .getInputs()
                        .get("baseSalary"));
    }

    @Test
    void retirementBlocksNewCalculationAndReviewWhileSavedVersionSurvives() {
        Long id = service.create(bound("STOP"));
        HrmPayrollTrialRunRespVO r = execute(id, "stop-first");
        String json = JsonUtils.toJsonString(r.getResult());
        schemeReview(scheme, "retire");
        issue(id, "SCHEME_BINDING_BLOCKED");
        assertEquals("SOURCE_CHANGED", service.inspect(id).getAvailability());
        assertServiceException(
                () -> service.validateCurrentRun(id, r.getId()), PAYROLL_REVIEW_SOURCE_CHANGED);
        assertEquals(json, JsonUtils.toJsonString(service.run(r.getId()).getResult()));
        assertEquals(1, service.execute(commandBeforeRetirement(id, r)).getRunVersion());
    }

    private HrmPayrollTrialExecuteReqVO commandBeforeRetirement(
            Long id, HrmPayrollTrialRunRespVO r) {
        return new HrmPayrollTrialExecuteReqVO()
                .setBatchId(id)
                .setRevision(r.getExpectedRevision())
                .setSourceHash(r.getSourceHash())
                .setRequestKey("stop-first");
    }

    @Test
    void basisAndSelectedSchemeChangesInvalidateVersionEvenWhenMoneyStaysEqual() {
        Long id = service.create(bound("BASIS-CHANGE"));
        HrmPayrollTrialRunRespVO a = execute(id, "basis-first");
        HrmPayrollTrialSaveReqVO req = edit(id);
        first(req).setReference("新的合成项目口径");
        service.update(req);
        assertEquals("INVALIDATED", service.inspect(id).getAvailability());
        HrmPayrollTrialRunRespVO b = execute(id, "basis-second");
        assertEquals(a.getResult().getTotals(), b.getResult().getTotals());
        assertNotEquals(a.getSourceHash(), b.getSourceHash());
        assertTrue(service.compare(a.getId(), b.getId()).getRuleChanged());
        assertEquals(
                "合成明确来源 baseSalary",
                service.run(a.getId())
                        .getResult()
                        .getBatch()
                        .getConfiguration()
                        .getSourceBindings()
                        .get(0)
                        .getReference());
    }

    @Test
    void bindingCannotBeClearedToBypassHistoricalSchemePermission() {
        Long id = service.create(bound("CANNOT-CLEAR"));
        Long old = execute(id, "clear-first").getId();
        HrmPayrollTrialSaveReqVO req = edit(id).setSchemeId(null);
        req.getConfiguration().setSourceBindings(null);
        invalid(() -> service.update(req));
        assertEquals(scheme, service.get(id).getSchemeId());
        when(permissions.hasAnyPermissions(10L, "hrm:payroll:scheme:query")).thenReturn(false);
        assertServiceException(() -> service.run(old), PAYROLL_TRIAL_PERMISSION);
    }

    @Test
    void addingBindingToLegacyBatchPreservesOldRunAndStrengthensAllHistoryAccess() {
        Long id = service.create(draft("UPGRADE"));
        HrmPayrollTrialRunRespVO old = execute(id, "upgrade-first");
        HrmPayrollTrialSaveReqVO req = edit(id).setSchemeId(scheme);
        req.getConfiguration()
                .setSourceBindings(bound("TEMP").getConfiguration().getSourceBindings());
        service.update(req);
        HrmPayrollTrialRunRespVO newer = execute(id, "upgrade-second");
        assertNull(service.run(old.getId()).getResult().getScheme());
        assertTrue(service.compare(old.getId(), newer.getId()).getRuleChanged());
        when(permissions.hasAnyPermissions(10L, "hrm:salary:option:query")).thenReturn(false);
        assertServiceException(() -> service.run(old.getId()), PAYROLL_TRIAL_PERMISSION);
    }

    @Test
    void schemeIsPartOfCurrentManifestThroughoutWorkflowStatuses() {
        Long id = service.create(bound("WORKFLOW"));
        HrmPayrollTrialRunRespVO r = execute(id, "workflow-first");
        String basis = service.validateCurrentRun(id, r.getId());
        for (int status : Arrays.asList(2, 3, 4)) {
            jdbc.update(
                    "UPDATE hrm_payroll_trial_batch SET status=?,revision=revision+1 WHERE id=?",
                    status,
                    id);
            assertEquals(basis, service.validateCurrentRun(id, r.getId()));
            assertEquals("CURRENT", service.inspect(id).getAvailability());
        }
    }

    @Test
    void crossTenantVersionCannotBindAndTenantCannotReadCapturedPolicy() {
        Long id = service.create(bound("TENANT-BOUND"));
        Long run = execute(id, "tenant-bound-first").getId();
        context(999L);
        assertServiceException(() -> service.get(id), PAYROLL_TRIAL_NOT_EXISTS);
        assertServiceException(() -> service.run(run), PAYROLL_TRIAL_NOT_EXISTS);
        context(1L);
        jdbc.update("UPDATE hrm_payroll_scheme_version SET tenant_id=999 WHERE id=?", scheme);
        invalidScheme(() -> service.create(bound("FOREIGN-SCHEME")));
    }

    private void invalidScheme(Runnable task) {
        assertServiceException(task::run, PAYROLL_SCHEME_NOT_EXISTS);
    }

    @Test
    void completeHistoricalPersonnelScopeStillProtectsSchemeBoundBatches() {
        qualify(102L, "EXCLUDED");
        HrmPayrollTrialSaveReqVO req = bound("MIXED-SCHEME");
        req.getConfiguration().getPeople().add(person(102L));
        Long id = service.create(req);
        Long run = execute(id, "mixed-scheme-first").getId();
        scope(new DeptDataPermissionRespDTO().setSelf(true));
        assertEquals(0, service.page(page()).getTotal());
        assertEquals(0, overview.page(overviewPage()).getTotal());
        assertServiceException(() -> service.run(run), PAYROLL_TRIAL_NOT_EXISTS);
    }

    @Test
    void selectedReviewerMustAlsoHaveEveryBoundPolicyPermissionBeforeBpmStarts() {
        Long id = service.create(bound("REVIEWER"));
        HrmPayrollTrialRunRespVO run = execute(id, "reviewer-first");
        when(permissions.hasAnyPermissions(10L, "hrm:payroll:trial:submit")).thenReturn(true);
        for (Long actor : Arrays.asList(11L, 12L)) {
            when(users.getUser(actor)).thenReturn(new AdminUserRespDTO().setId(actor).setStatus(0));
            when(permissions.getDeptDataPermission(actor))
                    .thenReturn(new DeptDataPermissionRespDTO().setAll(true));
            for (String p :
                    Arrays.asList(
                            "hrm:payroll:trial:query",
                            "hrm:payroll:calculation:query",
                            "hrm:payroll:eligibility:query",
                            "hrm:employee:query",
                            "hrm:payroll:trial:hr-review",
                            "hrm:payroll:trial:finance-review"))
                when(permissions.hasAnyPermissions(actor, p)).thenReturn(true);
            for (String p : policyPermissions)
                when(permissions.hasAnyPermissions(actor, p)).thenReturn(true);
        }
        for (Long actor : Arrays.asList(11L, 12L))
            for (String p : policyPermissions) {
                when(permissions.hasAnyPermissions(actor, p)).thenReturn(false);
                assertServiceException(
                        () ->
                                review.action(
                                        new HrmPayrollReviewActionReqVO()
                                                .setBatchId(id)
                                                .setRevision(service.get(id).getRevision())
                                                .setRunId(run.getId())
                                                .setAction("submit")
                                                .setRequestKey("reviewer-submit")
                                                .setEvidence("合成复核依据")
                                                .setHrReviewerId(11L)
                                                .setFinanceReviewerId(12L)),
                        PAYROLL_REVIEW_PERMISSION);
                when(permissions.hasAnyPermissions(actor, p)).thenReturn(true);
            }
        org.mockito.Mockito.verify(bpm, org.mockito.Mockito.never())
                .start(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void updatingVersionUsesOptimisticRevisionAndCannotRewriteSavedSources() {
        Long id = service.create(bound("REVISION"));
        HrmPayrollTrialSaveReqVO old = edit(id);
        service.update(edit(id));
        assertServiceException(() -> service.update(old), PAYROLL_TRIAL_STALE);
        assertEquals(2, service.get(id).getRevision());
    }

    @Test
    void boundExecutionLeavesExistingWagesInsuranceSlipsAndBankDataUntouched() {
        List<String> tables =
                Arrays.asList(
                        "hrm_salary_month_record",
                        "hrm_salary_month_employee_record",
                        "hrm_salary_slip",
                        "hrm_salary_slip_send_record",
                        "hrm_insurance_month_record",
                        "hrm_insurance_month_employee_record",
                        "hrm_employee_salary_card");
        Map<String, Integer> counts = new HashMap<>();
        for (String t : tables)
            counts.put(t, jdbc.queryForObject("SELECT COUNT(*) FROM " + t, Integer.class));
        execute(service.create(bound("NO-WRITE")), "no-write-first");
        for (String t : tables)
            assertEquals(
                    counts.get(t), jdbc.queryForObject("SELECT COUNT(*) FROM " + t, Integer.class));
    }
}
