package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo;
import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;
import java.math.BigDecimal;
@Data public class HrmPayrollCostRowVO {
 @ExcelProperty("员工编号") private Long employeeId;
 @ExcelProperty("员工姓名") private String employeeName;
 @ExcelProperty("人员类别") private String employeeCategory;
 @ExcelProperty("历史部门编号") private Long sourceDeptId;
 @ExcelProperty("分摊部门编号") private Long deptId;
 @ExcelProperty("分摊部门名称") private String deptName;
 @ExcelProperty("成本中心") private String costCenter;
 @ExcelProperty("项目") private String projectCode;
 @ExcelProperty("分摊比例%") private BigDecimal percent;
 @ExcelProperty("工资应发分摊") private BigDecimal gross;
 @ExcelProperty("单位社保分摊") private BigDecimal insurance;
 @ExcelProperty("单位公积金分摊") private BigDecimal fund;
 @ExcelProperty("配置单位成本分摊") private BigDecimal employerCost;
 @ExcelProperty("总成本") private BigDecimal total;
 @ExcelProperty("分摊规则编号") private Long ruleId;
}
