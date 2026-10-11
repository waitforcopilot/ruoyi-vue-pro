package cn.iocoder.yudao.module.hrm.dal.dataobject.payroll;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hrm_payroll_legal_entity")
@KeySequence("hrm_payroll_legal_entity_seq")
public class HrmPayrollLegalEntityDO extends BaseDO {
  @TableId private Long id;
  private String code;
  private String name;
  private String taxRegistrationNumber;
  private String evidence;
  private String status;
  private Long approvedBy;
  private Long submittedBy;

  @TableField(exist = false)
  private java.util.Set<String> allowedActions;
}
