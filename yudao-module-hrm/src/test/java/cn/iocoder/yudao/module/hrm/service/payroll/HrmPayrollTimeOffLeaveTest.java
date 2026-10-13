package cn.iocoder.yudao.module.hrm.service.payroll;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.bpm.api.task.BpmProcessInstanceApi;
import cn.iocoder.yudao.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import cn.iocoder.yudao.module.hrm.controller.admin.attendance.vo.leave.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.attendance.config.HrmAttendanceGroupDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.HrmPayrollTimeOffDO;
import cn.iocoder.yudao.module.hrm.dal.mysql.attendance.record.HrmAttendanceLeaveMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.employee.info.HrmEmployeeMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.HrmPayrollTimeOffMapper;
import cn.iocoder.yudao.module.hrm.service.attendance.config.HrmAttendanceGroupService;
import cn.iocoder.yudao.module.hrm.service.attendance.record.HrmAttendanceLeaveServiceImpl;
import cn.iocoder.yudao.module.hrm.service.employee.info.HrmEmployeeService;
import cn.iocoder.yudao.module.oa.api.payroll.OaPayrollSourceApi;
import java.math.BigDecimal;
import java.time.*;
import javax.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

@Import({
  HrmPayrollWorkService.class,
  HrmAttendanceLeaveServiceImpl.class,
  org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration.class
})
class HrmPayrollTimeOffLeaveTest extends BaseDbUnitTest {
  @Resource private HrmPayrollWorkService work;
  @Resource private HrmAttendanceLeaveServiceImpl leaves;
  @Resource private HrmEmployeeMapper employees;
  @Resource private HrmPayrollTimeOffMapper ledger;
  @Resource private HrmAttendanceLeaveMapper records;
  @MockBean private HrmEmployeeService employeeService;
  @MockBean private BpmProcessInstanceApi bpm;
  @MockBean private HrmAttendanceGroupService groups;
  @MockBean private OaPayrollSourceApi oa;
  private HrmEmployeeDO employee;
  private LocalDate day;

  @BeforeEach
  void setup() {
    day = LocalDate.now(ZoneId.of("Asia/Shanghai")).plusDays(2);
    employee = new HrmEmployeeDO().setName("调休员工").setJobNumber("BPM-LEAVE-1").setUserId(99L);
    employees.insert(employee);
    when(employeeService.validateEmployeeBySelf(99L)).thenReturn(employee);
    when(employeeService.getEmployee(employee.getId())).thenReturn(employee);
    when(bpm.createProcessInstance(eq(99L), any())).thenReturn("leave-process");
    when(groups.getEmployeeAttendanceShift(eq(employee.getId()), any()))
        .thenReturn(
            HrmAttendanceGroupDO.Shift.builder()
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(18, 0))
                .excludeRestTime(true)
                .restStartTime(LocalTime.of(12, 0))
                .restEndTime(LocalTime.of(13, 0))
                .build());
  }

  private HrmAttendanceLeaveCreateReqVO request() {
    HrmAttendanceLeaveCreateReqVO r = new HrmAttendanceLeaveCreateReqVO();
    r.setType("调休");
    r.setStartTime(day.atTime(9, 0));
    r.setEndTime(day.atTime(18, 0));
    r.setDay(new BigDecimal("9"));
    r.setReason("加班补休");
    return r;
  }

  private Long credit(int minutes, LocalDate expiry) {
    HrmPayrollTimeOffDO c = new HrmPayrollTimeOffDO();
    c.setEmployeeId(employee.getId());
    c.setMinutes(minutes);
    c.setStatus("CREDIT");
    c.setExpiresOn(expiry);
    ledger.insert(c);
    return c.getId();
  }

  @Test
  void reserveThenApproveExactlyOnce() {
    credit(600, day.plusDays(3));
    Long id = leaves.createLeave(99L, request());
    cn.iocoder.yudao.module.hrm.dal.dataobject.attendance.record.HrmAttendanceLeaveDO row =
        records.selectById(id);
    assertEquals(480, row.getTimeOffMinutes());
    assertEquals(0, BigDecimal.ONE.compareTo(row.getDay()));
    assertNotNull(row.getTimeOffSourceSnapshot());
    assertEquals(120, work.timeOffBalance(employee.getId()));
    leaves.updateLeaveApprovalStatus(
        id, "leave-process", BpmProcessInstanceStatusEnum.APPROVE.getStatus(), "同意");
    leaves.updateLeaveApprovalStatus(
        id, "leave-process", BpmProcessInstanceStatusEnum.REJECT.getStatus(), "迟到回调");
    assertEquals("CONSUMED", ledger.selectById(row.getTimeOffDebitId()).getStatus());
    assertEquals(2, ledger.selectList().size());
    assertEquals(120, work.timeOffBalance(employee.getId()));
  }

  @Test
  void rejectionRefundsOnceAndAllowsNewApplication() {
    credit(480, day.plusDays(3));
    Long id = leaves.createLeave(99L, request());
    leaves.updateLeaveApprovalStatus(
        id, "other-process", BpmProcessInstanceStatusEnum.REJECT.getStatus(), "错误流程");
    assertEquals(0, work.timeOffBalance(employee.getId()));
    leaves.updateLeaveApprovalStatus(
        id, "leave-process", BpmProcessInstanceStatusEnum.REJECT.getStatus(), "不同意");
    leaves.updateLeaveApprovalStatus(
        id, "leave-process", BpmProcessInstanceStatusEnum.REJECT.getStatus(), "重复");
    assertEquals(480, work.timeOffBalance(employee.getId()));
    assertEquals(3, ledger.selectList().size());
    when(bpm.createProcessInstance(eq(99L), any())).thenReturn("second-process");
    leaves.createLeave(99L, request());
    assertEquals(0, work.timeOffBalance(employee.getId()));
  }

  @Test
  void cancellationRefundsReservation() {
    credit(480, day.plusDays(3));
    Long id = leaves.createLeave(99L, request());
    HrmAttendanceLeaveCancelReqVO r = new HrmAttendanceLeaveCancelReqVO();
    r.setId(id);
    r.setReason("取消行程");
    leaves.cancelLeave(99L, r);
    assertEquals(480, work.timeOffBalance(employee.getId()));
    verify(bpm).cancelProcessInstanceByStartUser(99L, "leave-process", "取消行程");
  }

  @Test
  void insufficientOrExpiringCreditsRollBackWholeApplication() {
    credit(600, day.minusDays(1));
    assertThrows(RuntimeException.class, () -> leaves.createLeave(99L, request()));
    assertTrue(records.selectList().isEmpty());
    assertEquals(1, ledger.selectList().size());
    verifyNoInteractions(bpm);
  }

  @Test
  void failedProcessStartRollsBackReservationAndLeave() {
    credit(600, day.plusDays(3));
    when(bpm.createProcessInstance(eq(99L), any()))
        .thenThrow(new IllegalStateException("unavailable"));
    assertThrows(IllegalStateException.class, () -> leaves.createLeave(99L, request()));
    assertTrue(records.selectList().isEmpty());
    assertEquals(600, work.timeOffBalance(employee.getId()));
  }

  @Test
  void crossMidnightUsesPreviousShiftAndSubtractsNextDayBreak() {
    when(groups.getEmployeeAttendanceShift(eq(employee.getId()), any()))
        .thenReturn(
            HrmAttendanceGroupDO.Shift.builder()
                .startTime(LocalTime.of(22, 0))
                .endTime(LocalTime.of(6, 0))
                .excludeRestTime(true)
                .restStartTime(LocalTime.of(1, 0))
                .restEndTime(LocalTime.of(2, 0))
                .build());
    java.util.Map<String, Object> source =
        work.plannedTimeOff(employee.getId(), day.atStartOfDay(), day.atTime(6, 0));
    assertEquals(300, source.get("minutes"));
    assertEquals(new BigDecimal("0.71"), source.get("day"));
  }

  @Test
  void noShiftAndSubMinuteRequestsAreRejected() {
    when(groups.getEmployeeAttendanceShift(eq(employee.getId()), any())).thenReturn(null);
    assertThrows(
        RuntimeException.class,
        () -> work.plannedTimeOff(employee.getId(), day.atTime(9, 0), day.atTime(18, 0)));
    assertThrows(
        RuntimeException.class,
        () -> work.plannedTimeOff(employee.getId(), day.atTime(9, 0, 1), day.atTime(18, 0)));
  }

  @Test
  void amendedCreditExpiryBlocksApprovalAtomically() {
    Long lot = credit(480, day);
    Long id = leaves.createLeave(99L, request());
    HrmPayrollTimeOffDO c = ledger.selectById(lot);
    c.setExpiresOn(day.minusDays(1));
    ledger.updateById(c);
    assertThrows(
        RuntimeException.class,
        () ->
            leaves.updateLeaveApprovalStatus(
                id, "leave-process", BpmProcessInstanceStatusEnum.APPROVE.getStatus(), "同意"));
    assertEquals(
        BpmProcessInstanceStatusEnum.RUNNING.getStatus(),
        records.selectById(id).getApprovalStatus());
  }

  @Test
  void manualLedgerActionsCannotAlterLeaveReservations() {
    credit(600, day.plusDays(3));
    Long id = leaves.createLeave(99L, request());
    Long debit = records.selectById(id).getTimeOffDebitId();
    assertThrows(RuntimeException.class, () -> work.refundTimeOff(debit, "manual", "绕过审批", 2L));
    assertThrows(RuntimeException.class, () -> work.consumeTimeOff(debit, 2L));
    assertThrows(
        RuntimeException.class,
        () -> work.useTimeOff(employee.getId(), 1, "BPM-LEAVE-999", "预占", 2L));
    assertEquals(120, work.timeOffBalance(employee.getId()));
  }
}
