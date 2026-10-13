package cn.iocoder.yudao.module.hrm.dal.dataobject.payroll;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hrm_payroll_tax_policy_event")
@KeySequence("hrm_payroll_tax_policy_event_seq")
public class HrmPayrollTaxPolicyEventDO extends BaseDO {
  @TableId private Long id;
  private Long policyId;
  private String action;
  private Long actorId;
  private String reason;
  private String snapshot;
}
