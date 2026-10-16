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
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.employee.info.HrmEmployeeMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.salary.monthrecord.*;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import javax.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
@Import({HrmPayrollCostService.class,org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration.class})
class HrmPayrollCostServiceTest extends BaseDbUnitTest {
 @Resource HrmPayrollCostService service;
 @Resource HrmPayrollCostRuleMapper rules;
 @Resource HrmPayrollCostReportMapper reports;
 @org.springframework.boot.test.mock.mockito.SpyBean HrmPayrollWageRecoveryMapper recoveries;
 @Resource HrmPayrollRunMapper runs;
 @Resource HrmPayrollPaymentMapper payments;
 @Resource HrmSalaryMonthRecordMapper batches;
 @Resource HrmEmployeeMapper employees;
 @Resource HrmPayrollEntityMembershipMapper memberships;
 @MockBean HrmPayrollScopeService scope;
 @MockBean HrmPayrollEntityService entities;
 @MockBean DeptApi departments;
 Long employeeId,batchId,runId;
 HrmPayrollEntityMembershipDO member;
 @BeforeEach void setup(){
  HrmEmployeeDO e=new HrmEmployeeDO().setName("成本员工").setJobNumber("COST-1").setUserId(99L).setDeptId(999L);employees.insert(e);employeeId=e.getId();
  member=new HrmPayrollEntityMembershipDO().setEntityId(10L).setEmployeeId(employeeId).setDeptId(11L).setEmployeeCategory("正式员工").setEffectiveFrom(LocalDate.of(2026,1,1)).setEffectiveTo(LocalDate.of(2026,12,31)).setStatus("APPROVED");memberships.insert(member);
  HrmSalaryMonthRecordDO b=new HrmSalaryMonthRecordDO().setEntityId(10L).setBatchKey("REGULAR").setYear(2026).setMonth(10).setStatus(15).setStartTime(LocalDate.of(2026,10,1).atStartOfDay()).setEndTime(LocalDate.of(2026,10,31).atStartOfDay());batches.insert(b);batchId=b.getId();
  Map<String,Object> input=new LinkedHashMap<>();input.put("entityScope",Collections.singletonMap("memberships",Collections.singletonList(member)));input.put("entityPayrollDates",Collections.singletonMap(employeeId,Collections.singletonList(LocalDate.of(2026,10,20))));
  input.put("employees",Collections.singletonList(e));input.put("insurancePolicySources",Collections.singletonMap(employeeId,new HrmPayrollInsurancePolicyService.Quote().setEmployeeId(employeeId).setEntityId(10L).setCoverage("INSURED").setPayerEntityId(10L).setCorporateInsurance(new BigDecimal("2000")).setCorporateFund(new BigDecimal("500"))));
  HrmSalaryMonthEmployeeRecordDO result=new HrmSalaryMonthEmployeeRecordDO().setMonthRecordId(batchId).setEmployeeId(employeeId).setExpectedPaySalary(new BigDecimal("10000.03")).setPersonalTax(BigDecimal.ONE).setRealPaySalary(new BigDecimal("9999.03")).setOptionValues(Arrays.asList(value(110101,"2000"),value(120101,"500")));
  HrmPayrollRunDO run=new HrmPayrollRunDO().setMonthRecordId(batchId).setVersion(1).setInputSnapshot(JsonUtils.toJsonString(input)).setRuleSnapshot("{}").setResultSnapshot(JsonUtils.toJsonString(Collections.singletonList(result)));runs.insert(run);runId=run.getId();
  when(departments.getDept(any())).thenAnswer(call->new DeptRespDTO().setId(call.getArgument(0)).setName("核定部门"+call.getArgument(0)));
 }
 HrmSalaryMonthEmployeeRecordDO.OptionValue value(int code,String amount){return HrmSalaryMonthEmployeeRecordDO.OptionValue.builder().code(code).value(new BigDecimal(amount)).build();}
 HrmPayrollCostRuleReqVO.Part part(long dept,String percent){return new HrmPayrollCostRuleReqVO.Part().setDeptId(dept).setCostCenter("C"+dept).setProjectCode("P"+dept).setPercent(new BigDecimal(percent));}
 HrmPayrollCostRuleReqVO req(){return new HrmPayrollCostRuleReqVO().setEntityId(10L).setEmployeeId(employeeId).setEffectiveFrom(LocalDate.of(2026,1,1)).setEffectiveTo(LocalDate.of(2026,12,31)).setEmployerCost(new BigDecimal("100"))
  .setParts(Arrays.asList(part(11,"33.3333"),part(22,"33.3333"),part(33,"33.3334"))).setEvidence("正式成本归属和单位成本口径").setRequestKey(UUID.randomUUID().toString());}
 Long approveRule(HrmPayrollCostRuleReqVO r){Long id=service.createRule(r,1L);service.reviewRule(id,true,"独立核实",2L);return id;}
 BigDecimal sum(List<HrmPayrollCostRowVO> rows){return rows.stream().map(HrmPayrollCostRowVO::getTotal).reduce(BigDecimal.ZERO,BigDecimal::add);}
 @Test void defaultAttributionUsesFrozenHistoryAndEmployerContributions(){Long id=service.build(batchId,"冻结工资成本",1L);List<HrmPayrollCostRowVO> rows=service.rows(id,false,1L);assertEquals(1,rows.size());assertEquals(11L,rows.get(0).getDeptId());assertEquals("正式员工",rows.get(0).getEmployeeCategory());assertEquals(0,new BigDecimal("12500.03").compareTo(sum(rows)));}
 @Test void approvedAllocationReconcilesEveryComponentAndExtraCost(){Long rule=approveRule(req());Long report=service.build(batchId,"成本试算",1L);List<HrmPayrollCostRowVO> rows=service.rows(report,false,1L);assertEquals(3,rows.size());assertEquals(0,new BigDecimal("12600.03").compareTo(sum(rows)));assertTrue(rows.stream().allMatch(row->Objects.equals(rule,row.getRuleId())));assertEquals(0,new BigDecimal("2000").compareTo(rows.stream().map(HrmPayrollCostRowVO::getInsurance).reduce(BigDecimal.ZERO,BigDecimal::add)));}
 @Test void pendingRuleIsNotAppliedAndReviewRequiresIndependentActor(){Long rule=service.createRule(req(),1L);assertEquals(1,service.rows(service.build(batchId,"默认归属",1L),false,1L).size());assertServiceException(()->service.reviewRule(rule,true,"提交人",1L),PAYROLL_SELF_APPROVAL);assertServiceException(()->service.reviewRule(rule,true,"本人",99L),PAYROLL_SELF_APPROVAL);}
 @Test void invalidPercentDuplicateDimensionsAndUnknownMembershipAreRejected(){HrmPayrollCostRuleReqVO r=req();r.getParts().get(0).setPercent(BigDecimal.ONE);assertServiceException(()->service.createRule(r,1L),PAYROLL_COST_INVALID);HrmPayrollCostRuleReqVO duplicate=req().setParts(Arrays.asList(part(11,"50"),part(11,"50")));assertServiceException(()->service.createRule(duplicate,1L),PAYROLL_COST_INVALID);HrmEmployeeDO other=new HrmEmployeeDO().setName("其他主体").setJobNumber("OTHER");employees.insert(other);HrmPayrollCostRuleReqVO outsider=req().setEmployeeId(other.getId());assertServiceException(()->service.createRule(outsider,1L),PAYROLL_ENTITY_INVALID);}
 @Test void overlappingApprovalsAndStaleSuccessorsFailButHistoryRemains(){Long first=approveRule(req());Long invalid=service.createRule(req(),1L);assertServiceException(()->service.reviewRule(invalid,true,"重叠规则",2L),PAYROLL_VERSION_CONFLICT);Long report=service.build(batchId,"旧规则报表",1L);Long next=approveRule(req().setPriorRuleId(first).setEmployerCost(new BigDecimal("200")));assertEquals("SUPERSEDED",rules.selectById(first).getStatus());assertEquals(0,new BigDecimal("12600.03").compareTo(sum(service.rows(report,false,1L))));assertEquals(0,new BigDecimal("12700.03").compareTo(sum(service.rows(service.build(batchId,"新规则报表",1L),false,1L))));assertNotEquals(first,next);}
 @Test void requestsAreIdempotentAndCannotChangeAmounts(){HrmPayrollCostRuleReqVO r=req();Long id=service.createRule(r,1L);assertEquals(id,service.createRule(r,1L));r.setEmployerCost(BigDecimal.ONE);assertServiceException(()->service.createRule(r,1L),PAYROLL_COST_INVALID);}
 @Test void reportsNeedFrozenSourceIndependentReviewAndExportAuthorization(){Long id=service.build(batchId,"报告",1L);assertServiceException(()->service.rows(id,true,1L),PAYROLL_COST_INVALID);assertServiceException(()->service.reviewReport(id,true,"本人",1L),PAYROLL_SELF_APPROVAL);service.reviewReport(id,true,"正式核实",2L);assertFalse(service.rows(id,true,2L).isEmpty());doThrow(cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception(PAYROLL_ENTITY_INVALID)).when(scope).check(any(),eq("EXPORT"),eq(3L));assertServiceException(()->service.rows(id,true,3L),PAYROLL_ENTITY_INVALID);batches.updateById(new HrmSalaryMonthRecordDO().setId(batchId).setStatus(11));assertServiceException(()->service.build(batchId,"未冻结",1L),SALARY_MONTH_RECORD_STATUS_INVALID);}
 @Test void corruptSourceCannotBeApprovedOrExported(){Long id=service.build(batchId,"报表",1L);runs.updateById(new HrmPayrollRunDO().setId(runId).setRuleSnapshot("{\"changed\":true}"));assertServiceException(()->service.reviewReport(id,true,"来源已变",2L),PAYROLL_VERSION_CONFLICT);}
 @Test void allocationNeverCreatesNegativeTailAndHandlesSignedCorrections(){List<HrmPayrollCostRuleReqVO.Part> parts=Arrays.asList(part(1,"20"),part(2,"20"),part(3,"20"),part(4,"20"),part(5,"20"));List<BigDecimal> shares=HrmPayrollCostService.allocate(new BigDecimal("0.03"),parts);assertTrue(shares.stream().allMatch(v->v.signum()>=0));assertEquals(new BigDecimal("0.03"),shares.stream().reduce(BigDecimal.ZERO,BigDecimal::add));List<BigDecimal> negative=HrmPayrollCostService.allocate(new BigDecimal("-0.03"),parts);assertTrue(negative.stream().allMatch(v->v.signum()<=0));assertEquals(new BigDecimal("-0.03"),negative.stream().reduce(BigDecimal.ZERO,BigDecimal::add));}
 @Test void newerApprovedRuleRequiresRebuildingPendingReportButApprovedHistoryStaysExportable(){
  Long report=service.build(batchId,"默认归属报告",1L);approveRule(req());
  assertServiceException(()->service.reviewReport(report,true,"旧映射",2L),PAYROLL_VERSION_CONFLICT);
  Long fresh=service.build(batchId,"正式分摊",1L);service.reviewReport(fresh,true,"核实",2L);
  Long prior=rules.selectList().get(0).getId();approveRule(req().setPriorRuleId(prior).setEmployerCost(new BigDecimal("200")));
  assertEquals(0,new BigDecimal("12600.03").compareTo(sum(service.rows(fresh,true,2L))));
 }
 @Test void supplementsFollowOriginalWageDateAndDoNotRepeatEmployerExtras(){
  approveRule(req());HrmSalaryMonthRecordDO original=batches.selectById(batchId);
  member.setEffectiveTo(LocalDate.of(2026,10,20));
  HrmPayrollEntityMembershipDO later=new HrmPayrollEntityMembershipDO().setEntityId(10L).setEmployeeId(employeeId).setDeptId(22L).setEmployeeCategory("调动后").setEffectiveFrom(LocalDate.of(2026,10,21)).setEffectiveTo(LocalDate.of(2026,12,31));
  HrmPayrollPaymentDO payment=new HrmPayrollPaymentDO().setMonthRecordId(batchId).setRunId(runId).setEmployeeId(employeeId).setEmployeeName("成本员工").setBankAccount("test-account").setAttempt(1).setStatus("SUCCESS").setAmount(new BigDecimal("9999.03")).setValueDate(LocalDate.of(2026,10,25));payments.insert(payment);
  HrmSalaryMonthRecordDO supplement=new HrmSalaryMonthRecordDO().setEntityId(10L).setYear(2026).setMonth(10).setStartTime(original.getStartTime()).setEndTime(original.getEndTime()).setStatus(15).setParentBatchId(batchId).setBatchKey("SUPPLEMENT:cost");batches.insert(supplement);
  Map<String,Object> input=new LinkedHashMap<>();input.put("entityScope",Collections.singletonMap("memberships",Arrays.asList(member,later)));input.put("retroSources",Collections.singletonList(new HrmPayrollRetroDO().setEmployeeId(employeeId).setPaymentId(payment.getId())));
  HrmSalaryMonthEmployeeRecordDO row=new HrmSalaryMonthEmployeeRecordDO().setEmployeeId(employeeId).setExpectedPaySalary(new BigDecimal("2000")).setOptionValues(Collections.emptyList());
  HrmPayrollRunDO run=new HrmPayrollRunDO().setMonthRecordId(supplement.getId()).setVersion(1).setInputSnapshot(JsonUtils.toJsonString(input)).setRuleSnapshot("{}").setResultSnapshot(JsonUtils.toJsonString(Collections.singletonList(row)));runs.insert(run);
  Long report=service.build(supplement.getId(),"补发成本",1L);List<HrmPayrollCostRowVO> rows=service.rows(report,false,1L);
  assertTrue(rows.stream().allMatch(r->Objects.equals(11L,r.getSourceDeptId())&&r.getEmployerCost().signum()==0));assertEquals(0,new BigDecimal("2000").compareTo(sum(rows)));
  service.reviewReport(report,true,"正式核实",2L);runs.updateById(new HrmPayrollRunDO().setId(runId).setRuleSnapshot("{\"corruptParent\":true}"));
  assertServiceException(()->service.rows(report,true,2L),PAYROLL_VERSION_CONFLICT);
 }
 @Test void corruptedApprovedDistributionCannotSilentlyDropAnEmployee(){
  Long rule=approveRule(req());rules.updateById(new HrmPayrollCostRuleDO().setId(rule).setParts("[]"));
  assertServiceException(()->service.build(batchId,"损坏规则",1L),PAYROLL_COST_INVALID);assertEquals(0,reports.selectCount());
 }
 @Test void futureSuccessorDoesNotEraseEarlierEffectiveAllocation(){
  Long original=approveRule(req());Long future=approveRule(req().setPriorRuleId(original).setEffectiveFrom(LocalDate.of(2026,11,1)).setEmployerCost(new BigDecimal("200")));
  Long report=service.build(batchId,"十月按原有效规则",1L);assertTrue(service.rows(report,false,1L).stream().allMatch(r->Objects.equals(original,r.getRuleId())));assertEquals(0,new BigDecimal("12600.03").compareTo(sum(service.rows(report,false,1L))));
  Long unrelated=service.createRule(req().setEffectiveTo(LocalDate.of(2026,10,31)),1L);assertServiceException(()->service.reviewRule(unrelated,true,"无接续关系覆盖历史",2L),PAYROLL_VERSION_CONFLICT);
 }
 HrmPayrollWageRecoveryDO recoveryFixture(String status){HrmPayrollPaymentDO p=new HrmPayrollPaymentDO().setMonthRecordId(batchId).setRunId(runId).setEmployeeId(employeeId).setEmployeeName("成本员工").setBankAccount("cost-recovery-bank").setAmount(new BigDecimal("9999.03")).setStatus("SUCCESS").setAttempt(1).setValueDate(LocalDate.of(2026,10,15));payments.insert(p);List<HrmPayrollWageRecoveryService.Allocation> allocation=Collections.singletonList(new HrmPayrollWageRecoveryService.Allocation().setPaymentId(p.getId()).setEmployeeId(employeeId).setCorrectionId(123L).setIncomeReduction(new BigDecimal("2000")));String json=JsonUtils.toJsonString(allocation);HrmPayrollWageRecoveryDO recovery=new HrmPayrollWageRecoveryDO().setRetroId(999L).setEntityId(10L).setEmployeeId(employeeId).setPaymentId(p.getId()).setAmount(new BigDecimal("2000")).setStatus(status).setAllocationSnapshot(json).setAllocationDigest(cn.hutool.crypto.digest.DigestUtil.sha256Hex(json)).setReceiptReference("cost-recovery-receipt").setValueDate(LocalDate.of(2026,10,16)).setRequestKey("cost-recovery-request");recoveries.insert(recovery);return recovery;}
 @Test void settledWageRecoveriesReduceHistoricalCostWithoutChangingFrozenGrossOrEmployerFees(){recoveryFixture("SETTLED");Long report=service.build(batchId,"结清工资收回重编成本",1L);List<HrmPayrollCostRowVO> rows=service.rows(report,false,1L);assertEquals(0,new BigDecimal("10000.03").compareTo(rows.get(0).getGross()));assertEquals(0,new BigDecimal("-2000").compareTo(rows.get(0).getWageRecovery()));assertEquals(0,new BigDecimal("10500.03").compareTo(sum(rows)));assertEquals(0,new BigDecimal("2000").compareTo(rows.get(0).getInsurance()));service.reviewReport(report,true,"独立核对结清差额",2L);assertNotNull(service.approvedReport(batchId,2L));}
 @Test void pendingCashRecoveryDoesNotAdjustCostAndSettlementRequiresAReapprovedReport(){HrmPayrollWageRecoveryDO recovery=recoveryFixture("APPROVED");Long old=service.build(batchId,"原成本",1L);service.reviewReport(old,true,"核实",2L);assertEquals(0,new BigDecimal("12500.03").compareTo(sum(service.rows(old,false,2L))));recoveries.updateById(new HrmPayrollWageRecoveryDO().setId(recovery.getId()).setStatus("SETTLED"));assertNull(service.approvedReport(batchId,2L));assertServiceException(()->service.rows(old,true,2L),PAYROLL_VERSION_CONFLICT);Long updated=service.build(batchId,"结清后新成本",1L);service.reviewReport(updated,true,"核实新差额",2L);assertEquals(0,new BigDecimal("10500.03").compareTo(sum(service.rows(updated,true,2L))));}

 @Resource org.springframework.transaction.PlatformTransactionManager transactionManager;
 @Resource org.mybatis.spring.SqlSessionTemplate sqlSession;
 @Test void recoveryCommittedBetweenCostReadsCannotAttachNewProofToOldAmounts(){HrmPayrollWageRecoveryDO recovery=recoveryFixture("APPROVED");java.util.concurrent.atomic.AtomicBoolean first=new java.util.concurrent.atomic.AtomicBoolean(true);org.springframework.transaction.support.TransactionTemplate other=new org.springframework.transaction.support.TransactionTemplate(transactionManager);other.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  doAnswer(call->{Object snapshot=sqlSession.selectList(HrmPayrollWageRecoveryMapper.class.getName()+".selectList",Collections.singletonMap("ew",call.getArgument(0)));if(first.compareAndSet(true,false))other.execute(status->{recoveries.updateById(new HrmPayrollWageRecoveryDO().setId(recovery.getId()).setStatus("SETTLED"));return null;});return snapshot;}).when(recoveries).selectList(org.mockito.ArgumentMatchers.<com.baomidou.mybatisplus.core.conditions.Wrapper<HrmPayrollWageRecoveryDO>>any());
  Long report=service.build(batchId,"构建中另一事务结清收回",1L);assertEquals(0,new BigDecimal("12500.03").compareTo(sum(service.rows(report,false,1L))));assertServiceException(()->service.reviewReport(report,true,"不能用新摘要批准旧金额",2L),PAYROLL_VERSION_CONFLICT);
 }

 @Resource HrmPayrollInsuranceAdjustmentMapper insuranceAdjustments;
 HrmPayrollInsuranceAdjustmentDO insuranceFixture(String status,String personal,String corporate){HrmPayrollWageRecoveryDO unused=recoveryFixture("CANCELLED");String proof="{\"approvedInsuranceEvidence\":true}";HrmPayrollInsuranceAdjustmentDO a=new HrmPayrollInsuranceAdjustmentDO().setEntityId(10L).setEmployeeId(employeeId).setPaymentId(unused.getPaymentId()).setBillId(123L).setStatus(status).setPersonalDelta(new BigDecimal(personal)).setCorporateDelta(new BigDecimal(corporate)).setSourceSnapshot(proof).setSourceDigest(cn.hutool.crypto.digest.DigestUtil.sha256Hex(proof)).setReceiptReference("insurance-cost-ref").setValueDate(LocalDate.of(2026,10,16)).setRequestKey("insurance-cost-request");insuranceAdjustments.insert(a);return a;}
 @Test void settledEmployerDifferenceAdjustsHistoryWithoutAddingPersonalRecoveryToCost(){insuranceFixture("SETTLED","200","500");Long report=service.build(batchId,"历史单位补缴调整",1L);HrmPayrollCostRowVO row=service.rows(report,false,1L).get(0);assertEquals(0,new BigDecimal("500").compareTo(row.getInsuranceAdjustment()));assertEquals(0,new BigDecimal("13000.03").compareTo(row.getTotal()));assertEquals(0,new BigDecimal("2000").compareTo(row.getInsurance()));assertEquals(0,new BigDecimal("10000.03").compareTo(row.getGross()));}
 @Test void insuranceRefundRequiresNewCostApprovalAndAllocatesSignedCents(){approveRule(req());HrmPayrollInsuranceAdjustmentDO a=insuranceFixture("APPROVED","-200","-500.01");Long old=service.build(batchId,"结清前",1L);service.reviewReport(old,true,"核实",2L);assertNotNull(service.approvedReport(batchId,2L));insuranceAdjustments.updateById(new HrmPayrollInsuranceAdjustmentDO().setId(a.getId()).setStatus("SETTLED"));assertNull(service.approvedReport(batchId,2L));assertServiceException(()->service.rows(old,true,2L),PAYROLL_VERSION_CONFLICT);Long fresh=service.build(batchId,"退款后重新分摊",1L);service.reviewReport(fresh,true,"核实新成本",2L);List<HrmPayrollCostRowVO> rows=service.rows(fresh,true,2L);assertEquals(0,new BigDecimal("-500.01").compareTo(rows.stream().map(HrmPayrollCostRowVO::getInsuranceAdjustment).reduce(BigDecimal.ZERO,BigDecimal::add)));assertEquals(0,new BigDecimal("12100.02").compareTo(sum(rows)));}
}
