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
 @org.springframework.boot.test.mock.mockito.MockBean private cn.iocoder.yudao.module.hrm.service.payroll.HrmPayrollOffsetService offsets;

    @org.springframework.boot.test.mock.mockito.MockBean
    private cn.iocoder.yudao.module.hrm.service.payroll.HrmPayrollRetroService retroService;
    @org.junit.jupiter.api.BeforeEach
    void setupRetroSource() {
        org.mockito.Mockito.when(retroService.amount(org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any())).thenReturn(java.math.BigDecimal.ZERO);
    }

  @Resource private HrmPayrollSourceClaimService service;
  @Resource private HrmPayrollSourceClaimMapper claims;
  @Resource private HrmPayrollRunMapper runs;
  @Resource private HrmSalaryMonthRecordMapper batches;
  @Resource private HrmEmployeeMapper employees;
  @MockBean private HrmPayrollScopeService scope;
  @MockBean private HrmPayrollTaxWithholdingService taxWithholding;
  @Resource private HrmPayrollLegalEntityMapper legalEntities;
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

  private HrmPayrollRunDO taxRun(HrmSalaryMonthRecordDO batch, List<LocalDate> dates) {
    HrmPayrollRunDO run = run(batch, dates, false, false);
    HrmPayrollTaxWithholdingService.Source source =
        new HrmPayrollTaxWithholdingService.Source()
            .setEntityId(batch.getEntityId())
            .setEmployeeId(employee)
            .setExpectedPaymentDate(LocalDate.of(2026, 10, 14))
            .setProfile(
                new HrmPayrollTaxProfileDO()
                    .setTaxpayerKey("same-person-test")
                    .setTaxRegistrationNumber("agent"))
            .setQuote(
                new PayrollTaxPolicyEngine.Quote()
                    .setCurrentSalaryWithholding(new BigDecimal("100")));
    Map<String, Object> input = JsonUtils.parseObject(run.getInputSnapshot(), Map.class);
    input.put("withholdingContexts", Collections.singletonList(JsonUtils.toJsonString(source)));
    run.setInputSnapshot(JsonUtils.toJsonString(input))
        .setExpectedPaymentDate(source.getExpectedPaymentDate());
    runs.updateById(run);
    return run;
  }

  private void taxEntity() {
    legalEntities.insert(
        new HrmPayrollLegalEntityDO()
            .setId(1L)
            .setCode("TAX-AGENT")
            .setName("扣缴主体")
            .setStatus("APPROVED")
            .setTaxRegistrationNumber("agent"));
  }

  private HrmPayrollRunDO bonusRun(HrmSalaryMonthRecordDO batch, List<LocalDate> dates) {
    HrmPayrollRunDO run = taxRun(batch, dates);
    Map<String, Object> input = JsonUtils.parseObject(run.getInputSnapshot(), Map.class);
    HrmPayrollTaxWithholdingService.Source source =
        JsonUtils.parseObject(
            (String) ((List<?>) input.get("withholdingContexts")).get(0),
            HrmPayrollTaxWithholdingService.Source.class);
    source
        .setBatchId(batch.getId())
        .setBonus(new HrmPayrollBonusDO().setMode("SEPARATE").setAmount(new BigDecimal("36000")));
    input.put("withholdingContexts", Collections.singletonList(JsonUtils.toJsonString(source)));
    run.setInputSnapshot(JsonUtils.toJsonString(input));
    runs.updateById(run);
    return run;
  }

  @Test
  void supplementalPayrollClaimsOnlyDifferenceAndTaxYearWithoutRepayingWageDays() {
    HrmSalaryMonthRecordDO b=batch(10L).setParentBatchId(999L).setBatchKey("SUPPLEMENT:request");batches.updateById(b);
    legalEntities.insert(new HrmPayrollLegalEntityDO().setId(10L).setCode("SUPPLEMENT_ENTITY").setName("补发主体").setStatus("APPROVED"));
    HrmPayrollRetroDO retro=new HrmPayrollRetroDO().setId(100L).setBatchId(b.getId()).setEmployeeId(employee).setDelta(new BigDecimal("2000"));
    org.mockito.Mockito.when(retroService.sources(b.getId())).thenReturn(Collections.singletonList(retro));
    HrmPayrollTaxWithholdingService.Source tax=new HrmPayrollTaxWithholdingService.Source().setEntityId(10L).setBatchId(b.getId()).setEmployeeId(employee)
      .setExpectedPaymentDate(day(15)).setCurrentIncome(new BigDecimal("2000")).setCurrentSocialDeduction(BigDecimal.ZERO)
      .setProfile(new HrmPayrollTaxProfileDO().setTaxpayerKey("same-worker").setTaxRegistrationNumber("registered"))
      .setQuote(new PayrollTaxPolicyEngine.Quote().setCurrentWithholding(new BigDecimal("60")).setCurrentSalaryWithholding(new BigDecimal("60")));
    HrmSalaryMonthEmployeeRecordDO result=new HrmSalaryMonthEmployeeRecordDO().setMonthRecordId(b.getId()).setEmployeeId(employee).setExpectedPaySalary(new BigDecimal("2000"))
      .setPersonalTax(new BigDecimal("60")).setRealPaySalary(new BigDecimal("1940")).setTaxSourceSnapshot(JsonUtils.toJsonString(tax));
    HrmPayrollRunDO run=new HrmPayrollRunDO().setMonthRecordId(b.getId()).setVersion(1).setRuleSnapshot("{}").setInputSnapshot(JsonUtils.toJsonString(Collections.singletonMap("withholdingContexts",Collections.singletonList(JsonUtils.toJsonString(tax)))))
      .setResultSnapshot(JsonUtils.toJsonString(Collections.singletonList(result)));runs.insert(run);
    service.reserve(b,run,1L);service.assertReserved(b.getId(),run.getId());
    List<HrmPayrollSourceClaimDO> actual=claims.selectList();assertEquals(2,actual.size());
    assertEquals(new HashSet<>(Arrays.asList("RETRO_WAGE","TAX_YEAR")),new HashSet<>(Arrays.asList(actual.get(0).getKind(),actual.get(1).getKind())));
    result.setExpectedPaySalary(new BigDecimal("12000"));run.setResultSnapshot(JsonUtils.toJsonString(Collections.singletonList(result)));runs.updateById(run);
    assertServiceException(()->service.assertReserved(b.getId(),run.getId()),PAYROLL_SOURCE_CLAIM_INVALID);
    result.setExpectedPaySalary(new BigDecimal("2000"));run.setResultSnapshot(JsonUtils.toJsonString(Collections.singletonList(result))).setInputSnapshot("{}");runs.updateById(run);
    assertServiceException(()->service.assertReserved(b.getId(),run.getId()),PAYROLL_SOURCE_CLAIM_INVALID);
  }

  @Test
  void retroSourceIsReservedReleasedAndPermanentlyConsumedOnPayment() {
    HrmSalaryMonthRecordDO b=batch(10L);
    HrmPayrollRunDO r=run(b,Collections.singletonList(day(1)),false,false);
    org.mockito.Mockito.when(retroService.sources(b.getId())).thenReturn(Collections.singletonList(
      new HrmPayrollRetroDO().setId(1001L).setEmployeeId(employee).setDelta(new BigDecimal("-500"))));
    service.reserve(b,r,1L);
    assertTrue(claims.selectList().stream().anyMatch(c->"RETRO_WAGE".equals(c.getKind())&&"1001".equals(c.getSourceKey())&&"RESERVED".equals(c.getStatus())));
    b.setStatus(15);batches.updateById(b);service.release(b.getId(),r.getId(),"未付款解冻",1L);
    service.reserve(b,r,1L);service.paid(b.getId(),r.getId(),employee);
    assertTrue(claims.selectList().stream().anyMatch(c->"RETRO_WAGE".equals(c.getKind())&&"PAID".equals(c.getStatus())));
    HrmSalaryMonthRecordDO other=batch(20L);HrmPayrollRunDO next=run(other,Collections.singletonList(day(2)),false,false);
    java.util.List<HrmPayrollRetroDO> source=retroService.sources(b.getId());
    org.mockito.Mockito.when(retroService.sources(other.getId())).thenReturn(source);
    assertServiceException(()->service.reserve(other,next,1L),PAYROLL_SOURCE_CLAIM_INVALID);
  }

  @Test
  void separateBonusQuotaIsAcrossEntitiesAndRemainsConsumedAfterPayment() {
    taxEntity();
    legalEntities.insert(
        new HrmPayrollLegalEntityDO()
            .setId(2L)
            .setCode("BONUS-AGENT2")
            .setName("第二主体")
            .setStatus("APPROVED")
            .setTaxRegistrationNumber("agent2"));
    HrmSalaryMonthRecordDO a = batch(1L), b = batch(2L);
    HrmPayrollRunDO first = bonusRun(a, Collections.singletonList(day(1))),
        second = bonusRun(b, Collections.singletonList(day(2)));
    service.reserve(a, first, 3L);
    assertServiceException(() -> service.reserve(b, second, 3L), PAYROLL_SOURCE_CLAIM_INVALID);
    service.paid(a.getId(), first.getId(), employee);
    assertServiceException(() -> service.reserve(b, second, 3L), PAYROLL_SOURCE_CLAIM_INVALID);
    assertEquals(
        1,
        claims.selectList().stream()
            .filter(c -> "BONUS_YEAR".equals(c.getKind()) && "PAID".equals(c.getStatus()))
            .count());
  }

  @Test
  void unfreezingUnpaidBonusReleasesAnnualQuota() {
    taxEntity();
    HrmSalaryMonthRecordDO a = batch(1L), b = batch(1L);
    HrmPayrollRunDO first = bonusRun(a, Collections.singletonList(day(1))),
        second = bonusRun(b, Collections.singletonList(day(2)));
    service.reserve(a, first, 3L);
    batches.updateById(new HrmSalaryMonthRecordDO().setId(a.getId()).setStatus(15));
    service.release(a.getId(), first.getId(), "未发放重新核算", 3L);
    service.reserve(b, second, 3L);
    assertEquals(
        1,
        claims.selectList().stream()
            .filter(c -> "BONUS_YEAR".equals(c.getKind()) && "RESERVED".equals(c.getStatus()))
            .count());
  }

  @Test
  void pendingTaxYearBlocksAnotherPeriodAndPaidContextDoesNotRevalidateItself() {
    taxEntity();
    HrmSalaryMonthRecordDO a = batch(1L), b = batch(1L);
    HrmPayrollRunDO first = taxRun(a, Collections.singletonList(day(1))),
        second = taxRun(b, Collections.singletonList(day(2)));
    service.reserve(a, first, 3L);
    assertServiceException(() -> service.reserve(b, second, 3L), PAYROLL_SOURCE_CLAIM_INVALID);
    service.paid(a.getId(), first.getId(), employee);
    org.mockito.Mockito.clearInvocations(taxWithholding);
    service.assertReserved(a.getId(), first.getId());
    org.mockito.Mockito.verifyNoInteractions(taxWithholding);
    service.reserve(b, second, 3L);
    assertEquals(
        1,
        claims.selectList().stream()
            .filter(c -> "TAX_YEAR".equals(c.getKind()) && "RESERVED".equals(c.getStatus()))
            .count());
  }

  @Test
  void differentEmployeeIdsForSameTaxIdentityCannotFreezeConcurrently() throws Exception {
    taxEntity();
    HrmSalaryMonthRecordDO a = batch(1L), b = batch(1L);
    HrmPayrollRunDO first = taxRun(a, Collections.singletonList(day(1)));
    HrmEmployeeDO alias = new HrmEmployeeDO().setName("再入职编号").setJobNumber("CLAIM-ALIAS");
    employees.insert(alias);
    employee = alias.getId();
    HrmPayrollRunDO second = taxRun(b, Collections.singletonList(day(2)));
    java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
    java.util.concurrent.ExecutorService pool =
        java.util.concurrent.Executors.newFixedThreadPool(2);
    try {
      java.util.List<java.util.concurrent.Future<Boolean>> outcomes = new ArrayList<>();
      for (int i = 0; i < 2; i++) {
        final HrmSalaryMonthRecordDO target = i == 0 ? a : b;
        final HrmPayrollRunDO version = i == 0 ? first : second;
        outcomes.add(
            pool.submit(
                () -> {
                  cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.setTenantId(
                      0L);
                  try {
                    if (!start.await(10, java.util.concurrent.TimeUnit.SECONDS))
                      throw new IllegalStateException("start timeout");
                    service.reserve(target, version, 3L);
                    return true;
                  } catch (cn.iocoder.yudao.framework.common.exception.ServiceException ex) {
                    if (ex.getCode() != PAYROLL_SOURCE_CLAIM_INVALID.getCode()) throw ex;
                    return false;
                  } finally {
                    cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.clear();
                  }
                }));
      }
      start.countDown();
      int succeeded = 0;
      for (java.util.concurrent.Future<Boolean> outcome : outcomes)
        if (outcome.get(20, java.util.concurrent.TimeUnit.SECONDS)) succeeded++;
      assertEquals(1, succeeded);
      assertEquals(
          1,
          claims.selectList().stream()
              .filter(c -> "TAX_YEAR".equals(c.getKind()) && "RESERVED".equals(c.getStatus()))
              .count());
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void differentEntitiesAndEmployeeIdsCannotReserveSameBonusYearConcurrently() throws Exception {
    taxEntity();
    legalEntities.insert(
        new HrmPayrollLegalEntityDO()
            .setId(2L)
            .setCode("BONUS-CONCURRENT2")
            .setName("第二主体")
            .setStatus("APPROVED")
            .setTaxRegistrationNumber("agent2"));
    HrmSalaryMonthRecordDO a = batch(1L), b = batch(2L);
    HrmPayrollRunDO first = bonusRun(a, Collections.singletonList(day(1)));
    HrmEmployeeDO alias = new HrmEmployeeDO().setName("再入职编号").setJobNumber("BONUS-ALIAS");
    employees.insert(alias);
    employee = alias.getId();
    HrmPayrollRunDO second = bonusRun(b, Collections.singletonList(day(2)));
    java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
    java.util.concurrent.ExecutorService pool =
        java.util.concurrent.Executors.newFixedThreadPool(2);
    try {
      java.util.List<java.util.concurrent.Future<Boolean>> outcomes = new ArrayList<>();
      for (int i = 0; i < 2; i++) {
        final HrmSalaryMonthRecordDO target = i == 0 ? a : b;
        final HrmPayrollRunDO version = i == 0 ? first : second;
        outcomes.add(
            pool.submit(
                () -> {
                  cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.setTenantId(
                      0L);
                  try {
                    if (!start.await(10, java.util.concurrent.TimeUnit.SECONDS))
                      throw new IllegalStateException("start timeout");
                    service.reserve(target, version, 3L);
                    return true;
                  } catch (cn.iocoder.yudao.framework.common.exception.ServiceException ex) {
                    if (ex.getCode() != PAYROLL_SOURCE_CLAIM_INVALID.getCode()) throw ex;
                    return false;
                  } finally {
                    cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.clear();
                  }
                }));
      }
      start.countDown();
      int succeeded = 0;
      for (java.util.concurrent.Future<Boolean> outcome : outcomes)
        if (outcome.get(20, java.util.concurrent.TimeUnit.SECONDS)) succeeded++;
      assertEquals(1, succeeded);
      assertEquals(
          1,
          claims.selectList().stream()
              .filter(c -> "BONUS_YEAR".equals(c.getKind()) && "RESERVED".equals(c.getStatus()))
              .count());
    } finally {
      pool.shutdownNow();
    }
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
