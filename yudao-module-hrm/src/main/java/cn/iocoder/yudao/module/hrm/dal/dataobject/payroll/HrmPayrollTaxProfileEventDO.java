package cn.iocoder.yudao.module.hrm.dal.dataobject.payroll;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hrm_payroll_tax_profile_event")
@KeySequence("hrm_payroll_tax_profile_event_seq")
public class HrmPayrollTaxProfileEventDO extends BaseDO {
  @TableId private Long id;
  private Long profileId;
  private String action;
  private Long actorId;
  private String reason;
  private String snapshot;
}
