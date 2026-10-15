package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo;
import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;
import java.math.BigDecimal;
@Data public class HrmPayrollWageStructureRowVO {
 @ExcelProperty("主体编号") private Long entityId;
 @ExcelProperty("法人主体") private String entityName;
 @ExcelProperty("计薪年度") private Integer year;
 @ExcelProperty("计薪月份") private Integer month;
 @ExcelProperty("历史部门筛选") private Long deptId;
 @ExcelProperty("人员类别筛选") private String employeeCategory;
 @ExcelProperty("工资项编码") private Integer code;
 @ExcelProperty("工资项名称") private String name;
 @ExcelProperty("上期工资项名称") private String previousName;
 @ExcelProperty("本期金额（元）") private BigDecimal amount;
 @ExcelProperty("占应发比例%") private BigDecimal share;
 @ExcelProperty("上期金额（元）") private BigDecimal previous;
 @ExcelProperty("环比金额（元）") private BigDecimal delta;
 @ExcelProperty("环比变化率%") private BigDecimal deltaRate;
 @ExcelProperty("核算来源编号") private String runIds;
 @ExcelProperty("上期核算来源编号") private String previousRunIds;
 @ExcelProperty("对比状态") private String sourceStatus;
}
