package cn.iocoder.yudao.module.hrm.dal.dataobject.payroll;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hrm_payroll_time_entry")
@KeySequence("hrm_payroll_time_entry_seq")
public class HrmPayrollTimeEntryDO extends BaseDO {
  @TableId private Long id;
  private Long employeeId;
  private Long entityId;
  private Long ruleId;
  private Long sourceApplyId;
  private Long clockInId;
  private Long clockOutId;
  private String sourceSnapshot;
  private String sourceSystem;
  private String sourceRecordId;
  private String evidence;
  private java.time.LocalDateTime startTime;
  private java.time.LocalDateTime endTime;
  private java.time.LocalDateTime clockStart;
  private java.time.LocalDateTime clockEnd;
  private Integer breakMinutes;
  private Integer recognizedMinutes;
  private String dayType;
  private String compensation;
  private java.math.BigDecimal amount;
  private Long confirmedBy;
}
