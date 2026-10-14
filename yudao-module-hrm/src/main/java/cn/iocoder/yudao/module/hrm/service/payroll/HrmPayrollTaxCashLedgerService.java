package cn.iocoder.yudao.module.hrm.service.payroll;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.PAYROLL_TAX_INVALID;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollTaxCashLedgerVO;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollTaxCashLedgerVO.Row;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.salary.monthrecord.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.salary.monthrecord.HrmSalaryMonthRecordMapper;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/** Actual cash-year evidence, never inferred from the nominal payroll month. */
@Service
public class HrmPayrollTaxCashLedgerService {
  @Resource private HrmPayrollEntityService entities;
  @Resource private HrmPayrollTaxProfileService profiles;
  @Resource private HrmPayrollTaxProfileMapper profileMapper;
  @Resource private HrmPayrollPaymentMapper payments;
  @Resource private HrmPayrollRunMapper runs;
  @Resource private HrmPayrollTaxCorrectionMapper corrections;
  @Resource private HrmPayrollTaxPolicyMapper policyMapper;
  @Resource private HrmPayrollTaxSettlementMapper taxSettlements;
  @Resource private HrmPayrollTaxFilingMapper taxFilings;
  @Resource private HrmSalaryMonthRecordMapper batches;

  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public HrmPayrollTaxCashLedgerVO ledger(
      Long entityId, Long employeeId, Integer year, LocalDate asOfDate, Long actor) {
    entities.check(entityId, "READ", actor);
    return forApprovedPayroll(entityId, employeeId, year, asOfDate);
  }

  /** Internal payroll callers must first validate batch/entity and employee scope. */
  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public HrmPayrollTaxCashLedgerVO forApprovedPayroll(
      Long entityId, Long employeeId, Integer year, LocalDate asOfDate) {
    if (year == null
        || year < 2019
        || year > 2099
        || asOfDate == null
        || asOfDate.getYear() != year
        || asOfDate.isAfter(LocalDate.now(ZoneId.of("Asia/Shanghai"))))
      throw exception(PAYROLL_TAX_INVALID);
    return build(entityId, employeeId, year, asOfDate, true);
  }

  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public HrmPayrollTaxCashLedgerVO forTaxFiling(
      Long entityId, Long employeeId, Integer year, LocalDate asOfDate) {
    if (year == null
        || asOfDate == null
        || asOfDate.getYear() != year
        || asOfDate.isAfter(LocalDate.now(ZoneId.of("Asia/Shanghai"))))
      throw exception(PAYROLL_TAX_INVALID);
    return build(entityId, employeeId, year, asOfDate, false);
  }

  private HrmPayrollTaxCashLedgerVO build(
      Long entityId, Long employeeId, Integer year, LocalDate asOfDate, boolean requireClearedTax) {
    HrmPayrollTaxProfileDO profile = profiles.requireApproved(entityId, employeeId, year);
    if (asOfDate.isBefore(profile.getOpeningAsOfDate())) throw exception(PAYROLL_TAX_INVALID);
    Set<Long> employeeIds = new HashSet<>();
    employeeIds.add(employeeId);
    profileMapper
        .selectList(
            new LambdaQueryWrapperX<HrmPayrollTaxProfileDO>()
                .eq(HrmPayrollTaxProfileDO::getEntityId, entityId)
                .eq(HrmPayrollTaxProfileDO::getTaxpayerKey, profile.getTaxpayerKey())
                .eq(
                    HrmPayrollTaxProfileDO::getTaxRegistrationNumber,
                    profile.getTaxRegistrationNumber())
                .in(HrmPayrollTaxProfileDO::getStatus, "APPROVED", "SUPERSEDED"))
        .forEach(p -> employeeIds.add(p.getEmployeeId()));
    HrmPayrollTaxCashLedgerVO result =
        new HrmPayrollTaxCashLedgerVO().setProfileId(profile.getId()).setAsOfDate(asOfDate);
    BigDecimal income = profile.getOpeningIncome(),
        social = profile.getOpeningSocialDeduction(),
        basic = profile.getOpeningBasicDeduction(),
        withheld = profile.getOpeningWithheld();
    BigDecimal coveredIncome = BigDecimal.ZERO,
        coveredSocial = BigDecimal.ZERO,
        coveredBasic = BigDecimal.ZERO,
        coveredTax = BigDecimal.ZERO;
    Map<Long, HrmSalaryMonthRecordDO> batchCache = new HashMap<>();
    Map<Long, HrmPayrollRunDO> runCache = new HashMap<>();
    for (HrmPayrollPaymentDO payment :
        payments.selectList(
            new LambdaQueryWrapperX<HrmPayrollPaymentDO>()
                .in(HrmPayrollPaymentDO::getEmployeeId, employeeIds)
                .eq(HrmPayrollPaymentDO::getStatus, "SUCCESS")
                .orderByAsc(HrmPayrollPaymentDO::getId))) {
      HrmSalaryMonthRecordDO batch =
          batchCache.computeIfAbsent(payment.getMonthRecordId(), batches::selectById);
      if (batch == null) {
        result.getBlockers().add("成功明细" + payment.getId() + "缺少工资批次");
        continue;
      }
      LocalDate date = payment.getValueDate();
      if (date != null && (date.getYear() != year || date.isAfter(asOfDate))) continue;
      if (batch.getEntityId() == null) {
        result.getBlockers().add("存在未绑定扣缴主体的历史成功明细，需先核实主体归属");
        continue;
      }
      if (!Objects.equals(batch.getEntityId(), entityId)) continue;
      Row row =
          new Row()
              .setPaymentId(payment.getId())
              .setBatchId(batch.getId())
              .setRunId(payment.getRunId())
              .setEmployeeId(payment.getEmployeeId())
              .setValueDate(date)
              .setSalaryYear(batch.getYear())
              .setSalaryMonth(batch.getMonth())
              .setNet(payment.getAmount());
      result.getRows().add(row);
      if (date == null) {
        block(result, row, "UNDATED", "历史成功明细缺少实际到账日，不能推断税务年度");
        continue;
      }
      HrmPayrollRunDO run = runCache.computeIfAbsent(payment.getRunId(), runs::selectById);
      try {
        HrmPayrollTaxCorrectionDO correction =
            corrections.selectOne(
                new LambdaQueryWrapperX<HrmPayrollTaxCorrectionDO>()
                    .eq(HrmPayrollTaxCorrectionDO::getPaymentId, payment.getId())
                    .eq(HrmPayrollTaxCorrectionDO::getStatus, "APPROVED"));
        if (run != null && correction == null) {
          List<HrmSalaryMonthEmployeeRecordDO> frozen =
              JsonUtils.parseArray(run.getResultSnapshot(), HrmSalaryMonthEmployeeRecordDO.class);
          for (HrmSalaryMonthEmployeeRecordDO resultRow : frozen)
            if (Objects.equals(resultRow.getEmployeeId(), payment.getEmployeeId())
                && resultRow.getTaxSourceSnapshot() != null) {
              HrmPayrollTaxWithholdingService.Source source =
                  JsonUtils.parseObject(
                      resultRow.getTaxSourceSnapshot(),
                      HrmPayrollTaxWithholdingService.Source.class);
              if (source == null
                  || source.getProfile() == null
                  || !Objects.equals(
                      source.getProfile().getTaxRegistrationNumber(),
                      profile.getTaxRegistrationNumber()))
                throw new IllegalArgumentException("TAX_SUBJECT_MISMATCH");
            }
        }
        populate(row, payment, run, correction == null);
        if (correction != null) {
          HrmPayrollTaxProfileDO correctedProfile =
              profiles.requireApproved(entityId, payment.getEmployeeId(), year);
          HrmPayrollTaxPolicyDO correctedPolicy = policyMapper.selectById(correction.getPolicyId());
          if (!Objects.equals(correction.getEntityId(), entityId)
              || !Objects.equals(correction.getProfileId(), correctedProfile.getId())
              || !Objects.equals(correction.getTaxpayerKey(), profile.getTaxpayerKey())
              || !Objects.equals(
                  correction.getTaxRegistrationNumber(), profile.getTaxRegistrationNumber())
              || correctedPolicy == null
              || !"APPROVED".equals(correctedPolicy.getStatus())
              || !Objects.equals(
                  correction.getSourceDigest(), HrmPayrollTaxCorrectionService.digest(payment, run))
              || correction.getIncome().compareTo(row.getIncome()) != 0
              || correction.getSocialDeduction().compareTo(row.getSocialDeduction()) != 0
              || correction.getActualSalaryWithheld().compareTo(row.getWithheld()) != 0
              || correction.getSeparateBonusIncome().compareTo(row.getSeparateBonusIncome()) != 0
              || correction.getActualBonusWithheld().compareTo(row.getSeparateBonusTax()) != 0)
            throw new IllegalArgumentException("TAX_CORRECTION_STALE");
          row.setBasicDeduction(correction.getBasicDeduction()).setCorrectionId(correction.getId());
        }
      } catch (RuntimeException ex) {
        if ("TAX_SUBJECT_MISMATCH".equals(ex.getMessage()))
          block(result, row, "TAX_SUBJECT_MISMATCH", "冻结扣缴单位与当前税务档案不符，需办理主体迁移或税务更正");
        else if ("TAX_PERIOD_MISMATCH".equals(ex.getMessage()))
          block(result, row, "TAX_PERIOD_MISMATCH", "实际到账税务月份与冻结核算月份不符，需办理税务更正");
        else if ("TAX_CORRECTION_STALE".equals(ex.getMessage()))
          block(result, row, "TAX_CORRECTION_STALE", "批准税务更正的政策、档案或已发事实已变更，需重新核实更正版本");
        else block(result, row, "SOURCE_INVALID", "冻结结果或累计薪资项不完整/不一致，需核实税务来源");
        continue;
      }
      if ("DECLARE".equals(profile.getOpeningMode())
          && !date.isAfter(profile.getOpeningAsOfDate())) {
        row.setStatus("OPENING_COVERED").setReason("已由批准历史累计资料覆盖，不再次相加");
        coveredIncome = coveredIncome.add(row.getIncome());
        coveredSocial = coveredSocial.add(row.getSocialDeduction());
        coveredBasic = coveredBasic.add(row.getBasicDeduction());
        coveredTax = coveredTax.add(row.getWithheld());
      } else {
        row.setStatus("COUNTED");
        income = income.add(row.getIncome());
        social = social.add(row.getSocialDeduction());
        basic = basic.add(row.getBasicDeduction());
        withheld = withheld.add(row.getWithheld());
      }
    }
    for (HrmPayrollTaxSettlementDO settlement :
        taxSettlements.selectList(
            new LambdaQueryWrapperX<HrmPayrollTaxSettlementDO>()
                .eq(HrmPayrollTaxSettlementDO::getEntityId, entityId)
                .eq(HrmPayrollTaxSettlementDO::getYear, year)
                .eq(HrmPayrollTaxSettlementDO::getTaxpayerKey, profile.getTaxpayerKey())
                .eq(HrmPayrollTaxSettlementDO::getStatus, "SETTLED")
                .le(HrmPayrollTaxSettlementDO::getValueDate, asOfDate)
                .orderByAsc(HrmPayrollTaxSettlementDO::getId))) {
      if (settlement.getValueDate() == null
          || settlement.getAmount() == null
          || settlement.getAmount().signum() <= 0
          || !Arrays.asList("REFUND", "RECOVERY").contains(settlement.getKind())
          || settlement.getBankReference() == null) {
        result.getBlockers().add("税款结算" + settlement.getId() + "实际收付款依据不完整");
        continue;
      }
      BigDecimal tax = HrmPayrollTaxSettlementService.signed(settlement);
      Row adjustment =
          new Row()
              .setTaxSettlementId(settlement.getId())
              .setTaxOriginYear(settlement.getYear())
              .setTaxOriginMonth(settlement.getMonth())
              .setTaxSettlementMode(settlement.getMode())
              .setEmployeeId(settlement.getEmployeeId())
              .setValueDate(settlement.getValueDate())
              .setIncome(BigDecimal.ZERO)
              .setSocialDeduction(BigDecimal.ZERO)
              .setBasicDeduction(BigDecimal.ZERO)
              .setNet(tax.negate())
              .setSeparateBonusIncome(BigDecimal.ZERO)
              .setSeparateBonusTax(
                  "SEPARATE_BONUS".equals(settlement.getMode()) ? tax : BigDecimal.ZERO)
              .setWithheld("RESIDENT_WAGE".equals(settlement.getMode()) ? tax : BigDecimal.ZERO);
      if ("DECLARE".equals(profile.getOpeningMode())
          && !settlement.getValueDate().isAfter(profile.getOpeningAsOfDate())) {
        adjustment.setStatus("OPENING_COVERED").setReason("批准历史累计已包含实际税款结算，不重复相加");
        coveredTax = coveredTax.add(adjustment.getWithheld());
      } else {
        adjustment.setStatus("COUNTED").setReason("独立复核后按实际收付款确认的税款结算");
        withheld = withheld.add(adjustment.getWithheld());
      }
      result.getRows().add(adjustment);
    }
    if (withheld.signum() < 0 || coveredTax.signum() < 0)
      result.getBlockers().add("税款返还超过该年度已核实工资预缴，请核实历史累计及结算归属");
    if (requireClearedTax) checkTaxClearing(result, entityId, year, profile);
    if (coveredIncome.compareTo(profile.getOpeningIncome()) > 0
        || coveredSocial.compareTo(profile.getOpeningSocialDeduction()) > 0
        || coveredBasic.compareTo(profile.getOpeningBasicDeduction()) > 0
        || coveredTax.compareTo(profile.getOpeningWithheld()) > 0)
      result.getBlockers().add("历史累计资料低于截止日内已发工资证据，需更正档案");
    result.setReady(result.getBlockers().isEmpty());
    if (result.isReady())
      result
          .setIncome(income)
          .setSocialDeduction(social)
          .setBasicDeduction(basic)
          .setWithheld(withheld);
    return result;
  }

  private void checkTaxClearing(
      HrmPayrollTaxCashLedgerVO result,
      Long entityId,
      Integer year,
      HrmPayrollTaxProfileDO profile) {
    for (HrmPayrollTaxFilingDO filing :
        taxFilings.selectList(
            new LambdaQueryWrapperX<HrmPayrollTaxFilingDO>()
                .eq(HrmPayrollTaxFilingDO::getEntityId, entityId)
                .eq(HrmPayrollTaxFilingDO::getYear, year)
                .eq(HrmPayrollTaxFilingDO::getStatus, "RECONCILED"))) {
      if (filing.getFiledDate() != null && filing.getFiledDate().isAfter(result.getAsOfDate()))
        continue;
      cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollTaxFilingSourceVO source =
          JsonUtils.parseObject(
              filing.getSourceSnapshot(),
              cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollTaxFilingSourceVO
                  .class);
      for (cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollTaxFilingSourceVO.Row
          worker : source.getRows()) {
        HrmPayrollTaxProfileDO owner = profileMapper.selectById(worker.getProfileId());
        if (owner == null || !Objects.equals(owner.getTaxpayerKey(), profile.getTaxpayerKey()))
          continue;
        BigDecimal expected =
            "RESIDENT_WAGE".equals(worker.getMode())
                ? worker.getExpectedSalaryWithholding()
                : worker.getExpectedBonusWithholding();
        if (expected == null) expected = worker.getWithheld();
        BigDecimal difference = expected.subtract(worker.getWithheld());
        for (HrmPayrollTaxSettlementDO settled :
            taxSettlements.selectList(
                new LambdaQueryWrapperX<HrmPayrollTaxSettlementDO>()
                    .eq(HrmPayrollTaxSettlementDO::getEntityId, entityId)
                    .eq(HrmPayrollTaxSettlementDO::getYear, year)
                    .eq(HrmPayrollTaxSettlementDO::getMonth, filing.getMonth())
                    .eq(HrmPayrollTaxSettlementDO::getMode, worker.getMode())
                    .eq(HrmPayrollTaxSettlementDO::getTaxpayerKey, profile.getTaxpayerKey())
                    .eq(HrmPayrollTaxSettlementDO::getStatus, "SETTLED")))
          difference = difference.subtract(HrmPayrollTaxSettlementService.signed(settled));
        if (difference.signum() != 0)
          result
              .getBlockers()
              .add("申报" + filing.getId() + "的" + worker.getMode() + "税款收退差额尚未结清，先核实员工税款结算");
      }
    }
  }

  private static void block(
      HrmPayrollTaxCashLedgerVO result, Row row, String status, String reason) {
    row.setStatus(status).setReason(reason);
    result.getBlockers().add("明细" + row.getPaymentId() + "：" + reason);
  }

  static void populate(Row row, HrmPayrollPaymentDO payment, HrmPayrollRunDO run) {
    populate(row, payment, run, true);
  }

  static void populate(
      Row row, HrmPayrollPaymentDO payment, HrmPayrollRunDO run, boolean enforcePeriod) {
    if (run == null || !Objects.equals(run.getMonthRecordId(), payment.getMonthRecordId()))
      throw new IllegalArgumentException();
    if (payment.getAmount() != null
        && payment.getAmount().signum() == 0
        && (payment.getZeroSettlementReference() == null
            || payment.getZeroSettlementReference().trim().isEmpty()))
      throw new IllegalArgumentException("ZERO_SETTLEMENT_UNVERIFIED");
    List<HrmSalaryMonthEmployeeRecordDO> records;
    try {
      records = JsonUtils.parseArray(run.getResultSnapshot(), HrmSalaryMonthEmployeeRecordDO.class);
    } catch (RuntimeException ex) {
      throw new IllegalArgumentException(ex);
    }
    if (records == null) throw new IllegalArgumentException();
    List<HrmSalaryMonthEmployeeRecordDO> matching = new ArrayList<>();
    records.stream()
        .filter(r -> Objects.equals(r.getEmployeeId(), payment.getEmployeeId()))
        .forEach(matching::add);
    if (matching.size() != 1) throw new IllegalArgumentException();
    HrmSalaryMonthEmployeeRecordDO r = matching.get(0);
    if (enforcePeriod
        && r.getTaxPaymentDate() != null
        && payment.getValueDate() != null
        && !java.time.YearMonth.from(r.getTaxPaymentDate())
            .equals(java.time.YearMonth.from(payment.getValueDate())))
      throw new IllegalArgumentException("TAX_PERIOD_MISMATCH");
    if (r.getRealPaySalary() == null
        || payment.getAmount() == null
        || r.getRealPaySalary().compareTo(payment.getAmount()) != 0
        || r.getPersonalTax() == null
        || r.getPersonalTax().signum() < 0
        || r.getOptionValues() == null) throw new IllegalArgumentException();
    Map<Integer, BigDecimal> values = new HashMap<>();
    for (HrmSalaryMonthEmployeeRecordDO.OptionValue v : r.getOptionValues()) {
      if (v.getCode() == null
          || v.getValue() == null
          || values.put(v.getCode(), v.getValue()) != null) throw new IllegalArgumentException();
    }
    row.setIncome(delta(values, 270101, 250101))
        .setSocialDeduction(delta(values, 270103, 250103))
        .setBasicDeduction(delta(values, 270102, 250102))
        .setWithheld(r.getPersonalTax());
    row.setSeparateBonusIncome(BigDecimal.ZERO).setSeparateBonusTax(BigDecimal.ZERO);
    if (r.getTaxSourceSnapshot() != null) {
      HrmPayrollTaxWithholdingService.Source source =
          JsonUtils.parseObject(
              r.getTaxSourceSnapshot(), HrmPayrollTaxWithholdingService.Source.class);
      if (source == null
          || source.getQuote() == null
          || source.getQuote().getCurrentWithholding() == null
          || source.getQuote().getCurrentWithholding().compareTo(r.getPersonalTax()) != 0
          || source.getQuote().getCurrentSalaryWithholding() == null
          || source.getQuote().getSeparateBonusTax() == null
          || source
                  .getQuote()
                  .getCurrentSalaryWithholding()
                  .add(source.getQuote().getSeparateBonusTax())
                  .compareTo(r.getPersonalTax())
              != 0) throw new IllegalArgumentException();
      row.setWithheld(source.getQuote().getCurrentSalaryWithholding());
      if ("SEPARATE".equals(source.bonusMode())) {
        if (source.bonusAmount().signum() <= 0) throw new IllegalArgumentException();
        row.setSeparateBonusIncome(source.bonusAmount())
            .setSeparateBonusTax(source.getQuote().getSeparateBonusTax());
      } else if (source.getQuote().getSeparateBonusTax().signum() != 0)
        throw new IllegalArgumentException();
    }
  }

  private static BigDecimal delta(Map<Integer, BigDecimal> values, int current, int previous) {
    if (!values.containsKey(current) || !values.containsKey(previous))
      throw new IllegalArgumentException();
    BigDecimal amount = values.get(current).subtract(values.get(previous));
    if (amount.signum() < 0) throw new IllegalArgumentException();
    return amount;
  }
}
