package cn.iocoder.yudao.module.hrm.dal.dataobject.payroll;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hrm_payroll_tax_policy")
@KeySequence("hrm_payroll_tax_policy_seq")
public class HrmPayrollTaxPolicyDO extends BaseDO {
  @TableId private Long id;
  private Long entityId;
  private String requestKey;
  private String requestSnapshot;
  private Integer year;
  private Integer version;
  private Long priorPolicyId;
  private String taxRegistrationNumber;
  private String name;
  private java.math.BigDecimal monthlyBasicDeduction;
  private String residentBands;
  private String bonusBands;
  private Boolean separateBonusAllowed;
  private java.time.LocalDate separateBonusValidTo;
  private String officialReference;
  private String evidence;
  private String status;
  private Long submittedBy;
  private Long approvedBy;
  private String reviewOpinion;
  private java.time.LocalDateTime approvedTime;
}
