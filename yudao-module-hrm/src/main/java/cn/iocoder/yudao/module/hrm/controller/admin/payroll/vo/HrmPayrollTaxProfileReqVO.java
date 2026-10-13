package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import javax.validation.constraints.*;
import lombok.Data;

@Data
public class HrmPayrollTaxProfileReqVO {
  @NotNull private Long entityId;
  @NotNull private Long employeeId;

  @NotNull
  @Min(2019)
  @Max(2099)
  private Integer year;

  private Long priorProfileId;

  @NotBlank
  @Size(max = 100)
  private String requestKey;

  @NotBlank
  @Size(max = 100)
  private String taxpayerName;

  @NotBlank
  @Pattern(regexp = "ID_CARD|PASSPORT|OTHER")
  private String identifierType;

  @NotBlank
  @Pattern(regexp = "[A-Z]{2}")
  private String identifierCountry;

  @NotBlank
  @Pattern(regexp = "[0-9A-Za-z]{3,64}")
  @com.fasterxml.jackson.annotation.JsonProperty(
      access = com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY)
  private String identifier;

  @NotNull private Boolean firstSalaryThisYear;
  @NotNull private Boolean noPriorSalaryThisYear;
  @NotNull private Boolean separateBonusUsedElsewhere;

  @NotBlank
  @Pattern(regexp = "ZERO|DECLARE")
  private String openingMode;

  @NotNull private LocalDate openingAsOfDate;

  @NotNull
  @DecimalMin("0")
  private BigDecimal openingIncome;

  @NotNull
  @DecimalMin("0")
  private BigDecimal openingSocialDeduction;

  @NotNull
  @DecimalMin("0")
  private BigDecimal openingBasicDeduction;

  @NotNull
  @DecimalMin("0")
  private BigDecimal openingWithheld;

  @NotBlank
  @Size(max = 1000)
  private String officialReference;

  @NotBlank
  @Size(max = 1000)
  private String evidence;
}
