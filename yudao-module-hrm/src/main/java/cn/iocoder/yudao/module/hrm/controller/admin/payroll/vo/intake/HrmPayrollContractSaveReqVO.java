package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.intake;

import lombok.Data;
import javax.validation.Valid;
import javax.validation.constraints.*;

@Data
public class HrmPayrollContractSaveReqVO {
    private Long id;
    @Min(1) private Integer revision;
    @NotNull private Long sourceId;
    @NotBlank @Size(max = 200) private String title;
    @Size(max = 200) private String actualSystem;
    @Size(max = 120) private String ownerName;
    @Size(max = 500) private String applicableScope;
    @Size(max = 2000) private String evidence;
    @Valid @NotNull private HrmPayrollContractSchemaVO schema;
}
