package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo;

import java.math.BigDecimal;
import java.util.*;
import lombok.Data;

@Data
public class HrmPayrollTaxFilingSourceVO {
  private Long entityId, policyId;
  private Integer year, month, policyVersion;
  private String taxRegistrationNumber;
  private BigDecimal totalTax = BigDecimal.ZERO;
  private List<Row> rows = new ArrayList<>();

  @Data
  public static class Row {
    private Long profileId;
    private String
        taxpayerName,
        identifierType,
        identifierCountry,
        maskedIdentifier,
        mode;
    @com.fasterxml.jackson.annotation.JsonIgnore private String taxpayerKey;
    private List<Long> employeeIds = new ArrayList<>(),
        paymentIds = new ArrayList<>(),
        runIds = new ArrayList<>();
    private BigDecimal income = BigDecimal.ZERO,
        socialDeduction = BigDecimal.ZERO,
        basicDeduction = BigDecimal.ZERO,
        withheld = BigDecimal.ZERO;
    private BigDecimal cumulativeIncome,
        cumulativeSocialDeduction,
        cumulativeBasicDeduction,
        cumulativeAdditionalDeduction,
        cumulativeTaxable,
        cumulativeTax;
  }
}
