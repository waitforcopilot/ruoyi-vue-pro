package cn.iocoder.yudao.module.hrm.service.payroll;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollTaxCashLedgerVO;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollTaxFilingSourceVO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.salary.monthrecord.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.salary.monthrecord.HrmSalaryMonthRecordMapper;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/**
 * Monthly declaration preparation from actual successful cash and immutable withholding evidence.
 */
@Service
public class HrmPayrollTaxFilingSourceService {
  @Resource private HrmPayrollPaymentMapper payments;
  @Resource private HrmPayrollRunMapper runs;
  @Resource private HrmSalaryMonthRecordMapper batches;
  @Resource private HrmPayrollTaxPolicyService policies;
  @Resource private HrmPayrollTaxProfileService profiles;
  @Resource private HrmPayrollTaxCorrectionMapper corrections;
  @Resource private HrmPayrollTaxCashLedgerService cashLedger;
  @Resource private HrmPayrollTaxDeductionService deductions;
  @Resource private HrmPayrollTaxEmploymentService employment;

  private static class Candidate {
    final HrmSalaryMonthEmployeeRecordDO result;
    final HrmPayrollTaxWithholdingService.Source source;
    final Long paymentId;
    final boolean corrected;

    Candidate(
        HrmSalaryMonthEmployeeRecordDO result,
        HrmPayrollTaxWithholdingService.Source source,
        Long paymentId,
        boolean corrected) {
      this.result = result;
      this.source = source;
      this.paymentId = paymentId;
      this.corrected = corrected;
    }
  }

  public static YearMonth period(Integer year, Integer month) {
    if (year == null || year < 2019 || year > 2099 || month == null || month < 1 || month > 12)
      throw exception(PAYROLL_TAX_INVALID);
    YearMonth period = YearMonth.of(year, month);
    if (!period.atEndOfMonth().isBefore(LocalDate.now(ZoneId.of("Asia/Shanghai"))))
      throw exception(PAYROLL_TAX_INVALID);
    return period;
  }

  private String profileBasis(HrmPayrollTaxProfileDO profile) {
    Map<String, Object> basis = new LinkedHashMap<>();
    basis.put("resident", profile.getResidentThisYear());
    basis.put("first", profile.getFirstSalaryThisYear());
    basis.put("noSalary", profile.getNoPriorSalaryThisYear());
    basis.put("noContinuous", profile.getNoPriorContinuousRemunerationBeforeEntry());
    basis.put("openingMode", profile.getOpeningMode());
    basis.put("openingDate", profile.getOpeningAsOfDate());
    basis.put("income", profile.getOpeningIncome());
    basis.put("social", profile.getOpeningSocialDeduction());
    basis.put("basic", profile.getOpeningBasicDeduction());
    basis.put("withheld", profile.getOpeningWithheld());
    return JsonUtils.toJsonString(basis);
  }

  private BigDecimal value(HrmSalaryMonthEmployeeRecordDO result, int code) {
    BigDecimal value = null;
    for (HrmSalaryMonthEmployeeRecordDO.OptionValue option : result.getOptionValues())
      if (Objects.equals(option.getCode(), code)) {
        if (value != null || option.getValue() == null) throw exception(PAYROLL_TAX_INVALID);
        value = option.getValue();
      }
    if (value == null) throw exception(PAYROLL_TAX_INVALID);
    PayrollTaxPolicyEngine.amount(value);
    return value;
  }

  private HrmPayrollTaxFilingSourceVO.Row row(
      Map<String, HrmPayrollTaxFilingSourceVO.Row> rows,
      HrmPayrollTaxProfileDO profile,
      HrmPayrollPaymentDO payment,
      String mode) {
    String key = profile.getTaxpayerKey() + ":" + mode;
    HrmPayrollTaxFilingSourceVO.Row row =
        rows.computeIfAbsent(
            key,
            k ->
                new HrmPayrollTaxFilingSourceVO.Row()
                    .setTaxpayerKey(profile.getTaxpayerKey())
                    .setMode(mode));
    row.setProfileId(profile.getId())
        .setTaxpayerName(profile.getTaxpayerName())
        .setIdentifierType(profile.getIdentifierType())
        .setIdentifierCountry(profile.getIdentifierCountry())
        .setMaskedIdentifier(profile.getMaskedIdentifier());
    if (!row.getEmployeeIds().contains(payment.getEmployeeId()))
      row.getEmployeeIds().add(payment.getEmployeeId());
    row.getPaymentIds().add(payment.getId());
    if (!row.getRunIds().contains(payment.getRunId())) row.getRunIds().add(payment.getRunId());
    return row;
  }

  private void replaceValue(HrmSalaryMonthEmployeeRecordDO result, int code, BigDecimal amount) {
    List<HrmSalaryMonthEmployeeRecordDO.OptionValue> values = new ArrayList<>();
    for (HrmSalaryMonthEmployeeRecordDO.OptionValue value : result.getOptionValues())
      if (!Objects.equals(value.getCode(), code)) values.add(value);
    values.add(new HrmSalaryMonthEmployeeRecordDO.OptionValue().setCode(code).setValue(amount));
    result.setOptionValues(values);
  }

  private HrmPayrollTaxWithholdingService.Source correctedSource(
      HrmPayrollPaymentDO payment,
      HrmSalaryMonthEmployeeRecordDO result,
      HrmPayrollTaxWithholdingService.Source original,
      HrmPayrollTaxPolicyDO policy,
      HrmPayrollTaxCashLedgerVO.Row cash,
      YearMonth period) {
    HrmPayrollTaxCashLedgerVO ledger =
        cashLedger.forApprovedPayroll(
            policy.getEntityId(), payment.getEmployeeId(), period.getYear(), period.atEndOfMonth());
    if (!ledger.isReady()) throw exception(PAYROLL_TAX_INVALID);
    HrmPayrollTaxCashLedgerVO.Row corrected =
        ledger.getRows().stream()
            .filter(r -> Objects.equals(r.getPaymentId(), payment.getId()))
            .findFirst()
            .orElseThrow(() -> exception(PAYROLL_TAX_INVALID));
    if (corrected.getCorrectionId() == null) throw exception(PAYROLL_TAX_INVALID);
    cash.setIncome(corrected.getIncome())
        .setSocialDeduction(corrected.getSocialDeduction())
        .setBasicDeduction(corrected.getBasicDeduction())
        .setWithheld(corrected.getWithheld())
        .setSeparateBonusIncome(corrected.getSeparateBonusIncome())
        .setSeparateBonusTax(corrected.getSeparateBonusTax());
    HrmPayrollTaxProfileDO profile =
        profiles.requireApproved(policy.getEntityId(), payment.getEmployeeId(), period.getYear());
    if (profile.getOpeningAsOfDate() == null
        || !profile.getOpeningAsOfDate().isBefore(period.atDay(1)))
      throw exception(PAYROLL_TAX_INVALID);
    HrmPayrollTaxDeductionDO declaration =
        deductions.source(
            policy.getEntityId(),
            payment.getEmployeeId(),
            period.getYear(),
            period.getMonthValue());
    if (declaration == null) throw exception(PAYROLL_TAX_INVALID);
    Map<String, BigDecimal> values = deductions.values(declaration);
    BigDecimal additional = values.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    PayrollTaxEmploymentEngine.Result months =
        employment.forApprovedPayroll(
            policy.getEntityId(), payment.getEmployeeId(), period.atEndOfMonth());
    if (months == null
        || months.getMonths() == null
        || ledger.getBasicDeduction().compareTo(months.getBasicDeduction()) > 0)
      throw exception(PAYROLL_TAX_INVALID);
    PayrollTaxPolicyEngine.Quote quote =
        PayrollTaxPolicyEngine.quote(
            policy,
            new cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo
                    .HrmPayrollTaxPolicyTrialReqVO()
                .setPolicyId(policy.getId())
                .setBasicMonths(months.getMonths().size())
                .setSalaryIncome(ledger.getIncome())
                .setSocialDeduction(ledger.getSocialDeduction())
                .setAdditionalDeduction(additional)
                .setOtherDeduction(BigDecimal.ZERO)
                .setAlreadyWithheld(ledger.getWithheld())
                .setBonusAmount(BigDecimal.ZERO)
                .setBonusMode("NONE")
                .setSeparateBonusAlreadyUsed(false));
    replaceValue(result, 270101, ledger.getIncome());
    replaceValue(result, 270103, ledger.getSocialDeduction());
    replaceValue(result, 270102, quote.getBasicDeduction());
    replaceValue(result, 270104, additional);
    replaceValue(result, 270105, quote.getSalaryTaxable());
    replaceValue(result, 270106, quote.getCumulativeSalaryTax());
    BigDecimal currentIncome = cash.getIncome();
    if ("MERGED".equals(original.bonusMode()))
      currentIncome = currentIncome.subtract(original.bonusAmount());
    if ("SEPARATE".equals(original.bonusMode())) {
      if (policy.getSeparateBonusValidTo() == null
          || payment.getValueDate().isAfter(policy.getSeparateBonusValidTo()))
        throw exception(PAYROLL_TAX_INVALID);
      PayrollTaxPolicyEngine.Quote bonusQuote =
          PayrollTaxPolicyEngine.quote(
              policy,
              new cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo
                      .HrmPayrollTaxPolicyTrialReqVO()
                  .setPolicyId(policy.getId())
                  .setBasicMonths(0)
                  .setSalaryIncome(BigDecimal.ZERO)
                  .setSocialDeduction(BigDecimal.ZERO)
                  .setAdditionalDeduction(BigDecimal.ZERO)
                  .setOtherDeduction(BigDecimal.ZERO)
                  .setAlreadyWithheld(BigDecimal.ZERO)
                  .setBonusAmount(original.bonusAmount())
                  .setBonusMode("SEPARATE")
                  .setSeparateBonusAlreadyUsed(false));
      quote.setSeparateBonusTax(bonusQuote.getSeparateBonusTax());
    }
    PayrollTaxPolicyEngine.amount(currentIncome);
    quote.setBonusMode(original.bonusMode());
    return new HrmPayrollTaxWithholdingService.Source()
        .setEntityId(policy.getEntityId())
        .setEmployeeId(payment.getEmployeeId())
        .setBatchId(payment.getMonthRecordId())
        .setExpectedPaymentDate(payment.getValueDate())
        .setPolicy(policy)
        .setProfile(profile)
        .setBonus(original.getBonus())
        .setPaid(ledger)
        .setDeclaration(declaration)
        .setEmployment(months)
        .setAdditionalDeduction(additional)
        .setDeductionValues(new TreeMap<>(values))
        .setCurrentIncome(currentIncome)
        .setCurrentSocialDeduction(cash.getSocialDeduction())
        .setQuote(quote);
  }

  /** Internal: the caller must authorize the entity before retrieving this evidence. */
  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public HrmPayrollTaxFilingSourceVO source(Long entityId, Integer year, Integer month) {
    YearMonth period = period(year, month);
    HrmPayrollTaxPolicyDO policy = policies.requireApproved(entityId, year);
    HrmPayrollTaxFilingSourceVO out =
        new HrmPayrollTaxFilingSourceVO()
            .setEntityId(entityId)
            .setYear(year)
            .setMonth(month)
            .setPolicyId(policy.getId())
            .setPolicyVersion(policy.getVersion())
            .setTaxRegistrationNumber(policy.getTaxRegistrationNumber());
    Map<Long, HrmSalaryMonthRecordDO> batchCache = new HashMap<>();
    Map<Long, HrmPayrollRunDO> runCache = new HashMap<>();
    Map<String, HrmPayrollTaxFilingSourceVO.Row> rows = new TreeMap<>();
    Map<HrmPayrollTaxFilingSourceVO.Row, List<Candidate>> candidates = new IdentityHashMap<>();
    List<HrmPayrollPaymentDO> paid =
        payments.selectList(
            new LambdaQueryWrapperX<HrmPayrollPaymentDO>()
                .eq(HrmPayrollPaymentDO::getStatus, "SUCCESS")
                .and(
                    q ->
                        q.isNull(HrmPayrollPaymentDO::getValueDate)
                            .or()
                            .between(
                                HrmPayrollPaymentDO::getValueDate,
                                period.atDay(1),
                                period.atEndOfMonth())));
    paid.sort(
        Comparator.comparing(
                HrmPayrollPaymentDO::getValueDate, Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(HrmPayrollPaymentDO::getId));
    for (HrmPayrollPaymentDO payment : paid) {
      HrmSalaryMonthRecordDO batch =
          batchCache.computeIfAbsent(payment.getMonthRecordId(), batches::selectById);
      if (batch == null) throw exception(PAYROLL_TAX_INVALID);
      if (!Objects.equals(batch.getEntityId(), entityId)) continue;
      if (payment.getValueDate() == null) throw exception(PAYROLL_TAX_INVALID);
      HrmPayrollRunDO run = runCache.computeIfAbsent(payment.getRunId(), runs::selectById);
      HrmPayrollTaxCashLedgerVO.Row cash = new HrmPayrollTaxCashLedgerVO.Row();
      HrmSalaryMonthEmployeeRecordDO result;
      HrmPayrollTaxWithholdingService.Source source;
      HrmPayrollTaxCorrectionDO correction =
          corrections.selectOne(
              new LambdaQueryWrapperX<HrmPayrollTaxCorrectionDO>()
                  .eq(HrmPayrollTaxCorrectionDO::getPaymentId, payment.getId())
                  .eq(HrmPayrollTaxCorrectionDO::getStatus, "APPROVED"));
      try {
        HrmPayrollTaxCashLedgerService.populate(cash, payment, run, correction == null);
        List<HrmSalaryMonthEmployeeRecordDO> results =
            JsonUtils.parseArray(run.getResultSnapshot(), HrmSalaryMonthEmployeeRecordDO.class);
        result =
            results.stream()
                .filter(r -> Objects.equals(r.getEmployeeId(), payment.getEmployeeId()))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
        source =
            JsonUtils.parseObject(
                result.getTaxSourceSnapshot(), HrmPayrollTaxWithholdingService.Source.class);
      } catch (RuntimeException ex) {
        throw exception(PAYROLL_TAX_INVALID);
      }
      if (correction != null) {
        if (source == null || source.getProfile() == null) throw exception(PAYROLL_TAX_INVALID);
        source = correctedSource(payment, result, source, policy, cash, period);
      }
      if (source == null
          || source.getProfile() == null
          || source.getQuote() == null
          || source.getExpectedPaymentDate() == null
          || !Objects.equals(source.getEmployeeId(), payment.getEmployeeId())
          || !Objects.equals(source.getEntityId(), entityId)
          || !period.equals(YearMonth.from(source.getExpectedPaymentDate()))
          || !Boolean.TRUE.equals(source.getProfile().getResidentThisYear())
          || !Objects.equals(
              source.getProfile().getTaxRegistrationNumber(), policy.getTaxRegistrationNumber())
          || !Objects.equals(source.getQuote().getBonusMode(), source.bonusMode()))
        throw exception(PAYROLL_TAX_INVALID);
      HrmPayrollTaxProfileDO profile =
          profiles.requireApproved(entityId, payment.getEmployeeId(), year);
      if (profile.getTaxpayerKey() == null
          || profile.getTaxpayerName() == null
          || !Objects.equals(profile.getTaxpayerKey(), source.getProfile().getTaxpayerKey())
          || !Objects.equals(profile.getTaxRegistrationNumber(), policy.getTaxRegistrationNumber()))
        throw exception(PAYROLL_TAX_INVALID);
      if (!Objects.equals(source.getQuote().getPolicyId(), policy.getId())
          || !Objects.equals(source.getQuote().getPolicyVersion(), policy.getVersion())
          || !Objects.equals(profileBasis(source.getProfile()), profileBasis(profile)))
        throw exception(PAYROLL_VERSION_CONFLICT);
      BigDecimal expectedIncome = source.getCurrentIncome();
      if (expectedIncome == null) throw exception(PAYROLL_TAX_INVALID);
      if ("MERGED".equals(source.bonusMode()))
        expectedIncome = expectedIncome.add(source.bonusAmount());
      if (expectedIncome.compareTo(cash.getIncome()) != 0) throw exception(PAYROLL_TAX_INVALID);
      HrmPayrollTaxFilingSourceVO.Row wage = row(rows, profile, payment, "RESIDENT_WAGE");
      wage.setIncome(wage.getIncome().add(cash.getIncome()))
          .setSocialDeduction(wage.getSocialDeduction().add(cash.getSocialDeduction()))
          .setBasicDeduction(wage.getBasicDeduction().add(cash.getBasicDeduction()))
          .setWithheld(wage.getWithheld().add(cash.getWithheld()))
          .setCumulativeIncome(value(result, 270101))
          .setCumulativeSocialDeduction(value(result, 270103))
          .setCumulativeBasicDeduction(value(result, 270102))
          .setCumulativeAdditionalDeduction(value(result, 270104))
          .setCumulativeTaxable(value(result, 270105))
          .setCumulativeTax(value(result, 270106));
      candidates
          .computeIfAbsent(wage, key -> new ArrayList<>())
          .add(new Candidate(result, source, payment.getId(), correction != null));
      out.setTotalTax(out.getTotalTax().add(cash.getWithheld()));
      if ("SEPARATE".equals(source.bonusMode())) {
        HrmPayrollTaxFilingSourceVO.Row bonus = row(rows, profile, payment, "SEPARATE_BONUS");
        if (bonus.getPaymentIds().size() != 1) throw exception(PAYROLL_TAX_INVALID);
        bonus.setIncome(cash.getSeparateBonusIncome()).setWithheld(cash.getSeparateBonusTax());
        if (correction != null) {
          bonus.getTaxCorrectionIds().add(correction.getId());
          bonus
              .setExpectedBonusWithholding(source.getQuote().getSeparateBonusTax())
              .setBonusTaxBalance(
                  source.getQuote().getSeparateBonusTax().subtract(cash.getSeparateBonusTax()));
        }
        out.setTotalTax(out.getTotalTax().add(cash.getSeparateBonusTax()));
      }
    }
    for (Map.Entry<HrmPayrollTaxFilingSourceVO.Row, List<Candidate>> entry :
        candidates.entrySet()) {
      HrmPayrollTaxFilingSourceVO.Row wage = entry.getKey();
      Candidate complete = null;
      for (Candidate candidate : entry.getValue()) {
        if (candidate.source.getPaid() == null) throw exception(PAYROLL_TAX_INVALID);
        Set<Long> prior = new HashSet<>();
        for (HrmPayrollTaxCashLedgerVO.Row paidRow : candidate.source.getPaid().getRows())
          if (Arrays.asList("COUNTED", "OPENING_COVERED").contains(paidRow.getStatus()))
            prior.add(paidRow.getPaymentId());
        prior.add(candidate.paymentId);
        if (prior.containsAll(wage.getPaymentIds())) complete = candidate;
      }
      if (complete == null) throw exception(PAYROLL_VERSION_CONFLICT);
      for (HrmPayrollTaxCashLedgerVO.Row paidRow : complete.source.getPaid().getRows()) {
        if (paidRow.getCorrectionId() != null
            && !wage.getTaxCorrectionIds().contains(paidRow.getCorrectionId()))
          wage.getTaxCorrectionIds().add(paidRow.getCorrectionId());
      }
      if (!wage.getTaxCorrectionIds().isEmpty()) {
        Collections.sort(wage.getTaxCorrectionIds());
        BigDecimal actual = complete.source.getPaid().getWithheld();
        if (!complete.corrected)
          actual = actual.add(complete.source.getQuote().getCurrentSalaryWithholding());
        BigDecimal priorPeriodActual = actual.subtract(wage.getWithheld());
        PayrollTaxPolicyEngine.amount(priorPeriodActual);
        BigDecimal expectedCurrent =
            value(complete.result, 270106).subtract(priorPeriodActual).max(BigDecimal.ZERO);
        wage.setExpectedSalaryWithholding(expectedCurrent);
        out.setTotalTax(out.getTotalTax().subtract(wage.getWithheld()).add(expectedCurrent));
        wage.setCumulativeActualWithheld(actual)
            .setCumulativeTaxBalance(value(complete.result, 270106).subtract(actual));
      }
      wage.setCumulativeIncome(value(complete.result, 270101))
          .setCumulativeSocialDeduction(value(complete.result, 270103))
          .setCumulativeBasicDeduction(value(complete.result, 270102))
          .setCumulativeAdditionalDeduction(value(complete.result, 270104))
          .setCumulativeTaxable(value(complete.result, 270105))
          .setCumulativeTax(value(complete.result, 270106));
    }
    boolean amended = rows.values().stream().anyMatch(row -> !row.getTaxCorrectionIds().isEmpty());
    if (amended) {
      BigDecimal actual =
          rows.values().stream()
              .map(HrmPayrollTaxFilingSourceVO.Row::getWithheld)
              .reduce(BigDecimal.ZERO, BigDecimal::add);
      for (HrmPayrollTaxFilingSourceVO.Row row : rows.values())
        if (row.getExpectedBonusWithholding() != null)
          out.setTotalTax(
              out.getTotalTax().subtract(row.getWithheld()).add(row.getExpectedBonusWithholding()));
      out.setActualWithheldTotal(actual).setPeriodTaxDifference(out.getTotalTax().subtract(actual));
    }
    out.setRows(new ArrayList<>(rows.values()));
    return out;
  }
}
