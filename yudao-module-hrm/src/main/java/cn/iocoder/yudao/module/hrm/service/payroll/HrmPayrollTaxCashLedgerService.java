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
  @Resource private HrmSalaryMonthRecordMapper batches;

  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public HrmPayrollTaxCashLedgerVO ledger(
      Long entityId, Long employeeId, Integer year, LocalDate asOfDate, Long actor) {
    entities.check(entityId, "READ", actor);
    if (year == null
        || year < 2019
        || year > 2099
        || asOfDate == null
        || asOfDate.getYear() != year
        || asOfDate.isAfter(LocalDate.now(ZoneId.of("Asia/Shanghai"))))
      throw exception(PAYROLL_TAX_INVALID);
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
      if (!Objects.equals(batch.getEntityId(), entityId)) continue;
      LocalDate date = payment.getValueDate();
      if (date != null && (date.getYear() != year || date.isAfter(asOfDate))) continue;
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
        populate(row, payment, run);
      } catch (IllegalArgumentException ex) {
        block(result, row, "SOURCE_INVALID", "冻结结果或累计薪资项不完整/不一致，需核实税务来源");
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

  private static void block(
      HrmPayrollTaxCashLedgerVO result, Row row, String status, String reason) {
    row.setStatus(status).setReason(reason);
    result.getBlockers().add("明细" + row.getPaymentId() + "：" + reason);
  }

  static void populate(Row row, HrmPayrollPaymentDO payment, HrmPayrollRunDO run) {
    if (run == null || !Objects.equals(run.getMonthRecordId(), payment.getMonthRecordId()))
      throw new IllegalArgumentException();
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
  }

  private static BigDecimal delta(Map<Integer, BigDecimal> values, int current, int previous) {
    if (!values.containsKey(current) || !values.containsKey(previous))
      throw new IllegalArgumentException();
    BigDecimal amount = values.get(current).subtract(values.get(previous));
    if (amount.signum() < 0) throw new IllegalArgumentException();
    return amount;
  }
}
