package cn.iocoder.yudao.module.hrm.service.payroll;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollRequirementSaveReqVO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.HrmPayrollRequirementDO;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import javax.annotation.Resource;
import static org.junit.jupiter.api.Assertions.*;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;
@Import({HrmPayrollRequirementServiceImpl.class,HrmPayrollRequirementProjectService.class})
class HrmPayrollRequirementServiceImplTest extends BaseDbUnitTest {
 @Resource private HrmPayrollRequirementService service;
 @org.springframework.boot.test.mock.mockito.MockBean private cn.iocoder.yudao.module.system.api.user.AdminUserApi users;
 @org.springframework.boot.test.mock.mockito.MockBean private cn.iocoder.yudao.module.system.api.permission.PermissionApi permissions;
 @org.junit.jupiter.api.BeforeEach void users() {
  org.mockito.Mockito.when(permissions.hasAnyPermissions(org.mockito.ArgumentMatchers.anyLong(),org.mockito.ArgumentMatchers.anyString())).thenReturn(true);
  for(long id:new long[]{2,3}) org.mockito.Mockito.when(users.getUser(id)).thenReturn(new cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO().setId(id).setStatus(0));
 }
 private HrmPayrollRequirementSaveReqVO request() {
  HrmPayrollRequirementSaveReqVO r=new HrmPayrollRequirementSaveReqVO();r.setCode("PLAN-06");r.setModule("PLAN");r.setDescription("测试薪资方案规则");r.setStatus("PENDING");r.setReadiness("MISSING");r.setPriority(2);return r;
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
  assertServiceException(()->service.save(r,2L),PAYROLL_REQUIREMENT_CONFIRM_ACTION_REQUIRED);assertTrue(service.list(null).isEmpty());assertNull(service.legacyProject());
  r.setStatus("PENDING");r.setId(service.save(r,1L));r.setVersion(1);r.setStatus("CONFIRMED");
  assertServiceException(()->service.save(r,2L),PAYROLL_REQUIREMENT_CONFIRM_ACTION_REQUIRED);assertNull(row(r.getId()).getConfirmedBy());assertEquals(1,service.history(r.getId()).size());
 }
 @Test void confirmationUsesPersistedVersionAndDoesNotChangeReadiness() {
  HrmPayrollRequirementSaveReqVO r=ready();Long id=service.save(r,1L);r.setDescription("客户端未保存修改");
  service.confirm(id,1,2L);assertEquals("测试薪资方案规则",row(id).getDescription());assertEquals("CONFIRMED",row(id).getStatus());assertEquals("MISSING",row(id).getReadiness());assertEquals(2L,row(id).getConfirmedBy());assertEquals(2,service.history(id).size());
 }
 @Test void incompleteConfirmationIsRejectedWithoutNewHistory() {
  Long id=service.save(request(),1L);assertServiceException(()->service.confirm(id,1,2L),PAYROLL_REVIEW_INCOMPLETE);assertEquals(1,service.history(id).size());assertEquals("PENDING",row(id).getStatus());
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
  assertNull(service.legacyProject());HrmPayrollRequirementSaveReqVO r=request();r.setId(service.save(r,1L));Long project=service.legacyProject().getId();assertNull(service.legacyProject().getProjectScopeVersion());assertEquals(1,service.legacyProject().getMetadataVersion());
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

}
