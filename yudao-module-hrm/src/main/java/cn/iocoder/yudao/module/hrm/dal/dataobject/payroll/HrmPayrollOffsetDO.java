package cn.iocoder.yudao.module.hrm.dal.dataobject.payroll;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
@Data @EqualsAndHashCode(callSuper=true) @TableName("hrm_payroll_offset") @KeySequence("hrm_payroll_offset_seq")
public class HrmPayrollOffsetDO extends BaseDO {
 @TableId private Long id;
 private Long batchId,entityId,employeeId,sourceId,submittedBy,reviewedBy,consumedPaymentId;
 private java.math.BigDecimal netAmount;
 private String sourceType,sourceDigest,status,evidence,reviewReason,requestKey;
 @com.fasterxml.jackson.annotation.JsonIgnore private String requestSnapshot;
 @TableField(exist=false) private Boolean sourceFresh;
 @TableField(exist=false) private String sourceReason;
 private java.time.LocalDateTime reviewedTime;
}
