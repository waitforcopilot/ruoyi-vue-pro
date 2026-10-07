package cn.iocoder.yudao.module.hrm.service.payroll.trial;

import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.calculation.*;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.eligibility.*;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.trial.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.trial.*;
import cn.iocoder.yudao.module.hrm.service.payroll.calculation.*;
import cn.iocoder.yudao.module.hrm.service.payroll.eligibility.*;
import cn.iocoder.yudao.module.hrm.service.payroll.identity.*;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.*;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;

@Import({HrmPayrollTrialServiceImpl.class,HrmPayrollCalculationServiceImpl.class,HrmPayrollCalculationEngine.class,
        HrmPayrollEligibilityServiceImpl.class,HrmPayrollEmployeeMappingServiceImpl.class,HrmPayrollEmployeeAccess.class,ValidationAutoConfiguration.class})
class HrmPayrollTrialServiceImplTest extends BaseDbUnitTest {
 @Resource private HrmPayrollTrialService service;
 @Resource private HrmPayrollCalculationService calculation;
 @Resource private HrmPayrollEligibilityService eligibility;
 @Resource private HrmPayrollTrialBatchMapper batches;
 @Resource private HrmPayrollTrialRunMapper runs;
 @Resource private HrmPayrollTrialPersonMapper people;
 @Resource private javax.sql.DataSource dataSource;
 @MockBean private PermissionApi permissions;
 @MockBean private AdminUserApi users;
 private JdbcTemplate jdbc; private Long definition;
 private final LocalDate start=LocalDate.of(2026,10,1),end=LocalDate.of(2026,10,31);
 @BeforeEach void fixture() {
  jdbc=new JdbcTemplate(dataSource);context(1L);
  for(String p:Arrays.asList("hrm:employee:query","hrm:payroll:trial:query","hrm:payroll:calculation:query","hrm:payroll:eligibility:query","hrm:payroll:trial:maintain","hrm:payroll:trial:execute"))when(permissions.hasAnyPermissions(10L,p)).thenReturn(true);
  when(users.getUser(10L)).thenReturn(new AdminUserRespDTO().setId(10L).setNickname("合成试算人员"));scope(new DeptDataPermissionRespDTO().setAll(true));
  jdbc.update("INSERT INTO hrm_employee(id,name,job_number,dept_id,user_id,status,tenant_id) VALUES(101,'合成人员 A','001',11,10,20,1),(102,'合成人员 B','001',22,20,30,1),(103,'无归属人员','003',NULL,NULL,20,1),(999,'其他租户','001',11,10,20,999)");
  definition=definition("RULE");qualify(101L,"INCLUDED");
 }
 private void context(Long tenant) { TenantContextHolder.setTenantId(tenant);SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new LoginUser().setId(10L).setTenantId(tenant).setUserType(2),null,Collections.emptyList())); }
 @AfterEach void clear() { TenantContextHolder.clear();SecurityContextHolder.clearContext(); }
 private void scope(DeptDataPermissionRespDTO value) { when(permissions.getDeptDataPermission(10L)).thenReturn(value); }
 private Long definition(String code) {
  Long id=calculation.create(new HrmPayrollCalculationSaveReqVO().setCode(code).setTitle("合成常规工资").setScopeCode("QA").setApplicableScope("合成声明主体").setOwnerName("QA").setReference("合成技术依据").setEffectiveFrom(start).setEffectiveTo(end).setProgram(HrmPayrollWageTemplate.program()));
  calculation.review(new HrmPayrollCalculationReviewReqVO().setId(id).setRevision(1).setAction("confirm").setEvidence("合成核对"));return id;
 }
 private Long qualify(Long employee,String qualification) {
  Long id=eligibility.create(new HrmPayrollEligibilitySaveReqVO().setEntityCode("QA").setEntityName("合成主体").setEmployeeId(employee).setQualification(qualification).setOwnerName("QA").setReference("合成资格依据").setReason("合成决策").setEffectiveFrom(start).setEffectiveTo(end));
  eligibility.review(new HrmPayrollEligibilityReviewReqVO().setId(id).setRevision(1).setAction("confirm").setEvidence("合成核对"));return id;
 }
 private HrmPayrollTrialConfigVO.PersonInput person(Long id) { return new HrmPayrollTrialConfigVO.PersonInput().setEmployeeId(id).setInputReference("合成金额来源").setInputs(new LinkedHashMap<>(HrmPayrollWageTemplate.program().getCases().get(0).getInputs())); }
 private HrmPayrollTrialSaveReqVO draft(String code) {
  return new HrmPayrollTrialSaveReqVO().setCode(code).setTitle("合成工资试算").setEntityCode("QA").setEntityName("合成主体").setPeriodStart(start).setPeriodEnd(end).setDefinitionId(definition).setOwnerName("QA").setReference("合成工资口径").setConfiguration(new HrmPayrollTrialConfigVO().setPeople(new ArrayList<>(Collections.singletonList(person(101L)))));
 }
 private HrmPayrollTrialSaveReqVO edit(Long id) {
  HrmPayrollTrialRespVO row=service.get(id);return BeanUtils.toBean(row,HrmPayrollTrialSaveReqVO.class).setConfiguration(JsonUtils.parseObject(JsonUtils.toJsonString(row.getConfiguration()),HrmPayrollTrialConfigVO.class));
 }
 private HrmPayrollTrialExecuteReqVO command(Long id,String key) { HrmPayrollTrialCheckVO check=service.check(id);assertTrue(check.getReady(),JsonUtils.toJsonString(check));return new HrmPayrollTrialExecuteReqVO().setBatchId(id).setRevision(check.getRevision()).setSourceHash(check.getSourceHash()).setRequestKey(key); }
 private HrmPayrollTrialRunRespVO execute(Long id,String key) { return service.execute(command(id,key)); }
 private HrmPayrollTrialPageReqVO page() { HrmPayrollTrialPageReqVO q=new HrmPayrollTrialPageReqVO();q.setPageSize(100);return q; }
 private void invalid(Runnable work) { assertEquals(PAYROLL_TRIAL_INVALID.getCode(),assertThrows(ServiceException.class,work::run).getCode()); }
 private void issue(Long id,String code) { HrmPayrollTrialCheckVO c=service.check(id);assertFalse(c.getReady());assertTrue(c.getIssues().stream().anyMatch(i->code.equals(i.getCode()))||c.getPeople().stream().flatMap(p->p.getIssues().stream()).anyMatch(i->code.equals(i.getCode())),JsonUtils.toJsonString(c)); }

 @Test void creationOwnsRevisionStatusAndCapturedIdentityNotJobNumber() {
  Long id=service.create(draft("OWN").setId(900L).setRevision(99));HrmPayrollTrialRespVO row=service.get(id);
  assertNotEquals(900L,id);assertEquals(1,row.getRevision());assertEquals(0,row.getStatus());assertEquals(101L,row.getConfiguration().getPeople().get(0).getEmployeeId());assertNotNull(row.getConfiguration().getPeople().get(0).getEmployeeFingerprint());assertEquals(1,people.selectCount());
  assertServiceException(()->service.create(draft("OWN")),PAYROLL_TRIAL_DUPLICATE);
 }
 @Test void regularAmountsHaveExactBalanceAndTotalsAndFullExplanation() {
  HrmPayrollTrialRunRespVO run=execute(service.create(draft("MONEY")),"money-first");
  assertEquals("7800.00",run.getResult().getTotals().get("gross"));assertEquals("1250.00",run.getResult().getTotals().get("deductions"));assertEquals("350.00",run.getResult().getTotals().get("tax"));assertEquals("6200.00",run.getResult().getTotals().get("net"));
  assertEquals(4,run.getResult().getPeople().get(0).getCalculation().getItems().size());assertFalse(run.getResult().getPeople().get(0).getCalculation().getItems().get(3).getSteps().isEmpty());assertEquals(definition,run.getResult().getDefinition().getId());assertEquals(1,run.getRunVersion());assertEquals(2,service.get(run.getBatchId()).getRevision());
 }
 @Test void missingTaxDoesNotBecomeZeroAndExplicitZeroWorks() {
  HrmPayrollTrialSaveReqVO req=draft("ZERO");req.getConfiguration().getPeople().get(0).getInputs().remove("withheldTax");Long id=service.create(req);issue(id,"INPUT_OR_AMOUNT_INVALID");
  HrmPayrollTrialSaveReqVO fixed=edit(id);fixed.getConfiguration().getPeople().get(0).getInputs().put("withheldTax","0.00");service.update(fixed);assertEquals("6550.00",execute(id,"explicit-zero").getResult().getTotals().get("net"));
 }
 @Test void rejectedDecimalFormatsExtraFieldsAndNegativeInputsCannotRun() {
  for(String value:Arrays.asList("","1e3","NaN","1,000.00","-0.01","1.001")) {
   HrmPayrollTrialSaveReqVO req=draft("BAD"+Math.abs(value.hashCode()));req.getConfiguration().getPeople().get(0).getInputs().put("withheldTax",value);Long id=service.create(req);issue(id,"INPUT_OR_AMOUNT_INVALID");
  }
  HrmPayrollTrialSaveReqVO req=draft("EXTRA");req.getConfiguration().getPeople().get(0).getInputs().put("unknown","0");issue(service.create(req),"INPUT_OR_AMOUNT_INVALID");assertEquals(0,runs.selectCount());
 }
 @Test void jsonNumbersBooleansAndNullAreNotSilentlyCoercedToMoney() {
  for(String value:Arrays.asList("1.00","true","[]","{}"))assertThrows(Exception.class,()->JsonUtils.parseObject("{\"employeeId\":101,\"inputs\":{\"tax\":"+value+"}}",HrmPayrollTrialConfigVO.PersonInput.class));
 }
 @Test void nullInputIsMissingAndCannotBeSavedAsZero() {
  HrmPayrollTrialSaveReqVO req=draft("NULL-MONEY");req.getConfiguration().getPeople().get(0).getInputs().put("withheldTax",null);invalid(()->service.create(req));assertEquals(0,batches.selectCount());
 }
 @Test void metadataAndPerPersonEvidenceAreRequiredOnlyBeforeExecution() {
  Long id=service.create(draft("META").setOwnerName(null).setReference(null));issue(id,"BATCH_BASIS_MISSING");
  HrmPayrollTrialSaveReqVO req=edit(id).setOwnerName("QA").setReference("合成依据");req.getConfiguration().getPeople().get(0).setInputReference(null);service.update(req);issue(id,"INPUT_BASIS_MISSING");
 }
 @Test void unknownQualificationsBlockWhereasExcludedPeopleHaveNoZeroSalary() {
  HrmPayrollTrialSaveReqVO req=draft("EXCLUDED");req.getConfiguration().getPeople().add(person(102L).setInputs(Collections.emptyMap()).setInputReference(null));Long id=service.create(req);issue(id,"UNRESOLVED_QUALIFICATION");
  qualify(102L,"EXCLUDED");HrmPayrollTrialCheckVO c=service.check(id);assertTrue(c.getReady());assertEquals(1,c.getExcludedCount());
  HrmPayrollTrialResultVO result=execute(id,"with-excluded").getResult();assertEquals("6200.00",result.getTotals().get("net"));assertNull(result.getPeople().get(1).getCalculation());assertTrue(result.getPeople().get(1).getAmounts().isEmpty());assertEquals("EXCLUDED",result.getPeople().get(1).getState());
 }
 @Test void entirelyExcludedBatchCannotCreateEmptyWageVersion() {
  qualify(102L,"EXCLUDED");HrmPayrollTrialSaveReqVO req=draft("EMPTY");req.getConfiguration().setPeople(Collections.singletonList(person(102L)));issue(service.create(req),"NO_INCLUDED_PEOPLE");assertEquals(0,runs.selectCount());
 }
 @Test void monthlyAndCustomPeriodsAreExplicitAndBounded() {
  invalid(()->service.create(draft("PART").setPeriodStart(start.plusDays(1))));invalid(()->service.create(draft("BACK").setPeriodType("CUSTOM").setPeriodEnd(start.minusDays(1))));
  invalid(()->service.create(draft("LONG").setPeriodType("CUSTOM").setPeriodEnd(start.plusDays(366))));
  Long id=service.create(draft("CUSTOM").setPeriodType("CUSTOM").setPeriodStart(start.plusDays(1)));assertTrue(service.check(id).getReady());
 }
 @Test void draftOrRetiredRuleAndPartialPeriodCannotBeExecuted() {
  Long rule=calculation.create(BeanUtils.toBean(calculation.get(definition),HrmPayrollCalculationSaveReqVO.class).setCode("DRAFT"));issue(service.create(draft("DRAFT-RULE").setDefinitionId(rule)),"DEFINITION_NOT_EFFECTIVE");
  Long id=service.create(draft("PARTIAL").setPeriodType("CUSTOM").setPeriodEnd(end.plusDays(1)));issue(id,"DEFINITION_NOT_EFFECTIVE");
  calculation.review(new HrmPayrollCalculationReviewReqVO().setId(definition).setRevision(calculation.get(definition).getRevision()).setAction("retire").setEvidence("合成停用"));issue(service.create(draft("RETIRED")),"DEFINITION_NOT_EFFECTIVE");
 }
 @Test void fourBindingsMustBeDistinctCnyTwoDecimalOutputs() {
  HrmPayrollTrialSaveReqVO req=draft("BIND");req.getConfiguration().getRoles().setNet("tax");issue(service.create(req),"OUTPUT_BINDING_INVALID");
  req=draft("UNKNOWN-BIND");req.getConfiguration().getRoles().setTax("unknown");issue(service.create(req),"OUTPUT_BINDING_INVALID");
  jdbc.update("UPDATE hrm_payroll_calculation_definition SET program_json=REPLACE(program_json,'\"unit\":\"CNY\"','\"unit\":\"USD\"') WHERE id=?",definition);issue(service.create(draft("UNIT")),"OUTPUT_BINDING_INVALID");
 }
 @Test void netCannotBeNegativeOrDisagreeWithRoundedComponents() {
  HrmPayrollTrialSaveReqVO req=draft("NEGATIVE-NET");req.getConfiguration().getPeople().get(0).getInputs().put("withheldTax","99999.00");issue(service.create(req),"INPUT_OR_AMOUNT_INVALID");
  HrmPayrollCalculationSaveReqVO def=BeanUtils.toBean(calculation.get(definition),HrmPayrollCalculationSaveReqVO.class).setCode("BAD-BALANCE");def.getProgram().getItems().get(3).setExpression("gross - deductions - tax + 1");for(HrmPayrollCalculationSpecVO.BusinessCase c:def.getProgram().getCases())c.getExpected().put("net",new java.math.BigDecimal(c.getExpected().get("net")).add(java.math.BigDecimal.ONE).toPlainString());
  Long other=calculation.create(def);calculation.review(new HrmPayrollCalculationReviewReqVO().setId(other).setRevision(1).setAction("confirm").setEvidence("合成规则反例"));issue(service.create(draft("BALANCE").setDefinitionId(other)),"INPUT_OR_AMOUNT_INVALID");
 }
 @Test void duplicateIdsNullConfigurationAndUnboundedRosterAreRejected() {
  HrmPayrollTrialSaveReqVO req=draft("DUP-PERSON");req.getConfiguration().getPeople().add(person(101L));invalid(()->service.create(req));
  invalid(()->service.create(draft("NULL").setConfiguration(null)));HrmPayrollTrialSaveReqVO empty=draft("EMPTY-PERSON");empty.getConfiguration().setPeople(Collections.emptyList());invalid(()->service.create(empty));
  HrmPayrollTrialSaveReqVO nul=draft("NULL-PERSON");nul.getConfiguration().setPeople(Collections.singletonList(null));invalid(()->service.create(nul));assertEquals(0,batches.selectCount());
 }
 @Test void clientSnapshotCannotOverrideEmployeeAndStaleFingerprintIsRejected() {
  HrmPayrollTrialSaveReqVO req=draft("FINGERPRINT");req.getConfiguration().getPeople().get(0).setEmployeeFingerprint(String.join("",Collections.nCopies(64,"a")));invalid(()->service.create(req));
  assertServiceException(()->service.create(draft("OTHER-TENANT-PERSON").setConfiguration(new HrmPayrollTrialConfigVO().setPeople(Collections.singletonList(person(999L))))),PAYROLL_MAPPING_NOT_EXISTS);
 }
 @Test void sourceChangesBlockNewCalculationButOldVersionStaysReplayable() {
  Long id=service.create(draft("DRIFT"));HrmPayrollTrialExecuteReqVO cmd=command(id,"drift-first");HrmPayrollTrialRunRespVO run=service.execute(cmd);String saved=JsonUtils.toJsonString(run.getResult());
  jdbc.update("UPDATE hrm_employee SET status=30 WHERE id=101");issue(id,"PERSON_CHANGED");assertEquals(run.getId(),service.execute(cmd).getId());assertEquals(saved,JsonUtils.toJsonString(service.run(run.getId()).getResult()));
 }
 @Test void preflightHashIsStableButSourceRevisionChangeIsNotTrusted() {
  Long id=service.create(draft("HASH"));HrmPayrollTrialExecuteReqVO cmd=command(id,"hash-first");assertEquals(cmd.getSourceHash(),service.check(id).getSourceHash());
  jdbc.update("UPDATE hrm_payroll_employee_eligibility SET reference='改变合成依据' WHERE entity_code='QA' AND employee_id=101");assertTrue(service.check(id).getReady());assertServiceException(()->service.execute(cmd),PAYROLL_TRIAL_STALE);assertEquals(0,runs.selectCount());
 }
 @Test void executionIgnoresForgedHashAndRequiresCurrentDraftRevision() {
  Long id=service.create(draft("STALE"));HrmPayrollTrialExecuteReqVO cmd=command(id,"stale-first");assertServiceException(()->service.execute(new HrmPayrollTrialExecuteReqVO().setBatchId(id).setRevision(cmd.getRevision()).setRequestKey("forged-hash").setSourceHash(String.join("",Collections.nCopies(64,"a")))),PAYROLL_TRIAL_STALE);
  service.update(edit(id).setTitle("新版草稿"));assertServiceException(()->service.execute(cmd),PAYROLL_TRIAL_STALE);assertEquals(0,runs.selectCount());
 }
 @Test void idempotentRetryReturnsSameImmutableVersionAndConflictingKeyIsRejected() {
  Long id=service.create(draft("RETRY"));HrmPayrollTrialExecuteReqVO cmd=command(id,"retry-command");Long run=service.execute(cmd).getId();assertEquals(run,service.execute(cmd).getId());assertEquals(1,runs.selectCount());
  HrmPayrollTrialExecuteReqVO changed=command(id,"retry-command");assertServiceException(()->service.execute(changed),PAYROLL_TRIAL_REQUEST_CONFLICT);assertEquals(1,runs.selectCount());
 }
 @Test void editingInvalidatesCurrentRunPreservesHistoryAndComparisonShowsMoneyDeltas() {
  Long id=service.create(draft("VERSIONS"));HrmPayrollTrialRunRespVO first=execute(id,"version-first");String saved=JsonUtils.toJsonString(first.getResult());
  HrmPayrollTrialSaveReqVO req=edit(id);req.getConfiguration().getPeople().get(0).getInputs().put("baseSalary","6100.00");service.update(req);assertNull(service.get(id).getCurrentRunId());assertEquals(first.getId(),service.get(id).getLatestRunId());assertEquals(0,service.get(id).getStatus());
  HrmPayrollTrialRunRespVO second=execute(id,"version-second");assertEquals(2,second.getRunVersion());assertEquals(saved,JsonUtils.toJsonString(service.run(first.getId()).getResult()));
  HrmPayrollTrialCompareVO diff=service.compare(first.getId(),second.getId());assertEquals("100.00",diff.getTotalDifferences().get("net"));assertEquals("AMOUNTS_CHANGED",diff.getPeople().get(0).getChange());assertEquals(2,service.runs(id).size());assertNull(service.runs(id).get(0).getResult());assertEquals(4,service.history(id).size());
 }
 @Test void rosterChangesRemainVisibleAsAddedRemovedRatherThanZeroAmounts() {
  qualify(102L,"INCLUDED");Long id=service.create(draft("ROSTER"));Long a=execute(id,"roster-first").getId();HrmPayrollTrialSaveReqVO req=edit(id);req.getConfiguration().setPeople(Collections.singletonList(person(102L)));service.update(req);Long b=execute(id,"roster-second").getId();
  HrmPayrollTrialCompareVO diff=service.compare(a,b);assertEquals("REMOVED",diff.getPeople().get(0).getChange());assertNull(diff.getPeople().get(0).getRightAmounts());assertEquals("ADDED",diff.getPeople().get(1).getChange());assertNull(diff.getPeople().get(1).getLeftAmounts());assertEquals(2,people.selectCount());
 }
 @Test void crossBatchComparisonAndChangingFixedBatchIdentityAreRejected() {
  Long a=service.create(draft("FIXED-A")),b=service.create(draft("FIXED-B"));invalid(()->service.update(edit(a).setEntityCode("OTHER")));invalid(()->service.update(edit(a).setPeriodEnd(end.minusDays(1))));
  invalid(()->service.compare(execute(a,"fixed-a-run").getId(),execute(b,"fixed-b-run").getId()));
 }
 @Test void tenantIsolationAppliesToEveryBatchAndRunEndpoint() {
  Long id=service.create(draft("TENANT")),run=execute(id,"tenant-first").getId();context(999L);assertEquals(0,service.page(page()).getTotal());
  for(Runnable work:Arrays.<Runnable>asList(()->service.get(id),()->service.check(id),()->service.runs(id),()->service.run(run),()->service.history(id),()->service.compare(run,run)))assertServiceException(work::run,PAYROLL_TRIAL_NOT_EXISTS);
 }
 @Test void departmentScopeRequiresEveryHistoricalAndCurrentEmployee() {
  qualify(102L,"INCLUDED");HrmPayrollTrialSaveReqVO req=draft("SCOPE");req.getConfiguration().getPeople().add(person(102L));Long id=service.create(req);Long run=execute(id,"scope-first").getId();
  scope(new DeptDataPermissionRespDTO().setDeptIds(new HashSet<>(Collections.singletonList(11L))));assertEquals(0,service.page(page()).getTotal());assertServiceException(()->service.get(id),PAYROLL_TRIAL_NOT_EXISTS);assertServiceException(()->service.run(run),PAYROLL_TRIAL_NOT_EXISTS);
  scope(new DeptDataPermissionRespDTO().setAll(true));req=edit(id);req.getConfiguration().setPeople(Collections.singletonList(person(101L)));service.update(req);
  scope(new DeptDataPermissionRespDTO().setDeptIds(new HashSet<>(Collections.singletonList(11L))));assertEquals(0,service.page(page()).getTotal());assertServiceException(()->service.get(id),PAYROLL_TRIAL_NOT_EXISTS);
 }
 @Test void capturedAndCurrentDepartmentDriftAndSqlNullCannotLeakBatches() {
  Long id=service.create(draft("DEPT"));scope(new DeptDataPermissionRespDTO().setDeptIds(new HashSet<>(Collections.singletonList(11L))));assertEquals(1,service.page(page()).getTotal());
  jdbc.update("UPDATE hrm_employee SET dept_id=22 WHERE id=101");assertEquals(0,service.page(page()).getTotal());assertServiceException(()->service.get(id),PAYROLL_TRIAL_NOT_EXISTS);
  scope(new DeptDataPermissionRespDTO().setAll(true));HrmPayrollTrialSaveReqVO req=draft("NO-DEPT");req.getConfiguration().setPeople(Collections.singletonList(person(103L)));Long other=service.create(req);
  scope(new DeptDataPermissionRespDTO().setDeptIds(new HashSet<>(Arrays.asList(11L,22L))));assertEquals(1,service.page(page()).getTotal());assertServiceException(()->service.get(other),PAYROLL_TRIAL_NOT_EXISTS);
 }
 @Test void selfAndEmptyScopeCannotReadOtherOrNullUser() {
  Long id=service.create(draft("SELF"));scope(new DeptDataPermissionRespDTO().setSelf(true));assertEquals(1,service.page(page()).getTotal());
  jdbc.update("UPDATE hrm_employee SET user_id=NULL WHERE id=101");assertEquals(0,service.page(page()).getTotal());assertServiceException(()->service.get(id),PAYROLL_TRIAL_NOT_EXISTS);
  scope(new DeptDataPermissionRespDTO());assertEquals(0,service.page(page()).getTotal());
 }
 @Test void allScopeCanReplayHistoryAfterPersonnelDeletionButScopedReadersCannot() {
  Long id=service.create(draft("DELETED")),run=execute(id,"deleted-first").getId();jdbc.update("UPDATE hrm_employee SET deleted=TRUE WHERE id=101");assertEquals(run,service.run(run).getId());issue(id,"PERSON_UNAVAILABLE");
  scope(new DeptDataPermissionRespDTO().setSelf(true));assertEquals(0,service.page(page()).getTotal());assertServiceException(()->service.run(run),PAYROLL_TRIAL_NOT_EXISTS);
 }
 @Test void eachDependencyPermissionIsRequiredAndActionPermissionsAreSeparate() {
  Long id=service.create(draft("PERMISSION"));for(String p:Arrays.asList("hrm:payroll:trial:query","hrm:payroll:calculation:query","hrm:payroll:eligibility:query","hrm:employee:query")) {
   when(permissions.hasAnyPermissions(10L,p)).thenReturn(false);assertServiceException(()->service.get(id),PAYROLL_TRIAL_PERMISSION);assertServiceException(()->service.page(page()),PAYROLL_TRIAL_PERMISSION);when(permissions.hasAnyPermissions(10L,p)).thenReturn(true);
  }
  when(permissions.hasAnyPermissions(10L,"hrm:payroll:trial:maintain")).thenReturn(false);assertServiceException(()->service.update(edit(id)),PAYROLL_TRIAL_PERMISSION);
  HrmPayrollTrialExecuteReqVO cmd=command(id,"permission-first");when(permissions.hasAnyPermissions(10L,"hrm:payroll:trial:execute")).thenReturn(false);assertServiceException(()->service.execute(cmd),PAYROLL_TRIAL_PERMISSION);assertEquals(0,runs.selectCount());
 }
 @Test void pageAndRunListOmitHeavySensitiveSnapshots() {
  Long id=service.create(draft("LIST"));execute(id,"list-first");assertNull(service.page(page()).getList().get(0).getConfiguration());assertNull(service.page(page()).getList().get(0).getReference());assertNull(service.runs(id).get(0).getResult());
 }
 @Test void simultaneousRetryAllocatesOneVersionAndSingleAuditAction() throws Exception {
  Long id=service.create(draft("CONCURRENT"));HrmPayrollTrialExecuteReqVO cmd=command(id,"concurrent-first");ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch ready=new CountDownLatch(2),go=new CountDownLatch(1);
  Callable<Long> work=()->{context(1L);try{ready.countDown();assertTrue(go.await(10,TimeUnit.SECONDS));return service.execute(cmd).getId();}finally{clear();}};
  try { Future<Long> a=pool.submit(work),b=pool.submit(work);assertTrue(ready.await(10,TimeUnit.SECONDS));go.countDown();assertEquals(a.get(20,TimeUnit.SECONDS),b.get(20,TimeUnit.SECONDS));assertEquals(1,runs.selectCount());assertEquals(2,service.history(id).size()); }finally{pool.shutdownNow();}
 }
 @Test void distinctCommandsAtSameRevisionSerializeAndOnlyOneCommits() throws Exception {
  Long id=service.create(draft("SERIAL"));HrmPayrollTrialExecuteReqVO a=command(id,"serial-command-a"),b=command(id,"serial-command-b");ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch go=new CountDownLatch(1);
  try { List<Future<Integer>> outcomes=new ArrayList<>();for(HrmPayrollTrialExecuteReqVO cmd:Arrays.asList(a,b))outcomes.add(pool.submit(()->{context(1L);try{go.await();service.execute(cmd);return 0;}catch(ServiceException e){return e.getCode();}finally{clear();}}));go.countDown();List<Integer> codes=new ArrayList<>();for(Future<Integer> f:outcomes)codes.add(f.get(20,TimeUnit.SECONDS));assertTrue(codes.contains(0));assertTrue(codes.contains(PAYROLL_TRIAL_STALE.getCode()));assertEquals(1,runs.selectCount());}finally{pool.shutdownNow();}
 }
 @Test void executionDoesNotMutateLegacyWagesSlipsInsuranceOrPersonnel() {
  Map<String,Integer> before=new HashMap<>();for(String t:Arrays.asList("hrm_employee","hrm_salary_month_record","hrm_salary_month_employee_record","hrm_salary_slip","hrm_salary_slip_send_record","hrm_insurance_month_record","hrm_insurance_month_employee_record")) { before.put(t,jdbc.queryForObject("SELECT COUNT(*) FROM "+t,Integer.class)); }
  execute(service.create(draft("NO-LEGACY-WRITE")),"legacy-first");before.forEach((t,count)->assertEquals(count,jdbc.queryForObject("SELECT COUNT(*) FROM "+t,Integer.class)));assertEquals(20,jdbc.queryForObject("SELECT status FROM hrm_employee WHERE id=101",Integer.class));
 }
}
