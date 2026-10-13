package cn.iocoder.yudao.module.hrm.service.payroll;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.PAYROLL_TAX_INVALID;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollTaxPolicyReqVO.Band;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollTaxPolicyTrialReqVO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.HrmPayrollTaxPolicyDO;
import java.math.*;
import java.time.LocalDate;
import java.util.*;
import lombok.Data;

/** Resident cumulative withholding and separately assessed annual bonus, using exact thresholds. */
public final class PayrollTaxPolicyEngine {
  private PayrollTaxPolicyEngine() {}

  public static void amount(BigDecimal value) {
    if (value == null
        || value.signum() < 0
        || value.scale() > 2
        || (long) value.precision() - value.scale() > 12) throw exception(PAYROLL_TAX_INVALID);
  }

  public static void bands(List<Band> values) {
    if (values == null || values.isEmpty() || values.size() > 20)
      throw exception(PAYROLL_TAX_INVALID);
    Band previous = null;
    for (int i = 0; i < values.size(); i++) {
      Band band = values.get(i);
      if (band == null
          || band.getRate() == null
          || band.getRate().signum() < 0
          || band.getRate().compareTo(BigDecimal.ONE) > 0
          || (band.getRate().scale() < 0 || band.getRate().scale() > 6))
        throw exception(PAYROLL_TAX_INVALID);
      amount(band.getQuickDeduction());
      if (i == values.size() - 1) {
        if (band.getUpper() != null) throw exception(PAYROLL_TAX_INVALID);
      } else {
        amount(band.getUpper());
        if (band.getUpper().signum() == 0) throw exception(PAYROLL_TAX_INVALID);
      }
      if (previous == null) {
        if (band.getQuickDeduction().signum() != 0) throw exception(PAYROLL_TAX_INVALID);
      } else {
        if (band.getRate().compareTo(previous.getRate()) < 0
            || band.getUpper() != null && band.getUpper().compareTo(previous.getUpper()) <= 0)
          throw exception(PAYROLL_TAX_INVALID);
        BigDecimal expected =
            previous
                .getQuickDeduction()
                .add(previous.getUpper().multiply(band.getRate().subtract(previous.getRate())));
        if (expected.compareTo(band.getQuickDeduction()) != 0) throw exception(PAYROLL_TAX_INVALID);
      }
      previous = band;
    }
  }

  public static BigDecimal tax(BigDecimal income, List<Band> bands, boolean annualBonus) {
    amount(income);
    bands(bands);
    for (Band band : bands) {
      // Compare bonus with upper * 12, so 36000.01 cannot round back into the 3% band.
      if (band.getUpper() == null
          || income.compareTo(
                  annualBonus ? band.getUpper().multiply(BigDecimal.valueOf(12)) : band.getUpper())
              <= 0)
        return income
            .multiply(band.getRate())
            .subtract(band.getQuickDeduction())
            .max(BigDecimal.ZERO)
            .setScale(2, RoundingMode.HALF_UP);
    }
    throw exception(PAYROLL_TAX_INVALID);
  }

  @Data
  public static class Quote {
    private Long policyId;
    private Integer policyVersion;
    private Integer year;
    private String taxRegistrationNumber;
    private String policyStatus;
    private String bonusMode;
    private BigDecimal basicDeduction;
    private BigDecimal salaryTaxable;
    private BigDecimal cumulativeSalaryTax;
    private BigDecimal currentSalaryWithholding;
    private BigDecimal separateBonusTax;
    private BigDecimal currentWithholding;
    private BigDecimal creditToReconcile;
  }

  public static Quote quote(HrmPayrollTaxPolicyDO policy, HrmPayrollTaxPolicyTrialReqVO request) {
    if (policy == null
        || policy.getYear() == null
        || policy.getYear() < 2019
        || policy.getYear() > 2099
        || request == null
        || request.getBasicMonths() == null
        || request.getBasicMonths() < 0
        || request.getBasicMonths() > 12
        || request.getSeparateBonusAlreadyUsed() == null
        || !Arrays.asList("NONE", "MERGED", "SEPARATE").contains(request.getBonusMode()))
      throw exception(PAYROLL_TAX_INVALID);
    for (BigDecimal v :
        Arrays.asList(
            request.getSalaryIncome(),
            request.getSocialDeduction(),
            request.getAdditionalDeduction(),
            request.getOtherDeduction(),
            request.getAlreadyWithheld(),
            request.getBonusAmount())) amount(v);
    amount(policy.getMonthlyBasicDeduction());
    if ("NONE".equals(request.getBonusMode()) && request.getBonusAmount().signum() != 0)
      throw exception(PAYROLL_TAX_INVALID);
    if ("SEPARATE".equals(request.getBonusMode())
        && (!Boolean.TRUE.equals(policy.getSeparateBonusAllowed())
            || Boolean.TRUE.equals(request.getSeparateBonusAlreadyUsed())
            || request.getBonusAmount().signum() == 0
            || policy.getSeparateBonusValidTo() == null
            || LocalDate.of(policy.getYear(), 12, 31).isAfter(policy.getSeparateBonusValidTo())))
      throw exception(PAYROLL_TAX_INVALID);
    List<Band> resident = JsonUtils.parseArray(policy.getResidentBands(), Band.class),
        bonus = JsonUtils.parseArray(policy.getBonusBands(), Band.class);
    Quote quote = new Quote();
    quote.setPolicyId(policy.getId());
    quote.setPolicyVersion(policy.getVersion());
    quote.setYear(policy.getYear());
    quote.setTaxRegistrationNumber(policy.getTaxRegistrationNumber());
    quote.setPolicyStatus(policy.getStatus());
    quote.setBonusMode(request.getBonusMode());
    quote.setBasicDeduction(
        policy
            .getMonthlyBasicDeduction()
            .multiply(BigDecimal.valueOf(request.getBasicMonths()))
            .setScale(2, RoundingMode.HALF_UP));
    BigDecimal salary =
        "MERGED".equals(request.getBonusMode())
            ? request.getSalaryIncome().add(request.getBonusAmount())
            : request.getSalaryIncome();
    quote.setSalaryTaxable(
        salary
            .subtract(quote.getBasicDeduction())
            .subtract(request.getSocialDeduction())
            .subtract(request.getAdditionalDeduction())
            .subtract(request.getOtherDeduction())
            .max(BigDecimal.ZERO)
            .setScale(2, RoundingMode.HALF_UP));
    quote.setCumulativeSalaryTax(tax(quote.getSalaryTaxable(), resident, false));
    BigDecimal balance = quote.getCumulativeSalaryTax().subtract(request.getAlreadyWithheld());
    quote.setCurrentSalaryWithholding(balance.max(BigDecimal.ZERO));
    quote.setCreditToReconcile(balance.negate().max(BigDecimal.ZERO));
    quote.setSeparateBonusTax(
        "SEPARATE".equals(request.getBonusMode())
            ? tax(request.getBonusAmount(), bonus, true)
            : BigDecimal.ZERO.setScale(2));
    quote.setCurrentWithholding(
        quote.getCurrentSalaryWithholding().add(quote.getSeparateBonusTax()));
    return quote;
  }
}
