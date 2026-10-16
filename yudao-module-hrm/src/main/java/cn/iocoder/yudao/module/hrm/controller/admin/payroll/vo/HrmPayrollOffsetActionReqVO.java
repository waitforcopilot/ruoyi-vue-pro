package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo;
import javax.validation.constraints.*;
import lombok.Data;
@Data public class HrmPayrollOffsetActionReqVO { @NotNull private Long id; private Boolean approved; @NotBlank @Size(max=1000) private String reason; }
