package cn.iocoder.yudao.module.hrm.dal.dataobject.payroll;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hrm_payroll_work_variance")
@KeySequence("hrm_payroll_work_variance_seq")
public class HrmPayrollWorkVarianceDO extends BaseDO {
  @TableId private Long id;
  private Long entityId;
  private Long employeeId;
  private java.time.LocalDate workDate;
  private String requestKey;
  private String reason;
  private String sourceSnapshot;
  private Long ownerId;
  private Long reviewerId;
  private java.time.LocalDateTime dueTime;
  private String status;
  private Long submittedBy;
  private String evidence;
  private String submittedSnapshot;
  private String reviewOpinion;
  private String reminderState;
  private String reminderError;
  private java.time.LocalDateTime closedTime;

  @TableField(exist = false)
  private Boolean sourceFresh;

  @TableField(exist = false)
  private Boolean sourceReady;

  @TableField(exist = false)
  private Boolean overdue;
}
