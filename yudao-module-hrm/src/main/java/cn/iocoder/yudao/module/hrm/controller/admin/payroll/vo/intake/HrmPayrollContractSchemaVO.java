package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.intake;

import lombok.Data;
import javax.validation.Valid;
import javax.validation.constraints.*;
import java.util.List;

/** Explicit business-owned schema. No salary precision or unit is assumed. */
@Data
public class HrmPayrollContractSchemaVO {
    @Valid @NotNull @Size(min = 1, max = 32)
    private List<Field> fields;
    @NotNull @Size(min = 1, max = 8)
    private List<String> keyFields;
    @Size(max = 64) private String periodField;
    @Size(max = 64) private String subjectField;
    @Size(max = 64) private String employeeField;
    @Size(max = 64) private String externalEmployeeField;
    @Size(max = 64) private String employeeNamespace;

    @Data
    public static class Field {
        @NotBlank @Size(max = 64) @Pattern(regexp = "[A-Za-z][A-Za-z0-9_]*")
        private String key;
        @NotBlank @Size(max = 120) private String label;
        @NotBlank @Pattern(regexp = "TEXT|INTEGER|DECIMAL|DATE") private String type;
        @NotNull private Boolean required;
        @Size(max = 40) private String unit;
        @Min(0) @Max(8) private Integer scale;
        @Min(1) @Max(1024) private Integer maxLength;
    }
}
