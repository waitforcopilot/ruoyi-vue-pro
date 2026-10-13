package cn.iocoder.yudao.module.hrm.dal.dataobject.payroll;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
@Data
@EqualsAndHashCode(callSuper=true)
@TableName("hrm_payroll_input")
@KeySequence("hrm_payroll_input_seq")
public class HrmPayrollInputDO extends BaseDO {
  @TableId private Long id;
  private Long batchId;
  private Long entityId;
  private Long employeeId;
  private Long reviewedBy;
  private Integer version;
  private String kind;
  private String mode;
  private String evidence;
  private String payload;
  private String sourceSnapshot;
  private String status;
}
