package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class HrmPayrollInsuranceBillRowVO {
  @ExcelProperty("工号")
  private String jobNumber;

  @ExcelProperty("缴费月份")
  private String month;

  @ExcelProperty("项目编码")
  private String projectCode;

  @ExcelProperty("个人实缴（元）")
  private String personal;

  @ExcelProperty("单位实缴（元）")
  private String corporate;
}
