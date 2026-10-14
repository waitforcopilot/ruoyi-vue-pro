package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo;

import cn.idev.excel.annotation.*;
import lombok.Data;

@Data
@ExcelIgnoreUnannotated
public class HrmPayrollTaxFilingExportRowVO {
  @ExcelProperty("纳税登记号")
  private String taxRegistrationNumber;

  @ExcelProperty("收入所属月份")
  private String period;

  @ExcelProperty("收入类型")
  private String mode;

  @ExcelProperty("纳税人姓名")
  private String taxpayerName;

  @ExcelProperty("证件类型")
  private String identifierType;

  @ExcelProperty("签发国家")
  private String identifierCountry;

  @ExcelProperty("证件号码")
  private String identifier;

  @ExcelProperty("本月收入")
  private java.math.BigDecimal income;

  @ExcelProperty("本月专项扣除")
  private java.math.BigDecimal socialDeduction;

  @ExcelProperty("本月减除费用")
  private java.math.BigDecimal basicDeduction;

  @ExcelProperty("本月已预扣税")
  private java.math.BigDecimal withheld;

  @ExcelProperty("累计收入")
  private java.math.BigDecimal cumulativeIncome;

  @ExcelProperty("累计专项扣除")
  private java.math.BigDecimal cumulativeSocialDeduction;

  @ExcelProperty("累计减除费用")
  private java.math.BigDecimal cumulativeBasicDeduction;

  @ExcelProperty("累计专项附加扣除")
  private java.math.BigDecimal cumulativeAdditionalDeduction;

  @ExcelProperty("累计应税所得")
  private java.math.BigDecimal cumulativeTaxable;

  @ExcelProperty("累计应纳税额")
  private java.math.BigDecimal cumulativeTax;
}
