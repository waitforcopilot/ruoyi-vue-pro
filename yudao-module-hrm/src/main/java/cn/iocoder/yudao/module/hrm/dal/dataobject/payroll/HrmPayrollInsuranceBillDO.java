package cn.iocoder.yudao.module.hrm.dal.dataobject.payroll;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hrm_payroll_insurance_bill")
@KeySequence("hrm_payroll_insurance_bill_seq")
public class HrmPayrollInsuranceBillDO extends BaseDO {
  @TableId private Long id;
  private Integer version;
  private Long replacesId;
  private Long entityId;
  private Long reviewedBy;
  private Long approvedBy;
  private Integer year;
  private Integer month;
  private String fileName;
  private String digest;
  private String reference;
  private String evidence;
  private String sources;
  private String rowsSnapshot;
  private String errors;
  private String comparison;
  private String status;
}
