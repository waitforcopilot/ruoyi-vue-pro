package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo;
import javax.validation.constraints.*;
import lombok.Data;
@Data public class HrmPayrollInputIssueReqVO {
 @NotNull private Long batchId;
 @NotNull private Long employeeId;
 @NotBlank private String kind;
 @NotNull private Long ownerId;
 @NotNull private Long reviewerId;
 @NotNull private java.time.LocalDateTime dueTime;
 @NotBlank @Size(max=1000) private String reason;
 @NotBlank @Size(max=100) private String requestKey;
}
