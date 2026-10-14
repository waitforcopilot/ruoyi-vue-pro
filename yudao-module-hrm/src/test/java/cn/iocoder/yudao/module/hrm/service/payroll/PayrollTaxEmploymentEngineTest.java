package cn.iocoder.yudao.module.hrm.service.payroll;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.PAYROLL_TAX_INVALID;
import static org.junit.jupiter.api.Assertions.*;

import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.HrmPayrollEntityMembershipDO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;

class PayrollTaxEmploymentEngineTest {
  private HrmPayrollEntityMembershipDO m(long id, long employee, String from, String to) {
    return new HrmPayrollEntityMembershipDO()
        .setId(id)
        .setEmployeeId(employee)
        .setEntityId(10L)
        .setStatus("APPROVED")
        .setEffectiveFrom(LocalDate.parse(from))
        .setEffectiveTo(LocalDate.parse(to));
  }

  private PayrollTaxEmploymentEngine.Result calculate(
      boolean first, boolean noPrior, HrmPayrollEntityMembershipDO... rows) {
    return PayrollTaxEmploymentEngine.calculate(
        10L,
        new HashSet<>(Arrays.asList(1L, 2L)),
        LocalDate.of(2026, 10, 13),
        first,
        noPrior,
        new BigDecimal("5000"),
        Arrays.asList(rows));
  }

  @Test
  void partialMonthsGapsRehireAndOverlapsCountCalendarMonthsOnlyOnce() {
    PayrollTaxEmploymentEngine.Result r =
        calculate(
            false,
            false,
            m(1, 1, "2025-12-01", "2026-03-01"),
            m(2, 2, "2026-03-20", "2026-04-01"),
            m(3, 2, "2026-08-31", "2027-01-01"));
    assertEquals(Arrays.asList(1, 2, 3, 4, 8, 9, 10), r.getMonths());
    assertEquals(new BigDecimal("35000"), r.getBasicDeduction());
    assertEquals(Arrays.asList(1L, 2L, 3L), r.getMembershipIds());
  }

  @Test
  void unrelatedUnapprovedFutureAndPriorYearDoNotCreateMonths() {
    PayrollTaxEmploymentEngine.Result r =
        calculate(
            false,
            false,
            m(1, 3, "2026-01-01", "2026-12-31"),
            m(2, 1, "2026-01-01", "2026-12-31").setEntityId(20L),
            m(3, 1, "2026-01-01", "2026-12-31").setStatus("PENDING"),
            m(4, 1, "2026-11-01", "2026-12-31"),
            m(5, 1, "2025-01-01", "2025-12-31"));
    assertTrue(r.getMonths().isEmpty());
    assertEquals(BigDecimal.ZERO, r.getBasicDeduction());
  }

  @Test
  void qualifiedFirstSalaryCanStartAtJanuaryButNeedsQualificationAndEmployment() {
    PayrollTaxEmploymentEngine.Result r =
        calculate(true, true, m(1, 1, "2026-09-01", "2026-12-31"));
    assertEquals(10, r.getMonths().size());
    assertEquals(new BigDecimal("50000"), r.getBasicDeduction());
    assertEquals("QUALIFIED_FIRST_SALARY_FROM_JANUARY", r.getMode());
    assertServiceException(
        () -> calculate(true, false, m(1, 1, "2026-09-01", "2026-12-31")), PAYROLL_TAX_INVALID);
    assertServiceException(() -> calculate(true, true), PAYROLL_TAX_INVALID);
  }

  @Test
  void qualifiedFirstSalaryDoesNotExtendDeductionPastLeavingMonth() {
    PayrollTaxEmploymentEngine.Result r =
        calculate(true, true, m(1, 1, "2026-07-01", "2026-08-15"));
    assertEquals(8, r.getMonths().size());
    assertEquals(new BigDecimal("40000"), r.getBasicDeduction());
  }

  @Test
  void firstSalaryQualificationDoesNotApplyBefore2020AndMalformedHistoryIsRejected() {
    PayrollTaxEmploymentEngine.Result r =
        PayrollTaxEmploymentEngine.calculate(
            10L,
            Collections.singleton(1L),
            LocalDate.of(2019, 10, 1),
            true,
            true,
            new BigDecimal("5000"),
            Collections.singletonList(m(1, 1, "2019-09-01", "2019-12-31")));
    assertEquals(Arrays.asList(9, 10), r.getMonths());
    assertEquals("EMPLOYMENT_MONTHS", r.getMode());
    assertServiceException(
        () -> calculate(false, false, m(1, 1, "2026-02-01", "2026-01-01")), PAYROLL_TAX_INVALID);
  }
}
