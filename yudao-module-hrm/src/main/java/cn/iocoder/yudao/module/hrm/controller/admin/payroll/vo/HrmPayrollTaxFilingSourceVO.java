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

  @com.fasterxml.jackson.annotation.JsonInclude(
      com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
  private BigDecimal actualWithheldTotal;

  @com.fasterxml.jackson.annotation.JsonInclude(
      com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
  private BigDecimal periodTaxDifference;

  private List<Row> rows = new ArrayList<>();

  @Data
  public static class Row {
    private Long profileId;
    private String taxpayerName, identifierType, identifierCountry, maskedIdentifier, mode;
    @com.fasterxml.jackson.annotation.JsonIgnore private String taxpayerKey;
    private List<Long> employeeIds = new ArrayList<>(),
        paymentIds = new ArrayList<>(),
        runIds = new ArrayList<>();
    private BigDecimal income = BigDecimal.ZERO,
        socialDeduction = BigDecimal.ZERO,
        basicDeduction = BigDecimal.ZERO,
        withheld = BigDecimal.ZERO;

    @com.fasterxml.jackson.annotation.JsonInclude(
        com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY)
    private List<Long> taxCorrectionIds = new ArrayList<>();

    @com.fasterxml.jackson.annotation.JsonInclude(
        com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private BigDecimal cumulativeActualWithheld;

    @com.fasterxml.jackson.annotation.JsonInclude(
        com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private BigDecimal expectedBonusWithholding;

    @com.fasterxml.jackson.annotation.JsonInclude(
        com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private BigDecimal expectedSalaryWithholding;

    @com.fasterxml.jackson.annotation.JsonInclude(
        com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private BigDecimal bonusTaxBalance;

    @com.fasterxml.jackson.annotation.JsonInclude(
        com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private BigDecimal cumulativeTaxBalance;

    private BigDecimal cumulativeIncome,
        cumulativeSocialDeduction,
        cumulativeBasicDeduction,
        cumulativeAdditionalDeduction,
        cumulativeTaxable,
        cumulativeTax;
  }
}
