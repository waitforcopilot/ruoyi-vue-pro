package cn.iocoder.yudao.module.hrm.service.payroll;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.PAYROLL_TAX_INVALID;

import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.HrmPayrollEntityMembershipDO;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import lombok.Data;

/** Calendar employment months are counted once per withholding agent and taxpayer. */
public final class PayrollTaxEmploymentEngine {
  private PayrollTaxEmploymentEngine() {}

  @Data
  public static class Result {
    private String mode;
    private List<Integer> months;
    private BigDecimal basicDeduction;
    private List<Long> membershipIds;
  }

  public static Result calculate(
      Long entityId,
      Set<Long> employeeIds,
      LocalDate payDate,
      boolean firstSalary,
      boolean noPriorSalary,
      BigDecimal monthlyBasic,
      List<HrmPayrollEntityMembershipDO> memberships) {
    if (entityId == null
        || employeeIds == null
        || employeeIds.isEmpty()
        || payDate == null
        || payDate.getYear() < 2019
        || payDate.getYear() > 2099
        || memberships == null) throw exception(PAYROLL_TAX_INVALID);
    PayrollTaxPolicyEngine.amount(monthlyBasic);
    SortedSet<Integer> months = new TreeSet<>();
    List<Long> sourceIds = new ArrayList<>();
    LocalDate yearStart = LocalDate.of(payDate.getYear(), 1, 1);
    for (HrmPayrollEntityMembershipDO m : memberships) {
      if (m == null
          || !Objects.equals(m.getEntityId(), entityId)
          || !employeeIds.contains(m.getEmployeeId())
          || !"APPROVED".equals(m.getStatus())) continue;
      if (m.getId() == null
          || m.getEffectiveFrom() == null
          || m.getEffectiveTo() == null
          || m.getEffectiveTo().isBefore(m.getEffectiveFrom()))
        throw exception(PAYROLL_TAX_INVALID);
      LocalDate from = m.getEffectiveFrom().isBefore(yearStart) ? yearStart : m.getEffectiveFrom();
      LocalDate to = m.getEffectiveTo().isAfter(payDate) ? payDate : m.getEffectiveTo();
      if (from.isAfter(to)) continue;
      sourceIds.add(m.getId());
      YearMonth end = YearMonth.from(to);
      for (YearMonth month = YearMonth.from(from); !month.isAfter(end); month = month.plusMonths(1))
        months.add(month.getMonthValue());
    }
    String mode = "EMPLOYMENT_MONTHS";
    if (firstSalary && payDate.getYear() >= 2020) {
      if (!noPriorSalary || months.isEmpty()) throw exception(PAYROLL_TAX_INVALID);
      mode = "QUALIFIED_FIRST_SALARY_FROM_JANUARY";
      int lastEmploymentMonth = months.last();
      for (int month = 1; month <= lastEmploymentMonth; month++) months.add(month);
    }
    Collections.sort(sourceIds);
    return new Result()
        .setMode(mode)
        .setMonths(new ArrayList<>(months))
        .setMembershipIds(sourceIds)
        .setBasicDeduction(monthlyBasic.multiply(BigDecimal.valueOf(months.size())));
  }
}
