package cn.iocoder.yudao.module.hrm.dal.dataobject.payroll;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hrm_payroll_insurance_base")
@KeySequence("hrm_payroll_insurance_base_seq")
public class HrmPayrollInsuranceBaseDO extends BaseDO {
  @TableId private Long id;
  private Long entityId;
  private Long employeeId;
  private Long policyId;
  private String coverage;
  private Integer incomeYear;
  private java.time.LocalDate effectiveFrom;
  private java.time.LocalDate effectiveTo;
  private Long replacesId;
  private String declaredBases;
  private String evidence;

  @TableField(exist = false)
  private String employeeName;

  @TableField(exist = false)
  private String jobNumber;

  private String status;
  private Long reviewedBy;
  private Long approvedBy;
}
