package cn.iocoder.yudao.module.hrm.dal.dataobject.salary.slip;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
import lombok.*;

@TableName("hrm_salary_slip_delivery_attempt")
@KeySequence("hrm_salary_slip_delivery_attempt_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class HrmSalarySlipDeliveryAttemptDO extends BaseDO {
  @TableId private Long id;
  private Long deliveryId;
  private Integer attemptNumber;
  private String status;
  private Long providerLogId;
  private String failureCode;
  private LocalDateTime startedAt;
}
