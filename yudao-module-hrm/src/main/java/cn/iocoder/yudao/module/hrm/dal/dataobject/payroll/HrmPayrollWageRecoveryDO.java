package cn.iocoder.yudao.module.hrm.dal.dataobject.payroll;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
@Data @EqualsAndHashCode(callSuper=true) @TableName("hrm_payroll_wage_recovery") @KeySequence("hrm_payroll_wage_recovery_seq")
public class HrmPayrollWageRecoveryDO extends BaseDO {
 @TableId private Long id;
 private Long retroId,entityId,employeeId,paymentId,submittedBy,reviewedBy,confirmedBy;
 private java.math.BigDecimal amount;
 private String status,employeeAcknowledgement,evidence,reviewReason,receiptReference,receiptEvidence,requestKey,allocationSnapshot,allocationDigest;
 @com.fasterxml.jackson.annotation.JsonIgnore private String requestSnapshot;
 @TableField(exist=false) private java.math.BigDecimal originalIncome,correctedIncome;
 @TableField(exist=false) private Long priorRetroId;
 private java.time.LocalDate valueDate;
 private java.time.LocalDateTime reviewedTime,confirmedTime;
}
