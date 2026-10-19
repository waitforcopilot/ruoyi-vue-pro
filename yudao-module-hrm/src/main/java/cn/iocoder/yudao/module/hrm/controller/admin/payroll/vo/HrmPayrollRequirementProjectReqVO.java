package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo;
import lombok.Data;
import javax.validation.constraints.*;
@Data public class HrmPayrollRequirementProjectReqVO {
 private Long id;
 @NotBlank @Size(max=64) @Pattern(regexp="[A-Z][A-Z0-9_-]{1,63}") private String projectKey;
 @NotBlank @Size(max=200) private String name;
 private Integer metadataVersion;
}
