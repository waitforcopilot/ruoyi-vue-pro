package cn.iocoder.yudao.module.oa.api.payroll;

import cn.iocoder.yudao.module.oa.api.payroll.dto.OaPayrollOvertimeDTO;

public interface OaPayrollSourceApi {
  /** Returns an approved, tenant-visible source under the caller's transaction lock. */
  OaPayrollOvertimeDTO getApprovedOvertime(Long id);

  java.util.List<cn.iocoder.yudao.module.oa.api.payroll.dto.OaPayrollAttendanceDTO>
      getApprovedAttendance(
          java.util.Collection<Long> userIds,
          java.time.LocalDateTime from,
          java.time.LocalDateTime to);

  java.util.List<OaPayrollOvertimeDTO> getApprovedOvertimeList(
      Long userId, java.time.LocalDateTime from, java.time.LocalDateTime to);
}
