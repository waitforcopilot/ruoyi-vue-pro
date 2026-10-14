package cn.iocoder.yudao.module.hrm.service.payroll;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollTaxDeductionReqVO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.employee.info.HrmEmployeeMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import javax.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

@Import({
  HrmPayrollTaxDeductionService.class,
  org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration.class
})
class HrmPayrollTaxDeductionServiceTest extends BaseDbUnitTest {
  @Resource private HrmPayrollTaxDeductionService service;
  @Resource private HrmPayrollTaxDeductionMapper records;
  @Resource private HrmEmployeeMapper employees;
  @MockBean private HrmPayrollEntityService entities;
  @MockBean private HrmPayrollTaxProfileService profiles;
  private Long employeeId;

  @BeforeEach
  void setup() {
    HrmEmployeeDO e =
        new HrmEmployeeDO()
            .setName("扣除员工")
            .setJobNumber("DEDUCTION-1")
            .setUserId(99L)
            .setEntryTime(LocalDate.of(2026, 1, 1).atStartOfDay());
    employees.insert(e);
    employeeId = e.getId();
    when(profiles.requireApproved(10L, employeeId, 2026))
        .thenReturn(
            new HrmPayrollTaxProfileDO()
                .setTaxpayerKey("test-person")
                .setTaxRegistrationNumber("agent"));
  }

  private HrmPayrollTaxDeductionReqVO request(String mode) {
    Map<String, BigDecimal> values = new TreeMap<>();
    HrmPayrollTaxDeductionService.FIELDS.forEach(code -> values.put(code, BigDecimal.ZERO));
    return new HrmPayrollTaxDeductionReqVO()
        .setEntityId(10L)
        .setEmployeeId(employeeId)
        .setYear(2026)
        .setMonth(10)
        .setSourceDate(LocalDate.of(2026, 10, 1))
        .setMode(mode)
        .setValues(values)
        .setRequestKey(UUID.randomUUID().toString())
        .setOfficialReference("经授权的扣缴端2026年10月累计扣除资料")
        .setEvidence("测试核实员工身份、所属单位、报告月份和累计金额");
  }

  private Long approved(HrmPayrollTaxDeductionReqVO request) {
    Long id = service.create(request, 1L);
    service.review(id, true, "独立核对授权申报资料", 2L);
    return id;
  }

  @Test
  void requiresCompleteExplicitAmountsSourceMonthAndEvidence() {
    HrmPayrollTaxDeductionReqVO req = request("DECLARE");
    req.getValues().remove("260106");
    assertServiceException(() -> service.create(req, 1L), PAYROLL_TAX_INVALID);
    HrmPayrollTaxDeductionReqVO dateRequest =
        request("DECLARE").setSourceDate(LocalDate.of(2026, 9, 30));
    final HrmPayrollTaxDeductionReqVO wrongDate = dateRequest;
    assertServiceException(() -> service.create(wrongDate, 1L), PAYROLL_TAX_INVALID);
    HrmPayrollTaxDeductionReqVO wrongEvidence = request("DECLARE").setOfficialReference("");
    assertServiceException(() -> service.create(wrongEvidence, 1L), PAYROLL_TAX_INVALID);
    HrmPayrollTaxDeductionReqVO zero = request("ZERO");
    zero.getValues().put("260101", new BigDecimal("1"));
    assertServiceException(() -> service.create(zero, 1L), PAYROLL_TAX_INVALID);
  }

  @Test
  void independentReviewAndSubjectReviewAvoidancePreservePendingSource() {
    Long id = service.create(request("ZERO"), 1L);
    assertNull(service.source(10L, employeeId, 2026, 10));
    assertServiceException(() -> service.review(id, true, "本人提交", 1L), PAYROLL_SELF_APPROVAL);
    assertServiceException(() -> service.review(id, false, "员工本人复核", 99L), PAYROLL_SELF_APPROVAL);
    service.review(id, true, "独立核实零发生额", 2L);
    assertEquals(id, service.source(10L, employeeId, 2026, 10).getId());
    assertEquals(2, service.history(id, 2L).size());
  }

  @Test
  void versionedCorrectionsPreserveOriginalAmountsAndRejectStaleSuccessor() {
    HrmPayrollTaxDeductionReqVO first = request("DECLARE");
    first.getValues().put("260106", new BigDecimal("20000"));
    Long prior = approved(first);
    HrmPayrollTaxDeductionReqVO one = request("DECLARE").setPriorDeclarationId(prior),
        two = request("DECLARE").setPriorDeclarationId(prior);
    one.getValues().put("260106", new BigDecimal("18000"));
    Long a = service.create(one, 1L), b = service.create(two, 1L);
    service.review(a, true, "依据扣缴端更正资料", 2L);
    assertServiceException(() -> service.review(b, true, "旧基线", 2L), PAYROLL_VERSION_CONFLICT);
    assertEquals("SUPERSEDED", records.selectById(prior).getStatus());
    assertEquals(new BigDecimal("20000"), service.values(records.selectById(prior)).get("260106"));
    assertEquals(
        new BigDecimal("18000"),
        service.values(service.source(10L, employeeId, 2026, 10)).get("260106"));
  }

  @Test
  void retriesCannotReplaceDeclarationBodyAndChangedIdentityBlocksUse() {
    HrmPayrollTaxDeductionReqVO req = request("ZERO");
    Long id = approved(req);
    assertEquals(id, service.create(req, 1L));
    req.getValues().put("260101", BigDecimal.ONE);
    req.setMode("DECLARE");
    assertServiceException(() -> service.create(req, 1L), PAYROLL_TAX_INVALID);
    when(profiles.requireApproved(10L, employeeId, 2026))
        .thenReturn(
            new HrmPayrollTaxProfileDO()
                .setTaxpayerKey("other-person")
                .setTaxRegistrationNumber("agent"));
    assertServiceException(() -> service.source(10L, employeeId, 2026, 10), PAYROLL_TAX_INVALID);
  }

  @Test
  void pre2022InfantCareAndAnnualMedicalCannotEnterPayrollDeclarations() {
    HrmPayrollTaxDeductionReqVO req =
        request("DECLARE").setYear(2021).setSourceDate(LocalDate.of(2021, 10, 1));
    req.getValues().put("260106", BigDecimal.ONE);
    assertServiceException(() -> service.create(req, 1L), PAYROLL_TAX_INVALID);
    HrmPayrollTaxDeductionReqVO medical = request("DECLARE");
    medical.getValues().put("MEDICAL", BigDecimal.ONE);
    assertServiceException(() -> service.create(medical, 1L), PAYROLL_TAX_INVALID);
  }

  @Test
  void taxpayerApprovalAndEntityScopeAreRequired() {
    doThrow(new IllegalStateException("denied")).when(entities).check(10L, "MANAGE", 8L);
    assertThrows(IllegalStateException.class, () -> service.create(request("ZERO"), 8L));
    doThrow(new IllegalStateException("denied")).when(entities).check(10L, "READ", 8L);
    assertThrows(IllegalStateException.class, () -> service.list(10L, 2026, 10, employeeId, 8L));
  }
}
