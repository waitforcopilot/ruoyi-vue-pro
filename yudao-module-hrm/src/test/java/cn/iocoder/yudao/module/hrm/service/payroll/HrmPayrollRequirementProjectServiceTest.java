package cn.iocoder.yudao.module.hrm.service.payroll;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.*;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.mock.mockito.MockBean;
import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;
@Import({HrmPayrollRequirementAccessService.class,HrmPayrollRequirementProjectService.class,HrmPayrollRequirementServiceImpl.class})
class HrmPayrollRequirementProjectServiceTest extends BaseDbUnitTest {
 @Resource HrmPayrollRequirementProjectService service;
 @Resource HrmPayrollRequirementAccessService access;
 @Resource HrmPayrollRequirementService old;
 @Resource HrmPayrollRequirementProjectMapper projects;
 @Resource HrmPayrollRequirementMapper requirements;
 @Resource HrmPayrollRequirementHistoryMapper histories;
 @Resource HrmPayrollRequirementProjectGrantMapper grants;
 @Resource HrmPayrollRequirementTemplateMapper templates;
 @MockBean cn.iocoder.yudao.module.system.api.user.AdminUserApi users;
 @MockBean cn.iocoder.yudao.module.system.api.permission.PermissionApi permissions;
 @BeforeEach void setup(){for(long id:new long[]{1,2,3})when(users.getUser(id)).thenReturn(new cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO().setId(id).setStatus(0));when(permissions.hasAnyPermissions(anyLong(),anyString())).thenReturn(true);grant(0,1,"PROJECT_CREATE");}
 long create(String key){HrmPayrollRequirementProjectReqVO r=new HrmPayrollRequirementProjectReqVO();r.setProjectKey(key);r.setName(key);r.setCreationScopeJson(scope(10,true));return service.create(r,1L);}
 HrmPayrollRequirementTemplateDO seed(String key,int version,String text){HrmPayrollRequirementTemplateDO t=new HrmPayrollRequirementTemplateDO();t.setCatalogKey("DEFAULT");t.setTemplateItemId(key);t.setTemplateVersion(version);t.setOriginalCode(key);t.setDescription(text);t.setWithdrawn(false);templates.insert(t);return t;}
 String scope(int month,boolean extended){return "{\"entities\":"+(extended?"[101,102]":"[101]")+",\"organizations\":[201],\"workerTypes\":[\"TEST\"],\"scenarios\":[\"TEST\"],\"from\":\"2026-"+month+"-01\",\"to\":\"2026-"+month+"-28\",\"explanation\":\"TEST合成\"}";}
 void grant(long p,long user,String actions){HrmPayrollRequirementProjectGrantDO g=new HrmPayrollRequirementProjectGrantDO();g.setProjectId(p);g.setUserId(user);g.setActions(actions);g.setEvidence("TEST ONLY");g.setValidFrom(LocalDate.of(2026,1,1));g.setValidTo(LocalDate.of(2027,12,31));g.setCeilingJson("{\"entities\":[101,102],\"organizations\":[201],\"workerTypes\":[\"TEST\"],\"scenarios\":[\"TEST\"],\"from\":\"2026-01-01\",\"to\":\"2027-12-31\",\"explanation\":\"TEST\"}");grants.insert(g);}
 @Test void projectsArePrivateEvenToOtherTechnicalAdministrators(){long p=create("AA");assertEquals(1,service.list(1L).size());assertTrue(service.list(2L).isEmpty());assertServiceException(()->service.check(p,2L,"READ",false),PAYROLL_PROJECT_DENIED);grant(p,2,"READ");assertEquals(1,service.list(2L).size());assertServiceException(()->service.check(p,2L,"EDIT",false),PAYROLL_PROJECT_DENIED);}
 @Test void metadataDoesNotChangeScopeAndIsOptimistic(){long p=create("AA");HrmPayrollRequirementProjectReqVO r=new HrmPayrollRequirementProjectReqVO();r.setId(p);r.setProjectKey("AA");r.setName("new");r.setMetadataVersion(1);service.metadata(r,1L);assertEquals(2,projects.selectById(p).getMetadataVersion());assertNull(projects.selectById(p).getProjectScopeVersion());assertServiceException(()->service.metadata(r,1L),PAYROLL_VERSION_CONFLICT);}
 @Test void initializationIsPerProjectIdempotentAndKeepsUserContent(){seed("OV-01",1,"original");long a=create("AA"),b=create("BB");assertEquals(1,service.initialize(a,1L));assertEquals(0,service.initialize(a,1L));assertEquals(1,service.initialize(b,1L));List<HrmPayrollRequirementDO> rows=requirements.selectList();assertNotEquals(rows.get(0).getId(),rows.get(1).getId());assertEquals(rows.get(0).getCode(),rows.get(1).getCode());rows.get(0).setDescription("manual");requirements.updateById(rows.get(0));assertEquals(0,service.initialize(a,1L));assertEquals("manual",requirements.selectById(rows.get(0).getId()).getDescription());}
 @Test void templatesCompareBeforeAdoptingAndPreserveHistory(){seed("OV-01",1,"v1");long p=create("AA");service.initialize(p,1L);HrmPayrollRequirementDO r=requirements.selectList().get(0);r.setDescription("manual");requirements.updateById(r);seed("OV-01",2,"v2");assertEquals(1,service.differences(p,1L).size());assertEquals(0,service.initialize(p,1L));assertEquals("manual",requirements.selectById(r.getId()).getDescription());service.adopt(p,r.getId(),1,2,false,1L);assertEquals("manual",requirements.selectById(r.getId()).getDescription());assertEquals(2,requirements.selectById(r.getId()).getTemplateVersion());seed("OV-01",3,"v3");service.adopt(p,r.getId(),2,3,true,1L);assertEquals("v3",requirements.selectById(r.getId()).getDescription());assertEquals("REVIEWING",requirements.selectById(r.getId()).getStatus());assertEquals(3,histories.selectList().size());}
 @Test void removedRecordsAndWithdrawnTemplatesNeverReactivate(){seed("OV-01",1,"v1");long p=create("AA");service.initialize(p,1L);HrmPayrollRequirementDO r=requirements.selectList().get(0);r.setScopeDecision("REMOVED");requirements.updateById(r);seed("OV-01",2,"v2");service.adopt(p,r.getId(),1,2,true,1L);assertEquals("v1",requirements.selectById(r.getId()).getDescription());assertEquals("REMOVED",requirements.selectById(r.getId()).getScopeDecision());HrmPayrollRequirementTemplateDO t=seed("PLAN-01",1,"removed");t.setWithdrawn(true);templates.updateById(t);assertEquals(0,service.initialize(p,1L));}
 @Test void scopeRequiresExplicitBoundedAuthorityAndRetainsHistory(){long p=create("AA");seed("OV-01",1,"v1");service.initialize(p,1L);assertServiceException(()->service.scope(p,null,scope(10,false),"TEST",1L),PAYROLL_PROJECT_DENIED);grant(p,1,"READ,SCOPE");service.scope(p,null,scope(10,false),"TEST",1L);assertEquals(1,projects.selectById(p).getProjectScopeVersion());assertServiceException(()->service.scope(p,null,scope(11,false),"TEST",1L),PAYROLL_PROJECT_SCOPE_STALE);HrmPayrollRequirementDO r=requirements.selectList().get(0);r.setScopeMode("SUBSET");r.setScopeJson(scope(10,false));r.setStatus("CONFIRMED");r.setConfirmedBy(2L);r.setConfirmedAt(java.time.LocalDateTime.now());requirements.updateById(r);service.scope(p,1,scope(11,false),"TEST",1L);r=requirements.selectById(r.getId());assertEquals("SCOPE_MISMATCH",r.getScopeState());assertEquals("REVIEWING",r.getStatus());assertNull(r.getConfirmedBy());assertEquals(3,histories.selectList().size());assertEquals(2,projects.selectById(p).getProjectScopeVersion());}
 @Test void scopeCannotExceedAuthorityOrUseExpiredGrant(){long p=create("AA");grant(p,1,"READ,SCOPE");String bad=scope(10,false).replace("101","999");assertServiceException(()->service.scope(p,null,bad,"TEST",1L),PAYROLL_PROJECT_SCOPE_INVALID);assertNull(projects.selectById(p).getProjectScopeVersion());HrmPayrollRequirementProjectGrantDO g=grants.selectList().stream().filter(x->x.getActions().contains("SCOPE")).findFirst().get();g.setValidTo(LocalDate.of(2020,1,1));grants.updateById(g);assertServiceException(()->service.scope(p,null,scope(10,false),"TEST",1L),PAYROLL_PROJECT_DENIED);}
 @Test void crossProjectIdsCannotBeSubstituted(){seed("OV-01",1,"v1");long a=create("AA"),b=create("BB");service.initialize(a,1L);long r=requirements.selectList().get(0).getId();assertServiceException(()->service.target(b,r,1L,"EDIT",null,false),PAYROLL_PROJECT_DENIED);assertServiceException(()->service.adopt(b,r,1,2,true,1L),PAYROLL_PROJECT_DENIED);}
 @Test void scopeDatesRoundTripAsIsoStringsAndReadLegacyArrays(){
  HrmPayrollRequirementScope parsed=HrmPayrollRequirementScope.parse(scope(10,false));
  String json=JsonUtils.toJsonString(parsed);
  assertTrue(json.contains("\"from\":\"2026-10-01\""));
  assertEquals(parsed.getFrom(),HrmPayrollRequirementScope.parse(json).getFrom());
  String legacy=scope(10,false).replace("\"2026-10-01\"","[2026,10,1]").replace("\"2026-10-28\"","[2026,10,28]");
  assertEquals(parsed.getFrom(),HrmPayrollRequirementScope.parse(legacy).getFrom());
 }
 @Test void readGrantDoesNotGrantExport(){long p=create("AA");grant(p,2,"READ");assertServiceException(()->service.check(p,2L,"EXPORT",false),PAYROLL_PROJECT_DENIED);grant(p,2,"READ,EXPORT");assertEquals(p,service.check(p,2L,"EXPORT",false).getId());}
 @Test void failedCustomCreationRollsBackSequenceAndLeavesNoHistory(){
  long p=create("AA");HrmPayrollRequirementSaveReqVO r=new HrmPayrollRequirementSaveReqVO();
  r.setProjectId(p);r.setCode("CUSTOM-0000");r.setModule("COL");r.setDescription("TEST");r.setPriority(2);r.setStatus("CONFIRMED");r.setReadiness("MISSING");
  assertServiceException(()->old.save(r,1L),PAYROLL_REQUIREMENT_CONFIRM_ACTION_REQUIRED);
  assertEquals(0L,projects.selectById(p).getCustomSequence());assertTrue(requirements.selectList().isEmpty());assertTrue(histories.selectList().isEmpty());
  r.setStatus("PENDING");long id=old.save(r,1L);assertEquals("CUSTOM-0001",requirements.selectById(id).getCode());
 }
 @Test void customSequenceCannotReuseProjectNumbers(){long a=create("AA"),b=create("BB");HrmPayrollRequirementProjectDO p=service.check(a,1L,"EDIT",true);assertEquals("CUSTOM-0001",service.nextCode(p));assertEquals("CUSTOM-0002",service.nextCode(p));assertEquals("CUSTOM-0001",service.nextCode(service.check(b,1L,"EDIT",true)));}

 void scopedProject(long p){HrmPayrollRequirementProjectDO v=projects.selectById(p);v.setScopeJson(scope(10,true));v.setProjectScopeVersion(1);projects.updateById(v);}
 HrmPayrollRequirementProjectGrantDO delegated(long p,long user,String actions){HrmPayrollRequirementProjectGrantDO g=new HrmPayrollRequirementProjectGrantDO();g.setProjectId(p);g.setUserId(user);g.setActions(actions);g.setDelegableActions("");g.setFullProject(false);g.setCeilingJson(scope(10,false));g.setModulesJson("[\"COL\"]");g.setEvidence("TEST SYNTHETIC");g.setValidFrom(LocalDate.of(2026,1,1));g.setValidTo(LocalDate.of(2026,12,31));return g;}
 void manager(long p){grant(p,2,"READ,MEMBER,EDIT,ASSIGN,CONFIRM,EXPORT,SCOPE,META");HrmPayrollRequirementProjectGrantDO g=grants.selectList().stream().filter(x->x.getUserId()==2L).findFirst().get();g.setDelegableActions("READ,MEMBER,EDIT,ASSIGN,CONFIRM,EXPORT,SCOPE,META");g.setFullProject(true);grants.updateById(g);}
 HrmPayrollRequirementDO scopedRow(long p,String module){HrmPayrollRequirementDO r=new HrmPayrollRequirementDO();r.setProjectId(p);r.setCode("CUSTOM-9001");r.setModule(module);r.setDescription("TEST");r.setVersion(1);r.setPriority(2);r.setStatus("PENDING");r.setReadiness("MISSING");r.setScopeMode("SUBSET");r.setScopeJson(scope(10,false));r.setScopeState("MATCH");r.setProjectScopeVersion(1);requirements.insert(r);return r;}
 @Test void creatorDoesNotBecomeBusinessReviewerOrMemberAdministrator(){long p=create("AA");assertFalse(access.project(projects.selectById(p),1L,"CONFIRM"));assertFalse(access.project(projects.selectById(p),1L,"MEMBER"));assertTrue(access.project(projects.selectById(p),1L,"META"));}
 @Test void memberDelegationCannotIncreaseActionsScopeOrSelfGrant(){long p=create("AA");scopedProject(p);manager(p);HrmPayrollRequirementProjectGrantDO g=delegated(p,3,"READ,EDIT");g.setCeilingJson(scope(10,false).replace("101","999"));assertServiceException(()->service.grant(g,2L),PAYROLL_PROJECT_DENIED);g.setCeilingJson(scope(10,false));g.setActions("READ,BASELINE_PUBLISH");assertServiceException(()->service.grant(g,2L),PAYROLL_PROJECT_DENIED);g.setActions("READ,EDIT");g.setUserId(2L);assertServiceException(()->service.grant(g,2L),PAYROLL_PROJECT_DENIED);assertEquals(3,grants.selectList().size());}
 @Test void delegationRevokeInvalidatesChildImmediatelyAndVersionIsChecked(){long p=create("AA");scopedProject(p);manager(p);HrmPayrollRequirementProjectGrantDO g=delegated(p,3,"READ,EDIT");long id=service.grant(g,2L);assertTrue(access.project(projects.selectById(p),3L,"READ"));assertServiceException(()->service.revoke(p,id,9,"TEST",2L),PAYROLL_VERSION_CONFLICT);service.revoke(p,id,1,"TEST revoke",2L);assertFalse(access.project(projects.selectById(p),3L,"READ"));assertEquals(2,grants.selectById(id).getGrantVersion());assertServiceException(()->service.revoke(p,id,1,"TEST duplicate",2L),PAYROLL_VERSION_CONFLICT);}
 @Test void upstreamRevocationOrExpiryRevokesDelegatedAccess(){long p=create("AA");scopedProject(p);manager(p);long child=service.grant(delegated(p,3,"READ,EDIT"),2L);assertTrue(access.effective(grants.selectById(child)));HrmPayrollRequirementProjectGrantDO root=grants.selectById(grants.selectById(child).getParentGrantId());root.setRevokedAt(java.time.LocalDateTime.now());grants.updateById(root);assertFalse(access.effective(grants.selectById(child)));assertTrue(service.list(3L).isEmpty());}
 @Test void objectModuleAndBusinessBoundsAreIntersectedForEveryAction(){long p=create("AA");scopedProject(p);manager(p);HrmPayrollRequirementDO row=scopedRow(p,"COL");HrmPayrollRequirementProjectGrantDO g=delegated(p,3,"READ,EDIT,EXPORT");g.setRequirementIdsJson("["+row.getId()+"]");service.grant(g,2L);assertEquals(1,service.rows(p,null,3L,"READ").size());row.setModule("TAX");assertFalse(access.row(projects.selectById(p),row,3L,"READ"));row.setModule("COL");row.setId(row.getId()+100);assertFalse(access.row(projects.selectById(p),row,3L,"EXPORT"));row.setId(row.getId()-100);row.setScopeJson(scope(11,false));assertFalse(access.row(projects.selectById(p),row,3L,"EDIT"));assertFalse(access.full(projects.selectById(p),3L,"READ"));}
 @Test void editingCannotMoveRequirementOutsideGrantedModule(){long p=create("AA");scopedProject(p);manager(p);HrmPayrollRequirementDO row=scopedRow(p,"COL");service.grant(delegated(p,3,"READ,EDIT"),2L);HrmPayrollRequirementSaveReqVO request=cn.iocoder.yudao.framework.common.util.object.BeanUtils.toBean(row,HrmPayrollRequirementSaveReqVO.class);request.setModule("TAX");assertServiceException(()->old.save(request,3L),PAYROLL_PROJECT_DENIED);assertEquals("COL",requirements.selectById(row.getId()).getModule());assertTrue(histories.selectList().isEmpty());}
 @Test void editDoesNotAuthorizeProjectMetadataAndReviewerNeedsMatchingProjectScope(){long p=create("AA");scopedProject(p);manager(p);HrmPayrollRequirementDO row=scopedRow(p,"COL");service.grant(delegated(p,3,"READ,EDIT"),2L);assertServiceException(()->service.check(p,3L,"META",false),PAYROLL_PROJECT_DENIED);assertFalse(service.reviewer(row,3L));HrmPayrollRequirementProjectGrantDO g=delegated(p,3,"READ,CONFIRM");service.grant(g,2L);assertTrue(service.reviewer(row,3L));row.setModule("TAX");assertFalse(service.reviewer(row,3L));}

 @Test void tenantCreationAuthorityMustBeExplicitAndBounded(){
  HrmPayrollRequirementProjectReqVO req=new HrmPayrollRequirementProjectReqVO();req.setProjectKey("BOUNDED");req.setName("TEST");req.setCreationScopeJson(scope(10,false));
  assertServiceException(()->service.create(req,2L),PAYROLL_PROJECT_DENIED);
  req.setCreationScopeJson(scope(10,false).replace("101","999"));assertServiceException(()->service.create(req,1L),PAYROLL_PROJECT_DENIED);
  assertTrue(projects.selectList().isEmpty());
 }
 @Test void separateGrantsNeverCrossMultiplyActionsAndScopes(){
  long p=create("AA");scopedProject(p);manager(p);HrmPayrollRequirementDO a=scopedRow(p,"COL");
  HrmPayrollRequirementProjectGrantDO edit=delegated(p,3,"READ,EDIT");service.grant(edit,2L);
  HrmPayrollRequirementProjectGrantDO read=delegated(p,3,"READ");read.setCeilingJson(scope(10,true));service.grant(read,2L);
  a.setScopeJson(scope(10,true));assertTrue(access.row(projects.selectById(p),a,3L,"READ"));assertFalse(access.row(projects.selectById(p),a,3L,"EDIT"));
 }
 @Test void revokingOneGrantPreservesIndependentLegitimateAccess(){
  long p=create("AA");scopedProject(p);manager(p);long a=service.grant(delegated(p,3,"READ,EDIT"),2L);long b=service.grant(delegated(p,3,"READ"),2L);
  service.revoke(p,a,1,"TEST",2L);assertTrue(access.project(projects.selectById(p),3L,"READ"));assertFalse(access.project(projects.selectById(p),3L,"EDIT"));
  service.revoke(p,b,1,"TEST",2L);assertFalse(access.project(projects.selectById(p),3L,"READ"));
 }
 @Test void expiryAndDisabledAccountsInvalidateExistingDelegatedSessions(){
  long p=create("AA");scopedProject(p);manager(p);long id=service.grant(delegated(p,3,"READ,EDIT"),2L);
  when(users.getUser(3L)).thenReturn(new cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO().setId(3L).setStatus(1));assertFalse(access.effective(grants.selectById(id)));
  when(users.getUser(3L)).thenReturn(new cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO().setId(3L).setStatus(0));
  HrmPayrollRequirementProjectGrantDO g=grants.selectById(id);g.setValidTo(LocalDate.of(2020,1,1));grants.updateById(g);assertFalse(access.effective(grants.selectById(id)));
 }
 @Test void creatorDraftAccessIsExplicitAndRevocable(){
  long p=create("AA");manager(p);HrmPayrollRequirementProjectGrantDO bootstrap=grants.selectList().stream().filter(g->Objects.equals(g.getProjectId(),p)&&Objects.equals(g.getUserId(),1L)).findFirst().get();
  service.revoke(p,bootstrap.getId(),1,"TEST remove creator",2L);assertTrue(service.list(1L).isEmpty());assertTrue(service.list(2L).stream().anyMatch(x->x.getId()==p));
 }

 @Test void unknownCreatorCeilingCannotFollowLaterBusinessScope(){
  long p=create("AA");HrmPayrollRequirementProjectGrantDO creator=grants.selectList().stream().filter(g->Objects.equals(g.getProjectId(),p)&&Objects.equals(g.getUserId(),1L)).findFirst().get();
  // Reproduce the pre-S2 migrated creator grant whose original business boundary was unknown.
  grants.update(null,new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<HrmPayrollRequirementProjectGrantDO>().eq(HrmPayrollRequirementProjectGrantDO::getId,creator.getId()).set(HrmPayrollRequirementProjectGrantDO::getCeilingJson,null));
  scopedProject(p);HrmPayrollRequirementDO r=scopedRow(p,"COL");
  assertFalse(access.row(projects.selectById(p),r,1L,"READ"));assertFalse(access.full(projects.selectById(p),1L,"EDIT"));
 }
 @Test void boundedCreatorRetainsOldSubsetButNeverFollowsScopeExpansion(){
  long p=create("AA");scopedProject(p);HrmPayrollRequirementDO old=scopedRow(p,"COL");
  assertTrue(access.row(projects.selectById(p),old,1L,"READ"));
  HrmPayrollRequirementProjectDO project=projects.selectById(p);String expanded=scope(10,true).replace("101,102","101,102,103");project.setScopeJson(expanded);projects.updateById(project);
  HrmPayrollRequirementDO inherited=new HrmPayrollRequirementDO();inherited.setProjectId(p);inherited.setModule("COL");inherited.setScopeJson(expanded);
  assertFalse(access.row(projects.selectById(p),inherited,1L,"READ"));assertFalse(access.full(projects.selectById(p),1L,"EDIT"));assertTrue(access.row(projects.selectById(p),old,1L,"READ"));
 }
}
