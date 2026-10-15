package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo;
import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;
@Data public class HrmPayrollFinancialPreviewVO {
 @ExcelProperty("工作表") private String sheet;
 @ExcelProperty("文件行") private Integer row;
 @ExcelProperty("范围类型") private String dimension;
 @ExcelProperty("范围编号") private String dimensionKey;
 @ExcelProperty("校验结果") private String error;
}
