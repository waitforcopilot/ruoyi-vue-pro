package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.intake;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;
import javax.validation.constraints.*;

@Data
@EqualsAndHashCode(callSuper = true)
public class HrmPayrollContractPageReqVO extends PageParam {
    private Long sourceId;
    @Min(0) @Max(2) private Integer status;
    @Size(max = 200) private String search;
}
