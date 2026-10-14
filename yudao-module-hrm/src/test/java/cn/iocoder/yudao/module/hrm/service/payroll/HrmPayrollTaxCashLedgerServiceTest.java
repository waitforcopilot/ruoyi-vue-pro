package cn.iocoder.yudao.module.hrm.service.payroll;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollTaxCashLedgerVO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.salary.monthrecord.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.salary.monthrecord.HrmSalaryMonthRecordMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import javax.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

@Import(HrmPayrollTaxCashLedgerService.class)
class HrmPayrollTaxCashLedgerServiceTest extends BaseDbUnitTest {
  @Resource private HrmPayrollTaxCashLedgerService service;
  @Resource private HrmPayrollPaymentMapper payments;
  @Resource private HrmPayrollRunMapper runs;
  @Resource private HrmSalaryMonthRecordMapper batches;
  @Resource private HrmPayrollTaxProfileMapper profileMapper;
  @MockBean private HrmPayrollEntityService entities;
  @MockBean private HrmPayrollTaxProfileService profiles;
  private HrmPayrollTaxProfileDO profile;

  private static BigDecimal money(String value) {
    return new BigDecimal(value);
  }

  @BeforeEach
  void setup() {
    profile =
        new HrmPayrollTaxProfileDO()
            .setId(100L)
            .setEntityId(10L)
            .setEmployeeId(1L)
            .setYear(2026)
            .setTaxpayerKey("same-person")
            .setTaxRegistrationNumber("agent")
            .setOpeningMode("ZERO")
            .setOpeningAsOfDate(LocalDate.of(2026, 1, 1))
            .setOpeningIncome(BigDecimal.ZERO)
            .setOpeningSocialDeduction(BigDecimal.ZERO)
            .setOpeningBasicDeduction(BigDecimal.ZERO)
            .setOpeningWithheld(BigDecimal.ZERO);
    when(profiles.requireApproved(10L, 1L, 2026)).thenReturn(profile);
  }

  private HrmPayrollPaymentDO paid(Long entity, Long employee, LocalDate date, String status) {
    HrmSalaryMonthRecordDO batch =
        new HrmSalaryMonthRecordDO().setEntityId(entity).setYear(2025).setMonth(12).setStatus(17);
    batches.insert(batch);
    List<HrmSalaryMonthEmployeeRecordDO.OptionValue> values = new ArrayList<>();
    int[] codes = {250101, 270101, 250102, 270102, 250103, 270103};
    String[] amounts = {"10000", "20000", "5000", "10000", "1000", "2000"};
    for (int i = 0; i < codes.length; i++)
      values.add(
          new HrmSalaryMonthEmployeeRecordDO.OptionValue()
              .setCode(codes[i])
              .setValue(money(amounts[i])));
    HrmSalaryMonthEmployeeRecordDO employeeResult =
        new HrmSalaryMonthEmployeeRecordDO()
            .setEmployeeId(employee)
            .setRealPaySalary(money("8900"))
            .setPersonalTax(money("100"))
            .setOptionValues(values);
    HrmPayrollRunDO run =
        new HrmPayrollRunDO()
            .setMonthRecordId(batch.getId())
            .setVersion(1)
            .setInputSnapshot("{}")
            .setRuleSnapshot("{}")
            .setComputedBy(2L)
            .setResultSnapshot(JsonUtils.toJsonString(Collections.singletonList(employeeResult)));
    runs.insert(run);
    HrmPayrollPaymentDO payment =
        new HrmPayrollPaymentDO()
            .setMonthRecordId(batch.getId())
            .setRunId(run.getId())
            .setEmployeeId(employee)
            .setEmployeeName("测试人员")
            .setBankAccount("test-only")
            .setAmount(money("8900"))
            .setAttempt(1)
            .setStatus(status)
            .setValueDate(date);
    payments.insert(payment);
    return payment;
  }

  private HrmPayrollTaxCashLedgerVO ledger() {
    return service.ledger(10L, 1L, 2026, LocalDate.of(2026, 10, 1), 2L);
  }

  private static void equal(String expected, BigDecimal actual) {
    assertEquals(0, money(expected).compareTo(actual));
  }

  @Test
  void usesActualCashYearAndFrozenAmountsNotNominalMonthOrCurrentSalary() {
    paid(10L, 1L, LocalDate.of(2026, 1, 10), "SUCCESS");
    paid(10L, 1L, LocalDate.of(2025, 12, 20), "SUCCESS");
    paid(10L, 1L, LocalDate.of(2026, 10, 2), "SUCCESS");
    paid(10L, 1L, LocalDate.of(2026, 2, 1), "FAILED");
    paid(20L, 1L, LocalDate.of(2026, 2, 1), "SUCCESS");
    HrmPayrollTaxCashLedgerVO result = ledger();
    assertTrue(result.isReady());
    assertEquals(1, result.getRows().size());
    equal("10000", result.getIncome());
    equal("1000", result.getSocialDeduction());
    equal("5000", result.getBasicDeduction());
    equal("100", result.getWithheld());
    verify(entities).check(10L, "READ", 2L);
  }

  @Test
  void sameApprovedTaxIdentityIncludesRehiredEmployeeIdOnlyWithinAgent() {
    profileMapper.insert(
        new HrmPayrollTaxProfileDO()
            .setEntityId(10L)
            .setEmployeeId(2L)
            .setYear(2025)
            .setVersion(1)
            .setTaxpayerKey("same-person")
            .setTaxRegistrationNumber("agent")
            .setStatus("SUPERSEDED"));
    paid(10L, 2L, LocalDate.of(2026, 2, 1), "SUCCESS");
    paid(10L, 3L, LocalDate.of(2026, 2, 1), "SUCCESS");
    assertEquals(1, ledger().getRows().size());
    equal("10000", ledger().getIncome());
  }

  @Test
  void missingDateBlocksAndDoesNotBecomeZeroEvenWhenNominalYearIsEarlier() {
    paid(10L, 1L, null, "SUCCESS");
    HrmPayrollTaxCashLedgerVO result = ledger();
    assertFalse(result.isReady());
    assertNull(result.getIncome());
    assertEquals("UNDATED", result.getRows().get(0).getStatus());
  }

  @Test
  void declaredOpeningCoversEarlierCashOnceButMustCoverVisibleEvidence() {
    profile
        .setOpeningMode("DECLARE")
        .setOpeningAsOfDate(LocalDate.of(2026, 2, 1))
        .setOpeningIncome(money("10000"))
        .setOpeningSocialDeduction(money("1000"))
        .setOpeningBasicDeduction(money("5000"))
        .setOpeningWithheld(money("100"));
    paid(10L, 1L, LocalDate.of(2026, 1, 10), "SUCCESS");
    paid(10L, 1L, LocalDate.of(2026, 3, 10), "SUCCESS");
    HrmPayrollTaxCashLedgerVO result = ledger();
    assertTrue(result.isReady());
    equal("20000", result.getIncome());
    equal("200", result.getWithheld());
    assertEquals("OPENING_COVERED", result.getRows().get(0).getStatus());
    profile.setOpeningWithheld(money("99"));
    assertFalse(ledger().isReady());
    assertNull(ledger().getWithheld());
  }

  @Test
  void missingTaxSourceAndTamperedNetBlockRatherThanInventAmounts() {
    HrmPayrollPaymentDO payment = paid(10L, 1L, LocalDate.of(2026, 2, 1), "SUCCESS");
    payment.setAmount(money("1"));
    payments.updateById(payment);
    assertFalse(ledger().isReady());
    payment.setAmount(money("8900"));
    payments.updateById(payment);
    HrmPayrollRunDO run = runs.selectById(payment.getRunId());
    List<HrmSalaryMonthEmployeeRecordDO> results =
        JsonUtils.parseArray(run.getResultSnapshot(), HrmSalaryMonthEmployeeRecordDO.class);
    results.get(0).setOptionValues(Collections.emptyList());
    run.setResultSnapshot(JsonUtils.toJsonString(results));
    runs.updateById(run);
    assertEquals("SOURCE_INVALID", ledger().getRows().get(0).getStatus());
    assertNull(ledger().getIncome());
  }

  @Test
  void governedTaxPeriodMismatchIsExplicitAndRequiresCorrection() {
    HrmPayrollPaymentDO payment = paid(10L, 1L, LocalDate.of(2026, 9, 15), "SUCCESS");
    HrmPayrollRunDO run = runs.selectById(payment.getRunId());
    List<HrmSalaryMonthEmployeeRecordDO> rows =
        JsonUtils.parseArray(run.getResultSnapshot(), HrmSalaryMonthEmployeeRecordDO.class);
    rows.get(0).setTaxPaymentDate(LocalDate.of(2026, 8, 15));
    run.setResultSnapshot(JsonUtils.toJsonString(rows));
    runs.updateById(run);
    HrmPayrollTaxCashLedgerVO result = ledger();
    assertFalse(result.isReady());
    assertNull(result.getIncome());
    assertEquals("TAX_PERIOD_MISMATCH", result.getRows().get(0).getStatus());
  }

  @Test
  void changedWithholdingSubjectAndMalformedFrozenDataAreNotSilentlyAccumulated() {
    HrmPayrollPaymentDO payment = paid(10L, 1L, LocalDate.of(2026, 2, 1), "SUCCESS");
    HrmPayrollRunDO run = runs.selectById(payment.getRunId());
    List<HrmSalaryMonthEmployeeRecordDO> rows =
        JsonUtils.parseArray(run.getResultSnapshot(), HrmSalaryMonthEmployeeRecordDO.class);
    rows.get(0)
        .setTaxSourceSnapshot(
            JsonUtils.toJsonString(
                new HrmPayrollTaxWithholdingService.Source()
                    .setProfile(
                        new HrmPayrollTaxProfileDO().setTaxRegistrationNumber("another-agent"))));
    run.setResultSnapshot(JsonUtils.toJsonString(rows));
    runs.updateById(run);
    assertEquals("TAX_SUBJECT_MISMATCH", ledger().getRows().get(0).getStatus());
    assertNull(ledger().getIncome());
    run.setResultSnapshot("null");
    runs.updateById(run);
    assertEquals("SOURCE_INVALID", ledger().getRows().get(0).getStatus());
    assertNull(ledger().getWithheld());
  }

  @Test
  void unboundHistoricalPaidBatchCannotBeSilentlyExcluded() {
    paid(null, 1L, LocalDate.of(2026, 2, 1), "SUCCESS");
    assertFalse(ledger().isReady());
    assertNull(ledger().getIncome());
    assertTrue(ledger().getBlockers().stream().anyMatch(reason -> reason.contains("主体归属")));
  }

  @Test
  void datesAndPermissionsAreCheckedBeforeReadingPayments() {
    assertServiceException(
        () -> service.ledger(10L, 1L, 2026, LocalDate.of(2025, 12, 31), 2L), PAYROLL_TAX_INVALID);
    profile.setOpeningAsOfDate(LocalDate.of(2026, 4, 1));
    assertServiceException(
        () -> service.ledger(10L, 1L, 2026, LocalDate.of(2026, 3, 31), 2L), PAYROLL_TAX_INVALID);
    doThrow(new IllegalStateException("denied")).when(entities).check(10L, "READ", 9L);
    assertThrows(
        IllegalStateException.class,
        () -> service.ledger(10L, 1L, 2026, LocalDate.of(2026, 10, 1), 9L));
  }
}
