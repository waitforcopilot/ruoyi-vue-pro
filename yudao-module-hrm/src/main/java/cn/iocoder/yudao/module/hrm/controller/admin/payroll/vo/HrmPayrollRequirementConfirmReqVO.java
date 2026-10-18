package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo;
import lombok.Data;
import javax.validation.constraints.*;
@Data public class HrmPayrollRequirementConfirmReqVO {
 @NotNull @Positive private Long id;
 @NotNull @Min(1) private Integer version;
}
