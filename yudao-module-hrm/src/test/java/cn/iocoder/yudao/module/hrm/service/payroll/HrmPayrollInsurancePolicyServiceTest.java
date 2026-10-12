package cn.iocoder.yudao.module.hrm.service.payroll;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
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
  HrmPayrollEntityService.class,
  org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration.class
})
class HrmPayrollInsurancePolicyServiceTest extends BaseDbUnitTest {
  @Resource private HrmPayrollInsurancePolicyService service;
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

  @Test
  void policyAndBaseRequireIndependentActors() {
    Long e = entity("A"), person = employee("INS-1");
    Long p = service.createPolicy(policyRequest(e), 1L);
    assertServiceException(() -> service.approvePolicy(p, "通过", 1L), PAYROLL_SELF_APPROVAL);
    service.approvePolicy(p, "通过", 2L);
    Long b = service.proposeBase(request(e, person, p), 1L);
    assertServiceException(() -> service.reviewBase(b, "复核", 1L), PAYROLL_SELF_APPROVAL);
    assertServiceException(
        () -> service.preview(e, Collections.singleton(person), YearMonth.of(2026, 1), 1L),
        PAYROLL_INSURANCE_INVALID);
    service.reviewBase(b, "复核", 2L);
    assertServiceException(() -> service.approveBase(b, "批准", 2L), PAYROLL_SELF_APPROVAL);
    service.approveBase(b, "批准", 3L);
    assertEquals("APPROVED", bases.selectById(b).getStatus());
  }

  @Test
  void statutoryBoundsApplyAndPersonalCorporateAmountsRemainSeparate() {
    Long e = entity("A"), person = employee("INS-1"), p = policy(e);
    approved(request(e, person, p));
    HrmPayrollInsurancePolicyService.Quote q =
        service.preview(e, Collections.singleton(person), YearMonth.of(2026, 1), 1L).get(person);
    assertEquals(new BigDecimal("480.00"), q.getPersonalInsurance());
    assertEquals(new BigDecimal("960.00"), q.getCorporateInsurance());
    assertEquals(new BigDecimal("1500.00"), q.getPersonalFund());
    assertEquals(new BigDecimal("1500.00"), q.getCorporateFund());
    assertEquals(new BigDecimal("3000"), q.getProjects().get(0).getDeclaredBase());
    assertEquals(new BigDecimal("6000"), q.getProjects().get(0).getAppliedBase());
    assertEquals(new BigDecimal("30000"), q.getProjects().get(1).getAppliedBase());
    assertEquals(
        q.getPersonalInsurance(),
        service
            .payrollRecords(Collections.singletonMap(person, q))
            .get(person)
            .getPersonalInsuranceAmount());
  }

  @Test
  void singlePayerDoesNotDeductAgainInAnotherLegalEntity() {
    Long a = entity("A"), b = entity("B"), person = employee("INS-1");
    approved(request(a, person, policy(a)));
    HrmPayrollInsurancePolicyService.Quote q =
        service.preview(b, Collections.singleton(person), YearMonth.of(2026, 1), 1L).get(person);
    assertEquals("EXTERNAL", q.getCoverage());
    assertEquals(a, q.getPayerEntityId());
    assertEquals(BigDecimal.ZERO, q.getPersonalInsurance());
    assertEquals(BigDecimal.ZERO, q.getCorporateFund());
    assertTrue(q.getProjects().isEmpty());
    assertNull(q.getPolicyId());
    assertTrue(service.previewForEntity(b, YearMonth.of(2026, 1), 1L).isEmpty());
  }

  @Test
  void exemptionsAreExplicitApprovedRecords() {
    Long e = entity("A"), person = employee("INS-1");
    HrmPayrollInsuranceReqVO.Base r =
        new HrmPayrollInsuranceReqVO.Base()
            .setEntityId(e)
            .setEmployeeId(person)
            .setCoverage("EXEMPT")
            .setEffectiveFrom(day(1, 1))
            .setEffectiveTo(day(12, 31))
            .setEvidence("参保不适用的核定依据");
    approved(r);
    HrmPayrollInsurancePolicyService.Quote q =
        service.preview(e, Collections.singleton(person), YearMonth.of(2026, 1), 1L).get(person);
    assertEquals("EXEMPT", q.getCoverage());
    assertEquals(BigDecimal.ZERO, q.getPersonalInsurance());
    assertNotNull(q.getBaseId());
  }

  @Test
  void policyOverlapAndInvalidRatesAreRejected() {
    Long e = entity("A");
    Long first = policy(e), second = service.createPolicy(policyRequest(e), 1L);
    assertServiceException(
        () -> service.approvePolicy(second, "核实", 2L), PAYROLL_INSURANCE_INVALID);
    assertEquals("APPROVED", policies.selectById(first).getStatus());
    HrmPayrollInsuranceReqVO invalid = policyRequest(e);
    invalid.getProjects().get(0).setPersonalRate(new BigDecimal("101"));
    assertServiceException(() -> service.createPolicy(invalid, 1L), PAYROLL_INSURANCE_INVALID);
  }

  @Test
  void unknownOrMissingDeclaredProjectCannotSilentlyBecomeZero() {
    Long e = entity("A"), person = employee("INS-1"), p = policy(e);
    HrmPayrollInsuranceReqVO.Base r = request(e, person, p);
    r.getDeclaredBases().remove("FUND");
    assertServiceException(() -> service.proposeBase(r, 1L), PAYROLL_INSURANCE_INVALID);
    HrmPayrollInsuranceReqVO.Base bad = request(e, person, p);
    bad.getDeclaredBases().put("UNKNOWN", BigDecimal.ONE);
    assertServiceException(() -> service.proposeBase(bad, 1L), PAYROLL_INSURANCE_INVALID);
  }

  @Test
  void registeredPayerTransferPreservesTheOriginalAnnualRecord() {
    Long a = entity("A"),
        b = entity("B"),
        person = employee("INS-1"),
        pa = policy(a),
        pb = policy(b);
    Long old = approved(request(a, person, pa));
    HrmPayrollInsuranceReqVO.Base incoming = request(b, person, pb).setEffectiveFrom(day(7, 1));
    Long blocked = service.proposeBase(incoming, 1L);
    service.reviewBase(blocked, "复核", 2L);
    assertServiceException(() -> service.approveBase(blocked, "确认", 3L), PAYROLL_INSURANCE_INVALID);
    service.rejectBase(blocked, "先核定原缴费主体截止月份", 3L);
    approved(request(a, person, pa).setEffectiveTo(day(6, 30)).setReplacesId(old));
    approved(incoming);
    assertEquals("SUPERSEDED", bases.selectById(old).getStatus());
    assertEquals(day(12, 31), bases.selectById(old).getEffectiveTo());
    assertEquals(
        "EXTERNAL",
        service
            .preview(a, Collections.singleton(person), YearMonth.of(2026, 7), 1L)
            .get(person)
            .getCoverage());
    assertEquals(
        "INSURED",
        service
            .preview(b, Collections.singleton(person), YearMonth.of(2026, 7), 1L)
            .get(person)
            .getCoverage());
  }

  @Test
  void changedPolicyInvalidatesUnfrozenInputsWithoutChangingTheirSnapshot() {
    Long e = entity("A"), person = employee("INS-1"), p = policy(e);
    approved(request(e, person, p));
    Map<Long, HrmPayrollInsurancePolicyService.Quote> before =
        service.preview(e, Collections.singleton(person), YearMonth.of(2026, 1), 1L);
    String json =
        JsonUtils.toJsonString(Collections.singletonMap("insurancePolicySources", before));
    service.validateInputSnapshot(json);
    HrmPayrollInsuranceReqVO replacement = policyRequest(e).setReplacesId(p);
    replacement.getProjects().get(0).setPersonalRate(new BigDecimal("9"));
    Long next = service.createPolicy(replacement, 1L);
    service.approvePolicy(next, "批准政策更正", 2L);
    assertServiceException(() -> service.validateInputSnapshot(json), PAYROLL_INSURANCE_INVALID);
    assertEquals(new BigDecimal("480.00"), before.get(person).getPersonalInsurance());
    assertEquals("SUPERSEDED", policies.selectById(p).getStatus());
  }

  @Test
  void dataAndApprovalsDoNotCrossAuthorizedEntities() {
    Long a = entity("A"), b = entity("B");
    HrmPayrollEntityReqVO.Grant r =
        new HrmPayrollEntityReqVO.Grant()
            .setEntityId(a)
            .setUserId(4L)
            .setActions(Collections.singleton("READ"))
            .setEffectiveFrom(LocalDate.now(ZoneId.of("Asia/Shanghai")))
            .setEffectiveTo(LocalDate.now(ZoneId.of("Asia/Shanghai")).plusDays(1))
            .setReason("查询职责");
    entities.grant(r, 1L);
    assertServiceException(() -> service.policies(b, 4L), PAYROLL_ENTITY_INVALID);
    assertServiceException(() -> service.bases(b, 4L), PAYROLL_ENTITY_INVALID);
    assertServiceException(
        () -> service.createPolicy(policyRequest(a), 4L), PAYROLL_ENTITY_INVALID);
  }
}
