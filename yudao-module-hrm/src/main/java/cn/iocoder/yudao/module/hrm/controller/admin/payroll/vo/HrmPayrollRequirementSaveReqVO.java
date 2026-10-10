package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo;
import lombok.Data;
import javax.validation.constraints.*;
@Data
public class HrmPayrollRequirementSaveReqVO {
    private Long id;
    @NotBlank @Pattern(regexp="[A-Z][A-Z0-9]*-[0-9]{2,6}") @Size(max=64) private String code;
    @NotBlank @Pattern(regexp="OV|PLAN|CALC|TAX|ATT|OT|HOUR|INS|RPT|COL") private String module;
    @NotBlank @Size(max=1000) private String description;
    @Size(max=4000) private String fieldMapping;
    @Size(max=1000) private String sourceSystem;
    @Positive private Long sourceOwnerId;
    @Positive private Long reviewerId;
    @NotNull @Min(1) @Max(3) private Integer priority;
    @NotBlank @Pattern(regexp="PENDING|REVIEWING|CONFIRMED|DISPUTED|DEFERRED") private String status;
    @NotBlank @Pattern(regexp="MISSING|PARTIAL|READY|EXEMPT") private String readiness;
    @Size(max=2000) private String acceptance;
    @Size(max=2000) private String evidence;
    @Min(1) private Integer version;
}
