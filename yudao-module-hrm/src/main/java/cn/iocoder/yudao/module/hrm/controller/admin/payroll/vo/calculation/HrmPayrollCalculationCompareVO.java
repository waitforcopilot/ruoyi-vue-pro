package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.calculation;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.scheme.HrmPayrollSchemeCompareVO.Change;
import lombok.Data;
import java.util.List;
@Data
public class HrmPayrollCalculationCompareVO {
    private HrmPayrollCalculationRespVO left;
    private HrmPayrollCalculationRespVO right;
    private List<Change> changes;
}
