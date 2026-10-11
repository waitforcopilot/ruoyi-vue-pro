package cn.iocoder.yudao.module.hrm.dal.dataobject.payroll;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hrm_payroll_entity_membership")
@KeySequence("hrm_payroll_entity_membership_seq")
public class HrmPayrollEntityMembershipDO extends BaseDO {
  @TableId private Long id;
  private Long entityId;
  private Long replacesId;
  private Long employeeId;
  private Long deptId;
  private String employeeCategory;
  private String workCity;
  private java.time.LocalDate effectiveFrom;
  private java.time.LocalDate effectiveTo;
  private String evidence;
  private String status;
  private Long approvedBy;
}
