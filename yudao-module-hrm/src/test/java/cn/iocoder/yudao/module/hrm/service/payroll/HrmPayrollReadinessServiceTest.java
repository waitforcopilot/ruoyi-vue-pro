package cn.iocoder.yudao.module.hrm.service.payroll;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.salary.config.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.salary.employee.HrmSalaryEmployeeInfoDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.salary.monthrecord.HrmSalaryMonthRecordDO;
import cn.iocoder.yudao.module.hrm.dal.mysql.employee.info.HrmEmployeeMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.HrmPayrollInputMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.salary.monthrecord.HrmSalaryMonthRecordMapper;
import cn.iocoder.yudao.module.hrm.service.salary.config.*;
import cn.iocoder.yudao.module.hrm.service.salary.employee.HrmSalaryEmployeeInfoService;
import java.time.*;
import java.util.*;
import javax.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
@Import(HrmPayrollReadinessService.class)
class HrmPayrollReadinessServiceTest extends BaseDbUnitTest {
 @Resource HrmPayrollReadinessService service;
 @Resource HrmSalaryMonthRecordMapper batches;
 @Resource HrmEmployeeMapper employees;
 @Resource HrmPayrollInputMapper inputMapper;
 @MockBean HrmPayrollScopeService scope;
 @MockBean HrmPayrollInputService inputs;
 @MockBean HrmSalaryGroupService groups;
 @MockBean HrmSalaryTaxRuleService taxRules;
 @MockBean HrmSalaryConfigService config;
 @MockBean HrmSalaryEmployeeInfoService salaries;
 @MockBean HrmPayrollInsurancePolicyService insurance;
 @MockBean HrmPayrollTaxPolicyService policies;
 @MockBean HrmPayrollTaxProfileService profiles;
 Long batchId,employeeId;HrmSalaryTaxRuleDO taxRule;Set<LocalDate> dates=new HashSet<>(Arrays.asList(LocalDate.of(2026,10,31)));
 @BeforeEach void setup(){
  HrmSalaryMonthRecordDO b=new HrmSalaryMonthRecordDO().setEntityId(10L).setYear(2026).setMonth(10).setBatchKey("REGULAR").setStatus(11).setStartTime(LocalDate.of(2026,10,1).atStartOfDay()).setEndTime(LocalDate.of(2026,10,31).atStartOfDay()).setExpectedPaymentDate(LocalDate.of(2027,1,5));batches.insert(b);batchId=b.getId();
  HrmEmployeeDO employee=new HrmEmployeeDO().setName("就绪员工").setJobNumber("READY-1").setEntryStatus(1);employees.insert(employee);employeeId=employee.getId();
  when(scope.payrollDates(any())).thenReturn(Collections.singletonMap(employeeId,dates));
  when(groups.getEmployeeSalaryGroupMap(anyList())).thenReturn(Collections.singletonMap(employeeId,new HrmSalaryGroupDO().setId(21L).setTaxRuleId(22L)));
  taxRule=new HrmSalaryTaxRuleDO().setId(22L).setType(1).setTaxEnabled(true);when(taxRules.getSalaryTaxRuleMap(anyCollection())).thenReturn(Collections.singletonMap(22L,taxRule));
 }
 HrmPayrollReadinessService.Signal inspect(String kind){return service.inspect(batchId,employeeId,kind,1L);}
 @Test void paymentDateCannotBeImplicitlyGuessed(){assertTrue(inspect("PAYMENT_PLAN").isReady());batches.update(null,new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<HrmSalaryMonthRecordDO>().eq(HrmSalaryMonthRecordDO::getId,batchId).set(HrmSalaryMonthRecordDO::getExpectedPaymentDate,null));assertFalse(inspect("PAYMENT_PLAN").isReady());}
 @Test void membershipAndActorScopeAreEnforcedBeforeSourcesAreRead(){when(scope.payrollDates(any())).thenReturn(Collections.emptyMap());assertServiceException(()->inspect("SALARY_INFO"),PAYROLL_INPUT_ISSUE_INVALID);verifyNoInteractions(salaries);when(scope.payrollDates(any())).thenReturn(Collections.singletonMap(employeeId,dates));doThrow(cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception(PAYROLL_ENTITY_INVALID)).when(scope).check(any(),eq("READ"),eq(1L));assertServiceException(()->inspect("PAYMENT_PLAN"),PAYROLL_ENTITY_INVALID);}
 @Test void salaryRequiresEffectiveValuesForExactFullDayPayrollWindow(){assertFalse(inspect("SALARY_INFO").isReady());when(salaries.getEffectiveSalaryOptionList(any(),any(),eq(dates))).thenReturn(Collections.singletonList(new HrmSalaryEmployeeInfoDO.SalaryOption()));assertTrue(inspect("SALARY_INFO").isReady());org.mockito.ArgumentCaptor<LocalDateTime[]> range=org.mockito.ArgumentCaptor.forClass(LocalDateTime[].class);verify(salaries,times(2)).getEffectiveSalaryOptionList(any(),range.capture(),eq(dates));assertEquals(LocalDate.of(2026,10,1).atStartOfDay(),range.getValue()[0]);assertEquals(LocalDate.of(2026,10,31).atTime(LocalTime.MAX),range.getValue()[1]);}
 @Test void salaryGroupRequiresExplicitValidTaxConfiguration(){assertTrue(inspect("SALARY_GROUP").isReady());taxRule.setType(999);assertFalse(inspect("SALARY_GROUP").isReady());taxRule.setType(null);assertFalse(inspect("SALARY_GROUP").isReady());when(groups.getEmployeeSalaryGroupMap(anyList())).thenReturn(Collections.emptyMap());assertFalse(inspect("SALARY_GROUP").isReady());}
 @Test void insuranceUsesConfiguredContributionMonthAndHashesPrivateAmounts(){when(config.getSalaryConfig()).thenReturn(new HrmSalaryConfigDO().setSocialSecurityMonthType(0));HrmPayrollInsurancePolicyService.Quote quote=new HrmPayrollInsurancePolicyService.Quote().setBaseId(33L).setCoverage("INSURED").setBaseEvidence("private-evidence-8899").setPersonalInsurance(new java.math.BigDecimal("1234.56"));when(insurance.preview(eq(10L),anyCollection(),any(),eq(1L))).thenReturn(Collections.singletonMap(employeeId,quote));HrmPayrollReadinessService.Signal first=inspect("INSURANCE");assertTrue(first.isReady());assertEquals(33L,first.getSourceId());assertFalse(JsonUtils.toJsonString(first).contains("1234.56"));assertFalse(JsonUtils.toJsonString(first).contains("private-evidence"));quote.setPersonalInsurance(java.math.BigDecimal.ONE);assertNotEquals(first.getProofHash(),inspect("INSURANCE").getProofHash());verify(insurance,times(2)).preview(eq(10L),eq(Collections.singleton(employeeId)),eq(YearMonth.of(2026,9)),eq(1L));}
 @Test void taxYearFollowsPlannedCashDateAndResidentProfileIsRequired(){when(policies.requireApproved(10L,2027)).thenReturn(new HrmPayrollTaxPolicyDO().setId(44L));when(profiles.requireApproved(10L,employeeId,2027)).thenReturn(new HrmPayrollTaxProfileDO().setId(45L).setResidentThisYear(false));assertTrue(inspect("TAX_POLICY").isReady());assertFalse(inspect("TAX_PROFILE").isReady());when(profiles.requireApproved(10L,employeeId,2027)).thenReturn(new HrmPayrollTaxProfileDO().setId(46L).setResidentThisYear(true));assertTrue(inspect("TAX_PROFILE").isReady());}
 @Test void unknownTaxTypeCannotBecomeNotApplicable(){taxRule.setType(999);assertFalse(inspect("TAX_POLICY").isReady());taxRule.setType(null);assertFalse(inspect("TAX_PROFILE").isReady());taxRule.setType(3);assertEquals("NOT_APPLICABLE",inspect("TAX_PROFILE").getStatus());verifyNoInteractions(profiles,policies);}
 @Test void approvedInputNeedsAnExistingSourceAndSnapshotChangesAffectProof(){when(inputs.readiness(eq(batchId),eq(employeeId),eq("ATTENDANCE"),eq(1L))).thenReturn(new HrmPayrollInputService.Readiness().setStatus("APPROVED").setInputId(999L));assertEquals("STALE",inspect("ATTENDANCE").getStatus());HrmPayrollInputDO row=new HrmPayrollInputDO().setBatchId(batchId).setEmployeeId(employeeId).setKind("ATTENDANCE").setVersion(1).setStatus("APPROVED").setPayload("private-input").setSourceSnapshot("source-v1");inputMapper.insert(row);when(inputs.readiness(eq(batchId),eq(employeeId),eq("ATTENDANCE"),eq(1L))).thenReturn(new HrmPayrollInputService.Readiness().setStatus("APPROVED").setInputId(row.getId()));HrmPayrollReadinessService.Signal first=inspect("ATTENDANCE");assertTrue(first.isReady());assertFalse(JsonUtils.toJsonString(first).contains("private-input"));inputMapper.updateById(new HrmPayrollInputDO().setId(row.getId()).setSourceSnapshot("source-v2"));assertNotEquals(first.getProofHash(),inspect("ATTENDANCE").getProofHash());}
 @Test void configurationFailuresReturnGenericReasonsWithoutSourcePayload(){when(policies.requireApproved(anyLong(),anyInt())).thenThrow(cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception(PAYROLL_TAX_INVALID));assertFalse(inspect("TAX_POLICY").isReady());assertNotNull(inspect("TAX_POLICY").getProofHash());}
}
