package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.calculation;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import javax.validation.Valid;
import javax.validation.constraints.*;
import java.time.LocalDate;
@Data
public class HrmPayrollCalculationSaveReqVO {
    private Long id;
    @Min(1) private Integer revision;
    @NotBlank @Pattern(regexp = "[A-Z0-9][A-Z0-9_-]{0,63}") private String code;
    @NotBlank @Size(max = 160) private String title;
    @Size(max = 120) private String ownerName;
    @NotBlank @Pattern(regexp = "[A-Z0-9][A-Z0-9_-]{0,63}") private String scopeCode;
    @Size(max = 500) private String applicableScope;
    @Size(max = 4000) private String description;
    @Size(max = 2000) private String reference;
    @JsonFormat(pattern = "yyyy-MM-dd") private LocalDate effectiveFrom;
    @JsonFormat(pattern = "yyyy-MM-dd") private LocalDate effectiveTo;
    @Valid @NotNull private HrmPayrollCalculationSpecVO program;
}
