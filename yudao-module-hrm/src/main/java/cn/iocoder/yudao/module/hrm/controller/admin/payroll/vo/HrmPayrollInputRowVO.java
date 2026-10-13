package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo;
import lombok.Data;
import cn.idev.excel.annotation.ExcelProperty;
@Data
public class HrmPayrollInputRowVO {
 @ExcelProperty("工号") private String jobNumber;
 @ExcelProperty("主体编码") private String entityCode;
 @ExcelProperty("期间开始") private String periodFrom;
 @ExcelProperty("期间截止") private String periodTo;
 @ExcelProperty("数据类型") private String kind;
 @ExcelProperty("处理方式") private String mode;
 @ExcelProperty("字段编码") private String field;
 @ExcelProperty("数值") private String value;
 @ExcelProperty("来源依据") private String evidence;
}
