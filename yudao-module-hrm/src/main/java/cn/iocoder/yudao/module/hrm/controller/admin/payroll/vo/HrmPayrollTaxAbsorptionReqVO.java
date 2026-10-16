package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo;
import javax.validation.constraints.*;
import lombok.Data;
@Data public class HrmPayrollTaxAbsorptionReqVO {
 @NotNull private Long id;
 @NotNull private Long accountingFilingId;
 @NotBlank @Size(max=1000) private String evidence;
}
