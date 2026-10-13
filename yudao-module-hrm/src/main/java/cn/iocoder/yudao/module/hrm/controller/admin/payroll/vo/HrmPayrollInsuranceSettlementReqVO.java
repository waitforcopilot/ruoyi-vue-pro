package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo;

import java.math.BigDecimal;
import javax.validation.constraints.*;
import lombok.Data;

@Data
public class HrmPayrollInsuranceSettlementReqVO {
  @NotNull private Long lineId;

  @NotBlank
  @Pattern(regexp = "PAY|REFUND|RECORD")
  private String mode;

  @NotNull
  @DecimalMin("0")
  @Digits(integer = 10, fraction = 2)
  private BigDecimal personal;

  @NotNull
  @DecimalMin("0")
  @Digits(integer = 10, fraction = 2)
  private BigDecimal corporate;

  @NotBlank
  @Size(max = 128)
  private String reference;

  @NotBlank
  @Pattern(regexp = "[a-f0-9]{64}")
  private String digest;

  @NotBlank
  @Size(max = 1000)
  private String evidence;
}
