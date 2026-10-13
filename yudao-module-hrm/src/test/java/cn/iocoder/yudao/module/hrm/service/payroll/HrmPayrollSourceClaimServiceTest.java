package cn.iocoder.yudao.module.hrm.service.payroll;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.salary.monthrecord.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.employee.info.HrmEmployeeMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.salary.monthrecord.HrmSalaryMonthRecordMapper;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import javax.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

@Import(HrmPayrollSourceClaimService.class)
class HrmPayrollSourceClaimServiceTest extends BaseDbUnitTest {
  @Resource private HrmPayrollSourceClaimService service;
  @Resource private HrmPayrollSourceClaimMapper claims;
  @Resource private HrmPayrollRunMapper runs;
  @Resource private HrmSalaryMonthRecordMapper batches;
  @Resource private HrmEmployeeMapper employees;
  @MockBean private HrmPayrollScopeService scope;
  private Long employee;

  @BeforeEach
  void setup() {
    HrmEmployeeDO e = new HrmEmployeeDO().setName("来源员工").setJobNumber("CLAIM-1");
    employees.insert(e);
    employee = e.getId();
  }

  private LocalDate day(int d) {
    return LocalDate.of(2026, 9, d);
  }

  private HrmSalaryMonthRecordDO batch(Long entity) {
    HrmSalaryMonthRecordDO b =
        new HrmSalaryMonthRecordDO()
            .setEntityId(entity)
            .setBatchKey("REGULAR")
            .setYear(2026)
            .setMonth(9)
            .setTitle("来源防重测试")
            .setStatus(14)
            .setStartTime(day(1).atStartOfDay())
            .setEndTime(day(30).atStartOfDay());
    batches.insert(b);
    return b;
  }

  private HrmPayrollRunDO run(
      HrmSalaryMonthRecordDO b, List<LocalDate> dates, boolean overtime, boolean fees) {
    HrmPayrollInputDO confirmation = new HrmPayrollInputDO();
    confirmation.setKind("OVERTIME");
    Map<String, Object> sources = new LinkedHashMap<>();
    List<HrmPayrollTimeEntryDO> entries = new ArrayList<>();
    if (overtime)
      entries.add(
          new HrmPayrollTimeEntryDO()
              .setEmployeeId(employee)
              .setSourceApplyId(123L)
              .setSourceRecordId("123:" + dates.get(0))
              .setStartTime(dates.get(0).atTime(18, 0))
              .setEndTime(dates.get(0).atTime(20, 0))
              .setAmount(new BigDecimal("100.00")));
    sources.put("entries", entries);
    sources.put("cycles", Collections.emptyList());
    confirmation.setSourceSnapshot(JsonUtils.toJsonString(sources));
    HrmPayrollInsurancePolicyService.Quote quote = new HrmPayrollInsurancePolicyService.Quote();
    quote.setEntityId(b.getEntityId());
    quote.setEmployeeId(employee);
    quote.setPayerEntityId(b.getEntityId());
    quote.setYear(2026);
    quote.setMonth(9);
    quote.setCoverage(fees ? "INSURED" : "EXEMPT");
    if (fees) quote.setPersonalInsurance(new BigDecimal("100.00"));
    Map<String, Object> input = new LinkedHashMap<>();
    input.put("entityPayrollDates", Collections.singletonMap(employee, dates));
    input.put(
        "approvedInputs",
        Collections.singletonMap(employee, Collections.singletonMap("OVERTIME", confirmation)));
    input.put("insurancePolicySources", Collections.singletonMap(employee, quote));
    HrmPayrollRunDO r = new HrmPayrollRunDO();
    r.setMonthRecordId(b.getId());
    r.setVersion(1);
    r.setComputedBy(1L);
    r.setInputSnapshot(JsonUtils.toJsonString(input));
    r.setRuleSnapshot("{}");
    r.setResultSnapshot(
        JsonUtils.toJsonString(
            Collections.singletonList(
                HrmSalaryMonthEmployeeRecordDO.builder()
                    .employeeId(employee)
                    .entityId(b.getEntityId())
                    .monthRecordId(b.getId())
                    .realPaySalary(new BigDecimal("1000"))
                    .build())));
    runs.insert(r);
    return r;
  }

  @Test
  void freezeClaimsAllSourcesAndRetryDoesNotCreateDuplicates() {
    HrmSalaryMonthRecordDO b = batch(1L);
    HrmPayrollRunDO r = run(b, Arrays.asList(day(1), day(2)), true, true);
    service.reserve(b, r, 3L);
    assertEquals(4, claims.selectList().size());
    service.reserve(b, r, 3L);
    assertEquals(4, claims.selectList().size());
    service.assertReserved(b.getId(), r.getId());
  }

  @Test
  void overlappingWageDatesAcrossBatchesRollbackEveryNewClaim() {
    HrmSalaryMonthRecordDO a = batch(1L), b = batch(2L);
    HrmPayrollRunDO first = run(a, Collections.singletonList(day(2)), false, false),
        next = run(b, Arrays.asList(day(1), day(2)), false, false);
    service.reserve(a, first, 3L);
    assertServiceException(() -> service.reserve(b, next, 3L), PAYROLL_SOURCE_CLAIM_INVALID);
    assertEquals(1, claims.selectList().size());
    assertEquals(a.getId(), claims.selectList().get(0).getBatchId());
  }

  @Test
  void sameInsuranceMonthCannotBeChargedTwiceEvenWithDisjointWageDates() {
    HrmSalaryMonthRecordDO a = batch(1L), b = batch(2L);
    HrmPayrollRunDO first = run(a, Collections.singletonList(day(1)), false, true),
        next = run(b, Collections.singletonList(day(2)), false, true);
    service.reserve(a, first, 3L);
    assertServiceException(() -> service.reserve(b, next, 3L), PAYROLL_SOURCE_CLAIM_INVALID);
    assertEquals(2, claims.selectList().size());
  }

  @Test
  void unfreezeReleasesReservationsButKeepsAuditAndAllowsNewRun() {
    HrmSalaryMonthRecordDO a = batch(1L);
    HrmPayrollRunDO first = run(a, Collections.singletonList(day(1)), false, false);
    service.reserve(a, first, 3L);
    a.setStatus(15);
    batches.updateById(a);
    service.release(a.getId(), first.getId(), "修订数据重新试算", 4L);
    assertEquals("RELEASED", claims.selectList().get(0).getStatus());
    assertEquals(4L, claims.selectList().get(0).getReleasedBy());
    HrmSalaryMonthRecordDO b = batch(2L);
    HrmPayrollRunDO next = run(b, Collections.singletonList(day(1)), false, false);
    service.reserve(b, next, 3L);
    assertEquals(2, claims.selectList().size());
    service.assertReserved(b.getId(), next.getId());
  }

  @Test
  void successfulPaymentsKeepClaimsAndCannotBeReleased() {
    HrmSalaryMonthRecordDO a = batch(1L);
    HrmPayrollRunDO first = run(a, Collections.singletonList(day(1)), true, false);
    service.reserve(a, first, 3L);
    service.paid(a.getId(), first.getId(), employee);
    assertTrue(claims.selectList().stream().allMatch(c -> "PAID".equals(c.getStatus())));
    a.setStatus(15);
    batches.updateById(a);
    assertServiceException(
        () -> service.release(a.getId(), first.getId(), "不能重复发放", 4L),
        PAYROLL_SOURCE_CLAIM_INVALID);
    HrmSalaryMonthRecordDO b = batch(2L);
    HrmPayrollRunDO next = run(b, Collections.singletonList(day(1)), true, false);
    assertServiceException(() -> service.reserve(b, next, 3L), PAYROLL_SOURCE_CLAIM_INVALID);
  }

  @Test
  void manualAdjustmentRunClaimsOriginalInputsButReferencesFinalVersion() {
    HrmSalaryMonthRecordDO a = batch(1L);
    HrmPayrollRunDO base = run(a, Collections.singletonList(day(1)), true, false);
    HrmPayrollRunDO adjusted = new HrmPayrollRunDO();
    adjusted.setMonthRecordId(a.getId());
    adjusted.setVersion(2);
    adjusted.setComputedBy(1L);
    adjusted.setInputSnapshot(
        JsonUtils.toJsonString(Collections.singletonMap("sourceRunId", base.getId())));
    adjusted.setRuleSnapshot("{}");
    adjusted.setResultSnapshot(base.getResultSnapshot());
    runs.insert(adjusted);
    service.reserve(a, adjusted, 3L);
    assertTrue(claims.selectList().stream().allMatch(c -> adjusted.getId().equals(c.getRunId())));
    service.assertReserved(a.getId(), adjusted.getId());
  }

  @Test
  void preparedPaymentRequiresCompleteReservations() {
    HrmSalaryMonthRecordDO a = batch(1L);
    HrmPayrollRunDO r = run(a, Collections.singletonList(day(1)), false, false);
    assertServiceException(
        () -> service.assertReserved(a.getId(), r.getId()), PAYROLL_SOURCE_CLAIM_INVALID);
  }

  @Test
  void completedComprehensiveCycleIsClaimedWithItsOriginalPeriod() {
    HrmSalaryMonthRecordDO b = batch(1L);
    HrmPayrollRunDO r = run(b, Collections.singletonList(day(2)), false, false);
    Map<String, Object> data = JsonUtils.parseObject(r.getInputSnapshot(), Map.class);
    Map<String, Object> confirmations =
        (Map<String, Object>)
            ((Map<?, ?>) data.get("approvedInputs")).get(String.valueOf(employee));
    HrmPayrollInputDO input =
        JsonUtils.parseObject(
            JsonUtils.toJsonString(confirmations.get("OVERTIME")), HrmPayrollInputDO.class);
    HrmPayrollWorkCycleDO cycle =
        new HrmPayrollWorkCycleDO()
            .setEmployeeId(employee)
            .setCycleFrom(LocalDate.of(2026, 7, 3))
            .setCycleTo(day(2))
            .setAmount(new BigDecimal("200.00"));
    input.setSourceSnapshot(
        JsonUtils.toJsonString(
            new java.util.LinkedHashMap<String, Object>() {
              {
                put("entries", Collections.emptyList());
                put("cycles", Collections.singletonList(cycle));
              }
            }));
    confirmations.put("OVERTIME", input);
    r.setInputSnapshot(JsonUtils.toJsonString(data));
    runs.updateById(r);
    service.reserve(b, r, 3L);
    HrmPayrollSourceClaimDO claimed =
        claims.selectList().stream()
            .filter(c -> "WORK_CYCLE".equals(c.getKind()))
            .findFirst()
            .get();
    assertEquals(employee + ":2026-07-03:2026-09-02", claimed.getSourceKey());
    assertEquals(0, new BigDecimal("200").compareTo(claimed.getAmount()));
  }

  @Test
  void partialEmployeePaymentKeepsOtherReservationsAndBlocksUnfreeze() {
    HrmEmployeeDO other = new HrmEmployeeDO().setName("第二员工").setJobNumber("CLAIM-2");
    employees.insert(other);
    HrmSalaryMonthRecordDO b = batch(1L);
    HrmPayrollRunDO r = run(b, Collections.singletonList(day(1)), false, false);
    Map<String, Object> data = JsonUtils.parseObject(r.getInputSnapshot(), Map.class);
    Map<String, Object> dates = (Map<String, Object>) data.get("entityPayrollDates");
    dates.put(String.valueOf(other.getId()), dates.get(String.valueOf(employee)));
    Map<String, Object> confirmations = (Map<String, Object>) data.get("approvedInputs");
    confirmations.put(String.valueOf(other.getId()), confirmations.get(String.valueOf(employee)));
    HrmPayrollInsurancePolicyService.Quote q = new HrmPayrollInsurancePolicyService.Quote();
    q.setEntityId(1L);
    q.setPayerEntityId(1L);
    q.setEmployeeId(other.getId());
    q.setCoverage("EXEMPT");
    ((Map<String, Object>) data.get("insurancePolicySources"))
        .put(String.valueOf(other.getId()), q);
    List<HrmSalaryMonthEmployeeRecordDO> result =
        JsonUtils.parseArray(r.getResultSnapshot(), HrmSalaryMonthEmployeeRecordDO.class);
    result.add(
        HrmSalaryMonthEmployeeRecordDO.builder()
            .employeeId(other.getId())
            .entityId(1L)
            .monthRecordId(b.getId())
            .realPaySalary(new BigDecimal("1000"))
            .build());
    r.setInputSnapshot(JsonUtils.toJsonString(data));
    r.setResultSnapshot(JsonUtils.toJsonString(result));
    runs.updateById(r);
    service.reserve(b, r, 3L);
    service.paid(b.getId(), r.getId(), employee);
    assertEquals(1, claims.selectList().stream().filter(c -> "PAID".equals(c.getStatus())).count());
    assertEquals(
        1, claims.selectList().stream().filter(c -> "RESERVED".equals(c.getStatus())).count());
    b.setStatus(15);
    batches.updateById(b);
    assertServiceException(
        () -> service.release(b.getId(), r.getId(), "不能释放已成功项目", 4L), PAYROLL_SOURCE_CLAIM_INVALID);
  }
}
