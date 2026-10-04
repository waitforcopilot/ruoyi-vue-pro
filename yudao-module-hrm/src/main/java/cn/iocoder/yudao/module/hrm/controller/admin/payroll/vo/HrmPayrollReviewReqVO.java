package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo;
import lombok.Data;
import javax.validation.constraints.*;
@Data
public class HrmPayrollReviewReqVO {
    @NotNull private Long id;
    @NotNull @Min(1) private Integer version;
    @NotNull @Min(0) @Max(2) private Integer status;
    @NotBlank @Size(max = 2000) private String evidence;
}
