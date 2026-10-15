package cn.iocoder.yudao.module.hrm.service.payroll;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollRetroReqVO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.salary.monthrecord.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.employee.info.HrmEmployeeMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.salary.monthrecord.HrmSalaryMonthRecordMapper;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import javax.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
@Import({HrmPayrollRetroService.class,org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration.class})
class HrmPayrollRetroServiceTest extends BaseDbUnitTest {
 @Resource HrmPayrollRetroService service;
 @Resource HrmPayrollRetroMapper retros;
 @Resource HrmPayrollPaymentMapper payments;
 @Resource HrmPayrollRunMapper runs;
 @Resource HrmSalaryMonthRecordMapper batches;
 @Resource HrmEmployeeMapper employees;
 @MockBean HrmPayrollScopeService scope;
 @MockBean HrmPayrollTaxProfileService profiles;
 Long batchId, employeeId, paymentId; HrmSalaryMonthRecordDO original;
 @BeforeEach void setup() {
  HrmEmployeeDO e=new HrmEmployeeDO().setName("工资员工").setJobNumber("R-1").setUserId(99L);employees.insert(e);employeeId=e.getId();
  original=new HrmSalaryMonthRecordDO().setEntityId(10L).setYear(2026).setMonth(9).setTitle("九月工资").setStatus(17).setBatchKey("REGULAR");batches.insert(original);
  HrmSalaryMonthRecordDO b=new HrmSalaryMonthRecordDO().setEntityId(10L).setBatchKey("REGULAR").setStatus(5).setYear(2026).setMonth(10).setExpectedPaymentDate(LocalDate.of(2026,10,15));batches.insert(b);batchId=b.getId();
  HrmPayrollTaxWithholdingService.Source tax=new HrmPayrollTaxWithholdingService.Source().setEntityId(10L).setEmployeeId(employeeId).setCurrentIncome(new BigDecimal("10000")).setProfile(new HrmPayrollTaxProfileDO().setTaxpayerKey("verified"));
  HrmSalaryMonthEmployeeRecordDO result=new HrmSalaryMonthEmployeeRecordDO().setEmployeeId(employeeId).setRealPaySalary(new BigDecimal("9000")).setTaxSourceSnapshot(JsonUtils.toJsonString(tax)).setOptionValues(Collections.emptyList());
  HrmPayrollRunDO run=new HrmPayrollRunDO().setMonthRecordId(original.getId()).setVersion(1).setInputSnapshot("{}").setRuleSnapshot("{}").setComputedBy(1L).setResultSnapshot(JsonUtils.toJsonString(Collections.singletonList(result)));runs.insert(run);
  HrmPayrollPaymentDO p=new HrmPayrollPaymentDO().setEmployeeName("工资员工").setBankAccount("test-account").setAttempt(1).setMonthRecordId(original.getId()).setRunId(run.getId()).setEmployeeId(employeeId).setAmount(new BigDecimal("9000")).setStatus("SUCCESS").setValueDate(LocalDate.of(2026,9,15));payments.insert(p);paymentId=p.getId();
  when(profiles.requireApproved(any(),any(),anyInt())).thenReturn(new HrmPayrollTaxProfileDO().setTaxpayerKey("verified"));
  when(scope.payrollDates(any())).thenReturn(Collections.singletonMap(employeeId,Collections.singleton(LocalDate.of(2026,10,1))));
 }
 HrmPayrollRetroReqVO req(String income){return new HrmPayrollRetroReqVO().setBatchId(batchId).setPaymentId(paymentId).setCorrectedIncome(new BigDecimal(income)).setEvidence("调薪追溯核定及员工确认材料").setRequestKey(UUID.randomUUID().toString());}
 Long approve(String income){Long id=service.create(req(income),1L);service.review(id,true,"独立工资复核",2L);return id;}
 @Test void pendingAndIndependentReviewThenSignedSource() {
  Long id=service.create(req("11000"),1L);assertEquals(BigDecimal.ZERO,service.amount(batchId,employeeId));
  assertServiceException(()->service.review(id,true,"提交人",1L),PAYROLL_SELF_APPROVAL);
  assertServiceException(()->service.review(id,true,"本人",99L),PAYROLL_SELF_APPROVAL);
  service.review(id,true,"核实",2L);assertEquals(0,new BigDecimal("1000").compareTo(service.amount(batchId,employeeId)));
  assertEquals(2,service.history(id,2L).size());assertEquals(new BigDecimal("9000.00"),payments.selectById(paymentId).getAmount());
 }
 @Test void sameYearRecoveryAndCrossYearProtection() {
  Long id=approve("9500");assertEquals(0,new BigDecimal("-500").compareTo(service.amount(batchId,employeeId)));
  batches.updateById(new HrmSalaryMonthRecordDO().setId(batchId).setExpectedPaymentDate(LocalDate.of(2027,1,15)));
  assertServiceException(()->service.sources(batchId),PAYROLL_RETRO_INVALID);
 }
 @Test void crossYearSupplementRemainsCashYearIncome() {
  batches.updateById(new HrmSalaryMonthRecordDO().setId(batchId).setExpectedPaymentDate(LocalDate.of(2027,1,15)));
  approve("12000");assertEquals(0,new BigDecimal("2000").compareTo(service.amount(batchId,employeeId)));
 }
 @Test void staleApprovalAndUnpaidSuccessorAreRejected() {
  Long a=service.create(req("11000"),1L),b=service.create(req("12000"),1L);
  service.review(a,true,"核实",2L);
  assertServiceException(()->service.review(b,true,"旧基线",2L),PAYROLL_VERSION_CONFLICT);
  assertServiceException(()->service.create(req("12000").setPriorRetroId(a),1L),PAYROLL_RETRO_INVALID);
 }
 @Test void settledSuccessorUsesOnlyRemainingDifference() {
  Long a=approve("11000");
  HrmPayrollRunDO run=new HrmPayrollRunDO().setMonthRecordId(batchId).setVersion(1).setRuleSnapshot("{}").setResultSnapshot("[]").setComputedBy(1L).setInputSnapshot(JsonUtils.toJsonString(Collections.singletonMap("retroSources",service.sources(batchId))));runs.insert(run);
  payments.insert(new HrmPayrollPaymentDO().setEmployeeName("工资员工").setBankAccount("test-account").setAttempt(1).setMonthRecordId(batchId).setEmployeeId(employeeId).setRunId(run.getId()).setStatus("SUCCESS").setAmount(BigDecimal.ONE).setValueDate(LocalDate.of(2026,10,15)));
  HrmSalaryMonthRecordDO next=new HrmSalaryMonthRecordDO().setYear(2026).setMonth(11).setEntityId(10L).setBatchKey("REGULAR").setStatus(5).setExpectedPaymentDate(LocalDate.of(2026,11,15));batches.insert(next);
  Long id=service.create(req("11500").setBatchId(next.getId()).setPriorRetroId(a),1L);service.review(id,true,"已结清接续",2L);
  assertEquals(0,new BigDecimal("500").compareTo(service.amount(next.getId(),employeeId)));
 }
 @Test void idempotenceAndChangesCannotRewriteRequest() {
  HrmPayrollRetroReqVO r=req("12000");Long id=service.create(r,1L);assertEquals(id,service.create(r,1L));
  r.setCorrectedIncome(BigDecimal.ONE);assertServiceException(()->service.create(r,1L),PAYROLL_RETRO_INVALID);
 }
 @Test void frozenScopeAndOriginalCashAreChecked() {
  when(scope.payrollDates(any())).thenReturn(Collections.emptyMap());assertServiceException(()->service.create(req("11000"),1L),PAYROLL_RETRO_INVALID);
  when(scope.payrollDates(any())).thenReturn(Collections.singletonMap(employeeId,Collections.singleton(LocalDate.of(2026,10,1))));
  batches.updateById(new HrmSalaryMonthRecordDO().setId(batchId).setStatus(15));assertServiceException(()->service.create(req("11000"),1L),SALARY_MONTH_RECORD_STATUS_INVALID);
 }
 @Test void changedSourceInvalidatesFrozenEvidence() {
  approve("12000");String snapshot=JsonUtils.toJsonString(Collections.singletonMap("retroSources",service.sources(batchId)));
  service.validateInputSnapshot(snapshot,batchId);
  payments.updateById(new HrmPayrollPaymentDO().setId(paymentId).setValueDate(LocalDate.of(2026,9,16)));
  assertServiceException(()->service.validateInputSnapshot(snapshot,batchId),PAYROLL_VERSION_CONFLICT);
 }
 @Test void newApprovalInvalidatesPreviousComputation() {
  approve("12000");assertServiceException(()->service.validateInputSnapshot("{}",batchId),PAYROLL_VERSION_CONFLICT);
 }
 @Test void originalReadPermissionIsEnforced() {
  doThrow(cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception(PAYROLL_ENTITY_INVALID)).when(scope).check(argThat(b->b!=null && Objects.equals(b.getId(),original.getId())),eq("READ"),eq(1L));
  assertServiceException(()->service.create(req("11000"),1L),PAYROLL_ENTITY_INVALID);
 }
 @Test void cancellationRetainsEvidenceAndInvalidatesRun() {
  Long id=approve("11000");String input=JsonUtils.toJsonString(Collections.singletonMap("retroSources",service.sources(batchId)));
  assertServiceException(()->service.cancel(id,"本人",99L),PAYROLL_SELF_APPROVAL);
  service.cancel(id,"正式撤销依据",3L);assertEquals("CANCELLED",retros.selectById(id).getStatus());
  assertServiceException(()->service.validateInputSnapshot(input,batchId),PAYROLL_VERSION_CONFLICT);
  assertEquals(3,service.history(id,3L).size());
 }
 @Test void changedVerifiedIdentityCannotReceiveOriginalWages() {
  when(profiles.requireApproved(any(),any(),anyInt())).thenReturn(new HrmPayrollTaxProfileDO().setTaxpayerKey("another"));
  assertServiceException(()->service.create(req("11000"),1L),PAYROLL_RETRO_INVALID);
 }
 @Test void originalOrdinaryWageBasisExcludesAlreadyPaidHistoricalDifferences(){
  HrmPayrollPaymentDO payment=payments.selectById(paymentId);HrmPayrollRunDO run=runs.selectById(payment.getRunId());
  HrmSalaryMonthEmployeeRecordDO row=JsonUtils.parseArray(run.getResultSnapshot(),HrmSalaryMonthEmployeeRecordDO.class).get(0);
  HrmPayrollTaxWithholdingService.Source tax=JsonUtils.parseObject(row.getTaxSourceSnapshot(),HrmPayrollTaxWithholdingService.Source.class);tax.setCurrentIncome(new BigDecimal("12000"));
  row.setTaxSourceSnapshot(JsonUtils.toJsonString(tax)).setOptionValues(Collections.singletonList(HrmSalaryMonthEmployeeRecordDO.OptionValue.builder().code(10901).value(new BigDecimal("2000")).build()));
  runs.updateById(new HrmPayrollRunDO().setId(run.getId()).setResultSnapshot(JsonUtils.toJsonString(Collections.singletonList(row))));
  Long id=approve("11000");assertEquals(0,new BigDecimal("1000").compareTo(retros.selectById(id).getDelta()));
  assertEquals(0,new BigDecimal("10000").compareTo(retros.selectById(id).getOriginalIncome()));
 }
 @Test void derivedSupplementPaymentsCannotCreateIndependentCorrectionEntitlements(){
  batches.updateById(new HrmSalaryMonthRecordDO().setId(original.getId()).setParentBatchId(999L).setBatchKey("SUPPLEMENT:derived"));
  assertServiceException(()->service.create(req("11000"),1L),PAYROLL_RETRO_INVALID);
  assertServiceException(()->service.supplementBaseline(paymentId,1L),PAYROLL_RETRO_INVALID);
  assertTrue(service.candidates(batchId,1L).isEmpty());
 }
}
