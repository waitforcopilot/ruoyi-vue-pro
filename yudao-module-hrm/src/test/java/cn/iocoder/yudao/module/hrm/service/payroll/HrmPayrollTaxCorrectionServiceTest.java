package cn.iocoder.yudao.module.hrm.service.payroll;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollTaxCorrectionReqVO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.salary.monthrecord.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.employee.info.HrmEmployeeMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.salary.monthrecord.HrmSalaryMonthRecordMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import javax.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

@Import({
  HrmPayrollTaxCorrectionService.class,
  HrmPayrollTaxCashLedgerService.class,
  HrmPayrollTaxFilingSourceService.class,
  org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration.class
})
class HrmPayrollTaxCorrectionServiceTest extends BaseDbUnitTest {
  @Resource private HrmPayrollTaxCorrectionService service;
  @Resource private HrmPayrollTaxCashLedgerService ledger;
  @Resource private HrmPayrollTaxFilingSourceService filingSource;
  @Resource private HrmPayrollTaxCorrectionMapper corrections;
  @Resource private HrmPayrollPaymentMapper payments;
  @Resource private HrmPayrollRunMapper runs;
  @Resource private HrmSalaryMonthRecordMapper batches;
  @Resource private HrmEmployeeMapper employees;
  @Resource private HrmPayrollTaxPolicyMapper policies;
  @Resource private HrmPayrollSourceClaimMapper claims;
  @Resource private HrmPayrollBatchEventMapper events;
  @MockBean private HrmPayrollEntityService entities;
  @MockBean private HrmPayrollTaxPolicyService policyService;
  @MockBean private HrmPayrollTaxProfileService profiles;
  @MockBean private HrmPayrollTaxEmploymentService employment;
  @MockBean private HrmPayrollTaxDeductionService deductions;
  private HrmPayrollPaymentDO payment;
  private HrmPayrollRunDO run;
  private HrmPayrollTaxProfileDO profile;
  private HrmPayrollTaxPolicyDO policy;
  private HrmSalaryMonthRecordDO batch;

  private BigDecimal money(String v) {
    return new BigDecimal(v);
  }

  @BeforeEach
  void setup() {
    employees.insert(new HrmEmployeeDO().setId(1L).setName("员工").setUserId(20L));
    profile =
        new HrmPayrollTaxProfileDO()
            .setId(101L)
            .setVersion(2)
            .setEmployeeId(1L)
            .setEntityId(10L)
            .setYear(2026)
            .setResidentThisYear(true)
            .setTaxpayerKey("taxpayer")
            .setTaxpayerName("虚构员工")
            .setTaxRegistrationNumber("current-agent")
            .setOpeningMode("ZERO")
            .setOpeningAsOfDate(LocalDate.of(2026, 1, 1))
            .setOpeningIncome(BigDecimal.ZERO)
            .setOpeningSocialDeduction(BigDecimal.ZERO)
            .setOpeningBasicDeduction(BigDecimal.ZERO)
            .setOpeningWithheld(BigDecimal.ZERO);
    when(profiles.requireApproved(10L, 1L, 2026)).thenReturn(profile);
    policy =
        new HrmPayrollTaxPolicyDO()
            .setEntityId(10L)
            .setYear(2026)
            .setVersion(2)
            .setMonthlyBasicDeduction(money("5000"))
            .setTaxRegistrationNumber("current-agent")
            .setStatus("APPROVED");
    policy.setResidentBands("[{\"upper\":null,\"rate\":0.03,\"quickDeduction\":0}]");
    policies.insert(policy);
    when(policyService.requireApproved(10L, 2026)).thenReturn(policy);
    when(deductions.source(10L, 1L, 2026, 9))
        .thenReturn(new HrmPayrollTaxDeductionDO().setId(201L).setStatus("APPROVED"));
    when(deductions.values(any())).thenReturn(Collections.singletonMap("260101", BigDecimal.ZERO));
    when(employment.forApprovedPayroll(10L, 1L, LocalDate.of(2026, 9, 30)))
        .thenReturn(
            new PayrollTaxEmploymentEngine.Result()
                .setBasicDeduction(money("45000"))
                .setMonths(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9)));
    when(employment.forApprovedPayroll(10L, 1L, LocalDate.of(2026, 9, 1)))
        .thenReturn(new PayrollTaxEmploymentEngine.Result().setBasicDeduction(money("45000")));
    batch =
        new HrmSalaryMonthRecordDO()
            .setEntityId(10L)
            .setYear(2026)
            .setMonth(8)
            .setExpectedPaymentDate(LocalDate.of(2026, 8, 31))
            .setStatus(17);
    batches.insert(batch);
    HrmPayrollTaxProfileDO old =
        new HrmPayrollTaxProfileDO()
            .setId(99L)
            .setTaxpayerKey("taxpayer")
            .setTaxRegistrationNumber("old-agent");
    HrmPayrollTaxWithholdingService.Source source =
        new HrmPayrollTaxWithholdingService.Source()
            .setEntityId(10L)
            .setEmployeeId(1L)
            .setProfile(old)
            .setExpectedPaymentDate(LocalDate.of(2026, 8, 31))
            .setQuote(
                new PayrollTaxPolicyEngine.Quote()
                    .setCurrentWithholding(money("100"))
                    .setCurrentSalaryWithholding(money("100"))
                    .setSeparateBonusTax(BigDecimal.ZERO));
    int[] codes = {250101, 270101, 250102, 270102, 250103, 270103};
    String[] amounts = {"0", "10000", "0", "5000", "0", "1000"};
    List<HrmSalaryMonthEmployeeRecordDO.OptionValue> values = new ArrayList<>();
    for (int i = 0; i < codes.length; i++)
      values.add(
          new HrmSalaryMonthEmployeeRecordDO.OptionValue()
              .setCode(codes[i])
              .setValue(money(amounts[i])));
    HrmSalaryMonthEmployeeRecordDO result =
        new HrmSalaryMonthEmployeeRecordDO()
            .setEmployeeId(1L)
            .setTaxPaymentDate(LocalDate.of(2026, 8, 31))
            .setRealPaySalary(money("8900"))
            .setPersonalTax(money("100"))
            .setOptionValues(values)
            .setTaxSourceSnapshot(JsonUtils.toJsonString(source));
    run =
        new HrmPayrollRunDO()
            .setMonthRecordId(batch.getId())
            .setVersion(1)
            .setComputedBy(2L)
            .setInputSnapshot("{}")
            .setRuleSnapshot("{}")
            .setResultSnapshot(JsonUtils.toJsonString(Collections.singletonList(result)));
    runs.insert(run);
    payment =
        new HrmPayrollPaymentDO()
            .setMonthRecordId(batch.getId())
            .setRunId(run.getId())
            .setEmployeeId(1L)
            .setEmployeeName("员工")
            .setBankAccount("test-only")
            .setAmount(money("8900"))
            .setValueDate(LocalDate.of(2026, 9, 1))
            .setStatus("SUCCESS")
            .setAttempt(1);
    payments.insert(payment);
  }

  private HrmPayrollTaxCorrectionReqVO request(String key, String basic) {
    return new HrmPayrollTaxCorrectionReqVO()
        .setPaymentId(payment.getId())
        .setPriorCorrectionId(corrections.selectList().stream().filter(c->"APPROVED".equals(c.getStatus())).map(HrmPayrollTaxCorrectionDO::getId).findFirst().orElse(null))
        .setBasicDeduction(money(basic))
        .setOfficialReference("amended-tax-record")
        .setEvidence("正式更正资料")
        .setReason("实际到账月份与原核算月份不同")
        .setRequestKey(key);
  }

  @Test
  void previewKeepsImmutableCashAmountsAndActualWithholding() {
    HrmPayrollTaxCorrectionDO row = service.preview(request("a", "5000"), 2L);
    assertEquals(payment.getValueDate(), row.getValueDate());
    assertEquals(0, money("10000").compareTo(row.getIncome()));
    assertEquals(0, money("100").compareTo(row.getActualSalaryWithheld()));
    assertEquals(0, corrections.selectList().size());
  }

  @Test
  void approvedCorrectionRestoresLedgerWithoutRewritingFrozenPayment() {
    String original = run.getResultSnapshot();
    assertFalse(ledger.forApprovedPayroll(10L, 1L, 2026, LocalDate.of(2026, 10, 1)).isReady());
    Long id = service.submit(request("a", "10000"), 2L);
    service.review(id, true, "核对正式资料", 3L);
    cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollTaxCashLedgerVO result =
        ledger.forApprovedPayroll(10L, 1L, 2026, LocalDate.of(2026, 10, 1));
    assertTrue(result.isReady());
    assertEquals(0, money("10000").compareTo(result.getBasicDeduction()));
    assertEquals(0, money("100").compareTo(result.getWithheld()));
    assertEquals(id, result.getRows().get(0).getCorrectionId());
    assertEquals(original, runs.selectById(run.getId()).getResultSnapshot());
    assertEquals(payment.getValueDate(), payments.selectById(payment.getId()).getValueDate());
    assertEquals(2, events.selectList().size());
  }

  @Test
  void makerAndEmployeeCannotReview() {
    Long id = service.submit(request("a", "5000"), 2L);
    assertServiceException(() -> service.review(id, true, "审核", 2L), PAYROLL_SELF_APPROVAL);
    assertServiceException(() -> service.review(id, true, "审核", 20L), PAYROLL_SELF_APPROVAL);
  }

  @Test
  void identicalRetryReturnsSameRequestAndChangedBodyIsRejected() {
    Long id = service.submit(request("a", "5000"), 2L);
    assertEquals(id, service.submit(request("a", "5000"), 2L));
    assertServiceException(
        () -> service.submit(request("a", "6000"), 2L), PAYROLL_VERSION_CONFLICT);
  }

  @Test
  void rejectedCorrectionDoesNotUnblockLedger() {
    Long id = service.submit(request("a", "5000"), 2L);
    service.review(id, false, "依据不全", 3L);
    assertFalse(ledger.forApprovedPayroll(10L, 1L, 2026, LocalDate.of(2026, 10, 1)).isReady());
  }

  @Test
  void successorKeepsHistoryAndOnlyOneApprovedOverlay() {
    Long one = service.submit(request("a", "5000"), 2L);
    service.review(one, true, "审核", 3L);
    Long two = service.submit(request("b", "10000"), 2L);
    service.review(two, true, "审核", 3L);
    assertEquals("SUPERSEDED", corrections.selectById(one).getStatus());
    assertEquals("APPROVED", corrections.selectById(two).getStatus());
    assertEquals(2, service.list(payment.getId(), 3L).size());
  }

  @Test
  void changedProfileBeforeApprovalRejectsStaleRequest() {
    Long id = service.submit(request("a", "5000"), 2L);
    profile.setId(102L);
    assertServiceException(() -> service.review(id, true, "审核", 3L), PAYROLL_VERSION_CONFLICT);
  }

  @Test
  void pendingFrozenYearBlocksCorrection() {
    Long id = service.submit(request("a", "5000"), 2L);
    claims.insert(
        new HrmPayrollSourceClaimDO()
            .setEntityId(10L)
            .setEmployeeId(1L)
            .setBatchId(900L)
            .setRunId(900L)
            .setKind("TAX_YEAR")
            .setStatus("RESERVED")
            .setAmount(money("100"))
            .setSourceKey(
                cn.hutool.crypto.digest.DigestUtil.sha256Hex("10:taxpayer:2026") + ":900"));
    assertServiceException(() -> service.review(id, true, "审核", 3L), PAYROLL_SOURCE_CLAIM_INVALID);
  }

  @Test
  void changedApprovedPolicyOrProfileReblocksAppliedCorrection() {
    Long id = service.submit(request("a", "5000"), 2L);
    service.review(id, true, "审核", 3L);
    policies.updateById(new HrmPayrollTaxPolicyDO().setId(policy.getId()).setStatus("SUPERSEDED"));
    assertFalse(ledger.forApprovedPayroll(10L, 1L, 2026, LocalDate.of(2026, 10, 1)).isReady());
  }

  @Test
  void differentIdentityAndExcessiveAnnualBasicAreRejected() {
    assertServiceException(() -> service.preview(request("a", "60001"), 2L), PAYROLL_TAX_INVALID);
    profile.setTaxpayerKey("another-person");
    assertServiceException(() -> service.preview(request("b", "5000"), 2L), PAYROLL_TAX_INVALID);
  }

  @Test
  void correctedBasicCannotExceedApprovedEmploymentEntitlement() {
    assertServiceException(() -> service.preview(request("a", "45001"), 2L), PAYROLL_TAX_INVALID);
  }

  @Test
  void filingReconstructsCorrectedMonthAndSeparatesOverWithheldFromActualTax() {
    Long id = service.submit(request("a", "10000"), 2L);
    service.review(id, true, "核对正式资料", 3L);
    cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollTaxFilingSourceVO out =
        filingSource.source(10L, 2026, 9);
    assertEquals(0, money("0").compareTo(out.getTotalTax()));
    assertEquals(0, money("100").compareTo(out.getActualWithheldTotal()));
    assertEquals(0, money("-100").compareTo(out.getPeriodTaxDifference()));
    assertEquals(1, out.getRows().size());
    cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollTaxFilingSourceVO.Row row =
        out.getRows().get(0);
    assertEquals(Collections.singletonList(id), row.getTaxCorrectionIds());
    assertEquals(0, money("10000").compareTo(row.getCumulativeIncome()));
    assertEquals(0, money("45000").compareTo(row.getCumulativeBasicDeduction()));
    assertEquals(0, money("100").compareTo(row.getCumulativeActualWithheld()));
    assertEquals(0, money("-100").compareTo(row.getCumulativeTaxBalance()));
    assertEquals(0, money("100").compareTo(row.getWithheld()));
  }

  @Test
  void filingCorrectionRequiresReadyDeductionAndCurrentApprovedBasis() {
    Long id = service.submit(request("a", "5000"), 2L);
    service.review(id, true, "审核", 3L);
    when(deductions.source(10L, 1L, 2026, 9)).thenReturn(null);
    assertServiceException(() -> filingSource.source(10L, 2026, 9), PAYROLL_TAX_INVALID);
  }
  @Test void amendedIncomeAndContributionsReachLedgerAndFilingWithoutChangingPaidFacts(){
    String frozen=run.getResultSnapshot();Long id=service.submit(request("income-a","5000").setIncome(money("8000")).setSocialDeduction(money("800")),2L);
    assertFalse(ledger.forTaxFiling(10L,1L,2026,LocalDate.of(2026,9,30)).isReady());
    service.review(id,true,"核实正式收入及缴费更正",3L);
    cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollTaxCashLedgerVO out=ledger.forTaxFiling(10L,1L,2026,LocalDate.of(2026,9,30));assertTrue(out.isReady());assertEquals(0,money("8000").compareTo(out.getIncome()));assertEquals(0,money("800").compareTo(out.getSocialDeduction()));assertEquals(0,money("100").compareTo(out.getWithheld()));
    cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollTaxFilingSourceVO source=filingSource.source(10L,2026,9);assertEquals(0,money("8000").compareTo(source.getRows().get(0).getIncome()));assertEquals(0,money("800").compareTo(source.getRows().get(0).getSocialDeduction()));assertEquals(0,money("100").compareTo(source.getActualWithheldTotal()));assertEquals(frozen,runs.selectById(run.getId()).getResultSnapshot());assertEquals(0,money("8900").compareTo(payments.selectById(payment.getId()).getAmount()));
  }
  @Test void omittedSuccessorAmountsKeepApprovedValuesAndStalePredecessorIsRejected(){
    HrmPayrollTaxCorrectionReqVO stale=request("stale","6000");Long first=service.submit(request("first","5000").setIncome(money("8000")).setSocialDeduction(money("800")),2L);service.review(first,true,"核实",3L);
    assertServiceException(()->service.submit(stale,2L),PAYROLL_VERSION_CONFLICT);
    Long next=service.submit(request("next","10000"),2L);assertEquals(0,money("8000").compareTo(corrections.selectById(next).getIncome()));assertEquals(0,money("800").compareTo(corrections.selectById(next).getSocialDeduction()));service.review(next,true,"接续仅更正减除费用",3L);assertEquals("SUPERSEDED",corrections.selectById(first).getStatus());assertEquals(first,corrections.selectById(next).getPriorCorrectionId());
  }
  @Test void explicitZeroIncomeAndContributionsArePreservedWhileNegativeValuesFail(){
    assertServiceException(()->service.preview(request("neg","0").setIncome(money("-1")),2L),PAYROLL_TAX_INVALID);assertServiceException(()->service.preview(request("neg-social","0").setSocialDeduction(money("-1")),2L),PAYROLL_TAX_INVALID);
    Long id=service.submit(request("zero","0").setIncome(BigDecimal.ZERO).setSocialDeduction(BigDecimal.ZERO),2L);service.review(id,true,"原收入撤销的正式税务资料",3L);cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollTaxCashLedgerVO out=ledger.forTaxFiling(10L,1L,2026,LocalDate.of(2026,9,30));assertTrue(out.isReady());assertEquals(0,BigDecimal.ZERO.compareTo(out.getIncome()));assertEquals(0,BigDecimal.ZERO.compareTo(out.getSocialDeduction()));assertEquals(0,money("100").compareTo(out.getWithheld()));
  }
  @Test void priorYearAmendmentStaysInOriginalCashYearAndCannotRewriteCurrentYear(){
    HrmPayrollTaxProfileDO current=JsonUtils.parseObject(JsonUtils.toJsonString(profile),HrmPayrollTaxProfileDO.class);
    profile.setYear(2025).setOpeningAsOfDate(LocalDate.of(2025,1,1));policy.setYear(2025);policies.updateById(policy);when(profiles.requireApproved(10L,1L,2025)).thenReturn(profile);when(profiles.requireApproved(10L,1L,2026)).thenReturn(current);when(policyService.requireApproved(10L,2025)).thenReturn(policy);
    when(employment.forApprovedPayroll(10L,1L,LocalDate.of(2025,9,1))).thenReturn(new PayrollTaxEmploymentEngine.Result().setBasicDeduction(money("45000")));
    payment.setValueDate(LocalDate.of(2025,9,1));payments.updateById(payment);
    Long id=service.submit(request("prior-year","5000").setIncome(money("8000")),2L);service.review(id,true,"原年度更正",3L);
    cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollTaxCashLedgerVO prior=ledger.forTaxFiling(10L,1L,2025,LocalDate.of(2025,12,31)),present=ledger.forTaxFiling(10L,1L,2026,LocalDate.of(2026,9,30));assertTrue(prior.isReady());assertEquals(0,money("8000").compareTo(prior.getIncome()));assertTrue(present.isReady());assertEquals(0,BigDecimal.ZERO.compareTo(present.getIncome()));assertTrue(present.getRows().isEmpty());assertEquals(LocalDate.of(2025,9,1),corrections.selectById(id).getValueDate());
  }
  @Test void openingCoveredAmendmentRequiresExplicitIndependentConfirmationAndDoesNotDoubleApply(){
    profile.setOpeningMode("DECLARE").setOpeningAsOfDate(LocalDate.of(2026,9,30)).setOpeningIncome(money("8000")).setOpeningSocialDeduction(money("800")).setOpeningBasicDeduction(money("5000")).setOpeningWithheld(money("100"));
    HrmPayrollTaxCorrectionReqVO req=request("covered","5000").setIncome(money("8000")).setSocialDeduction(money("800"));assertServiceException(()->service.preview(req,2L),PAYROLL_TAX_OPENING_CORRECTION_UNCONFIRMED);req.setOpeningAlreadyIncludesCorrection(true);Long id=service.submit(req,2L);service.review(id,true,"已核实批准历史累计包含更正",3L);
    cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollTaxCashLedgerVO out=ledger.forTaxFiling(10L,1L,2026,LocalDate.of(2026,10,1));assertTrue(out.isReady());assertEquals(0,money("8000").compareTo(out.getIncome()));assertEquals("OPENING_COVERED",out.getRows().get(0).getStatus());
    corrections.update(null,new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<HrmPayrollTaxCorrectionDO>().eq(HrmPayrollTaxCorrectionDO::getId,id).set(HrmPayrollTaxCorrectionDO::getRequestSnapshot,null));assertFalse(ledger.forTaxFiling(10L,1L,2026,LocalDate.of(2026,10,1)).isReady());
  }

}
