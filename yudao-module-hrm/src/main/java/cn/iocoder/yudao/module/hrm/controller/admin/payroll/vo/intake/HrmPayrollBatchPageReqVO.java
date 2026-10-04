package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.intake;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;
import javax.validation.constraints.*;

@Data
@EqualsAndHashCode(callSuper = true)
public class HrmPayrollBatchPageReqVO extends PageParam {
    private Long contractId;
    @Min(0) @Max(1) private Integer status;
}
