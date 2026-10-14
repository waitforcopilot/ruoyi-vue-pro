package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo;

import cn.idev.excel.annotation.*;
import lombok.Data;

@Data
@ExcelIgnoreUnannotated
public class HrmPayrollTaxReceiptExportRowVO {
  @ExcelProperty("记录编号")
  private Long id;

  @ExcelProperty("申报版本编号")
  private Long filingId;

  @ExcelProperty("所属年度")
  private Integer year;

  @ExcelProperty("所属月份")
  private Integer month;

  @ExcelProperty("凭证类型")
  private String kind;

  @ExcelProperty("官方凭证编号")
  private String reference;

  @ExcelProperty("金额")
  private java.math.BigDecimal amount;

  @ExcelProperty("实际缴退日期")
  private String paymentDate;

  @ExcelProperty("原缴税凭证记录编号")
  private Long originalReceiptId;

  @ExcelProperty("核实依据")
  private String evidence;

  @ExcelProperty("复核状态")
  private String status;

  @ExcelProperty("复核人")
  private Long reviewedBy;

  @ExcelProperty("复核依据")
  private String reviewReason;
}
