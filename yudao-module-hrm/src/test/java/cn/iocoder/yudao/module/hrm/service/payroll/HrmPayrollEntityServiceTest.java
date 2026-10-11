package cn.iocoder.yudao.module.hrm.service.payroll;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollEntityReqVO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.employee.info.HrmEmployeeMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.*;
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
  HrmPayrollEntityService.class,
  org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration.class
})
class HrmPayrollEntityServiceTest extends BaseDbUnitTest {
  @Resource private HrmPayrollEntityService service;
  @Resource private HrmPayrollLegalEntityMapper entities;
  @Resource private HrmPayrollEntityMembershipMapper memberships;
  @Resource private HrmPayrollEntityGrantMapper grants;
  @Resource private HrmEmployeeMapper employees;
  @MockBean private AdminUserApi users;
  @MockBean private PermissionApi permissions;
  @MockBean private DeptApi departments;

  private LocalDate today() {
    return LocalDate.now(ZoneId.of("Asia/Shanghai"));
  }

  private HrmPayrollEntityReqVO request(String code) {
    HrmPayrollEntityReqVO r = new HrmPayrollEntityReqVO();
    r.setCode(code);
    r.setName("测试法人 " + code);
    r.setTaxRegistrationNumber("914400000000000001");
    r.setEvidence("营业执照及纳税登记核验资料");
    return r;
  }

  private Long grant(Long entity, Long user, Set<String> actions, LocalDate from, LocalDate to) {
    HrmPayrollEntityReqVO.Grant r = new HrmPayrollEntityReqVO.Grant();
    r.setEntityId(entity);
    r.setUserId(user);
    r.setActions(actions);
    r.setEffectiveFrom(from);
    r.setEffectiveTo(to);
    r.setReason("按职责和期限授权");
    return service.grant(r, 1L);
  }

  private Long approved(String code) {
    Long id = service.create(request(code), 1L);
    grant(
        id,
        2L,
        new HashSet<>(Arrays.asList("READ", "APPROVE")),
        today().minusDays(1),
        today().plusYears(1));
    service.approve(id, "核验主体信息一致", 2L);
    return id;
  }

  private HrmPayrollEntityReqVO.Membership member(
      Long entity, Long employee, LocalDate from, LocalDate to) {
    HrmPayrollEntityReqVO.Membership r = new HrmPayrollEntityReqVO.Membership();
    r.setEntityId(entity);
    r.setEmployeeId(employee);
    r.setDeptId(7L);
    r.setEmployeeCategory("生产人员");
    r.setWorkCity("深圳");
    r.setEffectiveFrom(from);
    r.setEffectiveTo(to);
    r.setEvidence("劳动合同与人员归属确认");
    return r;
  }

  @Test
  void onlyIndependentAuthorizedActorApprovesEntity() {
    Long id = service.create(request("A"), 1L);
    assertServiceException(() -> service.approve(id, "确认", 1L), PAYROLL_SELF_APPROVAL);
    assertServiceException(() -> service.approve(id, "确认", 2L), PAYROLL_ENTITY_INVALID);
    grant(id, 2L, Collections.singleton("APPROVE"), today().minusDays(1), today().plusDays(1));
    service.approve(id, "核实登记", 2L);
    assertEquals("APPROVED", entities.selectById(id).getStatus());
    assertEquals(3, service.history(id, 1L).size());
  }

  @Test
  void queryAndActionsDoNotLeakAnotherEntity() {
    Long a = approved("A"), b = approved("B");
    grant(a, 3L, Collections.singleton("READ"), today().minusDays(1), today().plusDays(1));
    assertEquals(1, service.list(3L).size());
    assertEquals(a, service.list(3L).get(0).getId());
    assertFalse(service.allowed(b, "READ", 3L));
    assertFalse(service.allowed(a, "CALCULATE", 3L));
    assertServiceException(() -> service.history(b, 3L), PAYROLL_ENTITY_INVALID);
    assertServiceException(() -> service.grants(a, 3L), PAYROLL_ENTITY_INVALID);
  }

  @Test
  void expiredAndRevokedAuthorizationStopsAccess() {
    Long a = approved("A");
    grant(a, 3L, Collections.singleton("READ"), today().minusDays(2), today().minusDays(1));
    assertFalse(service.allowed(a, "READ", 3L));
    Long active = grant(a, 3L, Collections.singleton("READ"), today(), today().plusDays(1));
    assertTrue(service.allowed(a, "READ", 3L));
    service.revoke(active, "职责调整收回", 1L);
    assertFalse(service.allowed(a, "READ", 3L));
    assertTrue(service.history(a, 1L).stream().anyMatch(e -> "REVOKE".equals(e.getAction())));
  }

  @Test
  void creatorCannotApproveAfterRevisingAndApprovedEntityCannotBeEdited() {
    Long id = service.create(request("A"), 1L);
    HrmPayrollEntityReqVO corrected = request("A");
    corrected.setName("修订名称");
    service.revise(id, corrected, 1L);
    assertEquals(1L, entities.selectById(id).getSubmittedBy());
    assertServiceException(() -> service.approve(id, "确认", 1L), PAYROLL_SELF_APPROVAL);
    grant(id, 2L, Collections.singleton("APPROVE"), today(), today().plusDays(1));
    service.approve(id, "确认", 2L);
    assertServiceException(() -> service.revise(id, request("A"), 1L), PAYROLL_ENTITY_INVALID);
  }

  @Test
  void unsupportedPrivilegesDoNotCreateGrant() {
    Long id = approved("A");
    assertServiceException(
        () -> grant(id, 3L, Collections.singleton("ROOT"), today(), today().plusDays(1)),
        PAYROLL_ENTITY_INVALID);
    assertFalse(service.allowed(id, "ROOT", 1L));
  }

  @Test
  void employeeTransfersRequireApprovedNonOverlappingHistory() {
    Long a = approved("A"), b = approved("B");
    HrmEmployeeDO employee = new HrmEmployeeDO().setName("转移员工").setJobNumber("ENTITY-1");
    employees.insert(employee);
    Long original =
        service.assign(
            member(a, employee.getId(), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)), 1L);
    assertServiceException(
        () -> service.approveMembership(original, "确认", 1L), PAYROLL_SELF_APPROVAL);
    service.approveMembership(original, "归属已核实", 2L);
    Long incoming =
        service.assign(
            member(b, employee.getId(), LocalDate.of(2026, 7, 1), LocalDate.of(2026, 12, 31)), 1L);
    assertServiceException(
        () -> service.approveMembership(incoming, "确认", 2L), PAYROLL_ENTITY_INVALID);
    HrmPayrollEntityReqVO.Membership ending =
        member(a, employee.getId(), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30));
    ending.setReplacesId(original);
    Long replacement = service.assign(ending, 1L);
    service.approveMembership(replacement, "批准六月底终止原主体归属", 2L);
    service.approveMembership(incoming, "新主体七月生效", 2L);
    assertEquals("SUPERSEDED", memberships.selectById(original).getStatus());
    assertEquals(LocalDate.of(2026, 12, 31), memberships.selectById(original).getEffectiveTo());
    assertEquals(
        1,
        service
            .effectiveMemberships(a, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30))
            .size());
    assertTrue(
        service
            .effectiveMemberships(a, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31))
            .isEmpty());
    assertEquals(
        1,
        service
            .effectiveMemberships(b, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31))
            .size());
  }

  @Test
  void membershipRevisionCannotReplaceAnotherEmployee() {
    Long a = approved("A");
    HrmEmployeeDO e1 = new HrmEmployeeDO().setName("员工甲").setJobNumber("E1"),
        e2 = new HrmEmployeeDO().setName("员工乙").setJobNumber("E2");
    employees.insert(e1);
    employees.insert(e2);
    Long first = service.assign(member(a, e1.getId(), today(), today().plusMonths(1)), 1L);
    service.approveMembership(first, "确认", 2L);
    HrmPayrollEntityReqVO.Membership wrong = member(a, e2.getId(), today(), today().plusMonths(1));
    wrong.setReplacesId(first);
    Long invalid = service.assign(wrong, 1L);
    assertServiceException(
        () -> service.approveMembership(invalid, "确认", 2L), PAYROLL_ENTITY_INVALID);
    assertEquals("APPROVED", memberships.selectById(first).getStatus());
  }

  @Test
  void codesAreCanonicalAndOptionsRequireManagement() {
    Long a = service.create(request("abc"), 1L);
    assertEquals("ABC", entities.selectById(a).getCode());
    assertServiceException(() -> service.create(request("ABC"), 1L), PAYROLL_ENTITY_INVALID);
    grant(a, 3L, Collections.singleton("READ"), today(), today().plusDays(1));
    assertEquals(Collections.singleton("READ"), service.list(3L).get(0).getAllowedActions());
    assertServiceException(() -> service.options(a, 3L), PAYROLL_ENTITY_INVALID);
  }
}
