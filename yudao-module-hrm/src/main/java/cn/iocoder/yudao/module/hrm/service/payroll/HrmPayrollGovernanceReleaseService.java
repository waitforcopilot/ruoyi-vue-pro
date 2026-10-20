package cn.iocoder.yudao.module.hrm.service.payroll;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollGovernanceVO.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;
import static cn.iocoder.yudao.module.hrm.service.payroll.HrmPayrollGovernanceStore.*;

/** Snapshot creation has no edit API. Checks and capture run in one serialized transaction. */
@Service
@Transactional(rollbackFor=Exception.class,isolation=Isolation.READ_COMMITTED)
public class HrmPayrollGovernanceReleaseService {
    @Resource private HrmPayrollGovernanceStore store;
    @Resource private HrmPayrollGovernanceRootService roots;
    @Resource private HrmPayrollGovernanceDecisionService decisions;
    @Resource private HrmPayrollRequirementAccessService access;
    @Resource private HrmPayrollRequirementProjectMapper projects;
    @Resource private HrmPayrollRequirementMapper requirements;
    @Resource private HrmPayrollRequirementHistoryMapper requirementHistories;
    @Resource private HrmPayrollRequirementProjectGrantMapper grants;
    @Resource private HrmPayrollRequirementProjectEventMapper projectEvents;
    @Resource private HrmPayrollWorkbenchRecordMapper workbenchRecords;
    @Resource private HrmPayrollWorkbenchService workbench;
    @Resource private HrmPayrollReviewService review;
    private HrmPayrollRequirementProjectDO project(Long id,Long actor){HrmPayrollRequirementProjectDO p=projects.selectById(id);allowed(p!=null&&access.project(p,actor,"READ"));return p;}
    List<HrmPayrollRequirementDO> rows(Long p){return requirements.selectList(new LambdaQueryWrapperX<HrmPayrollRequirementDO>().eq(HrmPayrollRequirementDO::getProjectId,p).orderByAsc(HrmPayrollRequirementDO::getCode).orderByAsc(HrmPayrollRequirementDO::getId));}
    List<HrmPayrollWorkbenchRecordDO> nodes(Long p){return workbenchRecords.selectList(new LambdaQueryWrapperX<HrmPayrollWorkbenchRecordDO>().eq(HrmPayrollWorkbenchRecordDO::getProjectId,p).orderByAsc(HrmPayrollWorkbenchRecordDO::getId));}
    /** Current object closure must be fully visible to publish. Technical admin is not an exception. */
    boolean fullClosure(HrmPayrollRequirementProjectDO p,Long actor){
        if(!roots.canPlatform(actor,"decision-read")||!roots.canPlatform(actor,"grant-read")||!access.full(p,actor,"READ")||!access.full(p,actor,"DECISION_READ")||!access.full(p,actor,"GRANT_READ")||!access.full(p,actor,"PERSON_READ"))return false;
        for(String kind:Arrays.asList("SOURCE","RELATION","ISSUE","EVIDENCE","HISTORY","TODO","PROPOSAL")){
            long expected=nodes(p.getId()).stream().filter(n->kind.equals(n.getKind())).count();
            if(workbench.list(p.getId(),kind,actor).size()!=expected)return false;
        }
        return true;
    }
    private static final Set<String> PERSON_FIELDS=new HashSet<>(Arrays.asList("ownerId","reviewerId","sourceOwnerId","handlerId","actorId","userId","grantorId","custodianId","signerId","designatedSignerId","approverId","createdBy","registeredBy","signedBy","approvedBy","revokedBy","confirmedBy","responsibleId"));
    /** Follow typed person references, never arbitrary object IDs that happen to equal a user ID. */
    private void referencedPeople(Object value,Set<Long> people){
        if(value instanceof Map)for(Map.Entry<?,?> entry:((Map<?,?>)value).entrySet()){
            if(PERSON_FIELDS.contains(entry.getKey())&&entry.getValue()!=null)people.add(id(entry.getValue()));
            else referencedPeople(entry.getValue(),people);
        }
        else if(value instanceof Collection)for(Object item:(Collection<?>)value)referencedPeople(item,people);
    }
    private Map<String,Object> dependencies(HrmPayrollRequirementProjectDO p,List<Map<String,Object>> users,Long actor){
        List<HrmPayrollRequirementProjectGrantDO> currentGrants=grants.selectList(new LambdaQueryWrapperX<HrmPayrollRequirementProjectGrantDO>().eq(HrmPayrollRequirementProjectGrantDO::getProjectId,p.getId()).orderByAsc(HrmPayrollRequirementProjectGrantDO::getId));
        Set<Long> rootIds=currentGrants.stream().map(HrmPayrollRequirementProjectGrantDO::getRootRecordId).filter(Objects::nonNull).collect(Collectors.toSet());
        List<HrmPayrollGovernanceRecordDO> controlled=new ArrayList<>(store.all(p.getId(),"ROOT"));
        Set<Long> present=controlled.stream().map(HrmPayrollGovernanceRecordDO::getId).collect(Collectors.toSet());
        for(Long rootId:new TreeSet<>(rootIds))if(present.add(rootId))controlled.add(store.record(rootId,"ROOT"));
        controlled.sort(Comparator.comparing(HrmPayrollGovernanceRecordDO::getId));
        Set<Long> anchorIds=controlled.stream().map(r->id(store.data(r).get("anchorId"))).collect(Collectors.toSet());
        List<HrmPayrollGovernanceRecordDO> anchors=store.all(0L,"ANCHOR").stream().filter(r->anchorIds.contains(r.getId())).collect(Collectors.toList());
        Map<String,Object> deps=map("project",p,"requirements",rows(p.getId()),"workbench",nodes(p.getId()),"grants",currentGrants,"decisions",store.all(p.getId(),"DECISION"),"roots",controlled,"anchors",anchors);
        Set<Long> people=new HashSet<>(Arrays.asList(actor,p.getOwnerId()));
        // Parse JSON-backed records as well as DOs to include upstream signers and decision approvers.
        referencedPeople(JsonUtils.parseObject(JsonUtils.toJsonString(deps),Map.class),people);
        for(HrmPayrollGovernanceRecordDO record:controlled)referencedPeople(store.data(record),people);
        for(HrmPayrollGovernanceRecordDO record:anchors)referencedPeople(store.data(record),people);
        for(HrmPayrollGovernanceRecordDO record:store.all(p.getId(),"DECISION"))referencedPeople(store.data(record),people);
        for(HrmPayrollWorkbenchRecordDO record:nodes(p.getId()))referencedPeople(JsonUtils.parseObject(record.getDataJson(),Map.class),people);
        deps.put("users",users.stream().filter(u->people.contains(id(u.get("id")))).collect(Collectors.toList()));
        return deps;
    }
    @SuppressWarnings("unchecked") private Map<String,Object> judgement(HrmPayrollRequirementProjectDO p,Long actor){
        List<Object> blocks=new ArrayList<>(),selected=new ArrayList<>();Map<String,Object> classification=new LinkedHashMap<>();
        if(Boolean.TRUE.equals(p.getArchived()))blocks.add(map("code","PROJECT_ARCHIVED"));
        if(!fullClosure(p,actor))blocks.add(map("code","FULL_CLOSURE_READ_REQUIRED"));
        if(!access.full(p,actor,"BASELINE_PUBLISH"))blocks.add(map("code","BASELINE_PUBLISH_REQUIRED"));
        try{roots.platform(actor,"baseline-publish");roots.authority(p.getId(),actor,"BASELINE_PUBLISH",p.getScopeJson());}catch(RuntimeException denied){blocks.add(map("code","CURRENT_CONTROLLED_PUBLISH_AUTHORITY_REQUIRED"));}
        try{selected=decisions.current(p.getId(),p.getScopeJson(),HrmPayrollGovernanceDecisionService.REQUIRED);
            for(String question:Arrays.asList("Q-03","Q-06","Q-08","Q-09")){
                try{List<Object> resolved=decisions.current(p.getId(),p.getScopeJson(),Collections.singletonList(question));for(Object fact:resolved)if(!selected.contains(fact))selected.add(fact);classification.put(question,"RESOLVED");continue;}catch(RuntimeException unresolved){/* Explicit classification is required below. */}
                Set<String> states=new HashSet<>();for(Object raw:selected){Map<?,?>d=(Map<?,?>)((Map<?,?>)raw).get("decision");Map<?,?>other=(Map<?,?>)d.get("otherQuestions");if(other!=null&&other.get(question)!=null)states.add(String.valueOf(other.get(question)));}
                if(states.size()!=1||!Arrays.asList("BASELINE_BLOCK","DATA_READINESS","ADVISORY").contains(states.iterator().next()))blocks.add(map("code","UNRESOLVED_QUESTION_CLASSIFICATION","question",question));
                else{String state=states.iterator().next();classification.put(question,state);if("BASELINE_BLOCK".equals(state))blocks.add(map("code","UNRESOLVED_BASELINE_QUESTION","question",question));}
            }
        }catch(RuntimeException incomplete){blocks.add(map("code","CURRENT_DECISION_COVERAGE_REQUIRED","questions",HrmPayrollGovernanceDecisionService.REQUIRED));}
        List<HrmPayrollRequirementDO> rows=rows(p.getId());List<HrmPayrollRequirementDO> included=rows.stream().filter(r->"INCLUDED".equals(r.getScopeDecision())).collect(Collectors.toList());if(included.isEmpty())blocks.add(map("code","EMPTY_INCLUDED_SET"));
        for(HrmPayrollRequirementDO r:rows){
            if("INCLUDED".equals(r.getScopeDecision())){
                if(!"MATCH".equals(r.getScopeState())||!Objects.equals(p.getProjectScopeVersion(),r.getProjectScopeVersion()))blocks.add(map("code","SCOPE_MISMATCH","requirementId",r.getId()));
                if(!workbench.currentConfirmationValid(r))blocks.add(map("code","CURRENT_CONFIRMATION_REQUIRED","requirementId",r.getId()));
                Map<String,Object> tuple=r.getConfirmedTupleJson()==null?Collections.emptyMap():JsonUtils.parseObject(r.getConfirmedTupleJson(),Map.class);
                if(!(tuple.get("governance") instanceof Map))blocks.add(map("code","CONFIRMATION_GOVERNANCE_FACT_REQUIRED","requirementId",r.getId()));
                if(!workbench.specificationReady(r))blocks.add(map("code","SPECIFICATION_NOT_VERIFIED","requirementId",r.getId()));
            }else{
                Map<String,Object>d=r.getScopeDecisionJson()==null?Collections.emptyMap():JsonUtils.parseObject(r.getScopeDecisionJson(),Map.class);
                if(blank(d.get("reason"))||d.get("responsibleId")==null&& !"REMOVED".equals(r.getScopeDecision())||"DEFERRED".equals(r.getScopeDecision())&&blank(d.get("reevaluate")))blocks.add(map("code","EXCLUSION_BASIS_INCOMPLETE","requirementId",r.getId()));
                if("REMOVED".equals(r.getScopeDecision())&&"DUPLICATE".equals(d.get("removalKind"))){HrmPayrollRequirementDO replacement=requirements.selectById(id(d.get("replacementId")));if(replacement==null||!Objects.equals(replacement.getProjectId(),p.getId())||"REMOVED".equals(replacement.getScopeDecision()))blocks.add(map("code","DUPLICATE_TARGET_INVALID","requirementId",r.getId()));}
            }
        }
        if(access.full(p,actor,"READ")){Map<String,Object>b=workbench.blockers(p.getId(),actor);if(Boolean.TRUE.equals(b.get("baselineBlocked")))blocks.add(map("code","BASELINE_ISSUES_OPEN","issueIds",b.get("baselineIssueIds")));}
        return map("allowed",blocks.isEmpty(),"blocks",blocks,"decisions",selected,"otherQuestionClassification",classification,"publicationMeaning","需求规格基线，不代表数据就绪或可投产");
    }
    public Map<String,Object> precheck(Long project,Long actor){return access.withReadAuthorization(()->precheckRead(project,actor));}
    private Map<String,Object> precheckRead(Long project,Long actor){
        store.lock();HrmPayrollRequirementProjectDO p=project(project,actor);List<Map<String,Object>>users=store.userFacts();Map<String,Object> gate=access.full(p,actor,"READ")?judgement(p,actor):map("allowed",false,"blocks",Collections.singletonList(map("code","FULL_CLOSURE_READ_REQUIRED")));
        if(!fullClosure(p,actor)){gate.remove("decisions");gate.put("blocks",Collections.singletonList(map("code","FULL_CLOSURE_READ_REQUIRED")));}
        gate.put("projectScopeVersion",p.getProjectScopeVersion());gate.put("dependencyToken",digest(dependencies(p,users,actor)));return gate;
    }
    @SuppressWarnings("unchecked") public Long publish(Publish c,Long actor){return access.withReadAuthorization(()->publishRead(c,actor));}
    private Long publishRead(Publish c,Long actor){
        store.lock();HrmPayrollRequirementProjectDO p=project(c.getProjectId(),actor);allowed(access.project(p,actor,"EXPORT")||!c.getDraft());List<Map<String,Object>> users=store.userFacts();
        if(!c.getDraft()){roots.platform(actor,"baseline-publish");allowed(fullClosure(p,actor));roots.authority(p.getId(),actor,"BASELINE_PUBLISH",p.getScopeJson());}
        else roots.platform(actor,"export");
        Map<String,Object>request=map("projectId",p.getId(),"scopeVersion",c.getProjectScopeVersion(),"reason",c.getReason(),"draft",c.getDraft(),"actor",actor,"dependencyToken",c.getDependencyToken());String requestDigest=digest(request);
        need(Objects.equals(p.getProjectScopeVersion(),c.getProjectScopeVersion()));Map<String,Object>deps=dependencies(p,users,actor);need(Objects.equals(c.getDependencyToken(),digest(deps)));Map<String,Object>gate=access.full(p,actor,"READ")?judgement(p,actor):map("allowed",false);if(!c.getDraft())need(Boolean.TRUE.equals(gate.get("allowed")));
        for(HrmPayrollGovernanceRecordDO existing:store.all(p.getId(),"RELEASE"))if(existing.getObjectKey().equals(c.getRequestKey())){Map<String,Object>d=store.data(existing);need(Objects.equals(d.get("requestDigest"),requestDigest));return existing.getId();}

        List<HrmPayrollRequirementDO> frozenRows=JsonUtils.parseArray(JsonUtils.toJsonString(rows(p.getId())),HrmPayrollRequirementDO.class);for(HrmPayrollRequirementDO req:frozenRows)req.setCurrentConfirmationValid(workbench.currentConfirmationValid(req));
        Map<String,Object> snapshot=map("project",p,"requirements",frozenRows,"workbench",nodes(p.getId()),"projectEvents",projectEvents.selectList(new LambdaQueryWrapperX<HrmPayrollRequirementProjectEventDO>().eq(HrmPayrollRequirementProjectEventDO::getProjectId,p.getId()).orderByAsc(HrmPayrollRequirementProjectEventDO::getId)),"statistics",access.full(p,actor,"READ")?review.statistics(p.getId(),actor,"READ",null):null,"gate",gate,"userFacts",users,"dependencyToken",c.getDependencyToken());
        Set<Long> frozenIds=frozenRows.stream().map(HrmPayrollRequirementDO::getId).collect(Collectors.toSet());snapshot.put("requirementHistories",frozenIds.isEmpty()?Collections.emptyList():requirementHistories.selectList(new LambdaQueryWrapperX<HrmPayrollRequirementHistoryDO>().in(HrmPayrollRequirementHistoryDO::getRequirementId,frozenIds).orderByAsc(HrmPayrollRequirementHistoryDO::getId)));
        // Keep all frozen grants and controlled evidence in the server snapshot, never a current pointer.
        List<HrmPayrollGovernanceRecordDO> controlled=store.all(p.getId(),"ROOT");Set<Long> anchorIds=new HashSet<>();for(HrmPayrollGovernanceRecordDO root:controlled)anchorIds.add(id(store.data(root).get("anchorId")));snapshot.put("controlledRoots",controlled);snapshot.put("controlledAnchors",store.all(0L,"ANCHOR").stream().filter(x->anchorIds.contains(x.getId())).collect(Collectors.toList()));snapshot.put("projectGrants",deps.get("grants"));
        snapshot.put("decisionVersions",store.all(p.getId(),"DECISION").stream().map(r->map("recordId",r.getId(),"recordVersion",r.getVersion(),"lifecycleAtAction",decisions.lifecycle(store.data(r)),"decision",store.data(r))).collect(Collectors.toList()));
        List<Object> relationFacts=new ArrayList<>();for(HrmPayrollWorkbenchRecordDO node:nodes(p.getId()))if("RELATION".equals(node.getKind())){Map<String,Object> rel=JsonUtils.parseObject(node.getDataJson(),Map.class);if(Boolean.TRUE.equals(rel.get("active")))relationFacts.add(map("relationId",node.getId(),"requirementId",rel.get("requirementId"),"sourceId",rel.get("sourceId"),"available",workbench.available(p,node)));}snapshot.put("relationFacts",relationFacts);
        Set<Long> people=new HashSet<>(Arrays.asList(actor,p.getOwnerId()));for(HrmPayrollRequirementDO req:frozenRows){people.add(req.getReviewerId());people.add(req.getSourceOwnerId());}for(HrmPayrollRequirementProjectGrantDO g:grants.selectList(new LambdaQueryWrapperX<HrmPayrollRequirementProjectGrantDO>().eq(HrmPayrollRequirementProjectGrantDO::getProjectId,p.getId()))){people.add(g.getUserId());people.add(g.getGrantorId());}for(HrmPayrollWorkbenchRecordDO node:nodes(p.getId())){Map<String,Object> nd=JsonUtils.parseObject(node.getDataJson(),Map.class);for(String key:Arrays.asList("ownerId","handlerId","reviewerId","actorId"))if(nd.get(key)!=null)people.add(id(nd.get(key)));}snapshot.put("userFacts",users.stream().filter(u->people.contains(id(u.get("id")))).collect(Collectors.toList()));
        Map<String,Object>d=map("snapshot",snapshot,"draft",c.getDraft(),"requestDigest",requestDigest,"createdBy",actor,"createdAt",now(),"reason",c.getReason(),"mode",p.getProjectKey().startsWith("TEST_")?"TEST合成验证，非正式业务批准":"FORMAL","version",store.all(p.getId(),"RELEASE").size()+1,"publicationAuthority",c.getDraft()?Collections.emptyList():roots.authority(p.getId(),actor,"BASELINE_PUBLISH",p.getScopeJson()));
        HrmPayrollGovernanceRecordDO release=store.insert(p.getId(),"RELEASE",c.getRequestKey(),d);store.event(release,c.getDraft()?"DRAFT_SNAPSHOT":"BASELINE_PUBLISH",actor,c.getReason());return release.getId();
    }
    public List<Object> list(Long project,Long actor){HrmPayrollRequirementProjectDO p=project(project,actor);return store.all(project,"RELEASE").stream().map(r->{Map<String,Object>d=store.data(r);return map("id",r.getId(),"version",d.get("version"),"draft",d.get("draft"),"createdAt",d.get("createdAt"),"mode",d.get("mode"));}).collect(Collectors.toList());}
}
