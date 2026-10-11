package cn.iocoder.yudao.module.hrm.dal.dataobject.payroll;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hrm_payroll_attendance_issue")
@KeySequence("hrm_payroll_attendance_issue_seq")
public class HrmPayrollAttendanceIssueDO extends BaseDO {
  @TableId private Long id;
  private Long employeeId;
  private java.time.LocalDate attendanceDate;
  private String type;
  private Integer clockType;
  private Long ownerId;
  private Long reviewerId;
  private String requestKey;
  private String status;
  private String initialSnapshot;
  private String evidence;
  private java.time.LocalDateTime correctionTime;
  private Long submittedBy;
  private Long correctionClockId;
  private String reviewOpinion;
  private java.time.LocalDateTime dueTime;
  private java.time.LocalDateTime closedTime;

  @TableField(exist = false)
  private String employeeName;

  @TableField(exist = false)
  private String ownerName;

  @TableField(exist = false)
  private String reviewerName;
}
