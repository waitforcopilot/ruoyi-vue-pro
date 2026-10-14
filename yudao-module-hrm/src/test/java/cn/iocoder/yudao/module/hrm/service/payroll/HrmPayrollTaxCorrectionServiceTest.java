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
  org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration.class
})
class HrmPayrollTaxCorrectionServiceTest extends BaseDbUnitTest {
  @Resource private HrmPayrollTaxCorrectionService service;
  @Resource private HrmPayrollTaxCashLedgerService ledger;
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
    policies.insert(policy);
    when(policyService.requireApproved(10L, 2026)).thenReturn(policy);
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
}
