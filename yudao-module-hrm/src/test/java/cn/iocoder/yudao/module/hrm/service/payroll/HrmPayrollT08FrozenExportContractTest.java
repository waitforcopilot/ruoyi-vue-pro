package cn.iocoder.yudao.module.hrm.service.payroll;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import java.io.*;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;
import java.nio.charset.StandardCharsets;
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

/** T08 historical export contracts on real H2 transactions, tenant SQL filtering, controlled roots and decisions.
 * Only external user/permission APIs and the Store system-user fact query are stubbed.
 * Tenant locking and all business services execute normally. This is not browser/session or MySQL acceptance. */
@Import({HrmPayrollT08FrozenExportContractTest.TenantSql.class,HrmPayrollGovernanceStore.class,
    HrmPayrollGovernanceRootService.class,HrmPayrollGovernanceDecisionService.class,
    HrmPayrollRequirementAccessService.class,HrmPayrollRequirementProjectService.class,
    HrmPayrollRequirementServiceImpl.class,HrmPayrollWorkbenchService.class,
    HrmPayrollReviewService.class,HrmPayrollGovernanceReleaseService.class,
    HrmPayrollGovernanceProjectionService.class,HrmPayrollGovernanceExportService.class})
@Timeout(40)
class HrmPayrollT08FrozenExportContractTest extends BaseDbUnitTest {
    @TestConfiguration
    static class TenantSql {
        // Same handler and insertion order as the production tenant configuration; no web/cache beans.
        @Bean TenantLineInnerInterceptor t08TenantSql(MybatisPlusInterceptor interceptor) {
            TenantLineInnerInterceptor tenant=new TenantLineInnerInterceptor(new TenantDatabaseInterceptor(new TenantProperties()));
            MyBatisUtils.addInterceptor(interceptor,tenant,0);return tenant;
        }
    }
    @Resource TenantLineInnerInterceptor t08TenantSql;
    @Resource MybatisPlusInterceptor sqlInterceptors;
    @SpyBean HrmPayrollGovernanceStore store;
    @Resource HrmPayrollReviewService review;
    @Resource HrmPayrollGovernanceReleaseService releases;
    @Resource HrmPayrollGovernanceProjectionService projection;
    @Resource HrmPayrollGovernanceExportService exports;
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
    @Resource HrmPayrollRequirementProjectEventMapper projectEvents;
    @Resource HrmPayrollWorkbenchRecordMapper records;
    @MockBean cn.iocoder.yudao.module.system.api.user.AdminUserApi users;
    @MockBean cn.iocoder.yudao.module.system.api.permission.PermissionApi permissions;
    Long project,anchor,reviewerRoot;
    final String scope="{\"entities\":[101],\"organizations\":[201],\"workerTypes\":[\"TEST\"],\"scenarios\":[\"TEST\"],\"from\":\"2026-10-01\",\"to\":\"2026-10-31\",\"explanation\":\"TEST T08 fixed scope\"}";
    String upper(){return scope.replace("[101]","[101,102]").replace("2026-10-31","2026-12-31");}
    void identity(long tenant,long actor){
        TenantContextHolder.setTenantId(tenant);
        SecurityFrameworkUtils.setLoginUser(new LoginUser().setId(actor).setUserType(2),new MockHttpServletRequest());
    }
    void clearIdentity(){TenantContextHolder.clear();SecurityContextHolder.clearContext();}
    @BeforeEach void setup(){
        identity(1L,1L);clock("2026-10-23T12:00:00");assertSame(t08TenantSql,sqlInterceptors.getInterceptors().get(0));
        when(permissions.hasAnyPermissions(anyLong(),anyString())).thenReturn(true);
        for(long user=1;user<=5;user++)when(users.getUser(user)).thenReturn(new cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO().setId(user).setStatus(0).setNickname("TEST T08 user "+user));
        when(permissions.hasAnyPermissions(eq(5L),anyString())).thenAnswer(call->Arrays.asList("hrm:payroll:requirement:query","hrm:payroll:requirement:update").contains(call.getArgument(1)));
        List<Map<String,Object>> facts=new ArrayList<>();for(long user=1;user<=5;user++)facts.add(map("id",user,"nickname","TEST T08 user "+user,"status",0));
        // External system-user fact query boundary only: all governance locking/validation stays real.
        doReturn(facts).when(store).userFacts();
        store.insert(0L,"LOCK","TENANT",map());
        String body="TEST independently registered T08 root evidence; not formal approval";
        // Custodian 1 and signer 4 are independent of the business reviewer 2.
        anchor=store.insert(0L,"ANCHOR","TEST-T08-ANCHOR",map("state","REGISTERED","mode","TEST","custodianId",1L,"signerId",4L,"actions",new ArrayList<>(HrmPayrollRequirementAccessService.ACTIONS),"scopeJson",upper(),"validFrom","2026-01-01","validTo","2027-12-31","body",body,"digest",bodyDigest(body),"materialVersion","TEST-T08-V1")).getId();
        project=newProject("TEST_T08");
        root(project,1L,new ArrayList<>(HrmPayrollRequirementAccessService.ACTIONS));
        reviewerRoot=root(project,2L,new ArrayList<>(HrmPayrollRequirementAccessService.ACTIONS));
        root(project,3L,new ArrayList<>(HrmPayrollRequirementAccessService.ACTIONS));
        root(project,4L,new ArrayList<>(HrmPayrollRequirementAccessService.ACTIONS));
        root(project,5L,Arrays.asList("READ","EDIT"));policy();
    }
    @AfterEach void cleanup(){ReflectionTestUtils.setField(decisions,"clock",Clock.system(ZONE));clearIdentity();}
    Long newProject(String key){
        HrmPayrollRequirementProjectDO p=new HrmPayrollRequirementProjectDO().setProjectKey(key).setName("TEST T08 project").setOwnerId(1L).setProjectScopeVersion(1).setMetadataVersion(1).setCustomSequence(0L).setScopeJson(scope).setArchived(false);projects.insert(p);return p.getId();
    }
    HrmPayrollGovernanceVO.Action action(Long ownerProject,Long id,String kind,String operation){
        HrmPayrollGovernanceVO.Action c=new HrmPayrollGovernanceVO.Action();c.setProjectId(ownerProject);c.setId(id);c.setVersion(store.record(id,kind).getVersion());c.setAction(operation);c.setReason("TEST independent T08 action");return c;
    }
    Long root(Long ownerProject,Long user,List<String> actions){
        HrmPayrollGovernanceVO.Root c=new HrmPayrollGovernanceVO.Root();c.setProjectId(ownerProject);c.setAnchorId(anchor);c.setUserId(user);c.setActions(actions);c.setDelegableActions(actions);c.setScopeJson(upper());c.setValidFrom("2026-01-01");c.setValidTo("2027-12-31");c.setMaterialVersion("TEST-T08-V1");c.setBody("TEST controlled T08 user "+user);c.setReason("TEST independent grant");
        Long id=roots.register(c,1L);roots.action(action(ownerProject,id,"ROOT","SIGN"),4L);return id;
    }
    void policy(){
        for(List<String> qs:Arrays.asList(Arrays.asList("Q-01","Q-02","Q-07","Q-10"),Collections.singletonList("Q-04"),Collections.singletonList("Q-05"))){
            HrmPayrollGovernanceVO.Decision c=new HrmPayrollGovernanceVO.Decision();c.setProjectId(project);c.setProjectScopeVersion(projects.selectById(project).getProjectScopeVersion());c.setTitle("TEST T08 "+qs);c.setQuestions(qs);c.setClauses("TEST PRD v0.4");c.setClauseVersion("TEST T08 V1");c.setBody("TEST controlled T08 decision "+qs);c.setScopeJson(projects.selectById(project).getScopeJson());c.setValidFrom("2026-01-01");c.setValidTo("2027-12-31");c.setApproverId(4L);c.setApprovalConditions("TEST independently reviewed");c.setSemanticChange(false);Map<String,String> other=new LinkedHashMap<>();for(String q:Arrays.asList("Q-03","Q-06","Q-08","Q-09"))other.put(q,"ADVISORY");c.setOtherQuestions(other);c.setReason("TEST controlled draft");
            Long id=decisions.draft(c,1L);decisions.action(action(project,id,"DECISION","SUBMIT"),1L);decisions.action(action(project,id,"DECISION","APPROVE"),4L);
        }
    }
    Long requirement(boolean applicable){
        HrmPayrollRequirementSaveReqVO q=new HrmPayrollRequirementSaveReqVO();q.setProjectId(project);q.setProjectScopeVersion(projects.selectById(project).getProjectScopeVersion());q.setCode("CUSTOM-0000");q.setModule("COL");q.setDescription("TEST original requirement");q.setPriority(2);q.setStatus("PENDING");q.setReadiness("MISSING");q.setScopeMode("SUBSET");q.setScopeJson(scope);q.setRuleVersion("TEST RULE V1");q.setSourceApplicability(applicable?"APPLICABLE":"NOT_APPLICABLE");q.setApplicabilityReason("TEST navigation-only discussion consumes no business data");q.setAcceptance("TEST independently verifiable expected result");q.setEvidence("TEST T08 evidence, not formal approval");
        q.setBusinessContextJson(JsonUtils.toJsonString(map("role","TEST HR","scenario","TEST monthly review","goal","TEST collect","expected","TEST expected","normalCase","TEST normal","exceptionCase","TEST missing","calculation","TEST rule","unit","TEST hour","precision","TEST 2 decimals","validity","TEST 2026-10","ruleException","TEST return for review")));
        Long id=requirements.save(q,1L);requirements.assign(id,reqs.selectById(id).getVersion(),2L,1L);return id;
    }
    HrmPayrollRequirementSaveReqVO draft(HrmPayrollRequirementDO row){return JsonUtils.parseObject(JsonUtils.toJsonString(row),HrmPayrollRequirementSaveReqVO.class);}
    <T extends Context>T context(T c,Long id){c.setProjectId(project);c.setProjectScopeVersion(projects.selectById(project).getProjectScopeVersion());c.setId(id);c.setVersion(id==null?null:records.selectById(id).getVersion());c.setReason("TEST T08 mutation");return c;}
    Long source(){
        Source s=new Source();s.setName("TEST source");s.setModule("COL");s.setScopeJson(scope);s.setOwnerId(1L);s.setTechnicalContact("TEST IT");s.setAcquisition("TEST file");s.setSpecification("TEST contract V1");s.setUniqueKey("employee,period");s.setEmployeeMatch("TEST employee ID");s.setFrequency("monthly");s.setCutoff("month end");s.setVolume("TEST two rows");s.setQualityRule("TEST QS");Field f=new Field();f.setName("hours");f.setMeaning("TEST work duration");f.setType("decimal");f.setUnit("hour");f.setRequired(true);s.setFields(Collections.singletonList(f));SourceSave c=context(new SourceSave(),null);c.setSource(s);return workbench.saveSource(c,1L);
    }
    Long link(Long source,Long requirement){
        Mapping m=new Mapping();m.setSourceField("hours");m.setPageField("workingHours");m.setMeaning("TEST work duration");m.setType("decimal");m.setUnit("hour");m.setRequired(true);m.setTransform("TEST identity");m.setNullRule("reject");m.setDuplicateRule("reject");m.setPeriodRule("same month");m.setExceptionRule("TEST manual correction");Relation r=new Relation();r.setSourceId(source);r.setRequirementId(requirement);r.setFields(Collections.singletonList(m));r.setConsumptionRule("TEST QC");RelationSave c=context(new RelationSave(),null);c.setRelation(r);return workbench.saveRelation(c,1L);
    }
    Source sourceData(Long id){return JsonUtils.parseObject(records.selectById(id).getDataJson(),Source.class);}
    void changeSource(Long id,Consumer<Source> change){Source s=sourceData(id);change.accept(s);SourceSave c=context(new SourceSave(),id);c.setSource(s);workbench.saveSource(c,1L);}
    void decision(Long id,String state){
        cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollReviewVO.Decision c=new cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollReviewVO.Decision();c.setProjectId(project);c.setProjectScopeVersion(1);c.setId(id);c.setVersion(reqs.selectById(id).getVersion());c.setDecision(state);c.setReason("TEST T08 explicit "+state+" scope basis");c.setResponsibleId(1L);c.setReevaluate("TEST independently re-evaluate next scope review");if("REMOVED".equals(state))c.setRemovalKind("OUT_OF_SCOPE");review.decide(c,1L);
    }
    Long included(){Long req=requirement(false);decision(req,"INCLUDED");requirements.confirm(req,reqs.selectById(req).getVersion(),2L);assertTrue(allowedGate());return req;}
    @SuppressWarnings("unchecked") Map<String,Object> statistics(){return review.statistics(project,1L,"READ",null);}
    boolean allowedGate(){return Boolean.TRUE.equals(releases.precheck(project,1L).get("allowed"));}
    boolean baselineIssueBlock(Long issue){
        Map<String,Object> gate=releases.precheck(project,1L);for(Object value:(List<?>)gate.get("blocks")){Map<?,?> block=(Map<?,?>)value;if("BASELINE_ISSUES_OPEN".equals(block.get("code")))for(Object id:(Collection<?>)block.get("issueIds"))if(String.valueOf(issue).equals(String.valueOf(id)))return true;}return false;
    }
    HrmPayrollGovernanceVO.Publish publication(){
        Map<String,Object> gate=releases.precheck(project,1L);HrmPayrollGovernanceVO.Publish c=new HrmPayrollGovernanceVO.Publish();c.setProjectId(project);c.setProjectScopeVersion(1);c.setDraft(false);c.setReason("TEST T08 dependency-consistent specification baseline, not formal approval");c.setRequestKey(UUID.randomUUID().toString());c.setDependencyToken(String.valueOf(gate.get("dependencyToken")));return c;
    }
    @SuppressWarnings("unchecked") Map<String,Object> snapshot(Long release){return (Map<String,Object>)store.data(store.record(release,"RELEASE")).get("snapshot");}
    static final class Outcome {final Object value;final Integer code;Outcome(Object value,Integer code){this.value=value;this.code=code;}}
    TransactionTemplate transaction(){TransactionTemplate tx=new TransactionTemplate(transactionManager);tx.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);return tx;}
    void await(CountDownLatch latch){try{assertTrue(latch.await(8,TimeUnit.SECONDS),"Timed out awaiting T08 transaction checkpoint");}catch(InterruptedException e){Thread.currentThread().interrupt();throw new AssertionError(e);}}
    Outcome worker(long actor,CyclicBarrier barrier,AtomicReference<Connection> connection,Callable<Object> operation){
        identity(1L,actor);try{return transaction().execute(status->{Connection c=DataSourceUtils.getConnection(dataSource);connection.set(c);try{assertFalse(c.getAutoCommit());barrier.await(8,TimeUnit.SECONDS);return new Outcome(operation.call(),null);}catch(RuntimeException e){throw e;}catch(Exception e){throw new AssertionError(e);}});}catch(ServiceException e){return new Outcome(null,e.getCode());}finally{clearIdentity();}
    }
    Outcome[] orderedTransactions(long firstActor,long secondActor,Callable<Object> first,Callable<Object> second) throws Exception {
        ExecutorService pool=Executors.newFixedThreadPool(2);CyclicBarrier barrier=new CyclicBarrier(2);CountDownLatch firstWrote=new CountDownLatch(1),secondAttempted=new CountDownLatch(1),commitFirst=new CountDownLatch(1);AtomicReference<Connection> ca=new AtomicReference<>(),cb=new AtomicReference<>();
        try {
            Future<Outcome> a=pool.submit(()->worker(firstActor,barrier,ca,()->{Object result=first.call();firstWrote.countDown();await(commitFirst);return result;}));
            Future<Outcome> b=pool.submit(()->worker(secondActor,barrier,cb,()->{await(firstWrote);secondAttempted.countDown();return second.call();}));
            await(firstWrote);await(secondAttempted);assertNotSame(ca.get(),cb.get());assertThrows(TimeoutException.class,()->b.get(200,TimeUnit.MILLISECONDS),"Second operation must wait for the actual tenant lock");commitFirst.countDown();Outcome firstResult=a.get(12,TimeUnit.SECONDS),secondResult=b.get(12,TimeUnit.SECONDS);assertNull(firstResult.code);return new Outcome[]{firstResult,secondResult};
        }finally{commitFirst.countDown();pool.shutdownNow();assertTrue(pool.awaitTermination(12,TimeUnit.SECONDS),"T08 workers must stop before SQL cleanup");}
    }
    void clock(String shanghai){ReflectionTestUtils.setField(decisions,"clock",Clock.fixed(LocalDateTime.parse(shanghai).atZone(ZONE).toInstant(),ZONE));}
    static final class Rich {Long first,second,na,source,hiddenSource,hiddenExtraSource,relation,hiddenRelation,hiddenExtraRelation,secondRelation,firstIssue,secondIssue,release;}
    void scopeOf(Long req,String subset,String description){HrmPayrollRequirementSaveReqVO q=draft(reqs.selectById(req));q.setScopeJson(subset);q.setDescription(description);requirements.save(q,1L);}
    String firstHalf(){return scope.replace("2026-10-31","2026-10-15");}
    String secondHalf(){return scope.replace("2026-10-01","2026-10-16");}
    void verify(Long object,String kind,String layer){Map<?,?> view=(Map<?,?>)workbench.detail(project,kind,object,3L);Verify c=context(new Verify(),object);c.setLayer(layer);c.setResult("PASS");c.setReference("TEST T08 synthetic specification check");c.setSampleDigest("TEST T08 controlled synthetic input");c.setExpectedBinding(String.valueOf(((Map<?,?>)view.get("bindings")).get(layer)));workbench.verify(c,3L);}
    Long issue(Long req){Issue i=new Issue();i.setDescription("TEST T08 issue for "+req);i.setRequirementIds(Collections.singletonList(req));i.setSourceIds(Collections.emptyList());i.setRelationIds(Collections.emptyList());i.setWholeProject(false);i.setCategory("ADVISORY");i.setHandlerId(2L);i.setReviewerId(3L);i.setDueDate(LocalDate.now(ZONE).plusDays(7).toString());IssueSave c=context(new IssueSave(),null);c.setIssue(i);return workbench.createIssue(c,1L);}
    IssueAction issueAction(Long issue,String action){IssueAction c=context(new IssueAction(),issue);c.setAction(action);c.setEvidence("TEST T08 "+action+" independent issue evidence");c.setExpectedBinding(String.valueOf(((Map<?,?>)workbench.detail(project,"ISSUE",issue,1L)).get("binding")));return c;}
    Rich rich(){
        Rich f=new Rich();f.first=requirement(true);f.second=requirement(true);f.na=requirement(false);
        StringBuilder longText=new StringBuilder("TEST_VISIBLE_LONG_TEXT\n");for(int n=0;n<35;n++)longText.append("完整中文说明🌿\n跨行可恢复");scopeOf(f.first,firstHalf(),longText.toString());scopeOf(f.second,secondHalf(),"TEST_HIDDEN_REQUIREMENT_CONTRACT");scopeOf(f.na,firstHalf(),"TEST navigation-only declaration");
        f.source=source();changeSource(f.source,s->{s.setScopeJson(firstHalf());s.setName("TEST_VISIBLE_SOURCE");s.setSpecification("TEST_VISIBLE_SPECIFICATION_V1");});f.hiddenSource=source();changeSource(f.hiddenSource,s->{s.setName("TEST_HIDDEN_SOURCE_NAME");s.setSpecification("TEST_HIDDEN_SPECIFICATION_BODY");});
        f.hiddenExtraSource=source();changeSource(f.hiddenExtraSource,s->{s.setName("TEST_HIDDEN_EXTRA_SOURCE");s.setSpecification("TEST_HIDDEN_EXTRA_SPEC");});
        f.relation=link(f.source,f.first);f.hiddenRelation=link(f.hiddenSource,f.first);f.hiddenExtraRelation=link(f.hiddenExtraSource,f.first);f.secondRelation=link(f.hiddenSource,f.second);
        Relation relation=JsonUtils.parseObject(records.selectById(f.relation).getDataJson(),Relation.class);Mapping second=JsonUtils.parseObject(JsonUtils.toJsonString(relation.getFields().get(0)),Mapping.class);second.setPageField("secondDisplayHours");relation.setFields(Arrays.asList(relation.getFields().get(0),second));RelationSave changed=context(new RelationSave(),f.relation);changed.setRelation(relation);workbench.saveRelation(changed,1L);
        verify(f.source,"SOURCE","SOURCE_SEMANTIC");verify(f.hiddenSource,"SOURCE","SOURCE_SEMANTIC");verify(f.hiddenExtraSource,"SOURCE","SOURCE_SEMANTIC");for(Long id:Arrays.asList(f.relation,f.hiddenRelation,f.hiddenExtraRelation,f.secondRelation))verify(id,"RELATION","RELATION_SEMANTIC");
        for(Long req:Arrays.asList(f.first,f.second)){decision(req,"INCLUDED");requirements.confirm(req,reqs.selectById(req).getVersion(),2L);}decision(f.na,"DEFERRED");requirements.confirm(f.na,reqs.selectById(f.na).getVersion(),2L);
        f.firstIssue=issue(f.first);f.secondIssue=issue(f.second);assertTrue(allowedGate());f.release=releases.publish(publication(),1L);return f;
    }
    HrmPayrollGovernanceVO.Job job(Long ownerProject,Long release){HrmPayrollGovernanceVO.Job c=new HrmPayrollGovernanceVO.Job();c.setProjectId(ownerProject);c.setReleaseId(release);return c;}
    Long ready(Long release,Long actor) throws Exception {Long id=exports.create(job(project,release),actor);exports.generate(project,id,actor);assertEquals("READY",exports.status(project,id,actor).get("state"));return id;}
    @SuppressWarnings("unchecked") Map<String,List<Map<String,Object>>> tables(Long release,Long actor){return (Map<String,List<Map<String,Object>>>)projection.project(project,release,actor,"EXPORT",null).get("sheets");}
    String state(){return JsonUtils.toJsonString(map("projects",projects.selectList(),"grants",grants.selectList(),"requirements",reqs.selectList(),"history",histories.selectList(),"projectEvents",projectEvents.selectList(),"records",records.selectList(),"jobs",store.all(null,"JOB"),"events",store.all(null,"EVENT"),"releases",store.all(null,"RELEASE"),"roots",store.all(null,"ROOT"),"decisions",store.all(null,"DECISION"),"anchors",store.all(null,"ANCHOR")));}
    void denied(Callable<?> request,cn.iocoder.yudao.framework.common.exception.ErrorCode code){String before=state();ServiceException e=assertThrows(ServiceException.class,request::call);assertEquals(code.getCode(),e.getCode());assertEquals(code.getMsg(),e.getMessage());assertEquals(before,state());}
    Object canonical(Object value){if(value instanceof Map){Map<String,Object> sorted=new TreeMap<>();((Map<?,?>)value).forEach((k,v)->sorted.put(String.valueOf(k),canonical(v)));return sorted;}if(value instanceof Collection){List<Object> list=new ArrayList<>();for(Object x:(Collection<?>)value)list.add(canonical(x));return list;}return value;}
    String normalized(Object value){return JsonUtils.toJsonString(canonical(JsonUtils.parseObject(JsonUtils.toJsonString(value),Object.class)));}
    Map<String,Map<String,Map<String,Object>>> expected(Map<String,List<Map<String,Object>>> tables){Map<String,Map<String,Map<String,Object>>> result=new TreeMap<>();tables.forEach((sheet,rows)->{Map<String,Map<String,Object>> records=new TreeMap<>();for(Map<String,Object> row:rows){String ref=String.valueOf(row.get("记录号"));assertNull(records.put(ref,row),"Duplicate expected reference "+ref);}result.put(sheet,records);});return result;}
    /** Independently reconstruct EVERY parent/field from actual claimed XLSX, enforcing chunk and type invariants. */
    Map<String,Map<String,Map<String,Object>>> rebuild(byte[] bytes) throws Exception {
        Map<String,Map<String,Map<String,Object>>> result=new TreeMap<>();List<String> header=Arrays.asList("记录类型","记录号","所属记录","字段名","片段序号","片段总数","值类型","内容");
        try(XSSFWorkbook book=new XSSFWorkbook(new ByteArrayInputStream(bytes))){assertEquals(6,book.getNumberOfSheets());for(int sheetNo=0;sheetNo<6;sheetNo++){
            assertEquals(HrmPayrollGovernanceProjectionService.SHEETS.get(sheetNo),book.getSheetName(sheetNo));Sheet sheet=book.getSheetAt(sheetNo);assertEquals(8,sheet.getRow(0).getLastCellNum());for(int col=0;col<8;col++)assertEquals(header.get(col),sheet.getRow(0).getCell(col).getStringCellValue());
            Map<String,Map<String,Object>> records=new TreeMap<>();Map<String,Map<String,TreeMap<Integer,String>>> chunks=new TreeMap<>();Map<String,Integer> totals=new HashMap<>();Map<String,String> types=new HashMap<>();
            for(int rowNo=1;rowNo<=sheet.getLastRowNum();rowNo++){Row row=sheet.getRow(rowNo);assertNotNull(row);assertEquals(8,row.getLastCellNum());for(Cell cell:row)assertNotEquals(CellType.FORMULA,cell.getCellType());String kind=row.getCell(0).getStringCellValue(),ref=row.getCell(1).getStringCellValue(),parent=row.getCell(2).getStringCellValue();
                if(!"DETAIL".equals(kind)){assertEquals(ref,parent);assertEquals("RECORD",row.getCell(6).getStringCellValue());assertNull(records.put(ref,new TreeMap<>()),"Duplicate parent "+ref);assertNotEquals("",kind);continue;}
                assertTrue(records.containsKey(parent));String field=row.getCell(3).getStringCellValue(),type=row.getCell(6).getStringCellValue(),key=parent+"\u0000"+field;int seq=(int)row.getCell(4).getNumericCellValue(),total=(int)row.getCell(5).getNumericCellValue();assertTrue(seq>=1&&total>=seq);if(totals.containsKey(key))assertEquals(totals.get(key),total);totals.put(key,total);if(types.containsKey(key))assertEquals(types.get(key),type);types.put(key,type);assertEquals(CellType.STRING,row.getCell(7).getCellType());assertNull(chunks.computeIfAbsent(parent,k->new TreeMap<>()).computeIfAbsent(field,k->new TreeMap<>()).put(seq,row.getCell(7).getStringCellValue()));
            }
            for(Map.Entry<String,Map<String,TreeMap<Integer,String>>> parent:chunks.entrySet())for(Map.Entry<String,TreeMap<Integer,String>> field:parent.getValue().entrySet()){
                String key=parent.getKey()+"\u0000"+field.getKey();int total=totals.get(key);assertEquals(total,field.getValue().size());StringBuilder text=new StringBuilder();for(int n=1;n<=total;n++){assertTrue(field.getValue().containsKey(n));text.append(field.getValue().get(n));}String type=types.get(key);assertTrue(Arrays.asList("NULL","TEXT","NUMBER","BOOLEAN","JSON").contains(type));Object value="NULL".equals(type)?null:"TEXT".equals(type)?text.toString():JsonUtils.parseObject(text.toString(),Object.class);records.get(parent.getKey()).put(field.getKey(),value);
            }
            for(Map.Entry<String,Map<String,Object>> row:records.entrySet()){assertFalse(row.getValue().isEmpty());assertEquals(row.getKey(),row.getValue().get("记录号"));assertNotNull(row.getValue().get("记录类型"));}result.put(sheet.getSheetName(),records);
        }}references(result);return result;
    }
    void references(Map<String,Map<String,Map<String,Object>>> book){
        Set<String> refs=new HashSet<>();for(Map<String,Map<String,Object>> sheet:book.values())refs.addAll(sheet.keySet());
        for(Map<String,Map<String,Object>> sheet:book.values())for(Map<String,Object> row:sheet.values())for(String field:Arrays.asList("需求关联","来源关联","对象关联","发布依据关联","授权关联","授权根关联","关联决议","受限关联")){
            Object raw=row.get(field);if(!(raw instanceof String))continue;String ref=(String)raw;
            if(ref.matches("^(REQ-|SRC-|REL-|ISSUE-|DEC-|APPROVAL-GRANT-|ROOT-|ANCHOR-|GRANT-|受限关联-).*|SUMMARY-1|受限发布依据"))assertTrue(refs.contains(ref),"Unresolved reference "+field+"/"+ref);
        }
    }
    Map<String,Object> metadata(Map<String,Map<String,Map<String,Object>>> rebuilt){Map<String,Object> metadata=rebuilt.get("评审版本说明").remove("EXPORT-METADATA");assertNotNull(metadata);assertEquals("SUMMARY",metadata.get("记录类型"));assertNotNull(metadata.get("本次生成时间"));return metadata;}
    void reconcile(Long release,Long actor,byte[] bytes) throws Exception {Map<String,Map<String,Map<String,Object>>> workbook=rebuild(bytes);Map<String,Object> meta=metadata(workbook);assertEquals(String.valueOf(actor),meta.get("本次导出人ID"));assertEquals(projection.project(project,release,actor,"EXPORT",null).get("label"),meta.get("文件完整性"));assertEquals(normalized(expected(tables(release,actor))),normalized(workbook));}
    void assertNoHidden(byte[] bytes,Rich f) throws Exception {
        String decoded=normalized(rebuild(bytes));
        for(String kind:Arrays.asList("DECISION","ROOT","ANCHOR"))for(HrmPayrollGovernanceRecordDO row:store.all("ANCHOR".equals(kind)?0L:project,kind)){
            Map<String,Object> data=store.data(row);for(String key:Arrays.asList("body","digest","approvalConditions"))if(data.get(key)!=null&&!String.valueOf(data.get(key)).isEmpty())assertFalse(decoded.contains(String.valueOf(data.get(key))),"Restricted authority fact leaked: "+kind+"/"+key);
        }
        for(String secret:Arrays.asList("TEST_HIDDEN_REQUIREMENT_CONTRACT","TEST_HIDDEN_SOURCE_NAME","TEST_HIDDEN_SPECIFICATION_BODY","TEST_HIDDEN_EXTRA_SOURCE","TEST_HIDDEN_EXTRA_SPEC","REQ-"+f.second,"SRC-"+f.hiddenSource,"SRC-"+f.hiddenExtraSource,"REL-"+f.hiddenRelation,"REL-"+f.hiddenExtraRelation,"REL-"+f.secondRelation,"ISSUE-"+f.secondIssue))assertFalse(decoded.contains(secret),"Leaked hidden typed identifier or body "+secret);
        for(HrmPayrollWorkbenchRecordDO record:records.selectList())if("EVIDENCE".equals(record.getKind())){Map<?,?> evidence=JsonUtils.parseObject(record.getDataJson(),Map.class);if(Arrays.asList(f.hiddenSource,f.hiddenExtraSource,f.hiddenRelation,f.hiddenExtraRelation,f.secondRelation).stream().anyMatch(id->String.valueOf(id).equals(String.valueOf(evidence.get("objectId")))))assertFalse(decoded.contains(String.valueOf(evidence.get("bindingDigest"))),"Hidden binding digest leaked");}
        try(ZipInputStream zip=new ZipInputStream(new ByteArrayInputStream(bytes))){for(ZipEntry entry;(entry=zip.getNextEntry())!=null;){if(!entry.getName().startsWith("xl/")||!entry.getName().endsWith(".xml"))continue;ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[4096];for(int n;(n=zip.read(buffer))!=-1;)out.write(buffer,0,n);String xml=new String(out.toByteArray(),StandardCharsets.UTF_8);for(String secret:Arrays.asList("TEST_HIDDEN_REQUIREMENT_CONTRACT","TEST_HIDDEN_SOURCE_NAME","TEST_HIDDEN_SPECIFICATION_BODY","TEST_HIDDEN_EXTRA_SOURCE","TEST_HIDDEN_EXTRA_SPEC","SRC-"+f.hiddenSource,"SRC-"+f.hiddenExtraSource,"REL-"+f.hiddenRelation,"REL-"+f.hiddenExtraRelation,"REL-"+f.secondRelation))assertFalse(xml.contains(secret));}}
    }
    Long readerRoot(){return grants.selectList().stream().filter(g->Objects.equals(g.getProjectId(),project)&&Objects.equals(g.getUserId(),3L)&&g.getParentGrantId()==null).findFirst().get().getRootRecordId();}
    void narrowReader(){roots.action(action(project,readerRoot(),"ROOT","REVOKE"),4L);HrmPayrollRequirementProjectGrantDO g=new HrmPayrollRequirementProjectGrantDO();g.setProjectId(project);g.setUserId(3L);g.setActions("READ,EXPORT,BASELINE_PUBLISH");g.setDelegableActions("");g.setFullProject(true);g.setCeilingJson(firstHalf());g.setValidFrom(LocalDate.of(2026,1,1));g.setValidTo(LocalDate.of(2027,12,31));g.setEvidence("TEST T08 current first-half scope, no full closure");projectService.grant(g,1L);}

    @Test void richHistoricalSixSheetDownloadRetainsAllBusinessFieldsAfterRealCurrentChanges() throws Exception {
        Rich f=rich();Map<String,List<Map<String,Object>>> before=tables(f.release,1L);byte[] first=exports.claim(project,ready(f.release,1L),1L);reconcile(f.release,1L,first);Map<String,Map<String,Map<String,Object>>> firstBusiness=rebuild(first);Map<String,Object> firstMeta=metadata(firstBusiness);
        changeSource(f.source,s->{s.setName("TEST_RENAMED_SOURCE");s.setSpecification("TEST_CHANGED_SPEC");s.getFields().get(0).setUnit("minute");});Supply delivery=context(new Supply(),f.source);delivery.setSupply("PROVIDED");delivery.setReplaceDelivery(true);delivery.setDeliveryDigest("TEST_REPLACED_DELIVERY_DIGEST");delivery.setDeliveryReference("TEST new controlled delivery");workbench.supply(delivery,1L);
        Relation relation=JsonUtils.parseObject(records.selectById(f.relation).getDataJson(),Relation.class);relation.getFields().get(0).setTransform("TEST revised divide by sixty");RelationSave c=context(new RelationSave(),f.relation);c.setRelation(relation);workbench.saveRelation(c,1L);workbench.issueAction(issueAction(f.firstIssue,"START"),2L);
        when(users.getUser(2L)).thenReturn(new cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO().setId(2L).setStatus(0).setNickname("TEST_RENAMED_REVIEWER"));List<Map<String,Object>> currentFacts=new ArrayList<>();for(long user=1;user<=5;user++)currentFacts.add(map("id",user,"nickname",users.getUser(user).getNickname(),"status",users.getUser(user).getStatus()));doReturn(currentFacts).when(store).userFacts();Long old=id(((Map<?,?>)decisions.current(project,scope,Collections.singletonList("Q-04")).get(0)).get("recordId"));HrmPayrollGovernanceVO.Decision replacement=JsonUtils.parseObject(JsonUtils.toJsonString(store.data(store.record(old,"DECISION"))),HrmPayrollGovernanceVO.Decision.class);replacement.setPreviousId(old);replacement.setBody("TEST new current authority matrix");replacement.setReason("TEST T08 independent replacement");Long next=decisions.draft(replacement,1L);decisions.action(action(project,next,"DECISION","SUBMIT"),1L);decisions.action(action(project,next,"DECISION","APPROVE"),4L);
        assertEquals("TEST_RENAMED_REVIEWER",users.getUser(2L).getNickname());assertEquals("TEST_RENAMED_SOURCE",sourceData(f.source).getName());assertEquals("TEST_CHANGED_SPEC",sourceData(f.source).getSpecification());assertEquals("minute",sourceData(f.source).getFields().get(0).getUnit());assertEquals("TEST revised divide by sixty",JsonUtils.parseObject(records.selectById(f.relation).getDataJson(),Relation.class).getFields().get(0).getTransform());assertEquals(next,id(((Map<?,?>)decisions.current(project,scope,Collections.singletonList("Q-04")).get(0)).get("recordId")));assertEquals(1,sourceData(f.source).getDeliveryVersion());assertEquals("PROCESSING",JsonUtils.parseObject(records.selectById(f.firstIssue).getDataJson(),Issue.class).getState());assertEquals("REVIEWING",reqs.selectById(f.first).getStatus());assertEquals(normalized(before),normalized(tables(f.release,1L)));
        byte[] after=exports.claim(project,ready(f.release,1L),1L);reconcile(f.release,1L,after);Map<String,Map<String,Map<String,Object>>> afterBusiness=rebuild(after);Map<String,Object> afterMeta=metadata(afterBusiness);assertEquals(normalized(firstBusiness),normalized(afterBusiness));firstMeta.remove("本次生成时间");afterMeta.remove("本次生成时间");assertEquals(normalized(firstMeta),normalized(afterMeta));assertFalse(normalized(afterBusiness).contains("TEST_RENAMED_REVIEWER"));assertFalse(normalized(afterBusiness).contains("TEST_CHANGED_SPEC"));assertFalse(normalized(afterBusiness).contains("TEST_REPLACED_DELIVERY_DIGEST"));
    }
    @Test void narrowedScopeRejectsFullPublicationAndOldFullJobButAllowsExactCurrentSectionBytes() throws Exception {
        Rich f=rich();Long old=ready(f.release,3L);byte[] originallyFull=exports.claim(project,old,3L);assertTrue(normalized(rebuild(originallyFull)).contains("TEST_HIDDEN_SOURCE_NAME"));narrowReader();denied(()->exports.claim(project,old,3L),PAYROLL_REVIEW_INCOMPLETE);denied(()->exports.status(project,old,3L),PAYROLL_REVIEW_INCOMPLETE);
        Map<String,Object> gate=releases.precheck(project,3L);assertEquals(false,gate.get("allowed"));assertEquals("FULL_CLOSURE_READ_REQUIRED",((Map<?,?>)((List<?>)gate.get("blocks")).get(0)).get("code"));HrmPayrollGovernanceVO.Publish publish=publication();publish.setDependencyToken(String.valueOf(gate.get("dependencyToken")));denied(()->releases.publish(publish,3L),PAYROLL_PROJECT_DENIED);
        Map<String,Object> section=projection.project(project,f.release,3L,"EXPORT",null);assertEquals(true,section.get("restricted"));assertEquals("权限受限节选",section.get("label"));Map<?,?> stats=(Map<?,?>)section.get("statistics");assertEquals(2,((Number)((Map<?,?>)stats.get("counts")).get("U")).intValue());assertEquals(1,((Number)stats.get("sourceCount")).intValue());Map<String,List<Map<String,Object>>> scoped=tables(f.release,3L);assertEquals(2,scoped.get("功能需求清单").size());assertEquals(1,scoped.get("数据源清单").stream().filter(r->"SOURCE".equals(r.get("记录类型"))).count());
        for(String sheet:Arrays.asList("数据源清单","字段映射","问题与待办")){Map<String,Long> count=new HashMap<>();for(Map<String,Object> row:scoped.get(sheet))if("受限关联".equals(row.get("记录类型")))count.merge(String.valueOf(row.get("需求关联")),1L,Long::sum);assertFalse(count.isEmpty());for(Long n:count.values())assertEquals(1L,n);}
        byte[] claimed=exports.claim(project,ready(f.release,3L),3L);reconcile(f.release,3L,claimed);assertNoHidden(claimed,f);assertTrue(normalized(rebuild(claimed)).contains("TEST_VISIBLE_SOURCE"));assertTrue(normalized(rebuild(claimed)).contains("NA-"+f.na));
        denied(()->exports.claim(project,old,4L),PAYROLL_PROJECT_DENIED);reconcile(f.release,4L,exports.claim(project,ready(f.release,4L),4L));
    }
    @ParameterizedTest @ValueSource(strings={"REVOKED_CREATE","REVOKED_GENERATE","REVOKED_CLAIM","DISABLED_CREATE","DISABLED_GENERATE","DISABLED_CLAIM"})
    void currentAccessLossAtEachDownloadStageCannotReuseOldIdentityAndOtherMemberUsesOwnJob(String scenario) throws Exception {
        Rich f=rich();Long old=scenario.endsWith("CREATE")?null:exports.create(job(project,f.release),3L);if(scenario.endsWith("CLAIM"))exports.generate(project,old,3L);String oldState=old==null?null:String.valueOf(store.data(store.record(old,"JOB")).get("state"));
        if(scenario.startsWith("REVOKED"))roots.action(action(project,readerRoot(),"ROOT","REVOKE"),4L);else when(users.getUser(3L)).thenReturn(new cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO().setId(3L).setStatus(1));
        denied(()->exports.create(job(project,f.release),3L),PAYROLL_PROJECT_DENIED);if(old!=null){if(scenario.endsWith("GENERATE"))denied(()->{exports.generate(project,old,3L);return null;},PAYROLL_PROJECT_DENIED);denied(()->exports.claim(project,old,3L),PAYROLL_PROJECT_DENIED);denied(()->exports.status(project,old,3L),PAYROLL_PROJECT_DENIED);assertEquals(oldState,store.data(store.record(old,"JOB")).get("state"));denied(()->exports.claim(project,old,4L),PAYROLL_PROJECT_DENIED);}
        byte[] own=exports.claim(project,ready(f.release,4L),4L);reconcile(f.release,4L,own);
    }
    @Test void releaseAndJobProjectTenantSubstitutionRejectsWithoutLeakingOrMutatingOwnedObjects() throws Exception {
        Rich f=rich();Map<String,Object> tenantOneAnchorData=new LinkedHashMap<>(store.data(store.record(anchor,"ANCHOR")));Long tenantOneProject=project,tenantOneAnchor=anchor;Long ready=ready(f.release,3L),other=newProject("TEST_T08_OTHER");root(other,1L,new ArrayList<>(HrmPayrollRequirementAccessService.ACTIONS));String original=state();assertTrue(releases.list(other,1L).isEmpty());
        denied(()->projection.project(other,f.release,1L,"READ",null),PAYROLL_PROJECT_DENIED);denied(()->exports.create(job(other,f.release),1L),PAYROLL_PROJECT_DENIED);denied(()->{exports.generate(other,ready,3L);return null;},PAYROLL_PROJECT_DENIED);denied(()->exports.status(other,ready,3L),PAYROLL_PROJECT_DENIED);denied(()->exports.claim(other,ready,3L),PAYROLL_PROJECT_DENIED);Long decision=id(((Map<?,?>)decisions.current(project,scope,Collections.singletonList("Q-04")).get(0)).get("recordId"));denied(()->decisions.history(other,decision,1L),PAYROLL_PROJECT_DENIED);assertEquals(original,state());
        identity(2L,1L);store.insert(0L,"LOCK","TENANT",map());assertNull(records.selectById(f.source));assertServiceException(()->store.record(f.release,"RELEASE"),PAYROLL_PROJECT_DENIED);assertServiceException(()->store.record(ready,"JOB"),PAYROLL_PROJECT_DENIED);
        assertServiceException(()->releases.list(project,1L),PAYROLL_PROJECT_DENIED);assertServiceException(()->projection.project(project,f.release,1L,"READ",null),PAYROLL_PROJECT_DENIED);assertServiceException(()->exports.create(job(project,f.release),3L),PAYROLL_PROJECT_DENIED);assertServiceException(()->exports.status(project,ready,3L),PAYROLL_PROJECT_DENIED);assertServiceException(()->exports.claim(project,ready,3L),PAYROLL_PROJECT_DENIED);assertServiceException(()->decisions.history(project,decision,1L),PAYROLL_PROJECT_DENIED);assertServiceException(()->{try{exports.generate(project,ready,3L);}catch(IOException e){throw new AssertionError(e);}},PAYROLL_PROJECT_DENIED);
        // Tenant 2 has its own valid authority, release and claimed bytes, so missing fixtures cannot mask isolation.
        anchor=store.insert(0L,"ANCHOR","TEST-T08-TENANT2-ANCHOR",tenantOneAnchorData).getId();project=newProject("TEST_T08_TENANT2");for(Long actor:Arrays.asList(1L,2L,4L))root(project,actor,new ArrayList<>(HrmPayrollRequirementAccessService.ACTIONS));policy();included();Long ownRelease=releases.publish(publication(),1L);byte[] ownBytes=exports.claim(project,ready(ownRelease,1L),1L);reconcile(ownRelease,1L,ownBytes);assertEquals(1,releases.list(project,1L).size());assertFalse(normalized(rebuild(ownBytes)).contains("TEST_VISIBLE_SOURCE"));
        project=tenantOneProject;anchor=tenantOneAnchor;identity(1L,1L);assertEquals(original,state());reconcile(f.release,3L,exports.claim(project,ready,3L));
    }
    void mutate(String dependency,Rich f){if("SOURCE".equals(dependency))changeSource(f.source,s->s.setSpecification("TEST_T08_NEW_CONTRACT"));else {Relation r=JsonUtils.parseObject(records.selectById(f.relation).getDataJson(),Relation.class);r.getFields().get(0).setTransform("TEST_T08_NEW_MAPPING");RelationSave c=context(new RelationSave(),f.relation);c.setRelation(r);workbench.saveRelation(c,1L);}}
    void restoreCurrent(Rich f){verify(f.source,"SOURCE","SOURCE_SEMANTIC");verify(f.relation,"RELATION","RELATION_SEMANTIC");requirements.confirm(f.first,reqs.selectById(f.first).getVersion(),2L);}
    @ParameterizedTest @ValueSource(strings={"SOURCE","MAPPING"})
    void associatedChangeCommitsBeforePublishRejectsOldTokenThenFreezesCompleteNewContracts(String dependency) throws Exception {
        Rich f=rich();HrmPayrollGovernanceVO.Publish old=publication();int count=store.all(project,"RELEASE").size();Outcome[] result=orderedTransactions(1L,1L,()->{mutate(dependency,f);return state();},()->releases.publish(old,1L));assertEquals(PAYROLL_REVIEW_INCOMPLETE.getCode(),result[1].code);assertEquals(result[0].value,state());assertEquals(count,store.all(project,"RELEASE").size());restoreCurrent(f);Long next=releases.publish(publication(),1L);assertNotEquals(f.release,next);byte[] bytes=exports.claim(project,ready(next,1L),1L);reconcile(next,1L,bytes);assertTrue(normalized(rebuild(bytes)).contains("SOURCE".equals(dependency)?"TEST_T08_NEW_CONTRACT":"TEST_T08_NEW_MAPPING"));assertFalse(normalized(rebuild(exports.claim(project,ready(f.release,1L),1L))).contains("SOURCE".equals(dependency)?"TEST_T08_NEW_CONTRACT":"TEST_T08_NEW_MAPPING"));
    }
    @ParameterizedTest @ValueSource(strings={"SOURCE","MAPPING"})
    void publicationCommitsBeforeAssociationChangeKeepsOneFrozenSixSheetObjectGraph(String dependency) throws Exception {
        Rich f=rich();HrmPayrollGovernanceVO.Publish request=publication();Outcome[] result=orderedTransactions(1L,1L,()->{Long release=releases.publish(request,1L);return new Object[]{release,normalized(tables(release,1L))};},()->{mutate(dependency,f);return null;});assertNull(result[1].code);Object[] published=(Object[])result[0].value;Long release=(Long)published[0];
        // Compare the SAME release projection captured before the opposing association mutation.
        assertEquals(published[1],normalized(tables(release,1L)));byte[] bytes=exports.claim(project,ready(release,1L),1L);reconcile(release,1L,bytes);assertFalse(normalized(rebuild(bytes)).contains("SOURCE".equals(dependency)?"TEST_T08_NEW_CONTRACT":"TEST_T08_NEW_MAPPING"));assertEquals("REVIEWING",reqs.selectById(f.first).getStatus());
    }
    @Test void hiddenSourceCardinalityCannotChangeCurrentRestrictedSixSheetContents() throws Exception {
        Rich f=rich();Long extra=source();changeSource(extra,s->{s.setName("TEST_CARDINALITY_HIDDEN_SENTINEL");s.setSpecification("TEST_CARDINALITY_HIDDEN_BODY");});verify(extra,"SOURCE","SOURCE_SEMANTIC");Long second=releases.publish(publication(),1L);assertEquals(4,((List<?>)snapshot(second).get("workbench")).stream().filter(x->"SOURCE".equals(((Map<?,?>)x).get("kind"))).count());
        narrowReader();Map<String,Map<String,Map<String,Object>>> a=rebuild(exports.claim(project,ready(f.release,3L),3L)),b=rebuild(exports.claim(project,ready(second,3L),3L));metadata(a);metadata(b);
        // Only independently published release identity, ordinal and time are public differences.
        for(Map<String,Map<String,Map<String,Object>>> book:Arrays.asList(a,b))for(String field:Arrays.asList("发布ID","发布版本","发布时刻"))book.get("评审版本说明").get("SUMMARY-1").remove(field);
        assertEquals(normalized(a),normalized(b));assertFalse(normalized(b).contains("TEST_CARDINALITY_HIDDEN"));assertFalse(normalized(b).contains("SRC-"+extra));
    }
    @ParameterizedTest @ValueSource(strings={"GENERATE_FIRST","REVOKE_FIRST"})
    void generationAndCurrentRootRevocationSerializeWithoutClaimingFormerCompleteBytes(String order) throws Exception {
        Rich f=rich();Long queued=exports.create(job(project,f.release),3L);Long root=readerRoot();Callable<Object> generate=()->{exports.generate(project,queued,3L);return store.data(store.record(queued,"JOB")).get("state");};Callable<Object> revoke=()->{roots.action(action(project,root,"ROOT","REVOKE"),4L);return state();};
        if("GENERATE_FIRST".equals(order)){Outcome[] result=orderedTransactions(3L,4L,generate,revoke);assertEquals("READY",result[0].value);assertNull(result[1].code);assertEquals("READY",store.data(store.record(queued,"JOB")).get("state"));}
        else {Outcome[] result=orderedTransactions(4L,3L,revoke,generate);assertEquals(PAYROLL_PROJECT_DENIED.getCode(),result[1].code);assertEquals(result[0].value,state());assertEquals("QUEUED",store.data(store.record(queued,"JOB")).get("state"));}
        denied(()->exports.claim(project,queued,3L),PAYROLL_PROJECT_DENIED);denied(()->exports.status(project,queued,3L),PAYROLL_PROJECT_DENIED);denied(()->exports.claim(project,queued,4L),PAYROLL_PROJECT_DENIED);reconcile(f.release,4L,exports.claim(project,ready(f.release,4L),4L));
    }

}
