package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo;
import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;
import java.math.BigDecimal;
@Data public class HrmPayrollGradeImportRowVO {
    @ExcelProperty("类型 GRADE/POST") private String type;
    @ExcelProperty("薪级编码") private String gradeCode;
    @ExcelProperty("薪档编码") private String stepCode;
    @ExcelProperty("最低金额") private BigDecimal minimum;
    @ExcelProperty("标准金额") private BigDecimal standard;
    @ExcelProperty("最高金额") private BigDecimal maximum;
    @ExcelProperty("岗位名称") private String postName;
    @ExcelProperty("岗位系数") private BigDecimal coefficient;
}
