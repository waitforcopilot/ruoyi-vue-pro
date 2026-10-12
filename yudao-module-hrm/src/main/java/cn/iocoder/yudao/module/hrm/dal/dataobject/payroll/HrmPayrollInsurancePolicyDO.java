package cn.iocoder.yudao.module.hrm.dal.dataobject.payroll;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hrm_payroll_insurance_policy")
@KeySequence("hrm_payroll_insurance_policy_seq")
public class HrmPayrollInsurancePolicyDO extends BaseDO {
  @TableId private Long id;
  private Long entityId;
  private String code;
  private String city;
  private Integer version;
  private java.time.LocalDate effectiveFrom;
  private java.time.LocalDate effectiveTo;
  private Long replacesId;
  private String evidence;
  private String projects;
  private String status;
  private Long approvedBy;
}
