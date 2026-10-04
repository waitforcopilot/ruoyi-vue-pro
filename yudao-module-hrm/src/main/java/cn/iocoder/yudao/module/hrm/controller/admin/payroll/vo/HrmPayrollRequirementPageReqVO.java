package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;
import javax.validation.constraints.Min;
import javax.validation.constraints.Max;
@Data
@EqualsAndHashCode(callSuper = true)
public class HrmPayrollRequirementPageReqVO extends PageParam {
    private String moduleCode;
    private String search;
    @javax.validation.constraints.Size(max = 64)
    @javax.validation.constraints.Pattern(regexp = "|DS-[A-Z0-9-]+")
    private String sourceCode;
    @Min(0) @Max(2) private Integer status;
    @Min(1) @Max(3) private Integer priority;
    @Min(0) @Max(2) private Integer scopeDecision;
}
