package cn.iocoder.yudao.module.hrm.dal.dataobject.payroll;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hrm_payroll_tax_profile")
@KeySequence("hrm_payroll_tax_profile_seq")
public class HrmPayrollTaxProfileDO extends BaseDO {
  @TableId private Long id;
  private Long entityId;
  private Long employeeId;
  private Integer year;
  private Integer version;
  private Long priorProfileId;
  private String taxRegistrationNumber;
  private String taxpayerName;
  private String identifierType;
  private String identifierCountry;
  @JsonIgnore private String identifierCipher;
  @JsonIgnore private String requestFingerprint;
  private String taxpayerKey;
  private String maskedIdentifier;
  private Boolean residentThisYear;
  private Boolean noPriorContinuousRemunerationBeforeEntry;
  private Boolean firstSalaryThisYear;
  private Boolean noPriorSalaryThisYear;
  private Boolean separateBonusUsedElsewhere;
  private String openingMode;
  private java.time.LocalDate openingAsOfDate;
  private java.math.BigDecimal openingIncome;
  private java.math.BigDecimal openingSocialDeduction;
  private java.math.BigDecimal openingBasicDeduction;
  private java.math.BigDecimal openingWithheld;
  private String officialReference;
  private String evidence;
  private String employeeSnapshot;
  private String requestKey;
  private String status;
  private Long submittedBy;
  private Long reviewedBy;
  private String reviewOpinion;
  private java.time.LocalDateTime reviewedTime;
}
