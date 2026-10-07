package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.trial;
import lombok.Data;
import javax.validation.constraints.*;
@Data
public class HrmPayrollTrialExecuteReqVO {
 @NotNull @Min(1) private Long batchId;
 @NotNull @Min(1) private Integer revision;
 @NotBlank @Pattern(regexp="[a-f0-9]{64}") private String sourceHash;
 @NotBlank @Pattern(regexp="[A-Za-z0-9_-]{8,64}") private String requestKey;
}
