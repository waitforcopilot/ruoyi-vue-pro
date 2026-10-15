package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo;
import javax.validation.Valid;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Data;
@Data public class HrmPayrollCostRuleReqVO {
 @NotNull private Long entityId;
 @NotNull private Long employeeId;
 private Long priorRuleId;
 @NotNull private LocalDate effectiveFrom;
 @NotNull private LocalDate effectiveTo;
 @NotNull @DecimalMin("0") @Digits(integer=12,fraction=2) private BigDecimal employerCost;
 @Valid @NotEmpty @Size(max=100) private List<Part> parts;
 @NotBlank @Size(max=1000) private String evidence;
 @NotBlank @Size(max=100) private String requestKey;
 @Data public static class Part {
  @NotNull private Long deptId;
  @Size(max=100) private String costCenter;
  @Size(max=100) private String projectCode;
  @NotNull @DecimalMin(value="0",inclusive=false) @DecimalMax("100") @Digits(integer=3,fraction=4) private BigDecimal percent;
 }
}
