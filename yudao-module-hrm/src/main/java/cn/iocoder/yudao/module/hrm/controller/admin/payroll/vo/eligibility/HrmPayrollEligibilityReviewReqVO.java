package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.eligibility;
import lombok.Data;
import javax.validation.constraints.*;
@Data
public class HrmPayrollEligibilityReviewReqVO {
    @NotNull @Min(1) private Long id;
    @NotNull @Min(1) private Integer revision;
    @NotBlank @Pattern(regexp = "confirm|retire") private String action;
    @NotBlank @Size(max = 4000) private String evidence;
}
