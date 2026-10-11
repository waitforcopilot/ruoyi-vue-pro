package cn.iocoder.yudao.module.hrm.service.payroll;
import cn.iocoder.yudao.module.hrm.controller.admin.attendance.vo.statistics.HrmAttendanceMonthRecordRespVO;
import cn.iocoder.yudao.module.hrm.service.attendance.statistics.HrmAttendanceStatisticsService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class HrmPayrollWindowServiceTest {
 @Test void affiliationGapsDoNotIncludeOtherEmployersAttendanceOrOvertime() {
  HrmPayrollWindowService service=new HrmPayrollWindowService();
  HrmAttendanceStatisticsService attendance=Mockito.mock(HrmAttendanceStatisticsService.class);HrmPayrollWorkService work=Mockito.mock(HrmPayrollWorkService.class);
  org.springframework.test.util.ReflectionTestUtils.setField(service,"attendance",attendance);org.springframework.test.util.ReflectionTestUtils.setField(service,"work",work);
  LocalDate start=LocalDate.of(2026,7,1),third=start.plusDays(2);Set<LocalDate> dates=new HashSet<>(Arrays.asList(start,third));
  when(attendance.getAttendancePeriodRecordMap(start,start,Collections.singletonList(1L))).thenReturn(Collections.singletonMap(1L,new HrmAttendanceMonthRecordRespVO().setActualDays(BigDecimal.ONE).setAttendDays(1).setLeaveMinutes(30).setAttendanceDeductAmount(new BigDecimal("10"))));
  when(attendance.getAttendancePeriodRecordMap(third,third,Collections.singletonList(1L))).thenReturn(Collections.singletonMap(1L,new HrmAttendanceMonthRecordRespVO().setActualDays(BigDecimal.ONE).setAttendDays(1).setLeaveMinutes(60).setAttendanceDeductAmount(new BigDecimal("20"))));
  when(work.earnings(1L,start,start)).thenReturn(new BigDecimal("100"));when(work.earnings(1L,third,third)).thenReturn(new BigDecimal("200"));
  HrmAttendanceMonthRecordRespVO sum=service.attendance(1L,dates);assertEquals(new BigDecimal("2"),sum.getActualDays());assertEquals(2,sum.getAttendDays());assertEquals(90,sum.getLeaveMinutes());assertEquals(new BigDecimal("30"),sum.getAttendanceDeductAmount());assertEquals(new BigDecimal("300"),service.earnings(1L,dates));verify(work).earnings(1L,start,start);verify(work).earnings(1L,third,third);verifyNoMoreInteractions(work);
  verify(attendance).getAttendancePeriodRecordMap(start,start,Collections.singletonList(1L));verify(attendance).getAttendancePeriodRecordMap(third,third,Collections.singletonList(1L));verifyNoMoreInteractions(attendance);
 }
}
