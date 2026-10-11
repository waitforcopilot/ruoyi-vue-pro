package cn.iocoder.yudao.module.hrm.service.payroll;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.attendance.config.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.attendance.record.HrmAttendanceClockDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.attendance.config.HrmAttendanceHolidayMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.attendance.record.HrmAttendanceClockMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.employee.info.HrmEmployeeMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.*;
import cn.iocoder.yudao.module.hrm.service.attendance.config.HrmAttendanceGroupService;
import cn.iocoder.yudao.module.oa.api.payroll.OaPayrollSourceApi;
import cn.iocoder.yudao.module.oa.api.payroll.dto.OaPayrollOvertimeDTO;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import javax.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

@Import({
  HrmPayrollWorkService.class,
  org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration.class
})
class HrmPayrollWorkServiceTest extends BaseDbUnitTest {
  @Resource private HrmPayrollWorkService service;
  @Resource private HrmEmployeeMapper employees;
  @Resource private HrmPayrollWorkRuleMapper rules;
  @Resource private HrmPayrollTimeEntryMapper entries;
  @Resource private HrmPayrollTimeOffMapper off;
  @Resource private HrmAttendanceClockMapper clocks;
  @Resource private HrmAttendanceHolidayMapper holidays;
  @MockBean private OaPayrollSourceApi oa;
  @MockBean private HrmAttendanceGroupService groups;
  private Long employeeId;
  private final LocalDate day = LocalDate.of(2026, 9, 10);

  @BeforeEach
  void fixtures() {
    HrmEmployeeDO employee =
        new HrmEmployeeDO().setName("工时员工").setJobNumber("WORK-1").setUserId(99L);
    employees.insert(employee);
    employeeId = employee.getId();
    when(groups.getAttendanceGroupMap(anyCollection()))
        .thenReturn(Collections.singletonMap(employeeId, new HrmAttendanceGroupDO()));
    when(groups.getEmployeeAttendanceShift(eq(employeeId), any()))
        .thenReturn(
            HrmAttendanceGroupDO.Shift.builder()
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(17, 0))
                .build());
  }

  private HrmPayrollWorkRuleReqVO policy(String type) {
    HrmPayrollWorkRuleReqVO r = new HrmPayrollWorkRuleReqVO();
    r.setEmployeeId(employeeId);
    r.setSystemType(type);
    r.setEffectiveFrom(day.withDayOfMonth(1));
    r.setEffectiveTo(day.withDayOfMonth(1).plusMonths(3).minusDays(1));
    r.setLegalBasis("工时制度批准文件编号");
    r.setCycleMinutes(100);
    r.setCycleMonths(1);
    r.setTimeOffExpiryDays(90);
    r.setLimitMinutes(2160);
    r.setHourlyBase(new BigDecimal("20"));
    r.setWorkdayRate(new BigDecimal("1.5"));
    r.setRestdayRate(new BigDecimal("2"));
    r.setHolidayRate(new BigDecimal("3"));
    return r;
  }

  private Long approved(String type) {
    Long id = service.createRule(policy(type));
    HrmPayrollWorkRuleDO r = rules.selectById(id);
    r.setCreator("1");
    rules.updateById(r);
    service.approveRule(id, 2L);
    return id;
  }

  private HrmPayrollTimeEntryReqVO request(LocalDateTime start, LocalDateTime end) {
    OaPayrollOvertimeDTO source = new OaPayrollOvertimeDTO();
    source.setId(7L);
    source.setApplicantUserId(99L);
    source.setStartTime(start);
    source.setEndTime(end);
    source.setProcessInstanceId("approved-flow");
    when(oa.getApprovedOvertime(7L)).thenReturn(source);
    HrmAttendanceClockDO in =
        HrmAttendanceClockDO.builder()
            .employeeId(employeeId)
            .type(1)
            .status(0)
            .clockTime(start)
            .attendanceTime(start)
            .build();
    clocks.insert(in);
    HrmAttendanceClockDO out =
        HrmAttendanceClockDO.builder()
            .employeeId(employeeId)
            .type(2)
            .status(0)
            .clockTime(end)
            .attendanceTime(end)
            .build();
    clocks.insert(out);
    HrmPayrollTimeEntryReqVO r = new HrmPayrollTimeEntryReqVO();
    r.setEmployeeId(employeeId);
    r.setSourceSystem("OA");
    r.setSourceRecordId("7");
    r.setClockInId(in.getId());
    r.setClockOutId(out.getId());
    r.setEvidence("核对已批准加班");
    r.setStartTime(start);
    r.setEndTime(end);
    r.setBreakMinutes(0);
    r.setDayType("HOLIDAY");
    r.setCompensation("PAY");
    return r;
  }

  @Test
  void policyApprovalAndStatutoryMinimum() {
    HrmPayrollWorkRuleReqVO p = policy("STANDARD");
    p.setHolidayRate(BigDecimal.ONE);
    assertServiceException(() -> service.createRule(p), PAYROLL_WORK_INVALID);
    Long id = service.createRule(policy("STANDARD"));
    HrmPayrollWorkRuleDO r = rules.selectById(id);
    r.setCreator("1");
    rules.updateById(r);
    assertServiceException(() -> service.approveRule(id, 1L), PAYROLL_SELF_APPROVAL);
    service.approveRule(id, 2L);
    assertEquals("APPROVED", rules.selectById(id).getStatus());
  }

  @Test
  void authoritativeClockAndCalendarOverrideClient() {
    approved("STANDARD");
    HrmPayrollTimeEntryReqVO r = request(day.atTime(18, 0), day.atTime(20, 0));
    r.setClockEnd(day.atTime(23, 0));
    Long id = service.confirm(r, 2L).get(0);
    HrmPayrollTimeEntryDO e = entries.selectById(id);
    assertEquals(120, e.getRecognizedMinutes());
    assertEquals("WORKDAY", e.getDayType());
    assertEquals(0, new BigDecimal("60").compareTo(e.getAmount()));
    assertTrue(e.getSourceSnapshot().contains("clockInId"));
    assertServiceException(() -> service.confirm(r, 2L), PAYROLL_WORK_INVALID);
  }

  @Test
  void wrongApplicantAndPendingApprovalCannotPay() {
    approved("STANDARD");
    HrmPayrollTimeEntryReqVO r = request(day.atTime(18, 0), day.atTime(20, 0));
    OaPayrollOvertimeDTO source = oa.getApprovedOvertime(7L);
    source.setApplicantUserId(100L);
    assertServiceException(() -> service.confirm(r, 2L), PAYROLL_WORK_INVALID);
    when(oa.getApprovedOvertime(7L)).thenReturn(null);
    assertServiceException(() -> service.confirm(r, 2L), PAYROLL_WORK_INVALID);
    assertEquals(0L, entries.selectCount());
  }

  @Test
  void crossMidnightSplitsAndUsesEachDaysClassification() {
    approved("STANDARD");
    HrmPayrollTimeEntryReqVO r = request(day.atTime(23, 0), day.plusDays(1).atTime(1, 0));
    HrmAttendanceHolidayDO holiday = new HrmAttendanceHolidayDO();
    holiday.setDate(day.plusDays(1).atStartOfDay());
    holiday.setType(3);
    holidays.insert(holiday);
    List<Long> ids = service.confirm(r, 2L);
    assertEquals(2, ids.size());
    assertEquals(0, new BigDecimal("30").compareTo(entries.selectById(ids.get(0)).getAmount()));
    assertEquals(0, new BigDecimal("60").compareTo(entries.selectById(ids.get(1)).getAmount()));
  }

  @Test
  void sourceChangedAfterConfirmationBlocksCalculation() {
    approved("STANDARD");
    HrmPayrollTimeEntryReqVO r = request(day.atTime(18, 0), day.atTime(20, 0));
    service.confirm(r, 2L);
    HrmAttendanceClockDO out = clocks.selectById(r.getClockOutId());
    out.setClockTime(day.atTime(21, 0));
    clocks.updateById(out);
    assertServiceException(() -> service.earnings(employeeId, day, day), PAYROLL_WORK_INVALID);
  }

  @Test
  void onlyRestDaysCanConvertToTimeOff() {
    approved("STANDARD");
    HrmPayrollTimeEntryReqVO r = request(day.atTime(18, 0), day.atTime(20, 0));
    r.setCompensation("TIME_OFF");
    assertServiceException(() -> service.confirm(r, 2L), PAYROLL_WORK_INVALID);
    when(groups.getEmployeeAttendanceShift(eq(employeeId), any())).thenReturn(null);
    Long id = service.confirm(r, 2L).get(0);
    assertEquals(0, entries.selectById(id).getAmount().signum());
    assertEquals(120, service.timeOffBalance(employeeId));
    Long debit = service.useTimeOff(employeeId, 90, "LEAVE-1", "调休申请占用", 2L);
    assertEquals(debit, service.useTimeOff(employeeId, 90, "LEAVE-1", "重试", 2L));
    assertEquals(30, service.timeOffBalance(employeeId));
    service.consumeTimeOff(debit, 2L);
    Long refund = service.refundTimeOff(debit, "ignored", "撤销调休", 2L);
    assertEquals(refund, service.refundTimeOff(debit, "ignored2", "重试", 2L));
    assertEquals(120, service.timeOffBalance(employeeId));
    assertServiceException(() -> service.consumeTimeOff(debit, 2L), PAYROLL_WORK_INVALID);
  }

  @Test
  void expiredRefundCannotExtendOriginalEntitlement() {
    HrmPayrollTimeOffDO credit = new HrmPayrollTimeOffDO();
    credit.setEmployeeId(employeeId);
    credit.setMinutes(120);
    credit.setStatus("CREDIT");
    credit.setExpiresOn(LocalDate.now(ZoneId.of("Asia/Shanghai")).plusDays(1));
    credit.setRequestKey("lot");
    off.insert(credit);
    Long debit = service.useTimeOff(employeeId, 120, "leave", "占用", 2L);
    credit.setExpiresOn(LocalDate.now(ZoneId.of("Asia/Shanghai")).minusDays(1));
    off.updateById(credit);
    assertServiceException(() -> service.consumeTimeOff(debit, 2L), PAYROLL_WORK_INVALID);
    service.refundTimeOff(debit, "refund", "退回", 2L);
    assertEquals(0, service.timeOffBalance(employeeId));
  }

  @Test
  void comprehensiveCycleDefersDailyPayAndCapsByApprovedExcess() {
    Long rule = approved("COMPREHENSIVE");
    HrmPayrollTimeEntryReqVO r = request(day.atTime(18, 0), day.atTime(20, 0));
    Long id = service.confirm(r, 2L).get(0);
    assertEquals(0, entries.selectById(id).getAmount().signum());
    Long cycle = service.settleCycle(rule, day.withDayOfMonth(1), 2L);
    assertEquals(cycle, service.settleCycle(rule, day.withDayOfMonth(1), 2L));
    assertEquals(
        0,
        new BigDecimal("10")
            .compareTo(
                service.earnings(employeeId, day.withDayOfMonth(1), day.withDayOfMonth(30))));
    assertEquals(20, HrmPayrollWorkService.cyclePayableMinutes(120, 100, 120));
  }

  @Test
  void irregularNonHolidayDoesNotUseStandardOvertimeMultiplier() {
    approved("IRREGULAR");
    HrmPayrollTimeEntryReqVO r = request(day.atTime(18, 0), day.atTime(20, 0));
    assertEquals(0, entries.selectById(service.confirm(r, 2L).get(0)).getAmount().signum());
  }

  @Test
  void endedCycleMustBeSettledBeforePayroll() {
    approved("COMPREHENSIVE");
    assertServiceException(
        () -> service.earnings(employeeId, day.withDayOfMonth(1), day.withDayOfMonth(30)),
        PAYROLL_WORK_INVALID);
  }

  @Test
  void approvedRegularShiftCannotAlsoBePaidAsOvertime() {
    approved("STANDARD");
    HrmPayrollTimeEntryReqVO r = request(day.atTime(9, 0), day.atTime(11, 0));
    assertServiceException(() -> service.confirm(r, 2L), PAYROLL_WORK_INVALID);
    assertEquals(0L, entries.selectCount());
  }
}
