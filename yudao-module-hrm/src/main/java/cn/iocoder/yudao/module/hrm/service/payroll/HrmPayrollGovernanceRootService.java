package cn.iocoder.yudao.module.hrm.service.payroll;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollGovernanceVO.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.*;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.context.annotation.Lazy;
import javax.annotation.Resource;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import static cn.iocoder.yudao.module.hrm.service.payroll.HrmPayrollGovernanceStore.*;

/** The ANCHOR is an independently registered controlled copy, never an API-created trust root.
 * Migration creates no anchors. TEST anchors cannot authorize non-TEST projects. */
@Service
public class HrmPayrollGovernanceRootService {
    @Resource private HrmPayrollGovernanceStore store;
    @Resource private HrmPayrollRequirementProjectMapper projects;
    @Resource private HrmPayrollRequirementProjectGrantMapper grants;
    @Resource private HrmPayrollRequirementMapper requirements;
    @Resource private HrmPayrollWorkbenchRecordMapper workbench;
    @Resource @Lazy private HrmPayrollRequirementAccessService access;
    @Resource private AdminUserApi users;
    @Resource private PermissionApi permissions;
    public boolean canPlatform(Long actor,String action){return permissions.hasAnyPermissions(actor,"hrm:payroll:requirement:query")&&permissions.hasAnyPermissions(actor,"hrm:payroll:requirement:"+action);}
    void platform(Long actor,String action){allowed(canPlatform(actor,action));}
    public void requireProjectMode(Long rootId,String key){Map<String,Object>d=store.data(store.record(rootId,"ROOT"));allowed(!"TEST".equals(d.get("mode"))||key.startsWith("TEST_"));}
    public Map<String,Object> userFact(Long actor){cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO u=actor==null?null:users.getUser(actor);return u==null?map("id",actor,"status","unknown"):map("id",u.getId(),"nickname",u.getNickname(),"status",u.getStatus());}
    boolean user(Long actor){return actor!=null&&users.getUser(actor)!=null&&Objects.equals(users.getUser(actor).getStatus(),0);}
    @SuppressWarnings("unchecked") Set<String> strings(Object x){return x==null?Collections.emptySet():new HashSet<>((Collection<String>)x);}
    private Map<String,Object> anchor(Long id){Map<String,Object>a=store.data(store.record(id,"ANCHOR"));allowed("REGISTERED".equals(a.get("state"))&&within(a)&&!blank(a.get("body"))&&Objects.equals(a.get("digest"),bodyDigest(String.valueOf(a.get("body")))));return a;}
    public List<Object> anchors(Long actor){allowed(user(actor));if(!canPlatform(actor,"root-register")&&!canPlatform(actor,"root-sign"))return Collections.emptyList();return store.all(0L,"ANCHOR").stream().filter(r->{Map<String,Object>a=store.data(r);return Objects.equals(actor,id(a.get("custodianId")))||Objects.equals(actor,id(a.get("signerId")));}).map(r->map("id",r.getId(),"data",store.data(r))).collect(Collectors.toList());}
    private boolean scopeContains(Object parent,String child){try{return HrmPayrollRequirementScope.parse(String.valueOf(parent)).contains(HrmPayrollRequirementScope.parse(child));}catch(RuntimeException invalid){return false;}}
    public boolean validRoot(Long rootId,HrmPayrollRequirementProjectGrantDO grant){
        try{HrmPayrollGovernanceRecordDO r=store.record(rootId,"ROOT");Map<String,Object>d=store.data(r),a=anchor(id(d.get("anchorId")));
            if(!"ACTIVE".equals(d.get("state"))||!within(d)||!user(id(d.get("signedBy")))||!user(id(a.get("custodianId")))||!Objects.equals(d.get("digest"),bodyDigest(String.valueOf(d.get("body")))))return false;
            if("TEST".equals(a.get("mode"))&&grant.getProjectId()!=0){HrmPayrollRequirementProjectDO p=projects.selectById(grant.getProjectId());if(p==null||!p.getProjectKey().startsWith("TEST_"))return false;}
            if(!Objects.equals(r.getProjectId(),grant.getProjectId()))return false;
            return strings(d.get("actions")).containsAll(access.actions(grant.getActions()))&&scopeContains(d.get("scopeJson"),grant.getCeilingJson())&&grant.getValidFrom()!=null&&grant.getValidTo()!=null&&!grant.getValidFrom().isBefore(LocalDate.parse(String.valueOf(d.get("validFrom"))))&&!grant.getValidTo().isAfter(LocalDate.parse(String.valueOf(d.get("validTo"))));
        }catch(RuntimeException invalid){return false;}
    }
    @Transactional(rollbackFor=Exception.class,isolation=Isolation.READ_COMMITTED)
    public Long register(Root c,Long actor){
        store.lock();platform(actor,"root-register");Map<String,Object>a=anchor(c.getAnchorId());allowed(user(actor)&&Objects.equals(actor,id(a.get("custodianId")))&&user(c.getUserId()));
        Set<String> actions=new HashSet<>(c.getActions());Set<String> known=new HashSet<>(HrmPayrollRequirementAccessService.ACTIONS);known.add("PROJECT_CREATE");need(known.containsAll(actions)&&actions.containsAll(c.getDelegableActions())&&strings(a.get("actions")).containsAll(actions));
        need(scopeContains(a.get("scopeJson"),c.getScopeJson()));LocalDate from=LocalDate.parse(c.getValidFrom()),to=LocalDate.parse(c.getValidTo());need(!to.isBefore(from)&&!from.isBefore(LocalDate.parse(String.valueOf(a.get("validFrom"))))&&!to.isAfter(LocalDate.parse(String.valueOf(a.get("validTo")))));
        if(c.getProjectId()!=0){HrmPayrollRequirementProjectDO p=projects.selectById(c.getProjectId());allowed(p!=null&&(!"TEST".equals(a.get("mode"))||p.getProjectKey().startsWith("TEST_")));}
        else need(actions.equals(Collections.singleton("PROJECT_CREATE")));
        Map<String,Object>d=JsonUtils.parseObject(JsonUtils.toJsonString(c),Map.class);d.putAll(map("state","PENDING_SIGNATURE","registeredBy",actor,"registeredByFact",userFact(actor),"registeredAt",now(),"digest",bodyDigest(c.getBody()),"mode",a.get("mode"),"anchorVersion",a.get("materialVersion"),"designatedSignerId",a.get("signerId")));
        HrmPayrollGovernanceRecordDO r=store.insert(c.getProjectId(),"ROOT",UUID.randomUUID().toString(),d);store.event(r,"ROOT_REGISTER",actor,c.getReason());return r.getId();
    }
    @Transactional(rollbackFor=Exception.class,isolation=Isolation.READ_COMMITTED)
    public void action(Action c,Long actor){
        store.lock();platform(actor,"root-sign");HrmPayrollGovernanceRecordDO r=store.record(c.getId(),"ROOT");allowed(Objects.equals(r.getProjectId(),c.getProjectId()));store.version(r,c.getVersion());Map<String,Object>d=store.data(r),a=anchor(id(d.get("anchorId")));
        allowed(user(actor)&&Objects.equals(actor,id(a.get("signerId")))&&!Objects.equals(actor,id(d.get("registeredBy"))));
        if("SIGN".equals(c.getAction())){need("PENDING_SIGNATURE".equals(d.get("state")));d.putAll(map("state","ACTIVE","signedBy",actor,"signedByFact",userFact(actor),"signedAt",now(),"signatureReason",c.getReason(),"anchorFact",a));store.state(r,d);
            HrmPayrollRequirementProjectGrantDO g=new HrmPayrollRequirementProjectGrantDO();g.setProjectId(r.getProjectId());g.setRootRecordId(r.getId());g.setUserId(id(d.get("userId")));g.setGrantorId(actor);g.setGrantVersion(1);g.setActions(String.join(",",strings(d.get("actions"))));g.setDelegableActions(String.join(",",strings(d.get("delegableActions"))));g.setFullProject(true);g.setCeilingJson(String.valueOf(d.get("scopeJson")));g.setValidFrom(LocalDate.parse(String.valueOf(d.get("validFrom"))));g.setValidTo(LocalDate.parse(String.valueOf(d.get("validTo"))));g.setEvidence("CONTROLLED_ROOT:"+r.getId()+":"+d.get("materialVersion")+":"+d.get("digest"));g.setAuthorityFactsJson(JsonUtils.toJsonString(map("grantorAtGrant",userFact(actor),"recipientAtGrant",userFact(g.getUserId()),"rootId",r.getId(),"rootVersion",r.getVersion(),"rootBodyDigest",d.get("digest"),"grantedAt",now())));grants.insert(g);
        }else{need("REVOKE".equals(c.getAction())&&"ACTIVE".equals(d.get("state")));d.putAll(map("state","REVOKED","revokedBy",actor,"revokedAt",now(),"revocationReason",c.getReason()));store.state(r,d);}
        store.event(r,"ROOT_"+c.getAction(),actor,c.getReason());
    }
    public List<Object> list(Long project,Long actor){return store.all(project,"ROOT").stream().filter(r->{Map<String,Object>d=store.data(r);if(access.full(projects.selectById(project),actor,"GRANT_READ")&&canPlatform(actor,"grant-read"))return true;try{Map<String,Object>a=anchor(id(d.get("anchorId")));return (canPlatform(actor,"root-register")||canPlatform(actor,"root-sign"))&&(Objects.equals(actor,id(a.get("custodianId")))||Objects.equals(actor,id(a.get("signerId"))));}catch(RuntimeException denied){return false;}}).map(r->map("id",r.getId(),"version",r.getVersion(),"data",store.data(r))).collect(Collectors.toList());}
    /** Complete current authority chain; result is internal and must be separately authorized on read/export. */
    public List<Object> authority(Long project,Long actor,String action,String scope){
        return authority(project,actor,action,scope,null,null);
    }
    public List<Object> authority(Long project,Long actor,String action,String scope,Long requirementId,Long sourceId){
        HrmPayrollRequirementProjectDO p=projects.selectById(project);allowed(p!=null&&user(actor));
        if(requirementId!=null){HrmPayrollRequirementDO req=requirements.selectById(requirementId);allowed(req!=null&&Objects.equals(req.getProjectId(),project));scope=req.getScopeJson();}
        for(HrmPayrollRequirementProjectGrantDO g:access.active(project,actor)){
            if(!access.actions(g.getActions()).contains(action)||!scopeContains(g.getCeilingJson(),scope))continue;
            if(requirementId!=null){HrmPayrollRequirementDO req=requirements.selectById(requirementId);if(req==null||!access.coversRow(g,req,p))continue;}
            else if(sourceId!=null){HrmPayrollWorkbenchRecordDO source=workbench.selectById(sourceId);if(source==null||!Objects.equals(source.getProjectId(),project)||!"SOURCE".equals(source.getKind()))continue;Map<String,Object> sd=JsonUtils.parseObject(source.getDataJson(),Map.class);if(g.getModulesJson()!=null&&!JsonUtils.parseArray(g.getModulesJson(),String.class).contains(String.valueOf(sd.get("module"))))continue;Set<Long> ids=g.getSourceIdsJson()==null?Collections.emptySet():new HashSet<>(JsonUtils.parseArray(g.getSourceIdsJson(),Long.class));if(!ids.contains(sourceId)&&(!ids.isEmpty()||!Boolean.TRUE.equals(g.getFullProject())||g.getModulesJson()!=null||g.getRequirementIdsJson()!=null))continue;}
            else if(!Boolean.TRUE.equals(g.getFullProject())||g.getModulesJson()!=null||g.getRequirementIdsJson()!=null||g.getSourceIdsJson()!=null)continue;
            List<Object> chain=new ArrayList<>();Set<Long> seen=new HashSet<>();HrmPayrollRequirementProjectGrantDO cur=g;
            while(cur!=null&&seen.add(cur.getId())&&seen.size()<=32){if(cur.getRootRecordId()!=null)allowed(!blank(cur.getAuthorityFactsJson()));cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO u=users.getUser(cur.getUserId());chain.add(map("grant",cur,"account",map("id",u.getId(),"nickname",u.getNickname(),"status",u.getStatus()),"accountActive",user(cur.getUserId()),"effectiveAt",now()));if(cur.getParentGrantId()==null)break;cur=grants.selectById(cur.getParentGrantId());}
            if(cur!=null&&cur.getRootRecordId()!=null&&validRoot(cur.getRootRecordId(),cur)){HrmPayrollGovernanceRecordDO root=store.record(cur.getRootRecordId(),"ROOT");chain.add(map("rootId",root.getId(),"rootVersion",root.getVersion(),"root",store.data(root)));return chain;}
        }
        allowed(false);return Collections.emptyList();
    }
}
