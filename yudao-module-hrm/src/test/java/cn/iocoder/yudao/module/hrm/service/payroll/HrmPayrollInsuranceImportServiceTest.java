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
  HrmPayrollInsuranceImportService.class,
  HrmPayrollEntityService.class,
  org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration.class
})
class HrmPayrollInsuranceImportServiceTest extends BaseDbUnitTest {
  @Resource private HrmPayrollInsurancePolicyService service;
  @Resource private HrmPayrollInsuranceImportService imports;
  @Resource private HrmPayrollInsuranceImportMapper importMapper;
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

  private List<cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollInsuranceBaseRowVO>
      rows(String job) {
    List<cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollInsuranceBaseRowVO>
        result = new ArrayList<>();
    for (String code : Arrays.asList("PENSION", "FUND"))
      result.add(
          new cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollInsuranceBaseRowVO()
              .setJobNumber(job)
              .setPolicyCode("SZ-2026")
              .setCoverage("INSURED")
              .setIncomeYear("2025")
              .setFromMonth("2026-01")
              .setToMonth("2026-12")
              .setProjectCode(code)
              .setDeclaredBase("8000.00")
              .setEvidence("上年收入与参保凭证"));
    return result;
  }

  private HrmPayrollInsuranceImportDO preview(
      Long entity,
      String digest,
      List<cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollInsuranceBaseRowVO>
          rows) {
    return imports.preview(
        entity, "申报.xlsx", String.join("", Collections.nCopies(64, digest)), rows, 1L);
  }

  @Test
  void successfulPreviewIsReadOnlyAndCommitIsIdempotentAndNeedsApproval() {
    Long e = entity("A"), person = employee("INS-1");
    policy(e);
    HrmPayrollInsuranceImportDO batch = preview(e, "a", rows("INS-1"));
    assertEquals("VALIDATED", batch.getStatus());
    assertTrue(bases.selectList().isEmpty());
    List<Long> ids = imports.commit(batch.getId(), 1L);
    assertEquals(1, ids.size());
    assertEquals(ids, imports.commit(batch.getId(), 1L));
    assertEquals("HR_REVIEW", bases.selectById(ids.get(0)).getStatus());
    assertServiceException(
        () -> service.preview(e, Collections.singleton(person), YearMonth.of(2026, 1), 1L),
        PAYROLL_INSURANCE_INVALID);
    service.reviewBase(ids.get(0), "HR核实", 2L);
    service.approveBase(ids.get(0), "财务批准", 3L);
    assertEquals(
        new BigDecimal("640.00"),
        service
            .preview(e, Collections.singleton(person), YearMonth.of(2026, 1), 1L)
            .get(person)
            .getPersonalInsurance());
    assertEquals(batch.getId(), preview(e, "a", rows("INS-1")).getId());
  }

  @Test
  void duplicateProjectAndUnknownEmployeeHaveExactFileLinesAndCannotCommit() {
    Long e = entity("A");
    employee("INS-1");
    policy(e);
    List<cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollInsuranceBaseRowVO> r =
        rows("INS-1");
    r.add(rows("INS-1").get(0));
    r.add(rows("UNKNOWN").get(0));
    HrmPayrollInsuranceImportDO batch = preview(e, "b", r);
    List<HrmPayrollInsuranceImportService.RowError> errors =
        JsonUtils.parseArray(batch.getErrors(), HrmPayrollInsuranceImportService.RowError.class);
    assertEquals("FAILED", batch.getStatus());
    assertEquals(4, errors.get(0).getRow());
    assertEquals(5, errors.get(1).getRow());
    assertServiceException(() -> imports.commit(batch.getId(), 1L), PAYROLL_INSURANCE_INVALID);
    assertTrue(bases.selectList().isEmpty());
  }

  @Test
  void missingProjectAndBlankAmountsNeverBecomeZero() {
    Long e = entity("A");
    employee("INS-1");
    policy(e);
    List<cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollInsuranceBaseRowVO> r =
        rows("INS-1");
    r.get(1).setDeclaredBase("");
    HrmPayrollInsuranceImportDO batch = preview(e, "c", r);
    assertEquals("FAILED", batch.getStatus());
    assertTrue(batch.getErrors().contains("真实零值请明确填 0"));
    assertTrue(bases.selectList().isEmpty());
    assertEquals("FAILED", preview(e, "d", rows("INS-1").subList(0, 1)).getStatus());
  }

  @Test
  void inconsistentEmployeeMetadataAndInvalidScaleAreRejected() {
    Long e = entity("A");
    employee("INS-1");
    policy(e);
    List<cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollInsuranceBaseRowVO> r =
        rows("INS-1");
    r.get(1).setToMonth("2026-11");
    assertEquals("FAILED", preview(e, "e", r).getStatus());
    r = rows("INS-1");
    r.get(0).setDeclaredBase("12.345");
    assertEquals("FAILED", preview(e, "f", r).getStatus());
  }

  @Test
  void changedBaselineAfterPreviewBlocksAllProposals() {
    Long e = entity("A"), person = employee("INS-1"), p = policy(e);
    HrmPayrollInsuranceImportDO batch = preview(e, "a", rows("INS-1"));
    Long other = service.proposeBase(request(e, person, p), 1L);
    assertServiceException(() -> imports.commit(batch.getId(), 1L), PAYROLL_INSURANCE_INVALID);
    assertEquals(1, bases.selectList().size());
    assertEquals(other, bases.selectList().get(0).getId());
    assertEquals("VALIDATED", importMapper.selectById(batch.getId()).getStatus());
  }

  @Test
  void importOwnerAndLegalEntityAreCheckedOnServer() {
    Long e = entity("A");
    employee("INS-1");
    policy(e);
    HrmPayrollInsuranceImportDO batch = preview(e, "a", rows("INS-1"));
    assertServiceException(() -> imports.get(batch.getId(), 99L), PAYROLL_ENTITY_INVALID);
    assertServiceException(() -> imports.commit(batch.getId(), 2L), PAYROLL_ENTITY_INVALID);
    HrmPayrollEntityReqVO.Grant grant =
        new HrmPayrollEntityReqVO.Grant()
            .setEntityId(e)
            .setUserId(4L)
            .setActions(new HashSet<>(Arrays.asList("READ", "MANAGE")))
            .setEffectiveFrom(LocalDate.now().minusDays(1))
            .setEffectiveTo(LocalDate.now().plusDays(1))
            .setReason("维护职责");
    entities.grant(grant, 1L);
    assertServiceException(() -> imports.commit(batch.getId(), 4L), PAYROLL_INSURANCE_INVALID);
    assertTrue(bases.selectList().isEmpty());
  }

  @Test
  void explicitExemptionKeepsEvidenceAndRequiresIndependentApproval() {
    Long e = entity("A"), person = employee("INS-1");
    cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollInsuranceBaseRowVO row =
        new cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollInsuranceBaseRowVO()
            .setJobNumber("INS-1")
            .setCoverage("EXEMPT")
            .setFromMonth("2026-01")
            .setToMonth("2026-12")
            .setEvidence("主管部门豁免凭证");
    HrmPayrollInsuranceImportDO batch = preview(e, "a", Collections.singletonList(row));
    assertEquals("VALIDATED", batch.getStatus());
    Long id = imports.commit(batch.getId(), 1L).get(0);
    service.reviewBase(id, "核验", 2L);
    service.approveBase(id, "核验", 3L);
    assertEquals(
        "EXEMPT",
        service
            .preview(e, Collections.singleton(person), YearMonth.of(2026, 1), 1L)
            .get(person)
            .getCoverage());
    assertEquals("主管部门豁免凭证", bases.selectById(id).getEvidence());
  }
}
