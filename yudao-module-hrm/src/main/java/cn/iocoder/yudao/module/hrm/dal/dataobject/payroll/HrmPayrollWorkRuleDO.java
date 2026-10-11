package cn.iocoder.yudao.module.hrm.dal.dataobject.payroll;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hrm_payroll_work_rule")
@KeySequence("hrm_payroll_work_rule_seq")
public class HrmPayrollWorkRuleDO extends BaseDO {
  @TableId private Long id;
  private Long employeeId;
  private String systemType;
  private java.time.LocalDate effectiveFrom;
  private java.time.LocalDate effectiveTo;
  private String legalBasis;
  private String status;
  private Long approvedBy;
  private Integer cycleMinutes;
  private Integer cycleMonths;
  private Integer timeOffExpiryDays;
  private Integer limitMinutes;
  private java.math.BigDecimal hourlyBase;
  private java.math.BigDecimal workdayRate;
  private java.math.BigDecimal restdayRate;
  private java.math.BigDecimal holidayRate;
}
