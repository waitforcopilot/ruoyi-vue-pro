package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo;
import javax.validation.constraints.*;
import lombok.Data;
@Data public class HrmPayrollWageRecoveryActionReqVO {
 @NotNull private Long id;
 private Boolean approved;
 @Size(max=1000) private String reason;
 @Size(max=1000) private String evidence;
 @Size(max=100) private String reference;
 private java.time.LocalDate valueDate;
 @DecimalMin("0.01") @Digits(integer=12,fraction=2) private java.math.BigDecimal amount;
}
