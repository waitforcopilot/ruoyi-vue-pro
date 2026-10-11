package cn.iocoder.yudao.module.hrm.service.payroll;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.hrm.controller.admin.attendance.vo.clock.*;
import cn.iocoder.yudao.module.hrm.controller.admin.attendance.vo.statistics.HrmAttendanceDailyDetailRespVO;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollAttendanceIssueReqVO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.attendance.config.HrmAttendanceGroupDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.attendance.record.HrmAttendanceClockDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.attendance.record.HrmAttendanceClockMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.employee.info.HrmEmployeeMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.*;
import cn.iocoder.yudao.module.hrm.service.attendance.config.HrmAttendanceGroupService;
import cn.iocoder.yudao.module.hrm.service.attendance.record.HrmAttendanceClockService;
import cn.iocoder.yudao.module.hrm.service.attendance.statistics.HrmAttendanceStatisticsService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import java.time.*;
import java.util.*;
import javax.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

@Import({
  HrmPayrollAttendanceIssueService.class,
  org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration.class
})
class HrmPayrollAttendanceIssueServiceTest extends BaseDbUnitTest {
  @Resource private HrmPayrollAttendanceIssueService service;
  @Resource private HrmEmployeeMapper employees;
  @Resource private HrmPayrollAttendanceIssueMapper issues;
  @Resource private HrmAttendanceClockMapper clocks;
  @MockBean private HrmAttendanceStatisticsService statistics;
  @MockBean private HrmAttendanceGroupService groups;
  @MockBean private HrmAttendanceClockService clockService;
  @MockBean private AdminUserApi users;
  private Long employeeId;
  private final LocalDate day = LocalDate.of(2026, 9, 10);
  private HrmAttendanceDailyDetailRespVO initial;

  @BeforeEach
  void fixtures() {
    HrmEmployeeDO employee =
        new HrmEmployeeDO().setName("补卡员工").setJobNumber("ATT-1").setUserId(10L);
    employees.insert(employee);
    employeeId = employee.getId();
    initial =
        new HrmAttendanceDailyDetailRespVO()
            .setEmployeeId(employeeId)
            .setScheduled(true)
            .setMisscardCount(1)
            .setAbsenteeism(false)
            .setClockList(Collections.emptyList());
    when(statistics.getAttendanceDailyDetail(any())).thenReturn(initial);
    when(groups.getEmployeeAttendanceShift(anyLong(), any()))
        .thenReturn(
            HrmAttendanceGroupDO.Shift.builder()
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(17, 0))
                .build());
    when(clockService.createAttendanceClock(any()))
        .thenAnswer(
            call -> {
              HrmAttendanceClockSaveReqVO r = call.getArgument(0);
              HrmAttendanceClockDO clock =
                  HrmAttendanceClockDO.builder()
                      .employeeId(r.getEmployeeId())
                      .type(r.getType())
                      .clockTime(r.getClockTime())
                      .attendanceTime(r.getAttendanceTime())
                      .sourceType(2)
                      .status(0)
                      .stage(1)
                      .build();
              clocks.insert(clock);
              return clock.getId();
            });
  }

  private HrmPayrollAttendanceIssueReqVO request() {
    HrmPayrollAttendanceIssueReqVO r = new HrmPayrollAttendanceIssueReqVO();
    r.setEmployeeId(employeeId);
    r.setAttendanceDate(day);
    r.setType("MISSCARD");
    r.setClockType(1);
    r.setOwnerId(10L);
    r.setReviewerId(20L);
    r.setRequestKey("ATT-KEY-1");
    r.setDueTime(day.plusDays(1).atTime(17, 0));
    return r;
  }

  private Long submitted() {
    Long id = service.create(request(), 30L);
    HrmPayrollAttendanceIssueReqVO.Submit submit = new HrmPayrollAttendanceIssueReqVO.Submit();
    submit.setId(id);
    submit.setEvidence("主管到岗证明及门禁资料");
    submit.setCorrectionTime(day.atTime(9, 0));
    service.submit(submit, 10L);
    return id;
  }

  @Test
  void actualAbnormalityAndIndependentReviewerRequired() {
    initial.setMisscardCount(0);
    assertServiceException(() -> service.create(request(), 30L), PAYROLL_ATTENDANCE_ISSUE_INVALID);
    initial.setMisscardCount(1);
    HrmPayrollAttendanceIssueReqVO r = request();
    r.setReviewerId(10L);
    assertServiceException(() -> service.create(r, 30L), PAYROLL_ATTENDANCE_ISSUE_INVALID);
    assertEquals(0L, issues.selectCount());
  }

  @Test
  void duplicateRequestDoesNotCreateDuplicateWorkflow() {
    Long id = service.create(request(), 30L);
    assertEquals(id, service.create(request(), 30L));
    assertEquals(1, service.history(id).size());
    HrmPayrollAttendanceIssueReqVO r = request();
    r.setClockType(2);
    assertServiceException(() -> service.create(r, 30L), PAYROLL_ATTENDANCE_ISSUE_INVALID);
  }

  @Test
  void otherEmployeesCannotSubmitOrReadEvidence() {
    Long id = service.create(request(), 30L);
    HrmPayrollAttendanceIssueReqVO.Submit submit = new HrmPayrollAttendanceIssueReqVO.Submit();
    submit.setId(id);
    submit.setEvidence("他人依据");
    submit.setCorrectionTime(day.atTime(9, 0));
    assertServiceException(() -> service.submit(submit, 99L), PAYROLL_ATTENDANCE_ISSUE_INVALID);
    assertServiceException(() -> service.myHistory(id, 99L), PAYROLL_ATTENDANCE_ISSUE_INVALID);
    assertEquals(1, service.myHistory(id, 10L).size());
    assertTrue(service.mine(99L).isEmpty());
  }

  @Test
  void designatedHrReviewsAndRejectedEvidenceRemains() {
    Long id = submitted();
    assertServiceException(() -> service.review(id, true, "复核", 10L), PAYROLL_ATTENDANCE_ISSUE_INVALID);
    assertServiceException(() -> service.review(id, true, "复核", 99L), PAYROLL_ATTENDANCE_ISSUE_INVALID);
    service.review(id, false, "证据不完整，补充门禁", 20L);
    assertEquals("OPEN", issues.selectById(id).getStatus());
    assertTrue(issues.selectById(id).getEvidence().contains("门禁"));
    assertEquals(3, service.history(id).size());
    assertEquals(0L, clocks.selectCount());
  }

  @Test
  void approvedCorrectionPreservesPhysicalClockAndCloseRequiresResolvedState() {
    HrmAttendanceClockDO physical =
        HrmAttendanceClockDO.builder()
            .employeeId(employeeId)
            .type(2)
            .clockTime(day.atTime(17, 0))
            .attendanceTime(day.atTime(17, 0))
            .sourceType(1)
            .status(0)
            .build();
    clocks.insert(physical);
    Long id = submitted();
    service.review(id, true, "核实资料一致", 20L);
    Long corrected = issues.selectById(id).getCorrectionClockId();
    assertNotNull(corrected);
    assertEquals(3, clocks.selectById(corrected).getSourceType());
    assertEquals(1, clocks.selectById(physical.getId()).getSourceType());
    assertEquals(day.atTime(17, 0), clocks.selectById(physical.getId()).getClockTime());
    assertServiceException(() -> service.close(id, "关闭", 20L), PAYROLL_ATTENDANCE_ISSUE_INVALID);
    initial.setClockList(
        Collections.singletonList(
            new HrmAttendanceClockRespVO()
                .setId(corrected)
                .setType(1)
                .setClockTime(day.atTime(9, 0))));
    initial.setMisscardCount(0);
    service.close(id, "重新计算后缺卡已消除", 20L);
    assertEquals("CLOSED", issues.selectById(id).getStatus());
    assertEquals(4, service.history(id).size());
    assertTrue(service.overdue(10L).isEmpty());
    service.reopen(id, "门禁证据更正，重新提交", 20L);
    assertEquals("OPEN", issues.selectById(id).getStatus());
    assertEquals(3, clocks.selectById(corrected).getStatus());
    assertEquals(1, clocks.selectById(physical.getId()).getSourceType());
    assertEquals(1, service.overdue(10L).size());
  }

  @Test
  void failedCorrectionDoesNotApproveOrLeavePartialEvent() {
    Long id = submitted();
    doReturn(99999L).when(clockService).createAttendanceClock(any());
    assertServiceException(() -> service.review(id, true, "核实", 20L), PAYROLL_ATTENDANCE_ISSUE_INVALID);
    assertEquals("SUBMITTED", issues.selectById(id).getStatus());
    assertEquals(2, service.history(id).size());
  }
}
