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
import java.time.LocalDate;
import java.util.*;
import javax.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

@Import({
  HrmPayrollTaxFilingService.class,
  org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration.class
})
class HrmPayrollTaxFilingServiceTest extends BaseDbUnitTest {
  @Resource private HrmPayrollTaxFilingService service;
  @Resource private HrmPayrollTaxFilingMapper filings;
  @Resource private HrmPayrollTaxReceiptMapper receipts;
  @MockBean private HrmPayrollTaxFilingSourceService sources;
  @MockBean private HrmPayrollEntityService entities;
  @MockBean private HrmPayrollTaxProfileService profiles;
  private HrmPayrollTaxFilingSourceVO source;

  private static BigDecimal money(String text) {
    return new BigDecimal(text);
  }

  @BeforeEach
  void setup() {
    source =
        new HrmPayrollTaxFilingSourceVO()
            .setEntityId(10L)
            .setYear(2026)
            .setMonth(9)
            .setPolicyId(1L)
            .setPolicyVersion(1)
            .setTaxRegistrationNumber("test-agency")
            .setTotalTax(money("120"));
    source.setRows(
        new ArrayList<>(
            Collections.singletonList(
                new HrmPayrollTaxFilingSourceVO.Row()
                    .setProfileId(90L)
                    .setTaxpayerName("虚构税务员工")
                    .setMaskedIdentifier("TEST***90")
                    .setTaxpayerKey("test-person")
                    .setMode("RESIDENT_WAGE")
                    .setWithheld(money("120")))));
    when(sources.source(10L, 2026, 9)).thenAnswer(invocation -> source);
    when(profiles.privateIdentifier(90L, 2L)).thenReturn("FICTIONAL-TAX-IDENTITY");
  }

  private HrmPayrollTaxFilingReqVO req() {
    return new HrmPayrollTaxFilingReqVO()
        .setEntityId(10L)
        .setYear(2026)
        .setMonth(9)
        .setZeroDeclaration(false)
        .setEvidence("核对本期全部到账及冻结税务资料")
        .setRequestKey(UUID.randomUUID().toString());
  }

  private Long approved() {
    Long id = service.create(req(), 1L);
    service.review(id, true, "独立复核全部申报来源", 2L);
    return id;
  }

  private Long filed() {
    Long id = approved();
    service.filed(id, "FILED-" + id, LocalDate.of(2026, 10, 1), "正式税务申报受理凭据", 1L);
    return id;
  }

  private HrmPayrollTaxReceiptReqVO receipt(Long filing, String amount) {
    return new HrmPayrollTaxReceiptReqVO()
        .setFilingId(filing)
        .setKind("PAYMENT")
        .setReference(UUID.randomUUID().toString())
        .setAmount(money(amount))
        .setPaymentDate(LocalDate.of(2026, 10, 2))
        .setEvidence("核对主管税务机关正式完税凭证")
        .setRequestKey(UUID.randomUUID().toString());
  }

  private Long paid(Long filing, String amount) {
    Long id = service.receipt(receipt(filing, amount), 1L);
    service.reviewReceipt(id, true, "独立核对税款缴付凭据", 2L);
    return id;
  }

  @Test
  void reviewIsIndependentAndSourceChangesBlockApprovalOrExport() {
    Long id = service.create(req(), 1L);
    assertServiceException(() -> service.review(id, true, "本人复核", 1L), PAYROLL_SELF_APPROVAL);
    assertServiceException(() -> service.export(id, 2L), PAYROLL_TAX_INVALID);
    source.setTotalTax(money("121"));
    assertServiceException(() -> service.review(id, true, "来源已变化", 2L), PAYROLL_VERSION_CONFLICT);
    assertEquals("PENDING", filings.selectById(id).getStatus());
  }

  @Test
  void approvedExportUsesAuditedIdentityAndDoesNotStorePlaintext() {
    Long id = approved();
    List<HrmPayrollTaxFilingExportRowVO> output = service.export(id, 2L);
    assertEquals(1, output.size());
    assertEquals("FICTIONAL-TAX-IDENTITY", output.get(0).getIdentifier());
    assertEquals("2026-09", output.get(0).getPeriod());
    assertFalse(filings.selectById(id).getSourceSnapshot().contains("FICTIONAL-TAX-IDENTITY"));
    assertFalse(
        cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(service.detail(id, 2L))
            .contains("test-person"));
    verify(profiles).privateIdentifier(90L, 2L);
    source.setPolicyVersion(2);
    assertServiceException(() -> service.export(id, 2L), PAYROLL_VERSION_CONFLICT);
  }

  @Test
  void idempotencyCannotReplaceBodies() {
    HrmPayrollTaxFilingReqVO req = req();
    Long id = service.create(req, 1L);
    assertEquals(id, service.create(req, 1L));
    req.setEvidence("替换申请依据");
    assertServiceException(() -> service.create(req, 1L), PAYROLL_TAX_INVALID);
  }

  @Test
  void zeroDeclarationRequiresExplicitEmptySourceAndClosedPeriod() {
    source.setRows(new ArrayList<>()).setTotalTax(BigDecimal.ZERO);
    assertServiceException(() -> service.create(req(), 1L), PAYROLL_TAX_INVALID);
    Long id = service.create(req().setZeroDeclaration(true), 1L);
    service.review(id, true, "独立核实无本期到账", 2L);
    service.filed(id, "ZERO-REF", LocalDate.of(2026, 10, 1), "正式零申报凭据", 1L);
    service.reconcile(id, "核实零申报且无缴税额", 2L);
    assertEquals("RECONCILED", filings.selectById(id).getStatus());
    assertServiceException(() -> service.create(req().setMonth(12), 1L), PAYROLL_TAX_INVALID);
  }

  @Test
  void officialReceiptMustBeReviewedAndMatchTaxBeforeReconciliation() {
    Long filing = filed();
    Long receipt = service.receipt(receipt(filing, "120"), 1L);
    assertServiceException(
        () -> service.reviewReceipt(receipt, true, "提交人本人", 1L), PAYROLL_SELF_APPROVAL);
    assertServiceException(() -> service.reconcile(filing, "待复核凭证不计入", 2L), PAYROLL_TAX_INVALID);
    service.reviewReceipt(receipt, true, "独立核对正式凭证", 2L);
    service.reconcile(filing, "正式缴税与本期预扣一致", 2L);
    assertEquals("RECONCILED", filings.selectById(filing).getStatus());
    assertEquals(
        0, money("0").compareTo((BigDecimal) service.detail(filing, 2L).get("difference")));
  }

  @Test
  void refundReferencesApprovedPaymentAndCannotExceedPaidAmount() {
    Long filing = filed();
    Long payment = paid(filing, "150");
    assertServiceException(() -> service.reconcile(filing, "缴税额高于本期预扣", 2L), PAYROLL_TAX_INVALID);
    Long refund =
        service.receipt(receipt(filing, "30").setKind("REFUND").setOriginalReceiptId(payment), 1L);
    service.reviewReceipt(refund, true, "核对正式退税凭据", 2L);
    service.reconcile(filing, "缴退净额与本期预扣一致", 2L);
    assertServiceException(
        () ->
            service.receipt(
                receipt(filing, "121").setKind("REFUND").setOriginalReceiptId(payment), 1L),
        PAYROLL_TAX_INVALID);
    assertServiceException(
        () -> service.receipt(receipt(filing, "1").setKind("REFUND"), 1L), PAYROLL_TAX_INVALID);
  }

  @Test
  void pendingRefundsRecheckCumulativeCapOnApproval() {
    Long filing = filed();
    Long payment = paid(filing, "150");
    Long
        one =
            service.receipt(
                receipt(filing, "100").setKind("REFUND").setOriginalReceiptId(payment), 1L),
        two =
            service.receipt(
                receipt(filing, "100").setKind("REFUND").setOriginalReceiptId(payment), 1L);
    service.reviewReceipt(one, true, "核实第一笔退款", 2L);
    assertServiceException(
        () -> service.reviewReceipt(two, true, "第二笔超出原付款", 2L), PAYROLL_TAX_INVALID);
    assertEquals("PENDING", receipts.selectById(two).getStatus());
  }

  @Test
  void receiptIdempotencyAndRejectedReferenceCorrectionPreserveHistory() {
    Long filing = filed();
    HrmPayrollTaxReceiptReqVO req = receipt(filing, "120");
    Long id = service.receipt(req, 1L);
    assertEquals(id, service.receipt(req, 1L));
    service.reviewReceipt(id, false, "附件不清晰，重新提供凭据", 2L);
    Long corrected = service.receipt(receipt(filing, "120").setReference(req.getReference()), 1L);
    assertNotEquals(id, corrected);
    assertEquals("REJECTED", receipts.selectById(id).getStatus());
  }

  @Test
  void supplementalFilingVersionsReuseOfficialPeriodReceipts() {
    Long old = filed();
    paid(old, "120");
    source.setTotalTax(money("125"));
    Long a = service.create(req().setPriorFilingId(old), 1L),
        b = service.create(req().setPriorFilingId(old), 1L);
    service.review(a, true, "接续本期新增到账来源", 2L);
    assertServiceException(() -> service.review(b, true, "旧接续基线", 2L), PAYROLL_VERSION_CONFLICT);
    service.filed(a, "AMEND-REF", LocalDate.of(2026, 10, 3), "正式更正申报受理", 1L);
    assertEquals(0, money("5").compareTo((BigDecimal) service.detail(a, 2L).get("difference")));
    paid(a, "5");
    service.reconcile(a, "按全部本期正式凭证对账", 2L);
    assertEquals("SUPERSEDED", filings.selectById(old).getStatus());
    assertNotNull(filings.selectById(old).getFiledDate());
  }

  @Test
  void importIsAtomicAndReplayDoesNotDuplicateReceipts() {
    Long filing = filed();
    HrmPayrollTaxReceiptImportRowVO good =
        new HrmPayrollTaxReceiptImportRowVO()
            .setKind("缴税")
            .setReference("IMPORT-REF")
            .setAmount(money("120"))
            .setPaymentDate("2026-10-02")
            .setEvidence("正式凭据及所属期核实");
    HrmPayrollTaxReceiptImportRowVO bad =
        new HrmPayrollTaxReceiptImportRowVO()
            .setKind("缴税")
            .setReference("INVALID-REF")
            .setAmount(BigDecimal.ZERO)
            .setPaymentDate("2026-10-02")
            .setEvidence("无效金额");
    assertServiceException(
        () -> service.importReceipts(filing, Arrays.asList(good, bad), "bad-file", 1L),
        PAYROLL_TAX_INVALID);
    assertTrue(receipts.selectList().isEmpty());
    List<Long> ids =
        service.importReceipts(filing, Collections.singletonList(good), "good-file", 1L);
    assertEquals(
        ids, service.importReceipts(filing, Collections.singletonList(good), "good-file", 1L));
    assertEquals(1, receipts.selectList().size());
  }

  @Test
  void entityReadManageAndReviewScopeAreEnforcedBeforeReturningEvidence() {
    Long filing = filed();
    doThrow(new IllegalStateException("denied")).when(entities).check(10L, "READ", 9L);
    assertThrows(IllegalStateException.class, () -> service.detail(filing, 9L));
    assertThrows(IllegalStateException.class, () -> service.exportReceipts(filing, 9L));
    doThrow(new IllegalStateException("denied")).when(entities).check(10L, "REVIEW", 9L);
    assertThrows(IllegalStateException.class, () -> service.reconcile(filing, "无权限", 9L));
  }
}
