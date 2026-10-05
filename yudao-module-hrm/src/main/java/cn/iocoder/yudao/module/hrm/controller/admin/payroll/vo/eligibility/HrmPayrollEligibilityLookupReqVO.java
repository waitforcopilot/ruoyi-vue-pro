package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.eligibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import javax.validation.constraints.*;
import java.time.LocalDate;
@Data
public class HrmPayrollEligibilityLookupReqVO {
    @NotBlank @Pattern(regexp = "[A-Z0-9][A-Z0-9_-]{0,63}") private String entityCode;
    @NotNull @Min(1) private Long employeeId;
    @NotNull @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") private LocalDate start;
    @NotNull @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") private LocalDate end;
}
