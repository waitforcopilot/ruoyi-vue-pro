package cn.iocoder.yudao.module.hrm.dal.dataobject.payroll;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
@Data
@EqualsAndHashCode(callSuper=true)
@TableName("hrm_payroll_source_claim")
@KeySequence("hrm_payroll_source_claim_seq")
public class HrmPayrollSourceClaimDO extends BaseDO {
 @TableId private Long id;
 private Long entityId;
 private Long employeeId;
 private Long batchId;
 private Long runId;
 private String kind;
 private String sourceKey;
 private java.math.BigDecimal amount;
 private String status;
 private Long releasedBy;
 private String releaseReason;
}
