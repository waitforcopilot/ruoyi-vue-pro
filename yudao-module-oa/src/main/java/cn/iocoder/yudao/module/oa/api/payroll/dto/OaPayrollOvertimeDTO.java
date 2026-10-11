package cn.iocoder.yudao.module.oa.api.payroll.dto;

import java.time.LocalDateTime;
import lombok.Data;

/** Minimal approved source information consumed by payroll; no applicant contact details. */
@Data
public class OaPayrollOvertimeDTO {
  private Long id;
  private Long applicantUserId;
  private LocalDateTime startTime;
  private LocalDateTime endTime;
  private String processInstanceId;
}
