package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.insurance;

import lombok.Data;
import javax.validation.constraints.*;

@Data
public class HrmPayrollInsuranceReviewReqVO {
    @NotNull @Min(1)
    private Long id;
    @NotNull @Min(1)
    private Integer revision;
    @NotBlank @Pattern(regexp = "confirm|retire")
    private String action;
    @NotBlank @Size(max = 5000)
    private String evidence;
}
