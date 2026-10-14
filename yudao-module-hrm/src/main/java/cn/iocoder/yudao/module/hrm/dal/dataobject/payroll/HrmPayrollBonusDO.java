package cn.iocoder.yudao.module.hrm.dal.dataobject.payroll;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hrm_payroll_bonus")
@KeySequence("hrm_payroll_bonus_seq")
public class HrmPayrollBonusDO extends BaseDO {
  @TableId private Long id;
  private Long batchId, entityId, employeeId, profileId, priorBonusId;
  private Integer version;
  private java.time.LocalDate expectedPaymentDate;
  private java.math.BigDecimal amount;
  private String mode, evidence, requestKey, status, reviewReason;
  @com.fasterxml.jackson.annotation.JsonIgnore private String requestSnapshot;
  private Long submittedBy, reviewedBy;
  private java.time.LocalDateTime reviewedTime;
}
