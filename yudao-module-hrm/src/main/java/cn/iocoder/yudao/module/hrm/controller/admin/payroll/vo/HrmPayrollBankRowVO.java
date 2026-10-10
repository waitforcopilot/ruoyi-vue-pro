package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo;
import lombok.Data;
import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import javax.validation.constraints.*;
import java.math.BigDecimal;
@Data @ExcelIgnoreUnannotated
public class HrmPayrollBankRowVO {
    @NotNull @ExcelProperty("代发明细编号") private Long id;
    @NotNull @Min(1) @ExcelProperty("发放次数") private Integer attempt;
    @ExcelProperty("员工姓名") private String employeeName;
    @ExcelProperty("银行") private String bankName;
    @NotBlank @ExcelProperty("银行账户") private String bankAccount;
    @NotNull @DecimalMin("0.00") @Digits(integer=12,fraction=2) @ExcelProperty("金额") private BigDecimal amount;
    @Pattern(regexp="SUCCESS|FAILED") @NotBlank @ExcelProperty("回盘状态") private String status;
    @Size(max=1000) @ExcelProperty("失败原因") private String failureReason;
}
