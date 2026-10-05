package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.calculation;

import lombok.Data;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import javax.validation.Valid;
import javax.validation.constraints.*;
import java.util.*;

/** Decimal expressions and explicit expectations; never inferred from the textual rule ledger. */
@Data
public class HrmPayrollCalculationSpecVO {
    @Valid @NotNull @Size(max = 32) private List<@NotNull Input> inputs = new ArrayList<>();
    @Valid @NotNull @Size(min = 1, max = 32) private List<@NotNull Item> items = new ArrayList<>();
    @Valid @NotNull @Size(max = 20) private List<@NotNull BusinessCase> cases = new ArrayList<>();
    @Min(0) @Max(8) @JsonDeserialize(using = ExplicitIntegerDeserializer.class) private Integer divisionScale;
    @Pattern(regexp = "HALF_UP|HALF_EVEN|DOWN|UP") private String divisionRoundingMode;

    @Data
    public static class Input {
        @NotBlank @Pattern(regexp = "[A-Za-z][A-Za-z0-9_]{0,63}") private String key;
        @NotBlank @Size(max = 120) private String label;
        @NotBlank @Pattern(regexp = "INTEGER|DECIMAL") private String type;
        @NotBlank @Size(max = 40) private String unit;
        @NotNull @Min(0) @Max(8) @JsonDeserialize(using = ExplicitIntegerDeserializer.class) private Integer scale;
    }
    @Data
    public static class Item {
        @NotBlank @Pattern(regexp = "[A-Za-z][A-Za-z0-9_]{0,63}") private String key;
        @NotBlank @Size(max = 120) private String label;
        @NotBlank @Size(max = 40) private String unit;
        @NotBlank @Size(max = 256) private String expression;
        @NotNull @Min(0) @Max(8) @JsonDeserialize(using = ExplicitIntegerDeserializer.class) private Integer amountScale;
        @NotBlank @Pattern(regexp = "HALF_UP|HALF_EVEN|DOWN|UP") private String roundingMode;
    }
    @Data
    public static class BusinessCase {
        @NotBlank @Size(max = 120) private String title;
        @NotNull @Size(max = 32) @JsonDeserialize(contentUsing = PlainDecimalStringDeserializer.class)
        private Map<@Size(max = 64) String, @Size(max = 40) String> inputs = new LinkedHashMap<>();
        @NotNull @Size(max = 32) @JsonDeserialize(contentUsing = PlainDecimalStringDeserializer.class)
        private Map<@Size(max = 64) String, @Size(max = 40) String> expected = new LinkedHashMap<>();
    }
}
