package cn.iocoder.yudao.module.hrm.dal.dataobject.payroll;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hrm_payroll_insurance_event")
@KeySequence("hrm_payroll_insurance_event_seq")
public class HrmPayrollInsuranceEventDO extends BaseDO {
  @TableId private Long id;
  private Long entityId;
  private Long policyId;
  private Long baseId;
  private String action;
  private Long actorId;
  private String reason;
  private String snapshot;
}
