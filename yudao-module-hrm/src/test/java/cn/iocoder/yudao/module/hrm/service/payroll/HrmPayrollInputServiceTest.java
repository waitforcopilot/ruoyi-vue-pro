package cn.iocoder.yudao.module.hrm.service.payroll;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.salary.monthrecord.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.employee.info.HrmEmployeeMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.salary.monthrecord.*;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import javax.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

@Import({
  HrmPayrollInputService.class,
  HrmPayrollInputImportService.class,
  HrmPayrollEntityService.class,
  HrmPayrollScopeService.class,
  org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration.class
})
class HrmPayrollInputServiceTest extends BaseDbUnitTest {
  @Resource private HrmPayrollInputService service;
  @Resource private HrmPayrollInputImportService imports;
  @Resource private HrmPayrollEntityService entities;
  @Resource private HrmEmployeeMapper employees;
  @Resource private HrmSalaryMonthRecordMapper batches;
  @Resource private HrmSalaryMonthEmployeeRecordMapper employeeRecords;
  @Resource private HrmPayrollInputMapper records;
  @Resource private HrmPayrollInputImportMapper imported;
  @MockBean private AdminUserApi users;
  @MockBean private PermissionApi permissions;
  @MockBean private DeptApi departments;
  @MockBean private HrmPayrollWindowService window;
  @MockBean private HrmPayrollTaxDeductionService deductions;
  @MockBean private HrmPayrollTaxCashLedgerService cashLedger;
  @MockBean private HrmPayrollWorkService work;
  @MockBean private HrmPayrollAttendanceService attendance;
  @MockBean private cn.iocoder.yudao.module.oa.api.payroll.OaPayrollSourceApi oa;
  private Long entityId, employeeId, batchId;

  @BeforeEach
  void setup() {
    entityId =
        entities.create(
            new HrmPayrollEntityReqVO()
                .setCode("A")
                .setName("甲法人")
                .setTaxRegistrationNumber("914400000000000001")
                .setEvidence("登记资料"),
            1L);
    entities.grant(
        new HrmPayrollEntityReqVO.Grant()
            .setEntityId(entityId)
            .setUserId(2L)
            .setActions(new HashSet<>(Arrays.asList("READ", "REVIEW", "APPROVE")))
            .setEffectiveFrom(LocalDate.now().minusDays(1))
            .setEffectiveTo(LocalDate.now().plusYears(1))
            .setReason("复核职责"),
        1L);
    entities.approve(entityId, "核实", 2L);
    employeeId = employee("EMP-1");
    HrmSalaryMonthRecordDO b =
        new HrmSalaryMonthRecordDO()
            .setEntityId(entityId)
            .setYear(2026)
            .setMonth(10)
            .setStatus(5)
            .setTitle("十月工资")
            .setStartTime(LocalDate.of(2026, 10, 1).atStartOfDay())
            .setEndTime(LocalDate.of(2026, 10, 31).atStartOfDay());
    batches.insert(b);
    batchId = b.getId();
    when(window.intervals(any()))
        .thenReturn(
            Collections.singletonList(
                new LocalDate[] {LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)}));
    when(window.earningsForEntity(anyLong(), eq(entityId), any())).thenReturn(BigDecimal.ZERO);
    when(work.rules(anyLong()))
        .thenAnswer(
            call ->
                Collections.singletonList(
                    new HrmPayrollWorkRuleDO()
                        .setId(1L)
                        .setEmployeeId(call.getArgument(0))
                        .setStatus("APPROVED")
                        .setSystemType("STANDARD")
                        .setEntityId(entityId)
                        .setEffectiveFrom(LocalDate.of(2026, 1, 1))
                        .setEffectiveTo(LocalDate.of(2026, 12, 31))));
  }

  private Long employee(String job) {
    HrmEmployeeDO e =
        new HrmEmployeeDO()
            .setName(job)
            .setJobNumber(job)
            .setUserId(10L)
            .setEntryStatus(1)
            .setEntryTime(LocalDate.of(2026, 1, 1).atStartOfDay());
    employees.insert(e);
    Long id =
        entities.assign(
            new HrmPayrollEntityReqVO.Membership()
                .setEntityId(entityId)
                .setEmployeeId(e.getId())
                .setDeptId(1L)
                .setEmployeeCategory("正式")
                .setWorkCity("深圳")
                .setEffectiveFrom(LocalDate.of(2026, 1, 1))
                .setEffectiveTo(LocalDate.of(2026, 12, 31))
                .setEvidence("归属依据"),
            1L);
    entities.approveMembership(id, "核实", 2L);
    return e.getId();
  }

  private HrmPayrollInputReqVO request(Long employee, String kind, String mode) {
    return new HrmPayrollInputReqVO()
        .setBatchId(batchId)
        .setEmployeeId(employee)
        .setKind(kind)
        .setMode(mode)
        .setEvidence("业务来源核实与明确零发生额证明");
  }

  private Map<String, BigDecimal> attendanceValues() {
    Map<String, BigDecimal> v = new LinkedHashMap<>();
    HrmPayrollInputService.ATTENDANCE_FIELDS.forEach(f -> v.put(f, BigDecimal.ZERO));
    v.put("ACTUAL_DAYS", new BigDecimal("20"));
    v.put("NEED_DAYS", new BigDecimal("22"));
    return v;
  }

  private Long approved(HrmPayrollInputReqVO r) {
    Long id = service.submit(r, 1L);
    service.review(id, true, "数据与来源已核实", 2L);
    return id;
  }

  @Test
  void cashYearInputUsesPlannedPaymentDateAndLaterCashEvidenceMakesItStale() {
    batches.updateById(new HrmSalaryMonthRecordDO().setId(batchId).setExpectedPaymentDate(LocalDate.of(2026,10,14)));
    cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollTaxCashLedgerVO ledger=
        new cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollTaxCashLedgerVO()
        .setReady(true).setIncome(new BigDecimal("10000")).setBasicDeduction(new BigDecimal("5000"))
        .setSocialDeduction(new BigDecimal("1000")).setWithheld(new BigDecimal("120"));
    when(cashLedger.forApprovedPayrollForBatch(eq(entityId),eq(employeeId),eq(2026),any(),any())).thenReturn(ledger);
    Long id=approved(request(employeeId,"CUMULATIVE_TAX","CASH"));
    assertEquals(new BigDecimal("120"),service.values(records.selectById(id)).get("250105"));
    ledger.setWithheld(new BigDecimal("150"));
    assertTrue(service.readiness(batchId,1L).stream().anyMatch(r->"CUMULATIVE_TAX".equals(r.getKind()) && "STALE".equals(r.getStatus())));
  }

  @Test
  void approvedDeductionDeclarationSynchronizesAndChangedVersionMakesInputStale() {
    Map<String,BigDecimal> values=new TreeMap<>(); HrmPayrollInputService.DEDUCTION_FIELDS.forEach(code->values.put(code,BigDecimal.ZERO));
    values.put("260106",new BigDecimal("20000"));
    HrmPayrollTaxDeductionDO declaration=new HrmPayrollTaxDeductionDO().setId(500L).setEntityId(entityId).setEmployeeId(employeeId).setYear(2026).setMonth(10).setVersion(1).setStatus("APPROVED");
    when(deductions.source(entityId,employeeId,2026,10)).thenReturn(declaration);
    when(deductions.values(declaration)).thenReturn(values);
    Long id=approved(request(employeeId,"DEDUCTIONS","SYNC"));
    assertEquals(new BigDecimal("20000"),service.values(records.selectById(id)).get("260106"));
    assertTrue(service.readiness(batchId,1L).stream().anyMatch(r->"DEDUCTIONS".equals(r.getKind()) && "APPROVED".equals(r.getStatus())));
    HrmPayrollTaxDeductionDO successor = new HrmPayrollTaxDeductionDO().setId(501L).setVersion(2);
    when(deductions.source(entityId,employeeId,2026,10)).thenReturn(successor);
    when(deductions.values(successor)).thenReturn(values);
    assertTrue(service.readiness(batchId,1L).stream().anyMatch(r->"DEDUCTIONS".equals(r.getKind()) && "STALE".equals(r.getStatus())));
  }

  @Test
  void declaredDeductionCannotBeBypassedWithZeroOrDifferentManualAmounts() {
    Map<String,BigDecimal> values=new TreeMap<>(); HrmPayrollInputService.DEDUCTION_FIELDS.forEach(code->values.put(code,BigDecimal.ZERO));
    values.put("260106",new BigDecimal("20000"));
    HrmPayrollTaxDeductionDO declaration=new HrmPayrollTaxDeductionDO().setId(500L);
    when(deductions.source(entityId,employeeId,2026,10)).thenReturn(declaration); when(deductions.values(declaration)).thenReturn(values);
    assertServiceException(()->service.submit(request(employeeId,"DEDUCTIONS","ZERO"),1L),PAYROLL_INPUT_INVALID);
    Map<String,BigDecimal> changed=new TreeMap<>(values);changed.put("260106",BigDecimal.ZERO);
    assertServiceException(()->service.submit(request(employeeId,"DEDUCTIONS","IMPORT").setValues(changed),1L),PAYROLL_INPUT_INVALID);
  }

  @Test
  void missingAndPendingInputsBlockWhileIndependentZeroConfirmationIsRequired() {
    assertEquals(4, service.readiness(batchId, 1L).size());
    assertTrue(
        service.readiness(batchId, 1L).stream().allMatch(r -> "MISSING".equals(r.getStatus())));
    assertServiceException(
        () ->
            service.forPayroll(
                batches.selectById(batchId), service.roster(batches.selectById(batchId)), 1L),
        PAYROLL_INPUT_INVALID);
    Long id = service.submit(request(employeeId, "DEDUCTIONS", "ZERO"), 1L);
    assertEquals("PENDING", records.selectById(id).getStatus());
    assertServiceException(() -> service.review(id, true, "确认", 1L), PAYROLL_SELF_APPROVAL);
    service.review(id, true, "独立核实", 2L);
    assertEquals(0, service.values(records.selectById(id)).get("260101").signum());
  }

  @Test
  void completeApprovedSourcesProvideActualNeedDaysAndProtectFreezeVersions() {
    approved(request(employeeId, "ATTENDANCE", "IMPORT").setValues(attendanceValues()));
    approved(request(employeeId, "OVERTIME", "ZERO"));
    approved(request(employeeId, "DEDUCTIONS", "ZERO"));
    approved(request(employeeId, "CUMULATIVE_TAX", "ZERO"));
    Map<Long, Map<String, HrmPayrollInputDO>> ready =
        service.forPayroll(
            batches.selectById(batchId), service.roster(batches.selectById(batchId)), 1L);
    assertEquals(
        new BigDecimal("20"),
        service.values(ready.get(employeeId).get("ATTENDANCE")).get("ACTUAL_DAYS"));
    assertEquals(
        new BigDecimal("22"),
        service.values(ready.get(employeeId).get("ATTENDANCE")).get("NEED_DAYS"));
    String snapshot = JsonUtils.toJsonString(Collections.singletonMap("approvedInputs", ready));
    service.validateInputSnapshot(snapshot);
    Long revision = service.submit(request(employeeId, "DEDUCTIONS", "ZERO"), 1L);
    assertEquals(2, records.selectById(revision).getVersion());
    assertServiceException(() -> service.validateInputSnapshot(snapshot), PAYROLL_INPUT_INVALID);
  }

  @Test
  void employeeCannotReviewOwnMandatoryPayrollInputsEvenWithEntityReviewGrant() {
    entities.grant(
        new HrmPayrollEntityReqVO.Grant()
            .setEntityId(entityId)
            .setUserId(10L)
            .setActions(new HashSet<>(Arrays.asList("READ", "REVIEW")))
            .setEffectiveFrom(LocalDate.now().minusDays(1))
            .setEffectiveTo(LocalDate.now().plusYears(1))
            .setReason("工资输入复核角色"),
        1L);
    for (String kind : HrmPayrollInputService.KINDS) {
      HrmPayrollInputReqVO request =
          request(employeeId, kind, "ATTENDANCE".equals(kind) ? "IMPORT" : "ZERO");
      if ("ATTENDANCE".equals(kind)) request.setValues(attendanceValues());
      Long id = service.submit(request, 1L);
      assertServiceException(() -> service.review(id, true, "本人批准", 10L), PAYROLL_SELF_APPROVAL);
      assertServiceException(() -> service.review(id, false, "本人驳回", 10L), PAYROLL_SELF_APPROVAL);
      assertEquals("PENDING", records.selectById(id).getStatus());
      service.review(id, true, "独立复核", 2L);
      assertEquals("APPROVED", records.selectById(id).getStatus());
    }
  }

  @Test
  void inputReviewRechecksCurrentEmployeeAccountForRejectionToo() {
    Long id = service.submit(request(employeeId, "DEDUCTIONS", "ZERO"), 1L);
    HrmEmployeeDO employee = employees.selectById(employeeId);
    employee.setUserId(2L);
    employees.updateById(employee);
    assertServiceException(() -> service.review(id, false, "本人取消旧输入", 2L), PAYROLL_SELF_APPROVAL);
    assertEquals("PENDING", records.selectById(id).getStatus());
  }

  @Test
  void attendanceBlankFieldsAndImplicitFullAttendanceAreRejected() {
    assertServiceException(
        () -> service.submit(request(employeeId, "ATTENDANCE", "ZERO"), 1L), PAYROLL_INPUT_INVALID);
    Map<String, BigDecimal> v = attendanceValues();
    v.remove("190105");
    assertServiceException(
        () -> service.submit(request(employeeId, "ATTENDANCE", "IMPORT").setValues(v), 1L),
        PAYROLL_INPUT_INVALID);
    assertTrue(records.selectList().isEmpty());
  }

  @Test
  void approvedOvertimeCannotBeDeclaredAsZeroOrSkippedWithoutRecognition() {
    cn.iocoder.yudao.module.oa.api.payroll.dto.OaPayrollOvertimeDTO approval =
        new cn.iocoder.yudao.module.oa.api.payroll.dto.OaPayrollOvertimeDTO()
            .setId(123L)
            .setStartTime(LocalDate.of(2026, 10, 3).atTime(18, 0))
            .setEndTime(LocalDate.of(2026, 10, 3).atTime(20, 0));
    when(oa.getApprovedOvertimeList(any(), any(), any()))
        .thenReturn(Collections.singletonList(approval));
    assertServiceException(
        () -> service.submit(request(employeeId, "OVERTIME", "ZERO"), 1L), PAYROLL_WORK_INVALID);
    assertTrue(records.selectList().isEmpty());
  }

  @Test
  void zeroOvertimeNeedsApprovedInstitutionAndSourceChangesInvalidateApproval() {
    Long id = approved(request(employeeId, "OVERTIME", "ZERO"));
    when(work.rules(anyLong())).thenReturn(Collections.emptyList());
    assertEquals(
        "STALE",
        service.readiness(batchId, 1L).stream()
            .filter(r -> r.getInputId() != null && r.getInputId().equals(id))
            .findFirst()
            .get()
            .getStatus());
    assertServiceException(
        () -> service.submit(request(employeeId, "OVERTIME", "ZERO"), 1L), PAYROLL_WORK_INVALID);
  }

  private HrmPayrollInputRowVO row(String job, String kind, String mode) {
    return new HrmPayrollInputRowVO()
        .setJobNumber(job)
        .setEntityCode("A")
        .setPeriodFrom("2026-10-01")
        .setPeriodTo("2026-10-31")
        .setKind(kind)
        .setMode(mode)
        .setEvidence("导入来源凭证");
  }

  private HrmPayrollInputImportDO preview(List<HrmPayrollInputRowVO> rows) {
    return imports.preview(
        batchId, "inputs.xlsx", String.join("", Collections.nCopies(64, "a")), rows, 1L);
  }

  @Test
  void exactRowErrorsAndWholeFileFailurePreventPartialApplication() {
    HrmPayrollInputImportDO batch =
        preview(
            Arrays.asList(
                row("EMP-1", "DEDUCTIONS", "ZERO"),
                row("EMP-1", "DEDUCTIONS", "ZERO"),
                row("UNKNOWN", "DEDUCTIONS", "ZERO"),
                row("EMP-1", "CUMULATIVE_TAX", "ZERO").setPeriodFrom("2026-09-01")));
    assertEquals("FAILED", batch.getStatus());
    List<HrmPayrollInsuranceImportService.RowError> errors =
        JsonUtils.parseArray(batch.getErrors(), HrmPayrollInsuranceImportService.RowError.class);
    assertEquals(3, errors.get(0).getRow());
    assertEquals(4, errors.get(1).getRow());
    assertEquals(5, errors.get(2).getRow());
    assertServiceException(() -> imports.commit(batch.getId(), 1L), PAYROLL_INPUT_INVALID);
    assertTrue(records.selectList().isEmpty());
  }

  @Test
  void concurrentPendingAfterPreviewRollsBackAllEmployeeProposals() {
    Long other = employee("EMP-2");
    HrmPayrollInputImportDO batch =
        preview(
            Arrays.asList(row("EMP-1", "DEDUCTIONS", "ZERO"), row("EMP-2", "DEDUCTIONS", "ZERO")));
    assertEquals("VALIDATED", batch.getStatus());
    Long pending = service.submit(request(other, "DEDUCTIONS", "ZERO"), 1L);
    assertServiceException(() -> imports.commit(batch.getId(), 1L), PAYROLL_INPUT_INVALID);
    assertEquals(1, records.selectList().size());
    assertEquals(pending, records.selectList().get(0).getId());
    assertEquals("VALIDATED", imported.selectById(batch.getId()).getStatus());
  }

  @Test
  void importCommitIsIdempotentAndStillRequiresIndependentReview() {
    HrmPayrollInputImportDO batch =
        preview(Collections.singletonList(row("EMP-1", "DEDUCTIONS", "ZERO")));
    List<Long> ids = imports.commit(batch.getId(), 1L);
    assertEquals(ids, imports.commit(batch.getId(), 1L));
    assertEquals(1, records.selectList().size());
    assertEquals("PENDING", records.selectById(ids.get(0)).getStatus());
    assertServiceException(() -> imports.get(batch.getId(), 99L), PAYROLL_ENTITY_INVALID);
  }

  @Test
  void oldScopedSnapshotsCannotFreezeWithoutRequiredConfirmations() {
    assertServiceException(
        () -> service.validateInputSnapshot("{\"entityScope\":{}}"), PAYROLL_INPUT_INVALID);
  }

  @Test
  void extremeScientificNotationIsRejectedBeforeSerializingAmounts() {
    Map<String, BigDecimal> v = attendanceValues();
    v.put("190101", new BigDecimal("1e2147483647"));
    assertServiceException(
        () -> service.submit(request(employeeId, "ATTENDANCE", "IMPORT").setValues(v), 1L),
        PAYROLL_INPUT_INVALID);
    assertTrue(records.selectList().isEmpty());
  }

  @Test
  void employmentBoundariesRestrictConfirmedDatesAndChangesInvalidateSnapshots() {
    HrmEmployeeDO employee = employees.selectById(employeeId);
    employee.setEntryTime(LocalDate.of(2026, 10, 15).atStartOfDay());
    employees.updateById(employee);
    Long id = approved(request(employeeId, "DEDUCTIONS", "ZERO"));
    String snapshot = records.selectById(id).getSourceSnapshot();
    List<LocalDate> confirmedDates =
        JsonUtils.parseObject(
            JsonUtils.parseTree(snapshot).get("dates").toString(),
            new com.fasterxml.jackson.core.type.TypeReference<List<LocalDate>>() {});
    assertEquals(LocalDate.of(2026, 10, 15), confirmedDates.get(0));
    employee.setLeaveTime(LocalDate.of(2026, 10, 25).atStartOfDay());
    employees.updateById(employee);
    assertEquals(
        "STALE",
        service.readiness(batchId, 1L).stream()
            .filter(r -> r.getInputId() != null && r.getInputId().equals(id))
            .findFirst()
            .get()
            .getStatus());
  }

  @Test
  void irregularAttendanceDoesNotApplyStandardClockPenalties() {
    HrmPayrollWorkRuleDO policy =
        new HrmPayrollWorkRuleDO()
            .setId(9L)
            .setEmployeeId(employeeId)
            .setEntityId(entityId)
            .setStatus("APPROVED")
            .setSystemType("IRREGULAR")
            .setEffectiveFrom(LocalDate.of(2026, 1, 1))
            .setEffectiveTo(LocalDate.of(2026, 12, 31));
    when(work.rules(employeeId)).thenReturn(Collections.singletonList(policy));
    cn.iocoder.yudao.module.hrm.controller.admin.attendance.vo.statistics
            .HrmAttendanceMonthRecordRespVO
        a =
            new cn.iocoder.yudao.module.hrm.controller.admin.attendance.vo.statistics
                    .HrmAttendanceMonthRecordRespVO()
                .setEmployeeId(employeeId)
                .setAttendDays(22)
                .setActualDays(new BigDecimal("20"))
                .setLateDeductAmount(BigDecimal.ONE)
                .setEarlyDeductAmount(new BigDecimal("2"))
                .setAbsenteeismDeductAmount(new BigDecimal("3"))
                .setMisscardDeductAmount(new BigDecimal("4"))
                .setAttendanceDeductAmount(new BigDecimal("15"));
    when(window.attendance(eq(employeeId), any())).thenReturn(a);
    Long id = approved(request(employeeId, "ATTENDANCE", "SYNC"));
    Map<String, BigDecimal> values = service.values(records.selectById(id));
    for (String code : Arrays.asList("190101", "190102", "190103", "190105"))
      assertEquals(0, values.get(code).signum());
    assertEquals(0, new BigDecimal("5").compareTo(values.get("190106")));
    policy.setSystemType("STANDARD");
    assertEquals(
        "STALE",
        service.readiness(batchId, 1L).stream()
            .filter(row -> id.equals(row.getInputId()))
            .findFirst()
            .get()
            .getStatus());
  }

  @Test
  void mixedWorkPoliciesOnlyUseStandardDatesForClockPenalties() {
    LocalDate split = LocalDate.of(2026, 10, 16);
    HrmPayrollWorkRuleDO standard =
        new HrmPayrollWorkRuleDO()
            .setId(1L)
            .setEmployeeId(employeeId)
            .setEntityId(entityId)
            .setStatus("APPROVED")
            .setSystemType("STANDARD")
            .setEffectiveFrom(LocalDate.of(2026, 1, 1))
            .setEffectiveTo(LocalDate.of(2026, 12, 31))
            .setSupersededFrom(split)
            .setSupersededBy(2L);
    HrmPayrollWorkRuleDO comprehensive =
        new HrmPayrollWorkRuleDO()
            .setId(2L)
            .setEmployeeId(employeeId)
            .setEntityId(entityId)
            .setStatus("APPROVED")
            .setSystemType("COMPREHENSIVE")
            .setEffectiveFrom(split)
            .setEffectiveTo(LocalDate.of(2026, 12, 31));
    when(work.rules(employeeId)).thenReturn(Arrays.asList(standard, comprehensive));
    cn.iocoder.yudao.module.hrm.controller.admin.attendance.vo.statistics
            .HrmAttendanceMonthRecordRespVO
        all =
            new cn.iocoder.yudao.module.hrm.controller.admin.attendance.vo.statistics
                    .HrmAttendanceMonthRecordRespVO()
                .setEmployeeId(employeeId)
                .setAttendDays(22)
                .setActualDays(new BigDecimal("20"))
                .setLateDeductAmount(new BigDecimal("10"))
                .setEarlyDeductAmount(BigDecimal.ZERO)
                .setAbsenteeismDeductAmount(BigDecimal.ZERO)
                .setMisscardDeductAmount(BigDecimal.ZERO)
                .setAttendanceDeductAmount(new BigDecimal("12"));
    cn.iocoder.yudao.module.hrm.controller.admin.attendance.vo.statistics
            .HrmAttendanceMonthRecordRespVO
        standardOnly =
            new cn.iocoder.yudao.module.hrm.controller.admin.attendance.vo.statistics
                    .HrmAttendanceMonthRecordRespVO()
                .setLateDeductAmount(new BigDecimal("3"))
                .setEarlyDeductAmount(BigDecimal.ZERO)
                .setAbsenteeismDeductAmount(BigDecimal.ZERO)
                .setMisscardDeductAmount(BigDecimal.ZERO);
    when(window.attendance(eq(employeeId), any()))
        .thenAnswer(
            call -> {
              Set<LocalDate> dates = call.getArgument(1);
              return dates.stream().anyMatch(date -> !date.isBefore(split)) ? all : standardOnly;
            });
    Long id = approved(request(employeeId, "ATTENDANCE", "SYNC"));
    Map<String, BigDecimal> values = service.values(records.selectById(id));
    assertEquals(0, new BigDecimal("3").compareTo(values.get("190101")));
    assertEquals(0, new BigDecimal("2").compareTo(values.get("190106")));
    assertEquals(0, new BigDecimal("20").compareTo(values.get("ACTUAL_DAYS")));
    verify(window, atLeastOnce())
        .attendance(
            eq(employeeId),
            argThat(
                dates ->
                    dates.size() == 15 && dates.stream().allMatch(date -> date.isBefore(split))));
  }

  @Test
  void attendanceInputRequiresExactlyOneEmployerWorkPolicyPerDay() {
    when(work.rules(employeeId)).thenReturn(Collections.emptyList());
    assertServiceException(
        () ->
            service.submit(
                request(employeeId, "ATTENDANCE", "IMPORT").setValues(attendanceValues()), 1L),
        PAYROLL_WORK_INVALID);
  }

  @Test
  void reopenedWorkVarianceMakesApprovedAttendanceInputStale() {
    Long id = approved(request(employeeId, "ATTENDANCE", "IMPORT").setValues(attendanceValues()));
    doThrow(
            cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception(
                PAYROLL_WORK_INVALID))
        .when(work)
        .requireVarianceReady(eq(entityId), eq(employeeId), any());
    assertEquals(
        "STALE",
        service.readiness(batchId, 1L).stream()
            .filter(row -> id.equals(row.getInputId()))
            .findFirst()
            .get()
            .getStatus());
    assertServiceException(
        () -> service.submit(request(employeeId, "OVERTIME", "ZERO"), 1L), PAYROLL_WORK_INVALID);
  }

  @Test
  void synchronizedAttendanceUsesActualValuesAndLaterChangesMakeConfirmationStale() {
    cn.iocoder.yudao.module.hrm.controller.admin.attendance.vo.statistics
            .HrmAttendanceMonthRecordRespVO
        a =
            new cn.iocoder.yudao.module.hrm.controller.admin.attendance.vo.statistics
                    .HrmAttendanceMonthRecordRespVO()
                .setEmployeeId(employeeId)
                .setAttendDays(22)
                .setActualDays(new BigDecimal("20"))
                .setLateDeductAmount(BigDecimal.ONE)
                .setEarlyDeductAmount(new BigDecimal("2"))
                .setAbsenteeismDeductAmount(new BigDecimal("3"))
                .setMisscardDeductAmount(new BigDecimal("4"))
                .setAttendanceDeductAmount(BigDecimal.TEN);
    when(window.attendance(eq(employeeId), any())).thenReturn(a);
    Long id = approved(request(employeeId, "ATTENDANCE", "SYNC"));
    assertEquals(new BigDecimal("20"), service.values(records.selectById(id)).get("ACTUAL_DAYS"));
    assertEquals(0, service.values(records.selectById(id)).get("190106").signum());
    a.setLateDeductAmount(new BigDecimal("2")).setAttendanceDeductAmount(new BigDecimal("11"));
    assertEquals(
        "STALE",
        service.readiness(batchId, 1L).stream()
            .filter(r -> r.getInputId() != null && r.getInputId().equals(id))
            .findFirst()
            .get()
            .getStatus());
  }

  @Test
  void paidSameEntityCumulativeSourcesCannotBeResetToZeroOrOverridden() {
    HrmSalaryMonthRecordDO paid =
        new HrmSalaryMonthRecordDO()
            .setEntityId(entityId)
            .setYear(2026)
            .setMonth(9)
            .setStatus(17)
            .setTitle("已发放九月工资")
            .setStartTime(LocalDate.of(2026, 9, 1).atStartOfDay())
            .setEndTime(LocalDate.of(2026, 9, 30).atStartOfDay());
    batches.insert(paid);
    List<HrmSalaryMonthEmployeeRecordDO.OptionValue> values = new ArrayList<>();
    int[] codes = {270101, 270102, 270103, 250105};
    String[] amounts = {"10000", "5000", "400", "0"};
    for (int j = 0; j < codes.length; j++)
      values.add(
          HrmSalaryMonthEmployeeRecordDO.OptionValue.builder()
              .code(codes[j])
              .name("历史累计")
              .value(new BigDecimal(amounts[j]))
              .build());
    HrmSalaryMonthEmployeeRecordDO result =
        HrmSalaryMonthEmployeeRecordDO.builder()
            .entityId(entityId)
            .employeeId(employeeId)
            .monthRecordId(paid.getId())
            .year(2026)
            .month(9)
            .personalTax(new BigDecimal("100"))
            .optionValues(values)
            .build();
    employeeRecords.insert(result);
    assertServiceException(
        () -> service.submit(request(employeeId, "CUMULATIVE_TAX", "ZERO"), 1L),
        PAYROLL_INPUT_INVALID);
    Long id = approved(request(employeeId, "CUMULATIVE_TAX", "SYNC"));
    assertEquals(
        0, new BigDecimal("100").compareTo(service.values(records.selectById(id)).get("250105")));
    Map<String, BigDecimal> changed = new LinkedHashMap<>(service.values(records.selectById(id)));
    changed.put("250105", BigDecimal.ONE);
    assertServiceException(
        () ->
            service.submit(request(employeeId, "CUMULATIVE_TAX", "IMPORT").setValues(changed), 1L),
        PAYROLL_INPUT_INVALID);
  }

  @Test
  void recognizedApprovalMatchesOriginalIdRatherThanDateQualifiedReference() {
    LocalDate date = LocalDate.of(2026, 10, 3);
    cn.iocoder.yudao.module.oa.api.payroll.dto.OaPayrollOvertimeDTO source =
        new cn.iocoder.yudao.module.oa.api.payroll.dto.OaPayrollOvertimeDTO()
            .setId(123L)
            .setApplicantUserId(10L)
            .setStartTime(date.atTime(18, 0))
            .setEndTime(date.atTime(20, 0))
            .setProcessInstanceId("approved-bpm-source");
    HrmPayrollTimeEntryDO entry =
        new HrmPayrollTimeEntryDO()
            .setId(1L)
            .setEmployeeId(employeeId)
            .setSourceApplyId(123L)
            .setEntityId(entityId)
            .setSourceRecordId("123:2026-10-03")
            .setStartTime(date.atTime(18, 0))
            .setEndTime(date.atTime(20, 0))
            .setRecognizedMinutes(120)
            .setAmount(new BigDecimal("100.00"));
    when(oa.getApprovedOvertimeList(any(), any(), any()))
        .thenReturn(Collections.singletonList(source));
    when(work.entries(eq(employeeId), any(), any())).thenReturn(Collections.singletonList(entry));
    when(window.earningsForEntity(eq(employeeId), eq(entityId), any()))
        .thenReturn(new BigDecimal("100.00"));
    Long id = approved(request(employeeId, "OVERTIME", "SYNC"));
    assertEquals(new BigDecimal("100.00"), service.values(records.selectById(id)).get("180101"));
  }

  @Test
  void foreignEmployerWorkRuleCannotMakeInputsReady() {
    Long employeeId = employee("FOREIGN-RULE");
    when(work.rules(employeeId))
        .thenReturn(
            Collections.singletonList(
                new HrmPayrollWorkRuleDO()
                    .setId(99L)
                    .setEntityId(entityId + 1)
                    .setEmployeeId(employeeId)
                    .setStatus("APPROVED")
                    .setSystemType("STANDARD")
                    .setEffectiveFrom(LocalDate.of(2026, 1, 1))
                    .setEffectiveTo(LocalDate.of(2026, 12, 31))));
    assertServiceException(
        () -> service.submit(request(employeeId, "OVERTIME", "ZERO"), 1L), PAYROLL_WORK_INVALID);
  }

  @Test
  void independentlyVerifiedZeroOvertimeCanBeSynchronizedButNotImplicitlyConfirmed() {
    Long employeeId = employee("VERIFIED-ZERO");
    LocalDate date = LocalDate.of(2026, 10, 3);
    cn.iocoder.yudao.module.oa.api.payroll.dto.OaPayrollOvertimeDTO source =
        new cn.iocoder.yudao.module.oa.api.payroll.dto.OaPayrollOvertimeDTO();
    source.setId(123L);
    source.setApplicantUserId(99L);
    source.setStartTime(date.atTime(18, 0));
    source.setEndTime(date.atTime(20, 0));
    HrmPayrollTimeEntryDO entry =
        new HrmPayrollTimeEntryDO()
            .setId(1L)
            .setEntityId(entityId)
            .setEmployeeId(employeeId)
            .setSourceApplyId(123L)
            .setSourceRecordId("123:2026-10-03")
            .setStartTime(date.atTime(18, 0))
            .setEndTime(date.atTime(20, 0))
            .setRecognizedMinutes(0)
            .setAmount(BigDecimal.ZERO)
            .setCompensation("VERIFIED_ZERO");
    when(oa.getApprovedOvertimeList(any(), any(), any()))
        .thenReturn(Collections.singletonList(source));
    when(work.entries(eq(employeeId), any(), any())).thenReturn(Collections.singletonList(entry));
    assertServiceException(
        () -> service.submit(request(employeeId, "OVERTIME", "ZERO"), 1L), PAYROLL_INPUT_INVALID);
    Long id = approved(request(employeeId, "OVERTIME", "SYNC"));
    assertEquals(BigDecimal.ZERO, service.values(records.selectById(id)).get("180101"));
    assertTrue(records.selectById(id).getSourceSnapshot().contains("VERIFIED_ZERO"));
  }
}
