package cn.iocoder.yudao.module.hrm.service.payroll;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.salary.monthrecord.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.salary.config.HrmSalaryOptionDO;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.employee.info.HrmEmployeeMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.salary.monthrecord.*;
import cn.iocoder.yudao.module.hrm.service.salary.config.HrmSalaryOptionService;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import javax.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
@Import({HrmPayrollSupplementService.class,HrmPayrollRetroService.class,HrmPayrollBatchService.class,org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration.class})
class HrmPayrollSupplementServiceTest extends BaseDbUnitTest {
 @org.springframework.boot.test.mock.mockito.MockBean private cn.iocoder.yudao.module.hrm.service.payroll.HrmPayrollOffsetService offsets;

 @org.springframework.boot.test.mock.mockito.MockBean private cn.iocoder.yudao.module.hrm.service.payroll.HrmPayrollSalarySourceService salarySources;

 @org.springframework.boot.test.mock.mockito.MockBean private cn.iocoder.yudao.module.hrm.service.payroll.HrmPayrollConfigGuardService configGuard;

 @org.springframework.boot.test.mock.mockito.MockBean private cn.iocoder.yudao.module.hrm.service.payroll.HrmPayrollInputIssueService inputIssues;

 @Resource HrmPayrollSupplementService service;
 @Resource HrmPayrollRetroService retro;
 @Resource HrmPayrollRetroMapper retros;
 @Resource HrmPayrollPaymentMapper payments;
 @Resource HrmPayrollRunMapper runs;
 @Resource HrmSalaryMonthRecordMapper batches;
 @Resource HrmSalaryMonthEmployeeRecordMapper employeeRecords;
 @Resource HrmEmployeeMapper employees;
 @MockBean HrmPayrollScopeService scope;
 @MockBean HrmPayrollTaxProfileService profiles;
 @MockBean HrmPayrollTaxWithholdingService taxWithholding;
 @MockBean HrmPayrollTaxDeductionService deductions;
 @MockBean HrmSalaryOptionService options;
 @MockBean HrmPayrollWorkService workService;
 @MockBean HrmPayrollInsurancePolicyService insurance;
 @MockBean HrmPayrollInputService inputs;
 @MockBean HrmPayrollSourceClaimService sourceClaims;
 @MockBean HrmPayrollAttendanceService attendanceService;
 Long employeeId,paymentId;HrmSalaryMonthRecordDO original;
 @BeforeEach void setup() {
  HrmEmployeeDO employee=new HrmEmployeeDO().setName("已离职补发员工").setJobNumber("SUPP-1").setUserId(99L).setLeaveTime(LocalDate.of(2026,9,30).atStartOfDay());employees.insert(employee);employeeId=employee.getId();
  original=new HrmSalaryMonthRecordDO().setEntityId(10L).setTitle("九月工资").setYear(2026).setMonth(9).setStatus(17).setBatchKey("REGULAR").setStartTime(LocalDate.of(2026,9,1).atStartOfDay()).setEndTime(LocalDate.of(2026,9,30).atStartOfDay());batches.insert(original);
  HrmPayrollTaxWithholdingService.Source saved=new HrmPayrollTaxWithholdingService.Source().setEntityId(10L).setEmployeeId(employeeId).setCurrentIncome(new BigDecimal("10000")).setProfile(new HrmPayrollTaxProfileDO().setTaxpayerKey("same-person"));
  HrmSalaryMonthEmployeeRecordDO result=new HrmSalaryMonthEmployeeRecordDO().setEmployeeId(employeeId).setRealPaySalary(new BigDecimal("9000")).setTaxSourceSnapshot(JsonUtils.toJsonString(saved)).setOptionValues(Collections.emptyList());
  HrmPayrollRunDO run=new HrmPayrollRunDO().setMonthRecordId(original.getId()).setVersion(1).setInputSnapshot("{}").setRuleSnapshot("{}").setResultSnapshot(JsonUtils.toJsonString(Collections.singletonList(result)));runs.insert(run);
  HrmPayrollPaymentDO p=new HrmPayrollPaymentDO().setMonthRecordId(original.getId()).setRunId(run.getId()).setEmployeeId(employeeId).setEmployeeName("已离职补发员工").setBankAccount("test-account").setAttempt(1).setAmount(new BigDecimal("9000")).setStatus("SUCCESS").setValueDate(LocalDate.of(2026,9,30));payments.insert(p);paymentId=p.getId();
  when(scope.payrollDates(any())).thenAnswer(call->{HrmSalaryMonthRecordDO b=call.getArgument(0);return b.getStartTime().toLocalDate().isAfter(LocalDate.of(2026,9,30))?Collections.emptyMap():Collections.singletonMap(employeeId,Collections.singleton(LocalDate.of(2026,9,1)));});
  when(scope.snapshot(any())).thenAnswer(call->Collections.singletonMap("batchId",((HrmSalaryMonthRecordDO)call.getArgument(0)).getId()));
  when(profiles.requireApproved(any(),any(),anyInt())).thenReturn(new HrmPayrollTaxProfileDO().setTaxpayerKey("same-person"));
  when(options.getSalaryOptionList(false)).thenReturn(Collections.singletonList(new HrmSalaryOptionDO().setCode(10901).setParentCode(10).setTaxEnabled(true).setType(1).setEnabled(true)));
  when(deductions.source(any(),any(),anyInt(),anyInt())).thenReturn(new HrmPayrollTaxDeductionDO());when(deductions.values(any())).thenReturn(Collections.emptyMap());
  when(taxWithholding.calculateForBatch(any(),any(),any(),any(),any(),any(),any())).thenAnswer(call->new HrmPayrollTaxWithholdingService.Source()
   .setBatchId(call.getArgument(0)).setEntityId(10L).setEmployeeId(employeeId).setExpectedPaymentDate(call.getArgument(3))
   .setCurrentIncome(call.getArgument(4)).setCurrentSocialDeduction(BigDecimal.ZERO).setAdditionalDeduction(BigDecimal.ZERO).setDeductionValues(Collections.emptyMap())
   .setPaid(new HrmPayrollTaxCashLedgerVO().setIncome(new BigDecimal("10000")).setBasicDeduction(new BigDecimal("5000")).setSocialDeduction(BigDecimal.ZERO).setWithheld(new BigDecimal("150")))
   .setQuote(new PayrollTaxPolicyEngine.Quote().setCurrentWithholding(new BigDecimal("60")).setCurrentSalaryWithholding(new BigDecimal("60")).setSeparateBonusTax(BigDecimal.ZERO).setCumulativeSalaryTax(new BigDecimal("210")).setSalaryTaxable(new BigDecimal("7000")).setBasicDeduction(new BigDecimal("5000"))));
 }
 HrmPayrollSupplementReqVO req(){return new HrmPayrollSupplementReqVO().setPaymentId(paymentId).setCorrectedIncome(new BigDecimal("12000")).setExpectedPaymentDate(LocalDate.of(2026,10,15)).setEvidence("追溯工资及员工确认材料").setRequestKey(UUID.randomUUID().toString());}
 Long approved(){Long batch=service.create(req(),1L);Long id=retro.list(batch,1L).get(0).getId();retro.review(id,true,"独立核定",2L);return batch;}
 @Test void departedEmployeeGetsOnlyApprovedDifferenceAndCashYearTax() {
  Long batch=approved();Long runId=service.compute(batch,1L);HrmSalaryMonthRecordDO b=batches.selectById(batch);
  assertEquals(original.getId(),b.getParentBatchId());assertEquals(original.getStartTime(),b.getStartTime());assertTrue(HrmPayrollSupplementService.isSupplement(b));
  assertEquals(11,b.getStatus());assertEquals(0,new BigDecimal("2000").compareTo(b.getExpectedPaySalary()));assertEquals(0,new BigDecimal("1940").compareTo(b.getRealPaySalary()));
  HrmPayrollRunDO run=runs.selectById(runId);HrmSalaryMonthEmployeeRecordDO row=JsonUtils.parseArray(run.getResultSnapshot(),HrmSalaryMonthEmployeeRecordDO.class).get(0);
  assertEquals(0,new BigDecimal("2000").compareTo(row.getTaxableSalary()));assertEquals(0,BigDecimal.ZERO.compareTo(row.getActualWorkDay()));assertEquals(LocalDate.of(2026,10,15),row.getTaxPaymentDate());
  assertEquals(0,new BigDecimal("9000").compareTo(payments.selectById(paymentId).getAmount()));
  assertTrue(JsonUtils.parseObject(run.getInputSnapshot(),Map.class).containsKey("withholdingContexts"));assertEquals(1,employeeRecords.selectListByMonthRecordId(batch).size());
 }
 @Test void pendingNoSourceFrozenAndCancelledCannotBeComputed() {
  Long batch=service.create(req(),1L);assertServiceException(()->service.compute(batch,1L),PAYROLL_RETRO_INVALID);
  Long id=retro.list(batch,1L).get(0).getId();retro.review(id,true,"核定",2L);retro.cancel(id,"正式撤销",3L);
  assertServiceException(()->service.compute(batch,1L),PAYROLL_RETRO_INVALID);
 }
 @Test void requestRetriesCannotChangeOriginalOrCashDate() {
  HrmPayrollSupplementReqVO request=req();Long batch=service.create(request,1L);assertEquals(batch,service.create(request,1L));
  request.setExpectedPaymentDate(LocalDate.of(2026,10,16));assertServiceException(()->service.create(request,1L),PAYROLL_VERSION_CONFLICT);
 }
 @Test void recalculationKeepsOldRunAndDoesNotDuplicateEmployeeRows() {
  Long batch=approved();Long first=service.compute(batch,1L),second=service.compute(batch,1L);assertNotEquals(first,second);
  assertEquals(2,runs.selectList(HrmPayrollRunDO::getMonthRecordId,batch).size());assertEquals(1,employeeRecords.selectListByMonthRecordId(batch).size());
  batches.updateById(new HrmSalaryMonthRecordDO().setId(batch).setStatus(15));assertServiceException(()->service.compute(batch,1L),SALARY_MONTH_RECORD_STATUS_INVALID);
 }
 @Test void independentNegativeSettlementAndUnrelatedSourceAreRejected() {
  assertServiceException(()->service.create(req().setCorrectedIncome(new BigDecimal("9000")),1L),PAYROLL_RETRO_INVALID);
  assertEquals(1,batches.selectCount());
 }
 @Test void catalogAndCurrentTaxEvidenceMustBeReady() {
  Long batch=approved();when(options.getSalaryOptionList(false)).thenReturn(Collections.emptyList());assertServiceException(()->service.compute(batch,1L),PAYROLL_RETRO_INVALID);
  assertEquals(5,batches.selectById(batch).getStatus());
 }
 @Test void derivedPaymentCannotBecomeANewIndependentSupplementRoot(){
  batches.updateById(new HrmSalaryMonthRecordDO().setId(original.getId()).setParentBatchId(999L).setBatchKey("SUPPLEMENT:derived"));
  assertServiceException(()->service.create(req(),1L),PAYROLL_RETRO_INVALID);assertEquals(1,batches.selectCount());
 }
 @Test void readOnlyPreparationShowsPendingThenReadyWithoutWritingResults(){Long batch=service.create(req(),1L);assertFalse(service.readiness(batch,1L).get(employeeId));Long id=retro.list(batch,1L).get(0).getId();retro.review(id,true,"独立核实",2L);assertTrue(service.readiness(batch,1L).get(employeeId));assertEquals(5,batches.selectById(batch).getStatus());assertTrue(employeeRecords.selectListByMonthRecordId(batch).isEmpty());assertEquals(1,runs.selectCount());verifyNoInteractions(sourceClaims);}
 @Test void preflightRechecksTaxAndOriginalPaymentWithoutLeakingPrivateErrors(){Long batch=approved();when(deductions.source(any(),any(),anyInt(),anyInt())).thenReturn(null);assertFalse(service.readiness(batch,1L).get(employeeId));when(deductions.source(any(),any(),anyInt(),anyInt())).thenReturn(new HrmPayrollTaxDeductionDO());assertTrue(service.readiness(batch,1L).get(employeeId));payments.updateById(new HrmPayrollPaymentDO().setId(paymentId).setAmount(new BigDecimal("8999")));assertFalse(service.readiness(batch,1L).get(employeeId));assertTrue(employeeRecords.selectListByMonthRecordId(batch).isEmpty());}

}
