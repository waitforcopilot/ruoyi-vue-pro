package cn.iocoder.yudao.module.hrm.dal.dataobject.salary.slip;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
import lombok.*;

@TableName("hrm_salary_slip_delivery")
@KeySequence("hrm_salary_slip_delivery_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class HrmSalarySlipDeliveryDO extends BaseDO {
  @TableId private Long id;
  private Long sendRecordId;
  private Long slipId;
  private Long userId;
  private String channel;
  private String status;
  private Integer attempts;
  private Long providerLogId;
  private String failureCode;
  private LocalDateTime lastAttemptAt;
  private LocalDateTime nextCheckAt;
}
