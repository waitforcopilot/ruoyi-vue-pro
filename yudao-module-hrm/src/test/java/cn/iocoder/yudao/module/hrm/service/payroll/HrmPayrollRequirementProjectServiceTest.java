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
@Import({HrmPayrollRequirementProjectService.class,HrmPayrollRequirementServiceImpl.class})
class HrmPayrollRequirementProjectServiceTest extends BaseDbUnitTest {
 @Resource HrmPayrollRequirementProjectService service;
 @Resource HrmPayrollRequirementService old;
 @Resource HrmPayrollRequirementProjectMapper projects;
 @Resource HrmPayrollRequirementMapper requirements;
 @Resource HrmPayrollRequirementHistoryMapper histories;
 @Resource HrmPayrollRequirementProjectGrantMapper grants;
 @Resource HrmPayrollRequirementTemplateMapper templates;
 @MockBean cn.iocoder.yudao.module.system.api.user.AdminUserApi users;
 @MockBean cn.iocoder.yudao.module.system.api.permission.PermissionApi permissions;
 @BeforeEach void setup(){for(long id:new long[]{1,2,3})when(users.getUser(id)).thenReturn(new cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO().setId(id).setStatus(0));when(permissions.hasAnyPermissions(anyLong(),anyString())).thenReturn(true);}
 long create(String key){HrmPayrollRequirementProjectReqVO r=new HrmPayrollRequirementProjectReqVO();r.setProjectKey(key);r.setName(key);return service.create(r,1L);}
 HrmPayrollRequirementTemplateDO seed(String key,int version,String text){HrmPayrollRequirementTemplateDO t=new HrmPayrollRequirementTemplateDO();t.setCatalogKey("DEFAULT");t.setTemplateItemId(key);t.setTemplateVersion(version);t.setOriginalCode(key);t.setDescription(text);t.setWithdrawn(false);templates.insert(t);return t;}
 String scope(int month,boolean extended){return "{\"entities\":"+(extended?"[101,102]":"[101]")+",\"organizations\":[201],\"workerTypes\":[\"TEST\"],\"scenarios\":[\"TEST\"],\"from\":\"2026-"+month+"-01\",\"to\":\"2026-"+month+"-28\",\"explanation\":\"TEST合成\"}";}
 void grant(long p,long user,String actions){HrmPayrollRequirementProjectGrantDO g=new HrmPayrollRequirementProjectGrantDO();g.setProjectId(p);g.setUserId(user);g.setActions(actions);g.setEvidence("TEST ONLY");g.setValidFrom(LocalDate.of(2026,1,1));g.setValidTo(LocalDate.of(2027,12,31));g.setCeilingJson("{\"entities\":[101,102],\"organizations\":[201],\"workerTypes\":[\"TEST\"],\"scenarios\":[\"TEST\"],\"from\":\"2026-01-01\",\"to\":\"2027-12-31\",\"explanation\":\"TEST\"}");grants.insert(g);}
 @Test void projectsArePrivateEvenToOtherTechnicalAdministrators(){long p=create("AA");assertEquals(1,service.list(1L).size());assertTrue(service.list(2L).isEmpty());assertServiceException(()->service.check(p,2L,"READ",false),PAYROLL_PROJECT_DENIED);grant(p,2,"READ");assertEquals(1,service.list(2L).size());assertServiceException(()->service.check(p,2L,"EDIT",false),PAYROLL_PROJECT_DENIED);}
 @Test void metadataDoesNotChangeScopeAndIsOptimistic(){long p=create("AA");HrmPayrollRequirementProjectReqVO r=new HrmPayrollRequirementProjectReqVO();r.setId(p);r.setProjectKey("AA");r.setName("new");r.setMetadataVersion(1);service.metadata(r,1L);assertEquals(2,projects.selectById(p).getMetadataVersion());assertNull(projects.selectById(p).getProjectScopeVersion());assertServiceException(()->service.metadata(r,1L),PAYROLL_VERSION_CONFLICT);}
 @Test void initializationIsPerProjectIdempotentAndKeepsUserContent(){seed("OV-01",1,"original");long a=create("AA"),b=create("BB");assertEquals(1,service.initialize(a,1L));assertEquals(0,service.initialize(a,1L));assertEquals(1,service.initialize(b,1L));List<HrmPayrollRequirementDO> rows=requirements.selectList();assertNotEquals(rows.get(0).getId(),rows.get(1).getId());assertEquals(rows.get(0).getCode(),rows.get(1).getCode());rows.get(0).setDescription("manual");requirements.updateById(rows.get(0));assertEquals(0,service.initialize(a,1L));assertEquals("manual",requirements.selectById(rows.get(0).getId()).getDescription());}
 @Test void templatesCompareBeforeAdoptingAndPreserveHistory(){seed("OV-01",1,"v1");long p=create("AA");service.initialize(p,1L);HrmPayrollRequirementDO r=requirements.selectList().get(0);r.setDescription("manual");requirements.updateById(r);seed("OV-01",2,"v2");assertEquals(1,service.differences(p,1L).size());assertEquals(0,service.initialize(p,1L));assertEquals("manual",requirements.selectById(r.getId()).getDescription());service.adopt(p,r.getId(),1,2,false,1L);assertEquals("manual",requirements.selectById(r.getId()).getDescription());assertEquals(2,requirements.selectById(r.getId()).getTemplateVersion());seed("OV-01",3,"v3");service.adopt(p,r.getId(),2,3,true,1L);assertEquals("v3",requirements.selectById(r.getId()).getDescription());assertEquals("REVIEWING",requirements.selectById(r.getId()).getStatus());assertEquals(3,histories.selectList().size());}
 @Test void removedRecordsAndWithdrawnTemplatesNeverReactivate(){seed("OV-01",1,"v1");long p=create("AA");service.initialize(p,1L);HrmPayrollRequirementDO r=requirements.selectList().get(0);r.setScopeDecision("REMOVED");requirements.updateById(r);seed("OV-01",2,"v2");service.adopt(p,r.getId(),1,2,true,1L);assertEquals("v1",requirements.selectById(r.getId()).getDescription());assertEquals("REMOVED",requirements.selectById(r.getId()).getScopeDecision());HrmPayrollRequirementTemplateDO t=seed("PLAN-01",1,"removed");t.setWithdrawn(true);templates.updateById(t);assertEquals(0,service.initialize(p,1L));}
 @Test void scopeRequiresExplicitBoundedAuthorityAndRetainsHistory(){long p=create("AA");seed("OV-01",1,"v1");service.initialize(p,1L);assertServiceException(()->service.scope(p,null,scope(10,false),"TEST",1L),PAYROLL_PROJECT_DENIED);grant(p,1,"READ,SCOPE");service.scope(p,null,scope(10,false),"TEST",1L);assertEquals(1,projects.selectById(p).getProjectScopeVersion());assertServiceException(()->service.scope(p,null,scope(11,false),"TEST",1L),PAYROLL_PROJECT_SCOPE_STALE);HrmPayrollRequirementDO r=requirements.selectList().get(0);r.setScopeMode("SUBSET");r.setScopeJson(scope(10,false));r.setStatus("CONFIRMED");r.setConfirmedBy(2L);r.setConfirmedAt(java.time.LocalDateTime.now());requirements.updateById(r);service.scope(p,1,scope(11,false),"TEST",1L);r=requirements.selectById(r.getId());assertEquals("SCOPE_MISMATCH",r.getScopeState());assertEquals("REVIEWING",r.getStatus());assertNull(r.getConfirmedBy());assertEquals(3,histories.selectList().size());assertEquals(2,projects.selectById(p).getProjectScopeVersion());}
 @Test void scopeCannotExceedAuthorityOrUseExpiredGrant(){long p=create("AA");grant(p,1,"READ,SCOPE");String bad=scope(10,false).replace("101","999");assertServiceException(()->service.scope(p,null,bad,"TEST",1L),PAYROLL_PROJECT_SCOPE_INVALID);assertNull(projects.selectById(p).getProjectScopeVersion());HrmPayrollRequirementProjectGrantDO g=grants.selectList().get(0);g.setValidTo(LocalDate.of(2020,1,1));grants.updateById(g);assertServiceException(()->service.scope(p,null,scope(10,false),"TEST",1L),PAYROLL_PROJECT_DENIED);}
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
}
