package cn.iocoder.yudao.module.hrm.service.payroll;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollEntityReqVO;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollInsuranceReqVO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.employee.info.HrmEmployeeMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.*;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import javax.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

@Import({
  HrmPayrollInsurancePolicyService.class,
  HrmPayrollInsuranceBillService.class,
  HrmPayrollEntityService.class,
  org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration.class
})
class HrmPayrollInsuranceBillServiceTest extends BaseDbUnitTest {
  @Resource private HrmPayrollInsurancePolicyService service;
  @Resource private HrmPayrollInsuranceBillService ledger;
  @Resource private HrmPayrollInsuranceBillMapper bills;
  @Resource private HrmPayrollInsuranceBillLineMapper lines;
  @Resource private HrmPayrollInsuranceSettlementMapper receipts;
  @Resource private HrmPayrollEntityService entities;
  @Resource private HrmPayrollInsurancePolicyMapper policies;
  @Resource private HrmPayrollInsuranceBaseMapper bases;
  @Resource private HrmEmployeeMapper employees;
  @MockBean private AdminUserApi users;
  @MockBean private PermissionApi permissions;
  @MockBean private DeptApi departments;

  private LocalDate day(int month, int d) {
    return LocalDate.of(2026, month, d);
  }

  private Long entity(String code) {
    HrmPayrollEntityReqVO r =
        new HrmPayrollEntityReqVO()
            .setCode(code)
            .setName(code + "法人")
            .setTaxRegistrationNumber("914400000000000001")
            .setEvidence("登记核实");
    Long id = entities.create(r, 1L);
    HrmPayrollEntityReqVO.Grant grant =
        new HrmPayrollEntityReqVO.Grant()
            .setEntityId(id)
            .setUserId(2L)
            .setActions(new HashSet<>(Arrays.asList("READ", "REVIEW", "APPROVE")))
            .setEffectiveFrom(LocalDate.now(ZoneId.of("Asia/Shanghai")).minusDays(1))
            .setEffectiveTo(LocalDate.now(ZoneId.of("Asia/Shanghai")).plusYears(1))
            .setReason("复核审批职责");
    entities.grant(grant, 1L);
    grant.setUserId(3L);
    entities.grant(grant, 1L);
    entities.approve(id, "核实", 2L);
    return id;
  }

  private Long employee(String job) {
    HrmEmployeeDO e = new HrmEmployeeDO().setName("参保员工").setJobNumber(job);
    employees.insert(e);
    return e.getId();
  }

  private HrmPayrollInsuranceReqVO.Project project(
      String code, int type, String min, String max, String personal, String corporate) {
    return new HrmPayrollInsuranceReqVO.Project()
        .setCode(code)
        .setName(code)
        .setType(type)
        .setMinimumBase(new BigDecimal(min))
        .setMaximumBase(new BigDecimal(max))
        .setPersonalRate(new BigDecimal(personal))
        .setCorporateRate(new BigDecimal(corporate));
  }

  private HrmPayrollInsuranceReqVO policyRequest(Long entity) {
    return new HrmPayrollInsuranceReqVO()
        .setEntityId(entity)
        .setCode("SZ-2026")
        .setCity("深圳市")
        .setEffectiveFrom(day(1, 1))
        .setEffectiveTo(day(12, 31))
        .setEvidence("用于测试的核定政策资料")
        .setProjects(
            Arrays.asList(
                project("PENSION", 1, "6000", "30000", "8", "16"),
                project("FUND", 10, "2500", "30000", "5", "5")));
  }

  private Long policy(Long entity) {
    Long id = service.createPolicy(policyRequest(entity), 1L);
    service.approvePolicy(id, "核定已复核", 2L);
    return id;
  }

  private HrmPayrollInsuranceReqVO.Base request(Long entity, Long employee, Long policy) {
    Map<String, BigDecimal> declared = new LinkedHashMap<>();
    declared.put("PENSION", new BigDecimal("3000"));
    declared.put("FUND", new BigDecimal("50000"));
    return new HrmPayrollInsuranceReqVO.Base()
        .setEntityId(entity)
        .setEmployeeId(employee)
        .setPolicyId(policy)
        .setCoverage("INSURED")
        .setIncomeYear(2025)
        .setEffectiveFrom(day(1, 1))
        .setEffectiveTo(day(12, 31))
        .setDeclaredBases(declared)
        .setEvidence("上年度工资汇总及参保主体申报依据");
  }

  private Long approved(HrmPayrollInsuranceReqVO.Base request) {
    Long id = service.proposeBase(request, 1L);
    service.reviewBase(id, "收入来源与主体已复核", 2L);
    service.approveBase(id, "批准申报基数", 3L);
    return id;
  }

  private List<cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollInsuranceBillRowVO>
      statement(String personal, String corporate) {
    return Arrays.asList(
        new cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollInsuranceBillRowVO()
            .setJobNumber("INS-1")
            .setMonth("2026-01")
            .setProjectCode("PENSION")
            .setPersonal(personal)
            .setCorporate(corporate),
        new cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollInsuranceBillRowVO()
            .setJobNumber("INS-1")
            .setMonth("2026-01")
            .setProjectCode("FUND")
            .setPersonal("1500.00")
            .setCorporate("1500.00"));
  }

  private HrmPayrollInsuranceBillDO preview(
      Long entity,
      List<cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollInsuranceBillRowVO>
          rows) {
    return ledger.preview(
        entity,
        YearMonth.of(2026, 1),
        "缴费机构账单编号001",
        "正式账单及核实依据",
        "statement.xlsx",
        String.join("", Collections.nCopies(64, "a")),
        rows,
        1L);
  }

  private Long setup() {
    Long e = entity("A"), person = employee("INS-1"), p = policy(e);
    approved(request(e, person, p));
    return e;
  }

  private void approveBill(HrmPayrollInsuranceBillDO bill) {
    ledger.submit(bill.getId(), 1L);
    ledger.review(bill.getId(), "HR核验账单", 2L);
    ledger.approve(bill.getId(), "财务核验账单", 3L);
  }

  private cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollInsuranceSettlementReqVO
      receipt(Long line, String mode, String personal, String corporate) {
    return new cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo
            .HrmPayrollInsuranceSettlementReqVO()
        .setLineId(line)
        .setMode(mode)
        .setPersonal(new BigDecimal(personal))
        .setCorporate(new BigDecimal(corporate))
        .setReference("机构缴费回执001")
        .setEvidence("回执原件已核实");
  }

  private Long propose(
      cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollInsuranceSettlementReqVO r,
      String proof) {
    return ledger.propose(
        r, proof.getBytes(java.nio.charset.StandardCharsets.UTF_8), "receipt.pdf", 1L);
  }

  private void approveReceipt(Long id) {
    ledger.transitionSettlement(id, "review", "HR核验回执", 2L);
    ledger.transitionSettlement(id, "approve", "财务核验回执", 3L);
  }

  @Test
  void matchedStatementIsImmutableAndRequiresIndependentReviewApproval() {
    Long e = setup();
    HrmPayrollInsuranceBillDO bill = preview(e, statement("480", "960"));
    assertEquals("VALIDATED", bill.getStatus());
    assertTrue(lines.selectList().isEmpty());
    ledger.submit(bill.getId(), 1L);
    assertServiceException(() -> ledger.review(bill.getId(), "通过", 1L), PAYROLL_SELF_APPROVAL);
    ledger.review(bill.getId(), "核验", 2L);
    assertServiceException(() -> ledger.approve(bill.getId(), "通过", 2L), PAYROLL_SELF_APPROVAL);
    ledger.approve(bill.getId(), "核验", 3L);
    assertEquals("RECONCILED", bills.selectById(bill.getId()).getStatus());
    assertEquals(2, lines.selectList().size());
    assertEquals(bill.getId(), preview(e, statement("480", "960")).getId());
  }

  @Test
  void partialBackpaymentsCloseDifferenceAndKeepOriginalActualsAndProof() {
    Long e = setup();
    HrmPayrollInsuranceBillDO bill = preview(e, statement("0", "0"));
    approveBill(bill);
    Long line = ledger.lines(bill.getId(), 1L).get(0).getId();
    cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollInsuranceSettlementReqVO r =
        receipt(line, "PAY", "200", "400");
    Long first = propose(r, "first receipt");
    assertEquals(first, propose(r, "first receipt"));
    assertEquals(new BigDecimal("0.00"), lines.selectById(line).getActualPersonal());
    assertServiceException(
        () -> ledger.transitionSettlement(first, "review", "核验", 1L), PAYROLL_SELF_APPROVAL);
    ledger.transitionSettlement(first, "review", "核验", 2L);
    assertServiceException(
        () -> ledger.transitionSettlement(first, "approve", "核验", 2L), PAYROLL_SELF_APPROVAL);
    ledger.transitionSettlement(first, "approve", "核验", 3L);
    assertEquals("UNDERPAID", lines.selectById(line).getStatus());
    assertEquals(new BigDecimal("200.00"), lines.selectById(line).getActualPersonal());
    Long second = propose(receipt(line, "PAY", "280", "560"), "second receipt");
    approveReceipt(second);
    assertEquals("RECONCILED", bills.selectById(bill.getId()).getStatus());
    assertEquals(new BigDecimal("0.00"), lines.selectById(line).getOriginalPersonal());
    assertEquals(new BigDecimal("480.00"), lines.selectById(line).getActualPersonal());
    assertEquals(first, propose(r, "first receipt"));
    assertNull(ledger.settlements(bill.getId(), 1L).get(0).getProofContent());
    assertNotNull(ledger.proof(first, 1L).getProofContent());
    assertServiceException(() -> ledger.proof(first, 2L), PAYROLL_ENTITY_INVALID);
  }

  @Test
  void refundsDoNotMaskOverpaymentAndCannotExceedDifference() {
    Long e = setup();
    HrmPayrollInsuranceBillDO bill = preview(e, statement("500", "1000"));
    approveBill(bill);
    Long line = ledger.lines(bill.getId(), 1L).get(0).getId();
    assertServiceException(
        () -> propose(receipt(line, "PAY", "20", "40"), "wrong direction"),
        PAYROLL_INSURANCE_INVALID);
    assertServiceException(
        () -> propose(receipt(line, "REFUND", "21", "40"), "too large"), PAYROLL_INSURANCE_INVALID);
    Long id = propose(receipt(line, "REFUND", "20", "40"), "refund receipt");
    approveReceipt(id);
    assertEquals("RECONCILED", bills.selectById(bill.getId()).getStatus());
    assertEquals(new BigDecimal("500.00"), lines.selectById(line).getOriginalPersonal());
  }

  @Test
  void missingRowsAreUnknownActualsUntilReceiptIsRecorded() {
    Long e = setup();
    HrmPayrollInsuranceBillDO bill = preview(e, statement("480", "960").subList(0, 1));
    approveBill(bill);
    HrmPayrollInsuranceBillLineDO missing = ledger.lines(bill.getId(), 1L).get(1);
    assertEquals("MISSING", missing.getStatus());
    assertNull(missing.getActualPersonal());
    assertServiceException(
        () -> propose(receipt(missing.getId(), "PAY", "1500", "1500"), "assumed zero"),
        PAYROLL_INSURANCE_INVALID);
    Long id = propose(receipt(missing.getId(), "RECORD", "1500", "1500"), "missing actual receipt");
    approveReceipt(id);
    assertEquals("RECONCILED", bills.selectById(bill.getId()).getStatus());
    assertNull(lines.selectById(missing.getId()).getOriginalPersonal());
  }

  @Test
  void duplicateBlankWrongPeriodAndUnknownSourcesAreErrorsNotZero() {
    Long e = setup();
    List<cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollInsuranceBillRowVO>
        rows = new ArrayList<>(statement("", "960"));
    rows.get(1).setMonth("2026-02");
    rows.add(statement("480", "960").get(0));
    rows.add(statement("480", "960").get(1).setJobNumber("UNKNOWN"));
    HrmPayrollInsuranceBillDO bill = preview(e, rows);
    assertEquals("FAILED", bill.getStatus());
    assertTrue(bill.getErrors().contains("不能为空"));
    assertTrue(bill.getErrors().contains("重复"));
    assertServiceException(() -> ledger.submit(bill.getId(), 1L), PAYROLL_INSURANCE_INVALID);
    assertTrue(lines.selectList().isEmpty());
  }

  @Test
  void changedApprovedSourceBlocksSubmissionAndDoesNotRewritePreview() {
    Long e = setup();
    HrmPayrollInsuranceBillDO bill = preview(e, statement("480", "960"));
    Long old = service.policies(e, 1L).get(0).getId();
    HrmPayrollInsuranceReqVO changed = policyRequest(e).setReplacesId(old);
    changed.getProjects().get(0).setPersonalRate(new BigDecimal("9"));
    Long p = service.createPolicy(changed, 1L);
    service.approvePolicy(p, "正式更正", 2L);
    assertServiceException(() -> ledger.submit(bill.getId(), 1L), PAYROLL_INSURANCE_INVALID);
    assertEquals(bill.getSources(), bills.selectById(bill.getId()).getSources());
    HrmPayrollInsuranceBillDO revised = ledger.revalidate(bill.getId(), 1L);
    assertEquals(2, revised.getVersion());
    assertEquals(bill.getId(), revised.getReplacesId());
    assertEquals("SUPERSEDED", bills.selectById(bill.getId()).getStatus());
    assertNotEquals(bill.getSources(), revised.getSources());
    assertEquals(bill.getSources(), bills.selectById(bill.getId()).getSources());
    assertServiceException(() -> ledger.submit(bill.getId(), 1L), PAYROLL_INSURANCE_INVALID);
    ledger.submit(revised.getId(), 1L);
    assertTrue(lines.selectList().isEmpty());
  }

  @Test
  void sameReceiptCannotChangeAmountsAndOpenProposalBlocksConcurrentAdjustment() {
    Long e = setup();
    HrmPayrollInsuranceBillDO bill = preview(e, statement("0", "0"));
    approveBill(bill);
    Long line = ledger.lines(bill.getId(), 1L).get(0).getId();
    Long id = propose(receipt(line, "PAY", "200", "400"), "same receipt");
    assertServiceException(
        () -> propose(receipt(line, "PAY", "201", "400"), "same receipt"),
        PAYROLL_INSURANCE_INVALID);
    assertServiceException(
        () -> propose(receipt(line, "PAY", "200", "400"), "another receipt"),
        PAYROLL_INSURANCE_INVALID);
    ledger.transitionSettlement(id, "reject", "金额与凭证需修正", 2L);
    assertEquals("REJECTED", receipts.selectById(id).getStatus());
    assertEquals(new BigDecimal("0.00"), lines.selectById(line).getActualPersonal());
    Long corrected = propose(receipt(line, "PAY", "480", "960"), "corrected receipt");
    approveReceipt(corrected);
    assertEquals("RECONCILED", bills.selectById(bill.getId()).getStatus());
  }

  @Test
  void crossEntityStatementAndPaymentViewsAreDenied() {
    Long e = setup();
    HrmPayrollInsuranceBillDO bill = preview(e, statement("0", "0"));
    approveBill(bill);
    assertServiceException(() -> ledger.get(bill.getId(), 99L), PAYROLL_ENTITY_INVALID);
    assertServiceException(() -> ledger.lines(bill.getId(), 99L), PAYROLL_ENTITY_INVALID);
    assertServiceException(() -> ledger.settlements(bill.getId(), 99L), PAYROLL_ENTITY_INVALID);
    assertServiceException(
        () ->
            ledger.propose(
                receipt(ledger.lines(bill.getId(), 1L).get(0).getId(), "PAY", "1", "1"),
                "receipt".getBytes(),
                "receipt.pdf",
                99L),
        PAYROLL_ENTITY_INVALID);
  }

  @Test
  void exportsRequireExportAuthorityAndKeepMissingActualsUncomputable() {
    Long e = setup();
    HrmPayrollInsuranceBillDO b = preview(e, statement("480", "960").subList(0, 1));
    approveBill(b);
    HrmPayrollEntityReqVO.Grant grant =
        new HrmPayrollEntityReqVO.Grant()
            .setEntityId(e)
            .setUserId(5L)
            .setActions(Collections.singleton("EXPORT"))
            .setEffectiveFrom(LocalDate.now().minusDays(1))
            .setEffectiveTo(LocalDate.now().plusDays(1))
            .setReason("授权对账导出");
    entities.grant(grant, 1L);
    assertServiceException(() -> ledger.get(b.getId(), 5L), PAYROLL_ENTITY_INVALID);
    assertServiceException(() -> ledger.exportLines(b.getId(), 2L), PAYROLL_ENTITY_INVALID);
    List<HrmPayrollInsuranceBillLineDO> exported = ledger.exportLines(b.getId(), 5L);
    assertNull(exported.get(1).getActualPersonal());
    assertNull(exported.get(1).getPersonalDifference());
    assertEquals(new BigDecimal("0.00"), exported.get(0).getPersonalDifference());
  }
}
