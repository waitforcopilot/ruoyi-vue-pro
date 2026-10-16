package cn.iocoder.yudao.module.hrm.service.payroll;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.PAYROLL_TAX_INVALID;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.PAYROLL_TAX_PAYMENT_DATE_MISSING;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollTaxCashLedgerVO;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollTaxPolicyTrialReqVO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import javax.annotation.Resource;
import lombok.Data;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** Source assembly for resident withholding. Only scoped payroll services may call it. */
@Service
public class HrmPayrollTaxWithholdingService {
  @Resource private HrmPayrollTaxPolicyService policies;
  @Resource private HrmPayrollBonusService bonuses;
  @Resource private HrmPayrollTaxProfileService profiles;
  @Resource private HrmPayrollTaxDeductionService deductions;
  @Resource private HrmPayrollTaxCashLedgerService cash;
  @Resource private HrmPayrollTaxEmploymentService employment;

  @Data
  public static class Source {
    private Long entityId, employeeId, batchId;
    private HrmPayrollBonusDO bonus;

    @com.fasterxml.jackson.annotation.JsonIgnore
    public BigDecimal bonusAmount() {
      return bonus == null ? BigDecimal.ZERO : bonus.getAmount();
    }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public String bonusMode() {
      return bonus == null ? "NONE" : bonus.getMode();
    }

    private LocalDate expectedPaymentDate;
    private HrmPayrollTaxPolicyDO policy;
    private HrmPayrollTaxProfileDO profile;
    private HrmPayrollTaxDeductionDO declaration;
    private HrmPayrollTaxCashLedgerVO paid;
    private PayrollTaxEmploymentEngine.Result employment;
    private BigDecimal currentIncome, currentSocialDeduction, additionalDeduction;
    private Map<String, BigDecimal> deductionValues;
    private PayrollTaxPolicyEngine.Quote quote;
  }

  /** Approved earnings must enter the formula inputs before derived wage items are evaluated. */
  public HrmPayrollBonusDO bonusForBatch(Long batchId, Long employeeId) {
    return batchId == null ? null : bonuses.source(batchId, employeeId);
  }

  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public Source calculate(
      Long entityId,
      Long employeeId,
      LocalDate payDate,
      BigDecimal currentIncome,
      BigDecimal currentSocial,
      BigDecimal confirmedAdditional) {
    return calculateForBatch(
        null, entityId, employeeId, payDate, currentIncome, currentSocial, confirmedAdditional);
  }

  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public Source calculateForBatch(
      Long batchId,
      Long entityId,
      Long employeeId,
      LocalDate payDate,
      BigDecimal currentIncome,
      BigDecimal currentSocial,
      BigDecimal confirmedAdditional) {
    if (payDate == null) throw exception(PAYROLL_TAX_PAYMENT_DATE_MISSING);
    PayrollTaxPolicyEngine.amount(currentIncome);
    PayrollTaxPolicyEngine.amount(currentSocial);
    PayrollTaxPolicyEngine.amount(confirmedAdditional);
    HrmPayrollTaxPolicyDO policy = policies.requireApproved(entityId, payDate.getYear());
    HrmPayrollTaxProfileDO profile =
        profiles.requireApproved(entityId, employeeId, payDate.getYear());
    if (!Boolean.TRUE.equals(profile.getResidentThisYear()))
      throw exception(
          cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.PAYROLL_TAX_RESIDENCY_UNCONFIRMED);
    HrmPayrollTaxDeductionDO declaration =
        deductions.source(entityId, employeeId, payDate.getYear(), payDate.getMonthValue());
    if (declaration == null) throw exception(PAYROLL_TAX_INVALID);
    BigDecimal additional =
        deductions.values(declaration).values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    if (additional.compareTo(confirmedAdditional) != 0) throw exception(PAYROLL_TAX_INVALID);
    LocalDate today = LocalDate.now(ZoneId.of("Asia/Shanghai"));
    LocalDate monthEnd=java.time.YearMonth.from(payDate).atEndOfMonth();
    LocalDate asOf = monthEnd.isAfter(today) ? today : monthEnd;
    if (asOf.getYear() != payDate.getYear()) throw exception(PAYROLL_TAX_INVALID);
    HrmPayrollTaxCashLedgerVO ledger =
        cash.forApprovedPayrollForBatch(entityId, employeeId, payDate.getYear(), asOf,batchId);
    PayrollTaxEmploymentEngine.Result months =
        employment.forApprovedPayroll(entityId, employeeId, payDate);
    if (!ledger.isReady() || ledger.getBasicDeduction().compareTo(months.getBasicDeduction()) > 0)
      throw exception(PAYROLL_TAX_INVALID);
    HrmPayrollBonusDO bonus = batchId == null ? null : bonuses.source(batchId, employeeId);
    if (bonus != null
        && (!Objects.equals(bonus.getEntityId(), entityId)
            || !Objects.equals(bonus.getExpectedPaymentDate(), payDate)
            || !Objects.equals(bonus.getProfileId(), profile.getId())))
      throw exception(PAYROLL_TAX_INVALID);
    BigDecimal bonusAmount = bonus == null ? BigDecimal.ZERO : bonus.getAmount();
    String bonusMode = bonus == null ? "NONE" : bonus.getMode();
    boolean separateUsed = Boolean.TRUE.equals(profile.getSeparateBonusUsedElsewhere());
    if ("SEPARATE".equals(bonusMode))
      separateUsed |= bonuses.separateUsed(batchId, profile.getTaxpayerKey(), payDate.getYear());
    HrmPayrollTaxPolicyTrialReqVO request =
        new HrmPayrollTaxPolicyTrialReqVO()
            .setPolicyId(policy.getId())
            .setBasicMonths(months.getMonths().size())
            .setSalaryIncome(ledger.getIncome().add(currentIncome))
            .setSocialDeduction(ledger.getSocialDeduction().add(currentSocial))
            .setAdditionalDeduction(additional)
            .setOtherDeduction(BigDecimal.ZERO)
            .setAlreadyWithheld(ledger.getWithheld())
            .setBonusAmount(bonusAmount)
            .setBonusMode(bonusMode)
            .setSeparateBonusAlreadyUsed(separateUsed);
    return new Source()
        .setBatchId(batchId)
        .setBonus(bonus)
        .setEntityId(entityId)
        .setEmployeeId(employeeId)
        .setExpectedPaymentDate(payDate)
        .setPolicy(policy)
        .setProfile(profile)
        .setDeclaration(declaration)
        .setDeductionValues(new TreeMap<>(deductions.values(declaration)))
        .setPaid(ledger)
        .setEmployment(months)
        .setCurrentIncome(currentIncome)
        .setCurrentSocialDeduction(currentSocial)
        .setAdditionalDeduction(additional)
        .setQuote(PayrollTaxPolicyEngine.quote(policy, request));
  }

  public void validateInputSnapshot(
      String inputSnapshot, Long entityId, LocalDate expectedPaymentDate) {
    Map<String, Object> input = JsonUtils.parseObject(inputSnapshot, Map.class);
    Object contexts = input.get("withholdingContexts");
    if (contexts == null) return;
    if (!(contexts instanceof List)) throw exception(PAYROLL_TAX_INVALID);
    Object entityScope = input.get("entityScope");
    Object batchId = entityScope instanceof Map ? ((Map<?, ?>) entityScope).get("batchId") : null;
    for (Object value : (List<?>) contexts) {
      if (!(value instanceof String)) throw exception(PAYROLL_TAX_INVALID);
      Source source = JsonUtils.parseObject((String) value, Source.class);
      if (source == null
          || !Objects.equals(source.getEntityId(), entityId)
          || (batchId instanceof Number
              && !Objects.equals(source.getBatchId(), ((Number) batchId).longValue()))
          || !Objects.equals(source.getExpectedPaymentDate(), expectedPaymentDate))
        throw exception(PAYROLL_TAX_INVALID);
      validate((String) value);
    }
  }

  public void validate(String snapshot) {
    Source saved;
    try {
      saved = JsonUtils.parseObject(snapshot, Source.class);
    } catch (RuntimeException ex) {
      throw exception(PAYROLL_TAX_INVALID);
    }
    if (saved == null || saved.getPaid() == null) throw exception(PAYROLL_TAX_INVALID);
    Source current =
        calculateForBatch(
            saved.getBatchId(),
            saved.getEntityId(),
            saved.getEmployeeId(),
            saved.getExpectedPaymentDate(),
            saved.getCurrentIncome(),
            saved.getCurrentSocialDeduction(),
            saved.getAdditionalDeduction());
    // Moving to the next calendar day alone does not invalidate unchanged cash evidence.
    // New paid rows, changed amounts, profiles, policies and declarations remain compared.
    current.getPaid().setAsOfDate(saved.getPaid().getAsOfDate());
    if (!Objects.equals(snapshot, JsonUtils.toJsonString(current)))
      throw exception(PAYROLL_TAX_INVALID);
  }
}
