package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo;
import java.math.BigDecimal;
import javax.validation.constraints.*;
import lombok.Data;
@Data public class HrmPayrollTaxSettlementReqVO {
 @NotNull private Long filingId;
 @NotNull private Long employeeId;
 @NotBlank @Pattern(regexp="RESIDENT_WAGE|SEPARATE_BONUS") private String mode;
 @NotBlank @Pattern(regexp="REFUND|RECOVERY") private String kind;
 @NotNull @DecimalMin(value="0",inclusive=false) @Digits(integer=12,fraction=2) private BigDecimal amount;
 @NotBlank @Size(max=1000) private String employeeAcknowledgement;
 @NotBlank @Size(max=1000) private String evidence;
 @NotBlank @Size(max=1000) private String reason;
 @NotBlank @Size(max=100) private String requestKey;
}
