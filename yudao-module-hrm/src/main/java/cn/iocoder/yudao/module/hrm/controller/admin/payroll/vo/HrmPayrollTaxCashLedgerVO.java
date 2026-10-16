package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import lombok.Data;

@Data
public class HrmPayrollTaxCashLedgerVO {
  private Long profileId;
  private LocalDate asOfDate;
  private boolean ready;
  private BigDecimal income;
  private BigDecimal socialDeduction;
  private BigDecimal basicDeduction;
  private BigDecimal withheld;
  private List<Row> rows = new ArrayList<>();
  private List<String> blockers = new ArrayList<>();

  @Data
  public static class Row {
    private Long paymentId, batchId, runId, employeeId;
    private Long correctionId;
    private Long taxSettlementId;
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private Long payrollOffsetId;
    private Integer taxOriginYear;
    private Integer taxOriginMonth;
    private String taxSettlementMode;
    private LocalDate valueDate;
    private Integer salaryYear, salaryMonth;
    private String status, reason;
    private BigDecimal income, socialDeduction, basicDeduction, withheld, net;
    private BigDecimal separateBonusIncome, separateBonusTax;
  }
}
