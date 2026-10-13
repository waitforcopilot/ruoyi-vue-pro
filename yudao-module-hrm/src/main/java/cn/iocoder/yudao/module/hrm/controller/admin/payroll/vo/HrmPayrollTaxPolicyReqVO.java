package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.*;
import lombok.Data;

@Data
public class HrmPayrollTaxPolicyReqVO {
  @NotNull private Long entityId;

  @NotBlank
  @Size(max = 100)
  private String requestKey;

  @NotNull
  @Min(2019)
  @Max(2099)
  private Integer year;

  private Long priorPolicyId;

  @NotBlank
  @Size(max = 100)
  private String name;

  @NotNull
  @DecimalMin("0")
  private BigDecimal monthlyBasicDeduction;

  @NotNull
  @Size(min = 1, max = 20)
  @Valid
  private List<Band> residentBands;

  @NotNull
  @Size(min = 1, max = 20)
  @Valid
  private List<Band> bonusBands;

  @NotNull private Boolean separateBonusAllowed;
  @NotNull private LocalDate separateBonusValidTo;

  @NotBlank
  @Size(max = 1000)
  private String officialReference;

  @NotBlank
  @Size(max = 1000)
  private String evidence;

  @Data
  public static class Band {
    private BigDecimal upper;

    @NotNull
    @DecimalMin("0")
    @DecimalMax("1")
    private BigDecimal rate;

    @NotNull
    @DecimalMin("0")
    private BigDecimal quickDeduction;
  }
}
