package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.calculation;
import lombok.Data;
import javax.validation.constraints.*;
@Data
public class HrmPayrollCalculationReviewReqVO {
    @NotNull @Min(1) private Long id;
    @NotNull @Min(1) private Integer revision;
    @NotBlank @Pattern(regexp = "confirm|retire") private String action;
    @NotBlank @Size(max = 4000) private String evidence;
}
