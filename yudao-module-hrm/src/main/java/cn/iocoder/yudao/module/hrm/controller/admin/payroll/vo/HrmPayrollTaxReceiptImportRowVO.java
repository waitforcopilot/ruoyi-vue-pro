package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo;

import cn.idev.excel.annotation.*;
import lombok.Data;

@Data
@ExcelIgnoreUnannotated
public class HrmPayrollTaxReceiptImportRowVO {
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
}
