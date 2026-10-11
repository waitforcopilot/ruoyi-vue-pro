package cn.iocoder.yudao.module.hrm.dal.dataobject.payroll;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hrm_payroll_time_off")
@KeySequence("hrm_payroll_time_off_seq")
public class HrmPayrollTimeOffDO extends BaseDO {
  @TableId private Long id;
  private Long employeeId;
  private Long timeEntryId;
  private Integer minutes;
  private String requestKey;
  private String reason;
  private Long actorId;
  private String status;
  private java.time.LocalDate expiresOn;
  private String allocations;
}
