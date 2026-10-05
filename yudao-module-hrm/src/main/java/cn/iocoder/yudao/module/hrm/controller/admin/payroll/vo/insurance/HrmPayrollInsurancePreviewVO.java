package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.insurance;

import lombok.Data;

@Data
public class HrmPayrollInsurancePreviewVO {
    private HrmPayrollInsuranceRespVO policy;
    private String baseAmount;
    private String corporateRawAmount;
    private String personalRawAmount;
    private String corporateAmount;
    private String personalAmount;
    private String corporateExpression;
    private String personalExpression;
    private String corporateSteps;
    private String personalSteps;
    private String explanation;
}
