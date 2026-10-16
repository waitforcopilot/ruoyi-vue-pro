package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo;
import javax.validation.constraints.*;
import lombok.Data;
@Data public class HrmPayrollInputIssueActionReqVO {
 @NotNull private Long id;
 @Size(max=1000) private String evidence;
 @Size(max=1000) private String reason;
 private Boolean approved;
}
