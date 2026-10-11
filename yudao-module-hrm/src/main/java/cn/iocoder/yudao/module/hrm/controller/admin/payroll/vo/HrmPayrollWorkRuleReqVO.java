package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import javax.validation.constraints.*;
import lombok.Data;

@Data
public class HrmPayrollWorkRuleReqVO {
  @NotNull private Long employeeId;

  @NotBlank
  @Pattern(regexp = "STANDARD|COMPREHENSIVE|IRREGULAR")
  private String systemType;

  @NotNull private LocalDate effectiveFrom;
  @NotNull private LocalDate effectiveTo;

  @NotBlank
  @Size(max = 1000)
  private String legalBasis;

  @NotNull
  @Min(1)
  @Max(527040)
  private Integer cycleMinutes;

  @NotNull
  @Min(1)
  @Max(12)
  private Integer cycleMonths;

  @NotNull
  @Min(1)
  @Max(1095)
  private Integer timeOffExpiryDays;

  @NotNull
  @Min(1)
  @Max(2160)
  private Integer limitMinutes;

  @NotNull
  @Digits(integer = 10, fraction = 4)
  @DecimalMin("0.01")
  private BigDecimal hourlyBase;

  @NotNull
  @DecimalMax("100")
  @Digits(integer = 3, fraction = 4)
  @DecimalMin("0")
  private BigDecimal workdayRate;

  @NotNull
  @DecimalMax("100")
  @Digits(integer = 3, fraction = 4)
  @DecimalMin("0")
  private BigDecimal restdayRate;

  @NotNull
  @DecimalMax("100")
  @Digits(integer = 3, fraction = 4)
  @DecimalMin("0")
  private BigDecimal holidayRate;
}
