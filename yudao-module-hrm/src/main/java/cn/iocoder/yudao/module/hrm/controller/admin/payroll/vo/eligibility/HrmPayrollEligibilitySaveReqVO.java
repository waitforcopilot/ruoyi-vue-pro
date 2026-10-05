package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.eligibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import javax.validation.constraints.*;
import java.time.LocalDate;
@Data
public class HrmPayrollEligibilitySaveReqVO {
    private Long id;
    @Min(1) private Integer revision;
    @NotBlank @Pattern(regexp = "[A-Z0-9][A-Z0-9_-]{0,63}") private String entityCode;
    @NotBlank @Size(max = 160) private String entityName;
    @NotNull @Min(1) private Long employeeId;
    @Pattern(regexp = "INCLUDED|EXCLUDED") private String qualification;
    @Size(max = 120) private String ownerName;
    @Size(max = 2000) private String reference;
    @Size(max = 2000) private String reason;
    @Size(max = 64) @Pattern(regexp = "[a-f0-9]{64}") private String employeeFingerprint;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") private LocalDate effectiveFrom;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") private LocalDate effectiveTo;
}
