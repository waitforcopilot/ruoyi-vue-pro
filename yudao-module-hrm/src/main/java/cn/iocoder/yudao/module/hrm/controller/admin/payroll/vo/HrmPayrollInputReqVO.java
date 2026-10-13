package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Map;
import javax.validation.constraints.*;
@Data
public class HrmPayrollInputReqVO {
 @NotNull private Long batchId;
 @NotNull private Long employeeId;
 @NotBlank @Pattern(regexp="ATTENDANCE|OVERTIME|DEDUCTIONS|CUMULATIVE_TAX") private String kind;
 @NotBlank @Pattern(regexp="SYNC|ZERO|IMPORT") private String mode;
 @NotBlank @Size(max=1000) private String evidence;
 private Map<String,BigDecimal> values;
}
