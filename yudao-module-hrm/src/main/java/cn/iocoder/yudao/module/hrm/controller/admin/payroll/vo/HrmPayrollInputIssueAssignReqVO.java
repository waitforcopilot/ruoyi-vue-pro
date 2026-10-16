package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo;
import javax.validation.constraints.*;
import lombok.Data;
@Data public class HrmPayrollInputIssueAssignReqVO {
 @NotNull private Long id;
 @NotNull private Long ownerId;
 @NotNull private Long reviewerId;
 @NotNull private java.time.LocalDateTime dueTime;
 @NotBlank @Size(max=1000) private String reason;
}
