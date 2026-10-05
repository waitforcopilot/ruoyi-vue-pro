package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.eligibility;
import lombok.Data;
@Data
public class HrmPayrollEligibilityLookupVO {
    private Boolean matched;
    private String issueCode;
    private String explanation;
    private HrmPayrollEligibilityRespVO eligibility;
    private Boolean personChanged;
}
