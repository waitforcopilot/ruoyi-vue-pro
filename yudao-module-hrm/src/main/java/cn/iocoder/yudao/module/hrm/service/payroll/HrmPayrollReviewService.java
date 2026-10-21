package cn.iocoder.yudao.module.hrm.service.payroll;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollReviewVO.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.*;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import javax.annotation.Resource;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;

/** Scope and review-only operations. No payroll, payment, employee or message writer is injected. */
@Service
public class HrmPayrollReviewService {
 @Resource private HrmPayrollRequirementProjectService projects;
 @Resource private HrmPayrollRequirementMapper requirements;
 @Resource private HrmPayrollRequirementAccessService access;
 @Resource private HrmPayrollWorkbenchService workbench;
 @Resource private HrmPayrollRequirementService requirementService;
 @Resource private PermissionApi permissions;
 private static final ZoneId ZONE=ZoneId.of("Asia/Shanghai");
 static boolean blank(String s){return s==null || s.trim().isEmpty();}
 static void valid(boolean b){if(!b)throw exception(PAYROLL_REVIEW_INCOMPLETE);}
 static void required(Object... fields){
  List<String> missing=new ArrayList<>();for(int n=0;n<fields.length;n+=2)if(fields[n+1]==null||blank(String.valueOf(fields[n+1])))missing.add(String.valueOf(fields[n]));
  if(!missing.isEmpty())throw new cn.iocoder.yudao.framework.common.exception.ServiceException(PAYROLL_REVIEW_INCOMPLETE.getCode(),"请补齐以下字段："+String.join("、",missing));
 }
 private void platform(Long actor,String suffix){if(!permissions.hasAnyPermissions(actor,"hrm:payroll:requirement:query") || !permissions.hasAnyPermissions(actor,"hrm:payroll:requirement:"+suffix))throw exception(PAYROLL_PROJECT_DENIED);}
 public static Business business(String json){try{return blank(json)?new Business():JsonUtils.parseObject(json,Business.class);}catch(RuntimeException e){throw exception(PAYROLL_REVIEW_INCOMPLETE);}}
 /** Drafts can be incomplete; REVIEWING and confirmation have different, explicit gates. */
 public static void validateBusiness(HrmPayrollRequirementDO r,boolean confirming){
  Business b=business(r.getBusinessContextJson());
  if(!blank(b.getPage()))valid("/hrm-payroll/review".equals(b.getPage()) && Objects.equals(b.getContextModule(),r.getModule()) && b.getAnchor()!=null && b.getAnchor().matches("module|field-[a-z0-9-]+|operation-[a-z0-9-]+|flow-[a-z0-9-]+") && Arrays.asList("MODULE","FIELD","OPERATION","FLOW").contains(b.getTargetType()));
  List<String> missing=new ArrayList<>();
  if(confirming || "REVIEWING".equals(r.getStatus())){
   if(blank(b.getScenario()))missing.add("触发场景");if(blank(b.getExpected()))missing.add("期望行为");
  }
  if(confirming){
   String[] labels={"业务角色","业务目标","正常样例","异常样例","计算或业务规则","单位","精度与舍入","有效期或适用依据","规则例外与处理"};
   String[] values={b.getRole(),b.getGoal(),b.getNormalCase(),b.getExceptionCase(),b.getCalculation(),b.getUnit(),b.getPrecision(),b.getValidity(),b.getRuleException()};
   for(int n=0;n<labels.length;n++)if(blank(values[n]))missing.add(labels[n]);
  }
  if("DISPUTED".equals(r.getStatus()) && r.getBusinessContextJson()!=null){if(b.getDisputeOwnerId()==null)missing.add("异议责任人ID");if(blank(b.getNextStep()))missing.add("异议下一步");}
  if(!missing.isEmpty())throw new cn.iocoder.yudao.framework.common.exception.ServiceException(PAYROLL_REVIEW_INCOMPLETE.getCode(),"请补齐以下字段："+String.join("、",missing));
 }
 private HrmPayrollRequirementDO target(Context c,Long actor,String action){return projects.target(c.getProjectId(),c.getId(),actor,action,c.getProjectScopeVersion(),true);}
 private void version(HrmPayrollRequirementDO r,Context c){if(!Objects.equals(r.getVersion(),c.getVersion()))throw exception(PAYROLL_VERSION_CONFLICT);}
 private void save(HrmPayrollRequirementDO r,Long actor){r.setVersion(r.getVersion()+1);r.setUpdater(String.valueOf(actor));projects.updateScoped(r);projects.history(r,actor);}
 private Map<String,Object> map(Object...v){Map<String,Object> m=new LinkedHashMap<>();for(int i=0;i<v.length;i+=2)m.put((String)v[i],v[i+1]);return m;}
 private void invalidate(HrmPayrollRequirementDO r){r.setStatus("REVIEWING");r.setSemanticVersion(r.getSemanticVersion()==null?2:r.getSemanticVersion()+1);r.setConfirmedBy(null);r.setConfirmedAt(null);r.setConfirmedScopeVersion(null);r.setConfirmedScopeJson(null);r.setConfirmedTupleJson(null);}
 @Transactional(rollbackFor=Exception.class,isolation=Isolation.READ_COMMITTED)
 public void decide(Decision c,Long actor){
  platform(actor,"scope-decide");HrmPayrollRequirementDO r=target(c,actor,"SCOPE_DECIDE");version(r,c);
  valid(!blank(c.getReason()));
  if("DEFERRED".equals(c.getDecision())){valid(c.getResponsibleId()!=null&&!blank(c.getReevaluate()));valid(access.activeUser(c.getResponsibleId())&&projects.visible(r,c.getResponsibleId(),"READ"));}
  if("REMOVED".equals(c.getDecision())){
   valid(c.getRemovalKind()!=null);
   if("DUPLICATE".equals(c.getRemovalKind())){
    valid(c.getReplacementId()!=null&&!Objects.equals(c.getReplacementId(),r.getId()));
    HrmPayrollRequirementDO replacement=requirements.selectById(c.getReplacementId());
    if(replacement==null||!Objects.equals(replacement.getProjectId(),r.getProjectId())||!projects.visible(replacement,actor,"READ"))throw exception(PAYROLL_PROJECT_DENIED);
    valid(!"REMOVED".equals(replacement.getScopeDecision()));
    // A retained target must never become removed while another live exclusion points at it.
   }else valid(c.getReplacementId()==null);
   for(HrmPayrollRequirementDO other:requirements.selectList(new LambdaQueryWrapperX<HrmPayrollRequirementDO>().eq(HrmPayrollRequirementDO::getProjectId,r.getProjectId()))){
    if("REMOVED".equals(other.getScopeDecision())&&!blank(other.getScopeDecisionJson())){Map<?,?>d=JsonUtils.parseObject(other.getScopeDecisionJson(),Map.class);if(Objects.equals(String.valueOf(r.getId()),String.valueOf(d.get("replacementId"))))valid(false);}
   }
  }else valid(c.getReplacementId()==null);
  if("REMOVED".equals(r.getScopeDecision())){valid("CANDIDATE".equals(c.getDecision()));invalidate(r);}
  r.setScopeDecision(c.getDecision());r.setScopeDecisionJson(JsonUtils.toJsonString(map("decision",c.getDecision(),"actorId",actor,"time",LocalDateTime.now(ZONE).toString(),"reason",c.getReason(),"responsibleId",c.getResponsibleId(),"reevaluate",c.getReevaluate(),"removalKind",c.getRemovalKind(),"replacementId",c.getReplacementId(),"scopeVersion",c.getProjectScopeVersion())));save(r,actor);
 }
 @Transactional(rollbackFor=Exception.class,isolation=Isolation.READ_COMMITTED)
 public void implementation(Implementation c,Long actor){
  platform(actor,"update");HrmPayrollRequirementDO r=target(c,actor,"EDIT");version(r,c);
  valid(!blank(c.getReason()));
  if(!"UNSCHEDULED".equals(c.getState()))valid(!blank(c.getPage())&&!blank(c.getApi())&&!blank(c.getTask()));
  if("ACCEPTED".equals(c.getState()))valid(!blank(c.getEvidence()));
  r.setImplementationJson(JsonUtils.toJsonString(map("state",c.getState(),"page",c.getPage(),"api",c.getApi(),"task",c.getTask(),"evidence",c.getEvidence(),"reason",c.getReason(),"actorId",actor,"time",LocalDateTime.now(ZONE).toString(),"conclusionVerified",false)));save(r,actor);
 }
 @Transactional(rollbackFor=Exception.class,isolation=Isolation.READ_COMMITTED)
 @SuppressWarnings("unchecked") public void collaborate(Collaboration c,Long actor){
  platform(actor,"update");HrmPayrollRequirementDO r=target(c,actor,"EDIT");version(r,c);valid(!blank(c.getReason())&&!blank(c.getReference()));
  if("REMINDER".equals(c.getKind()))valid(c.getRecipientId()!=null&&access.activeUser(c.getRecipientId())&&projects.visible(r,c.getRecipientId(),"READ"));
  List<Object> notes=blank(r.getCollaborationJson())?new ArrayList<>():JsonUtils.parseArray(r.getCollaborationJson(),Object.class);valid(notes.size()<100);
  notes.add(map("kind",c.getKind(),"reference",c.getReference(),"reason",c.getReason(),"recipientId",c.getRecipientId(),"actorId",actor,"time",LocalDateTime.now(ZONE).toString(),"channel","IN_APP_ONLY"));r.setCollaborationJson(JsonUtils.toJsonString(notes));save(r,actor);
 }
 public List<Object> batch(BatchAssignment c,Long actor){
  platform(actor,"assign");List<Object> result=new ArrayList<>();int index=0;
  for(Context item:c.getItems()){int n=index++;try{
   if(!Objects.equals(item.getProjectId(),c.getProjectId())||!Objects.equals(item.getProjectScopeVersion(),c.getProjectScopeVersion()))throw exception(PAYROLL_PROJECT_DENIED);
   projects.target(c.getProjectId(),item.getId(),actor,"ASSIGN",c.getProjectScopeVersion(),false);
   requirementService.assign(item.getId(),item.getVersion(),c.getReviewerId(),actor);
   result.add(map("index",n,"id",item.getId(),"success",true));
  }catch(cn.iocoder.yudao.framework.common.exception.ServiceException error){result.add(map("index",n,"success",false,"code",error.getCode(),"message","未完成：无权限、资格不足或版本已变，请核对本对象"));}}
  return result;
 }
 public HrmPayrollRequirementDO detail(Long project,Long id,Long actor){
  HrmPayrollRequirementProjectDO p=projects.check(project,actor,"READ",false);HrmPayrollRequirementDO row=requirements.selectById(id);
  if(row==null||!Objects.equals(row.getProjectId(),project))throw exception(PAYROLL_PROJECT_DENIED);access.requireRow(p,row,actor,"READ");
  row.setReadiness(workbench.readiness(row));row.setCurrentConfirmationValid(workbench.currentConfirmationValid(row));row.setConfirmedTupleJson(null);projects.sanitizeReviewMetadata(row,actor);
  row.setPermittedActions(HrmPayrollRequirementAccessService.ACTIONS.stream().filter(a->access.row(p,row,actor,a)).collect(Collectors.toList()));return row;
 }
 @SuppressWarnings("unchecked") public List<Object> reminders(Long project,Long actor){
  HrmPayrollRequirementProjectDO p=projects.check(project,actor,"READ",false);List<Object> result=new ArrayList<>();
  for(HrmPayrollRequirementDO r:requirements.selectList(new LambdaQueryWrapperX<HrmPayrollRequirementDO>().eq(HrmPayrollRequirementDO::getProjectId,project))){
   if(!access.row(p,r,actor,"READ")||blank(r.getCollaborationJson()))continue;
   for(Map<String,Object> note:(List<Map<String,Object>>)(List<?>)JsonUtils.parseArray(r.getCollaborationJson(),Map.class)){
    if("REMINDER".equals(note.get("kind"))&&Objects.equals(String.valueOf(actor),String.valueOf(note.get("recipientId")))){Business b=business(r.getBusinessContextJson());result.add(map("requirementId",r.getId(),"code",r.getCode(),"module",r.getModule(),"anchor",blank(b.getAnchor())?"module":b.getAnchor(),"reference",note.get("reference"),"actorId",note.get("actorId"),"time",note.get("time"),"channel","IN_APP_ONLY"));}
   }
  }return result;
 }
 private List<HrmPayrollRequirementDO> filtered(Long project,Long actor,String action,String module,String status,String decision,String text){
  return projects.rows(project,module,actor,action).stream().filter(r->status==null||status.equals(r.getStatus())).filter(r->decision==null||decision.equals(r.getScopeDecision())).filter(r->blank(text)||r.getDescription().contains(text)||r.getCode().contains(text)).collect(Collectors.toList());
 }
 public Map<String,Object> page(Long project,Long actor,String module,String status,String decision,String text,int pageNo,int pageSize,String sort){
  valid(pageNo>0&&pageSize>0&&pageSize<=100&&Arrays.asList("code","-code","version","-version").contains(sort));
  List<HrmPayrollRequirementDO> rows=projects.authorizedRows(project,module,actor,"READ").stream().filter(r->status==null||status.equals(r.getStatus())).filter(r->decision==null||decision.equals(r.getScopeDecision())).filter(r->blank(text)||r.getDescription().contains(text)||r.getCode().contains(text)).collect(Collectors.toList());
  Comparator<HrmPayrollRequirementDO> cmp=sort.contains("version")?Comparator.comparing(HrmPayrollRequirementDO::getVersion):Comparator.comparing(HrmPayrollRequirementDO::getCode);
  if(sort.startsWith("-"))cmp=cmp.reversed();rows.sort(cmp.thenComparing(HrmPayrollRequirementDO::getId));
  int from=Math.min(rows.size(),(int)Math.min(Integer.MAX_VALUE,((long)pageNo-1)*pageSize));return map("list",projects.presentRows(project,rows.subList(from,Math.min(rows.size(),from+pageSize))),"total",rows.size());
 }
 public Map<String,Object> export(Long project,Long actor,String module,String status,String decision,String text){
  platform(actor,"export");Map<String,Object> result=statistics(project,actor,"EXPORT",module,status,decision,text);
  result.put("requirements",filtered(project,actor,"EXPORT",module,status,decision,text));result.put("exportedAt",LocalDateTime.now(ZONE).toString());return result;
 }
 public Map<String,Object> statistics(Long project,Long actor,String action,String module){return statistics(project,actor,action,module,null,null,null);}
 @SuppressWarnings("unchecked") public Map<String,Object> statistics(Long project,Long actor,String action,String module,String status,String decision,String text){
  HrmPayrollRequirementProjectDO p=projects.check(project,actor,action,false);
  List<HrmPayrollRequirementDO> visible=projects.rows(project,null,actor,action);
  Set<Long> selected=visible.stream().filter(r->module==null||module.equals(r.getModule())).map(HrmPayrollRequirementDO::getId).collect(Collectors.toCollection(LinkedHashSet::new));
  List<HrmPayrollRequirementDO> rows=filtered(project,actor,action,module,status,decision,text);
  Map<String,Object> scope=collections(p,rows,actor,action);
  scope.put("projectId",project);scope.put("projectScopeVersion",p.getProjectScopeVersion());scope.put("moduleFilter",module);scope.put("filters",map("module",module,"status",status,"decision",decision,"text",text));scope.put("permissionRestricted",!projects.full(project,actor,"READ")||!projects.full(project,actor,action));
  scope.put("label",Boolean.TRUE.equals(scope.get("permissionRestricted"))?"权限受限可见节选（非发布基线）":module==null&&status==null&&decision==null&&blank(text)?"全项目当前统计（非发布基线）":"筛选统计（不改变全项目发布门槛）");
  if((module!=null||status!=null||decision!=null||!blank(text))&&projects.full(project,actor,"READ")&&projects.full(project,actor,action))scope.put("projectTotals",collections(p,visible,actor,action));
  return scope;
 }
 @SuppressWarnings("unchecked") private Map<String,Object> collections(HrmPayrollRequirementProjectDO p,List<HrmPayrollRequirementDO> rows,Long actor,String action){
  Map<String,Set<Long>> sets=new LinkedHashMap<>();for(String key:Arrays.asList("U","E","I","C","T","D"))sets.put(key,new LinkedHashSet<>());
  for(HrmPayrollRequirementDO r:rows){sets.get("U").add(r.getId());if("REMOVED".equals(r.getScopeDecision()))continue;sets.get("E").add(r.getId());if("INCLUDED".equals(r.getScopeDecision()))sets.get("I").add(r.getId());if("DEFERRED".equals(r.getScopeDecision()))sets.get("T").add(r.getId());if("DISPUTED".equals(r.getStatus()))sets.get("D").add(r.getId());if(Boolean.TRUE.equals(r.getCurrentConfirmationValid()))sets.get("C").add(r.getId());}
  Map<String,Object> ready=workbench.readinessSnapshot(p.getId(),actor);
  List<Map<String,Object>> L=((List<Map<String,Object>>)ready.get("items")).stream().filter(x->sets.get("E").contains(Long.valueOf(String.valueOf(x.get("requirementId"))))).collect(Collectors.toList());
  List<Map<String,Object>> current=L.stream().filter(x->sets.get("I").contains(Long.valueOf(String.valueOf(x.get("requirementId"))))).collect(Collectors.toList());
  List<Map<String,Object>> sources=(List<Map<String,Object>>)(List<?>)("EXPORT".equals(action)?workbench.export(p.getId(),actor).get("RELATION"):workbench.list(p.getId(),"RELATION",actor));
  if("EXPORT".equals(action)){
   Set<Long> allowed=sources.stream().map(x->Long.valueOf(String.valueOf(x.get("id")))).collect(Collectors.toSet());
   L=L.stream().filter(x->x.get("relationId")==null||allowed.contains(Long.valueOf(String.valueOf(x.get("relationId"))))).collect(Collectors.toList());
   for(HrmPayrollRequirementDO row:rows)if(sets.get("E").contains(row.getId())&&!("NOT_APPLICABLE".equals(row.getSourceApplicability())&&HrmPayrollWorkbenchService.validExemption(row.getApplicabilityReason()))&&L.stream().noneMatch(x->Objects.equals(String.valueOf(x.get("requirementId")),String.valueOf(row.getId()))))L.add(map("requirementId",row.getId(),"relationId",null,"available",false,"kind","UNSPECIFIED_SOURCE_GAP"));
   current=L.stream().filter(x->sets.get("I").contains(Long.valueOf(String.valueOf(x.get("requirementId"))))).collect(Collectors.toList());
  }
  Set<Long> distinct=new TreeSet<>();Set<Long> relationIds=L.stream().filter(x->x.get("relationId")!=null).map(x->Long.valueOf(String.valueOf(x.get("relationId")))).collect(Collectors.toSet());
  for(Map<String,Object> link:sources)if(relationIds.contains(Long.valueOf(String.valueOf(link.get("id")))))distinct.add(Long.valueOf(String.valueOf(((Map<?,?>)link.get("data")).get("sourceId"))));
  Set<Long> IC=new LinkedHashSet<>(sets.get("I"));IC.retainAll(sets.get("C"));Set<Long> missing=new LinkedHashSet<>(sets.get("I"));missing.removeAll(sets.get("C"));
  Map<String,Object> counts=new LinkedHashMap<>();sets.forEach((k,v)->counts.put(k,v.size()));
  List<Map<String,Object>> issues=((List<Map<String,Object>>)(List<?>)workbench.list(p.getId(),"ISSUE",actor)).stream().filter(x->{Set<Long> affected=new HashSet<>();for(Object id:(Collection<?>)x.get("affectedRequirementIds"))affected.add(Long.valueOf(String.valueOf(id)));return !Collections.disjoint(affected,sets.get("E"));}).collect(Collectors.toList());
  int unresolved=0,overdue=0;List<Long> blockIds=new ArrayList<>();
  for(Map<String,Object> issue:issues){Map<?,?>dates=(Map<?,?>)issue.get("dates"), data=(Map<?,?>)issue.get("data");if(Boolean.TRUE.equals(dates.get("unresolved")))unresolved++;if(Boolean.TRUE.equals(dates.get("overdue")))overdue++;if(Boolean.TRUE.equals(dates.get("unresolved"))&&"BASELINE_BLOCK".equals(data.get("category"))){Collection<?> affected=(Collection<?>)issue.get("affectedRequirementIds");if(Boolean.TRUE.equals(data.get("wholeProject"))||affected.stream().anyMatch(x->sets.get("I").contains(Long.valueOf(String.valueOf(x)))))blockIds.add(Long.valueOf(String.valueOf(issue.get("id"))));}}
  int exempt=(int)rows.stream().filter(x->sets.get("E").contains(x.getId())&&"NOT_APPLICABLE".equals(x.getSourceApplicability())&&HrmPayrollWorkbenchService.validExemption(x.getApplicabilityReason())).count();
  boolean full=projects.full(p.getId(),actor,"READ");
  return map("sets",sets,"counts",counts,"L",L,"sourceIds",distinct,"sourceCount",distinct.size(),"exempt",exempt,"confirmation",ratio(sets.get("C").size(),sets.get("E").size()),"scopeDisposition",ratio(sets.get("I").size()+sets.get("T").size(),sets.get("E").size()),"includedConfirmation",ratio(IC.size(),sets.get("I").size()),"readiness",ratio((int)L.stream().filter(x->Boolean.TRUE.equals(x.get("available"))).count(),L.size()),"includedReadiness",ratio((int)current.stream().filter(x->Boolean.TRUE.equals(x.get("available"))).count(),current.size()),"unresolvedIssues",unresolved,"overdueIssues",overdue,"missingIncludedConfirmationIds",missing,"baselineIssueIds",blockIds,"formalReleaseGate","INDEPENDENT_PUBLISH_CHECK_REQUIRED","fullProjectGateAvailable",full);
 }
 private Map<String,Object> ratio(int numerator,int denominator){return map("numerator",numerator,"denominator",denominator,"label",denominator==0?"暂无适用项（0/0）":numerator+"/"+denominator,"percent",denominator==0?null:java.math.BigDecimal.valueOf(numerator*100.0/denominator).setScale(2,java.math.RoundingMode.HALF_UP));}
}
