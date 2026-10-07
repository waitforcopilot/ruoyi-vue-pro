package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.trial;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import javax.validation.Valid;
import javax.validation.constraints.*;
import java.time.LocalDate;
@Data
public class HrmPayrollTrialSaveReqVO {
 private Long id; @Min(1) private Integer revision;
 @NotBlank @Pattern(regexp="[A-Z0-9][A-Z0-9_-]{0,63}") private String code;
 @NotBlank @Size(max=160) private String title;
 @NotBlank @Pattern(regexp="[A-Z0-9][A-Z0-9_-]{0,63}") private String entityCode;
 @NotBlank @Size(max=160) private String entityName;
 @NotBlank @Pattern(regexp="MONTHLY|CUSTOM") private String periodType="MONTHLY";
 @NotNull @JsonFormat(shape=JsonFormat.Shape.STRING,pattern="yyyy-MM-dd") private LocalDate periodStart;
 @NotNull @JsonFormat(shape=JsonFormat.Shape.STRING,pattern="yyyy-MM-dd") private LocalDate periodEnd;
 @NotNull @Min(1) private Long definitionId;
 @Size(max=120) private String ownerName;
 @Size(max=2000) private String reference;
 @Valid @NotNull private HrmPayrollTrialConfigVO configuration;
}
