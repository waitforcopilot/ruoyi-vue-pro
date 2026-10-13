package cn.iocoder.yudao.module.hrm.dal.dataobject.payroll;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hrm_payroll_work_variance_event")
@KeySequence("hrm_payroll_work_variance_event_seq")
public class HrmPayrollWorkVarianceEventDO extends BaseDO {
  @TableId private Long id;
  private Long varianceId;
  private String action;
  private Long actorId;
  private String reason;
  private String snapshot;
}
