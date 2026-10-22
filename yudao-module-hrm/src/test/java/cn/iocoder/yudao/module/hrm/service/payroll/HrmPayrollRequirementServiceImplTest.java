package cn.iocoder.yudao.module.hrm.service.payroll;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollRequirementSaveReqVO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.HrmPayrollRequirementDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.HrmPayrollRequirementProjectDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.HrmPayrollRequirementProjectGrantDO;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.HrmPayrollRequirementMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import javax.annotation.Resource;
import static org.junit.jupiter.api.Assertions.*;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;
@Import({HrmPayrollWorkbenchService.class,HrmPayrollRequirementAccessService.class,HrmPayrollRequirementServiceImpl.class,HrmPayrollRequirementProjectService.class})
class HrmPayrollRequirementServiceImplTest extends BaseDbUnitTest {
 // These tests isolate the pre-S5 contract. The real governance gate is covered by HrmPayrollGovernanceServiceTest and runtime regression.
 @org.springframework.boot.test.mock.mockito.MockBean private cn.iocoder.yudao.module.hrm.service.payroll.HrmPayrollGovernanceStore stage5Store;
 @org.springframework.boot.test.mock.mockito.MockBean private cn.iocoder.yudao.module.hrm.service.payroll.HrmPayrollGovernanceRootService stage5Roots;
 @org.springframework.boot.test.mock.mockito.MockBean private cn.iocoder.yudao.module.hrm.service.payroll.HrmPayrollGovernanceDecisionService stage5Decisions;
 @org.junit.jupiter.api.BeforeEach void stage5ContractIsolation(){
  org.mockito.Mockito.when(stage5Decisions.currentSemantic(org.mockito.ArgumentMatchers.any())).thenReturn(true);
  org.mockito.Mockito.when(stage5Roots.validRoot(org.mockito.ArgumentMatchers.anyLong(),org.mockito.ArgumentMatchers.any())).thenReturn(true);
 }

 @Resource private HrmPayrollRequirementService service;
 @Resource private HrmPayrollRequirementProjectService projectService;
 @Resource private HrmPayrollRequirementMapper requirements;
 @Resource private cn.iocoder.yudao.module.hrm.dal.mysql.payroll.HrmPayrollRequirementProjectGrantMapper grants;
 @Resource private cn.iocoder.yudao.module.hrm.dal.mysql.payroll.HrmPayrollRequirementProjectMapper projects;
 @org.springframework.boot.test.mock.mockito.MockBean private cn.iocoder.yudao.module.system.api.user.AdminUserApi users;
 @org.springframework.boot.test.mock.mockito.MockBean private cn.iocoder.yudao.module.system.api.permission.PermissionApi permissions;
 @org.junit.jupiter.api.BeforeEach void users() {
  cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.setTenantId(1L);
  org.mockito.Mockito.when(permissions.hasAnyPermissions(org.mockito.ArgumentMatchers.anyLong(),org.mockito.ArgumentMatchers.anyString())).thenReturn(true);
  for(long id:new long[]{1,2,3}) org.mockito.Mockito.when(users.getUser(id)).thenReturn(new cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO().setId(id).setStatus(0));
  cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.setLoginUser(new cn.iocoder.yudao.framework.security.core.LoginUser().setId(1L).setUserType(2),new org.springframework.mock.web.MockHttpServletRequest());
  projects.insert(new cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.HrmPayrollRequirementProjectDO().setId(1L).setProjectKey("LEGACY").setName("TEST explicit legacy fixture").setMetadataVersion(1).setProjectScopeVersion(1).setScopeJson("{\"entities\":[101],\"organizations\":[201],\"workerTypes\":[\"TEST\"],\"scenarios\":[\"TEST\"],\"from\":\"2026-10-01\",\"to\":\"2026-10-31\",\"explanation\":\"TEST legacy contract scope\"}"));
  for(long id:new long[]{1,2,3}){cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.HrmPayrollRequirementProjectGrantDO g=new cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.HrmPayrollRequirementProjectGrantDO();g.setProjectId(1L);g.setUserId(id);g.setCeilingJson("{\"entities\":[101],\"organizations\":[201],\"workerTypes\":[\"TEST\"],\"scenarios\":[\"TEST\"],\"from\":\"2026-10-01\",\"to\":\"2026-10-31\",\"explanation\":\"TEST legacy contract scope\"}");g.setActions("READ,EDIT,ASSIGN,CONFIRM,EXPORT");g.setEvidence("TEST legacy explicit access, no role bypass");g.setValidFrom(java.time.LocalDate.of(2020,1,1));g.setValidTo(java.time.LocalDate.of(2099,12,31));grants.insert(g);}
 }
 @org.junit.jupiter.api.AfterEach void clearIdentity(){cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.clear();org.springframework.security.core.context.SecurityContextHolder.clearContext();}
 private HrmPayrollRequirementSaveReqVO request() {
  HrmPayrollRequirementSaveReqVO r=new HrmPayrollRequirementSaveReqVO();r.setCode("PLAN-06");r.setModule("PLAN");r.setDescription("测试薪资方案规则");r.setStatus("PENDING");r.setReadiness("MISSING");r.setPriority(2);r.setProjectScopeVersion(1);r.setBusinessContextJson("{\"role\": \"TEST legacy compatibility discussion\", \"scenario\": \"TEST legacy compatibility discussion\", \"goal\": \"TEST legacy compatibility discussion\", \"expected\": \"TEST legacy compatibility discussion\", \"normalCase\": \"TEST legacy compatibility discussion\", \"exceptionCase\": \"TEST legacy compatibility discussion\", \"calculation\": \"TEST legacy compatibility discussion\", \"unit\": \"TEST legacy compatibility discussion\", \"precision\": \"TEST legacy compatibility discussion\", \"validity\": \"TEST legacy compatibility discussion\", \"ruleException\": \"TEST legacy compatibility discussion\"}");r.setSourceApplicability("NOT_APPLICABLE");r.setApplicabilityReason("TEST legacy discussion does not consume business data");r.setRuleVersion("TEST RULE V1");return r;
 }
 private HrmPayrollRequirementSaveReqVO ready() {
  HrmPayrollRequirementSaveReqVO r=request();r.setReviewerId(2L);r.setSourceOwnerId(3L);r.setEvidence("评审记录");r.setAcceptance("可验证场景");r.setSourceSystem("HRM");r.setFieldMapping("来源字段 → 本项目字段");return r;
 }
 private HrmPayrollRequirementDO row(Long id) {return service.list(null).stream().filter(x->x.getId().equals(id)).findFirst().get();}
 private HrmPayrollRequirementSaveReqVO createConfirmed() {
  HrmPayrollRequirementSaveReqVO r=ready();r.setId(service.save(r,1L));service.confirm(r.getId(),1,2L);r.setVersion(2);r.setStatus("CONFIRMED");return r;
 }
 @Test void initializeIsIdempotentAndDoesNotConfirmRequirements() {
  assertEquals(43,service.initialize(1L));assertEquals(0,service.initialize(1L));assertEquals(43,service.list(null).size());
  for(HrmPayrollRequirementDO r:service.list(null)){assertEquals("PENDING",r.getStatus());assertNull(r.getConfirmedBy());assertEquals(1,service.history(r.getId()).size());}
 }
 @Test void savingCannotGenerateSignatureEvenWithCompleteConfirmationFields() {
  HrmPayrollRequirementSaveReqVO r=ready();r.setStatus("CONFIRMED");
  assertServiceException(()->service.save(r,2L),PAYROLL_REQUIREMENT_CONFIRM_ACTION_REQUIRED);assertTrue(service.list(null).isEmpty());assertEquals(1L,service.legacyProject().getId());
  r.setStatus("PENDING");r.setId(service.save(r,1L));r.setVersion(1);r.setStatus("CONFIRMED");
  assertServiceException(()->service.save(r,2L),PAYROLL_REQUIREMENT_CONFIRM_ACTION_REQUIRED);assertNull(row(r.getId()).getConfirmedBy());assertEquals(1,service.history(r.getId()).size());
 }
 @Test void confirmationUsesPersistedVersionAndDoesNotChangeReadiness() {
  HrmPayrollRequirementSaveReqVO r=ready();Long id=service.save(r,1L);assertEquals("EXEMPT",row(id).getReadiness());r.setDescription("客户端未保存修改");
  service.confirm(id,1,2L);assertEquals("测试薪资方案规则",row(id).getDescription());assertEquals("CONFIRMED",row(id).getStatus());assertEquals("EXEMPT",row(id).getReadiness());assertEquals(2L,row(id).getConfirmedBy());assertEquals(2,service.history(id).size());
 }
 @Test void incompleteConfirmationIsRejectedWithoutNewHistory() {
  Long id=service.save(request(),1L);
  cn.iocoder.yudao.framework.common.exception.ServiceException error=assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->service.confirm(id,1,2L));
  assertEquals(PAYROLL_REVIEW_INCOMPLETE.getCode(),error.getCode());assertTrue(error.getMessage().contains("依据或异议说明"));assertTrue(error.getMessage().contains("评审负责人"));
  assertEquals(1,service.history(id).size());assertEquals("PENDING",row(id).getStatus());
 }
 private Long structuredRequirement(){
  HrmPayrollRequirementProjectDO project=new HrmPayrollRequirementProjectDO().setId(2L).setProjectKey("TEST_SCOPE_CONFIRM").setName("TEST confirmation scope ordering").setMetadataVersion(1).setProjectScopeVersion(1).setScopeJson(projects.selectById(1L).getScopeJson()).setCustomSequence(0L);projects.insert(project);
  for(long actor:new long[]{1,2}){
   HrmPayrollRequirementProjectGrantDO grant=new HrmPayrollRequirementProjectGrantDO();grant.setProjectId(project.getId());grant.setUserId(actor);grant.setFullProject(true);grant.setActions("READ,EDIT,ASSIGN,CONFIRM,SCOPE");grant.setCeilingJson(project.getScopeJson());grant.setEvidence("TEST explicit scope-confirm contract authority");grant.setValidFrom(java.time.LocalDate.of(2020,1,1));grant.setValidTo(java.time.LocalDate.of(2099,12,31));grants.insert(grant);
  }
  HrmPayrollRequirementSaveReqVO request=ready();request.setProjectId(project.getId());request.setScopeMode("SUBSET");request.setScopeJson(project.getScopeJson());return service.save(request,1L);
 }
 private void missingGovernance(){
  org.mockito.Mockito.when(stage5Decisions.forRequirement(org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.anyLong())).thenThrow(new cn.iocoder.yudao.framework.common.exception.ServiceException(PAYROLL_REVIEW_INCOMPLETE.getCode(),PAYROLL_REVIEW_INCOMPLETE.getMsg()));
  org.mockito.Mockito.clearInvocations(stage5Decisions);
 }
 private void assertScopeRejectedWithoutSignature(Long id){
  HrmPayrollRequirementDO before=requirements.selectById(id);int historyCount=service.history(id).size();
  cn.iocoder.yudao.framework.common.exception.ServiceException error=assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->service.confirm(id,before.getVersion(),2L));
  assertEquals(PAYROLL_PROJECT_SCOPE_STALE.getCode(),error.getCode());assertEquals(PAYROLL_PROJECT_SCOPE_STALE.getMsg(),error.getMessage());
  assertTrue(error.getMessage().matches(".*范围.*变化.*"));
  HrmPayrollRequirementDO after=requirements.selectById(id);assertEquals(before.getVersion(),after.getVersion());assertEquals(before.getStatus(),after.getStatus());assertNull(after.getConfirmedBy());assertNull(after.getConfirmedAt());assertNull(after.getConfirmedTupleJson());assertEquals(historyCount,service.history(id).size());
  org.mockito.Mockito.verifyNoInteractions(stage5Decisions);
 }
 @Test void scopeMismatchIsReportedBeforeMissingGovernanceWithoutSigning(){
  Long id=structuredRequirement();HrmPayrollRequirementDO req=requirements.selectById(id);
  projectService.scope(req.getProjectId(),1,req.getScopeJson().replace("2026-10-01","2026-10-16"),"TEST narrow period while preserving explicit requirement subset",1L);
  assertEquals("SCOPE_MISMATCH",requirements.selectById(id).getScopeState());assertEquals(2,requirements.selectById(id).getProjectScopeVersion());
  missingGovernance();assertScopeRejectedWithoutSignature(id);
 }
 @Test void undefinedProjectScopeIsReportedBeforeMissingGovernanceWithoutSigning(){
  Long id=structuredRequirement();Long project=requirements.selectById(id).getProjectId();
  projects.update(null,new LambdaUpdateWrapper<HrmPayrollRequirementProjectDO>().eq(HrmPayrollRequirementProjectDO::getId,project).set(HrmPayrollRequirementProjectDO::getProjectScopeVersion,null).set(HrmPayrollRequirementProjectDO::getScopeJson,null));
  missingGovernance();assertScopeRejectedWithoutSignature(id);
 }
 @Test void undefinedRequirementScopeAndStaleBoundVersionAreReportedBeforeMissingGovernance(){
  Long id=structuredRequirement();HrmPayrollRequirementDO req=requirements.selectById(id);req.setScopeState("UNDEFINED");requirements.updateById(req);
  missingGovernance();assertScopeRejectedWithoutSignature(id);
  req.setScopeState("MATCH");requirements.updateById(req);HrmPayrollRequirementProjectDO project=projects.selectById(req.getProjectId());project.setProjectScopeVersion(2);projects.updateById(project);
  assertScopeRejectedWithoutSignature(id);
 }
 @Test void inaccessibleRequirementDoesNotRevealScopeFailureOrReachGovernance(){
  Long id=structuredRequirement();HrmPayrollRequirementDO req=requirements.selectById(id);req.setScopeState("SCOPE_MISMATCH");requirements.updateById(req);
  grants.delete(new cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX<HrmPayrollRequirementProjectGrantDO>().eq(HrmPayrollRequirementProjectGrantDO::getProjectId,req.getProjectId()).eq(HrmPayrollRequirementProjectGrantDO::getUserId,2L));
  missingGovernance();assertServiceException(()->service.confirm(id,req.getVersion(),2L),PAYROLL_PROJECT_DENIED);
  org.mockito.Mockito.verifyNoInteractions(stage5Decisions);assertNull(requirements.selectById(id).getConfirmedBy());
 }
 @Test void matchingScopeStillRequiresGovernanceAndCanConfirmWhenSatisfied(){
  Long id=structuredRequirement();HrmPayrollRequirementDO before=requirements.selectById(id);int historyCount=service.history(id).size();missingGovernance();
  assertServiceException(()->service.confirm(id,before.getVersion(),2L),PAYROLL_REVIEW_INCOMPLETE);
  org.mockito.Mockito.verify(stage5Decisions).forRequirement(org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.eq(2L));
  assertEquals(before.getVersion(),requirements.selectById(id).getVersion());assertNull(requirements.selectById(id).getConfirmedBy());assertEquals(historyCount,service.history(id).size());
  // This suite isolates governance: the successful response represents its satisfied contract.
  org.mockito.Mockito.doReturn(java.util.Collections.singletonMap("decisions",java.util.Collections.singletonList("TEST effective decision"))).when(stage5Decisions).forRequirement(org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.eq(2L));
  service.confirm(id,before.getVersion(),2L);HrmPayrollRequirementDO confirmed=requirements.selectById(id);
  assertEquals("CONFIRMED",confirmed.getStatus());assertEquals(2L,confirmed.getConfirmedBy());assertNotNull(confirmed.getConfirmedAt());assertEquals(1,confirmed.getConfirmedScopeVersion());assertNotNull(confirmed.getConfirmedTupleJson());assertEquals(historyCount+1,service.history(id).size());
  org.mockito.Mockito.verify(stage5Decisions,org.mockito.Mockito.atLeast(2)).forRequirement(org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.eq(2L));
 }
 @Test void staleUpdateIsRejected() {
  HrmPayrollRequirementSaveReqVO r=request();r.setId(service.save(r,1L));r.setVersion(1);service.save(r,1L);assertServiceException(()->service.save(r,2L),PAYROLL_VERSION_CONFLICT);
 }
 @Test void staleConfirmationCannotSignNewContent() {
  HrmPayrollRequirementSaveReqVO r=ready();r.setId(service.save(r,1L));r.setVersion(1);r.setDescription("新版本");service.save(r,1L);
  assertServiceException(()->service.confirm(r.getId(),1,2L),PAYROLL_VERSION_CONFLICT);assertEquals("PENDING",row(r.getId()).getStatus());assertNull(row(r.getId()).getConfirmedAt());assertEquals(2,service.history(r.getId()).size());
 }
 @Test void duplicateCodeIsRejected() {service.save(request(),1L);assertServiceException(()->service.save(request(),2L),PAYROLL_REQUIREMENT_DUPLICATE);}
 @Test void reopeningRemovesConfirmationButRetainsHistory() {
  HrmPayrollRequirementSaveReqVO r=createConfirmed();r.setStatus("REVIEWING");service.save(r,3L);assertNull(row(r.getId()).getConfirmedBy());assertEquals(3,service.history(r.getId()).size());
 }
 @Test void confirmationRejectsEmptyMappingAndBareExemption() {
  int code=100;for(String mapping:new String[]{null," ","N/A","N/A\n ","不适用："}){
   HrmPayrollRequirementSaveReqVO r=ready();r.setCode("PLAN-"+(code++));r.setFieldMapping(mapping);Long id=service.save(r,1L);
   assertServiceException(()->service.confirm(id,1,2L),PAYROLL_REQUIREMENT_CONFIRMATION_INVALID);assertNull(row(id).getConfirmedBy());assertEquals(1,service.history(id).size());
  }
 }
 @Test void explicitExemptionWithReasonIsRetained() {
  HrmPayrollRequirementSaveReqVO r=ready();r.setFieldMapping("不适用：本需求为评审操作规范，不涉及外部业务数据字段");Long id=service.save(r,1L);service.confirm(id,1,2L);assertEquals(r.getFieldMapping(),row(id).getFieldMapping());
 }
 @Test void nonexistentOrDisabledOwnersCannotConfirm() {
  HrmPayrollRequirementSaveReqVO r=ready();r.setSourceOwnerId(999L);r.setId(service.save(r,1L));assertServiceException(()->service.confirm(r.getId(),1,2L),PAYROLL_REQUIREMENT_PERSON_INVALID);
  r.setVersion(1);r.setSourceOwnerId(3L);service.save(r,1L);org.mockito.Mockito.when(users.getUser(3L)).thenReturn(new cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO().setId(3L).setStatus(1));
  assertServiceException(()->service.confirm(r.getId(),2,2L),PAYROLL_REQUIREMENT_PERSON_INVALID);
 }
 @Test void nonexistentReviewerAndForgedConfirmationAreRejected() {
  HrmPayrollRequirementSaveReqVO r=ready();r.setReviewerId(999L);assertServiceException(()->service.save(r,1L),PAYROLL_REQUIREMENT_PERSON_INVALID);assertTrue(service.list(null).isEmpty());
  r.setReviewerId(2L);r.setId(service.save(r,1L));assertServiceException(()->service.confirm(r.getId(),1,3L),PAYROLL_REQUIREMENT_CONFIRMATION_INVALID);
 }
 @Test void legacyProjectIsServerOwnedAndStableAcrossEdits() {
  projects.update(null,new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.HrmPayrollRequirementProjectDO>().eq(cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.HrmPayrollRequirementProjectDO::getId,1L).set(cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.HrmPayrollRequirementProjectDO::getProjectScopeVersion,null).set(cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.HrmPayrollRequirementProjectDO::getScopeJson,null));
  assertEquals(1L,service.legacyProject().getId());HrmPayrollRequirementSaveReqVO r=request();r.setId(service.save(r,1L));Long project=service.legacyProject().getId();assertNull(service.legacyProject().getProjectScopeVersion());assertEquals(1,service.legacyProject().getMetadataVersion());
  r.setVersion(1);r.setDescription("只修改说明，保留归属");service.save(r,1L);assertEquals(project,row(r.getId()).getProjectId());assertTrue(service.history(r.getId()).get(0).getSnapshot().contains("\"projectId\":"+project));
 }
 @Test void multipleRequirementsReuseLegacyProject() {
  service.save(request(),1L);Long project=service.legacyProject().getId();HrmPayrollRequirementSaveReqVO second=request();second.setCode("PLAN-07");service.save(second,1L);
  for(HrmPayrollRequirementDO r:service.list(null))assertEquals(project,r.getProjectId());
 }
 @Test void confirmedContentChangeRequiresSeparateReconfirmationAndKeepsOldSnapshot() {
  HrmPayrollRequirementSaveReqVO r=createConfirmed();r.setDescription("已确认后修改业务行为");service.save(r,2L);assertEquals("REVIEWING",row(r.getId()).getStatus());assertNull(row(r.getId()).getConfirmedBy());assertNull(row(r.getId()).getConfirmedAt());assertEquals("CONFIRMED",r.getStatus());
  assertTrue(service.history(r.getId()).get(1).getSnapshot().contains("\"confirmedBy\":2"));r.setVersion(3);
  assertServiceException(()->service.save(r,2L),PAYROLL_REQUIREMENT_CONFIRM_ACTION_REQUIRED);service.confirm(r.getId(),3,2L);assertEquals("CONFIRMED",row(r.getId()).getStatus());assertEquals(4,service.history(r.getId()).size());
 }
 @Test void unchangedConfirmedSaveDoesNotRenewSignatureTime() {
  HrmPayrollRequirementSaveReqVO r=createConfirmed();java.time.LocalDateTime signed=row(r.getId()).getConfirmedAt();service.save(r,2L);assertEquals(signed,row(r.getId()).getConfirmedAt());assertEquals(2L,row(r.getId()).getConfirmedBy());
 }
 @Test void eachExistingReviewContractFieldInvalidatesConfirmation() {
  java.util.List<java.util.function.Consumer<HrmPayrollRequirementSaveReqVO>> edits=java.util.Arrays.asList(x->x.setCode("PLAN-07"),x->x.setModule("CALC"),x->x.setDescription("新行为"),x->x.setFieldMapping("新映射"),x->x.setSourceSystem("新来源"),x->x.setSourceOwnerId(2L),x->x.setReviewerId(3L),x->x.setAcceptance("新验收"),x->x.setEvidence("新依据"),x->x.setPriority(1));
  int code=200;for(java.util.function.Consumer<HrmPayrollRequirementSaveReqVO> edit:edits){HrmPayrollRequirementSaveReqVO r=ready();r.setCode("PLAN-"+(code++));r.setId(service.save(r,1L));service.confirm(r.getId(),1,2L);r.setVersion(2);r.setStatus("CONFIRMED");edit.accept(r);service.save(r,1L);assertEquals("REVIEWING",row(r.getId()).getStatus());assertNull(row(r.getId()).getConfirmedBy());}
 }
 @Test void staleRevisionCannotClearCurrentSignature() {
  HrmPayrollRequirementSaveReqVO r=createConfirmed();r.setVersion(1);r.setDescription("过期修改");assertServiceException(()->service.save(r,2L),PAYROLL_VERSION_CONFLICT);assertEquals("CONFIRMED",row(r.getId()).getStatus());assertEquals(2,service.history(r.getId()).size());
 }
 @Test void deferredOrAlreadyConfirmedCannotBeConfirmedAgain() {
  HrmPayrollRequirementSaveReqVO r=createConfirmed();assertServiceException(()->service.confirm(r.getId(),2,2L),PAYROLL_REQUIREMENT_CONFIRM_ACTION_REQUIRED);
  r.setStatus("DEFERRED");service.save(r,2L);assertServiceException(()->service.confirm(r.getId(),3,2L),PAYROLL_REQUIREMENT_CONFIRM_ACTION_REQUIRED);
 }
 @Test void invalidConfirmationArgumentsAndMissingRecordAreRejected() {
  assertServiceException(()->service.confirm(null,1,2L),PAYROLL_REQUIREMENT_CONFIRM_ACTION_REQUIRED);
  assertServiceException(()->service.confirm(1L,null,2L),PAYROLL_REQUIREMENT_CONFIRM_ACTION_REQUIRED);
  assertServiceException(()->service.confirm(1L,0,2L),PAYROLL_REQUIREMENT_CONFIRM_ACTION_REQUIRED);
  assertServiceException(()->service.confirm(999L,1,2L),PAYROLL_REQUIREMENT_NOT_EXISTS);
 }
 @Test void editorCannotAssignOrClearReviewerAndCannotCreateAssignedRequirement() {
  HrmPayrollRequirementSaveReqVO r=ready();r.setId(service.save(r,1L));r.setVersion(1);
  org.mockito.Mockito.when(permissions.hasAnyPermissions(1L,"hrm:payroll:requirement:assign")).thenReturn(false);
  for(Long reviewer:new Long[]{3L,null}) {r.setReviewerId(reviewer);assertServiceException(()->service.save(r,1L),PAYROLL_REQUIREMENT_ASSIGN_FORBIDDEN);}
  assertEquals(1,row(r.getId()).getVersion());assertEquals(2L,row(r.getId()).getReviewerId());assertEquals(1,service.history(r.getId()).size());
  HrmPayrollRequirementSaveReqVO created=ready();created.setCode("PLAN-99");assertServiceException(()->service.save(created,1L),PAYROLL_REQUIREMENT_ASSIGN_FORBIDDEN);assertEquals(1,service.list(null).size());
  r.setReviewerId(2L);r.setDescription("仅改内容");service.save(r,1L);assertEquals(2,row(r.getId()).getVersion());
 }
 @Test void assignPermissionCannotSelfAssign() {
  HrmPayrollRequirementSaveReqVO r=ready();assertServiceException(()->service.save(r,2L),PAYROLL_REQUIREMENT_ASSIGN_FORBIDDEN);assertTrue(service.list(null).isEmpty());
  r.setId(service.save(r,1L));r.setVersion(1);r.setReviewerId(1L);assertServiceException(()->service.save(r,1L),PAYROLL_REQUIREMENT_ASSIGN_FORBIDDEN);assertEquals(1,service.history(r.getId()).size());
 }
 @Test void assigneeNeedsBothReadAndConfirmAndMustBeActive() {
  for(String permission:new String[]{"query","confirm"}) {
   HrmPayrollRequirementSaveReqVO r=ready();
   org.mockito.Mockito.when(permissions.hasAnyPermissions(2L,"hrm:payroll:requirement:"+permission)).thenReturn(false);
   assertServiceException(()->service.save(r,1L),PAYROLL_REQUIREMENT_ASSIGNEE_FORBIDDEN);assertTrue(service.list(null).isEmpty());
   org.mockito.Mockito.when(permissions.hasAnyPermissions(2L,"hrm:payroll:requirement:"+permission)).thenReturn(true);
  }
  org.mockito.Mockito.when(users.getUser(2L)).thenReturn(new cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO().setId(2L).setStatus(1));
  assertServiceException(()->service.save(ready(),1L),PAYROLL_REQUIREMENT_PERSON_INVALID);assertTrue(service.list(null).isEmpty());
 }
 @Test void reassignmentInvalidatesSignatureAndOldReviewerCannotConfirm() {
  HrmPayrollRequirementSaveReqVO r=createConfirmed();r.setReviewerId(3L);service.save(r,1L);assertEquals("REVIEWING",row(r.getId()).getStatus());assertNull(row(r.getId()).getConfirmedBy());
  assertServiceException(()->service.confirm(r.getId(),3,2L),PAYROLL_REQUIREMENT_CONFIRMATION_INVALID);assertEquals(3,service.history(r.getId()).size());service.confirm(r.getId(),3,3L);assertEquals(3L,row(r.getId()).getConfirmedBy());assertEquals(4,service.history(r.getId()).size());
 }
 @Test void authorizedClearPreservesOldAssignmentInHistory() {
  HrmPayrollRequirementSaveReqVO r=ready();r.setId(service.save(r,1L));r.setVersion(1);r.setReviewerId(null);service.save(r,1L);assertNull(row(r.getId()).getReviewerId());assertTrue(service.history(r.getId()).get(1).getSnapshot().contains("\"reviewerId\":2"));
 }
 @Test void independentAssignmentUsesSavedContentAndNeedsNoContentEditing() {
  HrmPayrollRequirementSaveReqVO r=ready();Long id=service.save(r,1L);
  org.mockito.Mockito.when(permissions.hasAnyPermissions(1L,"hrm:payroll:requirement:update")).thenReturn(false);
  service.assign(id,1,3L,1L);assertEquals(3L,row(id).getReviewerId());assertEquals(r.getDescription(),row(id).getDescription());assertEquals(r.getFieldMapping(),row(id).getFieldMapping());assertEquals(r.getEvidence(),row(id).getEvidence());assertEquals(2,row(id).getVersion());assertEquals(2,service.history(id).size());
 }
 @Test void staleAssignmentCannotOverwriteNewContent() {
  HrmPayrollRequirementSaveReqVO r=ready();r.setId(service.save(r,1L));r.setVersion(1);r.setDescription("较新内容");service.save(r,1L);
  assertServiceException(()->service.assign(r.getId(),1,3L,1L),PAYROLL_VERSION_CONFLICT);assertEquals(2L,row(r.getId()).getReviewerId());assertEquals("较新内容",row(r.getId()).getDescription());assertEquals(2,service.history(r.getId()).size());
 }
 @Test void independentReassignmentReopensConfirmedVersionAndPreservesSignatureHistory() {
  HrmPayrollRequirementSaveReqVO r=createConfirmed();service.assign(r.getId(),2,3L,1L);assertEquals("REVIEWING",row(r.getId()).getStatus());assertNull(row(r.getId()).getConfirmedBy());assertEquals(3L,row(r.getId()).getReviewerId());assertTrue(service.history(r.getId()).get(1).getSnapshot().contains("\"confirmedBy\":2"));
 }
 @Test void independentAssignmentNoOpDoesNotWriteAndStillNeedsPermission() {
  HrmPayrollRequirementSaveReqVO r=ready();Long id=service.save(r,1L);service.assign(id,1,2L,1L);assertEquals(1,service.history(id).size());
  org.mockito.Mockito.when(permissions.hasAnyPermissions(1L,"hrm:payroll:requirement:assign")).thenReturn(false);assertServiceException(()->service.assign(id,1,2L,1L),PAYROLL_REQUIREMENT_ASSIGN_FORBIDDEN);assertEquals(1,service.history(id).size());
 }
 @Test void independentAssignmentCanClearButCannotSelfAssignOrTargetUnqualifiedUser() {
  HrmPayrollRequirementSaveReqVO r=ready();Long id=service.save(r,1L);
  assertServiceException(()->service.assign(id,1,1L,1L),PAYROLL_REQUIREMENT_ASSIGN_FORBIDDEN);
  org.mockito.Mockito.when(permissions.hasAnyPermissions(3L,"hrm:payroll:requirement:confirm")).thenReturn(false);
  assertServiceException(()->service.assign(id,1,3L,1L),PAYROLL_REQUIREMENT_ASSIGNEE_FORBIDDEN);assertEquals(1,service.history(id).size());service.assign(id,1,null,1L);assertNull(row(id).getReviewerId());assertEquals(2,service.history(id).size());
 }


 @Test void legacyEndpointsRequireExplicitProjectAccessEvenForTechnicalRoles(){
  Long id=service.save(request(),1L);
  grants.delete(null);
  assertServiceException(()->service.list(null),PAYROLL_PROJECT_DENIED);
  assertServiceException(()->service.history(id),PAYROLL_PROJECT_DENIED);
  assertServiceException(()->service.save(request(),1L),PAYROLL_PROJECT_DENIED);
  assertServiceException(()->service.initialize(1L),PAYROLL_PROJECT_DENIED);
 }
}
