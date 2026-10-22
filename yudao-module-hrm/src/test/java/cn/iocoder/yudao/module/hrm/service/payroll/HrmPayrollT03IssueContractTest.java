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

/** T03 issue contracts on real H2 transactions, tenant SQL filtering, controlled roots and decisions.
 * Only external user/permission APIs are mocked. This is not browser/session or MySQL acceptance. */
@Import({HrmPayrollT03IssueContractTest.TenantSql.class,HrmPayrollGovernanceStore.class,
    HrmPayrollGovernanceRootService.class,HrmPayrollGovernanceDecisionService.class,
    HrmPayrollRequirementAccessService.class,HrmPayrollRequirementProjectService.class,
    HrmPayrollRequirementServiceImpl.class,HrmPayrollWorkbenchService.class})
@Timeout(40)
class HrmPayrollT03IssueContractTest extends BaseDbUnitTest {
    @TestConfiguration
    static class TenantSql {
        // Same handler and insertion order as the production tenant configuration; no web/cache beans.
        @Bean TenantLineInnerInterceptor t03TenantSql(MybatisPlusInterceptor interceptor) {
            TenantLineInnerInterceptor tenant=new TenantLineInnerInterceptor(new TenantDatabaseInterceptor(new TenantProperties()));
            MyBatisUtils.addInterceptor(interceptor,tenant,0);return tenant;
        }
    }
    @Resource TenantLineInnerInterceptor t03TenantSql;
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
    @MockBean cn.iocoder.yudao.module.system.api.user.AdminUserApi users;
    @MockBean cn.iocoder.yudao.module.system.api.permission.PermissionApi permissions;
    Long project,anchor,reviewerRoot;
    final String scope="{\"entities\":[101],\"organizations\":[201],\"workerTypes\":[\"TEST\"],\"scenarios\":[\"TEST\"],\"from\":\"2026-10-01\",\"to\":\"2026-10-31\",\"explanation\":\"TEST T03 fixed scope\"}";
    String upper(){return scope.replace("[101]","[101,102]").replace("2026-10-31","2026-12-31");}
    void identity(long tenant,long actor){
        TenantContextHolder.setTenantId(tenant);
        SecurityFrameworkUtils.setLoginUser(new LoginUser().setId(actor).setUserType(2),new MockHttpServletRequest());
    }
    void clearIdentity(){TenantContextHolder.clear();SecurityContextHolder.clearContext();}
    @BeforeEach void setup(){
        identity(1L,1L);assertSame(t03TenantSql,sqlInterceptors.getInterceptors().get(0));
        when(permissions.hasAnyPermissions(anyLong(),anyString())).thenReturn(true);
        for(long user=1;user<=5;user++)when(users.getUser(user)).thenReturn(new cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO().setId(user).setStatus(0).setNickname("TEST T03 user "+user));
        when(permissions.hasAnyPermissions(eq(5L),anyString())).thenAnswer(call->Arrays.asList("hrm:payroll:requirement:query","hrm:payroll:requirement:update").contains(call.getArgument(1)));
        store.insert(0L,"LOCK","TENANT",map());
        String body="TEST independently registered T03 root evidence; not formal approval";
        // Custodian 1 and signer 4 are independent of the business reviewer 2.
        anchor=store.insert(0L,"ANCHOR","TEST-T03-ANCHOR",map("state","REGISTERED","mode","TEST","custodianId",1L,"signerId",4L,"actions",new ArrayList<>(HrmPayrollRequirementAccessService.ACTIONS),"scopeJson",upper(),"validFrom","2026-01-01","validTo","2027-12-31","body",body,"digest",bodyDigest(body),"materialVersion","TEST-T03-V1")).getId();
        project=newProject("TEST_T03");
        root(project,1L,new ArrayList<>(HrmPayrollRequirementAccessService.ACTIONS));
        reviewerRoot=root(project,2L,new ArrayList<>(HrmPayrollRequirementAccessService.ACTIONS));
        root(project,3L,new ArrayList<>(HrmPayrollRequirementAccessService.ACTIONS));
        root(project,4L,new ArrayList<>(HrmPayrollRequirementAccessService.ACTIONS));
        root(project,5L,Arrays.asList("READ","EDIT"));policy();
    }
    @AfterEach void cleanup(){clearIdentity();}
    Long newProject(String key){
        HrmPayrollRequirementProjectDO p=new HrmPayrollRequirementProjectDO().setProjectKey(key).setName("TEST T03 project").setOwnerId(1L).setProjectScopeVersion(1).setMetadataVersion(1).setCustomSequence(0L).setScopeJson(scope).setArchived(false);projects.insert(p);return p.getId();
    }
    HrmPayrollGovernanceVO.Action action(Long ownerProject,Long id,String kind,String operation){
        HrmPayrollGovernanceVO.Action c=new HrmPayrollGovernanceVO.Action();c.setProjectId(ownerProject);c.setId(id);c.setVersion(store.record(id,kind).getVersion());c.setAction(operation);c.setReason("TEST independent T03 action");return c;
    }
    Long root(Long ownerProject,Long user,List<String> actions){
        HrmPayrollGovernanceVO.Root c=new HrmPayrollGovernanceVO.Root();c.setProjectId(ownerProject);c.setAnchorId(anchor);c.setUserId(user);c.setActions(actions);c.setDelegableActions(actions);c.setScopeJson(upper());c.setValidFrom("2026-01-01");c.setValidTo("2027-12-31");c.setMaterialVersion("TEST-T03-V1");c.setBody("TEST controlled T03 user "+user);c.setReason("TEST independent grant");
        Long id=roots.register(c,1L);roots.action(action(ownerProject,id,"ROOT","SIGN"),4L);return id;
    }
    void policy(){
        for(List<String> qs:Arrays.asList(Arrays.asList("Q-01","Q-02","Q-07","Q-10"),Collections.singletonList("Q-04"),Collections.singletonList("Q-05"))){
            HrmPayrollGovernanceVO.Decision c=new HrmPayrollGovernanceVO.Decision();c.setProjectId(project);c.setProjectScopeVersion(projects.selectById(project).getProjectScopeVersion());c.setTitle("TEST T03 "+qs);c.setQuestions(qs);c.setClauses("TEST PRD v0.4");c.setClauseVersion("TEST T03 V1");c.setBody("TEST controlled T03 decision "+qs);c.setScopeJson(projects.selectById(project).getScopeJson());c.setValidFrom("2026-01-01");c.setValidTo("2027-12-31");c.setApproverId(4L);c.setApprovalConditions("TEST independently reviewed");c.setSemanticChange(false);Map<String,String> other=new LinkedHashMap<>();for(String q:Arrays.asList("Q-03","Q-06","Q-08","Q-09"))other.put(q,"ADVISORY");c.setOtherQuestions(other);c.setReason("TEST controlled draft");
            Long id=decisions.draft(c,1L);decisions.action(action(project,id,"DECISION","SUBMIT"),1L);decisions.action(action(project,id,"DECISION","APPROVE"),4L);
        }
    }
    Long requirement(boolean applicable){
        HrmPayrollRequirementSaveReqVO q=new HrmPayrollRequirementSaveReqVO();q.setProjectId(project);q.setProjectScopeVersion(projects.selectById(project).getProjectScopeVersion());q.setCode("CUSTOM-0000");q.setModule("COL");q.setDescription("TEST original requirement");q.setPriority(2);q.setStatus("PENDING");q.setReadiness("MISSING");q.setScopeMode("SUBSET");q.setScopeJson(scope);q.setRuleVersion("TEST RULE V1");q.setSourceApplicability(applicable?"APPLICABLE":"NOT_APPLICABLE");q.setApplicabilityReason("TEST navigation-only discussion consumes no business data");q.setAcceptance("TEST independently verifiable expected result");q.setEvidence("TEST T03 evidence, not formal approval");
        q.setBusinessContextJson(JsonUtils.toJsonString(map("role","TEST HR","scenario","TEST monthly review","goal","TEST collect","expected","TEST expected","normalCase","TEST normal","exceptionCase","TEST missing","calculation","TEST rule","unit","TEST hour","precision","TEST 2 decimals","validity","TEST 2026-10","ruleException","TEST return for review")));
        Long id=requirements.save(q,1L);requirements.assign(id,reqs.selectById(id).getVersion(),2L,1L);return id;
    }
    HrmPayrollRequirementSaveReqVO draft(HrmPayrollRequirementDO row){return JsonUtils.parseObject(JsonUtils.toJsonString(row),HrmPayrollRequirementSaveReqVO.class);}
    <T extends Context>T context(T c,Long id){c.setProjectId(project);c.setProjectScopeVersion(projects.selectById(project).getProjectScopeVersion());c.setId(id);c.setVersion(id==null?null:records.selectById(id).getVersion());c.setReason("TEST T03 mutation");return c;}
    Long source(){
        Source s=new Source();s.setName("TEST source");s.setModule("COL");s.setScopeJson(scope);s.setOwnerId(1L);s.setTechnicalContact("TEST IT");s.setAcquisition("TEST file");s.setSpecification("TEST contract V1");s.setUniqueKey("employee,period");s.setEmployeeMatch("TEST employee ID");s.setFrequency("monthly");s.setCutoff("month end");s.setVolume("TEST two rows");s.setQualityRule("TEST QS");Field f=new Field();f.setName("hours");f.setMeaning("TEST work duration");f.setType("decimal");f.setUnit("hour");f.setRequired(true);s.setFields(Collections.singletonList(f));SourceSave c=context(new SourceSave(),null);c.setSource(s);return workbench.saveSource(c,1L);
    }
    Long link(Long source,Long requirement){
        Mapping m=new Mapping();m.setSourceField("hours");m.setPageField("workingHours");m.setMeaning("TEST work duration");m.setType("decimal");m.setUnit("hour");m.setRequired(true);m.setTransform("TEST identity");m.setNullRule("reject");m.setDuplicateRule("reject");m.setPeriodRule("same month");m.setExceptionRule("TEST manual correction");Relation r=new Relation();r.setSourceId(source);r.setRequirementId(requirement);r.setFields(Collections.singletonList(m));r.setConsumptionRule("TEST QC");RelationSave c=context(new RelationSave(),null);c.setRelation(r);return workbench.saveRelation(c,1L);
    }
    Source sourceData(Long id){return JsonUtils.parseObject(records.selectById(id).getDataJson(),Source.class);}
    void changeSource(Long id,Consumer<Source> change){Source s=sourceData(id);change.accept(s);SourceSave c=context(new SourceSave(),id);c.setSource(s);workbench.saveSource(c,1L);}
    Long issue(Long req){
        Issue i=new Issue();i.setDescription("TEST T03 independently reviewed discrepancy");i.setRequirementIds(Collections.singletonList(req));i.setSourceIds(Collections.emptyList());i.setRelationIds(Collections.emptyList());i.setWholeProject(false);i.setCategory("DATA_READINESS");i.setHandlerId(2L);i.setReviewerId(3L);i.setDueDate(LocalDate.now(ZoneId.of("Asia/Shanghai")).minusDays(1).toString());
        IssueSave c=context(new IssueSave(),null);c.setIssue(i);Long id=workbench.createIssue(c,1L);assertEquals(1,records.selectById(id).getVersion());assertEvent(id,1,"CREATE",1L,"OPEN",1);return id;
    }
    Issue data(Long id){return JsonUtils.parseObject(records.selectById(id).getDataJson(),Issue.class);}
    @SuppressWarnings("unchecked") String binding(Long id){return (String)((Map<String,Object>)workbench.detail(project,"ISSUE",id,1L)).get("binding");}
    IssueAction command(Long id,String operation){
        IssueAction c=context(new IssueAction(),id);c.setAction(operation);c.setEvidence("TEST T03 "+operation+" independently supported evidence");c.setExpectedBinding(binding(id));return c;
    }
    void execute(Long id,String operation,Long actor){execute(command(id,operation),actor);}
    void execute(IssueAction c,Long actor){
        Issue before=data(c.getId());int version=records.selectById(c.getId()).getVersion();workbench.issueAction(c,actor);Issue after=data(c.getId());
        String expected;
        switch(c.getAction()){
            case "START":case "REJECT":expected="PROCESSING";break;
            case "SUBMIT":expected="REVIEW";break;
            case "CLOSE":expected="CLOSED";break;
            case "REOPEN":expected="OPEN";break;
            default:expected=before.getState();
        }
        assertEquals(version+1,records.selectById(c.getId()).getVersion());assertEquals(expected,after.getState());
        assertEvent(c.getId(),version+1,c.getAction(),actor,expected,after.getCycle());
        assertEquals(c.getAction().equals("REOPEN")?before.getCycle()+1:before.getCycle(),after.getCycle());
    }
    void assertEvent(Long id,int version,String operation,Long actor,String state,int cycle){
        List<Map> found=new ArrayList<>();
        for(HrmPayrollWorkbenchRecordDO row:records.selectList())if("HISTORY".equals(row.getKind())){
            Map event=JsonUtils.parseObject(row.getDataJson(),Map.class);
            if(String.valueOf(id).equals(String.valueOf(event.get("objectId")))&&("ISSUE_"+operation).equals(event.get("action"))&&String.valueOf(version).equals(String.valueOf(event.get("objectVersion"))))found.add(event);
        }
        assertEquals(1,found.size());Map event=found.get(0);assertEquals(String.valueOf(actor),String.valueOf(event.get("actor")));assertEquals("ISSUE",event.get("kind"));
        Issue snapshot=JsonUtils.parseObject(JsonUtils.toJsonString(event.get("snapshot")),Issue.class);assertEquals(state,snapshot.getState());assertEquals(cycle,snapshot.getCycle());
        assertNotNull(event.get("time"));assertNotNull(event.get("reason"));
    }
    List<Map> issueEvents(Long id){
        List<Map> result=new ArrayList<>();for(HrmPayrollWorkbenchRecordDO row:records.selectList())if("HISTORY".equals(row.getKind())){Map event=JsonUtils.parseObject(row.getDataJson(),Map.class);if(String.valueOf(id).equals(String.valueOf(event.get("objectId"))))result.add(event);}return result;
    }
    String persistentState(){
        // Full persisted row snapshots catch unexpected EVIDENCE/HISTORY/TODO or signature writes.
        return JsonUtils.toJsonString(map("records",records.selectList(),"requirements",reqs.selectList(),"requirementHistory",histories.selectList(),"governance",store.all(project,"EVENT")));
    }
    ServiceException rejected(IssueAction c,Long actor,cn.iocoder.yudao.framework.common.exception.ErrorCode code){
        String before=persistentState();ServiceException error=assertThrows(ServiceException.class,()->workbench.issueAction(c,actor));assertEquals(code.getCode(),error.getCode());assertEquals(before,persistentState());return error;
    }
    void toReview(Long id){execute(id,"START",2L);execute(id,"SUBMIT",2L);}
    void close(Long id){toReview(id);execute(id,"CLOSE",3L);}

    @Test void twoCompleteCyclesPreserveRejectReassignmentAndFirstClosureFacts(){
        Long req=requirement(false),id=issue(req);execute(id,"START",2L);execute(id,"SUBMIT",2L);String firstProcessing=data(id).getProcessing();execute(id,"REJECT",3L);String rejectedReview=data(id).getReview();
        IssueAction reassignment=command(id,"REASSIGN");reassignment.setHandlerId(1L);reassignment.setReviewerId(3L);execute(reassignment,4L);
        assertEquals(firstProcessing,data(id).getProcessing());assertEquals(rejectedReview,data(id).getReview());
        IssueAction corrected=command(id,"SUBMIT");corrected.setEvidence("TEST corrected processing from successor 1");execute(corrected,1L);execute(id,"CLOSE",3L);
        Issue firstClosed=data(id);assertEquals(1,firstClosed.getCycle());assertTrue(firstClosed.getWasOverdue());assertNotNull(firstClosed.getClosedDate());assertEquals(corrected.getEvidence(),firstClosed.getProcessing());String firstCloseReview=firstClosed.getReview();
        IssueAction reopen=command(id,"REOPEN");reopen.setEvidence("TEST new recurring discrepancy after first closure");reopen.setDueDate(LocalDate.now(ZoneId.of("Asia/Shanghai")).plusDays(10).toString());execute(reopen,3L);
        assertEquals(2,data(id).getCycle());assertEquals(reopen.getDueDate(),data(id).getDueDate());assertFalse(data(id).getWasOverdue());assertNull(data(id).getClosedDate());assertNull(data(id).getProcessing());assertNull(data(id).getReview());
        execute(id,"START",1L);IssueAction secondSubmit=command(id,"SUBMIT");secondSubmit.setEvidence("TEST cycle2 correction");execute(secondSubmit,1L);execute(id,"CLOSE",3L);
        assertEquals(2,data(id).getCycle());assertEquals(secondSubmit.getEvidence(),data(id).getProcessing());
        List<Map> events=issueEvents(id);assertEquals(11,events.size());
        List<Issue> closed=new ArrayList<>();for(Map event:events){if("ISSUE_CLOSE".equals(event.get("action")))closed.add(JsonUtils.parseObject(JsonUtils.toJsonString(event.get("snapshot")),Issue.class));}
        assertEquals(2,closed.size());Issue old=closed.stream().filter(i->i.getCycle()==1).findFirst().get();assertEquals(firstClosed.getProcessing(),old.getProcessing());assertEquals(firstCloseReview,old.getReview());assertEquals(firstClosed.getClosedDate(),old.getClosedDate());assertTrue(old.getWasOverdue());
        assertTrue(events.stream().anyMatch(e->"ISSUE_SUBMIT".equals(e.get("action"))&&"2".equals(String.valueOf(e.get("actor")))&&firstProcessing.equals(JsonUtils.parseObject(JsonUtils.toJsonString(e.get("snapshot")),Issue.class).getProcessing())));
        assertTrue(events.stream().anyMatch(e->"ISSUE_REJECT".equals(e.get("action"))&&rejectedReview.equals(JsonUtils.parseObject(JsonUtils.toJsonString(e.get("snapshot")),Issue.class).getReview())));
        assertNull(reqs.selectById(req).getConfirmedBy());assertNull(reqs.selectById(req).getConfirmedTupleJson());
    }
    @ParameterizedTest @ValueSource(strings={"START","SUBMIT","REJECT","CLOSE","REOPEN","REASSIGN"})
    void missingActionEvidenceRejectsWithoutAnyWritesAndCanBeCorrected(String operation){
        Long id=issue(requirement(false));if(Arrays.asList("SUBMIT").contains(operation))execute(id,"START",2L);else if(Arrays.asList("REJECT","CLOSE").contains(operation))toReview(id);else if("REOPEN".equals(operation))close(id);
        Long actor=Arrays.asList("START","SUBMIT").contains(operation)?2L:"REASSIGN".equals(operation)?1L:3L;
        IssueAction c=command(id,operation);if("REOPEN".equals(operation))c.setDueDate(LocalDate.now(ZoneId.of("Asia/Shanghai")).plusDays(5).toString());if("REASSIGN".equals(operation)){c.setHandlerId(1L);c.setReviewerId(3L);}c.setEvidence(" ");
        ServiceException error=rejected(c,actor,PAYROLL_REVIEW_INCOMPLETE);assertTrue(error.getMessage().contains("处理或复核依据"));c.setEvidence("TEST corrected action evidence");execute(c,actor);
    }
    @ParameterizedTest @ValueSource(strings={"START","SUBMIT","REJECT","CLOSE","REOPEN","REASSIGN"})
    void missingOperationReasonRejectsWithoutAnyWrites(String operation){
        Long id=issue(requirement(false));if("SUBMIT".equals(operation))execute(id,"START",2L);else if(Arrays.asList("REJECT","CLOSE").contains(operation))toReview(id);else if("REOPEN".equals(operation))close(id);
        Long actor=Arrays.asList("START","SUBMIT").contains(operation)?2L:"REASSIGN".equals(operation)?1L:3L;
        IssueAction c=command(id,operation);if("REOPEN".equals(operation))c.setDueDate(LocalDate.now(ZoneId.of("Asia/Shanghai")).plusDays(5).toString());if("REASSIGN".equals(operation)){c.setHandlerId(1L);c.setReviewerId(3L);}c.setReason(" ");rejected(c,actor,PAYROLL_REVIEW_INCOMPLETE);
        c.setReason("TEST corrected operation reason");execute(c,actor);
    }
    @ParameterizedTest @ValueSource(strings={"START","SUBMIT","REJECT","CLOSE","REOPEN","REASSIGN"})
    void wrongAssignedActorCannotExecuteAction(String operation){
        Long id=issue(requirement(false));if("SUBMIT".equals(operation))execute(id,"START",2L);else if(Arrays.asList("REJECT","CLOSE").contains(operation))toReview(id);else if("REOPEN".equals(operation))close(id);
        IssueAction c=command(id,operation);Long actor=Arrays.asList("START","SUBMIT","REJECT","CLOSE").contains(operation)?4L:5L;rejected(c,actor,PAYROLL_PROJECT_DENIED);
    }
    @Test void assignedHandlerStillNeedsPlatformActionPermission(){
        Long id=issue(requirement(false));when(permissions.hasAnyPermissions(2L,"hrm:payroll:requirement:issue-handle")).thenReturn(false);
        rejected(command(id,"START"),2L,PAYROLL_PROJECT_DENIED);
        when(permissions.hasAnyPermissions(2L,"hrm:payroll:requirement:issue-handle")).thenReturn(true);execute(id,"START",2L);
    }
    @Test void handlerCannotSelfCloseAndWrongReviewerCannotClose(){
        Long id=issue(requirement(false));toReview(id);rejected(command(id,"CLOSE"),2L,PAYROLL_PROJECT_DENIED);rejected(command(id,"CLOSE"),4L,PAYROLL_PROJECT_DENIED);execute(id,"CLOSE",3L);
    }
    @ParameterizedTest @ValueSource(strings={"START","SUBMIT","REJECT","CLOSE","REOPEN","REASSIGN"})
    void invalidStateRejectsIncludingCloseEvidenceRollback(String operation){
        Long id=issue(requirement(false));if("START".equals(operation))execute(id,"START",2L);if("REASSIGN".equals(operation))close(id);
        IssueAction c=command(id,operation);Long actor=Arrays.asList("START","SUBMIT").contains(operation)?2L:"REASSIGN".equals(operation)?1L:3L;if("REASSIGN".equals(operation)){c.setHandlerId(1L);c.setReviewerId(3L);}
        rejected(c,actor,PAYROLL_REVIEW_INCOMPLETE);
    }
    @ParameterizedTest @ValueSource(strings={"MISSING","EMPTY","INVALID"})
    void reopenDeadlineRejectsWithoutDestroyingFirstCycle(String variant){
        Long id=issue(requirement(false));close(id);Issue first=data(id);IssueAction c=command(id,"REOPEN");if("EMPTY".equals(variant))c.setDueDate("");else if("INVALID".equals(variant))c.setDueDate("2026-02-30");
        ServiceException error=rejected(c,3L,PAYROLL_REVIEW_INCOMPLETE);assertTrue(error.getMessage().contains("新期限"));assertEquals(first.getProcessing(),data(id).getProcessing());assertEquals(first.getReview(),data(id).getReview());assertEquals(first.getClosedDate(),data(id).getClosedDate());
        c.setDueDate(LocalDate.now(ZoneId.of("Asia/Shanghai")).plusDays(7).toString());execute(c,3L);assertEquals(2,data(id).getCycle());
    }
    @ParameterizedTest @ValueSource(strings={"HANDLER","REVIEWER","SAME_PERSON","INACTIVE_HANDLER","INACTIVE_REVIEWER"})
    void reassignmentRejectsIncompleteOrInvalidPeopleAndPreservesOldAssignments(String variant){
        Long id=issue(requirement(false));IssueAction c=command(id,"REASSIGN");c.setHandlerId(1L);c.setReviewerId(3L);
        if("HANDLER".equals(variant))c.setHandlerId(null);else if("REVIEWER".equals(variant))c.setReviewerId(null);else if("SAME_PERSON".equals(variant))c.setReviewerId(1L);else {long disabled="INACTIVE_HANDLER".equals(variant)?2L:3L;if(disabled==2L)c.setHandlerId(2L);when(users.getUser(disabled)).thenReturn(new cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO().setId(disabled).setStatus(1));}
        rejected(c,4L,variant.startsWith("INACTIVE")?PAYROLL_PROJECT_DENIED:PAYROLL_REVIEW_INCOMPLETE);assertEquals(2L,data(id).getHandlerId());assertEquals(3L,data(id).getReviewerId());
    }
    @Test void revokedHandlerCannotContinueAndAuthorizedSuccessorKeepsOriginalProcessingFacts(){
        Long id=issue(requirement(false));toReview(id);String prior=data(id).getProcessing();roots.action(action(project,reviewerRoot,"ROOT","REVOKE"),4L);
        rejected(command(id,"SUBMIT"),2L,PAYROLL_PROJECT_DENIED);assertServiceException(()->workbench.history(project,"ISSUE",id,2L),PAYROLL_PROJECT_DENIED);
        IssueAction c=command(id,"REASSIGN");c.setHandlerId(1L);c.setReviewerId(3L);execute(c,4L);assertEquals(prior,data(id).getProcessing());
        execute(id,"REJECT",3L);IssueAction replacement=command(id,"SUBMIT");replacement.setEvidence("TEST successor independently reprocesses discrepancy");execute(replacement,1L);execute(id,"CLOSE",3L);
        assertTrue(issueEvents(id).stream().anyMatch(e->"ISSUE_SUBMIT".equals(e.get("action"))&&"2".equals(String.valueOf(e.get("actor")))&&prior.equals(JsonUtils.parseObject(JsonUtils.toJsonString(e.get("snapshot")),Issue.class).getProcessing())));
        assertTrue(issueEvents(id).stream().anyMatch(e->"ISSUE_SUBMIT".equals(e.get("action"))&&"1".equals(String.valueOf(e.get("actor")))&&replacement.getEvidence().equals(JsonUtils.parseObject(JsonUtils.toJsonString(e.get("snapshot")),Issue.class).getProcessing())));
    }
    @Test void staleVersionCrossProjectAndCrossTenantCannotChangeIssue(){
        Long id=issue(requirement(false));IssueAction stale=command(id,"START");execute(id,"START",2L);rejected(stale,2L,PAYROLL_VERSION_CONFLICT);
        Long other=newProject("TEST_T03_OTHER");root(other,1L,new ArrayList<>(HrmPayrollRequirementAccessService.ACTIONS));IssueAction swapped=command(id,"SUBMIT");swapped.setProjectId(other);rejected(swapped,1L,PAYROLL_PROJECT_DENIED);
        String before=persistentState();identity(2L,1L);store.insert(0L,"LOCK","TENANT",map());assertNull(records.selectById(id));assertServiceException(()->workbench.issueAction(swapped,1L),PAYROLL_PROJECT_DENIED);assertServiceException(()->workbench.history(project,"ISSUE",id,1L),PAYROLL_PROJECT_DENIED);identity(1L,1L);assertEquals(before,persistentState());execute(id,"SUBMIT",2L);execute(id,"CLOSE",3L);
    }
    @Test void closingWithStaleDependencyBindingRejectsAndCanUseFreshBinding(){
        Long req=requirement(false),id=issue(req);toReview(id);IssueAction old=command(id,"CLOSE");HrmPayrollRequirementSaveReqVO q=draft(reqs.selectById(req));q.setDescription("TEST changed requirement after reviewer opened issue");requirements.save(q,1L);
        rejected(old,3L,PAYROLL_VERSION_CONFLICT);execute(id,"CLOSE",3L);
    }
    @Test void closeDoesNotConfirmRequirementCreateQualityOrMakeUnverifiedSourceAvailable(){
        Long req=requirement(true),source=source(),relation=link(source,req),id=issue(req);toReview(id);
        String requirement=JsonUtils.toJsonString(reqs.selectById(req)),sourceBefore=records.selectById(source).getDataJson(),relationBefore=records.selectById(relation).getDataJson();
        Set<String> qualityBefore=qualityEvidence();assertEquals(false,((Map<?,?>)workbench.detail(project,"RELATION",relation,1L)).get("available"));execute(id,"CLOSE",3L);
        assertEquals(requirement,JsonUtils.toJsonString(reqs.selectById(req)));assertNull(reqs.selectById(req).getConfirmedBy());assertNull(reqs.selectById(req).getConfirmedTupleJson());assertEquals(sourceBefore,records.selectById(source).getDataJson());assertEquals(relationBefore,records.selectById(relation).getDataJson());assertEquals(qualityBefore,qualityEvidence());assertEquals(false,((Map<?,?>)workbench.detail(project,"RELATION",relation,1L)).get("available"));
        assertEquals(1,records.selectList().stream().filter(r->"EVIDENCE".equals(r.getKind())).map(r->JsonUtils.parseObject(r.getDataJson(),Map.class)).filter(e->"ISSUE_CLOSE".equals(e.get("layer"))).count());
    }
    Set<String> qualityEvidence(){Set<String> result=new TreeSet<>();for(HrmPayrollWorkbenchRecordDO row:records.selectList())if("EVIDENCE".equals(row.getKind())){Map evidence=JsonUtils.parseObject(row.getDataJson(),Map.class);if(Arrays.asList("QS","QC","SOURCE_SEMANTIC","RELATION_SEMANTIC").contains(evidence.get("layer")))result.add(row.getDataJson());}return result;}
}
