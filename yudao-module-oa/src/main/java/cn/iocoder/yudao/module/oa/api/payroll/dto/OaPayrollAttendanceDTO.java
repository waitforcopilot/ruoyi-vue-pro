package cn.iocoder.yudao.module.oa.api.payroll.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class OaPayrollAttendanceDTO {
  private Long id;
  private Long applicantUserId;
  private String type;
  private LocalDateTime startTime;
  private LocalDateTime endTime;
  private String processInstanceId;
}
