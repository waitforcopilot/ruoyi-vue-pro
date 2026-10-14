package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import javax.validation.constraints.*;
import lombok.Data;

@Data
public class HrmPayrollTaxDeductionReqVO {
  @NotNull private Long entityId;
  @NotNull private Long employeeId;

  @NotNull
  @Min(2019)
  @Max(2099)
  private Integer year;

  @NotNull
  @Min(1)
  @Max(12)
  private Integer month;

  private Long priorDeclarationId;

  @NotBlank
  @Size(max = 100)
  private String requestKey;

  @NotNull private LocalDate sourceDate;

  @NotBlank
  @Pattern(regexp = "ZERO|DECLARE")
  private String mode;

  @NotNull private Map<String, BigDecimal> values;

  @NotBlank
  @Size(max = 1000)
  private String officialReference;

  @NotBlank
  @Size(max = 1000)
  private String evidence;
}
