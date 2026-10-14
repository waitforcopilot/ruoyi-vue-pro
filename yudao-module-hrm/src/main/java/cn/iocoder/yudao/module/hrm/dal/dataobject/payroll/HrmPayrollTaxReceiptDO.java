package cn.iocoder.yudao.module.hrm.dal.dataobject.payroll;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hrm_payroll_tax_receipt")
@KeySequence("hrm_payroll_tax_receipt_seq")
public class HrmPayrollTaxReceiptDO extends BaseDO {
  @TableId private Long id;
  private Long filingId, entityId, originalReceiptId;
  private Integer year, month;
  private String kind, reference, evidence, status, requestKey, reviewReason;
  @com.fasterxml.jackson.annotation.JsonIgnore private String requestSnapshot;
  private java.time.LocalDate paymentDate;
  private java.math.BigDecimal amount;
  private Long submittedBy, reviewedBy;
  private java.time.LocalDateTime reviewedTime;
}
