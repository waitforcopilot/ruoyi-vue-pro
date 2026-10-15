package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo;
import javax.validation.constraints.*;
import lombok.Data;
@Data public class HrmPayrollFinancialPlanReqVO {
 @NotNull private Long entityId;
 private Long priorPlanId;
 @NotNull @Min(2019) @Max(2099) private Integer year;
 @NotNull @Min(1) @Max(12) private Integer month;
 @NotBlank @Pattern(regexp="ENTITY|DEPARTMENT|COST_CENTER|PROJECT|CATEGORY") private String dimension;
 @NotBlank @Size(max=100) private String dimensionKey;
 @DecimalMin("0") @Digits(integer=12,fraction=2) private java.math.BigDecimal budget;
 @DecimalMin("0") @Digits(integer=12,fraction=2) private java.math.BigDecimal revenue;
 @DecimalMin("0") @Digits(integer=8,fraction=4) private java.math.BigDecimal headcount;
 @NotBlank @Size(max=1000) private String evidence;
 @Size(max=200) private String sourceReference;
 @NotBlank @Size(max=100) private String requestKey;
}
