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
 @Resource private HrmPayrollRequirementAccessService access;
 @Resource private HrmPayrollWorkbenchService workbench;
 @Resource private HrmPayrollWorkbenchRecordMapper workbenchRecords;
 public boolean allowed(HrmPayrollRequirementProjectDO p,Long actor,String action) {return access.project(p,actor,action);}
 private List<HrmPayrollRequirementProjectGrantDO> activeGrants(Long project,Long actor) {return access.active(project,actor);}
 public void requireRow(HrmPayrollRequirementDO r,Long actor,String action){HrmPayrollRequirementProjectDO p=check(r.getProjectId(),actor,action,true);access.requireRow(p,r,actor,action);}
 public boolean visible(HrmPayrollRequirementDO r,Long actor,String action){HrmPayrollRequirementProjectDO p=projects.selectById(r.getProjectId());return access.row(p,r,actor,"READ") && access.row(p,r,actor,action);}
 public boolean full(Long project,Long actor,String action){return access.full(projects.selectById(project),actor,action);}
 public boolean reviewer(HrmPayrollRequirementDO r,Long user){return access.reviewer(projects.selectById(r.getProjectId()),r,user);}
 public List<HrmPayrollRequirementDO> rows(Long id,String module,Long actor,String action){
  HrmPayrollRequirementProjectDO p=check(id,actor,action,false);
  List<HrmPayrollRequirementDO> result=requirements.selectList(new LambdaQueryWrapperX<HrmPayrollRequirementDO>().eq(HrmPayrollRequirementDO::getProjectId,id).eqIfPresent(HrmPayrollRequirementDO::getModule,module).orderByAsc(HrmPayrollRequirementDO::getCode));
  return result.stream().filter(r->access.row(p,r,actor,"READ") && access.row(p,r,actor,action)).peek(r->{r.setReadiness(workbench.readiness(r));r.setCurrentConfirmationValid(workbench.currentConfirmationValid(r));r.setConfirmedTupleJson(null);sanitizeReviewMetadata(r,actor);}).peek(r->r.setPermittedActions(HrmPayrollRequirementAccessService.ACTIONS.stream().filter(a->access.row(p,r,actor,a)).collect(Collectors.toList()))).collect(Collectors.toList());
 }
 @SuppressWarnings("unchecked") public void sanitizeReviewMetadata(HrmPayrollRequirementDO r,Long actor){
  if(r.getScopeDecisionJson()==null)return;
  Map<String,Object> metadata=JsonUtils.parseObject(r.getScopeDecisionJson(),Map.class);
  Object id=metadata.get("replacementId");
  if(id!=null){HrmPayrollRequirementDO replacement=requirements.selectById(Long.valueOf(String.valueOf(id)));HrmPayrollRequirementProjectDO p=projects.selectById(r.getProjectId());
   if(replacement==null||!Objects.equals(replacement.getProjectId(),r.getProjectId())||!access.row(p,replacement,actor,"READ")){metadata.remove("replacementId");metadata.put("replacementRestricted",true);r.setScopeDecisionJson(JsonUtils.toJsonString(metadata));}}
 }
 public HrmPayrollRequirementProjectDO check(Long id,Long actor,String action,boolean lock) {
  HrmPayrollRequirementProjectDO p=lock?projects.selectOneForUpdate(HrmPayrollRequirementProjectDO::getId,id):projects.selectById(id);
  if(p==null || !allowed(p,actor,"READ") || !allowed(p,actor,action)) throw exception(PAYROLL_PROJECT_DENIED);
  return p;
 }
 public List<HrmPayrollRequirementProjectDO> list(Long actor) {
  return projects.selectList().stream().filter(p->!"LEGACY".equals(p.getProjectKey()) && allowed(p,actor,"READ")).peek(p->{if(!access.full(p,actor,"READ"))p.setScopeJson(null);}).collect(Collectors.toList());
 }
 @Transactional(rollbackFor=Exception.class,isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
 public Long create(HrmPayrollRequirementProjectReqVO r,Long actor) {
  if("LEGACY".equals(r.getProjectKey()))throw exception(PAYROLL_PROJECT_DENIED);
  if(actor==null || users.getUser(actor)==null || !Objects.equals(users.getUser(actor).getStatus(),0)) throw exception(PAYROLL_PROJECT_DENIED);
  access.creationAuthority(actor,r.getCreationScopeJson());
  if(projects.selectOne(HrmPayrollRequirementProjectDO::getProjectKey,r.getProjectKey())!=null) throw exception(PAYROLL_REQUIREMENT_DUPLICATE);
  HrmPayrollRequirementProjectDO p=new HrmPayrollRequirementProjectDO().setProjectKey(r.getProjectKey()).setName(r.getName()).setMetadataVersion(1).setOwnerId(actor).setCustomSequence(0L).setCatalogKey("DEFAULT");
  projects.insert(p);
  HrmPayrollRequirementProjectGrantDO bootstrap=new HrmPayrollRequirementProjectGrantDO();bootstrap.setProjectId(p.getId());bootstrap.setUserId(actor);bootstrap.setGrantorId(actor);bootstrap.setActions("READ,META,EDIT,ASSIGN,EXPORT");bootstrap.setFullProject(true);bootstrap.setCeilingJson(r.getCreationScopeJson());bootstrap.setGrantVersion(1);bootstrap.setValidFrom(java.time.LocalDate.now(java.time.ZoneId.of("Asia/Shanghai")));bootstrap.setValidTo(java.time.LocalDate.of(9999,12,31));bootstrap.setEvidence("PROJECT_CREATOR_ACCESS: technical draft access only; no business confirmation, member or scope authority");grants.insert(bootstrap);
  event(p.getId(),actor,"CREATE",p);return p.getId();
 }
 @Transactional(rollbackFor=Exception.class,isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
 public void metadata(HrmPayrollRequirementProjectReqVO r,Long actor) {
  HrmPayrollRequirementProjectDO p=check(r.getId(),actor,"META",true);
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
  access.requireRow(p,r,actor,action);return r;
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
  HrmPayrollRequirementProjectDO p=check(id,actor,"SCOPE",true);
  if(!access.full(p,actor,"READ") || !access.full(p,actor,"SCOPE"))throw exception(PAYROLL_PROJECT_DENIED);
  version(p,expected);
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
   r.setSemanticVersion(r.getSemanticVersion()==null?2:r.getSemanticVersion()+1);r.setConfirmedTupleJson(null);r.setConfirmedBy(null);r.setConfirmedAt(null);r.setConfirmedScopeVersion(null);r.setConfirmedScopeJson(null);r.setVersion(r.getVersion()+1);
   updateScoped(r);history(r,actor);affected.add(r.getId());
  }
  workbench.scopeChanged(p,actor);
  event.put("affectedRequirementIds",affected);event.put("scopeVersion",p.getProjectScopeVersion());event(id,actor,"SCOPE",event);
 }
 public void updateScoped(HrmPayrollRequirementDO r) {
  requirements.updateById(r);
  // MyBatis ignores null on updateById; signatures must be explicitly cleared.
  if(r.getConfirmedBy()==null)requirements.update(null,new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<HrmPayrollRequirementDO>().eq(HrmPayrollRequirementDO::getId,r.getId()).set(HrmPayrollRequirementDO::getConfirmedTupleJson,null).set(HrmPayrollRequirementDO::getConfirmedBy,null).set(HrmPayrollRequirementDO::getConfirmedAt,null).set(HrmPayrollRequirementDO::getConfirmedScopeVersion,null).set(HrmPayrollRequirementDO::getConfirmedScopeJson,null));
 }
 public List<HrmPayrollRequirementProjectEventDO> events(Long id,Long actor) {check(id,actor,"READ",false);if(!access.full(projects.selectById(id),actor,"READ"))throw exception(PAYROLL_PROJECT_DENIED);return events.selectList(new LambdaQueryWrapperX<HrmPayrollRequirementProjectEventDO>().eq(HrmPayrollRequirementProjectEventDO::getProjectId,id).orderByDesc(HrmPayrollRequirementProjectEventDO::getId)).stream().filter(e->!e.getAction().startsWith("MEMBER_") || allowed(projects.selectById(id),actor,"MEMBER")).collect(Collectors.toList());}
 public List<HrmPayrollRequirementTemplateDO> catalog(HrmPayrollRequirementProjectDO p) {
  Map<String,HrmPayrollRequirementTemplateDO> latest=new LinkedHashMap<>();
  for(HrmPayrollRequirementTemplateDO t:templates.selectList(new LambdaQueryWrapperX<HrmPayrollRequirementTemplateDO>().eq(HrmPayrollRequirementTemplateDO::getCatalogKey,p.getCatalogKey()).orderByAsc(HrmPayrollRequirementTemplateDO::getTemplateVersion)))latest.put(t.getTemplateItemId(),t);
  return new ArrayList<>(latest.values());
 }
 @Transactional(rollbackFor=Exception.class,isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
 public int initialize(Long id,Long actor) {
  HrmPayrollRequirementProjectDO p=check(id,actor,"EDIT",true);if(!access.full(p,actor,"READ") || !access.full(p,actor,"EDIT"))throw exception(PAYROLL_PROJECT_DENIED);int n=0;
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
   if(!access.full(p,actor,"READ") && (r==null || !access.row(p,r,actor,"READ")))continue;
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
  access.requireRow(p,r,actor,"EDIT");
  if(!Objects.equals(r.getVersion(),version))throw exception(PAYROLL_VERSION_CONFLICT);
  HrmPayrollRequirementTemplateDO t=catalog(p).stream().filter(x->Objects.equals(x.getTemplateItemId(),r.getTemplateItemId())).findFirst().orElseThrow(()->exception(PAYROLL_TEMPLATE_INVALID));
  if(!Objects.equals(t.getTemplateVersion(),templateVersion) || Boolean.TRUE.equals(t.getWithdrawn()) || Objects.equals(r.getTemplateVersion(),templateVersion))throw exception(PAYROLL_TEMPLATE_INVALID);
  if(accept && !"REMOVED".equals(r.getScopeDecision())) {
   r.setDescription(t.getDescription());r.setStatus("REVIEWING");r.setSemanticVersion(r.getSemanticVersion()==null?2:r.getSemanticVersion()+1);r.setConfirmedTupleJson(null);r.setConfirmedBy(null);r.setConfirmedAt(null);r.setConfirmedScopeVersion(null);r.setConfirmedScopeJson(null);
  }
  Map<String,Object> e=new LinkedHashMap<>();e.put("requirementId",r.getId());e.put("previousTemplateVersion",r.getTemplateVersion());e.put("newTemplateVersion",templateVersion);e.put("accepted",accept);e.put("removedPreserved","REMOVED".equals(r.getScopeDecision()));
  r.setTemplateVersion(templateVersion);r.setVersion(r.getVersion()+1);updateScoped(r);history(r,actor);event(id,actor,"TEMPLATE_DECISION",e);
 }

 public List<Map<String,Object>> members(Long id,Long actor){
  HrmPayrollRequirementProjectDO p=check(id,actor,"MEMBER",false);
  List<Map<String,Object>> result=new ArrayList<>();
  for(HrmPayrollRequirementProjectGrantDO g:grants.selectList(new LambdaQueryWrapperX<HrmPayrollRequirementProjectGrantDO>().eq(HrmPayrollRequirementProjectGrantDO::getProjectId,id).orderByDesc(HrmPayrollRequirementProjectGrantDO::getId))){
   if(!access.active(id,actor).stream().anyMatch(parent->access.coversGrant(parent,g)) && !Objects.equals(g.getUserId(),actor) && !(access.full(p,actor,"READ") && access.full(p,actor,"MEMBER")))continue;
   Map<String,Object> item=new LinkedHashMap<>();item.put("grant",g);item.put("effective",access.effective(g));result.add(item);
  }return result;
 }
 @Transactional(rollbackFor=Exception.class,isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
 public Long grant(HrmPayrollRequirementProjectGrantDO r,Long actor){
  HrmPayrollRequirementProjectDO p=check(r.getProjectId(),actor,"MEMBER",true);access.validateGrant(r);
  HrmPayrollRequirementProjectGrantDO parent=access.delegation(p,actor,r);
  if(r.getRequirementIdsJson()!=null)for(Long rid:JsonUtils.parseArray(r.getRequirementIdsJson(),Long.class)){
   HrmPayrollRequirementDO row=requirements.selectById(rid);if(row==null || !Objects.equals(row.getProjectId(),p.getId()))throw exception(PAYROLL_PROJECT_DENIED);
  }
  if(r.getSourceIdsJson()!=null)for(Long sid:JsonUtils.parseArray(r.getSourceIdsJson(),Long.class)){
   HrmPayrollWorkbenchRecordDO source=workbenchRecords.selectById(sid);if(source==null || !"SOURCE".equals(source.getKind()) || !Objects.equals(source.getProjectId(),p.getId()))throw exception(PAYROLL_PROJECT_DENIED);
  }
  // Request cannot supply root identity, version, grantor or lifecycle state.
  r.setId(null);r.setParentGrantId(parent.getId());r.setGrantorId(actor);r.setGrantVersion(1);r.setRevokedAt(null);r.setRevokeReason(null);
  grants.insert(r);event(p.getId(),actor,"MEMBER_GRANT",r);return r.getId();
 }
 @Transactional(rollbackFor=Exception.class,isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
 public void revoke(Long project,Long grantId,Integer expected,String reason,Long actor){
  HrmPayrollRequirementProjectDO p=check(project,actor,"MEMBER",true);
  HrmPayrollRequirementProjectGrantDO g=grants.selectOneForUpdate(HrmPayrollRequirementProjectGrantDO::getId,grantId);
  if(g==null || !Objects.equals(g.getProjectId(),project))throw exception(PAYROLL_PROJECT_DENIED);
  boolean bootstrap=g.getParentGrantId()==null && Objects.equals(g.getUserId(),p.getOwnerId()) && g.getEvidence()!=null && g.getEvidence().startsWith("PROJECT_CREATOR_ACCESS:");
  if(g.getParentGrantId()==null && !bootstrap)throw exception(PAYROLL_PROJECT_DENIED);
  if(bootstrap){if(!access.full(p,actor,"READ") || !access.full(p,actor,"MEMBER") || !access.active(project,actor).stream().anyMatch(parent->access.actions(parent.getDelegableActions()).containsAll(access.actions(g.getActions()))))throw exception(PAYROLL_PROJECT_DENIED);}
  else access.delegation(p,actor,g);
  if(!Objects.equals(g.getGrantVersion(),expected) || g.getRevokedAt()!=null)throw exception(PAYROLL_VERSION_CONFLICT);
  if(reason==null || reason.trim().isEmpty())throw exception(PAYROLL_PROJECT_DENIED);
  g.setRevokedAt(java.time.LocalDateTime.now());g.setRevokeReason(reason);g.setGrantVersion(expected+1);grants.updateById(g);event(project,actor,"MEMBER_REVOKE",g);
 }
 public List<Long> reviewers(Long project,Long requirement,Long actor){
  HrmPayrollRequirementProjectDO p=check(project,actor,"ASSIGN",false);HrmPayrollRequirementDO r=requirements.selectById(requirement);access.requireRow(p,r,actor,"ASSIGN");
  return grants.selectList(new LambdaQueryWrapperX<HrmPayrollRequirementProjectGrantDO>().eq(HrmPayrollRequirementProjectGrantDO::getProjectId,project)).stream().map(HrmPayrollRequirementProjectGrantDO::getUserId).distinct().filter(user->!Objects.equals(user,actor) && access.reviewer(p,r,user)).collect(Collectors.toList());
 }
 public Map<String,Boolean> capabilities(Long id,Long actor){HrmPayrollRequirementProjectDO p=check(id,actor,"READ",false);Map<String,Boolean> result=new LinkedHashMap<>();for(String a:HrmPayrollRequirementAccessService.ACTIONS)result.put(a,allowed(p,actor,a));result.put("FULL_READ",access.full(p,actor,"READ"));result.put("FULL_EDIT",access.full(p,actor,"EDIT"));return result;}
}
