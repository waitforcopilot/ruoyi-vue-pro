package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class HrmPayrollInsuranceBaseRowVO {
  @ExcelProperty("工号")
  private String jobNumber;

  @ExcelProperty("政策编码")
  private String policyCode;

  @ExcelProperty("缴费方式INSURED或EXEMPT")
  private String coverage;

  @ExcelProperty("收入依据年度")
  private String incomeYear;

  @ExcelProperty("生效月份YYYY-MM")
  private String fromMonth;

  @ExcelProperty("截止月份YYYY-MM")
  private String toMonth;

  @ExcelProperty("替代申报编号")
  private String replacesId;

  @ExcelProperty("项目编码")
  private String projectCode;

  @ExcelProperty("申报基数元")
  private String declaredBase;

  @ExcelProperty("申报或豁免依据")
  private String evidence;
}
