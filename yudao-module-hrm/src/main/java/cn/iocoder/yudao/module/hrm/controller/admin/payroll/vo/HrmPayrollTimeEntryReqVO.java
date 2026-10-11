package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo;

import java.time.LocalDateTime;
import javax.validation.constraints.*;
import lombok.Data;

@Data
public class HrmPayrollTimeEntryReqVO {
  @NotNull private Long employeeId;

  @NotBlank
  @Pattern(regexp = "OA")
  private String sourceSystem;

  @NotBlank
  @Pattern(regexp = "[0-9]{1,18}")
  private String sourceRecordId;

  @NotNull private Long clockInId;
  @NotNull private Long clockOutId;

  @NotBlank
  @Size(max = 1000)
  private String evidence;

  @NotNull private LocalDateTime startTime;
  @NotNull private LocalDateTime endTime;
  private LocalDateTime clockStart;
  private LocalDateTime clockEnd;

  @NotNull
  @Min(0)
  private Integer breakMinutes;

  @NotBlank
  @Pattern(regexp = "WORKDAY|RESTDAY|HOLIDAY")
  private String dayType;

  @NotBlank
  @Pattern(regexp = "PAY|TIME_OFF")
  private String compensation;
}
