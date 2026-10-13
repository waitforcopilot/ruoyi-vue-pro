package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo;

import java.math.BigDecimal;
import javax.validation.constraints.*;
import lombok.Data;

@Data
public class HrmPayrollTaxPolicyTrialReqVO {
  @NotNull private Long policyId;

  @NotNull
  @Min(0)
  @Max(12)
  private Integer basicMonths;

  @NotNull
  @DecimalMin("0")
  private BigDecimal salaryIncome;

  @NotNull
  @DecimalMin("0")
  private BigDecimal socialDeduction;

  @NotNull
  @DecimalMin("0")
  private BigDecimal additionalDeduction;

  @NotNull
  @DecimalMin("0")
  private BigDecimal otherDeduction;

  @NotNull
  @DecimalMin("0")
  private BigDecimal alreadyWithheld;

  @NotNull
  @DecimalMin("0")
  private BigDecimal bonusAmount;

  @NotBlank
  @Pattern(regexp = "NONE|MERGED|SEPARATE")
  private String bonusMode;

  @NotNull private Boolean separateBonusAlreadyUsed;
}
