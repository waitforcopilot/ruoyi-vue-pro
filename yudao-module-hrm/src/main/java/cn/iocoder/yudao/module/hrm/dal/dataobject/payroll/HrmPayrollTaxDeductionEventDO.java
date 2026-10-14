package cn.iocoder.yudao.module.hrm.dal.dataobject.payroll;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hrm_payroll_tax_deduction_event")
@KeySequence("hrm_payroll_tax_deduction_event_seq")
public class HrmPayrollTaxDeductionEventDO extends BaseDO {
  @TableId private Long id;
  private Long declarationId, actorId;
  private String action, reason, snapshot;
}
