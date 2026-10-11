package cn.iocoder.yudao.module.hrm.dal.dataobject.payroll;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hrm_payroll_attendance_issue_event")
@KeySequence("hrm_payroll_attendance_issue_event_seq")
public class HrmPayrollAttendanceIssueEventDO extends BaseDO {
  @TableId private Long id;
  private Long issueId;
  private String action;
  private String fromStatus;
  private String toStatus;
  private Long actorId;
  private String reason;
  private String snapshot;
}
