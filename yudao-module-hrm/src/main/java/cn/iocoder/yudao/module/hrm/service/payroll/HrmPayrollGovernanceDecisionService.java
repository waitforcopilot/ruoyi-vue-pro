package cn.iocoder.yudao.module.hrm.service.payroll;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollGovernanceVO.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import javax.annotation.Resource;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import static cn.iocoder.yudao.module.hrm.service.payroll.HrmPayrollGovernanceStore.*;

@Service
public class HrmPayrollGovernanceDecisionService {
    public static final List<String> REQUIRED=Arrays.asList("Q-01","Q-02","Q-04","Q-05","Q-07","Q-10");
    public static final List<String> BEFORE_CONFIRM=Arrays.asList("Q-02","Q-04","Q-05");
    @Resource private HrmPayrollGovernanceStore store;
    @Resource private HrmPayrollGovernanceRootService roots;
    @Resource private HrmPayrollRequirementProjectMapper projects;
    @Resource private HrmPayrollRequirementMapper requirements;
    @Resource private HrmPayrollRequirementHistoryMapper histories;
    @Resource private HrmPayrollRequirementAccessService access;
    HrmPayrollRequirementProjectDO project(Long id){HrmPayrollRequirementProjectDO p=projects.selectById(id);allowed(p!=null);return p;}
    boolean contains(Object scope,String requested){return HrmPayrollRequirementScope.parse(String.valueOf(scope)).contains(HrmPayrollRequirementScope.parse(requested));}
    boolean overlaps(Object aa,Object bb){HrmPayrollRequirementScope a=HrmPayrollRequirementScope.parse(String.valueOf(aa)),b=HrmPayrollRequirementScope.parse(String.valueOf(bb));return !Collections.disjoint(a.getEntities(),b.getEntities())&&!Collections.disjoint(a.getOrganizations(),b.getOrganizations())&&!Collections.disjoint(a.getWorkerTypes(),b.getWorkerTypes())&&!Collections.disjoint(a.getScenarios(),b.getScenarios())&&!a.getFrom().isAfter(b.getTo())&&!b.getFrom().isAfter(a.getTo());}
    private Clock clock=Clock.system(ZONE);
    static LocalDateTime start(Object value){String v=String.valueOf(value);return v.length()==10?LocalDate.parse(v).atStartOfDay():LocalDateTime.parse(v);}
    static LocalDateTime end(Object value){String v=String.valueOf(value);return v.length()==10?LocalDate.parse(v).atTime(LocalTime.MAX):LocalDateTime.parse(v);}
    public String lifecycle(Map<String,Object>d){if(!"APPROVED".equals(d.get("state")))return String.valueOf(d.get("state"));LocalDateTime today=LocalDateTime.now(clock);if(d.get("replacementFrom")!=null&&!today.isBefore(start(d.get("replacementFrom"))))return "REPLACED";if(today.isBefore(start(d.get("validFrom"))))return "FUTURE";if(today.isAfter(end(d.get("validTo"))))return "EXPIRED";return "EFFECTIVE";}
    @SuppressWarnings("unchecked") Set<String> questions(Map<String,Object>d){return new TreeSet<>((List<String>)d.get("questions"));}
    @Transactional(rollbackFor=Exception.class,isolation=Isolation.READ_COMMITTED)
    public Long draft(Decision c,Long actor){
        store.lock();roots.platform(actor,"decision-edit");HrmPayrollRequirementProjectDO p=project(c.getProjectId());allowed(access.full(p,actor,"READ")&&access.full(p,actor,"DECISION_EDIT"));need(Objects.equals(p.getProjectScopeVersion(),c.getProjectScopeVersion())&&contains(p.getScopeJson(),c.getScopeJson()));
        List<Object> authority=roots.authority(p.getId(),actor,"DECISION_EDIT",c.getScopeJson());need(roots.user(c.getApproverId())&&!Objects.equals(actor,c.getApproverId()));
        Set<String> validQuestions=new HashSet<>();for(int i=1;i<=10;i++)validQuestions.add(String.format("Q-%02d",i));need(validQuestions.containsAll(c.getQuestions())&&new HashSet<>(c.getQuestions()).size()==c.getQuestions().size());
        LocalDateTime f=start(c.getValidFrom()),t=end(c.getValidTo());need(!t.isBefore(f));
        String family=UUID.randomUUID().toString();int version=1;
        if(c.getPreviousId()!=null){HrmPayrollGovernanceRecordDO old=store.record(c.getPreviousId(),"DECISION");allowed(Objects.equals(old.getProjectId(),p.getId()));Map<String,Object>od=store.data(old);family=String.valueOf(od.get("decisionId"));version=store.all(p.getId(),"DECISION").stream().map(store::data).filter(x->familyEquals(x,od)).mapToInt(x->id(x.get("decisionVersion")).intValue()).max().orElse(0)+1;}
        Map<String,Object>d=JsonUtils.parseObject(JsonUtils.toJsonString(c),Map.class);d.putAll(map("decisionId",family,"decisionVersion",version,"state","DRAFT","createdBy",actor,"createdAt",now(),"digest",bodyDigest(c.getBody()),"draftAuthority",authority));
        if(c.getPreviousId()!=null){Map<String,Object>prev=store.data(store.record(c.getPreviousId(),"DECISION"));if(!"APPROVED".equals(prev.get("state")))d.put("previousId",prev.get("previousId"));}
        HrmPayrollGovernanceRecordDO r=store.insert(p.getId(),"DECISION",family+":"+version,d);store.event(r,"DECISION_DRAFT",actor,c.getReason());return r.getId();
    }
    boolean familyEquals(Map<String,Object>a,Map<String,Object>b){return Objects.equals(a.get("decisionId"),b.get("decisionId"));}
    @Transactional(rollbackFor=Exception.class,isolation=Isolation.READ_COMMITTED)
    public void action(Action c,Long actor){
        store.lock();HrmPayrollGovernanceRecordDO r=store.record(c.getId(),"DECISION");allowed(Objects.equals(r.getProjectId(),c.getProjectId()));store.version(r,c.getVersion());Map<String,Object>d=store.data(r);HrmPayrollRequirementProjectDO p=project(r.getProjectId());
        need(Objects.equals(String.valueOf(p.getProjectScopeVersion()),String.valueOf(d.get("projectScopeVersion"))));
        if("SUBMIT".equals(c.getAction())){roots.platform(actor,"decision-edit");allowed(Objects.equals(actor,id(d.get("createdBy"))));roots.authority(p.getId(),actor,"DECISION_EDIT",String.valueOf(d.get("scopeJson")));need("DRAFT".equals(d.get("state")));d.put("state","PENDING_APPROVAL");}
        else{
            roots.platform(actor,"decision-approve");List<Object>a=roots.authority(p.getId(),actor,"DECISION_APPROVE",String.valueOf(d.get("scopeJson")));
            allowed(access.full(p,actor,"READ")&&access.full(p,actor,"DECISION_READ")&&access.full(p,actor,"GRANT_READ"));
            if("APPROVE".equals(c.getAction())){
                allowed(Objects.equals(actor,id(d.get("approverId")))&&!Objects.equals(actor,id(d.get("createdBy"))));need("PENDING_APPROVAL".equals(d.get("state")));need(Objects.equals(d.get("digest"),bodyDigest(String.valueOf(d.get("body")))));
                HrmPayrollGovernanceRecordDO previous=d.get("previousId")==null?null:store.record(id(d.get("previousId")),"DECISION");
                if(previous!=null){need(Objects.equals(previous.getProjectId(),p.getId()));Map<String,Object>old=store.data(previous);need("APPROVED".equals(old.get("state"))&&old.get("replacementId")==null&&familyEquals(old,d));}
                for(HrmPayrollGovernanceRecordDO other:store.all(p.getId(),"DECISION")){
                    if(other.getId().equals(r.getId())||previous!=null&&other.getId().equals(previous.getId()))continue;
                    Map<String,Object>o=store.data(other);if(!"APPROVED".equals(o.get("state"))||!Objects.equals(String.valueOf(o.get("projectScopeVersion")),String.valueOf(d.get("projectScopeVersion")))||Collections.disjoint(questions(o),questions(d))||!overlaps(o.get("scopeJson"),d.get("scopeJson")))continue;
                    LocalDateTime until=end(o.get("validTo"));if(o.get("replacementFrom")!=null)until=start(o.get("replacementFrom")).minusNanos(1);
                    need(start(d.get("validFrom")).isAfter(until)||start(o.get("validFrom")).isAfter(end(d.get("validTo"))));
                }
                d.putAll(map("state","APPROVED","approvedBy",actor,"approvedAt",now(),"approvalReason",c.getReason(),"approvalAuthority",a));
                if(previous!=null){Map<String,Object>old=store.data(previous);old.putAll(map("replacementId",r.getId(),"replacementFrom",d.get("validFrom")));store.state(previous,old);store.event(previous,"DECISION_REPLACEMENT_SCHEDULED",actor,c.getReason());}
            }else{need("REVOKE".equals(c.getAction())&&"APPROVED".equals(d.get("state")));d.putAll(map("state","REVOKED","revokedBy",actor,"revokedAt",now(),"revocationReason",c.getReason(),"revocationAuthority",a));
                if(d.get("previousId")!=null&&LocalDateTime.now(clock).isBefore(start(d.get("validFrom")))){HrmPayrollGovernanceRecordDO old=store.record(id(d.get("previousId")),"DECISION");Map<String,Object>od=store.data(old);if(Objects.equals(String.valueOf(od.get("replacementId")),String.valueOf(r.getId()))){od.remove("replacementId");od.remove("replacementFrom");store.state(old,od);store.event(old,"FUTURE_REPLACEMENT_CANCELLED",actor,c.getReason());}}
            }
        }
        store.state(r,d);store.event(r,"DECISION_"+c.getAction(),actor,c.getReason());refreshSemantics(p.getId(),actor);
    }
    /** Unique coverage is proved for each clause and requested scope, never first-match selection. */
    public List<Object> current(Long project,String scope,List<String> required){
        HrmPayrollRequirementProjectDO p=project(project);List<HrmPayrollGovernanceRecordDO> valid=store.all(project,"DECISION").stream().filter(r->{Map<String,Object>d=store.data(r);return "EFFECTIVE".equals(lifecycle(d))&&Objects.equals(String.valueOf(p.getProjectScopeVersion()),String.valueOf(d.get("projectScopeVersion")))&&contains(d.get("scopeJson"),scope);}).collect(Collectors.toList());
        Set<Long> used=new LinkedHashSet<>();for(String q:required){List<HrmPayrollGovernanceRecordDO> covering=valid.stream().filter(r->questions(store.data(r)).contains(q)).collect(Collectors.toList());need(covering.size()==1);used.add(covering.get(0).getId());}
        return valid.stream().filter(r->used.contains(r.getId())).map(r->map("recordId",r.getId(),"recordVersion",r.getVersion(),"lifecycleAtAction","EFFECTIVE","decision",store.data(r))).collect(Collectors.toList());
    }
    @SuppressWarnings("unchecked") public String semantic(Long project,String scope){List<Object> selected=current(project,scope,Collections.singletonList("Q-05"));Map<String,Object>d=(Map<String,Object>)((Map<?,?>)selected.get(0)).get("decision");return digest(map("body",d.get("body"),"clauses",d.get("clauses"),"clauseVersion",d.get("clauseVersion")));}
    public Map<String,Object> forAction(Long project,Long actor,String action,String scope){return map("decisions",current(project,scope,BEFORE_CONFIRM),"authority",roots.authority(project,actor,action,scope),"policySemantic",semantic(project,scope),"checkedAt",now());}
    public Map<String,Object> forRequirement(HrmPayrollRequirementDO r,Long actor){return map("decisions",current(r.getProjectId(),r.getScopeJson(),BEFORE_CONFIRM),"authority",roots.authority(r.getProjectId(),actor,"CONFIRM",r.getScopeJson(),r.getId(),null),"policySemantic",semantic(r.getProjectId(),r.getScopeJson()),"checkedAt",now());}
    public Map<String,Object> forVerification(Long project,Long actor,String scope,Long requirement,Long source){List<Object>a=new ArrayList<>();a.addAll(roots.authority(project,actor,"SOURCE_VERIFY",scope,null,source));if(requirement!=null)a.addAll(roots.authority(project,actor,"SOURCE_VERIFY",scope,requirement,null));return map("decisions",current(project,scope,BEFORE_CONFIRM),"authority",a,"checkedAt",now());}
    public Map<String,Object> forIssue(Long project,Long actor,String scope,Set<Long> ids){List<Object>a=new ArrayList<>();for(Long id:ids)a.addAll(roots.authority(project,actor,"ISSUE_REVIEW",scope,id,null));return map("decisions",current(project,scope,BEFORE_CONFIRM),"authority",a,"checkedAt",now());}
    @SuppressWarnings("unchecked") public boolean currentSemantic(HrmPayrollRequirementDO r){try{Map<String,Object>tuple=JsonUtils.parseObject(r.getConfirmedTupleJson(),Map.class);if(!(tuple.get("governance") instanceof Map))return true;return Objects.equals(((Map<?,?>)tuple.get("governance")).get("policySemantic"),semantic(r.getProjectId(),r.getScopeJson()));}catch(RuntimeException incomplete){return false;}}
    /** Delayed effective rule changes are evaluated under the same mutex before a new action. */
    @SuppressWarnings("unchecked") public void refreshSemantics(Long project,Long actor){
        for(HrmPayrollRequirementDO r:requirements.selectList(new LambdaQueryWrapperX<HrmPayrollRequirementDO>().eq(HrmPayrollRequirementDO::getProjectId,project))){
            if(!"CONFIRMED".equals(r.getStatus())||r.getConfirmedTupleJson()==null)continue;
            Map<String,Object>tuple=JsonUtils.parseObject(r.getConfirmedTupleJson(),Map.class);if(!(tuple.get("governance") instanceof Map))continue;
            String semantic;try{semantic=semantic(project,r.getScopeJson());}catch(RuntimeException absent){continue;}
            if(Objects.equals(semantic,((Map<?,?>)tuple.get("governance")).get("policySemantic")))continue;
            r.setStatus("REVIEWING");r.setSemanticVersion(r.getSemanticVersion()==null?2:r.getSemanticVersion()+1);r.setVersion(r.getVersion()+1);r.setConfirmedBy(null);r.setConfirmedAt(null);r.setConfirmedScopeVersion(null);r.setConfirmedScopeJson(null);r.setConfirmedTupleJson(null);
            requirements.updateById(r);requirements.update(null,new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<HrmPayrollRequirementDO>().eq(HrmPayrollRequirementDO::getId,r.getId()).set(HrmPayrollRequirementDO::getConfirmedBy,null).set(HrmPayrollRequirementDO::getConfirmedAt,null).set(HrmPayrollRequirementDO::getConfirmedScopeVersion,null).set(HrmPayrollRequirementDO::getConfirmedScopeJson,null).set(HrmPayrollRequirementDO::getConfirmedTupleJson,null));
            HrmPayrollRequirementHistoryDO h=new HrmPayrollRequirementHistoryDO();h.setRequirementId(r.getId());h.setVersion(r.getVersion());h.setActorId(actor);h.setSnapshot(JsonUtils.toJsonString(r));histories.insert(h);
        }
    }
    public List<Object> list(Long project,Long actor){HrmPayrollRequirementProjectDO p=project(project);roots.platform(actor,"decision-read");allowed(access.full(p,actor,"READ")&&access.full(p,actor,"DECISION_READ"));return store.all(project,"DECISION").stream().map(r->{Map<String,Object>d=store.data(r);if(!access.full(p,actor,"GRANT_READ")||!roots.canPlatform(actor,"grant-read")){d.remove("draftAuthority");d.remove("approvalAuthority");d.remove("revocationAuthority");d.put("authorityRestricted",true);}return map("id",r.getId(),"version",r.getVersion(),"lifecycle",lifecycle(d),"data",d);}).collect(Collectors.toList());}
    public List<Object> history(Long project,Long id,Long actor){list(project,actor);HrmPayrollGovernanceRecordDO target=store.record(id,"DECISION");allowed(Objects.equals(target.getProjectId(),project));boolean full=roots.canPlatform(actor,"grant-read")&&access.full(project(project),actor,"GRANT_READ")&&access.full(project(project),actor,"PERSON_READ");return store.all(project,"EVENT").stream().map(store::data).filter(d->Objects.equals(HrmPayrollGovernanceStore.id(d.get("objectId")),id)).map(d->full?(Object)d:map("action",d.get("action"),"version",d.get("version"),"time",d.get("time"),"authority","受限发布依据")).collect(Collectors.toList());}
}
