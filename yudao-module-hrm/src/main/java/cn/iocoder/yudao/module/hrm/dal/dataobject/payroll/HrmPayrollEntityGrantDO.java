package cn.iocoder.yudao.module.hrm.dal.dataobject.payroll;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hrm_payroll_entity_grant")
@KeySequence("hrm_payroll_entity_grant_seq")
public class HrmPayrollEntityGrantDO extends BaseDO {
  @TableId private Long id;
  private Long entityId;
  private Long userId;
  private String actions;
  private java.time.LocalDate effectiveFrom;
  private java.time.LocalDate effectiveTo;
  private String reason;
}
