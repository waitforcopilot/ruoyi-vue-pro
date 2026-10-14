package cn.iocoder.yudao.module.hrm.service.payroll;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.employee.info.HrmEmployeeMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import javax.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

@Import({
  HrmPayrollTaxSettlementService.class,
  org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration.class
})
class HrmPayrollTaxSettlementServiceTest extends BaseDbUnitTest {
  @Resource private HrmPayrollTaxSettlementService service;
  @Resource private HrmPayrollTaxSettlementMapper settlements;
  @Resource private HrmPayrollTaxSettlementEventMapper events;
  @Resource private HrmPayrollTaxFilingMapper filings;
  @Resource private HrmPayrollTaxProfileMapper profiles;
  @Resource private HrmEmployeeMapper employees;
  @MockBean private HrmPayrollTaxFilingService filingService;
  @MockBean private HrmPayrollEntityService entities;
  private HrmPayrollTaxFilingDO filing;
  private HrmPayrollTaxFilingSourceVO source;

  private static BigDecimal money(String value) {
    return new BigDecimal(value);
  }

  @BeforeEach
  void setup() {
    employees.insert(new HrmEmployeeDO().setId(1L).setName("虚构员工").setUserId(20L));
    profiles.insert(
        new HrmPayrollTaxProfileDO()
            .setId(90L)
            .setEntityId(10L)
            .setEmployeeId(1L)
            .setYear(2026)
            .setTaxpayerKey("person")
            .setTaxRegistrationNumber("agency")
            .setStatus("APPROVED"));
    source =
        new HrmPayrollTaxFilingSourceVO()
            .setEntityId(10L)
            .setYear(2026)
            .setMonth(9)
            .setTaxRegistrationNumber("agency")
            .setTotalTax(BigDecimal.ZERO)
            .setRows(
                Collections.singletonList(
                    new HrmPayrollTaxFilingSourceVO.Row()
                        .setProfileId(90L)
                        .setEmployeeIds(Collections.singletonList(1L))
                        .setMode("RESIDENT_WAGE")
                        .setWithheld(money("100"))
                        .setExpectedSalaryWithholding(BigDecimal.ZERO)));
    filing =
        new HrmPayrollTaxFilingDO()
            .setEntityId(10L)
            .setYear(2026)
            .setMonth(9)
            .setVersion(1)
            .setStatus("RECONCILED")
            .setExpectedTax(BigDecimal.ZERO)
            .setSourceSnapshot(JsonUtils.toJsonString(source));
    filings.insert(filing);
    when(filingService.detail(anyLong(), anyLong()))
        .thenReturn(Collections.singletonMap("sourceFresh", true));
  }

  private HrmPayrollTaxSettlementReqVO request(String key, String kind, String amount) {
    return new HrmPayrollTaxSettlementReqVO()
        .setFilingId(filing.getId())
        .setEmployeeId(1L)
        .setMode("RESIDENT_WAGE")
        .setKind(kind)
        .setAmount(money(amount))
        .setEmployeeAcknowledgement("核实员工收付款确认依据")
        .setEvidence("正式申报及税款凭据")
        .setReason("处理更正差额")
        .setRequestKey(key);
  }

  private Long approved(String key, String kind, String amount) {
    Long id = service.submit(request(key, kind, amount), 2L);
    service.review(id, true, "独立核实来源及余额", 3L);
    return id;
  }

  private HrmPayrollTaxSettlementConfirmReqVO bank(Long id, String reference, String amount) {
    return new HrmPayrollTaxSettlementConfirmReqVO()
        .setId(id)
        .setValueDate(LocalDate.now(ZoneId.of("Asia/Shanghai")))
        .setAmount(money(amount))
        .setBankReference(reference)
        .setEvidence("核对真实单笔银行凭据");
  }

  @Test
  void approvedRefundRequiresActualBankFactsAndKeepsOriginalTax() {
    Long id = approved("a", "REFUND", "100");
    assertEquals("APPROVED", settlements.selectById(id).getStatus());
    assertEquals(
        0, money("-100").compareTo(service.balances(filing.getId(), 3L).get(0).getRemaining()));
    service.confirm(bank(id, "BANK:REFUND:1", "100"), 3L);
    assertEquals("SETTLED", settlements.selectById(id).getStatus());
    assertEquals(
        0, BigDecimal.ZERO.compareTo(service.balances(filing.getId(), 3L).get(0).getRemaining()));
    assertEquals(0, money("100").compareTo(source.getRows().get(0).getWithheld()));
    assertEquals(3, events.selectList().size());
  }

  @Test
  void makerAndBeneficiaryCannotReviewOrConfirm() {
    Long id = service.submit(request("a", "REFUND", "100"), 2L);
    assertServiceException(() -> service.review(id, true, "审核", 2L), PAYROLL_SELF_APPROVAL);
    assertServiceException(() -> service.review(id, true, "审核", 20L), PAYROLL_SELF_APPROVAL);
    service.review(id, true, "审核", 3L);
    assertServiceException(() -> service.confirm(bank(id, "1", "100"), 2L), PAYROLL_SELF_APPROVAL);
  }

  @Test
  void pendingReservationsAndPartialPaymentsCannotOverRefund() {
    Long one = approved("a", "REFUND", "60");
    assertServiceException(
        () -> service.submit(request("b", "REFUND", "50"), 2L), PAYROLL_TAX_INVALID);
    service.confirm(bank(one, "1", "60"), 3L);
    Long two = approved("b", "REFUND", "40");
    service.confirm(bank(two, "2", "40"), 3L);
    assertEquals(
        0, BigDecimal.ZERO.compareTo(service.balances(filing.getId(), 3L).get(0).getRemaining()));
    assertServiceException(
        () -> service.submit(request("c", "REFUND", "1"), 2L), PAYROLL_TAX_INVALID);
  }

  @Test
  void unchangedRetryReturnsSameClaimChangedBodyIsRejected() {
    Long id = service.submit(request("a", "REFUND", "100"), 2L);
    assertEquals(id, service.submit(request("a", "REFUND", "100"), 2L));
    assertServiceException(
        () -> service.submit(request("a", "REFUND", "50"), 2L), PAYROLL_VERSION_CONFLICT);
  }

  @Test
  void bankReferenceIsGloballyUniqueAndMoneyCannotBeOverwritten() {
    Long one = approved("a", "REFUND", "60");
    service.confirm(bank(one, "BANK:1", "60"), 3L);
    service.confirm(bank(one, "BANK:1", "60"), 3L);
    assertServiceException(
        () -> service.confirm(bank(one, "BANK:2", "60"), 3L), PAYROLL_VERSION_CONFLICT);
    Long two = approved("b", "REFUND", "40");
    assertServiceException(
        () -> service.confirm(bank(two, "BANK:1", "40"), 3L), PAYROLL_TAX_INVALID);
    assertEquals("APPROVED", settlements.selectById(two).getStatus());
  }

  @Test
  void wrongAmountFutureDateAndUnapprovedStateAreRejected() {
    Long id = service.submit(request("a", "REFUND", "100"), 2L);
    assertServiceException(() -> service.confirm(bank(id, "1", "100"), 3L), PAYROLL_TAX_INVALID);
    service.review(id, true, "审核", 3L);
    assertServiceException(() -> service.confirm(bank(id, "1", "99"), 3L), PAYROLL_TAX_INVALID);
    assertServiceException(
        () ->
            service.confirm(
                bank(id, "1", "100")
                    .setValueDate(LocalDate.now(ZoneId.of("Asia/Shanghai")).plusDays(1)),
                3L),
        PAYROLL_TAX_INVALID);
  }

  @Test
  void rejectedOrCancelledClaimsReleaseReservationsButPaidCashCannotBeCancelled() {
    Long first = service.submit(request("a", "REFUND", "100"), 2L);
    service.review(first, false, "资料不全", 3L);
    Long second = approved("b", "REFUND", "100");
    assertServiceException(() -> service.cancel(second, "撤回", null, 3L), PAYROLL_TAX_INVALID);
    service.cancel(second, "核实银行未执行", "银行作废确认", 3L);
    Long third = approved("c", "REFUND", "100");
    service.confirm(bank(third, "1", "100"), 3L);
    assertServiceException(() -> service.cancel(third, "撤回", "1", 3L), PAYROLL_TAX_INVALID);
  }

  @Test
  void successorDeclarationUsesAllEarlierCashClearingAndAllowsRecovery() {
    Long refunded = approved("a", "REFUND", "100");
    service.confirm(bank(refunded, "REFUND:1", "100"), 3L);
    source.getRows().get(0).setExpectedSalaryWithholding(money("50"));
    source.setTotalTax(money("50"));
    filings.updateById(new HrmPayrollTaxFilingDO().setId(filing.getId()).setStatus("SUPERSEDED"));
    filing =
        new HrmPayrollTaxFilingDO()
            .setEntityId(10L)
            .setYear(2026)
            .setMonth(9)
            .setVersion(2)
            .setStatus("RECONCILED")
            .setExpectedTax(money("50"))
            .setSourceSnapshot(JsonUtils.toJsonString(source));
    filings.insert(filing);
    Long recovery = approved("b", "RECOVERY", "50");
    service.confirm(bank(recovery, "RECOVERY:1", "50"), 3L);
    assertEquals(
        0, BigDecimal.ZERO.compareTo(service.balances(filing.getId(), 3L).get(0).getRemaining()));
    assertEquals(2, service.list(filing.getId(), 3L).size());
  }

  @Test
  void unreconciledAndStaleGovernmentBasisCannotApproveCash() {
    filings.updateById(new HrmPayrollTaxFilingDO().setId(filing.getId()).setStatus("FILED"));
    assertServiceException(
        () -> service.submit(request("a", "REFUND", "100"), 2L), PAYROLL_TAX_INVALID);
    filings.updateById(new HrmPayrollTaxFilingDO().setId(filing.getId()).setStatus("RECONCILED"));
    Long id = service.submit(request("a", "REFUND", "100"), 2L);
    when(filingService.detail(anyLong(), anyLong()))
        .thenReturn(Collections.singletonMap("sourceFresh", false));
    assertServiceException(() -> service.review(id, true, "审核", 3L), PAYROLL_TAX_INVALID);
  }

  @Test
  void entityScopeIsCheckedAndUnknownEmployeesCannotReceiveMoney() {
    doThrow(new IllegalStateException()).when(entities).check(10L, "MANAGE", 9L);
    assertThrows(
        IllegalStateException.class, () -> service.submit(request("a", "REFUND", "100"), 9L));
    assertServiceException(
        () -> service.submit(request("b", "REFUND", "100").setEmployeeId(99L), 2L),
        PAYROLL_TAX_INVALID);
  }
}
