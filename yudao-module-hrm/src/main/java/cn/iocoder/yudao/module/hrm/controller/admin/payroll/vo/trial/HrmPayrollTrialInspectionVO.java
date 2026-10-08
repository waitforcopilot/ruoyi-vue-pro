package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.trial;

import lombok.Data;

/** Read-only comparison of current sources with a saved immutable version. */
@Data
public class HrmPayrollTrialInspectionVO {
    private HrmPayrollTrialRespVO batch;
    private HrmPayrollTrialCheckVO check;
    private HrmPayrollTrialRunRespVO run;
    private String availability;
}
