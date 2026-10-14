package cn.iocoder.yudao.module.hrm.dal.dataobject.payroll;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hrm_payroll_tax_deduction")
@KeySequence("hrm_payroll_tax_deduction_seq")
public class HrmPayrollTaxDeductionDO extends BaseDO {
  @TableId private Long id;
  private Long entityId, employeeId, priorDeclarationId;
  private Integer year, month, version;
  private String taxRegistrationNumber;
  @com.fasterxml.jackson.annotation.JsonIgnore private String taxpayerKey;
  private java.time.LocalDate sourceDate;
  private String mode, valuesSnapshot, officialReference, evidence;
  private String requestKey;
  @com.fasterxml.jackson.annotation.JsonIgnore private String requestSnapshot;
  private String status;
  private Long submittedBy, reviewedBy;
  private String reviewReason;
  private java.time.LocalDateTime reviewedTime;
}
