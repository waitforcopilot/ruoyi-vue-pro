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
import cn.iocoder.yudao.module.hrm.dal.dataobject.salary.monthrecord.HrmSalaryMonthRecordDO;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.employee.info.HrmEmployeeMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.salary.monthrecord.HrmSalaryMonthRecordMapper;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.notify.NotifyMessageSendApi;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
@Import({HrmPayrollInputIssueService.class,org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration.class})
class HrmPayrollInputIssueServiceTest extends BaseDbUnitTest {
 @Resource HrmPayrollInputIssueService service;
 @Resource HrmPayrollInputIssueMapper cases;
 @Resource HrmPayrollInputIssueEventMapper events;
 @Resource HrmSalaryMonthRecordMapper batches;
 @Resource HrmEmployeeMapper employees;
 @MockBean HrmPayrollScopeService scope;
 @MockBean HrmPayrollEntityService entities;
 @MockBean HrmPayrollReadinessService readiness;
 @MockBean HrmPayrollInputService inputService;
 @MockBean AdminUserApi users;
 @MockBean NotifyMessageSendApi notifications;
 Long batchId,employeeId;AtomicBoolean ready=new AtomicBoolean();
 LocalDateTime now(){return LocalDateTime.now(ZoneId.of("Asia/Shanghai"));}
 @BeforeEach void setup(){
  HrmSalaryMonthRecordDO b=new HrmSalaryMonthRecordDO().setEntityId(10L).setYear(2026).setMonth(10).setBatchKey("REGULAR").setStatus(11).setStartTime(LocalDate.of(2026,10,1).atStartOfDay()).setEndTime(LocalDate.of(2026,10,31).atStartOfDay());batches.insert(b);batchId=b.getId();
  HrmEmployeeDO employee=new HrmEmployeeDO().setName("缺口员工").setJobNumber("ISSUE-1").setUserId(99L).setEntryStatus(1);employees.insert(employee);employeeId=employee.getId();
  when(entities.allowed(any(),any(),any())).thenReturn(true);
  when(readiness.inspect(eq(batchId),eq(employeeId),anyString(),any())).thenAnswer(call->new HrmPayrollReadinessService.Signal().setBatchId(batchId).setEmployeeId(employeeId).setKind(call.getArgument(2)).setReady(ready.get()).setStatus(ready.get()?"READY":"MISSING").setProofHash(ready.get()?"verified-source":"missing-source"));
  when(notifications.sendSingleMessageToAdmin(any())).thenReturn(100L);
 }
 HrmPayrollInputIssueReqVO req(){return new HrmPayrollInputIssueReqVO().setBatchId(batchId).setEmployeeId(employeeId).setKind("ATTENDANCE").setOwnerId(2L).setReviewerId(3L).setDueTime(now().plusDays(2)).setReason("核实真实数据缺口").setRequestKey(UUID.randomUUID().toString());}
 Long create(){return service.create(req(),1L);}
 @Test void realMissingSourceBlocksUntilIndependentVerifiedClosure(){Long id=create();assertServiceException(()->service.assertResolved(batchId),PAYROLL_INPUT_ISSUE_INVALID);service.submit(id,"补正资料编号",2L);assertServiceException(()->service.review(id,true,"仅有文字未修复",3L),PAYROLL_INPUT_ISSUE_INVALID);assertEquals("SUBMITTED",cases.selectById(id).getStatus());ready.set(true);service.review(id,true,"独立检查来源已核实",3L);service.assertResolved(batchId);assertEquals("CLOSED",cases.selectById(id).getStatus());assertTrue(cases.selectById(id).getClosedSnapshot().contains("verified-source"));assertEquals(3,events.selectCount());}
 @Test void readyInputsCannotCreatePhantomBlockingCases(){ready.set(true);assertServiceException(this::create,PAYROLL_INPUT_ISSUE_INVALID);assertEquals(0,cases.selectCount());}
 @Test void reviewerMustDifferFromSubmitterOwnerAndSubject(){assertServiceException(()->service.create(req().setReviewerId(1L),1L),PAYROLL_SELF_APPROVAL);assertServiceException(()->service.create(req().setReviewerId(2L),1L),PAYROLL_SELF_APPROVAL);assertServiceException(()->service.create(req().setReviewerId(99L),1L),PAYROLL_SELF_APPROVAL);}
 @Test void employeeCanSupplyOwnProofWithoutSubjectWidePrivilegesAndOthersCannot(){Long id=create();when(entities.allowed(eq(10L),anyString(),eq(99L))).thenReturn(false);assertEquals(1,service.mine(99L).size());assertEquals("ASSIGNED_PROOF",service.mine(99L).get(0).getSourceMode());service.submit(id,"员工本人提供凭据",99L);assertEquals(99L,cases.selectById(id).getProofBy());assertTrue(service.mine(77L).isEmpty());assertServiceException(()->service.history(id,77L,true),PAYROLL_INPUT_ISSUE_INVALID);}
 @Test void assignmentChangesInvalidateOldOwnerAccessAndResetPendingProof(){Long id=create();service.submit(id,"原责任人材料",2L);service.reassign(new HrmPayrollInputIssueAssignReqVO().setId(id).setOwnerId(4L).setReviewerId(3L).setDueTime(now().plusDays(3)).setReason("责任人交接"),1L);assertEquals("OPEN",cases.selectById(id).getStatus());assertNull(cases.selectById(id).getEvidence());assertNull(cases.selectById(id).getProofBy());assertServiceException(()->service.submit(id,"旧责任人",2L),PAYROLL_INPUT_ISSUE_INVALID);service.submit(id,"新责任人材料",4L);}
 @Test void revokedOwnerGrantCannotUsePortalAssignmentOrReceiveProof(){Long id=create();when(entities.allowed(10L,"MANAGE",2L)).thenReturn(false);assertServiceException(()->service.submit(id,"已撤销授权",2L),PAYROLL_INPUT_ISSUE_INVALID);assertTrue(service.mine(2L).isEmpty());}
 @Test void rejectAndReopenKeepHistoryAndDoNotAutoClose(){Long id=create();service.submit(id,"资料",2L);service.review(id,false,"资料未完整",3L);assertEquals("OPEN",cases.selectById(id).getStatus());service.submit(id,"补全资料",2L);ready.set(true);service.review(id,true,"核实通过",3L);assertServiceException(()->service.reopen(id,"无真实变化",3L),PAYROLL_INPUT_ISSUE_INVALID);ready.set(false);service.reopen(id,"来源后来失效",3L);assertEquals("OPEN",cases.selectById(id).getStatus());assertNull(cases.selectById(id).getClosedTime());assertNull(cases.selectById(id).getClosedSnapshot());assertServiceException(()->service.assertResolved(batchId),PAYROLL_INPUT_ISSUE_INVALID);assertEquals(6,events.selectCount());}
 @Test void retriesCannotChangeAssignmentsOrCreateAnotherActiveSource(){HrmPayrollInputIssueReqVO req=req();Long id=service.create(req,1L);assertEquals(id,service.create(req,1L));assertServiceException(()->service.create(req().setOwnerId(4L),1L),PAYROLL_INPUT_ISSUE_INVALID);req.setOwnerId(4L);assertServiceException(()->service.create(req,1L),PAYROLL_INPUT_ISSUE_INVALID);}
 @Test void sameRequestIsSerializedAcrossConcurrentTransactions() throws Exception {HrmPayrollInputIssueReqVO req=req();ExecutorService executor=Executors.newFixedThreadPool(2);try{Future<Long> a=executor.submit(()->service.create(req,1L)),b=executor.submit(()->service.create(req,1L));assertEquals(a.get(10,TimeUnit.SECONDS),b.get(10,TimeUnit.SECONDS));assertEquals(1,cases.selectCount());}finally{executor.shutdownNow();}}
 @Test void frozenBatchesPreserveClosedEvidenceAndRejectNewOrReopenedCases(){Long id=create();service.submit(id,"已补资料",2L);ready.set(true);service.review(id,true,"来源一致",3L);batches.updateById(new HrmSalaryMonthRecordDO().setId(batchId).setStatus(15));assertServiceException(()->service.create(req().setKind("OVERTIME"),1L),PAYROLL_INPUT_ISSUE_INVALID);assertEquals("FROZEN_HISTORY",service.list(batchId,1L).get(0).getSourceMode());assertServiceException(()->service.reopen(id,"冻结后",3L),PAYROLL_INPUT_ISSUE_INVALID);}
 @Test void remindersAreDailyDeduplicatedAndContainNoWageOrEvidence(){Long id=create();cases.updateById(new HrmPayrollInputIssueDO().setId(id).setDueTime(now().minusDays(1)));assertEquals(2,service.remind(id));assertEquals(0,service.remind(id));org.mockito.ArgumentCaptor<cn.iocoder.yudao.module.system.api.notify.dto.NotifySendSingleToUserReqDTO> captured=org.mockito.ArgumentCaptor.forClass(cn.iocoder.yudao.module.system.api.notify.dto.NotifySendSingleToUserReqDTO.class);verify(notifications,times(2)).sendSingleMessageToAdmin(captured.capture());for(cn.iocoder.yudao.module.system.api.notify.dto.NotifySendSingleToUserReqDTO notification:captured.getAllValues()){assertEquals(new HashSet<>(Arrays.asList("caseId","period","kind","dueTime")),notification.getTemplateParams().keySet());assertEquals("考勤",notification.getTemplateParams().get("kind"));assertFalse(JsonUtils.toJsonString(notification).contains("核实真实数据缺口"));}}
 @Test void onlyFailedRecipientIsRetriedAndClosedCasesStopReminders(){Long id=create();cases.updateById(new HrmPayrollInputIssueDO().setId(id).setDueTime(now().minusDays(1)));when(notifications.sendSingleMessageToAdmin(any())).thenThrow(new IllegalStateException("配置不可用")).thenReturn(101L);assertEquals(1,service.remind(id));assertEquals(1,service.remind(id));assertEquals(0,service.remind(id));verify(notifications,times(3)).sendSingleMessageToAdmin(any());service.submit(id,"已修复",2L);ready.set(true);service.review(id,true,"核实",3L);assertEquals(0,service.remind(id));}
 @Test void resubmissionClearsCurrentOpinionButPreservesReviewHistory() {
  Long id=create();
  service.submit(id,"第一轮材料",2L);
  service.review(id,false,"第一轮退回意见",3L);
  assertEquals("第一轮退回意见",cases.selectById(id).getReviewOpinion());
  service.submit(id,"第二轮材料",2L);
  HrmPayrollInputIssueDO current=cases.selectById(id);
  assertEquals("SUBMITTED",current.getStatus());
  assertEquals("第二轮材料",current.getEvidence());
  assertEquals(2L,current.getProofBy());
  assertNull(current.getReviewOpinion());
  assertNull(service.list(batchId,1L).get(0).getReviewOpinion());
  assertNull(service.mine(99L).get(0).getReviewOpinion());
  List<HrmPayrollInputIssueEventDO> history=service.history(id,3L,false);
  assertEquals(4,history.size());
  assertEquals("REJECT",history.get(2).getAction());
  assertEquals("第一轮退回意见",history.get(2).getReason());
  assertTrue(history.get(2).getSnapshot().contains("第一轮退回意见"));
  assertFalse(history.get(3).getSnapshot().contains("第一轮退回意见"));
  assertServiceException(()->service.review(id,true,"来源仍缺失",3L),PAYROLL_INPUT_ISSUE_INVALID);
  service.review(id,false,"第二轮退回意见",3L);
  assertEquals("第二轮退回意见",cases.selectById(id).getReviewOpinion());
  assertEquals(5,service.history(id,3L,false).size());
 }
 @Test void invalidResubmissionDoesNotEraseReviewOpinion() {
  Long id=create();service.submit(id,"第一轮材料",2L);service.review(id,false,"仍需补正",3L);
  assertServiceException(()->service.submit(id,"",2L),PAYROLL_INPUT_ISSUE_INVALID);
  assertServiceException(()->service.submit(id,"未授权材料",77L),PAYROLL_INPUT_ISSUE_INVALID);
  assertEquals("仍需补正",cases.selectById(id).getReviewOpinion());
  assertEquals("OPEN",cases.selectById(id).getStatus());assertEquals(3,events.selectCount());
 }

}
