package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo;
import lombok.Data;
import javax.validation.Valid;
import javax.validation.constraints.*;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;
@Data public class HrmPayrollGradePolicyReqVO {
    @NotBlank @Pattern(regexp="[A-Za-z0-9_-]{1,64}") private String code;
    @NotBlank @Size(max=100) private String name;
    @NotNull private Integer optionCode;
    @NotNull private LocalDate effectiveFrom;
    @NotNull private LocalDate effectiveTo;
    @NotBlank @Size(max=1000) private String evidence;
    @NotEmpty @Size(max=1000) @Valid private List<Grade> grades;
    @NotNull @Size(max=1000) @Valid private List<PostCoefficient> postCoefficients;
    @Data public static class Grade {
        @NotBlank @Pattern(regexp="[A-Za-z0-9_-]{1,64}") private String gradeCode;
        @NotBlank @Pattern(regexp="[A-Za-z0-9_-]{1,64}") private String stepCode;
        @NotNull @DecimalMin("0.01") @Digits(integer=10,fraction=2) private BigDecimal minimum;
        @NotNull @DecimalMin("0.01") @Digits(integer=10,fraction=2) private BigDecimal standard;
        @NotNull @DecimalMin("0.01") @Digits(integer=10,fraction=2) private BigDecimal maximum;
    }
    @Data public static class PostCoefficient {
        @NotBlank @Size(max=128) private String postName;
        @NotNull @DecimalMin("0.01") @DecimalMax("100") @Digits(integer=3,fraction=4) private BigDecimal coefficient;
    }
    @Data public static class Apply {
        @NotNull private Long policyId;
        @NotNull private Long employeeId;
        @NotBlank private String gradeCode;
        @NotBlank private String stepCode;
        @NotNull private LocalDate effectiveDate;
        @DecimalMin("0.01") @Digits(integer=10,fraction=2) private BigDecimal baseAmount;
        @NotNull @DecimalMin("0.8") @DecimalMax("1") private BigDecimal probationFactor=BigDecimal.ONE;
    }
}
