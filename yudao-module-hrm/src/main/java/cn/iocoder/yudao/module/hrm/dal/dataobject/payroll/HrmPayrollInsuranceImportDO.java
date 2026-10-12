package cn.iocoder.yudao.module.hrm.dal.dataobject.payroll;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hrm_payroll_insurance_import")
@KeySequence("hrm_payroll_insurance_import_seq")
public class HrmPayrollInsuranceImportDO extends BaseDO {
  @TableId private Long id;
  private Long entityId;
  private String fileName;
  private String digest;
  private String rowsSnapshot;
  private String errors;
  private String requests;
  private String proposalIds;
  private String status;
}
