package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo;
import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;
@Data public class HrmPayrollFinancialImportRowVO {
 @cn.idev.excel.annotation.ExcelIgnore private Integer sourceRow;
 @cn.idev.excel.annotation.ExcelIgnore private String sourceSheet;
 @ExcelProperty("年度") private String year;
 @ExcelProperty("月份") private String month;
 @ExcelProperty("范围类型") private String dimension;
 @ExcelProperty("范围编号") private String dimensionKey;
 @ExcelProperty("接续批准版本编号") private String priorPlanId;
 @ExcelProperty("工资成本预算（元）") private String budget;
 @ExcelProperty("营收（元）") private String revenue;
 @ExcelProperty("编制人数（人）") private String headcount;
 @ExcelProperty("核实依据") private String evidence;
}
