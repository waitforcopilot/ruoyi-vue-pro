package cn.iocoder.yudao.module.hrm.service.payroll;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.salary.monthrecord.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.salary.config.HrmSalaryOptionDO;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.salary.monthrecord.HrmSalaryMonthRecordMapper;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import javax.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
@Import(HrmPayrollWageStructureService.class)
class HrmPayrollWageStructureServiceTest extends BaseDbUnitTest {
 @Resource HrmPayrollWageStructureService service;
 @Resource HrmSalaryMonthRecordMapper batches;
 @Resource HrmPayrollRunMapper runs;
 @Resource HrmPayrollPaymentMapper payments;
 @Resource HrmPayrollFinancialEventMapper events;
 @MockBean HrmPayrollEntityService entities;
 @MockBean HrmPayrollScopeService scope;
 Long currentBatch,previousBatch,currentRun,previousRun;
 @BeforeEach void setup(){currentBatch=period(2026,10,"9000","1500","500");previousBatch=period(2026,9,"8000","1000","500");currentRun=runs.selectList(HrmPayrollRunDO::getMonthRecordId,currentBatch).get(0).getId();previousRun=runs.selectList(HrmPayrollRunDO::getMonthRecordId,previousBatch).get(0).getId();when(entities.name(10L)).thenReturn("工资结构主体");}
 HrmSalaryOptionDO option(int code,int parent,int type,String name){return new HrmSalaryOptionDO().setCode(code).setParentCode(parent).setType(type).setName(name);}
 HrmSalaryMonthEmployeeRecordDO.OptionValue value(int code,String amount){return HrmSalaryMonthEmployeeRecordDO.OptionValue.builder().code(code).value(new BigDecimal(amount)).build();}
 List<HrmPayrollEntityMembershipDO> members(YearMonth period){return Arrays.asList(new HrmPayrollEntityMembershipDO().setEmployeeId(1L).setEntityId(10L).setDeptId(11L).setEmployeeCategory("正式").setEffectiveFrom(period.atDay(1)).setEffectiveTo(period.atDay(20)),new HrmPayrollEntityMembershipDO().setEmployeeId(1L).setEntityId(10L).setDeptId(22L).setEmployeeCategory("调动后").setEffectiveFrom(period.atDay(21)).setEffectiveTo(period.atEndOfMonth()));}
 Long period(int year,int month,String basic,String overtime,String deduction){
  YearMonth period=YearMonth.of(year,month);HrmSalaryMonthRecordDO batch=new HrmSalaryMonthRecordDO().setEntityId(10L).setYear(year).setMonth(month).setBatchKey("REGULAR").setStatus(15).setStartTime(period.atDay(1).atStartOfDay()).setEndTime(period.atEndOfMonth().atStartOfDay());batches.insert(batch);
  Map<String,Object> input=new LinkedHashMap<>();input.put("entityScope",Collections.singletonMap("memberships",members(period)));input.put("entityPayrollDates",Collections.singletonMap(1L,Collections.singletonList(period.atDay(20))));
  List<HrmSalaryOptionDO> options=Arrays.asList(option(10101,10,1,"基本工资"),option(180101,180,1,"加班工资"),option(200101,200,0,"考勤扣款"),option(190104,190,0,"原始请假"),option(110101,110,2,"公司社保"),option(210101,210,1,"应发汇总"));
  BigDecimal gross=new BigDecimal(basic).add(new BigDecimal(overtime)).subtract(new BigDecimal(deduction));HrmSalaryMonthEmployeeRecordDO result=new HrmSalaryMonthEmployeeRecordDO().setMonthRecordId(batch.getId()).setEmployeeId(1L).setExpectedPaySalary(gross).setRealPaySalary(gross).setOptionValues(Arrays.asList(value(10101,basic),value(180101,overtime),value(200101,deduction),value(190104,deduction),value(110101,"1000"),value(210101,gross.toPlainString())));
  HrmPayrollRunDO run=new HrmPayrollRunDO().setMonthRecordId(batch.getId()).setVersion(1).setInputSnapshot(JsonUtils.toJsonString(input)).setRuleSnapshot(JsonUtils.toJsonString(Collections.singletonMap("options",options))).setResultSnapshot(JsonUtils.toJsonString(Collections.singletonList(result)));runs.insert(run);return batch.getId();
 }
 HrmPayrollWageStructureService.Result analyze(){return service.analyze(10L,2026,10,null,null,1L);}
 void amount(String expected,BigDecimal value){assertNotNull(value);assertEquals(0,new BigDecimal(expected).compareTo(value));}
 HrmPayrollWageStructureRowVO component(HrmPayrollWageStructureService.Result result,int code){return result.getRows().stream().filter(r->Objects.equals(code,r.getCode())).findFirst().get();}
 @Test void signedComponentsReconcileGrossWithoutCountingRawAttendanceOrEmployerCosts(){HrmPayrollWageStructureService.Result result=analyze();assertTrue(result.isReady());assertTrue(result.isPreviousReady());assertEquals(1,result.getEmployees());amount("10000",result.getGross());amount("8500",result.getPreviousGross());amount("1500",result.getDelta());amount("17.6471",result.getDeltaRate());assertEquals(3,result.getRows().size());HrmPayrollWageStructureRowVO basic=component(result,10101);amount("9000",basic.getAmount());amount("90",basic.getShare());amount("12.5",basic.getDeltaRate());amount("-500",component(result,200101).getAmount());amount("-5",component(result,200101).getShare());assertNull(component(result,200101).getDeltaRate());amount("10000",result.getRows().stream().map(HrmPayrollWageStructureRowVO::getAmount).reduce(BigDecimal.ZERO,BigDecimal::add));}
 @Test void filtersUseLastFrozenWageDayInsteadOfPeriodEndOrCurrentDepartment(){amount("10000",service.analyze(10L,2026,10,11L,"正式",1L).getGross());HrmPayrollWageStructureService.Result other=service.analyze(10L,2026,10,22L,null,1L);assertTrue(other.isReady());amount("0",other.getGross());assertEquals(0,other.getEmployees());assertTrue(other.getRows().isEmpty());}
 @Test void missingPreviousPeriodDoesNotBecomeZeroComparison(){batches.deleteById(previousBatch);HrmPayrollWageStructureService.Result result=analyze();assertFalse(result.isPreviousReady());assertNull(result.getPreviousGross());assertNull(result.getDelta());assertNull(component(result,10101).getPrevious());assertTrue(component(result,10101).getSourceStatus().contains("上期无"));}
 @Test void missingCurrentPeriodDoesNotBecomeZeroOrExport(){batches.updateById(new HrmSalaryMonthRecordDO().setId(currentBatch).setStatus(11));HrmPayrollWageStructureService.Result result=analyze();assertFalse(result.isReady());assertNull(result.getGross());assertNull(component(result,10101).getAmount());assertServiceException(()->service.export(10L,2026,10,null,null,1L),PAYROLL_FINANCIAL_INVALID);}
 @Test void corruptedGrossOrCatalogCannotProducePlausiblePercentages(){HrmPayrollRunDO run=runs.selectById(currentRun);List<HrmSalaryMonthEmployeeRecordDO> rows=JsonUtils.parseArray(run.getResultSnapshot(),HrmSalaryMonthEmployeeRecordDO.class);rows.get(0).setExpectedPaySalary(new BigDecimal("11000"));runs.updateById(new HrmPayrollRunDO().setId(currentRun).setResultSnapshot(JsonUtils.toJsonString(rows)));assertServiceException(this::analyze,PAYROLL_COST_INVALID);}
 @Test void zeroAttendanceDeductionUsesRealMinusTypeAndKeepsZeroComponent(){
  period(2026,12,"6000","0","0");
  HrmPayrollWageStructureService.Result result=service.analyze(10L,2026,12,null,null,1L);
  amount("6000",result.getGross());amount("0",component(result,200101).getAmount());
 }
 @Test void computedUnknownOrMissingTypesUnderGrossCategoriesAreRejectedEvenAtZero(){
  HrmPayrollRunDO original=runs.selectById(currentRun);
  Map<String,Object> savedRules=JsonUtils.parseObject(original.getRuleSnapshot(),Map.class);
  for(Integer invalid:Arrays.asList(2,99,null)){
   List<HrmSalaryOptionDO> options=JsonUtils.parseArray(JsonUtils.toJsonString(savedRules.get("options")),HrmSalaryOptionDO.class);
   options.stream().filter(o->Objects.equals(o.getCode(),10101)).findFirst().get().setType(invalid);
   runs.updateById(new HrmPayrollRunDO().setId(currentRun).setRuleSnapshot(JsonUtils.toJsonString(Collections.singletonMap("options",options))));
   assertServiceException(this::analyze,PAYROLL_COST_INVALID);
  }
  List<HrmSalaryOptionDO> options=JsonUtils.parseArray(JsonUtils.toJsonString(savedRules.get("options")),HrmSalaryOptionDO.class);
  options.add(option(10999,10,2,"非法零值计算项"));
  List<HrmSalaryMonthEmployeeRecordDO> rows=JsonUtils.parseArray(original.getResultSnapshot(),HrmSalaryMonthEmployeeRecordDO.class);
  List<HrmSalaryMonthEmployeeRecordDO.OptionValue> values=new ArrayList<>(rows.get(0).getOptionValues());values.add(value(10999,"0"));rows.get(0).setOptionValues(values);
  runs.updateById(new HrmPayrollRunDO().setId(currentRun).setRuleSnapshot(JsonUtils.toJsonString(Collections.singletonMap("options",options))).setResultSnapshot(JsonUtils.toJsonString(rows)));
  assertServiceException(this::analyze,PAYROLL_COST_INVALID);
 }
 @Test void duplicateWorkersAndInvalidAdjustmentLineageAreRejected(){HrmPayrollRunDO run=runs.selectById(currentRun);List<HrmSalaryMonthEmployeeRecordDO> rows=JsonUtils.parseArray(run.getResultSnapshot(),HrmSalaryMonthEmployeeRecordDO.class);rows.add(rows.get(0));runs.updateById(new HrmPayrollRunDO().setId(currentRun).setResultSnapshot(JsonUtils.toJsonString(rows)));assertServiceException(this::analyze,PAYROLL_COST_INVALID);runs.updateById(new HrmPayrollRunDO().setId(currentRun).setInputSnapshot(JsonUtils.toJsonString(Collections.singletonMap("sourceRunId",previousRun))));assertServiceException(this::analyze,PAYROLL_VERSION_CONFLICT);}
 @Test void itemRenamingPreservesBothPeriodLabelsAndFlagsChangedMeaning(){HrmPayrollRunDO run=runs.selectById(previousRun);Map<String,Object> rules=JsonUtils.parseObject(run.getRuleSnapshot(),Map.class);List<HrmSalaryOptionDO> options=JsonUtils.parseArray(JsonUtils.toJsonString(rules.get("options")),HrmSalaryOptionDO.class);options.get(0).setName("上期工资定义");runs.updateById(new HrmPayrollRunDO().setId(previousRun).setRuleSnapshot(JsonUtils.toJsonString(Collections.singletonMap("options",options))));HrmPayrollWageStructureRowVO row=component(analyze(),10101);assertEquals("基本工资",row.getName());assertEquals("上期工资定义",row.getPreviousName());assertTrue(row.getSourceStatus().contains("名称变化"));}
 @Test void zeroGrossRetainsRealAmountsWithoutFalseShares(){Long zero=period(2026,11,"9000","0","9000");HrmPayrollWageStructureService.Result result=service.analyze(10L,2026,11,null,null,1L);amount("0",result.getGross());assertTrue(result.getRows().stream().allMatch(r->r.getShare()==null));assertTrue(result.getSourceStatus().contains("非正"));}
 @Test void januaryComparisonUsesPreviousCalendarYear(){period(2026,1,"9000","1500","500");period(2025,12,"8000","1000","500");HrmPayrollWageStructureService.Result result=service.analyze(10L,2026,1,null,null,1L);assertTrue(result.isPreviousReady());amount("8500",result.getPreviousGross());}
 @Test void supplementalGrossUsesOriginalPaidHistoryAndDoesNotDoubleCountPeople(){
  HrmPayrollPaymentDO payment=new HrmPayrollPaymentDO().setMonthRecordId(currentBatch).setRunId(currentRun).setEmployeeId(1L).setEmployeeName("补发员工").setBankAccount("test-account").setAmount(new BigDecimal("10000")).setAttempt(1).setStatus("SUCCESS").setValueDate(LocalDate.of(2026,10,25));payments.insert(payment);
  HrmSalaryMonthRecordDO batch=new HrmSalaryMonthRecordDO().setEntityId(10L).setParentBatchId(currentBatch).setBatchKey("SUPPLEMENT:structure").setYear(2026).setMonth(10).setStatus(15).setStartTime(LocalDate.of(2026,10,1).atStartOfDay()).setEndTime(LocalDate.of(2026,10,31).atStartOfDay());batches.insert(batch);
  Map<String,Object> input=new LinkedHashMap<>();input.put("entityScope",Collections.singletonMap("memberships",members(YearMonth.of(2026,10))));input.put("retroSources",Collections.singletonList(new HrmPayrollRetroDO().setEmployeeId(1L).setPaymentId(payment.getId())));
  HrmSalaryMonthEmployeeRecordDO row=new HrmSalaryMonthEmployeeRecordDO().setMonthRecordId(batch.getId()).setEmployeeId(1L).setExpectedPaySalary(new BigDecimal("2000")).setOptionValues(Collections.singletonList(value(10901,"2000")));
  HrmPayrollRunDO run=new HrmPayrollRunDO().setMonthRecordId(batch.getId()).setVersion(1).setInputSnapshot(JsonUtils.toJsonString(input)).setRuleSnapshot(JsonUtils.toJsonString(Collections.singletonMap("option",option(10901,10,1,"批准工资补发")))).setResultSnapshot(JsonUtils.toJsonString(Collections.singletonList(row)));runs.insert(run);
  HrmPayrollWageStructureService.Result result=service.analyze(10L,2026,10,11L,null,1L);amount("12000",result.getGross());assertEquals(1,result.getEmployees());amount("2000",component(result,10901).getAmount());
 }
 @Test void exportsRequireSourceScopeAndDoNotCopyPrivateWageRows(){List<HrmPayrollWageStructureRowVO> rows=service.export(10L,2026,10,11L,"正式",1L);assertFalse(rows.isEmpty());assertEquals(1,events.selectCount());assertTrue(events.selectList().get(0).getSnapshot().contains("runIds"));doThrow(cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception(PAYROLL_ENTITY_INVALID)).when(scope).check(any(),eq("EXPORT"),eq(3L));assertServiceException(()->service.export(10L,2026,10,null,null,3L),PAYROLL_ENTITY_INVALID);}
}
