package cn.iocoder.yudao.module.hrm.dal.dataobject.payroll;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

@cn.idev.excel.annotation.ExcelIgnoreUnannotated
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hrm_payroll_insurance_bill_line")
@KeySequence("hrm_payroll_insurance_bill_line_seq")
public class HrmPayrollInsuranceBillLineDO extends BaseDO {
  @TableId private Long id;
  private Long billId;
  private Long entityId;
  private Long employeeId;
  private Long revision;

  @cn.idev.excel.annotation.ExcelProperty("工号")
  private String jobNumber;

  @cn.idev.excel.annotation.ExcelProperty("员工")
  private String employeeName;

  @cn.idev.excel.annotation.ExcelProperty("项目编码")
  private String projectCode;

  @cn.idev.excel.annotation.ExcelProperty("项目名称")
  private String projectName;

  @cn.idev.excel.annotation.ExcelProperty("对账状态")
  private String status;

  @cn.idev.excel.annotation.ExcelProperty("个人应缴（元）")
  private java.math.BigDecimal expectedPersonal;

  @cn.idev.excel.annotation.ExcelProperty("单位应缴（元）")
  private java.math.BigDecimal expectedCorporate;

  @cn.idev.excel.annotation.ExcelProperty("原账单个人实缴（元）")
  private java.math.BigDecimal originalPersonal;

  @cn.idev.excel.annotation.ExcelProperty("原账单单位实缴（元）")
  private java.math.BigDecimal originalCorporate;

  @cn.idev.excel.annotation.ExcelProperty("核实后个人实缴（元）")
  private java.math.BigDecimal actualPersonal;

  @cn.idev.excel.annotation.ExcelProperty("核实后单位实缴（元）")
  private java.math.BigDecimal actualCorporate;

  @TableField(exist = false)
  @cn.idev.excel.annotation.ExcelProperty("个人差额（应缴减实缴）")
  private java.math.BigDecimal personalDifference;

  @TableField(exist = false)
  @cn.idev.excel.annotation.ExcelProperty("单位差额（应缴减实缴）")
  private java.math.BigDecimal corporateDifference;
}
