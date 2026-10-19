package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo;
import lombok.Data;
import javax.validation.constraints.*;
import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonFormat;
@Data
public class HrmPayrollRequirementMemberReqVO {
 @NotNull @Positive private Long projectId;
 @NotNull @Positive private Long userId;
 @NotBlank @Size(max=200) private String actions;
 @Size(max=200) private String delegableActions;
 @NotBlank @Size(max=12000) private String ceilingJson;
 @Size(max=1000) private String modulesJson;
 @Size(max=12000) private String requirementIdsJson;
 @Size(max=12000) private String sourceIdsJson;
 @NotNull private Boolean fullProject;
 @NotBlank @Size(max=2000) private String evidence;
 @NotNull @JsonFormat(pattern="yyyy-MM-dd") private LocalDate validFrom;
 @NotNull @JsonFormat(pattern="yyyy-MM-dd") private LocalDate validTo;
}
