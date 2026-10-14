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

  private static class Candidate {
    final HrmSalaryMonthEmployeeRecordDO result;
    final HrmPayrollTaxWithholdingService.Source source;
    final Long paymentId;

    Candidate(
        HrmSalaryMonthEmployeeRecordDO result,
        HrmPayrollTaxWithholdingService.Source source,
        Long paymentId) {
      this.result = result;
      this.source = source;
      this.paymentId = paymentId;
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
      try {
        HrmPayrollTaxCashLedgerService.populate(cash, payment, run);
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
          .add(new Candidate(result, source, payment.getId()));
      out.setTotalTax(out.getTotalTax().add(cash.getWithheld()));
      if ("SEPARATE".equals(source.bonusMode())) {
        HrmPayrollTaxFilingSourceVO.Row bonus = row(rows, profile, payment, "SEPARATE_BONUS");
        if (bonus.getPaymentIds().size() != 1) throw exception(PAYROLL_TAX_INVALID);
        bonus.setIncome(cash.getSeparateBonusIncome()).setWithheld(cash.getSeparateBonusTax());
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
      wage.setCumulativeIncome(value(complete.result, 270101))
          .setCumulativeSocialDeduction(value(complete.result, 270103))
          .setCumulativeBasicDeduction(value(complete.result, 270102))
          .setCumulativeAdditionalDeduction(value(complete.result, 270104))
          .setCumulativeTaxable(value(complete.result, 270105))
          .setCumulativeTax(value(complete.result, 270106));
    }
    out.setRows(new ArrayList<>(rows.values()));
    return out;
  }
}
