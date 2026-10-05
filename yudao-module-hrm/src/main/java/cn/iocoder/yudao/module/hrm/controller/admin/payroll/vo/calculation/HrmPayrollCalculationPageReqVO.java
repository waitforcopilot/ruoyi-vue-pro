package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.calculation;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;
import javax.validation.constraints.*;
@Data @EqualsAndHashCode(callSuper = true)
public class HrmPayrollCalculationPageReqVO extends PageParam {
    @Size(max = 160) private String search;
    @Size(max = 64) private String code;
    @Size(max = 64) private String scopeCode;
    @Min(0) @Max(2) private Integer status;
}
