package cn.iocoder.yudao.module.hrm.service.payroll;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollBonusReqVO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.salary.monthrecord.HrmSalaryMonthRecordDO;
import cn.iocoder.yudao.module.hrm.dal.mysql.employee.info.HrmEmployeeMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.HrmPayrollBonusMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.salary.monthrecord.HrmSalaryMonthRecordMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import javax.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

@Import({
  HrmPayrollBonusService.class,
  org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration.class
})
class HrmPayrollBonusServiceTest extends BaseDbUnitTest {
  @Resource private HrmPayrollBonusService service;
  @Resource private HrmPayrollBonusMapper bonuses;

  @Resource
  private cn.iocoder.yudao.module.hrm.dal.mysql.payroll.HrmPayrollSourceClaimMapper claims;

  @Resource private cn.iocoder.yudao.module.hrm.dal.mysql.payroll.HrmPayrollPaymentMapper payments;
  @Resource private cn.iocoder.yudao.module.hrm.dal.mysql.payroll.HrmPayrollRunMapper runs;
  @Resource private HrmEmployeeMapper employees;
  @Resource private HrmSalaryMonthRecordMapper batches;
  @MockBean private HrmPayrollScopeService scope;
  @MockBean private HrmPayrollTaxProfileService profiles;
  @MockBean private HrmPayrollTaxPolicyService policies;
  private Long employeeId, batchId;

  @BeforeEach
  void setup() {
    HrmEmployeeDO e = new HrmEmployeeDO().setName("奖金员工").setJobNumber("BONUS-1").setUserId(99L);
    employees.insert(e);
    employeeId = e.getId();
    HrmSalaryMonthRecordDO b =
        new HrmSalaryMonthRecordDO()
            .setEntityId(10L)
            .setStatus(5)
            .setYear(2026)
            .setMonth(10)
            .setExpectedPaymentDate(LocalDate.of(2026, 10, 15));
    batches.insert(b);
    batchId = b.getId();
    when(scope.payrollDates(any()))
        .thenReturn(
            Collections.singletonMap(employeeId, Collections.singleton(LocalDate.of(2026, 10, 1))));
    when(profiles.requireApproved(10L, employeeId, 2026))
        .thenReturn(new HrmPayrollTaxProfileDO().setId(123L).setResidentThisYear(true));
    when(policies.requireApproved(10L, 2026))
        .thenReturn(
            new HrmPayrollTaxPolicyDO()
                .setSeparateBonusAllowed(true)
                .setSeparateBonusValidTo(LocalDate.of(2027, 12, 31)));
  }

  private HrmPayrollBonusReqVO req() {
    return new HrmPayrollBonusReqVO()
        .setBatchId(batchId)
        .setEmployeeId(employeeId)
        .setAmount(new BigDecimal("36000.01"))
        .setMode("SEPARATE")
        .setEvidence("批准奖金授予金额及计税方式")
        .setRequestKey(UUID.randomUUID().toString());
  }

  private Long approve(HrmPayrollBonusReqVO r) {
    Long id = service.create(r, 1L);
    service.review(id, true, "独立复核依据", 2L);
    return id;
  }

  @Test
  void pendingNotUsableAndIndependentReview() {
    Long id = service.create(req(), 1L);
    assertNull(service.source(batchId, employeeId));
    assertServiceException(() -> service.review(id, true, "提交人", 1L), PAYROLL_SELF_APPROVAL);
    assertServiceException(() -> service.review(id, true, "员工本人", 99L), PAYROLL_SELF_APPROVAL);
    service.review(id, true, "独立复核", 2L);
    assertEquals(id, service.source(batchId, employeeId).getId());
    assertEquals(2, service.history(id, 2L).size());
  }

  @Test
  void cancellationPreservesHistoryAndStaleSuccessorCannotApprove() {
    Long id = approve(req());
    Long a =
        service.create(req().setPriorBonusId(id).setMode("NONE").setAmount(BigDecimal.ZERO), 1L);
    Long b = service.create(req().setPriorBonusId(id), 1L);
    service.review(a, true, "撤销奖金授予", 2L);
    assertServiceException(() -> service.review(b, true, "旧基线", 2L), PAYROLL_VERSION_CONFLICT);
    assertEquals("SUPERSEDED", bonuses.selectById(id).getStatus());
    assertEquals(new BigDecimal("36000.01"), bonuses.selectById(id).getAmount());
    assertEquals(0, service.source(batchId, employeeId).getAmount().signum());
  }

  @Test
  void retryCannotChangeAmounts() {
    HrmPayrollBonusReqVO r = req();
    Long id = approve(r);
    assertEquals(id, service.create(r, 1L));
    r.setAmount(BigDecimal.ONE);
    assertServiceException(() -> service.create(r, 1L), PAYROLL_TAX_INVALID);
  }

  @Test
  void changedPaymentDateOrTaxProfileInvalidatesSource() {
    Long id = approve(req());
    batches.updateById(
        new HrmSalaryMonthRecordDO()
            .setId(batchId)
            .setExpectedPaymentDate(LocalDate.of(2026, 10, 16)));
    assertServiceException(() -> service.source(batchId, employeeId), PAYROLL_VERSION_CONFLICT);
    batches.updateById(
        new HrmSalaryMonthRecordDO()
            .setId(batchId)
            .setExpectedPaymentDate(LocalDate.of(2026, 10, 15)));
    when(profiles.requireApproved(10L, employeeId, 2026))
        .thenReturn(new HrmPayrollTaxProfileDO().setId(124L).setResidentThisYear(true));
    assertServiceException(() -> service.source(batchId, employeeId), PAYROLL_VERSION_CONFLICT);
    assertEquals("APPROVED", bonuses.selectById(id).getStatus());
  }

  @Test
  void eligibilityAndRosterMustBeConfirmed() {
    when(profiles.requireApproved(10L, employeeId, 2026))
        .thenReturn(
            new HrmPayrollTaxProfileDO()
                .setId(123L)
                .setResidentThisYear(true)
                .setSeparateBonusUsedElsewhere(true));
    assertServiceException(() -> service.create(req(), 1L), PAYROLL_TAX_INVALID);
    when(scope.payrollDates(any())).thenReturn(Collections.emptyMap());
    assertServiceException(
        () -> service.create(req().setMode("MERGED"), 1L), PAYROLL_ENTITY_INVALID);
  }

  @Test
  void frozenBatchAndContradictoryZeroCannotBeChanged() {
    assertServiceException(() -> service.create(req().setMode("NONE"), 1L), PAYROLL_TAX_INVALID);
    assertServiceException(
        () -> service.create(req().setAmount(BigDecimal.ZERO), 1L), PAYROLL_TAX_INVALID);
    batches.updateById(new HrmSalaryMonthRecordDO().setId(batchId).setStatus(15));
    assertServiceException(() -> service.create(req(), 1L), SALARY_MONTH_RECORD_STATUS_INVALID);
  }

  @Test
  void employeeChoicesOnlyContainBatchMembersAndPublicFields() {
    List<Map<String, Object>> choices = service.employees(batchId, 2L);
    assertEquals(1, choices.size());
    assertEquals(new HashSet<>(Arrays.asList("id", "name", "jobNumber")), choices.get(0).keySet());
    assertEquals(employeeId, choices.get(0).get("id"));
    when(scope.payrollDates(any())).thenReturn(Collections.emptyMap());
    assertTrue(service.employees(batchId, 2L).isEmpty());
  }

  @Test
  void quotaIgnoresReleasedClaimsAllowsItsOwnReservationButNeverPaid() {
    HrmPayrollSourceClaimDO c =
        new HrmPayrollSourceClaimDO()
            .setKind("BONUS_YEAR")
            .setSourceKey(HrmPayrollBonusService.quotaKey("same-person", 2026))
            .setBatchId(batchId)
            .setStatus("RESERVED");
    claims.insert(c);
    assertFalse(service.separateUsed(batchId, "same-person", 2026));
    assertTrue(service.separateUsed(batchId + 1, "same-person", 2026));
    c.setStatus("PAID");
    claims.updateById(c);
    assertTrue(service.separateUsed(batchId, "same-person", 2026));
    c.setStatus("RELEASED");
    claims.updateById(c);
    assertFalse(service.separateUsed(batchId + 1, "same-person", 2026));
  }

  @Test
  void unexpectedActualYearRetainsBonusUsageAcrossEntities() {
    cn.iocoder.yudao.module.hrm.dal.dataobject.salary.monthrecord.HrmSalaryMonthEmployeeRecordDO
        result =
            new cn.iocoder.yudao.module.hrm.dal.dataobject.salary.monthrecord
                    .HrmSalaryMonthEmployeeRecordDO()
                .setEmployeeId(employeeId)
                .setTaxSourceSnapshot(
                    cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(
                        new HrmPayrollTaxWithholdingService.Source()
                            .setProfile(new HrmPayrollTaxProfileDO().setTaxpayerKey("same-person"))
                            .setBonus(
                                new HrmPayrollBonusDO()
                                    .setMode("SEPARATE")
                                    .setAmount(new BigDecimal("1000")))));
    HrmPayrollRunDO run =
        new HrmPayrollRunDO()
            .setVersion(1)
            .setInputSnapshot("{}")
            .setRuleSnapshot("{}")
            .setComputedBy(1L)
            .setMonthRecordId(batchId)
            .setResultSnapshot(
                cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(
                    Collections.singletonList(result)));
    runs.insert(run);
    claims.insert(
        new HrmPayrollSourceClaimDO()
            .setKind("BONUS_YEAR")
            .setSourceKey(HrmPayrollBonusService.quotaKey("same-person", 2025))
            .setBatchId(batchId)
            .setRunId(run.getId())
            .setEmployeeId(employeeId)
            .setStatus("PAID"));
    payments.insert(
        new HrmPayrollPaymentDO()
            .setEmployeeName("测试奖金员工")
            .setBankAccount("test-bank")
            .setAmount(new BigDecimal("1000"))
            .setAttempt(1)
            .setMonthRecordId(batchId)
            .setRunId(run.getId())
            .setEmployeeId(employeeId)
            .setStatus("SUCCESS")
            .setValueDate(LocalDate.of(2026, 1, 1)));
    assertTrue(service.separateUsed(batchId + 1, "same-person", 2026));
    assertFalse(service.separateUsed(batchId + 1, "other-person", 2026));
  }

  @Test
  void sourceAndHistoryAreScoped() {
    doThrow(new IllegalStateException("denied")).when(scope).check(any(), eq("READ"), eq(8L));
    Long id = approve(req());
    assertThrows(IllegalStateException.class, () -> service.list(batchId, 8L));
    assertThrows(IllegalStateException.class, () -> service.history(id, 8L));
  }
}
