package cn.iocoder.yudao.module.hrm.service.payroll;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollEntityReqVO;
import cn.iocoder.yudao.module.hrm.controller.admin.salary.vo.monthrecord.HrmSalaryMonthRecordPageReqVO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.salary.monthrecord.HrmSalaryMonthRecordDO;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.HrmPayrollBatchEventMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.salary.monthrecord.HrmSalaryMonthRecordMapper;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import java.time.*;
import java.util.*;
import javax.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

@Import({
  HrmPayrollScopeService.class,
  HrmPayrollEntityService.class,
  HrmPayrollBatchService.class,
  HrmPayrollPaymentService.class,
  org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration.class
})
class HrmPayrollScopeServiceTest extends BaseDbUnitTest {
  @org.springframework.boot.test.mock.mockito.MockBean private HrmPayrollTaxWithholdingService taxWithholding;
 @org.springframework.boot.test.mock.mockito.MockBean private HrmPayrollSourceClaimService sourceClaims;

    @org.springframework.boot.test.mock.mockito.MockBean private cn.iocoder.yudao.module.hrm.service.payroll.HrmPayrollInputService payrollInputs;

    @org.springframework.boot.test.mock.mockito.MockBean private cn.iocoder.yudao.module.hrm.service.payroll.HrmPayrollInsurancePolicyService payrollInsurancePolicies;
  @Resource private HrmPayrollScopeService scope;
  @Resource private HrmPayrollEntityService entities;
  @Resource private HrmPayrollBatchService approvals;
  @Resource private HrmPayrollPaymentService payments;
  @Resource private HrmSalaryMonthRecordMapper batches;
  @Resource private HrmPayrollBatchEventMapper events;
  @MockBean private AdminUserApi users;
  @MockBean private PermissionApi permissions;
  @MockBean private DeptApi departments;
  @MockBean private HrmPayrollWorkService work;
  @MockBean private HrmPayrollAttendanceService attendance;

  @MockBean
  private cn.iocoder.yudao.module.hrm.service.employee.employment.HrmEmployeeSalaryCardService
      cards;

  @MockBean private cn.iocoder.yudao.module.hrm.service.employee.info.HrmEmployeeService employees;

  private LocalDate today() {
    return LocalDate.now(ZoneId.of("Asia/Shanghai"));
  }

  private void login(Long id) {
    SecurityFrameworkUtils.setLoginUser(
        new LoginUser().setId(id).setUserType(2),
        new org.springframework.mock.web.MockHttpServletRequest());
  }

  @BeforeEach
  void setup() {
    login(1L);
  }

  @AfterEach
  void clear() {
    org.springframework.security.core.context.SecurityContextHolder.clearContext();
  }

  private void grant(Long entity, Long actor, String... actions) {
    HrmPayrollEntityReqVO.Grant r = new HrmPayrollEntityReqVO.Grant();
    r.setEntityId(entity);
    r.setUserId(actor);
    r.setActions(new HashSet<>(Arrays.asList(actions)));
    r.setEffectiveFrom(today().minusDays(1));
    r.setEffectiveTo(today().plusYears(1));
    r.setReason("核算职责授权");
    entities.grant(r, 1L);
  }

  private Long entity(String code) {
    HrmPayrollEntityReqVO r = new HrmPayrollEntityReqVO();
    r.setCode(code);
    r.setName(code + "公司");
    r.setTaxRegistrationNumber("914400000000000001");
    r.setEvidence("登记资料");
    Long id = entities.create(r, 1L);
    grant(id, 2L, "READ", "APPROVE");
    entities.approve(id, "登记核实", 2L);
    return id;
  }

  private HrmSalaryMonthRecordDO batch(Long entity, int status) {
    HrmSalaryMonthRecordDO b =
        new HrmSalaryMonthRecordDO()
            .setEntityId(entity)
            .setBatchKey("REGULAR")
            .setYear(2026)
            .setMonth(10)
            .setStartTime(LocalDate.of(2026, 10, 1).atStartOfDay())
            .setEndTime(LocalDate.of(2026, 10, 31).atStartOfDay())
            .setStatus(status);
    batches.insert(b);
    return b;
  }

  @Test
  void paginationAndSingleRecordAccessRespectSubject() {
    Long a = entity("A"), b = entity("B");
    HrmSalaryMonthRecordDO ba = batch(a, 11), bb = batch(b, 11);
    grant(a, 3L, "READ");
    login(3L);
    assertEquals(1, scope.page(new HrmSalaryMonthRecordPageReqVO(), false).getTotal());
    assertEquals(ba.getId(), scope.list(null).get(0).getId());
    assertEquals(
        0, scope.page(new HrmSalaryMonthRecordPageReqVO().setEntityId(b), false).getTotal());
    assertServiceException(() -> scope.require(bb.getId(), "READ"), PAYROLL_ENTITY_INVALID);
    assertServiceException(() -> approvals.runs(bb.getId()), PAYROLL_ENTITY_INVALID);
    assertServiceException(() -> approvals.events(bb.getId()), PAYROLL_ENTITY_INVALID);
    assertServiceException(() -> payments.list(bb.getId()), PAYROLL_ENTITY_INVALID);
  }

  @Test
  void readPermissionDoesNotAllowCalculationApprovalOrBankExport() {
    Long a = entity("A");
    HrmSalaryMonthRecordDO b = batch(a, 16);
    grant(a, 3L, "READ");
    login(3L);
    assertServiceException(() -> scope.check(b, "CALCULATE"), PAYROLL_ENTITY_INVALID);
    assertServiceException(
        () -> approvals.transition(b.getId(), 1L, "approve", "确认", 3L), PAYROLL_ENTITY_INVALID);
    assertServiceException(() -> payments.export(b.getId()), PAYROLL_ENTITY_INVALID);
    grant(a, 3L, "EXPORT");
    assertTrue(payments.export(b.getId()).isEmpty());
  }

  @Test
  void reconciliationCannotCrossSubjectsEvenWithOtherSubjectApproval() {
    Long a = entity("A"), b = entity("B");
    HrmSalaryMonthRecordDO bb = batch(b, 16);
    grant(a, 3L, "READ", "APPROVE");
    login(3L);
    assertServiceException(
        () -> payments.reconcileFile(bb.getId(), "回盘.xlsx", "digest", Collections.emptyList(), 3L),
        PAYROLL_ENTITY_INVALID);
    assertServiceException(() -> payments.prepare(bb.getId(), 1L, 3L), PAYROLL_ENTITY_INVALID);
  }

  @Test
  void historicalUnboundBatchesRequireExplicitAttributionAndRecalculation() {
    Long a = entity("A");
    HrmSalaryMonthRecordDO legacy = batch(null, 11);
    when(permissions.hasAnyRoles(7L, "super_admin")).thenReturn(true);
    login(7L);
    assertNotNull(scope.require(legacy.getId(), "READ"));
    assertServiceException(
        () -> scope.require(legacy.getId(), "CALCULATE"), PAYROLL_ENTITY_INVALID);
    scope.bindDraft(legacy.getId(), a, "根据原工资资料确认主体");
    assertEquals(a, batches.selectById(legacy.getId()).getEntityId());
    assertEquals(5, batches.selectById(legacy.getId()).getStatus());
    assertEquals("bind_subject", events.selectList().get(0).getAction());
    assertServiceException(() -> scope.validateInputSnapshot("{}"), PAYROLL_ENTITY_INVALID);
  }

  @Test
  void paidHistoricalBatchesCannotBeReattributed() {
    Long a = entity("A");
    HrmSalaryMonthRecordDO legacy = batch(null, 17);
    when(permissions.hasAnyRoles(7L, "super_admin")).thenReturn(true);
    login(7L);
    assertServiceException(
        () -> scope.bindDraft(legacy.getId(), a, "归属调整"), PAYROLL_ENTITY_INVALID);
    assertNull(batches.selectById(legacy.getId()).getEntityId());
    assertTrue(events.selectList().isEmpty());
  }

  @Test
  void immutableScopeSnapshotCannotBeUsedForDeletedBatch() {
    Long a = entity("A");
    HrmSalaryMonthRecordDO b = batch(a, 14);
    String snapshot =
        JsonUtils.toJsonString(Collections.singletonMap("entityScope", scope.snapshot(b)));
    scope.validateInputSnapshot(snapshot);
    batches.deleteById(b.getId());
    assertServiceException(() -> scope.validateInputSnapshot(snapshot), PAYROLL_ENTITY_INVALID);
  }
}
