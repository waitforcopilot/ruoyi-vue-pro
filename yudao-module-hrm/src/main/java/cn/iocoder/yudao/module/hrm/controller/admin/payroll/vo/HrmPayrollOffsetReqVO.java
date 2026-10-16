package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo;
import javax.validation.constraints.*;
import lombok.Data;
@Data public class HrmPayrollOffsetReqVO {
 @NotNull private Long batchId;
 @NotNull private Long employeeId;
 @NotNull private Long sourceId;
 @NotBlank private String sourceType;
 @NotBlank @Size(max=1000) private String evidence;
 @NotBlank @Size(max=100) private String requestKey;
}
