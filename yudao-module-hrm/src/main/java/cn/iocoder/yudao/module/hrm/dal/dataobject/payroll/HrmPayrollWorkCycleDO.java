package cn.iocoder.yudao.module.hrm.dal.dataobject.payroll;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hrm_payroll_work_cycle")
@KeySequence("hrm_payroll_work_cycle_seq")
public class HrmPayrollWorkCycleDO extends BaseDO {
  @TableId private Long id;
  private Long employeeId;
  private Long entityId;
  private Long ruleId;
  private java.time.LocalDate cycleFrom;
  private java.time.LocalDate cycleTo;
  private Integer plannedMinutes;
  private Integer actualMinutes;
  private Integer recognizedMinutes;
  private Integer payableMinutes;
  private java.math.BigDecimal amount;
  private String sourceSnapshot;
  private Long settledBy;

  @TableField(exist = false)
  private Boolean sourceFresh;
}
