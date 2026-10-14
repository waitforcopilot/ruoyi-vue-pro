package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo;

import javax.validation.constraints.*;
import lombok.Data;

@Data
public class HrmPayrollBonusReqVO {
  @NotNull private Long batchId;
  @NotNull private Long employeeId;
  private Long priorBonusId;

  @NotNull
  @DecimalMin("0")
  @Digits(integer = 12, fraction = 2)
  private java.math.BigDecimal amount;

  @NotBlank
  @Pattern(regexp = "NONE|MERGED|SEPARATE")
  private String mode;

  @NotBlank
  @Size(max = 1000)
  private String evidence;

  @NotBlank
  @Size(max = 100)
  private String requestKey;
}
