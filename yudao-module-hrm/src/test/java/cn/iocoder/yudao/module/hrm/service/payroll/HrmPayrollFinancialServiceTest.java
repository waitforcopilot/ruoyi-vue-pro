package cn.iocoder.yudao.module.hrm.service.payroll;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.salary.monthrecord.HrmSalaryMonthRecordDO;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.salary.monthrecord.HrmSalaryMonthRecordMapper;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import java.math.BigDecimal;
import java.util.*;
import javax.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
@Import({HrmPayrollFinancialService.class,org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration.class})
class HrmPayrollFinancialServiceTest extends BaseDbUnitTest {
 @Resource HrmPayrollFinancialService service;
 @Resource HrmPayrollFinancialPlanMapper plans;
 @Resource HrmSalaryMonthRecordMapper batches;
 @MockBean HrmPayrollEntityService entities;
 @MockBean HrmPayrollScopeService scope;
 @MockBean HrmPayrollCostService costs;
 @MockBean DeptApi departments;
 Long batchId;
 @BeforeEach void setup(){
  HrmSalaryMonthRecordDO b=new HrmSalaryMonthRecordDO().setEntityId(10L).setYear(2026).setMonth(10).setBatchKey("REGULAR").setStatus(15);batches.insert(b);batchId=b.getId();
  List<HrmPayrollCostRowVO> rows=Arrays.asList(cost(1L,11L,"6000"),cost(1L,22L,"2000"),cost(2L,11L,"2000"));
  when(costs.approvedReport(eq(batchId),any())).thenReturn(new HrmPayrollCostReportDO().setId(100L).setBatchId(batchId).setVersion(1).setStatus("APPROVED").setResultSnapshot(JsonUtils.toJsonString(rows)));
  when(entities.name(10L)).thenReturn("批准主体");
 }
 HrmPayrollCostRowVO cost(Long employee,Long dept,String amount){return new HrmPayrollCostRowVO().setEmployeeId(employee).setDeptId(dept).setDeptName("部门"+dept).setCostCenter("CC1").setProjectCode("PJ1").setEmployeeCategory("正式").setTotal(new BigDecimal(amount));}
 HrmPayrollFinancialPlanReqVO req(){return new HrmPayrollFinancialPlanReqVO().setEntityId(10L).setYear(2026).setMonth(10).setDimension("ENTITY").setDimensionKey("ALL").setBudget(new BigDecimal("9000")).setRevenue(new BigDecimal("50000")).setHeadcount(new BigDecimal("3")).setEvidence("财务批准预算及营收/编制来源").setRequestKey(UUID.randomUUID().toString());}
 Long approve(HrmPayrollFinancialPlanReqVO req){Long id=service.create(req,1L);service.review(id,true,"独立财务核实",2L);return id;}
 HrmPayrollCostAnalysisRowVO entity(HrmPayrollFinancialService.Analysis analysis){return analysis.getRows().stream().filter(row->"ENTITY".equals(row.getDimension())).findFirst().get();}
 void amount(String expected,BigDecimal value){assertNotNull(value);assertEquals(0,new BigDecimal(expected).compareTo(value));}
 @Test void approvedPlansProduceExactVariancesAndUniqueHeadcounts(){Long id=approve(req());HrmPayrollFinancialService.Analysis a=service.analyze(10L,2026,10,1L);assertTrue(a.isReady());HrmPayrollCostAnalysisRowVO row=entity(a);assertEquals(id,row.getPlanId());amount("10000",row.getActualCost());assertEquals(2,row.getEmployees());amount("1000",row.getBudgetVariance());amount("11.1111",row.getBudgetVarianceRate());amount("20",row.getCostRevenueRate());amount("5000",row.getCostPerEmployee());amount("3333.33",row.getCostPerPosition());amount("-1",row.getStaffingVariance());assertEquals(Collections.singletonList(100L),a.getReportIds());}
 @Test void pendingSourcesDoNotBecomeBudgetsAndIndependentReviewIsRequired(){Long id=service.create(req(),1L);assertNull(entity(service.analyze(10L,2026,10,1L)).getBudget());assertServiceException(()->service.review(id,true,"提交人",1L),PAYROLL_SELF_APPROVAL);service.review(id,false,"不予批准",2L);assertNull(entity(service.analyze(10L,2026,10,1L)).getBudget());}
 @Test void missingAndZeroDenominatorsAreDifferentFromFabricatedMetrics(){approve(req().setRevenue(BigDecimal.ZERO).setHeadcount(BigDecimal.ZERO).setBudget(BigDecimal.ZERO));HrmPayrollCostAnalysisRowVO row=entity(service.analyze(10L,2026,10,1L));assertNull(row.getCostRevenueRate());assertNull(row.getCostPerPosition());assertNull(row.getBudgetVarianceRate());amount("10000",row.getBudgetVariance());assertTrue(row.getSourceStatus().contains("已确认营收为零"));assertTrue(row.getSourceStatus().contains("编制为零"));}
 @Test void incompleteCostSourceBlocksActualsAndExports(){approve(req());when(costs.approvedReport(eq(batchId),any())).thenReturn(null);HrmPayrollFinancialService.Analysis a=service.analyze(10L,2026,10,1L);assertFalse(a.isReady());assertEquals(Collections.singletonList(batchId),a.getMissingBatchIds());assertNull(entity(a).getActualCost());assertNull(entity(a).getCostRevenueRate());amount("9000",entity(a).getBudget());assertServiceException(()->service.export(10L,2026,10,"ENTITY",1L),PAYROLL_FINANCIAL_INVALID);}
 @Test void noFrozenPeriodDoesNotInventZeroActualCost(){batches.updateById(new HrmSalaryMonthRecordDO().setId(batchId).setStatus(11));assertFalse(service.analyze(10L,2026,10,1L).isReady());assertNull(entity(service.analyze(10L,2026,10,1L)).getActualCost());}
 @Test void newVersionsKeepHistoryAndRejectStaleCompetingApprovals(){Long first=approve(req());Long a=service.create(req().setPriorPlanId(first).setBudget(new BigDecimal("8000")),1L),b=service.create(req().setPriorPlanId(first).setBudget(new BigDecimal("7000")),1L);service.review(a,true,"批准接续",2L);assertServiceException(()->service.review(b,true,"旧基线",2L),PAYROLL_VERSION_CONFLICT);assertEquals("SUPERSEDED",plans.selectById(first).getStatus());amount("9000",plans.selectById(first).getBudget());amount("8000",entity(service.analyze(10L,2026,10,1L)).getBudget());}
 @Test void retriesAreCanonicalAndCannotChangeFinancialValues(){HrmPayrollFinancialPlanReqVO r=req().setDimension("DEPARTMENT").setDimensionKey("0011");Long id=service.create(r,1L);r.setDimensionKey("0011").setBudget(new BigDecimal("9000.00"));assertEquals(id,service.create(r,1L));r.setRevenue(BigDecimal.ONE);assertServiceException(()->service.create(r,1L),PAYROLL_FINANCIAL_INVALID);}
 HrmPayrollFinancialImportRowVO fileRow(String dimension,String key){return new HrmPayrollFinancialImportRowVO().setYear("2026").setMonth("10").setDimension(dimension).setDimensionKey(key).setBudget("9000").setEvidence("正式文件依据");}
 @Test void previewIdentifiesNumericPeriodDuplicateAndMissingEvidenceRows(){List<HrmPayrollFinancialImportRowVO> rows=Arrays.asList(fileRow("ENTITY","ALL"),fileRow("ENTITY","ALL"),fileRow("DEPARTMENT","11").setBudget("bad").setSourceRow(8).setSourceSheet("Sheet2"),fileRow("DEPARTMENT","12").setMonth("9"),fileRow("PROJECT","PJ1").setEvidence(""));List<HrmPayrollFinancialPreviewVO> checked=service.preview(10L,2026,10,rows,1L);assertEquals("",checked.get(0).getError());assertTrue(checked.get(1).getError().contains("重复"));assertEquals(8,checked.get(2).getRow());assertEquals("Sheet2",checked.get(2).getSheet());assertTrue(checked.get(2).getError().contains("预算"));assertTrue(checked.get(3).getError().contains("期间"));assertTrue(checked.get(4).getError().contains("核实依据"));assertEquals(0,plans.selectCount());}
 @Test void invalidImportsRollbackAllRowsAndValidRetriesRemainIdempotent(){List<HrmPayrollFinancialImportRowVO> bad=Arrays.asList(fileRow("ENTITY","ALL"),fileRow("DEPARTMENT","11").setBudget("-1"));assertServiceException(()->service.importRows(10L,2026,10,bad,"file-1","UPLOAD:hash",1L),PAYROLL_FINANCIAL_INVALID);assertEquals(0,plans.selectCount());List<HrmPayrollFinancialImportRowVO> good=Arrays.asList(fileRow("ENTITY","ALL"),fileRow("DEPARTMENT","11"));List<Long> ids=service.importRows(10L,2026,10,good,"file-2","UPLOAD:hash",1L);service.review(ids.get(0),true,"独立批准",2L);assertEquals(ids,service.importRows(10L,2026,10,good,"file-2","UPLOAD:hash",1L));assertEquals(2,plans.selectCount());assertServiceException(()->service.importRows(10L,2026,10,good,"new-file","UPLOAD:hash",1L),PAYROLL_VERSION_CONFLICT);}
 @Test void unknownMetricsAndMismatchedPeriodCannotBeCreated(){assertServiceException(()->service.create(req().setBudget(null).setRevenue(null).setHeadcount(null),1L),PAYROLL_FINANCIAL_INVALID);assertServiceException(()->service.create(req().setMonth(13),1L),PAYROLL_FINANCIAL_INVALID);}
 @Test void scopeAndExportFilterAreEnforcedAndExportIsAudited(){approve(req());List<HrmPayrollCostAnalysisRowVO> rows=service.export(10L,2026,10,"ENTITY",1L);assertEquals(1,rows.size());assertTrue(service.history(10L,2026,10,1L).stream().anyMatch(e->"EXPORT".equals(e.getAction())));doThrow(cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception(PAYROLL_ENTITY_INVALID)).when(entities).check(10L,"EXPORT",3L);assertServiceException(()->service.export(10L,2026,10,"ENTITY",3L),PAYROLL_ENTITY_INVALID);doThrow(cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception(PAYROLL_ENTITY_INVALID)).when(scope).check(any(),eq("EXPORT"),eq(4L));assertServiceException(()->service.export(10L,2026,10,"ENTITY",4L),PAYROLL_ENTITY_INVALID);}
 @Test void plannedScopeWithKnownNoActivityHasZeroCostButNoFakePerEmployeeRatio(){approve(req().setDimension("DEPARTMENT").setDimensionKey("99"));HrmPayrollCostAnalysisRowVO row=service.analyze(10L,2026,10,1L).getRows().stream().filter(r->"DEPARTMENT".equals(r.getDimension())&&"99".equals(r.getDimensionKey())).findFirst().get();amount("0",row.getActualCost());assertEquals(0,row.getEmployees());assertNull(row.getCostPerEmployee());assertTrue(row.getSourceStatus().contains("无实际计薪人员"));}
 @Test void establishmentUsesWholePeopleAndTemplatesKeepThatUnit(){
  assertServiceException(()->service.create(req().setHeadcount(new BigDecimal("3.5")),1L),PAYROLL_FINANCIAL_INVALID);
  approve(req().setHeadcount(new BigDecimal("3.0000")));assertEquals("3",service.template(10L,2026,10,1L).get(0).getHeadcount());
 }
}
