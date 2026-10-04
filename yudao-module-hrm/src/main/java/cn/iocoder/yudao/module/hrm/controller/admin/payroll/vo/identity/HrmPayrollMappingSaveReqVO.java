package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.identity;
import lombok.Data;
import javax.validation.constraints.*;
import java.time.LocalDate;
@Data
public class HrmPayrollMappingSaveReqVO {
    private Long id;
    private Integer revision;
    @NotNull @Min(1) private Long sourceId;
    @NotBlank @Pattern(regexp="[A-Z][A-Z0-9_-]{0,63}") private String namespace;
    @NotBlank @Size(max=128) private String externalCode;
    @NotNull @Min(1) private Long employeeId;
    @Size(max=64) private String employeeFingerprint;
    @Size(max=120) private String ownerName;
    @Size(max=2000) private String reference;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
}
