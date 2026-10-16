package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo;
import javax.validation.constraints.*;
import lombok.Data;
@Data public class HrmPayrollInsuranceAdjustmentActionReqVO {
 @NotNull private Long id;
 private Boolean approved;
 @Size(max=1000) private String reason,evidence;
 @Size(max=100) private String reference;
 private java.time.LocalDate valueDate;
 @DecimalMin("0") @Digits(integer=12,fraction=2) private java.math.BigDecimal amount;
}
