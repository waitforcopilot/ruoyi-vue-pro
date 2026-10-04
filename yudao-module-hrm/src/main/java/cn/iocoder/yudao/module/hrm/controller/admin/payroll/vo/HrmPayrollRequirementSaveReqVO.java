package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo;
import lombok.Data;
import javax.validation.constraints.*;
import java.util.List;
@Data
public class HrmPayrollRequirementSaveReqVO {
    private Long id;
    @Min(1) private Integer version;
    @NotBlank @Size(max = 64) @Pattern(regexp = "[A-Z]+-[A-Z0-9-]+")
    private String code;
    @NotBlank @Pattern(regexp = "overview|plan|calc|tax|attendance|overtime|hours|insurance|report|req")
    private String moduleCode;
    @NotBlank @Size(max = 500)
    private String title;
    @Size(max = 4000)
    private String description;
    @Size(max = 120)
    private String ownerName;
    @Min(1) @Max(3)
    private Integer priority;
    @Min(0) @Max(2)
    private Integer scopeDecision;
    @Size(max = 2000)
    private String scopeReason;
    @Size(max = 500)
    private String applicableScope;
    @Size(max = 100)
    private List<String> sourceCodes;
    @Size(max = 4000)
    private String fieldMapping;
    @Size(max = 4000)
    private String acceptanceCriteria;
    @Size(max = 2000)
    private String remark;
}
