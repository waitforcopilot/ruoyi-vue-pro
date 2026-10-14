package cn.iocoder.yudao.module.hrm.dal.dataobject.payroll;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.*;

/** Approved tax-source overlay; payment and frozen wage amounts remain immutable. */
@TableName("hrm_payroll_tax_correction")
@KeySequence("hrm_payroll_tax_correction_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class HrmPayrollTaxCorrectionDO extends BaseDO {
  @TableId private Long id;
  private Long entityId;
  private Long employeeId;
  private Long paymentId;
  private Long runId;
  private Long priorCorrectionId;
  private Integer version;
  private Long policyId;
  private Integer policyVersion;
  private Long profileId;
  private Integer profileVersion;
  private LocalDate valueDate;
  private String taxRegistrationNumber;
  @JsonIgnore private String taxpayerKey;
  private String sourceDigest;
  private String officialReference;
  private String evidence;
  private String reason;
  private BigDecimal income;
  private BigDecimal socialDeduction;
  private BigDecimal basicDeduction;
  private BigDecimal actualSalaryWithheld;
  private BigDecimal separateBonusIncome;
  private BigDecimal actualBonusWithheld;
  private String status;
  private Long submittedBy;
  private Long approvedBy;
  private String reviewOpinion;
  private LocalDateTime approvedTime;
  private String requestKey;
  @JsonIgnore private String requestSnapshot;
  @JsonIgnore private String originalSnapshot;
}
