package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo;

import java.time.LocalDate;
import java.util.Set;
import javax.validation.constraints.*;
import lombok.Data;

@Data
public class HrmPayrollEntityReqVO {
  @NotBlank
  @Pattern(regexp = "[A-Za-z0-9_-]{1,64}")
  private String code;

  @NotBlank
  @Size(max = 128)
  private String name;

  @NotBlank
  @Pattern(regexp = "[A-Z0-9]{15,20}")
  private String taxRegistrationNumber;

  @NotBlank
  @Size(max = 1000)
  private String evidence;

  @Data
  public static class Grant {
    @NotNull private Long entityId;
    @NotNull private Long userId;
    @NotEmpty private Set<String> actions;
    @NotNull private LocalDate effectiveFrom;
    @NotNull private LocalDate effectiveTo;

    @NotBlank
    @Size(max = 1000)
    private String reason;
  }

  @Data
  public static class Membership {
    @NotNull private Long entityId;
    private Long replacesId;
    @NotNull private Long employeeId;
    @NotNull private Long deptId;

    @NotBlank
    @Size(max = 64)
    private String employeeCategory;

    @NotBlank
    @Size(max = 128)
    private String workCity;

    @NotNull private LocalDate effectiveFrom;
    @NotNull private LocalDate effectiveTo;

    @NotBlank
    @Size(max = 1000)
    private String evidence;
  }
}
