package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo;
import java.math.BigDecimal;
import javax.validation.constraints.*;
import lombok.Data;
@Data
public class HrmPayrollTaxCorrectionReqVO {
  @NotNull private Long paymentId;
  private Long priorCorrectionId;
  @DecimalMin("0") @Digits(integer=12,fraction=2) private BigDecimal income;
  @DecimalMin("0") @Digits(integer=12,fraction=2) private BigDecimal socialDeduction;
  private Boolean openingAlreadyIncludesCorrection;
  @NotNull @DecimalMin("0") @Digits(integer=12,fraction=2) private BigDecimal basicDeduction;
  @NotBlank @Size(max=100) private String officialReference;
  @NotBlank @Size(max=1000) private String evidence;
  @NotBlank @Size(max=1000) private String reason;
  @NotBlank @Size(max=100) private String requestKey;
}
