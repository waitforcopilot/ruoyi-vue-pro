package cn.iocoder.yudao.module.hrm.dal.dataobject.payroll;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hrm_payroll_insurance_settlement")
@KeySequence("hrm_payroll_insurance_settlement_seq")
public class HrmPayrollInsuranceSettlementDO extends BaseDO {
  @TableId private Long id;
  private Long billId;
  private Long lineId;
  private Long entityId;
  private Long lineRevision;
  private Long reviewedBy;
  private Long approvedBy;
  private String mode;
  private String reference;
  private String digest;
  private String evidence;
  private String status;
  private String proofName;
  private String proofContent;
  private java.math.BigDecimal personal;
  private java.math.BigDecimal corporate;
}
