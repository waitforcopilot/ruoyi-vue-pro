package cn.iocoder.yudao.module.hrm.dal.dataobject.payroll;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hrm_payroll_no_overtime")
@KeySequence("hrm_payroll_no_overtime_seq")
public class HrmPayrollNoOvertimeDO extends BaseDO {
  @TableId private Long id;
  private Long entityId;
  private Long employeeId;
  private Long sourceApplyId;
  private java.time.LocalDate workDate;
  private String requestKey;
  private String reason;
  private String evidence;
  private String sourceSnapshot;
  private String status;
  private Long submittedBy;
  private Long reviewedBy;
  private String reviewOpinion;
  private java.time.LocalDateTime reviewedTime;
  private Long timeEntryId;

  @TableField(exist = false)
  private Boolean sourceFresh;
}
