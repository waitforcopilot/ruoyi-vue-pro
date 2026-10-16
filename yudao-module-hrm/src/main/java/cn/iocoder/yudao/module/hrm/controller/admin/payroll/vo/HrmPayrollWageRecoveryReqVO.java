package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo;
import javax.validation.constraints.*;
import lombok.Data;
@Data public class HrmPayrollWageRecoveryReqVO {
 @NotNull private Long paymentId;
 private Long priorRetroId;
 @NotNull @DecimalMin("0") @Digits(integer=12,fraction=2) private java.math.BigDecimal correctedIncome;
 @NotEmpty @Size(max=100) private java.util.List<@NotNull Long> taxCorrectionIds;
 @NotBlank @Size(max=1000) private String employeeAcknowledgement;
 @NotBlank @Size(max=1000) private String evidence;
 @NotBlank @Size(max=100) private String requestKey;
}
