package cn.iocoder.yudao.module.hrm.service.payroll;

import cn.iocoder.yudao.module.hrm.controller.admin.attendance.vo.statistics.HrmAttendanceMonthRecordRespVO;
import cn.iocoder.yudao.module.hrm.service.attendance.statistics.HrmAttendanceStatisticsService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;

/** Aggregates only dates for which this employer has approved employee affiliation. */
@Service
public class HrmPayrollWindowService {
  @Resource private HrmAttendanceStatisticsService attendance;
  @Resource private HrmPayrollWorkService work;

  public List<LocalDate[]> intervals(Set<LocalDate> dates) {
    List<LocalDate[]> result = new ArrayList<>();
    if (dates == null || dates.isEmpty()) return result;
    LocalDate start = null, previous = null;
    for (LocalDate date : new TreeSet<>(dates)) {
      if (previous != null && !date.equals(previous.plusDays(1))) {
        result.add(new LocalDate[] {start, previous});
        start = null;
      }
      if (start == null) start = date;
      previous = date;
    }
    result.add(new LocalDate[] {start, previous});
    return result;
  }

  public BigDecimal earnings(Long employeeId, Set<LocalDate> dates) {
    BigDecimal total = BigDecimal.ZERO;
    for (LocalDate[] range : intervals(dates))
      total = total.add(work.earnings(employeeId, range[0], range[1]));
    return total;
  }

  public BigDecimal earningsForEntity(Long employeeId, Long entityId, Set<LocalDate> dates) {
    BigDecimal total = BigDecimal.ZERO;
    for (LocalDate[] range : intervals(dates))
      total = total.add(work.earningsForEntity(employeeId, entityId, range[0], range[1]));
    return total;
  }

  public boolean hasSources(Long employeeId, Set<LocalDate> dates) {
    for (LocalDate[] range : intervals(dates))
      if (work.hasSources(employeeId, range[0], range[1])) return true;
    return false;
  }

  private int n(Integer v) {
    return v == null ? 0 : v;
  }

  private BigDecimal n(BigDecimal v) {
    return v == null ? BigDecimal.ZERO : v;
  }

  public HrmAttendanceMonthRecordRespVO attendance(Long employeeId, Set<LocalDate> dates) {
    HrmAttendanceMonthRecordRespVO result = null;
    for (LocalDate[] range : intervals(dates)) {
      HrmAttendanceMonthRecordRespVO row =
          attendance
              .getAttendancePeriodRecordMap(
                  range[0], range[1], Collections.singletonList(employeeId))
              .get(employeeId);
      if (row == null)
        throw cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception(
            cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.PAYROLL_ATTENDANCE_INVALID);
      if (result == null) {
        result = row;
        continue;
      }
      result.setAttendDays(n(result.getAttendDays()) + n(row.getAttendDays()));
      result.setActualDays(n(result.getActualDays()).add(n(row.getActualDays())));
      result.setLateMinute(n(result.getLateMinute()) + n(row.getLateMinute()));
      result.setLateCount(n(result.getLateCount()) + n(row.getLateCount()));
      result.setEarlyMinute(n(result.getEarlyMinute()) + n(row.getEarlyMinute()));
      result.setEarlyCount(n(result.getEarlyCount()) + n(row.getEarlyCount()));
      result.setMisscardCount(n(result.getMisscardCount()) + n(row.getMisscardCount()));
      result.setAbsenteeismDays(n(result.getAbsenteeismDays()).add(n(row.getAbsenteeismDays())));
      result.setAbsenteeismMinutes(
          n(result.getAbsenteeismMinutes()) + n(row.getAbsenteeismMinutes()));
      result.setLeaveDays(n(result.getLeaveDays()).add(n(row.getLeaveDays())));
      result.setLeaveMinutes(n(result.getLeaveMinutes()) + n(row.getLeaveMinutes()));
      result.setLateDeductAmount(n(result.getLateDeductAmount()).add(n(row.getLateDeductAmount())));
      result.setEarlyDeductAmount(
          n(result.getEarlyDeductAmount()).add(n(row.getEarlyDeductAmount())));
      result.setMisscardDeductAmount(
          n(result.getMisscardDeductAmount()).add(n(row.getMisscardDeductAmount())));
      result.setAbsenteeismDeductAmount(
          n(result.getAbsenteeismDeductAmount()).add(n(row.getAbsenteeismDeductAmount())));
      result.setAttendanceDeductAmount(
          n(result.getAttendanceDeductAmount()).add(n(row.getAttendanceDeductAmount())));
      result.setFullAttendance(
          Boolean.TRUE.equals(result.getFullAttendance())
              && Boolean.TRUE.equals(row.getFullAttendance()));
    }
    return result;
  }
}
