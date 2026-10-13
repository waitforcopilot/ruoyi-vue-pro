package cn.iocoder.yudao.module.hrm.service.payroll;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollWorkVarianceReqVO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.employee.info.HrmEmployeeMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.*;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import java.time.*;
import java.util.*;
import javax.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

@Import({
  HrmPayrollWorkVarianceService.class,
  org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration.class
})
class HrmPayrollWorkVarianceServiceTest extends BaseDbUnitTest {
  @Resource private HrmPayrollWorkVarianceService service;
  @Resource private HrmPayrollWorkVarianceMapper cases;
  @Resource private HrmEmployeeMapper employees;
  @MockBean private HrmPayrollWorkService work;
  @MockBean private HrmPayrollEntityService entities;
  @MockBean private AdminUserApi users;
  @MockBean private cn.iocoder.yudao.module.system.api.notify.NotifyMessageSendApi notifications;
  private Long employeeId;
  private final LocalDate day = LocalDate.of(2026, 9, 10);
  private Map<String, Object> source;

  @BeforeEach
  void setup() {
    HrmEmployeeDO employee =
        new HrmEmployeeDO().setName("差异员工").setJobNumber("VAR-1").setUserId(99L);
    employees.insert(employee);
    employeeId = employee.getId();
    source = new LinkedHashMap<>();
    source.put("plannedMinutes", 480);
    source.put("actualMinutes", 540);
    source.put("differenceMinutes", 60);
    source.put("ready", true);
    when(work.reconcileDay(eq(10L), eq(employeeId), eq(day), anyLong()))
        .thenAnswer(call -> new LinkedHashMap<>(source));
  }

  private HrmPayrollWorkVarianceReqVO request() {
    return new HrmPayrollWorkVarianceReqVO()
        .setEntityId(10L)
        .setEmployeeId(employeeId)
        .setWorkDate(day)
        .setRequestKey("case-1")
        .setReason("计划与实际存在差异")
        .setOwnerId(3L)
        .setReviewerId(2L)
        .setDueTime(LocalDate.now(ZoneId.of("Asia/Shanghai")).plusDays(2).atTime(9, 0));
  }

  @Test
  void assignedOwnerAndIndependentReviewerCloseWithFullHistory() {
    Long id = service.create(request(), 1L);
    assertServiceException(() -> service.submit(id, "他人提交", 4L), PAYROLL_WORK_INVALID);
    service.submit(id, "核对延长工作及OA认可依据", 3L);
    assertServiceException(() -> service.review(id, true, "自行复核", 3L), PAYROLL_WORK_INVALID);
    service.review(id, true, "独立复核差异事实与依据", 2L);
    assertEquals("CLOSED", cases.selectById(id).getStatus());
    assertEquals(3, service.history(id, 2L).size());
    assertServiceException(() -> service.review(id, true, "重复", 2L), PAYROLL_WORK_INVALID);
    service.reopen(id, "重新核实", 2L);
    assertEquals("OPEN", cases.selectById(id).getStatus());
    assertEquals(4, service.history(id, 2L).size());
  }

  @Test
  void missingClockFactsCannotBeApprovedAsZero() {
    source.put("ready", false);
    source.put("actualMinutes", null);
    Long id = service.create(request(), 1L);
    service.submit(id, "没有打卡不能证明零工时", 3L);
    assertServiceException(() -> service.review(id, true, "确认", 2L), PAYROLL_WORK_INVALID);
    assertEquals("SUBMITTED", cases.selectById(id).getStatus());
  }

  @Test
  void changedSourceAfterSubmissionRequiresNewOwnerEvidence() {
    Long id = service.create(request(), 1L);
    service.submit(id, "原依据", 3L);
    source.put("actualMinutes", 600);
    assertServiceException(() -> service.review(id, true, "确认旧来源", 2L), PAYROLL_WORK_INVALID);
    assertFalse(service.list(10L, employeeId, 2L).get(0).getSourceFresh());
    service.review(id, false, "需重新核实", 2L);
    service.submit(id, "更新来源后重新提交", 3L);
    service.review(id, true, "新来源已核实", 2L);
    assertEquals("CLOSED", cases.selectById(id).getStatus());
    assertEquals(5, service.history(id, 2L).size());
  }

  @Test
  void selfReviewAndSameOwnerReviewerAreRejected() {
    assertServiceException(
        () -> service.create(request().setReviewerId(99L), 1L), PAYROLL_SELF_APPROVAL);
    assertServiceException(
        () -> service.create(request().setReviewerId(1L), 1L), PAYROLL_SELF_APPROVAL);
    assertServiceException(
        () -> service.create(request().setReviewerId(3L), 1L), PAYROLL_SELF_APPROVAL);
    assertEquals(0, cases.selectCount());
  }

  @Test
  void idempotentRequestsAndActiveDatesCannotDuplicate() {
    HrmPayrollWorkVarianceReqVO request = request();
    Long id = service.create(request, 1L);
    assertEquals(id, service.create(request, 1L));
    assertServiceException(
        () -> service.create(request().setRequestKey("another"), 1L), PAYROLL_WORK_INVALID);
    assertServiceException(() -> service.create(request, 4L), PAYROLL_WORK_INVALID);
    assertServiceException(
        () -> service.create(request().setReason("修改原请求"), 1L), PAYROLL_WORK_INVALID);
  }

  @Test
  void unchangedNoDifferenceIsNotAnExceptionCase() {
    source.put("differenceMinutes", 0);
    assertServiceException(() -> service.create(request(), 1L), PAYROLL_WORK_INVALID);
  }

  @Test
  void remindersDeduplicateRecipientsAndRetryOnlyFailedDeliveries() {
    Long id = service.create(request(), 1L);
    cases.updateById(
        new HrmPayrollWorkVarianceDO()
            .setId(id)
            .setDueTime(LocalDateTime.now(ZoneId.of("Asia/Shanghai")).minusMinutes(1)));
    when(notifications.sendSingleMessageToAdmin(any()))
        .thenAnswer(
            call -> {
              cn.iocoder.yudao.module.system.api.notify.dto.NotifySendSingleToUserReqDTO message =
                  call.getArgument(0);
              if (message.getUserId().equals(2L)) throw new IllegalStateException("通知失败");
              return 100L;
            });
    assertEquals(1, service.remind(id));
    assertFalse(cases.selectById(id).getReminderError().isEmpty());
    doReturn(101L).when(notifications).sendSingleMessageToAdmin(any());
    assertEquals(1, service.remind(id));
    assertEquals(0, service.remind(id));
    verify(notifications, times(1))
        .sendSingleMessageToAdmin(argThat(message -> message.getUserId().equals(3L)));
    assertEquals("", cases.selectById(id).getReminderError());
  }

  @Test
  void revokedRecipientGrantPreventsReminderDelivery() {
    Long id = service.create(request(), 1L);
    cases.updateById(
        new HrmPayrollWorkVarianceDO()
            .setId(id)
            .setDueTime(LocalDateTime.now(ZoneId.of("Asia/Shanghai")).minusMinutes(1)));
    doThrow(
            cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception(
                PAYROLL_ENTITY_INVALID))
        .when(entities)
        .check(10L, "REVIEW", 2L);
    when(notifications.sendSingleMessageToAdmin(any())).thenReturn(100L);
    assertEquals(1, service.remind(id));
    verify(notifications, never())
        .sendSingleMessageToAdmin(argThat(message -> message.getUserId().equals(2L)));
  }

  @Test
  void reassignmentResetsSubmissionAndPreservesOriginalAssigneesInHistory() {
    Long id = service.create(request(), 1L);
    service.submit(id, "原责任人的核实", 3L);
    cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollWorkVarianceAssignReqVO
        assignment =
            new cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo
                    .HrmPayrollWorkVarianceAssignReqVO()
                .setId(id)
                .setOwnerId(4L)
                .setReviewerId(5L)
                .setDueTime(request().getDueTime())
                .setReason("岗位调整，重新分配核实职责");
    service.reassign(assignment, 1L);
    assertEquals("OPEN", cases.selectById(id).getStatus());
    assertTrue(service.history(id, 1L).get(0).getSnapshot().contains("\"ownerId\":3"));
    assertServiceException(() -> service.submit(id, "原责任人不能再提交", 3L), PAYROLL_WORK_INVALID);
    service.submit(id, "新责任人核实", 4L);
    service.review(id, true, "新独立复核人确认", 5L);
    assertEquals("CLOSED", cases.selectById(id).getStatus());
    assertEquals(5, service.history(id, 1L).size());
  }

  @Test
  void overdueFlagAndEntityPermissionsAreChecked() {
    Long id = service.create(request(), 1L);
    cases.updateById(
        new HrmPayrollWorkVarianceDO()
            .setId(id)
            .setDueTime(LocalDateTime.now(ZoneId.of("Asia/Shanghai")).minusMinutes(1)));
    assertTrue(service.list(10L, employeeId, 2L).get(0).getOverdue());
    doThrow(
            cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception(
                PAYROLL_ENTITY_INVALID))
        .when(entities)
        .check(10L, "READ", 4L);
    assertServiceException(() -> service.list(10L, employeeId, 4L), PAYROLL_ENTITY_INVALID);
    assertServiceException(() -> service.history(id, 4L), PAYROLL_ENTITY_INVALID);
  }
}
