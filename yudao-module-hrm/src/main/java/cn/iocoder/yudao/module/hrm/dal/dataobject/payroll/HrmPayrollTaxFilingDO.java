package cn.iocoder.yudao.module.hrm.dal.dataobject.payroll;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hrm_payroll_tax_filing")
@KeySequence("hrm_payroll_tax_filing_seq")
public class HrmPayrollTaxFilingDO extends BaseDO {
  @TableId private Long id;
  private Long entityId, priorFilingId;
  private Integer year, month, version;
  private Boolean zeroDeclaration;
  private String requestKey, status, evidence, reviewReason;
  @com.fasterxml.jackson.annotation.JsonIgnore private String requestSnapshot, sourceSnapshot;
  private Long submittedBy, reviewedBy, filedBy;
  private java.time.LocalDateTime reviewedTime;
  private String filingReference, filingEvidence;
  private java.time.LocalDate filedDate;
  private java.math.BigDecimal expectedTax;
}
