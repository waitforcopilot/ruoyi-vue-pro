package cn.iocoder.yudao.module.hrm.service.payroll;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.PAYROLL_ATTENDANCE_INVALID;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.HrmPayrollAttendanceIssueDO;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.HrmPayrollAttendanceIssueMapper;
import cn.iocoder.yudao.module.hrm.service.attendance.statistics.HrmAttendanceStatisticsService;
import java.time.LocalDate;
import java.util.*;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class HrmPayrollAttendanceService {
  @Resource private HrmAttendanceStatisticsService statistics;
  @Resource private HrmPayrollAttendanceIssueMapper issues;

  public Map<String, Object> snapshot(LocalDate from, LocalDate to, List<Long> employeeIds) {
    if (!employeeIds.isEmpty()
        && issues.selectCount(
                new LambdaQueryWrapperX<HrmPayrollAttendanceIssueDO>()
                    .in(HrmPayrollAttendanceIssueDO::getEmployeeId, employeeIds)
                    .ge(HrmPayrollAttendanceIssueDO::getAttendanceDate, from)
                    .le(HrmPayrollAttendanceIssueDO::getAttendanceDate, to)
                    .ne(HrmPayrollAttendanceIssueDO::getStatus, "CLOSED"))
            > 0) throw exception(PAYROLL_ATTENDANCE_INVALID);
    Map<String, Object> snapshot = statistics.getPayrollAttendanceSnapshot(from, to, employeeIds);
    if (snapshot == null || !(snapshot.get("sources") instanceof Map))
      throw exception(PAYROLL_ATTENDANCE_INVALID);
    Map<?, ?> sources = (Map<?, ?>) snapshot.get("sources");
    for (Long employeeId : employeeIds) {
      Object source =
          sources.containsKey(employeeId)
              ? sources.get(employeeId)
              : sources.get(employeeId.toString());
      if (!(source instanceof Map) || ((Map<?, ?>) source).get("group") == null)
        throw exception(PAYROLL_ATTENDANCE_INVALID);
    }
    return snapshot;
  }

  public void validateInputSnapshot(String json) {
    Map<String, Object> input = JsonUtils.parseObject(json, Map.class);
    Object value = input.get("attendanceSources");
    if (!(value instanceof Map)) return;
    Map<String, Object> original = (Map<String, Object>) value;
    LocalDate
        from = JsonUtils.parseObject(JsonUtils.toJsonString(original.get("from")), LocalDate.class),
        to = JsonUtils.parseObject(JsonUtils.toJsonString(original.get("to")), LocalDate.class);
    List<Long> employeeIds = new ArrayList<>();
    for (Object id : (List<?>) original.get("employeeIds"))
      employeeIds.add(((Number) id).longValue());
    Map<String, Object> current = snapshot(from, to, employeeIds);
    if (!JsonUtils.parseTree(JsonUtils.toJsonString(current))
        .equals(JsonUtils.parseTree(JsonUtils.toJsonString(original))))
      throw exception(PAYROLL_ATTENDANCE_INVALID);
  }
}
