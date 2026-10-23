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
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import javax.sql.DataSource;
import java.sql.Connection;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.test.util.ReflectionTestUtils;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.annotation.Resource;
import java.time.*;
import java.util.*;
import java.util.function.Consumer;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;
import static cn.iocoder.yudao.module.hrm.service.payroll.HrmPayrollGovernanceStore.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** T07 governance contracts on real H2 transactions, tenant SQL filtering, controlled roots and decisions.
 * Only external user/permission APIs and the Store system-user fact query are stubbed.
 * Tenant locking and all business services execute normally. This is not browser/session or MySQL acceptance. */
@Import({HrmPayrollT07GovernanceConsistencyTest.TenantSql.class,HrmPayrollGovernanceStore.class,
    HrmPayrollGovernanceRootService.class,HrmPayrollGovernanceDecisionService.class,
    HrmPayrollRequirementAccessService.class,HrmPayrollRequirementProjectService.class,
    HrmPayrollRequirementServiceImpl.class,HrmPayrollWorkbenchService.class,
    HrmPayrollReviewService.class,HrmPayrollGovernanceReleaseService.class})
@Timeout(40)
class HrmPayrollT07GovernanceConsistencyTest extends BaseDbUnitTest {
    @TestConfiguration
    static class TenantSql {
        // Same handler and insertion order as the production tenant configuration; no web/cache beans.
        @Bean TenantLineInnerInterceptor t07TenantSql(MybatisPlusInterceptor interceptor) {
            TenantLineInnerInterceptor tenant=new TenantLineInnerInterceptor(new TenantDatabaseInterceptor(new TenantProperties()));
            MyBatisUtils.addInterceptor(interceptor,tenant,0);return tenant;
        }
    }
    @Resource TenantLineInnerInterceptor t07TenantSql;
    @Resource MybatisPlusInterceptor sqlInterceptors;
    @SpyBean HrmPayrollGovernanceStore store;
    @Resource HrmPayrollReviewService review;
    @Resource HrmPayrollGovernanceReleaseService releases;
    @Resource PlatformTransactionManager transactionManager;
    @Resource DataSource dataSource;
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
    @MockBean cn.iocoder.yudao.module.system.api.user.AdminUserApi users;
    @MockBean cn.iocoder.yudao.module.system.api.permission.PermissionApi permissions;
    Long project,anchor,reviewerRoot;
    final String scope="{\"entities\":[101],\"organizations\":[201],\"workerTypes\":[\"TEST\"],\"scenarios\":[\"TEST\"],\"from\":\"2026-10-01\",\"to\":\"2026-10-31\",\"explanation\":\"TEST T07 fixed scope\"}";
    String upper(){return scope.replace("[101]","[101,102]").replace("2026-10-31","2026-12-31");}
    void identity(long tenant,long actor){
        TenantContextHolder.setTenantId(tenant);
        SecurityFrameworkUtils.setLoginUser(new LoginUser().setId(actor).setUserType(2),new MockHttpServletRequest());
    }
    void clearIdentity(){TenantContextHolder.clear();SecurityContextHolder.clearContext();}
    @BeforeEach void setup(){
        identity(1L,1L);clock("2026-10-23T12:00:00");assertSame(t07TenantSql,sqlInterceptors.getInterceptors().get(0));
        when(permissions.hasAnyPermissions(anyLong(),anyString())).thenReturn(true);
        for(long user=1;user<=5;user++)when(users.getUser(user)).thenReturn(new cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO().setId(user).setStatus(0).setNickname("TEST T07 user "+user));
        when(permissions.hasAnyPermissions(eq(5L),anyString())).thenAnswer(call->Arrays.asList("hrm:payroll:requirement:query","hrm:payroll:requirement:update").contains(call.getArgument(1)));
        List<Map<String,Object>> facts=new ArrayList<>();for(long user=1;user<=5;user++)facts.add(map("id",user,"nickname","TEST T07 user "+user,"status",0));
        // External system-user fact query boundary only: all governance locking/validation stays real.
        doReturn(facts).when(store).userFacts();
        store.insert(0L,"LOCK","TENANT",map());
        String body="TEST independently registered T07 root evidence; not formal approval";
        // Custodian 1 and signer 4 are independent of the business reviewer 2.
        anchor=store.insert(0L,"ANCHOR","TEST-T07-ANCHOR",map("state","REGISTERED","mode","TEST","custodianId",1L,"signerId",4L,"actions",new ArrayList<>(HrmPayrollRequirementAccessService.ACTIONS),"scopeJson",upper(),"validFrom","2026-01-01","validTo","2027-12-31","body",body,"digest",bodyDigest(body),"materialVersion","TEST-T07-V1")).getId();
        project=newProject("TEST_T07");
        root(project,1L,new ArrayList<>(HrmPayrollRequirementAccessService.ACTIONS));
        reviewerRoot=root(project,2L,new ArrayList<>(HrmPayrollRequirementAccessService.ACTIONS));
        root(project,3L,new ArrayList<>(HrmPayrollRequirementAccessService.ACTIONS));
        root(project,4L,new ArrayList<>(HrmPayrollRequirementAccessService.ACTIONS));
        root(project,5L,Arrays.asList("READ","EDIT"));policy();
    }
    @AfterEach void cleanup(){ReflectionTestUtils.setField(decisions,"clock",Clock.system(ZONE));clearIdentity();}
    Long newProject(String key){
        HrmPayrollRequirementProjectDO p=new HrmPayrollRequirementProjectDO().setProjectKey(key).setName("TEST T07 project").setOwnerId(1L).setProjectScopeVersion(1).setMetadataVersion(1).setCustomSequence(0L).setScopeJson(scope).setArchived(false);projects.insert(p);return p.getId();
    }
    HrmPayrollGovernanceVO.Action action(Long ownerProject,Long id,String kind,String operation){
        HrmPayrollGovernanceVO.Action c=new HrmPayrollGovernanceVO.Action();c.setProjectId(ownerProject);c.setId(id);c.setVersion(store.record(id,kind).getVersion());c.setAction(operation);c.setReason("TEST independent T07 action");return c;
    }
    Long root(Long ownerProject,Long user,List<String> actions){
        HrmPayrollGovernanceVO.Root c=new HrmPayrollGovernanceVO.Root();c.setProjectId(ownerProject);c.setAnchorId(anchor);c.setUserId(user);c.setActions(actions);c.setDelegableActions(actions);c.setScopeJson(upper());c.setValidFrom("2026-01-01");c.setValidTo("2027-12-31");c.setMaterialVersion("TEST-T07-V1");c.setBody("TEST controlled T07 user "+user);c.setReason("TEST independent grant");
        Long id=roots.register(c,1L);roots.action(action(ownerProject,id,"ROOT","SIGN"),4L);return id;
    }
    void policy(){
        for(List<String> qs:Arrays.asList(Arrays.asList("Q-01","Q-02","Q-07","Q-10"),Collections.singletonList("Q-04"),Collections.singletonList("Q-05"))){
            HrmPayrollGovernanceVO.Decision c=new HrmPayrollGovernanceVO.Decision();c.setProjectId(project);c.setProjectScopeVersion(projects.selectById(project).getProjectScopeVersion());c.setTitle("TEST T07 "+qs);c.setQuestions(qs);c.setClauses("TEST PRD v0.4");c.setClauseVersion("TEST T07 V1");c.setBody("TEST controlled T07 decision "+qs);c.setScopeJson(projects.selectById(project).getScopeJson());c.setValidFrom("2026-01-01");c.setValidTo("2027-12-31");c.setApproverId(4L);c.setApprovalConditions("TEST independently reviewed");c.setSemanticChange(false);Map<String,String> other=new LinkedHashMap<>();for(String q:Arrays.asList("Q-03","Q-06","Q-08","Q-09"))other.put(q,"ADVISORY");c.setOtherQuestions(other);c.setReason("TEST controlled draft");
            Long id=decisions.draft(c,1L);decisions.action(action(project,id,"DECISION","SUBMIT"),1L);decisions.action(action(project,id,"DECISION","APPROVE"),4L);
        }
    }
    Long requirement(boolean applicable){
        HrmPayrollRequirementSaveReqVO q=new HrmPayrollRequirementSaveReqVO();q.setProjectId(project);q.setProjectScopeVersion(projects.selectById(project).getProjectScopeVersion());q.setCode("CUSTOM-0000");q.setModule("COL");q.setDescription("TEST original requirement");q.setPriority(2);q.setStatus("PENDING");q.setReadiness("MISSING");q.setScopeMode("SUBSET");q.setScopeJson(scope);q.setRuleVersion("TEST RULE V1");q.setSourceApplicability(applicable?"APPLICABLE":"NOT_APPLICABLE");q.setApplicabilityReason("TEST navigation-only discussion consumes no business data");q.setAcceptance("TEST independently verifiable expected result");q.setEvidence("TEST T07 evidence, not formal approval");
        q.setBusinessContextJson(JsonUtils.toJsonString(map("role","TEST HR","scenario","TEST monthly review","goal","TEST collect","expected","TEST expected","normalCase","TEST normal","exceptionCase","TEST missing","calculation","TEST rule","unit","TEST hour","precision","TEST 2 decimals","validity","TEST 2026-10","ruleException","TEST return for review")));
        Long id=requirements.save(q,1L);requirements.assign(id,reqs.selectById(id).getVersion(),2L,1L);return id;
    }
    HrmPayrollRequirementSaveReqVO draft(HrmPayrollRequirementDO row){return JsonUtils.parseObject(JsonUtils.toJsonString(row),HrmPayrollRequirementSaveReqVO.class);}
    <T extends Context>T context(T c,Long id){c.setProjectId(project);c.setProjectScopeVersion(projects.selectById(project).getProjectScopeVersion());c.setId(id);c.setVersion(id==null?null:records.selectById(id).getVersion());c.setReason("TEST T07 mutation");return c;}
    Long source(){
        Source s=new Source();s.setName("TEST source");s.setModule("COL");s.setScopeJson(scope);s.setOwnerId(1L);s.setTechnicalContact("TEST IT");s.setAcquisition("TEST file");s.setSpecification("TEST contract V1");s.setUniqueKey("employee,period");s.setEmployeeMatch("TEST employee ID");s.setFrequency("monthly");s.setCutoff("month end");s.setVolume("TEST two rows");s.setQualityRule("TEST QS");Field f=new Field();f.setName("hours");f.setMeaning("TEST work duration");f.setType("decimal");f.setUnit("hour");f.setRequired(true);s.setFields(Collections.singletonList(f));SourceSave c=context(new SourceSave(),null);c.setSource(s);return workbench.saveSource(c,1L);
    }
    Long link(Long source,Long requirement){
        Mapping m=new Mapping();m.setSourceField("hours");m.setPageField("workingHours");m.setMeaning("TEST work duration");m.setType("decimal");m.setUnit("hour");m.setRequired(true);m.setTransform("TEST identity");m.setNullRule("reject");m.setDuplicateRule("reject");m.setPeriodRule("same month");m.setExceptionRule("TEST manual correction");Relation r=new Relation();r.setSourceId(source);r.setRequirementId(requirement);r.setFields(Collections.singletonList(m));r.setConsumptionRule("TEST QC");RelationSave c=context(new RelationSave(),null);c.setRelation(r);return workbench.saveRelation(c,1L);
    }
    Source sourceData(Long id){return JsonUtils.parseObject(records.selectById(id).getDataJson(),Source.class);}
    void changeSource(Long id,Consumer<Source> change){Source s=sourceData(id);change.accept(s);SourceSave c=context(new SourceSave(),id);c.setSource(s);workbench.saveSource(c,1L);}
    void decision(Long id,String state){
        cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollReviewVO.Decision c=new cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollReviewVO.Decision();c.setProjectId(project);c.setProjectScopeVersion(1);c.setId(id);c.setVersion(reqs.selectById(id).getVersion());c.setDecision(state);c.setReason("TEST T07 explicit "+state+" scope basis");c.setResponsibleId(1L);c.setReevaluate("TEST independently re-evaluate next scope review");if("REMOVED".equals(state))c.setRemovalKind("OUT_OF_SCOPE");review.decide(c,1L);
    }
    Long included(){Long req=requirement(false);decision(req,"INCLUDED");requirements.confirm(req,reqs.selectById(req).getVersion(),2L);assertTrue(allowedGate());return req;}
    @SuppressWarnings("unchecked") Map<String,Object> statistics(){return review.statistics(project,1L,"READ",null);}
    boolean allowedGate(){return Boolean.TRUE.equals(releases.precheck(project,1L).get("allowed"));}
    boolean baselineIssueBlock(Long issue){
        Map<String,Object> gate=releases.precheck(project,1L);for(Object value:(List<?>)gate.get("blocks")){Map<?,?> block=(Map<?,?>)value;if("BASELINE_ISSUES_OPEN".equals(block.get("code")))for(Object id:(Collection<?>)block.get("issueIds"))if(String.valueOf(issue).equals(String.valueOf(id)))return true;}return false;
    }
    HrmPayrollGovernanceVO.Publish publication(){
        Map<String,Object> gate=releases.precheck(project,1L);HrmPayrollGovernanceVO.Publish c=new HrmPayrollGovernanceVO.Publish();c.setProjectId(project);c.setProjectScopeVersion(1);c.setDraft(false);c.setReason("TEST T07 dependency-consistent specification baseline, not formal approval");c.setRequestKey(UUID.randomUUID().toString());c.setDependencyToken(String.valueOf(gate.get("dependencyToken")));return c;
    }
    @SuppressWarnings("unchecked") Map<String,Object> snapshot(Long release){return (Map<String,Object>)store.data(store.record(release,"RELEASE")).get("snapshot");}
    static final class Outcome {final Object value;final Integer code;Outcome(Object value,Integer code){this.value=value;this.code=code;}}
    TransactionTemplate transaction(){TransactionTemplate tx=new TransactionTemplate(transactionManager);tx.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);return tx;}
    void await(CountDownLatch latch){try{assertTrue(latch.await(8,TimeUnit.SECONDS),"Timed out awaiting T07 transaction checkpoint");}catch(InterruptedException e){Thread.currentThread().interrupt();throw new AssertionError(e);}}
    Outcome worker(long actor,CyclicBarrier barrier,AtomicReference<Connection> connection,Callable<Object> operation){
        identity(1L,actor);try{return transaction().execute(status->{Connection c=DataSourceUtils.getConnection(dataSource);connection.set(c);try{assertFalse(c.getAutoCommit());barrier.await(8,TimeUnit.SECONDS);return new Outcome(operation.call(),null);}catch(RuntimeException e){throw e;}catch(Exception e){throw new AssertionError(e);}});}catch(ServiceException e){return new Outcome(null,e.getCode());}finally{clearIdentity();}
    }
    Outcome[] orderedTransactions(long firstActor,long secondActor,Callable<Object> first,Callable<Object> second) throws Exception {
        ExecutorService pool=Executors.newFixedThreadPool(2);CyclicBarrier barrier=new CyclicBarrier(2);CountDownLatch firstWrote=new CountDownLatch(1),secondAttempted=new CountDownLatch(1),commitFirst=new CountDownLatch(1);AtomicReference<Connection> ca=new AtomicReference<>(),cb=new AtomicReference<>();
        try {
            Future<Outcome> a=pool.submit(()->worker(firstActor,barrier,ca,()->{Object result=first.call();firstWrote.countDown();await(commitFirst);return result;}));
            Future<Outcome> b=pool.submit(()->worker(secondActor,barrier,cb,()->{await(firstWrote);secondAttempted.countDown();return second.call();}));
            await(firstWrote);await(secondAttempted);assertNotSame(ca.get(),cb.get());assertThrows(TimeoutException.class,()->b.get(200,TimeUnit.MILLISECONDS),"Second operation must wait for the actual tenant lock");commitFirst.countDown();Outcome firstResult=a.get(12,TimeUnit.SECONDS),secondResult=b.get(12,TimeUnit.SECONDS);assertNull(firstResult.code);return new Outcome[]{firstResult,secondResult};
        }finally{commitFirst.countDown();pool.shutdownNow();assertTrue(pool.awaitTermination(12,TimeUnit.SECONDS),"T07 workers must stop before SQL cleanup");}
    }
    void clock(String shanghai){ReflectionTestUtils.setField(decisions,"clock",Clock.fixed(LocalDateTime.parse(shanghai).atZone(ZONE).toInstant(),ZONE));}
    Long currentDecision(String question){return id(((Map<?,?>)decisions.current(project,scope,Collections.singletonList(question)).get(0)).get("recordId"));}
    HrmPayrollGovernanceVO.Decision replacement(Long previous,String body){
        HrmPayrollGovernanceVO.Decision c=JsonUtils.parseObject(JsonUtils.toJsonString(store.data(store.record(previous,"DECISION"))),HrmPayrollGovernanceVO.Decision.class);c.setPreviousId(previous);c.setReason("TEST T07 replacement proposal");if(body!=null)c.setBody(body);return c;
    }
    Long submit(HrmPayrollGovernanceVO.Decision c){Long id=decisions.draft(c,1L);decisions.action(action(project,id,"DECISION","SUBMIT"),1L);return id;}
    Long approve(HrmPayrollGovernanceVO.Decision c){Long id=submit(c);decisions.action(action(project,id,"DECISION","APPROVE"),4L);return id;}
    String state(){return JsonUtils.toJsonString(map("project",projects.selectById(project),"requirements",reqs.selectList(),"history",histories.selectList(),"records",records.selectList(),"governance",store.all(null,"DECISION"),"roots",store.all(null,"ROOT"),"grants",grants.selectList(),"events",store.all(null,"EVENT"),"releases",store.all(null,"RELEASE")));}
    void publicationDenied(HrmPayrollGovernanceVO.Publish c,cn.iocoder.yudao.framework.common.exception.ErrorCode code){String before=state();ServiceException error=assertThrows(ServiceException.class,()->releases.publish(c,1L));assertEquals(code.getCode(),error.getCode());assertEquals(before,state());}
    Map<?,?> block(String code){for(Object raw:(List<?>)releases.precheck(project,1L).get("blocks")){Map<?,?> value=(Map<?,?>)raw;if(code.equals(value.get("code")))return value;}fail("Missing precheck reason "+code);return null;}
    Map<?,?> questionBlock(String code,String question){for(Object raw:(List<?>)releases.precheck(project,1L).get("blocks")){Map<?,?> value=(Map<?,?>)raw;if(code.equals(value.get("code"))&&question.equals(value.get("question")))return value;}fail("Missing precheck reason "+code+"/"+question);return null;}
    @SuppressWarnings("unchecked") List<Map<String,Object>> frozenRequirements(Map<String,Object> snapshot){return (List<Map<String,Object>>)(List<?>)snapshot.get("requirements");}
    Map<String,Object> frozenRequirement(Map<String,Object> snapshot,Long requirement){return frozenRequirements(snapshot).stream().filter(r->String.valueOf(requirement).equals(String.valueOf(r.get("id")))).findFirst().get();}
    @SuppressWarnings("unchecked") List<Map<String,Object>> frozenDecisions(Map<String,Object> snapshot){return (List<Map<String,Object>>)(List<?>)snapshot.get("decisionVersions");}
    @SuppressWarnings("unchecked") Map<String,Object> frozenDecision(Map<String,Object> snapshot,Long record){return (Map<String,Object>)frozenDecisions(snapshot).stream().filter(d->String.valueOf(record).equals(String.valueOf(d.get("recordId")))).findFirst().get().get("decision");}
    boolean used(Map<String,Object> snapshot,Long decision){Map<?,?> gate=(Map<?,?>)snapshot.get("gate");return ((List<?>)gate.get("decisions")).stream().map(x->(Map<?,?>)x).anyMatch(x->String.valueOf(decision).equals(String.valueOf(x.get("recordId"))));}
    void assertFrozenApproved(Map<String,Object> snapshot,Long record,String body){Map<String,Object> frozen=frozenDecision(snapshot,record);assertEquals("APPROVED",frozen.get("state"));assertEquals(body,frozen.get("body"));assertEquals("4",String.valueOf(frozen.get("approvedBy")));assertNotNull(frozen.get("approvalAuthority"));assertNotNull(frozen.get("approvedAt"));assertEquals(bodyDigest(body),frozen.get("digest"));assertTrue(used(snapshot,record));}

    Object frozenClassification(Map<String,Object> snapshot,String question){return ((Map<?,?>)((Map<?,?>)snapshot.get("gate")).get("otherQuestionClassification")).get(question);}

    @ParameterizedTest @ValueSource(strings={"Q-04","Q-05"})
    void draftAndSubmissionKeepOldCurrentPolicySignatureAndPublicationGate(String question){
        Long req=included(),old=currentDecision(question);String tuple=reqs.selectById(req).getConfirmedTupleJson(),oldBody=String.valueOf(store.data(store.record(old,"DECISION")).get("body"));HrmPayrollGovernanceVO.Decision c=replacement(old,"TEST T07 draft not effective "+question);Long pending=decisions.draft(c,1L);
        assertEquals(old,currentDecision(question));assertEquals(tuple,reqs.selectById(req).getConfirmedTupleJson());assertTrue(allowedGate());Long draftRelease=releases.publish(publication(),1L);assertFrozenApproved(snapshot(draftRelease),old,oldBody);assertFalse(used(snapshot(draftRelease),pending));assertEquals("DRAFT",frozenDecision(snapshot(draftRelease),pending).get("state"));
        decisions.action(action(project,pending,"DECISION","SUBMIT"),1L);assertEquals(old,currentDecision(question));assertEquals(tuple,reqs.selectById(req).getConfirmedTupleJson());assertTrue(allowedGate());Long pendingRelease=releases.publish(publication(),1L);assertFrozenApproved(snapshot(pendingRelease),old,oldBody);assertFalse(used(snapshot(pendingRelease),pending));assertEquals("PENDING_APPROVAL",frozenDecision(snapshot(pendingRelease),pending).get("state"));
    }
    @Test void futureReplacementUsesExactEffectiveAndExpiryBoundariesInCurrentCoverageAndGate(){
        included();Long old=currentDecision("Q-04");HrmPayrollGovernanceVO.Decision c=replacement(old,"TEST T07 future matrix");c.setValidFrom("2026-10-24T09:00:00");c.setValidTo("2026-10-25T09:00:00");Long future=approve(c);
        clock("2026-10-24T08:59:59.999999999");assertEquals(old,currentDecision("Q-04"));assertTrue(allowedGate());Long before=releases.publish(publication(),1L);assertTrue(used(snapshot(before),old));assertFalse(used(snapshot(before),future));
        clock("2026-10-24T09:00:00");assertEquals(future,currentDecision("Q-04"));assertTrue(allowedGate());Long atStart=releases.publish(publication(),1L);assertFrozenApproved(snapshot(atStart),future,c.getBody());
        clock("2026-10-25T09:00:00");assertEquals(future,currentDecision("Q-04"));assertTrue(allowedGate());clock("2026-10-25T09:00:00.000000001");assertServiceException(()->currentDecision("Q-04"),PAYROLL_REVIEW_INCOMPLETE);block("CURRENT_DECISION_COVERAGE_REQUIRED");publicationDenied(publication(),PAYROLL_REVIEW_INCOMPLETE);
    }
    @Test void revokingFutureReplacementRestoresOldCoverageWithoutResigningRequirement(){
        Long req=included(),old=currentDecision("Q-04");String tuple=reqs.selectById(req).getConfirmedTupleJson();HrmPayrollGovernanceVO.Decision c=replacement(old,"TEST T07 cancelled future matrix");c.setValidFrom("2026-11-01T09:00:00");Long future=approve(c);decisions.action(action(project,future,"DECISION","REVOKE"),4L);
        Map<String,Object> previous=store.data(store.record(old,"DECISION"));assertFalse(previous.containsKey("replacementId"));assertFalse(previous.containsKey("replacementFrom"));clock("2026-11-02T09:00:00");assertEquals(old,currentDecision("Q-04"));assertEquals(tuple,reqs.selectById(req).getConfirmedTupleJson());assertTrue(allowedGate());assertEquals("REVOKED",store.data(store.record(future,"DECISION")).get("state"));
    }
    @Test void revokedCurrentDecisionCannotServeAsCurrentPublicationBasis(){
        included();Long matrix=currentDecision("Q-04");decisions.action(action(project,matrix,"DECISION","REVOKE"),4L);assertServiceException(()->currentDecision("Q-04"),PAYROLL_REVIEW_INCOMPLETE);block("CURRENT_DECISION_COVERAGE_REQUIRED");publicationDenied(publication(),PAYROLL_REVIEW_INCOMPLETE);
    }
    @ParameterizedTest @ValueSource(strings={"PENDING_ROOT","REVOKED_ROOT","EXPIRED_ROOT","OUTSIDE_SCOPE"})
    void independentApproverNeedsCurrentSignedRootAndMatchingScope(String variant){
        included();HrmPayrollGovernanceVO.Decision proposal=replacement(currentDecision("Q-04"),"TEST T07 approver authority boundary");proposal.setApproverId(3L);Long pending=submit(proposal);
        // Use user3 as designated approver; keep independent anchor signer4 active.
        HrmPayrollRequirementProjectGrantDO old=grants.selectList().stream().filter(g->Objects.equals(g.getProjectId(),project)&&Objects.equals(g.getUserId(),3L)).findFirst().get();roots.action(action(project,old.getRootRecordId(),"ROOT","REVOKE"),4L);
        HrmPayrollGovernanceVO.Root root=new HrmPayrollGovernanceVO.Root();root.setProjectId(project);root.setAnchorId(anchor);root.setUserId(3L);root.setActions(new ArrayList<>(HrmPayrollRequirementAccessService.ACTIONS));root.setDelegableActions(new ArrayList<>(HrmPayrollRequirementAccessService.ACTIONS));root.setScopeJson("OUTSIDE_SCOPE".equals(variant)?scope.replace("2026-10-01","2026-10-16"):upper());root.setValidFrom("2026-01-01");root.setValidTo("EXPIRED_ROOT".equals(variant)?LocalDate.now(ZONE).minusDays(1).toString():"2027-12-31");root.setBody("TEST T07 root boundary "+variant);root.setMaterialVersion("TEST-T07-BOUNDARY");root.setReason("TEST independent root boundary");Long rootId=roots.register(root,1L);
        if(!"PENDING_ROOT".equals(variant))roots.action(action(project,rootId,"ROOT","SIGN"),4L);if("REVOKED_ROOT".equals(variant))roots.action(action(project,rootId,"ROOT","REVOKE"),4L);
        String before=state();ServiceException error=assertThrows(ServiceException.class,()->decisions.action(action(project,pending,"DECISION","APPROVE"),3L));assertEquals(PAYROLL_PROJECT_DENIED.getCode(),error.getCode());assertEquals(before,state());assertEquals("PENDING_APPROVAL",store.data(store.record(pending,"DECISION")).get("state"));
    }
    @ParameterizedTest @ValueSource(strings={"NO_SCOPE","REQUIRED_POLICY","POLICY_SCOPE","CONFIRMATION","FULL_VISIBILITY","EXCLUSION_REASON","EXCLUSION_OWNER","EXCLUSION_REEVALUATE","EMPTY_I"})
    void eachPublicationGateHasItsOwnControlledReasonAndNoReleaseSideEffects(String condition){
        Long req=included();String expected;
        switch(condition){
            case "NO_SCOPE":projects.update(null,new LambdaUpdateWrapper<HrmPayrollRequirementProjectDO>().eq(HrmPayrollRequirementProjectDO::getId,project).set(HrmPayrollRequirementProjectDO::getScopeJson,null).set(HrmPayrollRequirementProjectDO::getProjectScopeVersion,null));expected="SCOPE_MISMATCH";break;
            case "REQUIRED_POLICY":decisions.action(action(project,currentDecision("Q-04"),"DECISION","REVOKE"),4L);expected="CURRENT_DECISION_COVERAGE_REQUIRED";break;
            case "POLICY_SCOPE":Long old=currentDecision("Q-04");HrmPayrollGovernanceVO.Decision c=replacement(old,null);c.setScopeJson(scope.replace("2026-10-01","2026-10-16"));approve(c);expected="CURRENT_DECISION_COVERAGE_REQUIRED";break;
            case "CONFIRMATION":reqs.update(null,new LambdaUpdateWrapper<HrmPayrollRequirementDO>().eq(HrmPayrollRequirementDO::getId,req).set(HrmPayrollRequirementDO::getConfirmedTupleJson,null));expected="CURRENT_CONFIRMATION_REQUIRED";break;
            case "FULL_VISIBILITY":HrmPayrollRequirementProjectGrantDO grant=grants.selectList().stream().filter(g->Objects.equals(g.getProjectId(),project)&&Objects.equals(g.getUserId(),1L)).findFirst().get();grant.setFullProject(false);grant.setRequirementIdsJson("["+req+"]");grants.updateById(grant);expected="FULL_CLOSURE_READ_REQUIRED";break;
            case "EMPTY_I":decision(req,"DEFERRED");expected="EMPTY_INCLUDED_SET";break;
            default:Long excluded=requirement(false);decision(excluded,"DEFERRED");HrmPayrollRequirementDO row=reqs.selectById(excluded);Map<String,Object> basis=JsonUtils.parseObject(row.getScopeDecisionJson(),Map.class);basis.remove("EXCLUSION_REASON".equals(condition)?"reason":"EXCLUSION_OWNER".equals(condition)?"responsibleId":"reevaluate");row.setScopeDecisionJson(JsonUtils.toJsonString(basis));reqs.updateById(row);expected="EXCLUSION_BASIS_INCOMPLETE";
        }
        block(expected);HrmPayrollGovernanceVO.Publish request=publication();publicationDenied(request,Arrays.asList("FULL_VISIBILITY","NO_SCOPE").contains(condition)?PAYROLL_PROJECT_DENIED:PAYROLL_REVIEW_INCOMPLETE);assertTrue(store.all(project,"RELEASE").isEmpty());
    }
    @ParameterizedTest @ValueSource(strings={"Q-03","Q-06","Q-08","Q-09"})
    void unresolvedQuestionClassificationMustBeUniqueAndPermittedWhileResolvedQuestionUsesDecision(String question){
        included();classify(question,"MISSING");questionBlock("UNRESOLVED_QUESTION_CLASSIFICATION",question);publicationDenied(publication(),PAYROLL_REVIEW_INCOMPLETE);
        classify(question,"CONFLICT");questionBlock("UNRESOLVED_QUESTION_CLASSIFICATION",question);publicationDenied(publication(),PAYROLL_REVIEW_INCOMPLETE);
        classify(question,"BASELINE_BLOCK");questionBlock("UNRESOLVED_BASELINE_QUESTION",question);publicationDenied(publication(),PAYROLL_REVIEW_INCOMPLETE);
        for(String allowed:Arrays.asList("DATA_READINESS","ADVISORY")){classify(question,allowed);assertTrue(allowedGate());Long release=releases.publish(publication(),1L);assertEquals(allowed,frozenClassification(snapshot(release),question));}
        HrmPayrollGovernanceVO.Decision resolved=replacement(currentDecision("Q-04"),"TEST T07 resolved "+question);resolved.setPreviousId(null);resolved.setQuestions(Collections.singletonList(question));Long id=approve(resolved);assertTrue(allowedGate());Long release=releases.publish(publication(),1L);assertTrue(used(snapshot(release),id));assertEquals("RESOLVED",frozenClassification(snapshot(release),question));
    }
    void classify(String question,String classification){
        List<Long> selected=new ArrayList<>();for(Object raw:decisions.current(project,scope,HrmPayrollGovernanceDecisionService.REQUIRED))selected.add(id(((Map<?,?>)raw).get("recordId")));int at=0;
        for(Long previous:selected){HrmPayrollGovernanceVO.Decision c=replacement(previous,null);Map<String,String> other=new LinkedHashMap<>(c.getOtherQuestions());if("MISSING".equals(classification))other.remove(question);else other.put(question,"CONFLICT".equals(classification)?(at++==0?"ADVISORY":"DATA_READINESS"):classification);c.setOtherQuestions(other);approve(c);}
    }
    @Test void identicalConcurrentPublicationRequestCreatesOneReleaseAndOnePublicationEvent() throws Exception {
        included();HrmPayrollGovernanceVO.Publish request=publication();Outcome[] result=orderedTransactions(1L,1L,()->releases.publish(request,1L),()->releases.publish(request,1L));assertNull(result[1].code);assertEquals(result[0].value,result[1].value);assertEquals(1,store.all(project,"RELEASE").size());assertEquals(1,store.all(project,"EVENT").stream().map(store::data).filter(e->"BASELINE_PUBLISH".equals(e.get("action"))).count());
    }
    @ParameterizedTest @ValueSource(strings={"MATRIX","RULE","SCOPE","REQUIREMENT"})
    void dependencyCommitBeforeOldTokenPublishRejectsWithoutMixedSnapshot(String dependency) throws Exception {
        Long req=included();Long previous=currentDecision("RULE".equals(dependency)?"Q-05":"Q-04");HrmPayrollGovernanceVO.Decision c=replacement(previous,"TEST T07 committed "+dependency);Long pending=Arrays.asList("MATRIX","RULE").contains(dependency)?submit(c):null;
        // Draft/submission itself changes the dependency token without invalidating the old effective basis.
        HrmPayrollGovernanceVO.Publish stale=publication();Outcome[] result=orderedTransactions(4L,1L,()->{changeDependency(dependency,req,pending);return state();},()->releases.publish(stale,1L));assertEquals(PAYROLL_REVIEW_INCOMPLETE.getCode(),result[1].code);assertEquals(result[0].value,state());assertTrue(store.all(project,"RELEASE").isEmpty());
        if("MATRIX".equals(dependency)){assertTrue(allowedGate());Long release=releases.publish(publication(),1L);assertFrozenApproved(snapshot(release),pending,c.getBody());}
        else {assertEquals("REVIEWING",reqs.selectById(req).getStatus());assertNull(reqs.selectById(req).getConfirmedTupleJson());block("CURRENT_CONFIRMATION_REQUIRED");
            if("RULE".equals(dependency)||"REQUIREMENT".equals(dependency)){requirements.confirm(req,reqs.selectById(req).getVersion(),2L);assertTrue(allowedGate());Long release=releases.publish(publication(),1L);assertNotNull(frozenRequirement(snapshot(release),req).get("confirmedTupleJson"));if("RULE".equals(dependency))assertFrozenApproved(snapshot(release),pending,c.getBody());}
        }
    }
    @ParameterizedTest @ValueSource(strings={"MATRIX","RULE","SCOPE","REQUIREMENT"})
    void publicationCommitBeforeDependencyChangeKeepsOldScopeSignatureAndDecisionFacts(String dependency) throws Exception {
        Long req=included(),previous=currentDecision("RULE".equals(dependency)?"Q-05":"Q-04");String tuple=reqs.selectById(req).getConfirmedTupleJson(),oldBody=String.valueOf(store.data(store.record(previous,"DECISION")).get("body"));HrmPayrollGovernanceVO.Decision c=replacement(previous,"TEST T07 later "+dependency);Long pending=Arrays.asList("MATRIX","RULE").contains(dependency)?submit(c):null;HrmPayrollGovernanceVO.Publish request=publication();
        Outcome[] result=orderedTransactions(1L,4L,()->releases.publish(request,1L),()->{changeDependency(dependency,req,pending);return null;});assertNull(result[1].code);Long release=(Long)result[0].value;Map<String,Object> frozen=snapshot(release);String stable=JsonUtils.toJsonString(frozen);assertFrozenApproved(frozen,previous,oldBody);assertEquals(tuple,frozenRequirement(frozen,req).get("confirmedTupleJson"));assertEquals("1",String.valueOf(frozenRequirement(frozen,req).get("projectScopeVersion")));assertEquals("1",String.valueOf(((Map<?,?>)frozen.get("project")).get("projectScopeVersion")));if(pending!=null)assertFalse(used(frozen,pending));
        if(!"MATRIX".equals(dependency)){assertEquals("REVIEWING",reqs.selectById(req).getStatus());assertNull(reqs.selectById(req).getConfirmedTupleJson());}else assertEquals(tuple,reqs.selectById(req).getConfirmedTupleJson());assertEquals(stable,JsonUtils.toJsonString(snapshot(release)));
    }
    void changeDependency(String kind,Long req,Long pending){
        if(Arrays.asList("MATRIX","RULE").contains(kind))decisions.action(action(project,pending,"DECISION","APPROVE"),4L);
        else if("SCOPE".equals(kind))projectService.scope(project,1,upper(),"TEST T07 independently authorized expansion",4L);
        else {HrmPayrollRequirementSaveReqVO q=draft(reqs.selectById(req));q.setDescription("TEST T07 revised requirement contract");requirements.save(q,4L);}
    }
}
