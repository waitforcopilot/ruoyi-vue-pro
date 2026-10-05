package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.insurance;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;
import javax.validation.constraints.*;

@Data @EqualsAndHashCode(callSuper = true)
public class HrmPayrollInsurancePageReqVO extends PageParam {
    private Integer cityAreaId;
    @Size(max = 64)
    private String scopeCode;
    private Integer projectType;
    @Size(max = 160)
    private String search;
    @Min(0) @Max(2)
    private Integer status;
}
