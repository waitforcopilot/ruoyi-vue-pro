package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo;

import javax.validation.constraints.*;
import lombok.Data;

@Data
public class HrmPayrollTaxFilingReqVO {
  @NotNull private Long entityId;

  @NotNull
  @Min(2019)
  @Max(2099)
  private Integer year;

  @NotNull
  @Min(1)
  @Max(12)
  private Integer month;

  @NotNull private Boolean zeroDeclaration;
  private Long priorFilingId;

  @NotBlank
  @Size(max = 100)
  private String requestKey;

  @NotBlank
  @Size(max = 1000)
  private String evidence;
}
