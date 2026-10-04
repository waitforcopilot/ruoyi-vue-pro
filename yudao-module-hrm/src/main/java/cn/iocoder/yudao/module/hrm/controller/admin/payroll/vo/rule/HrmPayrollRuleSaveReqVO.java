package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.rule;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import javax.validation.Valid;
import javax.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;

/** Business rule evidence, not an executable formula. Samples must be synthetic or anonymized. */
@Data
public class HrmPayrollRuleSaveReqVO {
    private Long id;
    @Min(1) private Integer revision;
    @NotBlank @Size(max=64) @Pattern(regexp="RULE-[A-Z0-9-]+") private String code;
    @NotBlank @Size(max=200) private String title;
    @NotBlank @Pattern(regexp="PERIOD|PAYROLL|ATTENDANCE|OVERTIME|HOURS|INSURANCE|TAX|ROUNDING|LIFECYCLE|SLIP|ACCESS|PAYMENT|COST|OTHER") private String category;
    @Pattern(regexp="Q-(0[1-9]|1[0-9])") private String questionCode;
    @Size(max=120) private String ownerName;
    @Size(max=64) @Pattern(regexp="[A-Z0-9][A-Z0-9_-]*") private String scopeCode;
    @Size(max=500) private String applicableScope;
    @JsonFormat(shape=JsonFormat.Shape.STRING, pattern="yyyy-MM-dd") private LocalDate effectiveFrom;
    @JsonFormat(shape=JsonFormat.Shape.STRING, pattern="yyyy-MM-dd") private LocalDate effectiveTo;
    @Size(max=4000) private String definition;
    @Size(max=2000) private String reference;
    @Valid @NotNull @Size(max=32) private List<Parameter> parameters;
    @Valid @NotNull @Size(max=20) private List<BusinessCase> cases;

    @Data
    public static class Parameter {
        @NotBlank @Size(max=64) @Pattern(regexp="[A-Za-z][A-Za-z0-9_]*") private String key;
        @NotBlank @Size(max=120) private String label;
        @NotBlank @Pattern(regexp="TEXT|INTEGER|DECIMAL|BOOLEAN|DATE") private String type;
        @Size(max=1024) private String value;
        @Size(max=40) private String unit;
        @Min(0) @Max(8) private Integer scale;
    }
    @Data
    public static class BusinessCase {
        @NotBlank @Size(max=120) private String title;
        @NotBlank @Size(max=4000) private String inputJson;
        @NotBlank @Size(max=2000) private String expectedResult;
    }
}
