package cn.iocoder.yudao.module.hrm.service.payroll;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.salary.employee.HrmSalaryEmployeeInfoDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.salary.config.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.salary.monthrecord.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.employee.info.HrmEmployeeMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.salary.employee.HrmSalaryEmployeeInfoMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.salary.monthrecord.HrmSalaryMonthRecordMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.HrmPayrollRunMapper;
import cn.iocoder.yudao.module.hrm.service.salary.config.*;
import cn.iocoder.yudao.module.hrm.service.salary.employee.HrmSalaryEmployeeInfoService;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;
import javax.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
@Import({HrmPayrollBatchService.class,HrmPayrollSalarySourceService.class,HrmPayrollConfigGuardService.class})
class HrmPayrollSalarySourceServiceTest extends BaseDbUnitTest {
 @Resource HrmPayrollBatchService batchService;
 @Resource HrmPayrollSalarySourceService service;
 @Resource HrmPayrollConfigGuardService guard;
 @Resource HrmSalaryMonthRecordMapper batches;
 @Resource HrmPayrollRunMapper runs;
 @Resource HrmEmployeeMapper employees;
 @Resource HrmSalaryEmployeeInfoMapper salaryMapper;
 @MockBean HrmPayrollScopeService scope;
 @MockBean HrmSalaryEmployeeInfoService salaries;
 @MockBean HrmSalaryGroupService groups;
 @MockBean HrmSalaryTaxRuleService taxRules;
 @MockBean HrmSalaryOptionService options;
 @MockBean HrmSalaryConfigService config;
 @MockBean HrmPayrollFormulaService formulas;
 @MockBean HrmPayrollSourceClaimService sourceClaims;
 @MockBean HrmPayrollRetroService retroService;
 @MockBean HrmPayrollTaxWithholdingService taxWithholding;
 @MockBean HrmPayrollInputService inputs;
 @MockBean HrmPayrollInputIssueService inputIssues;
 @MockBean HrmPayrollAttendanceService attendance;
 @MockBean HrmPayrollInsurancePolicyService insurance;
 @MockBean HrmPayrollWorkService work;
 Long employeeId,batchId,runId,salaryId;Map<Long,HrmSalaryGroupDO> assigned;Map<Long,HrmSalaryTaxRuleDO> taxes;List<HrmSalaryOptionDO> catalog;HrmSalaryConfigDO settings;
 List<HrmSalaryEmployeeInfoDO.SalaryOption> values(String amount){return Collections.singletonList(new HrmSalaryEmployeeInfoDO.SalaryOption().setCode(10101).setName("基本工资").setValue(new BigDecimal(amount)));}
 @BeforeEach void setup(){HrmEmployeeDO employee=new HrmEmployeeDO().setName("冻结工资员工").setJobNumber("FREEZE-1");employees.insert(employee);employeeId=employee.getId();HrmSalaryEmployeeInfoDO salary=new HrmSalaryEmployeeInfoDO().setEmployeeId(employeeId).setSalaryOptions(values("10000.00"));salaryMapper.insert(salary);salaryId=salary.getId();
  HrmSalaryMonthRecordDO b=new HrmSalaryMonthRecordDO().setEntityId(10L).setBatchKey("REGULAR").setYear(2026).setMonth(10).setStatus(11).setStartTime(LocalDate.of(2026,10,1).atStartOfDay()).setEndTime(LocalDate.of(2026,10,31).atStartOfDay()).setExpectedPaymentDate(LocalDate.of(2026,11,5));batches.insert(b);batchId=b.getId();
  assigned=Collections.singletonMap(employeeId,new HrmSalaryGroupDO().setId(21L).setTaxRuleId(22L).setSalaryStandard(new BigDecimal("21.75")));taxes=Collections.singletonMap(employeeId,new HrmSalaryTaxRuleDO().setId(22L).setTaxEnabled(true).setType(1));catalog=Collections.singletonList(new HrmSalaryOptionDO().setCode(10101).setParentCode(10).setType(1).setTaxEnabled(true));settings=new HrmSalaryConfigDO().setSocialSecurityMonthType(1);
  when(scope.payrollDates(any())).thenReturn(Collections.singletonMap(employeeId,Collections.singleton(LocalDate.of(2026,10,1))));when(salaries.getEffectiveSalaryOptionList(any(),any(),anySet())).thenAnswer(call->salaryMapper.selectById(salaryId).getSalaryOptions());when(groups.getEmployeeSalaryGroupMap(anyList())).thenReturn(assigned);when(taxRules.getSalaryTaxRuleMap(anyCollection())).thenReturn(Collections.singletonMap(22L,taxes.get(employeeId)));when(options.getSalaryOptionList(false)).thenReturn(catalog);when(formulas.effective(any())).thenReturn(Collections.emptyList());when(config.getSalaryConfig()).thenReturn(settings);
  Map<String,Object> input=new LinkedHashMap<>();input.put("effectiveSalaryValues",Collections.singletonMap(employeeId,values("10000")));input.put("salarySourceConfig",settings);input.put("salarySourceDigest",service.digest(input.get("effectiveSalaryValues"),assigned,taxes,catalog,Collections.emptyList(),settings));
  Map<String,Object> rules=new LinkedHashMap<>();rules.put("salaryGroups",assigned);rules.put("taxRules",taxes);rules.put("options",catalog);rules.put("formulas",Collections.emptyList());
  runId=batchService.snapshot(batchId,input,rules,Collections.singletonList(new HrmSalaryMonthEmployeeRecordDO().setEmployeeId(employeeId).setExpectedPaySalary(new BigDecimal("10000")).setPersonalTax(BigDecimal.ZERO).setRealPaySalary(new BigDecimal("10000")).setOptionValues(Collections.emptyList())),1L);batches.updateById(new HrmSalaryMonthRecordDO().setId(batchId).setStatus(14));
 }
 @Test void salaryChangedAfterTrialMustBlockFreezeBeforeSourceReservation(){salaryMapper.updateById(new HrmSalaryEmployeeInfoDO().setId(salaryId).setSalaryOptions(values("11000")));assertServiceException(()->batchService.transition(batchId,runId,"freeze","定薪已变更",3L),PAYROLL_SALARY_SOURCE_STALE);assertEquals(14,batches.selectById(batchId).getStatus());verify(sourceClaims,never()).reserve(any(),any(),any());}
 @Test void unchangedSourcesFreezeAndEquivalentDecimalScalesDoNotInvalidate(){salaryMapper.updateById(new HrmSalaryEmployeeInfoDO().setId(salaryId).setSalaryOptions(values("10000.0000")));batchService.transition(batchId,runId,"freeze","来源一致",3L);assertEquals(15,batches.selectById(batchId).getStatus());verify(sourceClaims).reserve(any(),any(),eq(3L));}
 @Test void groupAndTaxRuleChangesInvalidatePreviousTrial(){assigned.get(employeeId).setSalaryStandard(new BigDecimal("22"));assertServiceException(()->batchService.transition(batchId,runId,"freeze","计薪标准已变",3L),PAYROLL_SALARY_SOURCE_STALE);assigned.get(employeeId).setSalaryStandard(new BigDecimal("21.75"));taxes.get(employeeId).setTaxEnabled(false);assertServiceException(()->batchService.transition(batchId,runId,"freeze","计税配置已变",3L),PAYROLL_SALARY_SOURCE_STALE);}
 @Test void catalogAndFormulaAndContributionMonthChangesInvalidatePreviousTrial(){catalog.get(0).setEnabled(false);assertServiceException(()->batchService.transition(batchId,runId,"freeze","目录已变",3L),PAYROLL_SALARY_SOURCE_STALE);catalog.get(0).setEnabled(null);when(formulas.effective(any())).thenReturn(Collections.singletonList(new HrmPayrollFormulaDO().setId(99L).setStatus("APPROVED")));assertServiceException(()->batchService.transition(batchId,runId,"freeze","公式已变",3L),PAYROLL_SALARY_SOURCE_STALE);when(formulas.effective(any())).thenReturn(Collections.emptyList());settings.setSocialSecurityMonthType(0);assertServiceException(()->batchService.transition(batchId,runId,"freeze","缴费月份已变",3L),PAYROLL_SALARY_SOURCE_STALE);}
 @Test void missingHistoricalProofRequiresRecalculationAndStoredEvidenceTamperingIsRejected(){HrmPayrollRunDO run=runs.selectById(runId);Map<String,Object> input=JsonUtils.parseObject(run.getInputSnapshot(),Map.class);input.remove("salarySourceDigest");runs.updateById(new HrmPayrollRunDO().setId(runId).setInputSnapshot(JsonUtils.toJsonString(input)));assertServiceException(()->batchService.transition(batchId,runId,"freeze","旧试算缺证明",3L),PAYROLL_SALARY_SOURCE_STALE);runs.updateById(new HrmPayrollRunDO().setId(runId).setInputSnapshot(run.getInputSnapshot()));input=JsonUtils.parseObject(run.getInputSnapshot(),Map.class);input.put("effectiveSalaryValues",Collections.singletonMap(employeeId,values("1")));runs.updateById(new HrmPayrollRunDO().setId(runId).setInputSnapshot(JsonUtils.toJsonString(input)));assertServiceException(()->batchService.transition(batchId,runId,"freeze","证明失配",3L),PAYROLL_SALARY_SOURCE_STALE);}
 @Test void manualAdjustmentCannotBypassOriginalSalarySourceProof(){batches.updateById(new HrmSalaryMonthRecordDO().setId(batchId).setStatus(11));Long adjusted=batchService.snapshot(batchId,Collections.singletonMap("sourceRunId",runId),Collections.emptyMap(),JsonUtils.parseArray(runs.selectById(runId).getResultSnapshot(),HrmSalaryMonthEmployeeRecordDO.class),2L);batches.updateById(new HrmSalaryMonthRecordDO().setId(batchId).setStatus(14));salaryMapper.updateById(new HrmSalaryEmployeeInfoDO().setId(salaryId).setSalaryOptions(values("11000")));assertServiceException(()->batchService.transition(batchId,adjusted,"freeze","人工版本不得跳过根来源",3L),PAYROLL_SALARY_SOURCE_STALE);}
 @Test void configurationGuardRequiresTransactionAndSerializesCompetingWrites() throws Exception {
  assertThrows(org.springframework.transaction.IllegalTransactionStateException.class,()->guard.lock());
  org.springframework.transaction.support.TransactionTemplate template=new org.springframework.transaction.support.TransactionTemplate(manager);java.util.concurrent.CountDownLatch locked=new java.util.concurrent.CountDownLatch(1),release=new java.util.concurrent.CountDownLatch(1),started=new java.util.concurrent.CountDownLatch(1);java.util.concurrent.ExecutorService executor=java.util.concurrent.Executors.newFixedThreadPool(2);
  try{java.util.concurrent.Future<?> holder=executor.submit(()->template.execute(status->{guard.lock();locked.countDown();try{if(!release.await(5,java.util.concurrent.TimeUnit.SECONDS))throw new IllegalStateException("timeout");}catch(InterruptedException e){throw new IllegalStateException(e);}return null;}));assertTrue(locked.await(5,java.util.concurrent.TimeUnit.SECONDS));java.util.concurrent.Future<?> writer=executor.submit(()->{started.countDown();return template.execute(status->{guard.lock();salaryMapper.updateById(new HrmSalaryEmployeeInfoDO().setId(salaryId).setSalaryOptions(values("12000")));return null;});});assertTrue(started.await(5,java.util.concurrent.TimeUnit.SECONDS));assertThrows(java.util.concurrent.TimeoutException.class,()->writer.get(200,java.util.concurrent.TimeUnit.MILLISECONDS));release.countDown();holder.get(5,java.util.concurrent.TimeUnit.SECONDS);writer.get(5,java.util.concurrent.TimeUnit.SECONDS);assertEquals(0,new BigDecimal("12000").compareTo(salaryMapper.selectById(salaryId).getSalaryOptions().get(0).getValue()));}finally{release.countDown();executor.shutdownNow();}
 }
 @Resource org.springframework.transaction.PlatformTransactionManager manager;

}
