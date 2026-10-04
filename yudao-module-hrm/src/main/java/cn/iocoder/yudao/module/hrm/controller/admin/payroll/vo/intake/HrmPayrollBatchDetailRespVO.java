package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.intake;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class HrmPayrollBatchDetailRespVO extends HrmPayrollBatchRespVO {
    private HrmPayrollContractRespVO contractSnapshot;
    private HrmPayrollPreviewResultVO result;
}
