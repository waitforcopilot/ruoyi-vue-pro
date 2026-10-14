package cn.iocoder.yudao.module.hrm.service.payroll;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.HrmPayrollTaxProfileMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import javax.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

@Import(HrmPayrollTaxEmploymentService.class)
class HrmPayrollTaxEmploymentServiceTest extends BaseDbUnitTest {
  @Resource private HrmPayrollTaxEmploymentService service;
  @Resource private HrmPayrollTaxProfileMapper mapper;
  @MockBean private HrmPayrollEntityService entities;
  @MockBean private HrmPayrollTaxProfileService profiles;
  @MockBean private HrmPayrollTaxPolicyService policies;
  private final LocalDate payDate = LocalDate.of(2026, 10, 14);

  @BeforeEach
  void setup() {
    when(profiles.requireApproved(10L, 1L, 2026))
        .thenReturn(
            new HrmPayrollTaxProfileDO()
                .setEmployeeId(1L)
                .setTaxpayerKey("same-person")
                .setTaxRegistrationNumber("agent")
                .setFirstSalaryThisYear(false)
                .setNoPriorSalaryThisYear(false));
    when(policies.requireApproved(10L, 2026))
        .thenReturn(new HrmPayrollTaxPolicyDO().setMonthlyBasicDeduction(new BigDecimal("5000")));
  }

  private HrmPayrollEntityMembershipDO membership(long id, long employee, String from, String to) {
    return new HrmPayrollEntityMembershipDO()
        .setId(id)
        .setEntityId(10L)
        .setEmployeeId(employee)
        .setStatus("APPROVED")
        .setEffectiveFrom(LocalDate.parse(from))
        .setEffectiveTo(LocalDate.parse(to));
  }

  @Test
  void approvedHistoricalIdentityCombinesRehireMonthsWithinThisAgent() {
    mapper.insert(
        new HrmPayrollTaxProfileDO()
            .setEntityId(10L)
            .setEmployeeId(2L)
            .setYear(2025)
            .setVersion(1)
            .setTaxpayerKey("same-person")
            .setTaxRegistrationNumber("agent")
            .setStatus("SUPERSEDED"));
    mapper.insert(
        new HrmPayrollTaxProfileDO()
            .setEntityId(20L)
            .setEmployeeId(3L)
            .setYear(2025)
            .setVersion(1)
            .setTaxpayerKey("same-person")
            .setTaxRegistrationNumber("other-agent")
            .setStatus("APPROVED"));
    when(entities.effectiveMemberships(10L, LocalDate.of(2026, 1, 1), payDate))
        .thenReturn(
            Arrays.asList(
                membership(1, 1, "2026-08-15", "2026-12-31"),
                membership(2, 2, "2026-01-01", "2026-03-01"),
                membership(3, 3, "2026-04-01", "2026-07-31")));
    PayrollTaxEmploymentEngine.Result result = service.deduction(10L, 1L, payDate, 8L);
    assertEquals(Arrays.asList(1, 2, 3, 8, 9, 10), result.getMonths());
    assertEquals(new BigDecimal("30000"), result.getBasicDeduction());
    verify(entities).check(10L, "READ", 8L);
  }

  @Test
  void independentProfileQualificationUsesApprovedPolicyAndFuturePlanningDoesNotChangeSources() {
    when(profiles.requireApproved(10L, 1L, 2026))
        .thenReturn(
            new HrmPayrollTaxProfileDO()
                .setEmployeeId(1L)
                .setTaxpayerKey("same-person")
                .setTaxRegistrationNumber("agent")
                .setFirstSalaryThisYear(true)
                .setNoPriorSalaryThisYear(true));
    when(entities.effectiveMemberships(10L, LocalDate.of(2026, 1, 1), payDate))
        .thenReturn(Collections.singletonList(membership(1, 1, "2026-08-15", "2026-12-31")));
    assertEquals(10, service.deduction(10L, 1L, payDate, 8L).getMonths().size());
    verify(policies).requireApproved(10L, 2026);
    doThrow(new IllegalStateException("denied")).when(entities).check(10L, "READ", 9L);
    assertThrows(IllegalStateException.class, () -> service.deduction(10L, 1L, payDate, 9L));
  }
}
