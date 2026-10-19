package cn.iocoder.yudao.module.hrm.service.payroll;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.*;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;

/** Current database authorization only. No role/admin bypass and no cached grant decisions. */
@Service
public class HrmPayrollRequirementAccessService {
    public static final Set<String> ACTIONS = new HashSet<>(Arrays.asList("READ","META","EDIT","ASSIGN","CONFIRM","EXPORT","MEMBER","SCOPE","SOURCE_EDIT","SOURCE_VERIFY","ISSUE_MANAGE","ISSUE_HANDLE","ISSUE_REVIEW"));
    /** project_id=0 is a tenant-local creation authority, never a project membership. */
    public HrmPayrollRequirementProjectGrantDO creationAuthority(Long actor,String scopeJson) {
        HrmPayrollRequirementScope requested=HrmPayrollRequirementScope.parse(scopeJson);
        return active(0L,actor).stream().filter(g->g.getParentGrantId()==null && actions(g.getActions()).contains("PROJECT_CREATE")
            && g.getEvidence()!=null && !g.getEvidence().trim().isEmpty() && g.getCeilingJson()!=null
            && HrmPayrollRequirementScope.parse(g.getCeilingJson()).contains(requested))
            .findFirst().orElseThrow(()->exception(PAYROLL_PROJECT_DENIED));
    }
    @Resource private HrmPayrollRequirementProjectGrantMapper grants;
    @Resource private AdminUserApi users;
    @Resource private PermissionApi permissions;
    public boolean activeUser(Long actor) {
        cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO u=actor==null?null:users.getUser(actor);
        return u!=null && Objects.equals(u.getStatus(),0);
    }
    public Set<String> actions(String value) {
        if(value==null || value.trim().isEmpty()) return Collections.emptySet();
        return new HashSet<>(Arrays.asList(value.split(",")));
    }
    private boolean creator(HrmPayrollRequirementProjectDO p,Long actor,String action) {
        return false; // Creator access is an explicit, revocable bootstrap grant.
    }
    public boolean effective(HrmPayrollRequirementProjectGrantDO g) {return effective(g,new HashSet<>());}
    private boolean effective(HrmPayrollRequirementProjectGrantDO g,Set<Long> seen) {
        LocalDate now=LocalDate.now(ZoneId.of("Asia/Shanghai"));
        if(g==null || g.getId()==null || !seen.add(g.getId()) || seen.size()>32 || g.getRevokedAt()!=null || !activeUser(g.getUserId()) || g.getValidFrom()==null || g.getValidTo()==null || now.isBefore(g.getValidFrom()) || now.isAfter(g.getValidTo())) return false;
        if(g.getParentGrantId()==null) return true; // Root registration is external to member management.
        HrmPayrollRequirementProjectGrantDO parent=grants.selectById(g.getParentGrantId());
        return parent!=null && Objects.equals(parent.getProjectId(),g.getProjectId()) && Objects.equals(parent.getUserId(),g.getGrantorId()) && effective(parent,seen) && coversGrant(parent,g);
    }
    public List<HrmPayrollRequirementProjectGrantDO> active(Long project,Long actor) {
        if(!activeUser(actor)) return Collections.emptyList();
        return grants.selectList(new cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX<HrmPayrollRequirementProjectGrantDO>().eq(HrmPayrollRequirementProjectGrantDO::getProjectId,project).eq(HrmPayrollRequirementProjectGrantDO::getUserId,actor)).stream().filter(this::effective).collect(Collectors.toList());
    }
    public boolean project(HrmPayrollRequirementProjectDO p,Long actor,String action) {
        return p!=null && activeUser(actor) && (creator(p,actor,action) || active(p.getId(),actor).stream().anyMatch(g->actions(g.getActions()).contains(action)));
    }
    private <T> Set<T> set(String json,Class<T> type) {
        return json==null ? Collections.emptySet() : new HashSet<>(JsonUtils.parseArray(json,type));
    }
    public boolean row(HrmPayrollRequirementProjectDO p,HrmPayrollRequirementDO r,Long actor,String action) {
        if(p==null || r==null || !Objects.equals(p.getId(),r.getProjectId()) || !activeUser(actor)) return false;
        if(creator(p,actor,action)) return true;
        return active(p.getId(),actor).stream().anyMatch(g->actions(g.getActions()).contains(action) && coversRow(g,r,p));
    }
    public boolean coversRow(HrmPayrollRequirementProjectGrantDO g,HrmPayrollRequirementDO r,HrmPayrollRequirementProjectDO p) {
        try {
            Set<String> modules=set(g.getModulesJson(),String.class);Set<Long> ids=set(g.getRequirementIdsJson(),Long.class);
            if(!modules.isEmpty() && !modules.contains(r.getModule()))return false;
            if(!ids.isEmpty() && !ids.contains(r.getId()))return false;
            if(ids.isEmpty() && !set(g.getSourceIdsJson(),Long.class).isEmpty())return false;
            return (r.getScopeJson()==null && p.getScopeJson()==null && Boolean.TRUE.equals(g.getFullProject())) || (g.getCeilingJson()==null && p.getScopeJson()==null) || (r.getScopeJson()!=null && g.getCeilingJson()!=null && HrmPayrollRequirementScope.parse(g.getCeilingJson()).contains(HrmPayrollRequirementScope.parse(r.getScopeJson())));
        } catch(RuntimeException invalid) {return false;}
    }
    public boolean full(HrmPayrollRequirementProjectDO p,Long actor,String action) {
        if(p==null || !activeUser(actor))return false;
        if(creator(p,actor,action))return true;
        return active(p.getId(),actor).stream().anyMatch(g->actions(g.getActions()).contains(action) && !Boolean.FALSE.equals(g.getFullProject()) && set(g.getModulesJson(),String.class).isEmpty() && set(g.getRequirementIdsJson(),Long.class).isEmpty() && set(g.getSourceIdsJson(),Long.class).isEmpty() && (p.getScopeJson()==null || (g.getCeilingJson()!=null && HrmPayrollRequirementScope.parse(g.getCeilingJson()).contains(HrmPayrollRequirementScope.parse(p.getScopeJson())))));
    }
    public void requireRow(HrmPayrollRequirementProjectDO p,HrmPayrollRequirementDO r,Long actor,String action) {
        if(!row(p,r,actor,"READ") || !row(p,r,actor,action))throw exception(PAYROLL_PROJECT_DENIED);
    }
    /** Source authorization is independent of requirement traversal. Narrow readers need explicit source IDs. */
    public boolean source(HrmPayrollRequirementProjectDO p,Long sourceId,String module,String scope,Long actor,String action) {
        if(p==null || scope==null || !activeUser(actor))return false;
        return active(p.getId(),actor).stream().anyMatch(g->{
            Set<Long> ids=set(g.getSourceIdsJson(),Long.class);
            if(!actions(g.getActions()).contains(action))return false;
            if(ids.isEmpty() && (!Boolean.TRUE.equals(g.getFullProject()) || !set(g.getRequirementIdsJson(),Long.class).isEmpty() || !set(g.getModulesJson(),String.class).isEmpty()))return false;
            if(!ids.isEmpty() && (sourceId==null || !ids.contains(sourceId)))return false;
            Set<String> modules=set(g.getModulesJson(),String.class);
            try {return (modules.isEmpty() || modules.contains(module)) && g.getCeilingJson()!=null && HrmPayrollRequirementScope.parse(g.getCeilingJson()).contains(HrmPayrollRequirementScope.parse(scope));}
            catch(RuntimeException invalid){return false;}
        });
    }
    public boolean reviewer(HrmPayrollRequirementProjectDO p,HrmPayrollRequirementDO r,Long user) {
        return activeUser(user) && row(p,r,user,"READ") && row(p,r,user,"CONFIRM") && permissions.hasAnyPermissions(user,"hrm:payroll:requirement:query") && permissions.hasAnyPermissions(user,"hrm:payroll:requirement:confirm");
    }
    public boolean coversGrant(HrmPayrollRequirementProjectGrantDO parent,HrmPayrollRequirementProjectGrantDO child) {
        try {
            if(!actions(parent.getActions()).contains("MEMBER") || !actions(parent.getDelegableActions()).containsAll(actions(child.getActions())) || !actions(parent.getDelegableActions()).containsAll(actions(child.getDelegableActions())))return false;
            if(child.getValidFrom().isBefore(parent.getValidFrom()) || child.getValidTo().isAfter(parent.getValidTo()))return false;
            if(Boolean.FALSE.equals(parent.getFullProject()) && !Boolean.FALSE.equals(child.getFullProject()))return false;
            Set<String> pm=set(parent.getModulesJson(),String.class),cm=set(child.getModulesJson(),String.class);
            Set<Long> pi=set(parent.getRequirementIdsJson(),Long.class),ci=set(child.getRequirementIdsJson(),Long.class);
            Set<Long> ps=set(parent.getSourceIdsJson(),Long.class),cs=set(child.getSourceIdsJson(),Long.class);
            if(!ps.isEmpty() && (cs.isEmpty() || !ps.containsAll(cs) || pi.isEmpty() && !ci.isEmpty()))return false;
            if(!pm.isEmpty() && (cm.isEmpty() || !pm.containsAll(cm)))return false;
            if(!pi.isEmpty() && (ci.isEmpty() || !pi.containsAll(ci)))return false;
            return child.getCeilingJson()!=null && (parent.getCeilingJson()==null || HrmPayrollRequirementScope.parse(parent.getCeilingJson()).contains(HrmPayrollRequirementScope.parse(child.getCeilingJson())));
        } catch(RuntimeException ex){return false;}
    }
    public HrmPayrollRequirementProjectGrantDO delegation(HrmPayrollRequirementProjectDO p,Long actor,HrmPayrollRequirementProjectGrantDO child) {
        if(!project(p,actor,"READ") || !project(p,actor,"MEMBER") || Objects.equals(actor,child.getUserId()))throw exception(PAYROLL_PROJECT_DENIED);
        return active(p.getId(),actor).stream().filter(g->coversGrant(g,child)).findFirst().orElseThrow(()->exception(PAYROLL_PROJECT_DENIED));
    }
    public void validateGrant(HrmPayrollRequirementProjectGrantDO g) {
        if(!activeUser(g.getUserId()) || !ACTIONS.containsAll(actions(g.getActions())) || !ACTIONS.containsAll(actions(g.getDelegableActions())) || !actions(g.getActions()).contains("READ") || g.getEvidence()==null || g.getEvidence().trim().isEmpty() || g.getValidFrom()==null || g.getValidTo()==null || g.getValidFrom().isAfter(g.getValidTo()))throw exception(PAYROLL_PROJECT_DENIED);
        HrmPayrollRequirementScope.parse(g.getCeilingJson());
        Set<String> modules=set(g.getModulesJson(),String.class);Set<Long> ids=set(g.getRequirementIdsJson(),Long.class);
        if(!new HashSet<>(Arrays.asList("OV","PLAN","CALC","TAX","ATT","OT","HOUR","INS","RPT","COL")).containsAll(modules) || ids.stream().anyMatch(x->x==null || x<=0) || set(g.getSourceIdsJson(),Long.class).stream().anyMatch(x->x==null || x<=0) || (!Boolean.FALSE.equals(g.getFullProject()) && (!modules.isEmpty() || !ids.isEmpty() || !set(g.getSourceIdsJson(),Long.class).isEmpty())))throw exception(PAYROLL_PROJECT_DENIED);
    }
}
