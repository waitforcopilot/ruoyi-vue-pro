package cn.iocoder.yudao.module.hrm.dal.dataobject.payroll;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
@Data @EqualsAndHashCode(callSuper=true)
@TableName("hrm_payroll_retro") @KeySequence("hrm_payroll_retro_seq")
public class HrmPayrollRetroDO extends BaseDO {
  @TableId private Long id;
  private Long entityId, batchId, employeeId, paymentId, priorRetroId;
  private Integer version;
  private java.math.BigDecimal originalIncome, correctedIncome, delta;
  private String evidence, requestKey, status, reviewReason, originalDigest;
  @com.fasterxml.jackson.annotation.JsonIgnore private String requestSnapshot;
  private Long submittedBy, reviewedBy;
  private java.time.LocalDateTime reviewedTime;
}
