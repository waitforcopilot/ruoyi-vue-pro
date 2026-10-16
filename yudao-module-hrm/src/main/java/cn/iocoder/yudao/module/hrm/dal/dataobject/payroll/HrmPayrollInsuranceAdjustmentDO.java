package cn.iocoder.yudao.module.hrm.dal.dataobject.payroll;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
@Data @EqualsAndHashCode(callSuper=true) @TableName("hrm_payroll_insurance_adjustment") @KeySequence("hrm_payroll_insurance_adjustment_seq")
public class HrmPayrollInsuranceAdjustmentDO extends BaseDO {
 @TableId private Long id;
 private Long entityId,employeeId,paymentId,billId,correctionId,submittedBy,reviewedBy,confirmedBy;
 private java.math.BigDecimal personalDelta,corporateDelta;
 private String status,employeeAcknowledgement,evidence,reviewReason,receiptReference,receiptEvidence,requestKey,sourceDigest;
 @com.fasterxml.jackson.annotation.JsonIgnore private String sourceSnapshot,requestSnapshot;
 private java.time.LocalDate valueDate;
 private java.time.LocalDateTime reviewedTime,confirmedTime;
}
