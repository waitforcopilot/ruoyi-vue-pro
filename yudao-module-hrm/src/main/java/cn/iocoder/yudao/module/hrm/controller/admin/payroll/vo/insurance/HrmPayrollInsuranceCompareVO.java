package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.insurance;

import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.scheme.HrmPayrollSchemeCompareVO.Change;
import lombok.Data;
import java.util.List;

@Data
public class HrmPayrollInsuranceCompareVO {
    private HrmPayrollInsuranceRespVO left;
    private HrmPayrollInsuranceRespVO right;
    private List<Change> changes;
}
