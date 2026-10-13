package cn.iocoder.yudao.module.hrm.service.payroll;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.*;
import java.math.BigDecimal;
import java.util.*;
import javax.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

@Import({
  HrmPayrollTaxPolicyService.class,
  org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration.class
})
class HrmPayrollTaxPolicyServiceTest extends BaseDbUnitTest {
  @Resource private HrmPayrollTaxPolicyService service;
  @Resource private HrmPayrollTaxPolicyMapper policies;
  @Resource private HrmPayrollLegalEntityMapper companies;
  @MockBean private HrmPayrollEntityService entities;

  @BeforeEach
  void setup() {
    companies.insert(
        new HrmPayrollLegalEntityDO()
            .setId(10L)
            .setCode("TAX-AGENT")
            .setName("纳税主体")
            .setTaxRegistrationNumber("914400000000000001")
            .setStatus("APPROVED"));
  }

  private HrmPayrollTaxPolicyReqVO request(Integer year) {
    return service
        .defaults(10L, year, 1L)
        .setRequestKey(UUID.randomUUID().toString())
        .setOfficialReference("测试正式政策依据，使用标准税率表")
        .setEvidence("复核适用年度和纳税登记资料");
  }

  private Long approved() {
    Long id = service.create(request(2026), 1L);
    service.review(id, true, "独立核对", 2L);
    return id;
  }

  private HrmPayrollTaxPolicyTrialReqVO trial(Long id) {
    return new HrmPayrollTaxPolicyTrialReqVO()
        .setPolicyId(id)
        .setBasicMonths(12)
        .setSalaryIncome(new BigDecimal("120000"))
        .setSocialDeduction(BigDecimal.ZERO)
        .setAdditionalDeduction(BigDecimal.ZERO)
        .setOtherDeduction(BigDecimal.ZERO)
        .setAlreadyWithheld(new BigDecimal("3000"))
        .setBonusAmount(BigDecimal.ZERO)
        .setBonusMode("NONE")
        .setSeparateBonusAlreadyUsed(false);
  }

  private void money(String expected, BigDecimal value) {
    assertEquals(0, new BigDecimal(expected).compareTo(value));
  }

  @Test
  void defaultsNeedOfficialEvidenceAndIndependentApproval() {
    assertServiceException(
        () -> service.create(service.defaults(10L, 2026, 1L).setRequestKey("missing-evidence"), 1L),
        PAYROLL_TAX_INVALID);
    Long id = service.create(request(2026), 1L);
    assertServiceException(() -> service.requireApproved(10L, 2026), PAYROLL_TAX_INVALID);
    assertServiceException(() -> service.review(id, true, "自行批准", 1L), PAYROLL_SELF_APPROVAL);
    service.review(id, true, "独立复核", 2L);
    assertEquals(id, service.requireApproved(10L, 2026).getId());
    assertEquals(2, service.history(id, 2L).size());
  }

  @Test
  void policyRequestRetriesReturnOriginalVersionAndRejectChangedPayload() {
    HrmPayrollTaxPolicyReqVO request = request(2026);
    Long id = service.create(request, 1L);
    assertEquals(id, service.create(request, 1L));
    service.review(id, true, "独立复核", 2L);
    assertEquals(id, service.create(request, 1L));
    assertServiceException(() -> service.create(request.setName("改写原请求"), 1L), PAYROLL_TAX_INVALID);
    assertEquals(1, policies.selectCount());
  }

  @Test
  void salaryCumulativeTaxUsesExplicitEmploymentMonths() {
    Long id = approved();
    PayrollTaxPolicyEngine.Quote full = service.trial(trial(id), 1L);
    money("3480", full.getCumulativeSalaryTax());
    money("480", full.getCurrentWithholding());
    PayrollTaxPolicyEngine.Quote partial =
        service.trial(trial(id).setBasicMonths(2).setAlreadyWithheld(BigDecimal.ZERO), 1L);
    money("10000", partial.getBasicDeduction());
    money("8480", partial.getCurrentWithholding());
  }

  @Test
  void standardAnnualBracketsMatchGoldenTaxAmounts() {
    Long id = approved();
    String[] income = {"36000", "144000", "300000", "420000", "660000", "960000", "1200000"};
    String[] expected = {"1080", "11880", "43080", "73080", "145080", "250080", "358080"};
    for (int i = 0; i < income.length; i++)
      money(
          expected[i],
          service
              .trial(
                  trial(id)
                      .setBasicMonths(0)
                      .setSalaryIncome(new BigDecimal(income[i]))
                      .setAlreadyWithheld(BigDecimal.ZERO),
                  1L)
              .getCumulativeSalaryTax());
  }

  @Test
  void excessWithholdingNeverBecomesAutomaticWageRefund() {
    Long id = approved();
    PayrollTaxPolicyEngine.Quote quote =
        service.trial(
            trial(id)
                .setSalaryIncome(new BigDecimal("5000"))
                .setAlreadyWithheld(new BigDecimal("100")),
            1L);
    money("0", quote.getCurrentWithholding());
    money("100", quote.getCreditToReconcile());
  }

  @Test
  void annualBonusBoundaryIsComparedBeforeDivisionRounding() {
    Long id = approved();
    HrmPayrollTaxPolicyTrialReqVO request =
        trial(id)
            .setSalaryIncome(BigDecimal.ZERO)
            .setAlreadyWithheld(BigDecimal.ZERO)
            .setBonusMode("SEPARATE");
    money(
        "1080",
        service.trial(request.setBonusAmount(new BigDecimal("36000")), 1L).getSeparateBonusTax());
    money(
        "3390",
        service
            .trial(request.setBonusAmount(new BigDecimal("36000.01")), 1L)
            .getSeparateBonusTax());
  }

  @Test
  void mergedAndSeparateStrategiesRemainDistinctAndBonusCannotOffsetSalaryCredit() {
    Long id = approved();
    HrmPayrollTaxPolicyTrialReqVO request = trial(id).setBonusAmount(new BigDecimal("36000"));
    money("1560", service.trial(request.setBonusMode("SEPARATE"), 1L).getCurrentWithholding());
    money("4080", service.trial(request.setBonusMode("MERGED"), 1L).getCurrentWithholding());
    request.setSalaryIncome(BigDecimal.ZERO).setAlreadyWithheld(new BigDecimal("100"));
    PayrollTaxPolicyEngine.Quote quote = service.trial(request.setBonusMode("SEPARATE"), 1L);
    money("1080", quote.getCurrentWithholding());
    money("100", quote.getCreditToReconcile());
  }

  @Test
  void separateBonusRequiresValidYearAndUnusedAnnualEligibility() {
    Long id = approved();
    assertServiceException(
        () ->
            service.trial(
                trial(id)
                    .setBonusAmount(new BigDecimal("36000"))
                    .setBonusMode("SEPARATE")
                    .setSeparateBonusAlreadyUsed(true),
                1L),
        PAYROLL_TAX_INVALID);
    HrmPayrollTaxPolicyReqVO future = request(2028);
    assertFalse(future.getSeparateBonusAllowed());
    assertServiceException(
        () -> service.create(future.setSeparateBonusAllowed(true), 1L), PAYROLL_TAX_INVALID);
  }

  @Test
  void successorPolicyPreservesPriorParametersAndStaleCandidateCannotOverwrite() {
    Long first = approved();
    Long next = service.create(request(2026).setMonthlyBasicDeduction(new BigDecimal("6000")), 1L);
    Long stale = service.create(request(2026).setMonthlyBasicDeduction(new BigDecimal("7000")), 1L);
    service.review(next, true, "新依据复核", 2L);
    assertEquals("SUPERSEDED", policies.selectById(first).getStatus());
    money("5000", policies.selectById(first).getMonthlyBasicDeduction());
    assertEquals(next, service.requireApproved(10L, 2026).getId());
    assertServiceException(() -> service.review(stale, true, "覆盖接续", 2L), PAYROLL_VERSION_CONFLICT);
    assertEquals("PENDING", policies.selectById(stale).getStatus());
  }

  @Test
  void malformedBandsHugeExponentAndNegativeAmountsAreRejected() {
    HrmPayrollTaxPolicyReqVO malformed = request(2026);
    malformed.getResidentBands().get(1).setQuickDeduction(BigDecimal.ZERO);
    assertServiceException(() -> service.create(malformed, 1L), PAYROLL_TAX_INVALID);
    HrmPayrollTaxPolicyReqVO finalMalformed = request(2026);
    finalMalformed.getResidentBands().get(0).setRate(new BigDecimal("0E+100000"));
    assertServiceException(() -> service.create(finalMalformed, 1L), PAYROLL_TAX_INVALID);
    Long id = approved();
    assertServiceException(
        () -> service.trial(trial(id).setSalaryIncome(BigDecimal.ONE.negate()), 1L),
        PAYROLL_TAX_INVALID);
    assertServiceException(
        () -> service.trial(trial(id).setBonusAmount(BigDecimal.ONE), 1L), PAYROLL_TAX_INVALID);
  }

  @Test
  void changedOrAmbiguousRegistrationBlocksNewFiscalUse() {
    Long id = approved();
    companies.updateById(
        new HrmPayrollLegalEntityDO().setId(10L).setTaxRegistrationNumber("914400000000000002"));
    assertServiceException(() -> service.requireApproved(10L, 2026), PAYROLL_TAX_INVALID);
    assertServiceException(() -> service.trial(trial(id), 1L), PAYROLL_TAX_INVALID);
    companies.insert(
        new HrmPayrollLegalEntityDO()
            .setId(11L)
            .setCode("DUP")
            .setName("重复登记映射")
            .setTaxRegistrationNumber("914400000000000002")
            .setStatus("APPROVED"));
    assertServiceException(() -> service.defaults(10L, 2026, 1L), PAYROLL_TAX_INVALID);
  }

  @Test
  void rejectedPolicyAndUnauthorizedReadsKeepHistory() {
    Long id = service.create(request(2026), 1L);
    service.review(id, false, "依据不完整", 2L);
    assertEquals("REJECTED", policies.selectById(id).getStatus());
    doThrow(
            cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception(
                PAYROLL_ENTITY_INVALID))
        .when(entities)
        .check(10L, "READ", 9L);
    assertServiceException(() -> service.list(10L, 2026, 9L), PAYROLL_ENTITY_INVALID);
    assertServiceException(() -> service.history(id, 9L), PAYROLL_ENTITY_INVALID);
  }
}
