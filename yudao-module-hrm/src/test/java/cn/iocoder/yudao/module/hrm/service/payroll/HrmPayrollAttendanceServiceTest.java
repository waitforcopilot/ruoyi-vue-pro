package cn.iocoder.yudao.module.hrm.service.payroll;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;
import static org.mockito.Mockito.*;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.HrmPayrollAttendanceIssueDO;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.HrmPayrollAttendanceIssueMapper;
import cn.iocoder.yudao.module.hrm.service.attendance.statistics.HrmAttendanceStatisticsService;
import java.time.*;
import java.util.*;
import javax.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

@Import(HrmPayrollAttendanceService.class)
class HrmPayrollAttendanceServiceTest extends BaseDbUnitTest {
  @Resource private HrmPayrollAttendanceService service;
  @Resource private HrmPayrollAttendanceIssueMapper issues;
  @MockBean private HrmAttendanceStatisticsService statistics;

  private Map<String, Object> employeeSource(String time) {
    Map<String, Object> source = new LinkedHashMap<>();
    source.put("clockTime", time);
    source.put("group", Collections.singletonMap("id", 1));
    return source;
  }

  private Map<String, Object> source() {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("from", LocalDate.of(2026, 9, 21));
    map.put("to", LocalDate.of(2026, 10, 20));
    map.put("employeeIds", Collections.singletonList(1L));
    map.put("sources", Collections.singletonMap("1", employeeSource("2026-09-21 09:00:00")));
    return map;
  }

  @Test
  void changedSourceBlocksFreezeButUnchangedSourcePasses() {
    Map<String, Object> source = source();
    when(statistics.getPayrollAttendanceSnapshot(any(), any(), any())).thenReturn(source);
    String snapshot = JsonUtils.toJsonString(Collections.singletonMap("attendanceSources", source));
    service.validateInputSnapshot(snapshot);
    Map<String, Object> changed = new LinkedHashMap<>(source);
    changed.put("sources", Collections.singletonMap("1", employeeSource("2026-09-21 10:00:00")));
    when(statistics.getPayrollAttendanceSnapshot(any(), any(), any())).thenReturn(changed);
    assertServiceException(() -> service.validateInputSnapshot(snapshot), PAYROLL_ATTENDANCE_INVALID);
  }

  @Test
  void openWorkOrderBlocksTrialAndFreeze() {
    HrmPayrollAttendanceIssueDO issue = new HrmPayrollAttendanceIssueDO();
    issue.setEmployeeId(1L);
    issue.setAttendanceDate(LocalDate.of(2026, 10, 1));
    issue.setStatus("APPROVED");
    issue.setRequestKey("OPEN-CASE");
    issues.insert(issue);
    assertServiceException(
        () ->
            service.snapshot(
                LocalDate.of(2026, 9, 21),
                LocalDate.of(2026, 10, 20),
                Collections.singletonList(1L)),
        PAYROLL_ATTENDANCE_INVALID);
    issue.setStatus("CLOSED");
    issues.updateById(issue);
    when(statistics.getPayrollAttendanceSnapshot(any(), any(), any())).thenReturn(source());
    service.snapshot(
        LocalDate.of(2026, 9, 21), LocalDate.of(2026, 10, 20), Collections.singletonList(1L));
  }

  @Test
  void missingAttendanceGroupDoesNotBecomeFullAttendance() {
    Map<String, Object> snapshot = source();
    snapshot.put("sources", Collections.singletonMap("1", Collections.emptyMap()));
    when(statistics.getPayrollAttendanceSnapshot(any(), any(), any())).thenReturn(snapshot);
    assertServiceException(
        () ->
            service.snapshot(
                LocalDate.of(2026, 9, 21),
                LocalDate.of(2026, 10, 20),
                Collections.singletonList(1L)),
        PAYROLL_ATTENDANCE_INVALID);
  }
}
