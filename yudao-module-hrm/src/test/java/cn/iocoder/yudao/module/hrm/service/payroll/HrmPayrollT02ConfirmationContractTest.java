package cn.iocoder.yudao.module.hrm.service.payroll;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.mybatis.core.util.MyBatisUtils;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.config.TenantProperties;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.db.TenantDatabaseInterceptor;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollGovernanceVO;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollRequirementSaveReqVO;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollWorkbenchVO.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.*;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.sql.Connection;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;
import static cn.iocoder.yudao.module.hrm.service.payroll.HrmPayrollGovernanceStore.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** T02 service contracts on real H2 transactions, tenant SQL filtering, controlled roots and decisions.
 * Only external user/permission APIs are mocked. This is not browser/session or MySQL acceptance. */
@Import({HrmPayrollT02ConfirmationContractTest.TenantSql.class,HrmPayrollGovernanceStore.class,
    HrmPayrollGovernanceRootService.class,HrmPayrollGovernanceDecisionService.class,
    HrmPayrollRequirementAccessService.class,HrmPayrollRequirementProjectService.class,
    HrmPayrollRequirementServiceImpl.class,HrmPayrollWorkbenchService.class})
@Timeout(40)
class HrmPayrollT02ConfirmationContractTest extends BaseDbUnitTest {
    @TestConfiguration
    static class TenantSql {
        // Same handler and insertion order as the production tenant configuration; no web/cache beans.
        @Bean TenantLineInnerInterceptor t02TenantSql(MybatisPlusInterceptor interceptor) {
            TenantLineInnerInterceptor tenant=new TenantLineInnerInterceptor(new TenantDatabaseInterceptor(new TenantProperties()));
            MyBatisUtils.addInterceptor(interceptor,tenant,0);return tenant;
        }
    }
    @Resource TenantLineInnerInterceptor t02TenantSql;
    @Resource MybatisPlusInterceptor sqlInterceptors;
    @Resource HrmPayrollGovernanceStore store;
    @Resource HrmPayrollGovernanceRootService roots;
    @Resource HrmPayrollGovernanceDecisionService decisions;
    @Resource HrmPayrollRequirementService requirements;
    @Resource HrmPayrollRequirementProjectService projectService;
    @Resource HrmPayrollWorkbenchService workbench;
    @Resource HrmPayrollRequirementMapper reqs;
    @Resource HrmPayrollRequirementProjectMapper projects;
    @Resource HrmPayrollRequirementProjectGrantMapper grants;
    @Resource HrmPayrollRequirementHistoryMapper histories;
    @Resource HrmPayrollWorkbenchRecordMapper records;
    @Resource PlatformTransactionManager transactionManager;
    @Resource DataSource dataSource;
    @MockBean cn.iocoder.yudao.module.system.api.user.AdminUserApi users;
    @MockBean cn.iocoder.yudao.module.system.api.permission.PermissionApi permissions;
    Long project,anchor,reviewerRoot;
    final String scope="{\"entities\":[101],\"organizations\":[201],\"workerTypes\":[\"TEST\"],\"scenarios\":[\"TEST\"],\"from\":\"2026-10-01\",\"to\":\"2026-10-31\",\"explanation\":\"TEST T02 fixed scope\"}";
    String upper(){return scope.replace("[101]","[101,102]").replace("2026-10-31","2026-12-31");}
    void identity(long tenant,long actor){
        TenantContextHolder.setTenantId(tenant);
        SecurityFrameworkUtils.setLoginUser(new LoginUser().setId(actor).setUserType(2),new MockHttpServletRequest());
    }
    void clearIdentity(){TenantContextHolder.clear();SecurityContextHolder.clearContext();}
    @BeforeEach void setup(){
        identity(1L,1L);assertSame(t02TenantSql,sqlInterceptors.getInterceptors().get(0));
        when(permissions.hasAnyPermissions(anyLong(),anyString())).thenReturn(true);
        for(long user=1;user<=5;user++)when(users.getUser(user)).thenReturn(new cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO().setId(user).setStatus(0).setNickname("TEST T02 user "+user));
        when(permissions.hasAnyPermissions(eq(5L),anyString())).thenAnswer(call->Arrays.asList("hrm:payroll:requirement:query","hrm:payroll:requirement:update").contains(call.getArgument(1)));
        store.insert(0L,"LOCK","TENANT",map());
        String body="TEST independently registered T02 root evidence; not formal approval";
        // Custodian 1 and signer 4 are independent of the business reviewer 2.
        anchor=store.insert(0L,"ANCHOR","TEST-T02-ANCHOR",map("state","REGISTERED","mode","TEST","custodianId",1L,"signerId",4L,"actions",new ArrayList<>(HrmPayrollRequirementAccessService.ACTIONS),"scopeJson",upper(),"validFrom","2026-01-01","validTo","2027-12-31","body",body,"digest",bodyDigest(body),"materialVersion","TEST-T02-V1")).getId();
        project=newProject("TEST_T02");
        root(project,1L,new ArrayList<>(HrmPayrollRequirementAccessService.ACTIONS));
        reviewerRoot=root(project,2L,new ArrayList<>(HrmPayrollRequirementAccessService.ACTIONS));
        root(project,3L,new ArrayList<>(HrmPayrollRequirementAccessService.ACTIONS));
        root(project,4L,new ArrayList<>(HrmPayrollRequirementAccessService.ACTIONS));
        root(project,5L,Arrays.asList("READ","EDIT"));policy();
    }
    @AfterEach void cleanup(){clearIdentity();}
    Long newProject(String key){
        HrmPayrollRequirementProjectDO p=new HrmPayrollRequirementProjectDO().setProjectKey(key).setName("TEST T02 project").setOwnerId(1L).setProjectScopeVersion(1).setMetadataVersion(1).setCustomSequence(0L).setScopeJson(scope).setArchived(false);projects.insert(p);return p.getId();
    }
    HrmPayrollGovernanceVO.Action action(Long ownerProject,Long id,String kind,String operation){
        HrmPayrollGovernanceVO.Action c=new HrmPayrollGovernanceVO.Action();c.setProjectId(ownerProject);c.setId(id);c.setVersion(store.record(id,kind).getVersion());c.setAction(operation);c.setReason("TEST independent T02 action");return c;
    }
    Long root(Long ownerProject,Long user,List<String> actions){
        HrmPayrollGovernanceVO.Root c=new HrmPayrollGovernanceVO.Root();c.setProjectId(ownerProject);c.setAnchorId(anchor);c.setUserId(user);c.setActions(actions);c.setDelegableActions(actions);c.setScopeJson(upper());c.setValidFrom("2026-01-01");c.setValidTo("2027-12-31");c.setMaterialVersion("TEST-T02-V1");c.setBody("TEST controlled T02 user "+user);c.setReason("TEST independent grant");
        Long id=roots.register(c,1L);roots.action(action(ownerProject,id,"ROOT","SIGN"),4L);return id;
    }
    void policy(){
        for(List<String> qs:Arrays.asList(Arrays.asList("Q-01","Q-02","Q-07","Q-10"),Collections.singletonList("Q-04"),Collections.singletonList("Q-05"))){
            HrmPayrollGovernanceVO.Decision c=new HrmPayrollGovernanceVO.Decision();c.setProjectId(project);c.setProjectScopeVersion(projects.selectById(project).getProjectScopeVersion());c.setTitle("TEST T02 "+qs);c.setQuestions(qs);c.setClauses("TEST PRD v0.4");c.setClauseVersion("TEST T02 V1");c.setBody("TEST controlled T02 decision "+qs);c.setScopeJson(projects.selectById(project).getScopeJson());c.setValidFrom("2026-01-01");c.setValidTo("2027-12-31");c.setApproverId(4L);c.setApprovalConditions("TEST independently reviewed");c.setSemanticChange(false);Map<String,String> other=new LinkedHashMap<>();for(String q:Arrays.asList("Q-03","Q-06","Q-08","Q-09"))other.put(q,"ADVISORY");c.setOtherQuestions(other);c.setReason("TEST controlled draft");
            Long id=decisions.draft(c,1L);decisions.action(action(project,id,"DECISION","SUBMIT"),1L);decisions.action(action(project,id,"DECISION","APPROVE"),4L);
        }
    }
    Long requirement(boolean applicable){
        HrmPayrollRequirementSaveReqVO q=new HrmPayrollRequirementSaveReqVO();q.setProjectId(project);q.setProjectScopeVersion(projects.selectById(project).getProjectScopeVersion());q.setCode("CUSTOM-0000");q.setModule("COL");q.setDescription("TEST original requirement");q.setPriority(2);q.setStatus("PENDING");q.setReadiness("MISSING");q.setScopeMode("SUBSET");q.setScopeJson(scope);q.setRuleVersion("TEST RULE V1");q.setSourceApplicability(applicable?"APPLICABLE":"NOT_APPLICABLE");q.setApplicabilityReason("TEST navigation-only discussion consumes no business data");q.setAcceptance("TEST independently verifiable expected result");q.setEvidence("TEST T02 evidence, not formal approval");
        q.setBusinessContextJson(JsonUtils.toJsonString(map("role","TEST HR","scenario","TEST monthly review","goal","TEST collect","expected","TEST expected","normalCase","TEST normal","exceptionCase","TEST missing","calculation","TEST rule","unit","TEST hour","precision","TEST 2 decimals","validity","TEST 2026-10","ruleException","TEST return for review")));
        Long id=requirements.save(q,1L);requirements.assign(id,reqs.selectById(id).getVersion(),2L,1L);return id;
    }
    HrmPayrollRequirementSaveReqVO draft(HrmPayrollRequirementDO row){return JsonUtils.parseObject(JsonUtils.toJsonString(row),HrmPayrollRequirementSaveReqVO.class);}
    void confirm(Long id){requirements.confirm(id,reqs.selectById(id).getVersion(),2L);}
    <T extends Context>T context(T c,Long id){c.setProjectId(project);c.setProjectScopeVersion(projects.selectById(project).getProjectScopeVersion());c.setId(id);c.setVersion(id==null?null:records.selectById(id).getVersion());c.setReason("TEST T02 mutation");return c;}
    Long source(){
        Source s=new Source();s.setName("TEST source");s.setModule("COL");s.setScopeJson(scope);s.setOwnerId(1L);s.setTechnicalContact("TEST IT");s.setAcquisition("TEST file");s.setSpecification("TEST contract V1");s.setUniqueKey("employee,period");s.setEmployeeMatch("TEST employee ID");s.setFrequency("monthly");s.setCutoff("month end");s.setVolume("TEST two rows");s.setQualityRule("TEST QS");Field f=new Field();f.setName("hours");f.setMeaning("TEST work duration");f.setType("decimal");f.setUnit("hour");f.setRequired(true);s.setFields(Collections.singletonList(f));SourceSave c=context(new SourceSave(),null);c.setSource(s);return workbench.saveSource(c,1L);
    }
    Long link(Long source,Long requirement){
        Mapping m=new Mapping();m.setSourceField("hours");m.setPageField("workingHours");m.setMeaning("TEST work duration");m.setType("decimal");m.setUnit("hour");m.setRequired(true);m.setTransform("TEST identity");m.setNullRule("reject");m.setDuplicateRule("reject");m.setPeriodRule("same month");m.setExceptionRule("TEST manual correction");Relation r=new Relation();r.setSourceId(source);r.setRequirementId(requirement);r.setFields(Collections.singletonList(m));r.setConsumptionRule("TEST QC");RelationSave c=context(new RelationSave(),null);c.setRelation(r);return workbench.saveRelation(c,1L);
    }
    Source sourceData(Long id){return JsonUtils.parseObject(records.selectById(id).getDataJson(),Source.class);}
    void changeSource(Long id,Consumer<Source> change){Source s=sourceData(id);change.accept(s);SourceSave c=context(new SourceSave(),id);c.setSource(s);workbench.saveSource(c,1L);}
    void changeMapping(Long id){Relation r=JsonUtils.parseObject(records.selectById(id).getDataJson(),Relation.class);r.getFields().get(0).setTransform("TEST divide by 60");RelationSave c=context(new RelationSave(),id);c.setRelation(r);workbench.saveRelation(c,1L);}
    List<HrmPayrollRequirementHistoryDO> history(Long id){return histories.selectList(new cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX<HrmPayrollRequirementHistoryDO>().eq(HrmPayrollRequirementHistoryDO::getRequirementId,id));}
    void unchanged(Long id,String before,int historyCount){assertEquals(before,JsonUtils.toJsonString(reqs.selectById(id)));assertEquals(historyCount,history(id).size());}
    void oldSignatureRemains(Long id,String tuple){assertTrue(history(id).stream().anyMatch(h->Objects.equals(tuple,JsonUtils.parseObject(h.getSnapshot(),HrmPayrollRequirementDO.class).getConfirmedTupleJson())));}

    @Test void editOnlyCannotForgeReviewerStatusOrUseConfirmationAction(){
        Long id=requirement(false);String before=JsonUtils.toJsonString(reqs.selectById(id));int count=history(id).size();
        HrmPayrollRequirementSaveReqVO forged=draft(reqs.selectById(id));forged.setReviewerId(3L);
        assertServiceException(()->requirements.save(forged,5L),PAYROLL_PROJECT_DENIED);unchanged(id,before,count);
        forged.setReviewerId(2L);forged.setStatus("CONFIRMED");assertServiceException(()->requirements.save(forged,5L),PAYROLL_REQUIREMENT_CONFIRM_ACTION_REQUIRED);unchanged(id,before,count);
        assertServiceException(()->requirements.confirm(id,reqs.selectById(id).getVersion(),5L),PAYROLL_PROJECT_DENIED);unchanged(id,before,count);
        forged.setStatus("PENDING");forged.setDescription("TEST legitimate EDIT-only saved content");requirements.save(forged,5L);assertNull(reqs.selectById(id).getConfirmedBy());confirm(id);assertEquals(2L,reqs.selectById(id).getConfirmedBy());
    }
    @Test void nonAssignedAndOutOfScopeConfirmersCannotSign(){
        Long id=requirement(false);int count=history(id).size();assertServiceException(()->requirements.confirm(id,reqs.selectById(id).getVersion(),3L),PAYROLL_REQUIREMENT_CONFIRMATION_INVALID);assertNull(reqs.selectById(id).getConfirmedBy());assertEquals(count,history(id).size());
        HrmPayrollRequirementProjectGrantDO g=grants.selectList().stream().filter(x->Objects.equals(x.getUserId(),2L)).findFirst().get();g.setCeilingJson(scope.replace("2026-10-01","2026-10-16"));grants.updateById(g);
        assertServiceException(()->requirements.confirm(id,reqs.selectById(id).getVersion(),2L),PAYROLL_PROJECT_DENIED);assertNull(reqs.selectById(id).getConfirmedBy());assertEquals(count,history(id).size());
    }
    @ParameterizedTest @ValueSource(strings={"BUSINESS_CONTEXT","RULE_VERSION","APPLICABILITY","SOURCE_CONTRACT","MAPPING","REVIEWER","PROJECT_SCOPE"})
    void sevenIndependentSemanticChangesInvalidateOnlyTheirConsumers(String kind){
        Long first=requirement(true),second=requirement(true),source=source(),relation=link(source,first);link(source,second);confirm(first);confirm(second);
        HrmPayrollRequirementDO before=reqs.selectById(first),other=reqs.selectById(second);String tuple=before.getConfirmedTupleJson();
        HrmPayrollRequirementSaveReqVO q=draft(before);
        switch(kind){
            case "BUSINESS_CONTEXT":Map<String,Object> business=JsonUtils.parseObject(q.getBusinessContextJson(),Map.class);business.put("goal","TEST revised business goal");q.setBusinessContextJson(JsonUtils.toJsonString(business));requirements.save(q,1L);break;
            case "RULE_VERSION":q.setRuleVersion("TEST RULE V2");requirements.save(q,1L);break;
            case "APPLICABILITY":q.setSourceApplicability("NOT_APPLICABLE");q.setApplicabilityReason("TEST menu-only function no longer consumes business data");requirements.save(q,1L);break;
            case "SOURCE_CONTRACT":changeSource(source,s->s.setSpecification("TEST contract V2"));break;
            case "MAPPING":changeMapping(relation);break;
            case "REVIEWER":requirements.assign(first,before.getVersion(),3L,1L);break;
            case "PROJECT_SCOPE":projectService.scope(project,1,upper(),"TEST explicitly expand project scope",1L);break;
            default:fail(kind);
        }
        HrmPayrollRequirementDO after=reqs.selectById(first);assertEquals("REVIEWING",after.getStatus());assertNull(after.getConfirmedBy());assertNull(after.getConfirmedAt());assertNull(after.getConfirmedTupleJson());assertTrue(after.getSemanticVersion()>before.getSemanticVersion());oldSignatureRemains(first,tuple);
        if(Arrays.asList("SOURCE_CONTRACT","PROJECT_SCOPE").contains(kind)){assertEquals("REVIEWING",reqs.selectById(second).getStatus());assertNull(reqs.selectById(second).getConfirmedTupleJson());oldSignatureRemains(second,other.getConfirmedTupleJson());}
        else {assertEquals("CONFIRMED",reqs.selectById(second).getStatus());assertEquals(other.getConfirmedTupleJson(),reqs.selectById(second).getConfirmedTupleJson());assertTrue(workbench.currentConfirmationValid(reqs.selectById(second)));}
    }
    @ParameterizedTest @ValueSource(strings={"NAME","CONTACT","OWNER","SUPPLY"})
    void operationalChangesPreserveFunctionalSignature(String kind){
        Long id=requirement(true),source=source();link(source,id);confirm(id);HrmPayrollRequirementDO before=reqs.selectById(id);int semantic=sourceData(source).getSemanticVersion();
        if("SUPPLY".equals(kind)){Supply c=context(new Supply(),source);c.setSupply("PENDING");c.setReplaceDelivery(false);workbench.supply(c,1L);}
        else changeSource(source,s->{if("NAME".equals(kind))s.setName("TEST display name V2");else if("CONTACT".equals(kind))s.setTechnicalContact("TEST IT V2");else s.setOwnerId(3L);});
        HrmPayrollRequirementDO after=reqs.selectById(id);assertEquals(before.getVersion(),after.getVersion());assertEquals(before.getConfirmedTupleJson(),after.getConfirmedTupleJson());assertEquals(before.getConfirmedBy(),after.getConfirmedBy());assertEquals(before.getConfirmedAt(),after.getConfirmedAt());assertEquals(semantic,sourceData(source).getSemanticVersion());assertTrue(workbench.currentConfirmationValid(after));
    }
    @ParameterizedTest @ValueSource(strings={"REVOKED","EXPIRED","DISABLED"})
    void lostMembershipPreservesPastSignatureButRequiresAuthorizedSuccessor(String kind){
        Long id=requirement(false);confirm(id);HrmPayrollRequirementDO signed=reqs.selectById(id);String tuple=signed.getConfirmedTupleJson();
        if("REVOKED".equals(kind))roots.action(action(project,reviewerRoot,"ROOT","REVOKE"),4L);
        else if("DISABLED".equals(kind))when(users.getUser(2L)).thenReturn(new cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO().setId(2L).setStatus(1));
        else {HrmPayrollRequirementProjectGrantDO g=grants.selectList().stream().filter(x->Objects.equals(x.getUserId(),2L)).findFirst().get();g.setValidTo(LocalDate.of(2020,1,1));grants.updateById(g);}
        assertEquals(tuple,reqs.selectById(id).getConfirmedTupleJson());assertEquals(signed.getConfirmedAt(),reqs.selectById(id).getConfirmedAt());assertTrue(workbench.currentConfirmationValid(reqs.selectById(id)));
        assertServiceException(()->requirements.confirm(id,signed.getVersion(),2L),PAYROLL_PROJECT_DENIED);
        identity(1L,2L);assertServiceException(()->requirements.history(id),PAYROLL_PROJECT_DENIED);identity(1L,1L);assertFalse(requirements.history(id).isEmpty());oldSignatureRemains(id,tuple);
        requirements.assign(id,signed.getVersion(),3L,1L);assertEquals("REVIEWING",reqs.selectById(id).getStatus());assertNull(reqs.selectById(id).getConfirmedBy());
        requirements.confirm(id,reqs.selectById(id).getVersion(),3L);assertEquals(3L,reqs.selectById(id).getConfirmedBy());oldSignatureRemains(id,tuple);
    }
    @Test void crossProjectAndCrossTenantObjectSubstitutionCannotMutateOriginal(){
        Long id=requirement(true),source=source(),relation=link(source,id);confirm(id);String before=JsonUtils.toJsonString(reqs.selectById(id));int count=history(id).size();
        Long other=newProject("TEST_T02_OTHER");root(other,1L,new ArrayList<>(HrmPayrollRequirementAccessService.ACTIONS));
        HrmPayrollRequirementSaveReqVO foreign=draft(reqs.selectById(id));foreign.setId(null);foreign.setVersion(null);foreign.setProjectId(other);foreign.setReviewerId(null);foreign.setStatus("PENDING");foreign.setSourceApplicability("NOT_APPLICABLE");Long foreignId=requirements.save(foreign,1L);
        assertServiceException(()->requirements.confirm(foreignId,reqs.selectById(foreignId).getVersion(),2L),PAYROLL_PROJECT_DENIED);assertNull(reqs.selectById(foreignId).getConfirmedBy());
        HrmPayrollRequirementSaveReqVO moved=draft(reqs.selectById(id));moved.setProjectId(other);assertServiceException(()->requirements.save(moved,1L),PAYROLL_PROJECT_DENIED);
        RelationSave rc=context(new RelationSave(),relation);rc.setProjectId(other);rc.setRelation(JsonUtils.parseObject(records.selectById(relation).getDataJson(),Relation.class));assertServiceException(()->workbench.saveRelation(rc,1L),PAYROLL_PROJECT_DENIED);unchanged(id,before,count);
        // A current tenant-2 lock prevents an unrelated missing-lock failure from masking isolation.
        identity(2L,1L);store.insert(0L,"LOCK","TENANT",map());assertNull(reqs.selectById(id));assertNull(projects.selectById(project));assertNull(records.selectById(source));assertNull(records.selectById(relation));
        assertServiceException(()->requirements.confirm(id,moved.getVersion(),2L),PAYROLL_REQUIREMENT_NOT_EXISTS);assertServiceException(()->requirements.history(id),PAYROLL_REQUIREMENT_NOT_EXISTS);assertServiceException(()->requirements.save(moved,1L),PAYROLL_PROJECT_DENIED);assertServiceException(()->workbench.saveRelation(rc,1L),PAYROLL_PROJECT_DENIED);
        identity(1L,1L);unchanged(id,before,count);confirmOwnProjectPositive(id);
    }
    void confirmOwnProjectPositive(Long id){HrmPayrollRequirementSaveReqVO q=draft(reqs.selectById(id));q.setDescription("TEST authorized original-project revision");requirements.save(q,1L);confirm(id);assertEquals(2L,reqs.selectById(id).getConfirmedBy());}

    static final class Outcome {final Integer code;Outcome(Integer code){this.code=code;}}
    TransactionTemplate transaction(){TransactionTemplate tx=new TransactionTemplate(transactionManager);tx.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);return tx;}
    void await(CountDownLatch latch){try{assertTrue(latch.await(8,TimeUnit.SECONDS),"Timed out awaiting transaction checkpoint");}catch(InterruptedException e){Thread.currentThread().interrupt();throw new AssertionError(e);}}
    /** Both real connections read the same committed version, then first holds its writes uncommitted.
     * The second must wait for that commit before its service outcome is observable. */
    Outcome orderedTransactions(Long id,Long firstActor,Long secondActor,Consumer<HrmPayrollRequirementDO> first,Consumer<HrmPayrollRequirementDO> second) throws Exception {
        ExecutorService pool=Executors.newFixedThreadPool(2);CyclicBarrier bothRead=new CyclicBarrier(2);CountDownLatch firstWrote=new CountDownLatch(1),secondAttempted=new CountDownLatch(1),commitFirst=new CountDownLatch(1);AtomicReference<Connection> firstConnection=new AtomicReference<>(),secondConnection=new AtomicReference<>();AtomicReference<Integer> firstVersion=new AtomicReference<>(),secondVersion=new AtomicReference<>();
        Future<Outcome> a=null,b=null;
        try {
            a=pool.submit(()->worker(id,firstActor,bothRead,firstConnection,firstVersion,row->{first.accept(row);firstWrote.countDown();await(commitFirst);}));
            b=pool.submit(()->worker(id,secondActor,bothRead,secondConnection,secondVersion,row->{await(firstWrote);secondAttempted.countDown();second.accept(row);}));
            await(firstWrote);await(secondAttempted);assertNotSame(firstConnection.get(),secondConnection.get());assertEquals(firstVersion.get(),secondVersion.get());
            Future<Outcome> pending=b;assertThrows(TimeoutException.class,()->pending.get(200,TimeUnit.MILLISECONDS),"Second operation must wait for the real database lock");
            commitFirst.countDown();assertNull(a.get(12,TimeUnit.SECONDS).code);return b.get(12,TimeUnit.SECONDS);
        } finally {commitFirst.countDown();pool.shutdownNow();assertTrue(pool.awaitTermination(12,TimeUnit.SECONDS),"Workers must stop before SQL cleanup");}
    }
    Outcome worker(Long id,Long actor,CyclicBarrier bothRead,AtomicReference<Connection> connection,AtomicReference<Integer> version,Consumer<HrmPayrollRequirementDO> operation) {
        identity(1L,actor);
        try {
            transaction().execute(status->{
                Connection c=DataSourceUtils.getConnection(dataSource);connection.set(c);try{assertFalse(c.getAutoCommit());}catch(java.sql.SQLException e){throw new AssertionError(e);}
                HrmPayrollRequirementDO row=reqs.selectById(id);version.set(row.getVersion());try{bothRead.await(8,TimeUnit.SECONDS);}catch(Exception e){throw new AssertionError(e);}operation.accept(row);return null;
            });return new Outcome(null);
        }catch(ServiceException expected){return new Outcome(expected.getCode());}finally{clearIdentity();}
    }
    @Test void twoTransactionsRejectStaleSaveThenPersistManualMerge() throws Exception {
        Long id=requirement(false);int version=reqs.selectById(id).getVersion();
        Outcome rejected=orderedTransactions(id,1L,2L,row->{HrmPayrollRequirementSaveReqVO q=draft(row);q.setDescription("TEST A saved description");requirements.save(q,1L);},row->{HrmPayrollRequirementSaveReqVO q=draft(row);q.setEvidence("TEST B recoverable input");requirements.save(q,2L);});
        assertEquals(PAYROLL_VERSION_CONFLICT.getCode(),rejected.code);assertEquals("TEST A saved description",reqs.selectById(id).getDescription());assertEquals(version+1,reqs.selectById(id).getVersion());assertNotEquals("TEST B recoverable input",reqs.selectById(id).getEvidence());
        transaction().execute(status->{HrmPayrollRequirementSaveReqVO merged=draft(reqs.selectById(id));merged.setEvidence("TEST B recoverable input");requirements.save(merged,2L);return null;});
        transaction().execute(status->{HrmPayrollRequirementDO stable=reqs.selectById(id);assertEquals("TEST A saved description",stable.getDescription());assertEquals("TEST B recoverable input",stable.getEvidence());assertEquals(version+2,stable.getVersion());assertNull(stable.getConfirmedBy());return null;});
    }
    @Test void scopeCommitBeforeOldConfirmationCannotAttachOldSignature() throws Exception {
        Long id=requirement(false);Outcome rejected=orderedTransactions(id,1L,2L,row->projectService.scope(project,1,scope.replace("2026-10-01","2026-10-16"),"TEST scope commits before confirmation",1L),row->requirements.confirm(id,row.getVersion(),2L));
        assertEquals(PAYROLL_PROJECT_SCOPE_STALE.getCode(),rejected.code);assertEquals(2,reqs.selectById(id).getProjectScopeVersion());assertEquals("SCOPE_MISMATCH",reqs.selectById(id).getScopeState());assertNull(reqs.selectById(id).getConfirmedTupleJson());assertFalse(history(id).stream().anyMatch(h->JsonUtils.parseObject(h.getSnapshot(),HrmPayrollRequirementDO.class).getConfirmedBy()!=null));
    }
    @Test void confirmationCommitBeforeScopeKeepsOnlyHistoricalOldScopeSignature() throws Exception {
        Long id=requirement(false);Outcome revised=orderedTransactions(id,2L,1L,row->requirements.confirm(id,row.getVersion(),2L),row->projectService.scope(project,1,upper(),"TEST confirmation commits before scope",1L));
        assertNull(revised.code);HrmPayrollRequirementDO current=reqs.selectById(id);assertEquals(2,current.getProjectScopeVersion());assertEquals("REVIEWING",current.getStatus());assertNull(current.getConfirmedBy());assertNull(current.getConfirmedTupleJson());
        List<HrmPayrollRequirementDO> signed=new ArrayList<>();for(HrmPayrollRequirementHistoryDO h:history(id)){HrmPayrollRequirementDO snapshot=JsonUtils.parseObject(h.getSnapshot(),HrmPayrollRequirementDO.class);if(snapshot.getConfirmedBy()!=null)signed.add(snapshot);}assertEquals(1,signed.size());assertEquals(1,signed.get(0).getConfirmedScopeVersion());assertEquals(2L,signed.get(0).getConfirmedBy());
    }
    @ParameterizedTest @ValueSource(strings={"SOURCE","MAPPING"})
    void dependencyCommitBeforeOldConfirmationRejectsStaleRequirement(String kind) throws Exception {
        Long id=requirement(true),source=source(),relation=link(source,id);confirm(id);String tuple=reqs.selectById(id).getConfirmedTupleJson();
        Outcome rejected=orderedTransactions(id,1L,2L,row->{if("SOURCE".equals(kind))changeSource(source,s->s.setSpecification("TEST contract V2"));else changeMapping(relation);},row->requirements.confirm(id,row.getVersion(),2L));
        assertEquals(PAYROLL_VERSION_CONFLICT.getCode(),rejected.code);assertEquals("REVIEWING",reqs.selectById(id).getStatus());assertNull(reqs.selectById(id).getConfirmedTupleJson());oldSignatureRemains(id,tuple);
    }
}
