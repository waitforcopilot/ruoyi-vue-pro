package cn.iocoder.yudao.module.hrm.service.payroll;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.*;
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

@Import(HrmPayrollTaxFilingSourceService.class)
class HrmPayrollTaxFilingSourceServiceTest extends BaseDbUnitTest {
  @Resource private HrmPayrollTaxFilingSourceService service;
  @Resource private HrmPayrollPaymentMapper payments;
  @Resource private HrmPayrollRunMapper runs;
  @Resource private HrmSalaryMonthRecordMapper batches;
  @MockBean private HrmPayrollTaxPolicyService policies;
  @MockBean private HrmPayrollTaxProfileService profiles;
  @MockBean private HrmPayrollTaxCashLedgerService cashLedger;
  @MockBean private HrmPayrollTaxDeductionService deductions;
  @MockBean private HrmPayrollTaxEmploymentService employment;
  private HrmPayrollTaxPolicyDO policy;
  private HrmPayrollTaxProfileDO profile;

  private static BigDecimal money(String value) {
    return new BigDecimal(value);
  }

  @BeforeEach
  void setup() {
    policy =
        new HrmPayrollTaxPolicyDO()
            .setId(7L)
            .setVersion(1)
            .setYear(2026)
            .setTaxRegistrationNumber("test-agency")
            .setStatus("APPROVED")
            .setMonthlyBasicDeduction(money("5000"))
            .setResidentBands(
                "[{\"upper\":36000,\"rate\":0.03,\"quickDeduction\":0},{\"upper\":null,\"rate\":0.10,\"quickDeduction\":2520}]")
            .setSeparateBonusAllowed(true)
            .setSeparateBonusValidTo(LocalDate.of(2027, 12, 31))
            .setBonusBands(
                "[{\"upper\":3000,\"rate\":0.03,\"quickDeduction\":0},{\"upper\":null,\"rate\":0.10,\"quickDeduction\":210}]");
    profile =
        new HrmPayrollTaxProfileDO()
            .setId(90L)
            .setEntityId(10L)
            .setEmployeeId(1L)
            .setYear(2026)
            .setResidentThisYear(true)
            .setTaxpayerKey("same-person")
            .setTaxpayerName("虚构税务员工")
            .setIdentifierType("NATIONAL_ID")
            .setIdentifierCountry("CN")
            .setMaskedIdentifier("TEST***90")
            .setTaxRegistrationNumber("test-agency");
    when(policies.requireApproved(10L, 2026)).thenReturn(policy);
    when(profiles.requireApproved(10L, 1L, 2026)).thenReturn(profile);
  }

  private HrmPayrollPaymentDO payment(
      Long entity, String bonusMode, LocalDate date, HrmPayrollPaymentDO prior) {
    BigDecimal priorIncome = prior == null ? BigDecimal.ZERO : money("10000"),
        priorSocial = prior == null ? BigDecimal.ZERO : money("1000"),
        priorBasic = prior == null ? BigDecimal.ZERO : money("5000"),
        priorTax = prior == null ? BigDecimal.ZERO : money("120");
    BigDecimal bonus = "NONE".equals(bonusMode) ? BigDecimal.ZERO : money("36000");
    HrmPayrollTaxPolicyTrialReqVO req =
        new HrmPayrollTaxPolicyTrialReqVO()
            .setBasicMonths(1)
            .setSalaryIncome(priorIncome.add(money("10000")))
            .setSocialDeduction(priorSocial.add(money("1000")))
            .setAdditionalDeduction(BigDecimal.ZERO)
            .setOtherDeduction(BigDecimal.ZERO)
            .setAlreadyWithheld(priorTax)
            .setBonusAmount(bonus)
            .setBonusMode(bonusMode)
            .setSeparateBonusAlreadyUsed(false);
    PayrollTaxPolicyEngine.Quote quote = PayrollTaxPolicyEngine.quote(policy, req);
    HrmSalaryMonthRecordDO batch =
        new HrmSalaryMonthRecordDO().setEntityId(entity).setYear(2025).setMonth(12).setStatus(17);
    batches.insert(batch);
    HrmPayrollTaxCashLedgerVO paid =
        new HrmPayrollTaxCashLedgerVO()
            .setReady(true)
            .setIncome(priorIncome)
            .setSocialDeduction(priorSocial)
            .setBasicDeduction(priorBasic)
            .setWithheld(priorTax);
    if (prior != null)
      paid.getRows()
          .add(
              new HrmPayrollTaxCashLedgerVO.Row().setPaymentId(prior.getId()).setStatus("COUNTED"));
    HrmPayrollTaxWithholdingService.Source source =
        new HrmPayrollTaxWithholdingService.Source()
            .setBatchId(batch.getId())
            .setEntityId(entity)
            .setEmployeeId(1L)
            .setExpectedPaymentDate(LocalDate.of(2026, 9, 26))
            .setProfile(profile)
            .setPolicy(policy)
            .setPaid(paid)
            .setCurrentIncome(money("10000"))
            .setCurrentSocialDeduction(money("1000"))
            .setAdditionalDeduction(BigDecimal.ZERO)
            .setQuote(quote);
    if (!"NONE".equals(bonusMode))
      source.setBonus(new HrmPayrollBonusDO().setMode(bonusMode).setAmount(bonus));
    List<HrmSalaryMonthEmployeeRecordDO.OptionValue> values = new ArrayList<>();
    int[] codes = {250101, 270101, 250102, 270102, 250103, 270103, 270104, 270105, 270106};
    BigDecimal[] amounts = {
      priorIncome,
      priorIncome.add(money("10000")).add("MERGED".equals(bonusMode) ? bonus : BigDecimal.ZERO),
      priorBasic,
      money("5000"),
      priorSocial,
      priorSocial.add(money("1000")),
      BigDecimal.ZERO,
      quote.getSalaryTaxable(),
      quote.getCumulativeSalaryTax()
    };
    for (int i = 0; i < codes.length; i++)
      values.add(
          new HrmSalaryMonthEmployeeRecordDO.OptionValue().setCode(codes[i]).setValue(amounts[i]));
    BigDecimal net =
        money("10000").add(bonus).subtract(money("1000")).subtract(quote.getCurrentWithholding());
    HrmSalaryMonthEmployeeRecordDO result =
        new HrmSalaryMonthEmployeeRecordDO()
            .setEmployeeId(1L)
            .setEntityId(entity)
            .setMonthRecordId(batch.getId())
            .setYear(2025)
            .setMonth(12)
            .setRealPaySalary(net)
            .setPersonalTax(quote.getCurrentWithholding())
            .setTaxPaymentDate(source.getExpectedPaymentDate())
            .setTaxSourceSnapshot(JsonUtils.toJsonString(source))
            .setOptionValues(values);
    HrmPayrollRunDO run =
        new HrmPayrollRunDO()
            .setMonthRecordId(batch.getId())
            .setVersion(1)
            .setInputSnapshot("{}")
            .setRuleSnapshot("{}")
            .setComputedBy(1L)
            .setResultSnapshot(JsonUtils.toJsonString(Collections.singletonList(result)));
    runs.insert(run);
    HrmPayrollPaymentDO payment =
        new HrmPayrollPaymentDO()
            .setMonthRecordId(batch.getId())
            .setRunId(run.getId())
            .setEmployeeId(1L)
            .setEmployeeName("虚构税务员工")
            .setBankAccount("test-bank")
            .setAmount(net)
            .setStatus("SUCCESS")
            .setAttempt(1)
            .setValueDate(date);
    payments.insert(payment);
    return payment;
  }

  @Test
  void sourceUsesCashMonthAndSeparatesBonusIncomeAndTax() {
    HrmPayrollPaymentDO p = payment(10L, "SEPARATE", LocalDate.of(2026, 9, 25), null);
    HrmPayrollTaxFilingSourceVO data = service.source(10L, 2026, 9);
    assertEquals(2, data.getRows().size());
    assertEquals(0, money("1200").compareTo(data.getTotalTax()));
    HrmPayrollTaxFilingSourceVO.Row
        wage =
            data.getRows().stream()
                .filter(r -> "RESIDENT_WAGE".equals(r.getMode()))
                .findFirst()
                .get(),
        bonus =
            data.getRows().stream()
                .filter(r -> "SEPARATE_BONUS".equals(r.getMode()))
                .findFirst()
                .get();
    assertEquals(0, money("10000").compareTo(wage.getIncome()));
    assertEquals(0, money("120").compareTo(wage.getWithheld()));
    assertEquals(0, money("36000").compareTo(bonus.getIncome()));
    assertEquals(0, money("1080").compareTo(bonus.getWithheld()));
    assertEquals(Collections.singletonList(p.getId()), wage.getPaymentIds());
    assertFalse(JsonUtils.toJsonString(data).contains("same-person"));
  }

  @Test
  void mergedBonusAppearsOnlyInWageSource() {
    payment(10L, "MERGED", LocalDate.of(2026, 9, 25), null);
    HrmPayrollTaxFilingSourceVO data = service.source(10L, 2026, 9);
    assertEquals(1, data.getRows().size());
    assertEquals(0, money("46000").compareTo(data.getRows().get(0).getIncome()));
    assertEquals(0, money("1480").compareTo(data.getTotalTax()));
  }

  @Test
  void completeCumulativeBasisIsChosenEvenWhenActualBankDatesAreReversed() {
    HrmPayrollPaymentDO first = payment(10L, "NONE", LocalDate.of(2026, 9, 25), null);
    payment(10L, "NONE", LocalDate.of(2026, 9, 24), first);
    HrmPayrollTaxFilingSourceVO data = service.source(10L, 2026, 9);
    assertEquals(1, data.getRows().size());
    assertEquals(0, money("20000").compareTo(data.getRows().get(0).getIncome()));
    assertEquals(0, money("20000").compareTo(data.getRows().get(0).getCumulativeIncome()));
    assertEquals(0, money("390").compareTo(data.getTotalTax()));
  }

  @Test
  void incompleteCumulativeGraphRequiresCorrection() {
    payment(10L, "NONE", LocalDate.of(2026, 9, 25), null);
    payment(10L, "NONE", LocalDate.of(2026, 9, 26), null);
    assertServiceException(() -> service.source(10L, 2026, 9), PAYROLL_VERSION_CONFLICT);
  }

  @Test
  void unknownDateOrChangedTaxpayerCannotBeSilentlyExcluded() {
    HrmPayrollPaymentDO p = payment(10L, "NONE", null, null);
    assertServiceException(() -> service.source(10L, 2026, 9), PAYROLL_TAX_INVALID);
    payments.updateById(
        new HrmPayrollPaymentDO().setId(p.getId()).setValueDate(LocalDate.of(2026, 9, 25)));
    profile.setTaxpayerKey("other-person");
    assertServiceException(() -> service.source(10L, 2026, 9), PAYROLL_TAX_INVALID);
  }

  @Test
  void changedAnnualPolicyOrFinancialProfileRequiresBasisCorrection() {
    payment(10L, "NONE", LocalDate.of(2026, 9, 25), null);
    policy.setVersion(2);
    assertServiceException(() -> service.source(10L, 2026, 9), PAYROLL_VERSION_CONFLICT);
    policy.setVersion(1);
    profile.setOpeningIncome(money("50000"));
    assertServiceException(() -> service.source(10L, 2026, 9), PAYROLL_VERSION_CONFLICT);
  }

  @Test
  void onlySuccessfulTargetEntityPaymentsAreIncluded() {
    HrmPayrollPaymentDO p = payment(10L, "NONE", LocalDate.of(2026, 9, 25), null);
    payments.updateById(new HrmPayrollPaymentDO().setId(p.getId()).setStatus("FAILED"));
    payment(20L, "NONE", LocalDate.of(2026, 9, 25), null);
    assertTrue(service.source(10L, 2026, 9).getRows().isEmpty());
    verify(profiles, never()).requireApproved(20L, 1L, 2026);
  }
}
