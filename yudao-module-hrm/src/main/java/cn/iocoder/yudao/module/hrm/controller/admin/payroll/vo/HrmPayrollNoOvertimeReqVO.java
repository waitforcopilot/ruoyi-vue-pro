package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo;

import javax.validation.constraints.*;
import lombok.Data;

@Data
public class HrmPayrollNoOvertimeReqVO {
  @NotNull private Long entityId;
  @NotNull private Long employeeId;

  @NotNull
  @Min(1)
  private Long sourceApplyId;

  @NotNull private java.time.LocalDate workDate;

  @NotBlank
  @Size(max = 100)
  private String requestKey;

  @NotBlank
  @Size(max = 1000)
  private String reason;

  @NotBlank
  @Size(max = 1000)
  private String evidence;
}
