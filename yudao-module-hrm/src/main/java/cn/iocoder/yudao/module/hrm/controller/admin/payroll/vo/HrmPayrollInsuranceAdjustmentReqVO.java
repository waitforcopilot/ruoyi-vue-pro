package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo;
import javax.validation.constraints.*;
import lombok.Data;
@Data public class HrmPayrollInsuranceAdjustmentReqVO {
 @NotNull private Long paymentId;
 @NotNull private Long billId;
 private Long correctionId;
 @NotBlank @Size(max=1000) private String employeeAcknowledgement;
 @NotBlank @Size(max=1000) private String evidence;
 @NotBlank @Size(max=100) private String requestKey;
}
