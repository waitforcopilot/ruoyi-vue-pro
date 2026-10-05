package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.eligibility;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;
import javax.validation.constraints.*;
@Data @EqualsAndHashCode(callSuper = true)
public class HrmPayrollEligibilityPageReqVO extends PageParam {
    @Size(max = 64) private String entityCode;
    @Min(1) private Long employeeId;
    @Size(max = 160) private String search;
    @Min(0) @Max(2) private Integer status;
    @Pattern(regexp = "INCLUDED|EXCLUDED") private String qualification;
}
