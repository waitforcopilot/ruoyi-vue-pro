package cn.iocoder.yudao.module.hrm.service.payroll;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollRequirementProjectReqVO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.*;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;

/** Project ownership alone never implies authority to approve business scope. */
@Service
public class HrmPayrollRequirementProjectService {
 @Resource private HrmPayrollRequirementProjectMapper projects;
 @Resource private HrmPayrollRequirementProjectGrantMapper grants;
 @Resource private HrmPayrollRequirementProjectEventMapper events;
 @Resource private HrmPayrollRequirementTemplateMapper templates;
 @Resource private HrmPayrollRequirementMapper requirements;
 @Resource private HrmPayrollRequirementHistoryMapper histories;
 @Resource private AdminUserApi users;
 public boolean allowed(HrmPayrollRequirementProjectDO p,Long actor,String action) {
  if(p==null || actor==null) return false;
  cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO user=users.getUser(actor);
  if(user==null || !Objects.equals(user.getStatus(),0)) return false;
  if(Objects.equals(p.getOwnerId(),actor) && !"SCOPE".equals(action)) return true;
  return activeGrants(p.getId(),actor).stream().anyMatch(g->Arrays.asList(g.getActions().split(",")).contains(action));
 }
 private List<HrmPayrollRequirementProjectGrantDO> activeGrants(Long project,Long actor) {
  LocalDate date=LocalDate.now(java.time.ZoneId.of("Asia/Shanghai"));
  return grants.selectList(new LambdaQueryWrapperX<HrmPayrollRequirementProjectGrantDO>().eq(HrmPayrollRequirementProjectGrantDO::getProjectId,project).eq(HrmPayrollRequirementProjectGrantDO::getUserId,actor).le(HrmPayrollRequirementProjectGrantDO::getValidFrom,date).ge(HrmPayrollRequirementProjectGrantDO::getValidTo,date));
 }
 public HrmPayrollRequirementProjectDO check(Long id,Long actor,String action,boolean lock) {
  HrmPayrollRequirementProjectDO p=lock?projects.selectOneForUpdate(HrmPayrollRequirementProjectDO::getId,id):projects.selectById(id);
  if(p==null || "LEGACY".equals(p.getProjectKey()) || !allowed(p,actor,"READ") || !allowed(p,actor,action)) throw exception(PAYROLL_PROJECT_DENIED);
  return p;
 }
 public List<HrmPayrollRequirementProjectDO> list(Long actor) {
  return projects.selectList().stream().filter(p->!"LEGACY".equals(p.getProjectKey()) && allowed(p,actor,"READ")).collect(Collectors.toList());
 }
 @Transactional(rollbackFor=Exception.class,isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
 public Long create(HrmPayrollRequirementProjectReqVO r,Long actor) {
  if("LEGACY".equals(r.getProjectKey()))throw exception(PAYROLL_PROJECT_DENIED);
  if(actor==null || users.getUser(actor)==null || !Objects.equals(users.getUser(actor).getStatus(),0)) throw exception(PAYROLL_PROJECT_DENIED);
  if(projects.selectOne(HrmPayrollRequirementProjectDO::getProjectKey,r.getProjectKey())!=null) throw exception(PAYROLL_REQUIREMENT_DUPLICATE);
  HrmPayrollRequirementProjectDO p=new HrmPayrollRequirementProjectDO().setProjectKey(r.getProjectKey()).setName(r.getName()).setMetadataVersion(1).setOwnerId(actor).setCustomSequence(0L).setCatalogKey("DEFAULT");
  projects.insert(p);event(p.getId(),actor,"CREATE",p);return p.getId();
 }
 @Transactional(rollbackFor=Exception.class,isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
 public void metadata(HrmPayrollRequirementProjectReqVO r,Long actor) {
  HrmPayrollRequirementProjectDO p=check(r.getId(),actor,"EDIT",true);
  if(!Objects.equals(p.getMetadataVersion(),r.getMetadataVersion())) throw exception(PAYROLL_VERSION_CONFLICT);
  if(!Objects.equals(p.getProjectKey(),r.getProjectKey())) throw exception(PAYROLL_PROJECT_DENIED);
  if(Objects.equals(p.getName(),r.getName())) return;
  p.setName(r.getName()).setMetadataVersion(p.getMetadataVersion()+1);projects.updateById(p);event(p.getId(),actor,"METADATA",p);
 }
 public void version(HrmPayrollRequirementProjectDO p,Integer expected) {
  if(!Objects.equals(p.getProjectScopeVersion(),expected)) throw exception(PAYROLL_PROJECT_SCOPE_STALE);
 }
 public HrmPayrollRequirementDO target(Long project,Long id,Long actor,String action,Integer scope,boolean lock) {
  HrmPayrollRequirementProjectDO p=check(project,actor,action,lock);version(p,scope);
  HrmPayrollRequirementDO r=requirements.selectById(id);
  if(r==null || !Objects.equals(r.getProjectId(),project)) throw exception(PAYROLL_PROJECT_DENIED);
  return r;
 }
 public String nextCode(HrmPayrollRequirementProjectDO p) {
  long n=p.getCustomSequence()+1;p.setCustomSequence(n);projects.updateById(p);return String.format(java.util.Locale.ROOT,"CUSTOM-%04d",n);
 }
 public void bind(HrmPayrollRequirementDO r,HrmPayrollRequirementProjectDO p,String mode,String subset) {
  r.setScopeMode(mode==null?"INHERIT":mode);r.setProjectScopeVersion(p.getProjectScopeVersion());
  if(p.getScopeJson()==null) {r.setScopeState("UNDEFINED");r.setScopeJson(subset);return;}
  HrmPayrollRequirementScope parent=HrmPayrollRequirementScope.parse(p.getScopeJson());
  if("SUBSET".equals(r.getScopeMode())) {
   HrmPayrollRequirementScope child=HrmPayrollRequirementScope.parse(subset);r.setScopeJson(JsonUtils.toJsonString(child));r.setScopeState(parent.contains(child)?"MATCH":"SCOPE_MISMATCH");
  } else {r.setScopeJson(p.getScopeJson());r.setScopeState("MATCH");}
 }
 public void history(HrmPayrollRequirementDO r,Long actor) {
  HrmPayrollRequirementHistoryDO h=new HrmPayrollRequirementHistoryDO();h.setRequirementId(r.getId());h.setVersion(r.getVersion());h.setActorId(actor);h.setSnapshot(JsonUtils.toJsonString(r));histories.insert(h);
 }
 private void event(Long id,Long actor,String action,Object snapshot) {
  HrmPayrollRequirementProjectEventDO e=new HrmPayrollRequirementProjectEventDO();e.setProjectId(id);e.setActorId(actor);e.setAction(action);e.setSnapshot(JsonUtils.toJsonString(snapshot));events.insert(e);
 }
 @Transactional(rollbackFor=Exception.class,isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
 public void scope(Long id,Integer expected,String json,String reason,Long actor) {
  HrmPayrollRequirementProjectDO p=check(id,actor,"SCOPE",true);version(p,expected);
  if(reason==null || reason.trim().isEmpty() || reason.length()>2000) throw exception(PAYROLL_PROJECT_SCOPE_INVALID);
  HrmPayrollRequirementScope next=HrmPayrollRequirementScope.parse(json);
  HrmPayrollRequirementScope old=p.getScopeJson()==null?null:HrmPayrollRequirementScope.parse(p.getScopeJson());
  boolean granted=activeGrants(id,actor).stream().filter(g->Arrays.asList(g.getActions().split(",")).contains("SCOPE") && g.getEvidence()!=null && !g.getEvidence().trim().isEmpty()).anyMatch(g->{HrmPayrollRequirementScope ceiling=HrmPayrollRequirementScope.parse(g.getCeilingJson());return ceiling.contains(next) && (old==null || ceiling.contains(old));});
  if(!granted) throw exception(PAYROLL_PROJECT_SCOPE_INVALID);
  Map<String,Object> event=new LinkedHashMap<>();event.put("before",p.getScopeJson());event.put("after",next);event.put("reason",reason);
  p.setScopeJson(JsonUtils.toJsonString(next)).setProjectScopeVersion(expected==null?1:expected+1);projects.updateById(p);
  List<Long> affected=new ArrayList<>();
  for(HrmPayrollRequirementDO r:requirements.selectList(new LambdaQueryWrapperX<HrmPayrollRequirementDO>().eq(HrmPayrollRequirementDO::getProjectId,id).last("FOR UPDATE"))) {
   if("REMOVED".equals(r.getScopeDecision()))continue;
   bind(r,p,r.getScopeMode(),r.getScopeJson());
   if("CONFIRMED".equals(r.getStatus()))r.setStatus("REVIEWING");
   r.setConfirmedBy(null);r.setConfirmedAt(null);r.setConfirmedScopeVersion(null);r.setConfirmedScopeJson(null);r.setVersion(r.getVersion()+1);
   updateScoped(r);history(r,actor);affected.add(r.getId());
  }
  event.put("affectedRequirementIds",affected);event.put("scopeVersion",p.getProjectScopeVersion());event(id,actor,"SCOPE",event);
 }
 public void updateScoped(HrmPayrollRequirementDO r) {
  requirements.updateById(r);
  // MyBatis ignores null on updateById; signatures must be explicitly cleared.
  if(r.getConfirmedBy()==null)requirements.update(null,new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<HrmPayrollRequirementDO>().eq(HrmPayrollRequirementDO::getId,r.getId()).set(HrmPayrollRequirementDO::getConfirmedBy,null).set(HrmPayrollRequirementDO::getConfirmedAt,null).set(HrmPayrollRequirementDO::getConfirmedScopeVersion,null).set(HrmPayrollRequirementDO::getConfirmedScopeJson,null));
 }
 public List<HrmPayrollRequirementProjectEventDO> events(Long id,Long actor) {check(id,actor,"READ",false);return events.selectList(new LambdaQueryWrapperX<HrmPayrollRequirementProjectEventDO>().eq(HrmPayrollRequirementProjectEventDO::getProjectId,id).orderByDesc(HrmPayrollRequirementProjectEventDO::getId));}
 public List<HrmPayrollRequirementTemplateDO> catalog(HrmPayrollRequirementProjectDO p) {
  Map<String,HrmPayrollRequirementTemplateDO> latest=new LinkedHashMap<>();
  for(HrmPayrollRequirementTemplateDO t:templates.selectList(new LambdaQueryWrapperX<HrmPayrollRequirementTemplateDO>().eq(HrmPayrollRequirementTemplateDO::getCatalogKey,p.getCatalogKey()).orderByAsc(HrmPayrollRequirementTemplateDO::getTemplateVersion)))latest.put(t.getTemplateItemId(),t);
  return new ArrayList<>(latest.values());
 }
 @Transactional(rollbackFor=Exception.class,isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
 public int initialize(Long id,Long actor) {
  HrmPayrollRequirementProjectDO p=check(id,actor,"EDIT",true);int n=0;
  for(HrmPayrollRequirementTemplateDO t:catalog(p)) {
   if(Boolean.TRUE.equals(t.getWithdrawn()) || requirements.selectOne(new LambdaQueryWrapperX<HrmPayrollRequirementDO>().eq(HrmPayrollRequirementDO::getProjectId,id).eq(HrmPayrollRequirementDO::getTemplateItemId,t.getTemplateItemId()).last("FOR UPDATE"))!=null)continue;
   HrmPayrollRequirementDO r=new HrmPayrollRequirementDO();r.setProjectId(id);r.setCode(t.getOriginalCode());r.setModule(t.getOriginalCode().split("-")[0]);r.setDescription(t.getDescription());r.setTemplateItemId(t.getTemplateItemId());r.setTemplateVersion(t.getTemplateVersion());r.setPriority(2);r.setVersion(1);r.setStatus("PENDING");r.setReadiness("MISSING");r.setScopeDecision("CANDIDATE");bind(r,p,"INHERIT",null);requirements.insert(r);history(r,actor);n++;
  }
  event(id,actor,"INITIALIZE",Collections.singletonMap("added",n));return n;
 }
 public List<Map<String,Object>> differences(Long id,Long actor) {
  HrmPayrollRequirementProjectDO p=check(id,actor,"READ",false);List<Map<String,Object>> result=new ArrayList<>();
  for(HrmPayrollRequirementTemplateDO t:catalog(p)) {
   HrmPayrollRequirementDO r=requirements.selectOne(new LambdaQueryWrapperX<HrmPayrollRequirementDO>().eq(HrmPayrollRequirementDO::getProjectId,id).eq(HrmPayrollRequirementDO::getTemplateItemId,t.getTemplateItemId()).last("FOR UPDATE"));
   if(r==null || !Objects.equals(r.getTemplateVersion(),t.getTemplateVersion()) || Boolean.TRUE.equals(t.getWithdrawn())) {
    Map<String,Object> m=new LinkedHashMap<>();m.put("template",t);m.put("requirement",r);result.add(m);
   }
  }
  return result;
 }
 @Transactional(rollbackFor=Exception.class,isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
 public void adopt(Long id,Long requirement,Integer version,Integer templateVersion,boolean accept,Long actor) {
  HrmPayrollRequirementProjectDO p=check(id,actor,"EDIT",true);
  HrmPayrollRequirementDO r=requirements.selectOneForUpdate(HrmPayrollRequirementDO::getId,requirement);
  if(r==null || !Objects.equals(r.getProjectId(),id)) throw exception(PAYROLL_PROJECT_DENIED);
  if(!Objects.equals(r.getVersion(),version))throw exception(PAYROLL_VERSION_CONFLICT);
  HrmPayrollRequirementTemplateDO t=catalog(p).stream().filter(x->Objects.equals(x.getTemplateItemId(),r.getTemplateItemId())).findFirst().orElseThrow(()->exception(PAYROLL_TEMPLATE_INVALID));
  if(!Objects.equals(t.getTemplateVersion(),templateVersion) || Boolean.TRUE.equals(t.getWithdrawn()) || Objects.equals(r.getTemplateVersion(),templateVersion))throw exception(PAYROLL_TEMPLATE_INVALID);
  if(accept && !"REMOVED".equals(r.getScopeDecision())) {
   r.setDescription(t.getDescription());r.setStatus("REVIEWING");r.setConfirmedBy(null);r.setConfirmedAt(null);r.setConfirmedScopeVersion(null);r.setConfirmedScopeJson(null);
  }
  Map<String,Object> e=new LinkedHashMap<>();e.put("requirementId",r.getId());e.put("previousTemplateVersion",r.getTemplateVersion());e.put("newTemplateVersion",templateVersion);e.put("accepted",accept);e.put("removedPreserved","REMOVED".equals(r.getScopeDecision()));
  r.setTemplateVersion(templateVersion);r.setVersion(r.getVersion()+1);updateScoped(r);history(r,actor);event(id,actor,"TEMPLATE_DECISION",e);
 }
}
