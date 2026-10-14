package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo;
import java.math.BigDecimal;
import java.time.LocalDate;
import javax.validation.constraints.*;
import lombok.Data;
@Data public class HrmPayrollTaxSettlementConfirmReqVO {
 @NotNull private Long id;
 @NotNull private LocalDate valueDate;
 @NotNull @DecimalMin(value="0",inclusive=false) @Digits(integer=12,fraction=2) private BigDecimal amount;
 @NotBlank @Size(max=100) private String bankReference;
 @NotBlank @Size(max=1000) private String evidence;
}
