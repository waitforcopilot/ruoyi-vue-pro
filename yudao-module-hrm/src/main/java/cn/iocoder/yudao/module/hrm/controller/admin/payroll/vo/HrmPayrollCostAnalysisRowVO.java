package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo;
import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;
import java.math.BigDecimal;
@Data public class HrmPayrollCostAnalysisRowVO {
 @ExcelProperty("主体编号") private Long entityId;
 @ExcelProperty("法人主体") private String entityName;
 @ExcelProperty("计薪年度") private Integer year;
 @ExcelProperty("计薪月份") private Integer month;
 @ExcelProperty("批准成本报告编号") private String costReportIds;
 @ExcelProperty("范围类型") private String dimension;
 @ExcelProperty("范围编号") private String dimensionKey;
 @ExcelProperty("范围名称") private String name;
 @ExcelProperty("实际成本（元）") private BigDecimal actualCost;
 @ExcelProperty("实有人数") private Integer employees;
 @ExcelProperty("批准编制（人）") private BigDecimal headcount;
 @ExcelProperty("批准预算（元）") private BigDecimal budget;
 @ExcelProperty("营收（元）") private BigDecimal revenue;
 @ExcelProperty("预算差额（元）") private BigDecimal budgetVariance;
 @ExcelProperty("预算差异率%") private BigDecimal budgetVarianceRate;
 @ExcelProperty("成本营收率%") private BigDecimal costRevenueRate;
 @ExcelProperty("人均成本（元/人）") private BigDecimal costPerEmployee;
 @ExcelProperty("编制单位成本（元/人）") private BigDecimal costPerPosition;
 @ExcelProperty("人员编制差额") private BigDecimal staffingVariance;
 @ExcelProperty("资料状态") private String sourceStatus;
 @ExcelProperty("计划版本编号") private Long planId;
 @ExcelProperty("计划版本") private Integer planVersion;
}
