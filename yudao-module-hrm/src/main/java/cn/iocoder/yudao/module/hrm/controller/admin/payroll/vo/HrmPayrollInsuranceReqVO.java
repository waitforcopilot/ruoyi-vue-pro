package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import javax.validation.Valid;
import javax.validation.constraints.*;
import lombok.Data;

@Data
public class HrmPayrollInsuranceReqVO {
  @NotNull private Long entityId;

  @NotBlank
  @Pattern(regexp = "[A-Za-z0-9_-]{1,64}")
  private String code;

  @NotBlank
  @Size(max = 128)
  private String city;

  @NotNull private LocalDate effectiveFrom;
  @NotNull private LocalDate effectiveTo;
  private Long replacesId;

  @NotBlank
  @Size(max = 1000)
  private String evidence;

  @NotEmpty
  @Size(max = 30)
  @Valid
  private List<Project> projects;

  @Data
  public static class Project {
    @NotBlank
    @Pattern(regexp = "[A-Z0-9_]{1,32}")
    private String code;

    @NotBlank
    @Size(max = 64)
    private String name;

    @NotNull
    @Min(1)
    @Max(11)
    private Integer type;

    @NotNull
    @DecimalMin("0")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal minimumBase;

    @NotNull
    @DecimalMin("0")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal maximumBase;

    @NotNull
    @DecimalMin("0")
    @DecimalMax("100")
    @Digits(integer = 3, fraction = 6)
    private BigDecimal personalRate;

    @NotNull
    @DecimalMin("0")
    @DecimalMax("100")
    @Digits(integer = 3, fraction = 6)
    private BigDecimal corporateRate;

    @DecimalMin("0")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal personalFixed;

    @DecimalMin("0")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal corporateFixed;
  }

  @Data
  public static class Base {
    @NotNull private Long entityId;
    @NotNull private Long employeeId;
    private Long policyId;

    @NotBlank
    @Pattern(regexp = "INSURED|EXEMPT")
    private String coverage;

    @Min(2000)
    @Max(2100)
    private Integer incomeYear;

    @NotNull private LocalDate effectiveFrom;
    @NotNull private LocalDate effectiveTo;
    private Long replacesId;
    private Map<String, BigDecimal> declaredBases;

    @NotBlank
    @Size(max = 1000)
    private String evidence;
  }
}
