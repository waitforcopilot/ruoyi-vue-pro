package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo;

import javax.validation.constraints.*;
import lombok.Data;

@Data
public class HrmPayrollAttendanceIssueReqVO {
  @NotNull private Long employeeId;
  @NotNull private java.time.LocalDate attendanceDate;

  @NotBlank
  @Pattern(regexp = "MISSCARD|LATE|EARLY|ABSENT")
  private String type;

  @Min(1)
  @Max(2)
  private Integer clockType;

  @NotNull private Long ownerId;
  @NotNull private Long reviewerId;

  @NotBlank
  @Size(max = 100)
  private String requestKey;

  @NotNull private java.time.LocalDateTime dueTime;

  @Data
  public static class Submit {
    @NotNull private Long id;

    @NotBlank
    @Size(max = 1000)
    private String evidence;

    private java.time.LocalDateTime correctionTime;
  }
}
