package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.insurance;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import javax.validation.constraints.*;
import java.math.BigDecimal;

/** Explicit policy parameters. Null denotes missing information, never zero. */
@Data
public class HrmPayrollInsuranceConfigVO {
    @DecimalMin("0") @Digits(integer = 10, fraction = 2)
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal lowerBase;
    @DecimalMin("0") @Digits(integer = 10, fraction = 2)
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal upperBase;
    @Pattern(regexp = "YUAN_MONTH|YUAN_YEAR|YUAN_DECLARED")
    private String baseUnit;
    @Pattern(regexp = "RATE|FIXED|RATE_PLUS_FIXED")
    private String corporateMode;
    @Pattern(regexp = "RATE|FIXED|RATE_PLUS_FIXED")
    private String personalMode;
    @DecimalMin("0") @DecimalMax("100") @Digits(integer = 3, fraction = 4)
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal corporateRatePercent;
    @DecimalMin("0") @DecimalMax("100") @Digits(integer = 3, fraction = 4)
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal personalRatePercent;
    @DecimalMin("0") @Digits(integer = 10, fraction = 2)
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal corporateFixedAmount;
    @DecimalMin("0") @Digits(integer = 10, fraction = 2)
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal personalFixedAmount;
    @Min(0) @Max(4)
    private Integer amountScale;
    @Pattern(regexp = "HALF_UP|HALF_EVEN|DOWN|UP")
    private String roundingMode;
    @Pattern(regexp = "TOTAL|COMPONENT")
    private String roundingStage;
}
