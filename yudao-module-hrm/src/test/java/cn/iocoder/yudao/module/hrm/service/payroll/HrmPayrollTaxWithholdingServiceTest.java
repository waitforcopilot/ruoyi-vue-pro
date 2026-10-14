package cn.iocoder.yudao.module.hrm.service.payroll;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollTaxCashLedgerVO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import javax.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

@Import(HrmPayrollTaxWithholdingService.class)
class HrmPayrollTaxWithholdingServiceTest extends BaseDbUnitTest {
  @Resource private HrmPayrollTaxWithholdingService service;
  @MockBean private HrmPayrollTaxPolicyService policies;
  @MockBean private HrmPayrollBonusService bonuses;
  @MockBean private HrmPayrollTaxProfileService profiles;
  @MockBean private HrmPayrollTaxDeductionService deductions;
  @MockBean private HrmPayrollTaxCashLedgerService cash;
  @MockBean private HrmPayrollTaxEmploymentService employment;
  private HrmPayrollTaxCashLedgerVO ledger;
  private final LocalDate payDate = LocalDate.of(2026, 10, 14);

  private static BigDecimal money(String amount) {
    return new BigDecimal(amount);
  }

  @BeforeEach
  void setup() {
    HrmPayrollTaxPolicyDO policy =
        new HrmPayrollTaxPolicyDO()
            .setId(1L)
            .setVersion(1)
            .setYear(2026)
            .setTaxRegistrationNumber("agent")
            .setStatus("APPROVED")
            .setMonthlyBasicDeduction(money("5000"))
            .setResidentBands(
                "[{\"upper\":36000,\"rate\":0.03,\"quickDeduction\":0},{\"upper\":null,\"rate\":0.10,\"quickDeduction\":2520}]")
            .setBonusBands("[]");
    when(policies.requireApproved(10L, 2026)).thenReturn(policy);
    when(profiles.requireApproved(10L, 1L, 2026))
        .thenReturn(
            new HrmPayrollTaxProfileDO()
                .setId(2L)
                .setResidentThisYear(true)
                .setYear(2026)
                .setSeparateBonusUsedElsewhere(false));
    HrmPayrollTaxDeductionDO declaration =
        new HrmPayrollTaxDeductionDO()
            .setId(3L)
            .setVersion(1)
            .setValuesSnapshot("official-claim-fixture");
    when(deductions.source(10L, 1L, 2026, 10)).thenReturn(declaration);
    Map<String, BigDecimal> amounts = new TreeMap<>();
    HrmPayrollTaxDeductionService.FIELDS.forEach(code -> amounts.put(code, BigDecimal.ZERO));
    amounts.put("260106", money("2000"));
    when(deductions.values(declaration)).thenReturn(amounts);
    ledger =
        new HrmPayrollTaxCashLedgerVO()
            .setReady(true)
            .setIncome(money("100000"))
            .setSocialDeduction(money("10000"))
            .setBasicDeduction(money("45000"))
            .setWithheld(money("1000"));
    when(cash.forApprovedPayroll(eq(10L), eq(1L), eq(2026), any())).thenReturn(ledger);
    when(employment.forApprovedPayroll(10L, 1L, payDate))
        .thenReturn(
            new PayrollTaxEmploymentEngine.Result()
                .setMode("EMPLOYMENT_MONTHS")
                .setMonths(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10))
                .setBasicDeduction(money("50000")));
  }

  @Test
  void approvedBonusUsesSeparateOrMergedQuoteAndQuota() {
    HrmPayrollTaxPolicyDO policy = policies.requireApproved(10L, 2026);
    policy
        .setSeparateBonusAllowed(true)
        .setSeparateBonusValidTo(LocalDate.of(2027, 12, 31))
        .setBonusBands(
            "[{\"upper\":3000,\"rate\":0.03,\"quickDeduction\":0},{\"upper\":null,\"rate\":0.10,\"quickDeduction\":210}]");
    profiles.requireApproved(10L, 1L, 2026).setTaxpayerKey("same-person");
    HrmPayrollBonusDO bonus =
        new HrmPayrollBonusDO()
            .setId(8L)
            .setBatchId(90L)
            .setEntityId(10L)
            .setEmployeeId(1L)
            .setProfileId(2L)
            .setExpectedPaymentDate(payDate)
            .setMode("SEPARATE")
            .setAmount(money("36000.01"))
            .setStatus("APPROVED");
    when(bonuses.source(90L, 1L)).thenReturn(bonus);
    HrmPayrollTaxWithholdingService.Source source =
        service.calculateForBatch(
            90L, 10L, 1L, payDate, money("10000"), money("1000"), money("2000"));
    assertEquals(0, money("3390.00").compareTo(source.getQuote().getSeparateBonusTax()));
    assertEquals(0, money("4570.00").compareTo(source.getQuote().getCurrentWithholding()));
    assertEquals(0, money("1180").compareTo(source.getQuote().getCurrentSalaryWithholding()));
    String snapshot = JsonUtils.toJsonString(source);
    service.validate(snapshot);
    bonus.setMode("MERGED");
    assertServiceException(() -> service.validate(snapshot), PAYROLL_TAX_INVALID);
    source =
        service.calculateForBatch(
            90L, 10L, 1L, payDate, money("10000"), money("1000"), money("2000"));
    assertEquals(0, money("4780.00").compareTo(source.getQuote().getCurrentWithholding()));
    assertEquals(0, source.getQuote().getSeparateBonusTax().signum());
    bonus.setMode("SEPARATE");
    when(bonuses.separateUsed(90L, "same-person", 2026)).thenReturn(true);
    assertServiceException(
        () ->
            service.calculateForBatch(
                90L, 10L, 1L, payDate, money("10000"), money("1000"), money("2000")),
        PAYROLL_TAX_INVALID);
  }

  private HrmPayrollTaxWithholdingService.Source calculate() {
    return service.calculate(10L, 1L, payDate, money("10000"), money("1000"), money("2000"));
  }

  @Test
  void quoteUsesVerifiedCashYearTotalsAndApprovedEmploymentDeduction() {
    HrmPayrollTaxWithholdingService.Source result = calculate();
    assertEquals(money("47000.00"), result.getQuote().getSalaryTaxable());
    assertEquals(money("2180.00"), result.getQuote().getCumulativeSalaryTax());
    assertEquals(money("1180.00"), result.getQuote().getCurrentSalaryWithholding());
    assertEquals(payDate, result.getExpectedPaymentDate());
  }

  @Test
  void excessPreviouslyWithheldTaxIsTrackedAndDoesNotAutomaticallyRefund() {
    ledger.setWithheld(money("4000"));
    HrmPayrollTaxWithholdingService.Source result = calculate();
    assertEquals(0, result.getQuote().getCurrentSalaryWithholding().signum());
    assertEquals(money("1820.00"), result.getQuote().getCreditToReconcile());
  }

  @Test
  void missingOrInconsistentSourceCannotBeBypassedWithHandAmounts() {
    assertServiceException(
        () -> service.calculate(10L, 1L, payDate, money("10000"), money("1000"), BigDecimal.ZERO),
        PAYROLL_TAX_INVALID);
    ledger.setReady(false);
    assertServiceException(() -> calculate(), PAYROLL_TAX_INVALID);
    ledger.setReady(true).setBasicDeduction(money("60000"));
    assertServiceException(() -> calculate(), PAYROLL_TAX_INVALID);
    ledger.setBasicDeduction(money("45000"));
    when(deductions.source(10L, 1L, 2026, 10)).thenReturn(null);
    assertServiceException(() -> calculate(), PAYROLL_TAX_INVALID);
  }

  @Test
  void unconfirmedOrNonresidentIdentityCannotUseResidentCumulativePolicy() {
    when(profiles.requireApproved(10L, 1L, 2026))
        .thenReturn(new HrmPayrollTaxProfileDO().setResidentThisYear(false));
    assertServiceException(() -> calculate(), PAYROLL_TAX_RESIDENCY_UNCONFIRMED);
    when(profiles.requireApproved(10L, 1L, 2026)).thenReturn(new HrmPayrollTaxProfileDO());
    assertServiceException(() -> calculate(), PAYROLL_TAX_RESIDENCY_UNCONFIRMED);
  }

  @Test
  void frozenSourceValidationDetectsLaterPaymentsAndPolicyVersions() {
    String snapshot = JsonUtils.toJsonString(calculate());
    service.validate(snapshot);
    ledger.setWithheld(money("1100"));
    assertServiceException(() -> service.validate(snapshot), PAYROLL_TAX_INVALID);
    ledger.setWithheld(money("1000"));
    when(policies.requireApproved(10L, 2026))
        .thenReturn(
            new HrmPayrollTaxPolicyDO()
                .setId(5L)
                .setVersion(2)
                .setYear(2026)
                .setMonthlyBasicDeduction(money("5000"))
                .setResidentBands(
                    "[{\"upper\":36000,\"rate\":0.03,\"quickDeduction\":0},{\"upper\":null,\"rate\":0.10,\"quickDeduction\":2520}]")
                .setBonusBands("[]"));
    assertServiceException(() -> service.validate(snapshot), PAYROLL_TAX_INVALID);
  }
}
