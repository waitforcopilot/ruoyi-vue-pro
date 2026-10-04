package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.intake;

import lombok.Data;
import javax.validation.constraints.*;

@Data
public class HrmPayrollContractReviewReqVO {
    @NotNull private Long id;
    @NotNull @Min(1) private Integer revision;
    @NotBlank @Pattern(regexp = "confirm|retire") private String action;
    @NotBlank @Size(max = 2000) private String evidence;
}
