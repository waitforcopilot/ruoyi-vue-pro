package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo;

import javax.validation.constraints.*;
import lombok.Data;

@Data
public class HrmPayrollTaxReceiptReqVO {
  @NotNull private Long filingId;

  @NotBlank
  @Pattern(regexp = "PAYMENT|REFUND")
  private String kind;

  private Long originalReceiptId;

  @NotNull
  @DecimalMin(value = "0", inclusive = false)
  @Digits(integer = 16, fraction = 2)
  private java.math.BigDecimal amount;

  @NotNull private java.time.LocalDate paymentDate;

  @NotBlank
  @Size(max = 100)
  private String reference;

  @NotBlank
  @Size(max = 100)
  private String requestKey;

  @NotBlank
  @Size(max = 1000)
  private String evidence;
}
