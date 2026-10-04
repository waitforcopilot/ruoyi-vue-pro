package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo;
import lombok.Data;
import javax.validation.constraints.*;
import java.util.List;
@Data
public class HrmPayrollSourceSaveReqVO {
    private Long id;
    @Min(1) private Integer version;
    @NotBlank @Size(max = 64) @Pattern(regexp = "DS-[A-Z0-9-]+")
    private String code;
    @NotBlank @Size(max = 200)
    private String name;
    @Size(max = 4000)
    private String description;
    @Size(max = 4000)
    private String requiredFields;
    @Size(max = 200)
    private String actualSystem;
    @Size(max = 4000)
    private String fieldMapping;
    @Size(max = 120)
    private String ownerName;
    @NotNull @Min(0) @Max(2)
    private Integer readiness;
    @Size(max = 2000)
    private String evidence;
}
