package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo;

import javax.validation.constraints.*;
import lombok.Data;

@Data
public class HrmPayrollWorkVarianceReqVO {
  @NotNull private Long entityId;
  @NotNull private Long employeeId;
  @NotNull private java.time.LocalDate workDate;

  @NotBlank
  @Size(max = 100)
  private String requestKey;

  @NotBlank
  @Size(max = 1000)
  private String reason;

  @NotNull private Long ownerId;
  @NotNull private Long reviewerId;
  @NotNull private java.time.LocalDateTime dueTime;
}
