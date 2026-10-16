package cn.iocoder.yudao.module.hrm.service.payroll;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollBankRowVO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.employee.employment.HrmEmployeeSalaryCardDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.salary.monthrecord.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.salary.monthrecord.HrmSalaryMonthRecordMapper;
import cn.iocoder.yudao.module.hrm.service.employee.employment.HrmEmployeeSalaryCardService;
import cn.iocoder.yudao.module.hrm.service.employee.info.HrmEmployeeService;
import java.math.BigDecimal;
import java.util.*;
import javax.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

@Import({HrmPayrollPaymentService.class, HrmPayrollBatchService.class})
class HrmPayrollPaymentServiceTest extends BaseDbUnitTest {
 @org.springframework.boot.test.mock.mockito.MockBean private cn.iocoder.yudao.module.hrm.service.payroll.HrmPayrollInputIssueService inputIssues;

    @org.springframework.boot.test.mock.mockito.MockBean
    private cn.iocoder.yudao.module.hrm.service.payroll.HrmPayrollRetroService retroService;
    @org.junit.jupiter.api.BeforeEach
    void setupRetroSource() {
        org.mockito.Mockito.when(retroService.amount(org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any())).thenReturn(java.math.BigDecimal.ZERO);
    }

  @org.springframework.boot.test.mock.mockito.MockBean
  private HrmPayrollTaxWithholdingService taxWithholding;

  @org.springframework.boot.test.mock.mockito.MockBean
  private HrmPayrollSourceClaimService sourceClaims;

  @org.springframework.boot.test.mock.mockito.MockBean
  private cn.iocoder.yudao.module.hrm.service.payroll.HrmPayrollInputService payrollInputs;

  @org.springframework.boot.test.mock.mockito.MockBean
  private cn.iocoder.yudao.module.hrm.service.payroll.HrmPayrollAttendanceService
      payrollAttendanceService;

  @org.springframework.boot.test.mock.mockito.MockBean private HrmPayrollWorkService workService;

  @org.springframework.boot.test.mock.mockito.MockBean
  private cn.iocoder.yudao.module.hrm.service.payroll.HrmPayrollScopeService payrollScope;

  @org.springframework.boot.test.mock.mockito.MockBean
  private cn.iocoder.yudao.module.hrm.service.payroll.HrmPayrollInsurancePolicyService
      payrollInsurancePolicies;

  @Resource private HrmPayrollPaymentService service;
  @Resource private HrmPayrollBatchService batches;
  @Resource private HrmSalaryMonthRecordMapper mapper;
  @MockBean private HrmEmployeeService employees;
  @MockBean private HrmEmployeeSalaryCardService cards;

  @org.junit.jupiter.api.BeforeEach
  void configureScopedQueries() {
    org.mockito.Mockito.when(
            payrollScope.page(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyBoolean()))
        .thenAnswer(call -> mapper.selectPage(call.getArgument(0)));
    org.mockito.Mockito.when(
            payrollScope.list(org.mockito.ArgumentMatchers.nullable(Integer.class)))
        .thenAnswer(call -> mapper.selectListByStatus(call.getArgument(0)));
  }

  @Test
  void partialFailureOnlyRetriesFailedRowsAndCannotReverseSuccess() {
    HrmSalaryMonthRecordDO batch =
        new HrmSalaryMonthRecordDO().setYear(2026).setMonth(10).setStatus(11);
    mapper.insert(batch);
    List<HrmSalaryMonthEmployeeRecordDO> results = new ArrayList<>();
    for (long id = 1; id <= 2; id++) {
      when(employees.getEmployee(id)).thenReturn(new HrmEmployeeDO().setId(id).setName("员工" + id));
      when(cards.getSalaryCardByEmployeeId(id))
          .thenReturn(
              new HrmEmployeeSalaryCardDO().setEmployeeId(id).setBankCardNumber("123456" + id));
      results.add(
          new HrmSalaryMonthEmployeeRecordDO()
              .setEmployeeId(id)
              .setRealPaySalary(new BigDecimal("100.00")));
    }
    Long run =
        batches.snapshot(
            batch.getId(), Collections.emptyMap(), Collections.emptyMap(), results, 1L);
    mapper.updateById(new HrmSalaryMonthRecordDO().setId(batch.getId()).setStatus(15));
    service.prepare(batch.getId(), run, 3L);
    service.prepare(batch.getId(), run, 3L);
    List<HrmPayrollBankRowVO> rows = service.export(batch.getId());
    assertEquals(2, rows.size());
    rows.get(0).setStatus("SUCCESS");
    rows.get(0).setValueDate(java.time.LocalDate.of(2026, 10, 1));
    rows.get(1).setStatus("FAILED");
    rows.get(1).setFailureReason("银行拒绝");
    Long receipt = service.reconcileFile(batch.getId(), "bank.xlsx", "checksum-v1", rows, 3L);
    assertEquals(
        receipt, service.reconcileFile(batch.getId(), "bank-copy.xlsx", "checksum-v1", rows, 3L));
    assertEquals(1, service.receipts(batch.getId()).size());
    assertNull(service.receipts(batch.getId()).get(0).getDetails());
    assertEquals(16, mapper.selectById(batch.getId()).getStatus());
    assertServiceException(() -> service.retry(rows.get(0).getId(), 3L), PAYROLL_PAYMENT_INVALID);
    service.retry(rows.get(1).getId(), 3L);
    assertServiceException(
        () -> service.reconcile(batch.getId(), Collections.singletonList(rows.get(1)), 3L),
        PAYROLL_PAYMENT_INVALID);
    List<HrmPayrollBankRowVO> retries = service.export(batch.getId());
    assertEquals(1, retries.size());
    assertEquals(rows.get(1).getId(), retries.get(0).getId());
    retries.get(0).setStatus("SUCCESS");
    retries.get(0).setValueDate(java.time.LocalDate.of(2026, 10, 2));
    service.reconcile(batch.getId(), retries, 3L);
    assertEquals(17, mapper.selectById(batch.getId()).getStatus());
    service.reconcile(batch.getId(), retries, 3L);
    assertTrue(service.list(batch.getId()).get(0).getBankAccount().startsWith("****"));
    assertNull(service.list(batch.getId()).get(1).getFailureReason());
    batches.transition(batch.getId(), run, "archive", "财务台账已核对", 3L);
    assertEquals(10, mapper.selectById(batch.getId()).getStatus());
    assertEquals(
        1L,
        service
            .batches(
                new cn.iocoder.yudao.module.hrm.controller.admin.salary.vo.monthrecord
                    .HrmSalaryMonthRecordPageReqVO())
            .getTotal());
  }

  @Test
  void accountOrAmountMismatchRollsBack() {
    HrmSalaryMonthRecordDO batch =
        new HrmSalaryMonthRecordDO().setYear(2026).setMonth(10).setStatus(11);
    mapper.insert(batch);
    when(employees.getEmployee(1L)).thenReturn(new HrmEmployeeDO().setId(1L).setName("员工"));
    when(cards.getSalaryCardByEmployeeId(1L))
        .thenReturn(new HrmEmployeeSalaryCardDO().setBankCardNumber("1234567"));
    Long run =
        batches.snapshot(
            batch.getId(),
            Collections.emptyMap(),
            Collections.emptyMap(),
            Collections.singletonList(
                new HrmSalaryMonthEmployeeRecordDO()
                    .setEmployeeId(1L)
                    .setRealPaySalary(new BigDecimal("100"))),
            1L);
    mapper.updateById(new HrmSalaryMonthRecordDO().setId(batch.getId()).setStatus(15));
    service.prepare(batch.getId(), run, 3L);
    HrmPayrollBankRowVO row = service.export(batch.getId()).get(0);
    row.setStatus("SUCCESS");
    row.setAmount(new BigDecimal("101"));
    assertEquals(
        "金额不匹配",
        service.validateRows(batch.getId(), Collections.singletonList(row)).get(0).getReason());
    assertServiceException(
        () -> service.reconcile(batch.getId(), Collections.singletonList(row), 3L),
        PAYROLL_PAYMENT_INVALID);
    assertEquals("PENDING", service.list(batch.getId()).get(0).getStatus());
    row.setAmount(new BigDecimal("100"));
    assertServiceException(
        () -> service.reconcile(batch.getId(), Arrays.asList(row, row), 3L),
        PAYROLL_PAYMENT_INVALID);
    assertEquals("PENDING", service.list(batch.getId()).get(0).getStatus());
  }

  @Test
  void successfulReceiptsRequireActualDatesAndCannotRewriteThem() {
    HrmSalaryMonthRecordDO batch =
        new HrmSalaryMonthRecordDO().setYear(2026).setMonth(10).setStatus(11);
    mapper.insert(batch);
    when(employees.getEmployee(1L)).thenReturn(new HrmEmployeeDO().setId(1L).setName("日期员工"));
    when(cards.getSalaryCardByEmployeeId(1L))
        .thenReturn(new HrmEmployeeSalaryCardDO().setBankCardNumber("123456"));
    Long run =
        batches.snapshot(
            batch.getId(),
            Collections.emptyMap(),
            Collections.emptyMap(),
            Collections.singletonList(
                new HrmSalaryMonthEmployeeRecordDO()
                    .setEmployeeId(1L)
                    .setRealPaySalary(new BigDecimal("100"))),
            1L);
    mapper.updateById(new HrmSalaryMonthRecordDO().setId(batch.getId()).setStatus(15));
    service.prepare(batch.getId(), run, 3L);
    HrmPayrollBankRowVO row = service.export(batch.getId()).get(0).setStatus("SUCCESS");
    assertServiceException(
        () -> service.reconcile(batch.getId(), Collections.singletonList(row), 3L),
        PAYROLL_PAYMENT_INVALID);
    row.setValueDate(java.time.LocalDate.now(java.time.ZoneId.of("Asia/Shanghai")).plusDays(1));
    assertServiceException(
        () -> service.reconcile(batch.getId(), Collections.singletonList(row), 3L),
        PAYROLL_PAYMENT_INVALID);
    row.setValueDate(java.time.LocalDate.of(2026, 10, 1));
    service.reconcile(batch.getId(), Collections.singletonList(row), 3L);
    assertEquals(row.getValueDate(), service.list(batch.getId()).get(0).getValueDate());
    row.setValueDate(java.time.LocalDate.of(2026, 10, 2));
    assertServiceException(
        () -> service.reconcile(batch.getId(), Collections.singletonList(row), 3L),
        PAYROLL_PAYMENT_INVALID);
    assertEquals(
        java.time.LocalDate.of(2026, 10, 1), service.list(batch.getId()).get(0).getValueDate());
  }

  private Long[] zeroPreparation() {
    HrmSalaryMonthRecordDO batch =
        new HrmSalaryMonthRecordDO()
            .setYear(2026)
            .setMonth(10)
            .setStatus(11)
            .setExpectedPaymentDate(java.time.LocalDate.of(2026, 10, 1));
    mapper.insert(batch);
    when(employees.getEmployee(1L)).thenReturn(new HrmEmployeeDO().setId(1L).setName("无需转账员工"));
    Long run =
        batches.snapshot(
            batch.getId(),
            Collections.emptyMap(),
            Collections.emptyMap(),
            Collections.singletonList(
                new HrmSalaryMonthEmployeeRecordDO()
                    .setEmployeeId(1L)
                    .setRealPaySalary(BigDecimal.ZERO)),
            1L);
    mapper.updateById(new HrmSalaryMonthRecordDO().setId(batch.getId()).setStatus(15));
    service.prepare(batch.getId(), run, 3L);
    return new Long[] {batch.getId(), run, service.list(batch.getId()).get(0).getId()};
  }

  @Test
  void zeroNetNeedsDatedAccountingProofAndDoesNotGenerateBankTransfer() {
    Long[] ids = zeroPreparation();
    assertEquals(16, mapper.selectById(ids[0]).getStatus());
    assertEquals("ZERO_PENDING", service.list(ids[0]).get(0).getStatus());
    assertNull(service.list(ids[0]).get(0).getValueDate());
    assertTrue(service.export(ids[0]).isEmpty());
    verify(sourceClaims, never()).paid(anyLong(), anyLong(), anyLong());
    service.settleZero(
        ids[2], java.time.LocalDate.of(2026, 10, 1), "ZERO-2026-10", "核对正式会计结算凭据及日期", 3L);
    assertEquals(17, mapper.selectById(ids[0]).getStatus());
    assertEquals(java.time.LocalDate.of(2026, 10, 1), service.list(ids[0]).get(0).getValueDate());
    verify(sourceClaims).paid(ids[0], ids[1], 1L);
    service.settleZero(
        ids[2], java.time.LocalDate.of(2026, 10, 1), "ZERO-2026-10", "核对正式会计结算凭据及日期", 3L);
    verify(sourceClaims, times(1)).paid(ids[0], ids[1], 1L);
  }

  @Test
  void zeroSettlementRejectsSelfFutureWrongPeriodAndBankCallback() {
    Long[] ids = zeroPreparation();
    assertServiceException(
        () -> service.settleZero(ids[2], java.time.LocalDate.of(2026, 10, 1), "REF", "本人确认", 1L),
        PAYROLL_SELF_APPROVAL);
    assertServiceException(
        () ->
            service.settleZero(
                ids[2],
                java.time.LocalDate.now(java.time.ZoneId.of("Asia/Shanghai")).plusDays(1),
                "REF",
                "未来结算",
                3L),
        PAYROLL_PAYMENT_INVALID);
    assertServiceException(
        () -> service.settleZero(ids[2], java.time.LocalDate.of(2026, 9, 30), "REF", "错误税务期间", 3L),
        PAYROLL_TAX_INVALID);
    HrmPayrollBankRowVO row =
        new HrmPayrollBankRowVO()
            .setId(ids[2])
            .setAmount(BigDecimal.ZERO)
            .setBankAccount("")
            .setAttempt(1)
            .setStatus("SUCCESS")
            .setValueDate(java.time.LocalDate.of(2026, 10, 1));
    assertTrue(
        service
            .validateRows(ids[0], Collections.singletonList(row))
            .get(0)
            .getReason()
            .contains("零实发"));
  }

  @Test
  void unpaidZeroOnlyPreparationCanReturnToFrozenForRecalculation() {
    Long[] ids = zeroPreparation();
    service.cancelZeroPreparation(ids[0], "发放安排需重新核定", 3L);
    assertEquals(15, mapper.selectById(ids[0]).getStatus());
    assertTrue(service.list(ids[0]).isEmpty());
    verify(sourceClaims, never()).paid(anyLong(), anyLong(), anyLong());
    service.prepare(ids[0], ids[1], 3L);
    assertEquals(16, mapper.selectById(ids[0]).getStatus());
    assertEquals(1, service.list(ids[0]).size());
  }
}
