package cn.iocoder.yudao.module.hrm.service.payroll;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollWorkbenchVO.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.*;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;

/** All S3 writes serialize on the same project lock used by membership, scope and confirmation.
 * Typed project-owned records hold independent identities. Evidence and history are append-only.
 * Availability is calculated from current immutable tuples; no mutable READY flag exists here. */
@Service
@Transactional(rollbackFor=Exception.class,isolation=Isolation.READ_COMMITTED)
public class HrmPayrollWorkbenchService {
 @Resource private HrmPayrollGovernanceStore governanceStore;
 @Resource @org.springframework.context.annotation.Lazy private HrmPayrollGovernanceDecisionService governanceDecisions;
 @Resource private HrmPayrollWorkbenchRecordMapper records;
 @Resource private HrmPayrollRequirementProjectMapper projects;
 @Resource private HrmPayrollRequirementMapper requirements;
 @Resource private HrmPayrollRequirementHistoryMapper histories;
 @Resource private HrmPayrollRequirementAccessService access;
 @Resource private PermissionApi permissions;
 /** A read-local object graph. No authorization result survives the supplier or a request. */
 private final ThreadLocal<ReadGraph> readGraph=new ThreadLocal<>();
 private static final class ReadGraph {
  final Long project;final Map<Long,HrmPayrollWorkbenchRecordDO> objects=new HashMap<>();
  final Map<String,List<HrmPayrollWorkbenchRecordDO>> kinds=new HashMap<>();
  final Map<String,Object> parsed=new HashMap<>();
  ReadGraph(Long project,List<HrmPayrollWorkbenchRecordDO> rows){this.project=project;for(HrmPayrollWorkbenchRecordDO o:rows){objects.put(o.getId(),o);kinds.computeIfAbsent(o.getKind(),k->new ArrayList<>()).add(o);}}
 }
 public <T>T withReadSnapshot(Long project,java.util.function.Supplier<T> read){
  ReadGraph previous=readGraph.get();if(previous!=null&&Objects.equals(previous.project,project))return read.get();
  ReadGraph current=new ReadGraph(project,records.selectList(new LambdaQueryWrapperX<HrmPayrollWorkbenchRecordDO>().eq(HrmPayrollWorkbenchRecordDO::getProjectId,project).orderByAsc(HrmPayrollWorkbenchRecordDO::getId)));
  readGraph.set(current);try{return read.get();}finally{if(previous==null)readGraph.remove();else readGraph.set(previous);}
 }
 private <T>T read(Long project,java.util.function.Supplier<T> query){return access.withReadAuthorization(()->withReadSnapshot(project,query));}
 private static final Set<String> KINDS=new HashSet<>(Arrays.asList("SOURCE","RELATION","ISSUE","EVIDENCE","HISTORY","TODO","PROPOSAL"));
 private static final ZoneId SHANGHAI=ZoneId.of("Asia/Shanghai");
 static Map<String,Object> map(Object... pairs){Map<String,Object> m=new LinkedHashMap<>();for(int i=0;i<pairs.length;i+=2)m.put((String)pairs[i],pairs[i+1]);return m;}
 public static String digest(Object value){try{byte[] bytes=MessageDigest.getInstance("SHA-256").digest(JsonUtils.toJsonString(value).getBytes(StandardCharsets.UTF_8));StringBuilder b=new StringBuilder();for(byte x:bytes)b.append(String.format("%02x",x&255));return b.toString();}catch(Exception e){throw new IllegalStateException(e);}}
 private static boolean blank(String s){return s==null||s.trim().isEmpty();}
 private void valid(boolean ok){if(!ok)throw exception(PAYROLL_REVIEW_INCOMPLETE);}
 private void denied(boolean ok){if(!ok)throw exception(PAYROLL_PROJECT_DENIED);}
 private void current(Object a,Object b){if(!Objects.equals(a,b))throw exception(PAYROLL_VERSION_CONFLICT);}
 private void platform(Long actor,String action){denied(permissions.hasAnyPermissions(actor,"hrm:payroll:requirement:query"));String suffix;
  if("READ".equals(action))return;
  if("EDIT".equals(action))suffix="update";
  else if("CONFIRM".equals(action))suffix="confirm";
  else suffix=action.toLowerCase(Locale.ROOT).replace('_','-');
  denied(permissions.hasAnyPermissions(actor,"hrm:payroll:requirement:"+suffix));
 }
 private HrmPayrollRequirementProjectDO project(Long id,Long actor,boolean lock){
  if(lock)governanceStore.lock();
  platform(actor,"READ");HrmPayrollRequirementProjectDO p=lock?projects.selectOneForUpdate(HrmPayrollRequirementProjectDO::getId,id):projects.selectById(id);
  denied(p!=null&&access.project(p,actor,"READ"));return p;
 }
 private HrmPayrollRequirementProjectDO context(Context c,Long actor){HrmPayrollRequirementProjectDO p=project(c.getProjectId(),actor,true);current(p.getProjectScopeVersion(),c.getProjectScopeVersion());valid(!blank(c.getReason()));return p;}
 private HrmPayrollWorkbenchRecordDO object(Long project,Long id,String kind){ReadGraph graph=readGraph.get();HrmPayrollWorkbenchRecordDO o=id==null?null:graph!=null&&Objects.equals(graph.project,project)?graph.objects.get(id):records.selectById(id);denied(o!=null&&Objects.equals(o.getProjectId(),project)&&kind.equals(o.getKind()));return o;}
 @SuppressWarnings("unchecked") private <T>T data(HrmPayrollWorkbenchRecordDO o,Class<T> type){ReadGraph graph=readGraph.get();if(graph==null||!Objects.equals(graph.project,o.getProjectId()))return JsonUtils.parseObject(o.getDataJson(),type);return (T)graph.parsed.computeIfAbsent(o.getId()+":"+type.getName(),key->JsonUtils.parseObject(o.getDataJson(),type));}
 private List<HrmPayrollWorkbenchRecordDO> all(Long project,String kind){ReadGraph graph=readGraph.get();if(graph!=null&&Objects.equals(graph.project,project))return graph.kinds.getOrDefault(kind,Collections.emptyList());return records.selectList(new LambdaQueryWrapperX<HrmPayrollWorkbenchRecordDO>().eq(HrmPayrollWorkbenchRecordDO::getProjectId,project).eq(HrmPayrollWorkbenchRecordDO::getKind,kind).orderByAsc(HrmPayrollWorkbenchRecordDO::getId));}
 private HrmPayrollWorkbenchRecordDO insert(Long project,String kind,Object value){HrmPayrollWorkbenchRecordDO o=new HrmPayrollWorkbenchRecordDO();o.setProjectId(project);o.setKind(kind);o.setVersion(1);o.setDataJson(JsonUtils.toJsonString(value));records.insert(o);return o;}
 private void update(HrmPayrollWorkbenchRecordDO o,Object value){o.setVersion(o.getVersion()+1);o.setDataJson(JsonUtils.toJsonString(value));records.updateById(o);}
 private HrmPayrollRequirementDO requirement(Long project,Long id){HrmPayrollRequirementDO r=requirements.selectById(id);denied(r!=null&&Objects.equals(r.getProjectId(),project));return r;}
 private void row(HrmPayrollRequirementProjectDO p,HrmPayrollRequirementDO r,Long actor,String action){platform(actor,action);access.requireRow(p,r,actor,action);}
 private boolean sourceAllowed(HrmPayrollRequirementProjectDO p,HrmPayrollWorkbenchRecordDO o,Source s,Long actor,String action){return access.source(p,o==null?null:o.getId(),s.getModule(),s.getScopeJson(),actor,"READ")&&access.source(p,o==null?null:o.getId(),s.getModule(),s.getScopeJson(),actor,action);}
 private void sourceAccess(HrmPayrollRequirementProjectDO p,HrmPayrollWorkbenchRecordDO o,Source s,Long actor,String action){platform(actor,action);denied(sourceAllowed(p,o,s,actor,action));}
 private void sourceOwner(HrmPayrollRequirementProjectDO p,HrmPayrollWorkbenchRecordDO o,Source s){denied(access.activeUser(s.getOwnerId())&&sourceAllowed(p,o,s,s.getOwnerId(),"READ"));}
 private List<HrmPayrollWorkbenchRecordDO> relations(Long project,Long source,Long req,boolean active){return all(project,"RELATION").stream().filter(o->{Relation r=data(o,Relation.class);return (source==null||source.equals(r.getSourceId()))&&(req==null||req.equals(r.getRequirementId()))&&(!active||Boolean.TRUE.equals(r.getActive()));}).collect(Collectors.toList());}
 private Set<Long> impacted(Long project,Long source){return relations(project,source,null,true).stream().map(o->data(o,Relation.class).getRequirementId()).collect(Collectors.toCollection(TreeSet::new));}
 private void relationAccess(HrmPayrollRequirementProjectDO p,HrmPayrollWorkbenchRecordDO o,Long actor,String action){Relation r=data(o,Relation.class);row(p,requirement(p.getId(),r.getRequirementId()),actor,action);HrmPayrollWorkbenchRecordDO s=object(p.getId(),r.getSourceId(),"SOURCE");sourceAccess(p,s,data(s,Source.class),actor,"READ");}
 private List<Map<String,Object>> authority(Long project,Long actor){return access.active(project,actor).stream().map(g->map("grantId",g.getId(),"grantVersion",g.getGrantVersion(),"parentGrantId",g.getParentGrantId(),"actions",g.getActions(),"ceiling",g.getCeilingJson(),"requirementIds",g.getRequirementIdsJson(),"sourceIds",g.getSourceIdsJson(),"validFrom",g.getValidFrom(),"validTo",g.getValidTo(),"basis",g.getEvidence())).collect(Collectors.toList());}
 private void event(HrmPayrollRequirementProjectDO p,HrmPayrollWorkbenchRecordDO o,String action,Long actor,String reason,Set<Long> affected){
  insert(p.getId(),"HISTORY",map("objectId",o.getId(),"kind",o.getKind(),"objectVersion",o.getVersion(),"action",action,"actor",actor,"time",LocalDateTime.now(SHANGHAI).toString(),"reason",reason,"snapshot",JsonUtils.parseObject(o.getDataJson(),Map.class),"affectedRequirementIds",affected));
 }
 private void todo(HrmPayrollRequirementProjectDO p,HrmPayrollWorkbenchRecordDO o,String reason,Set<Long> affected){
  Set<Long> recipients=new TreeSet<>();if(p.getOwnerId()!=null)recipients.add(p.getOwnerId());
  for(Long id:affected){HrmPayrollRequirementDO r=requirement(p.getId(),id);if(r.getReviewerId()!=null)recipients.add(r.getReviewerId());if(r.getSourceOwnerId()!=null)recipients.add(r.getSourceOwnerId());if(r.getCreator()!=null)try{recipients.add(Long.valueOf(r.getCreator()));}catch(NumberFormatException ignored){}}
  if("SOURCE".equals(o.getKind()))recipients.add(data(o,Source.class).getOwnerId());
  if("ISSUE".equals(o.getKind())){Issue issue=data(o,Issue.class);recipients.add(issue.getHandlerId());recipients.add(issue.getReviewerId());}
  for(HrmPayrollRequirementProjectGrantDO g:recordsForVerifiers(p.getId()))recipients.add(g.getUserId());
  insert(p.getId(),"TODO",map("objectId",o.getId(),"kind",o.getKind(),"objectVersion",o.getVersion(),"reason",reason,"affectedRequirementIds",affected,"recipients",recipients,"state","OPEN"));
 }
 private List<HrmPayrollRequirementProjectGrantDO> recordsForVerifiers(Long project){return grants.selectList(new LambdaQueryWrapperX<HrmPayrollRequirementProjectGrantDO>().eq(HrmPayrollRequirementProjectGrantDO::getProjectId,project)).stream().filter(g->access.effective(g)&&access.actions(g.getActions()).contains("SOURCE_VERIFY")).collect(Collectors.toList());}
 @Resource private HrmPayrollRequirementProjectGrantMapper grants;
 private void invalidate(HrmPayrollRequirementDO r,Long actor){
  if("REMOVED".equals(r.getScopeDecision()))return;
  r.setSemanticVersion(r.getSemanticVersion()==null?2:r.getSemanticVersion()+1);r.setVersion(r.getVersion()+1);
  if("CONFIRMED".equals(r.getStatus()))r.setStatus("REVIEWING");
  requirements.update(null,new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<HrmPayrollRequirementDO>().eq(HrmPayrollRequirementDO::getId,r.getId()).set(HrmPayrollRequirementDO::getSemanticVersion,r.getSemanticVersion()).set(HrmPayrollRequirementDO::getVersion,r.getVersion()).set(HrmPayrollRequirementDO::getStatus,r.getStatus()).set(HrmPayrollRequirementDO::getConfirmedBy,null).set(HrmPayrollRequirementDO::getConfirmedAt,null).set(HrmPayrollRequirementDO::getConfirmedScopeVersion,null).set(HrmPayrollRequirementDO::getConfirmedScopeJson,null).set(HrmPayrollRequirementDO::getConfirmedTupleJson,null));
  r=requirements.selectById(r.getId());HrmPayrollRequirementHistoryDO h=new HrmPayrollRequirementHistoryDO();h.setRequirementId(r.getId());h.setVersion(r.getVersion());h.setActorId(actor);h.setSnapshot(JsonUtils.toJsonString(r));histories.insert(h);
 }
 private Object sourceSemantic(Source s){return map("module",s.getModule(),"scope",HrmPayrollRequirementScope.parse(s.getScopeJson()),"acquisition",s.getAcquisition(),"specification",s.getSpecification(),"uniqueKey",s.getUniqueKey(),"employeeMatch",s.getEmployeeMatch(),"frequency",s.getFrequency(),"cutoff",s.getCutoff(),"fields",s.getFields());}
 private Object mappingSemantic(Relation r){return map("fields",r.getFields());}
 public Long saveSource(SourceSave c,Long actor){
  HrmPayrollRequirementProjectDO p=context(c,actor);Source next=c.getSource();HrmPayrollWorkbenchRecordDO o=c.getId()==null?null:object(p.getId(),c.getId(),"SOURCE");Source old=o==null?null:data(o,Source.class);
  if(o!=null){sourceAccess(p,o,old,actor,"SOURCE_EDIT");current(o.getVersion(),c.getVersion());}
  sourceAccess(p,o,next,actor,"SOURCE_EDIT");valid(HrmPayrollRequirementScope.parse(p.getScopeJson()).contains(HrmPayrollRequirementScope.parse(next.getScopeJson())));sourceOwner(p,o,next);
  boolean semantic=old!=null&&!digest(sourceSemantic(old)).equals(digest(sourceSemantic(next)));
  Set<Long> affected=o==null?new TreeSet<>():impacted(p.getId(),o.getId());
  if(semantic)for(Long id:affected)denied(access.row(p,requirement(p.getId(),id),actor,"EDIT"));
  next.setSemanticVersion(old==null?1:old.getSemanticVersion()+(semantic?1:0));next.setOperationalVersion(old==null?1:old.getOperationalVersion()+1);
  next.setDeliveryVersion(old==null?0:old.getDeliveryVersion());next.setSupply(old==null?"UNINVENTORIED":old.getSupply());next.setDeliveryDigest(old==null?null:old.getDeliveryDigest());next.setDeliveryReference(old==null?null:old.getDeliveryReference());next.setCopiedFromProjectId(old==null?null:old.getCopiedFromProjectId());next.setCopiedFromSourceId(old==null?null:old.getCopiedFromSourceId());
  if(o==null)o=insert(p.getId(),"SOURCE",next);else update(o,next);
  if(semantic)for(Long id:affected)invalidate(requirement(p.getId(),id),actor);
  event(p,o,semantic?"SOURCE_SEMANTIC":"SOURCE_OPERATIONAL",actor,c.getReason(),affected);todo(p,o,c.getReason(),affected);return o.getId();
 }
 public Long copy(Copy c,Long actor){
  // Canonical project lock order, including inverse copies.
  Long lo=Math.min(c.getProjectId(),c.getSourceProjectId()),hi=Math.max(c.getProjectId(),c.getSourceProjectId());project(lo,actor,true);if(!lo.equals(hi))project(hi,actor,true);
  HrmPayrollRequirementProjectDO target=context(c,actor),from=project(c.getSourceProjectId(),actor,false);denied(!target.getId().equals(from.getId()));
  HrmPayrollWorkbenchRecordDO original=object(from.getId(),c.getSourceId(),"SOURCE");Source s=data(original,Source.class);sourceAccess(from,original,s,actor,"READ");current(original.getVersion(),c.getSourceVersion());
  sourceAccess(target,null,s,actor,"SOURCE_EDIT");valid(HrmPayrollRequirementScope.parse(target.getScopeJson()).contains(HrmPayrollRequirementScope.parse(s.getScopeJson())));sourceOwner(target,null,s);
  s.setSemanticVersion(1);s.setOperationalVersion(1);s.setDeliveryVersion(0);s.setSupply("UNINVENTORIED");s.setDeliveryDigest(null);s.setDeliveryReference(null);s.setCopiedFromProjectId(from.getId());s.setCopiedFromSourceId(original.getId());
  HrmPayrollWorkbenchRecordDO result=insert(target.getId(),"SOURCE",s);event(target,result,"SOURCE_COPY",actor,c.getReason(),new TreeSet<>());return result.getId();
 }
 public Long saveRelation(RelationSave c,Long actor){
  HrmPayrollRequirementProjectDO p=context(c,actor);Relation r=c.getRelation();HrmPayrollRequirementDO req=requirement(p.getId(),r.getRequirementId());row(p,req,actor,"EDIT");valid(!"NOT_APPLICABLE".equals(req.getSourceApplicability()));
  HrmPayrollWorkbenchRecordDO source=object(p.getId(),r.getSourceId(),"SOURCE");Source s=data(source,Source.class);sourceAccess(p,source,s,actor,"READ");
  HrmPayrollWorkbenchRecordDO o=c.getId()==null?null:object(p.getId(),c.getId(),"RELATION");Relation old=o==null?null:data(o,Relation.class);
  if(old!=null){relationAccess(p,o,actor,"EDIT");current(o.getVersion(),c.getVersion());denied(Objects.equals(old.getSourceId(),r.getSourceId())&&Objects.equals(old.getRequirementId(),r.getRequirementId()));valid(Boolean.TRUE.equals(old.getActive()));}
  valid(relations(p.getId(),r.getSourceId(),r.getRequirementId(),true).stream().noneMatch(x->!Objects.equals(x.getId(),c.getId())));
  Set<String> fields=s.getFields().stream().map(Field::getName).collect(Collectors.toSet());valid(r.getFields().stream().allMatch(x->fields.contains(x.getSourceField())));
  boolean changed=old==null||!digest(mappingSemantic(old)).equals(digest(mappingSemantic(r)));r.setMappingVersion(old==null?1:old.getMappingVersion()+(changed?1:0));r.setActive(true);
  if(o==null)o=insert(p.getId(),"RELATION",r);else update(o,r);
  if(changed)invalidate(req,actor);
  Set<Long> affected=new TreeSet<>(Arrays.asList(req.getId()));event(p,o,changed?"MAPPING":"CONSUMPTION_RULE",actor,c.getReason(),affected);todo(p,o,c.getReason(),affected);return o.getId();
 }
 public void unlink(Context c,Long actor){HrmPayrollRequirementProjectDO p=context(c,actor);HrmPayrollWorkbenchRecordDO o=object(p.getId(),c.getId(),"RELATION");relationAccess(p,o,actor,"EDIT");current(o.getVersion(),c.getVersion());Relation r=data(o,Relation.class);valid(Boolean.TRUE.equals(r.getActive()));r.setActive(false);update(o,r);invalidate(requirement(p.getId(),r.getRequirementId()),actor);Set<Long>a=new TreeSet<>(Arrays.asList(r.getRequirementId()));event(p,o,"UNLINK",actor,c.getReason(),a);todo(p,o,c.getReason(),a);}
 public void supply(Supply c,Long actor){
  HrmPayrollRequirementProjectDO p=context(c,actor);HrmPayrollWorkbenchRecordDO o=object(p.getId(),c.getId(),"SOURCE");Source s=data(o,Source.class);sourceAccess(p,o,s,actor,"SOURCE_EDIT");current(o.getVersion(),c.getVersion());
  if(Boolean.TRUE.equals(c.getReplaceDelivery())){valid(!blank(c.getDeliveryDigest())&&!blank(c.getDeliveryReference()));s.setDeliveryVersion(s.getDeliveryVersion()+1);s.setDeliveryDigest(c.getDeliveryDigest());s.setDeliveryReference(c.getDeliveryReference());}
  if("PROVIDED".equals(c.getSupply()))valid(s.getDeliveryVersion()>0&&!blank(s.getDeliveryDigest()));s.setSupply(c.getSupply());s.setOperationalVersion(s.getOperationalVersion()+1);update(o,s);Set<Long>a=impacted(p.getId(),o.getId());event(p,o,"SUPPLY",actor,c.getReason(),a);todo(p,o,c.getReason(),a);
 }
 private Map<String,Object> sourceBinding(HrmPayrollRequirementProjectDO p,HrmPayrollWorkbenchRecordDO o,Source s,String layer){
  Map<String,Object>b=map("tenantId",cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.getRequiredTenantId(),"projectId",p.getId(),"projectScopeVersion",p.getProjectScopeVersion(),"sourceId",o.getId(),"semanticVersion",s.getSemanticVersion());
  if("SOURCE_SEMANTIC".equals(layer)){b.put("ownerId",s.getOwnerId());b.put("scope",HrmPayrollRequirementScope.parse(s.getScopeJson()));}
  else{b.put("deliveryVersion",s.getDeliveryVersion());b.put("deliveryDigest",s.getDeliveryDigest());b.put("checkRule",s.getQualityRule());}return b;
 }
 private Map<String,Object> relationSemanticBinding(HrmPayrollRequirementProjectDO p,HrmPayrollWorkbenchRecordDO o,Relation r){HrmPayrollWorkbenchRecordDO src=object(p.getId(),r.getSourceId(),"SOURCE");Source s=data(src,Source.class);HrmPayrollRequirementDO req=requirement(p.getId(),r.getRequirementId());return map("projectId",p.getId(),"projectScopeVersion",p.getProjectScopeVersion(),"sourceId",src.getId(),"sourceSemanticVersion",s.getSemanticVersion(),"sourceOwnerId",s.getOwnerId(),"relationId",o.getId(),"mappingVersion",r.getMappingVersion(),"resolvedScopeDigest",digest(req.getScopeJson()));}
 @SuppressWarnings("unchecked") private Map<String,Object> evidenceData(HrmPayrollWorkbenchRecordDO o){return data(o,Map.class);}
 private HrmPayrollWorkbenchRecordDO latestEvidence(Long project,String layer,Long objectId,String binding){List<HrmPayrollWorkbenchRecordDO> list=new ArrayList<>(all(project,"EVIDENCE"));Collections.reverse(list);return list.stream().filter(o->{Map<String,Object> e=evidenceData(o);return layer.equals(e.get("layer"))&&String.valueOf(objectId).equals(String.valueOf(e.get("objectId")))&&binding.equals(e.get("bindingDigest"));}).findFirst().orElse(null);}
 private boolean pass(HrmPayrollWorkbenchRecordDO e){return e!=null&&"PASS".equals(evidenceData(e).get("result"));}
 private HrmPayrollWorkbenchRecordDO qs(HrmPayrollRequirementProjectDO p,HrmPayrollWorkbenchRecordDO src,Source s){return latestEvidence(p.getId(),"QS",src.getId(),digest(sourceBinding(p,src,s,"QS")));}
 private Map<String,Object> qcBinding(HrmPayrollRequirementProjectDO p,HrmPayrollWorkbenchRecordDO o,Relation r){
  HrmPayrollWorkbenchRecordDO src=object(p.getId(),r.getSourceId(),"SOURCE");Source s=data(src,Source.class);HrmPayrollWorkbenchRecordDO qs=qs(p,src,s);return map("tenantId",cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.getRequiredTenantId(),"projectId",p.getId(),"QS",qs==null?null:qs.getId(),"QSVersion",qs==null?null:qs.getVersion(),"relationId",o.getId(),"mappingVersion",r.getMappingVersion(),"resolvedScopeDigest",digest(requirement(p.getId(),r.getRequirementId()).getScopeJson()),"consumptionRule",r.getConsumptionRule());
 }
 private Map<String,Object> binding(HrmPayrollRequirementProjectDO p,HrmPayrollWorkbenchRecordDO o,String layer){
  if(layer.equals("SOURCE_SEMANTIC")||layer.equals("QS")){denied("SOURCE".equals(o.getKind()));return sourceBinding(p,o,data(o,Source.class),layer);}
  denied("RELATION".equals(o.getKind()));Relation r=data(o,Relation.class);valid(Boolean.TRUE.equals(r.getActive()));return layer.equals("RELATION_SEMANTIC")?relationSemanticBinding(p,o,r):qcBinding(p,o,r);
 }
 public Long verify(Verify c,Long actor){
  HrmPayrollRequirementProjectDO p=context(c,actor);HrmPayrollWorkbenchRecordDO o=object(p.getId(),c.getId(),c.getLayer().equals("QS")||c.getLayer().equals("SOURCE_SEMANTIC")?"SOURCE":"RELATION");current(o.getVersion(),c.getVersion());
  HrmPayrollWorkbenchRecordDO src=o;Relation relation=null;
  if("RELATION".equals(o.getKind())){relation=data(o,Relation.class);relationAccess(p,o,actor,"READ");HrmPayrollRequirementDO req=requirement(p.getId(),relation.getRequirementId());row(p,req,actor,"SOURCE_VERIFY");valid("MATCH".equals(req.getScopeState()));src=object(p.getId(),relation.getSourceId(),"SOURCE");}
  Source s=data(src,Source.class);sourceAccess(p,src,s,actor,"SOURCE_VERIFY");sourceOwner(p,src,s);valid(HrmPayrollRequirementScope.parse(p.getScopeJson()).contains(HrmPayrollRequirementScope.parse(s.getScopeJson())));
  Map<String,Object>b=binding(p,o,c.getLayer());current(digest(b),c.getExpectedBinding());
  if("QS".equals(c.getLayer()))valid(s.getDeliveryVersion()>0&&!blank(s.getDeliveryDigest()));
  if("QC".equals(c.getLayer()))valid(pass(qs(p,src,s)));
  if(relation!=null)valid(HrmPayrollRequirementScope.parse(s.getScopeJson()).contains(HrmPayrollRequirementScope.parse(requirement(p.getId(),relation.getRequirementId()).getScopeJson())));
  HrmPayrollWorkbenchRecordDO evidence=insert(p.getId(),"EVIDENCE",map("layer",c.getLayer(),"objectId",o.getId(),"binding",b,"bindingDigest",digest(b),"result",c.getResult(),"reference",c.getReference(),"sampleDigest",c.getSampleDigest(),"executionReason",c.getReason(),"actorId",actor,"executedAt",LocalDateTime.now(SHANGHAI).toString(),"authority",authority(p.getId(),actor),"governance",governanceDecisions.forVerification(p.getId(),actor,s.getScopeJson(),relation==null?null:relation.getRequirementId(),src.getId())));
  event(p,o,c.getLayer(),actor,c.getReason(),src==o?impacted(p.getId(),src.getId()):new TreeSet<>(Arrays.asList(relation.getRequirementId())));return evidence.getId();
 }
 private boolean semanticReady(HrmPayrollRequirementProjectDO p,HrmPayrollWorkbenchRecordDO o,Relation r){HrmPayrollWorkbenchRecordDO src=object(p.getId(),r.getSourceId(),"SOURCE");Source s=data(src,Source.class);return pass(latestEvidence(p.getId(),"SOURCE_SEMANTIC",src.getId(),digest(sourceBinding(p,src,s,"SOURCE_SEMANTIC"))))&&pass(latestEvidence(p.getId(),"RELATION_SEMANTIC",o.getId(),digest(relationSemanticBinding(p,o,r))));}
 public boolean specificationReady(HrmPayrollRequirementDO r){if("NOT_APPLICABLE".equals(r.getSourceApplicability()))return validExemption(r.getApplicabilityReason());HrmPayrollRequirementProjectDO p=projects.selectById(r.getProjectId());List<HrmPayrollWorkbenchRecordDO> links=relations(p.getId(),null,r.getId(),true);return !links.isEmpty()&&links.stream().allMatch(o->semanticReady(p,o,data(o,Relation.class)));}
 public boolean available(HrmPayrollRequirementProjectDO p,HrmPayrollWorkbenchRecordDO o){Relation r=data(o,Relation.class);if(!Boolean.TRUE.equals(r.getActive()))return false;HrmPayrollRequirementDO req=requirement(p.getId(),r.getRequirementId());HrmPayrollWorkbenchRecordDO src=object(p.getId(),r.getSourceId(),"SOURCE");Source s=data(src,Source.class);
  return !"NOT_APPLICABLE".equals(req.getSourceApplicability())&&"MATCH".equals(req.getScopeState())&&p.getScopeJson()!=null&&HrmPayrollRequirementScope.parse(p.getScopeJson()).contains(HrmPayrollRequirementScope.parse(s.getScopeJson()))&&HrmPayrollRequirementScope.parse(s.getScopeJson()).contains(HrmPayrollRequirementScope.parse(req.getScopeJson()))&&access.activeUser(s.getOwnerId())&&sourceAllowed(p,src,s,s.getOwnerId(),"READ")&&"PROVIDED".equals(s.getSupply())&&semanticReady(p,o,r)&&pass(qs(p,src,s))&&pass(latestEvidence(p.getId(),"QC",o.getId(),digest(qcBinding(p,o,r))))&&!dataBlocked(p,req.getId(),src.getId());
 }
 public static boolean validExemption(String reason){if(blank(reason))return false;String x=reason.trim();return x.length()>=8&&!Arrays.asList("N/A","NA","不适用").contains(x.toUpperCase(Locale.ROOT));}
 public String readiness(HrmPayrollRequirementDO req){if("NOT_APPLICABLE".equals(req.getSourceApplicability())&&validExemption(req.getApplicabilityReason()))return "EXEMPT";HrmPayrollRequirementProjectDO p=projects.selectById(req.getProjectId());List<HrmPayrollWorkbenchRecordDO>rs=relations(p.getId(),null,req.getId(),true);if(rs.isEmpty())return "MISSING";long n=rs.stream().filter(o->available(p,o)).count();return n==rs.size()?"READY":n==0?"MISSING":"PARTIAL";}
 public Map<String,Object> readinessSnapshot(Long project,Long actor){return read(project,()->readinessSnapshotRead(project,actor));}
    private Map<String,Object> readinessSnapshotRead(Long project,Long actor){
  HrmPayrollRequirementProjectDO p=project(project,actor,false);List<Object> items=new ArrayList<>();int gaps=0,usable=0,exempt=0;
  for(HrmPayrollRequirementDO req:requirements.selectList(new LambdaQueryWrapperX<HrmPayrollRequirementDO>().eq(HrmPayrollRequirementDO::getProjectId,project))){
   if("REMOVED".equals(req.getScopeDecision())||!access.row(p,req,actor,"READ"))continue;
   if("NOT_APPLICABLE".equals(req.getSourceApplicability())&&validExemption(req.getApplicabilityReason())){exempt++;continue;}
   List<HrmPayrollWorkbenchRecordDO> links=relations(project,null,req.getId(),true).stream().filter(o->visible(p,o,actor)).collect(Collectors.toList());
   if("NOT_APPLICABLE".equals(req.getSourceApplicability())||links.isEmpty()){gaps++;items.add(map("requirementId",req.getId(),"relationId",null,"available",false,"kind","UNSPECIFIED_SOURCE_GAP"));continue;}
   for(HrmPayrollWorkbenchRecordDO o:links){boolean ready=available(p,o);if(ready)usable++;items.add(map("requirementId",req.getId(),"relationId",o.getId(),"available",ready,"kind","RELATION"));}
  }
  return map("items",items,"available",usable,"denominator",items.size(),"gaps",gaps,"exempt",exempt,"permissionRestricted",!access.full(p,actor,"READ"),"notApplicableLabel",items.isEmpty()?"暂无适用项（0/0）":null);
 }
 @SuppressWarnings("unchecked") public String visibleConfirmationTuple(String json,Long project,Long actor){
  if(json==null)return null;Map<String,Object> tuple=JsonUtils.parseObject(json,Map.class);tuple.remove("authority");if(tuple.remove("governance")!=null)tuple.put("governanceReferencesRestricted",true);
  if(tuple.get("sources") instanceof List){List<Object> visible=new ArrayList<>();for(Object raw:(List<?>)tuple.get("sources")){Map<String,Object> ref=(Map<String,Object>)raw;HrmPayrollWorkbenchRecordDO o=records.selectById(Long.valueOf(String.valueOf(ref.get("sourceId"))));if(o!=null&&Objects.equals(o.getProjectId(),project)&&visible(project(project,actor,false),o,actor))visible.add(ref);else tuple.put("sourceReferencesRestricted",true);}tuple.put("sources",visible);}
  return JsonUtils.toJsonString(tuple);
 }
 @SuppressWarnings("unchecked") public boolean currentConfirmationValid(HrmPayrollRequirementDO req){
  if(!"CONFIRMED".equals(req.getStatus())||req.getConfirmedTupleJson()==null)return false;
  try{Map<String,Object> tuple=JsonUtils.parseObject(req.getConfirmedTupleJson(),Map.class);HrmPayrollRequirementProjectDO p=projects.selectById(req.getProjectId());
   if(p==null||!Objects.equals(p.getProjectScopeVersion(),req.getConfirmedScopeVersion())||!String.valueOf(p.getProjectScopeVersion()).equals(String.valueOf(tuple.get("projectScopeVersion")))||!digest(req.getScopeJson()).equals(tuple.get("resolvedScopeDigest"))||!String.valueOf(req.getSemanticVersion()==null?1:req.getSemanticVersion()).equals(String.valueOf(tuple.get("semanticVersion")))||!Objects.equals(req.getSourceApplicability(),tuple.get("applicability"))||!Objects.equals(req.getRuleVersion(),tuple.get("ruleVersion")))return false;
   List<Object> refs=new ArrayList<>();if("APPLICABLE".equals(req.getSourceApplicability()))for(HrmPayrollWorkbenchRecordDO o:relations(p.getId(),null,req.getId(),true)){Relation r=data(o,Relation.class);HrmPayrollWorkbenchRecordDO src=object(p.getId(),r.getSourceId(),"SOURCE");refs.add(map("sourceId",src.getId(),"semanticVersion",data(src,Source.class).getSemanticVersion(),"relationId",o.getId(),"mappingVersion",r.getMappingVersion()));}
   return governanceDecisions.currentSemantic(req)&&digest(refs).equals(digest(tuple.get("sources")))&&(!"NOT_APPLICABLE".equals(req.getSourceApplicability())||validExemption(req.getApplicabilityReason()));
  }catch(RuntimeException invalid){return false;}
 }
 public String confirmationTuple(HrmPayrollRequirementDO req,Long actor){
  HrmPayrollRequirementProjectDO p=project(req.getProjectId(),actor,true);
  if(p.getProjectScopeVersion()==null||!"MATCH".equals(req.getScopeState()))throw new cn.iocoder.yudao.framework.common.exception.ServiceException(PAYROLL_REVIEW_INCOMPLETE.getCode(),"请补齐以下字段：需求适用范围（须与当前项目范围匹配）");
  HrmPayrollReviewService.required("规则引用版本",req.getRuleVersion(),"功能说明",req.getDescription(),"验收条件",req.getAcceptance(),"依据或异议说明",req.getEvidence());
  List<Object> refs=new ArrayList<>();
  if("NOT_APPLICABLE".equals(req.getSourceApplicability())){
   if(!validExemption(req.getApplicabilityReason()))throw new cn.iocoder.yudao.framework.common.exception.ServiceException(PAYROLL_REVIEW_INCOMPLETE.getCode(),"请补齐以下字段：不适用理由（至少8个字符，说明为何不消费业务数据）");
  }else{
   if(!"APPLICABLE".equals(req.getSourceApplicability()))throw new cn.iocoder.yudao.framework.common.exception.ServiceException(PAYROLL_REVIEW_INCOMPLETE.getCode(),"请补齐以下字段：数据来源适用性（适用或不适用）");
   List<HrmPayrollWorkbenchRecordDO> rs=relations(p.getId(),null,req.getId(),true);
   if(rs.isEmpty())throw new cn.iocoder.yudao.framework.common.exception.ServiceException(PAYROLL_REVIEW_INCOMPLETE.getCode(),"请补齐以下字段：数据来源消费关系及字段映射（至少一条有效关系）");
   for(HrmPayrollWorkbenchRecordDO o:rs){
    Relation r=data(o,Relation.class);relationAccess(p,o,actor,"READ");
    HrmPayrollWorkbenchRecordDO src=object(p.getId(),r.getSourceId(),"SOURCE");Source s=data(src,Source.class);sourceOwner(p,src,s);
    if(r.getFields().isEmpty())throw new cn.iocoder.yudao.framework.common.exception.ServiceException(PAYROLL_REVIEW_INCOMPLETE.getCode(),"请补齐以下字段：消费关系"+o.getId()+"的字段映射（至少一个字段）");
    if(!HrmPayrollRequirementScope.parse(s.getScopeJson()).contains(HrmPayrollRequirementScope.parse(req.getScopeJson())))throw new cn.iocoder.yudao.framework.common.exception.ServiceException(PAYROLL_REVIEW_INCOMPLETE.getCode(),"请补齐以下字段：数据来源"+src.getId()+"的适用范围（须覆盖需求适用范围）");
    refs.add(map("sourceId",src.getId(),"semanticVersion",s.getSemanticVersion(),"relationId",o.getId(),"mappingVersion",r.getMappingVersion()));
   }
  }
  return JsonUtils.toJsonString(map("tenantId",cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.getRequiredTenantId(),"projectId",p.getId(),"projectScopeVersion",p.getProjectScopeVersion(),"resolvedScopeDigest",digest(req.getScopeJson()),"requirementId",req.getId(),"semanticVersion",req.getSemanticVersion()==null?1:req.getSemanticVersion(),"applicability",req.getSourceApplicability(),"applicabilityReason",req.getApplicabilityReason(),"sources",refs,"ruleVersion",req.getRuleVersion(),"confirmedBy",actor,"confirmedAt",LocalDateTime.now(SHANGHAI).toString(),"authority",authority(p.getId(),actor),"governance",governanceDecisions.forRequirement(req,actor)));
 }
 /** Called inside the requirement save transaction, after its project lock. */
 public void applicabilityChanged(HrmPayrollRequirementDO previous,HrmPayrollRequirementDO next,Long actor){if(previous==null||Objects.equals(previous.getSourceApplicability(),next.getSourceApplicability())||!"NOT_APPLICABLE".equals(next.getSourceApplicability()))return;
  HrmPayrollRequirementProjectDO p=projects.selectById(next.getProjectId());for(HrmPayrollWorkbenchRecordDO o:relations(p.getId(),null,next.getId(),true)){Relation r=data(o,Relation.class);r.setActive(false);update(o,r);Set<Long>a=new TreeSet<>(Arrays.asList(next.getId()));event(p,o,"APPLICABILITY_DEACTIVATE",actor,"来源适用性变更，旧关系留历史",a);todo(p,o,"来源适用性变更",a);}}
 /** Scope version is in every evidence tuple; event/todos are persisted in the scope transaction. */
 public void scopeChanged(HrmPayrollRequirementProjectDO p,Long actor){for(HrmPayrollWorkbenchRecordDO o:all(p.getId(),"SOURCE")){Set<Long>a=impacted(p.getId(),o.getId());event(p,o,"SCOPE_REVERIFY",actor,"项目范围修订，旧语义和QS/QC失配；来源和交付版本保留",a);todo(p,o,"项目范围修订后重新核验",a);}}
 public Long propose(Proposal c,Long actor){HrmPayrollRequirementProjectDO p=context(c,actor);HrmPayrollWorkbenchRecordDO o=object(p.getId(),c.getId(),"SOURCE");sourceAccess(p,o,data(o,Source.class),actor,"READ");current(o.getVersion(),c.getVersion());HrmPayrollWorkbenchRecordDO proposal=insert(p.getId(),"PROPOSAL",map("objectId",o.getId(),"sourceVersion",o.getVersion(),"actor",actor,"suggestion",c.getSuggestion(),"reason",c.getReason(),"state","PENDING_OWNER"));todo(p,o,"来源变更建议："+c.getSuggestion(),impacted(p.getId(),o.getId()));return proposal.getId();}
 private Set<Long> issueImpact(Long p,Issue issue){Set<Long> ids=new TreeSet<>(issue.getRequirementIds());if(issue.getRelationIds()!=null)for(Long id:issue.getRelationIds()){Relation rel=data(object(p,id,"RELATION"),Relation.class);ids.add(rel.getRequirementId());}for(Long id:issue.getSourceIds())ids.addAll(impacted(p,id));if(Boolean.TRUE.equals(issue.getWholeProject()))ids.addAll(requirements.selectList(new LambdaQueryWrapperX<HrmPayrollRequirementDO>().eq(HrmPayrollRequirementDO::getProjectId,p)).stream().map(HrmPayrollRequirementDO::getId).collect(Collectors.toList()));return ids;}
 private void issueAccess(HrmPayrollRequirementProjectDO p,Issue issue,Long actor,String action){
  platform(actor,action);denied(access.activeUser(actor));if(Boolean.TRUE.equals(issue.getWholeProject()))denied(access.full(p,actor,"READ")&&access.full(p,actor,action));
  for(Long id:issueImpact(p.getId(),issue))row(p,requirement(p.getId(),id),actor,action);
  for(Long id:issue.getSourceIds()){HrmPayrollWorkbenchRecordDO o=object(p.getId(),id,"SOURCE");sourceAccess(p,o,data(o,Source.class),actor,action);}
  if(issue.getRelationIds()!=null)for(Long id:issue.getRelationIds()){HrmPayrollWorkbenchRecordDO link=object(p.getId(),id,"RELATION");Relation r=data(link,Relation.class);row(p,requirement(p.getId(),r.getRequirementId()),actor,action);HrmPayrollWorkbenchRecordDO src=object(p.getId(),r.getSourceId(),"SOURCE");sourceAccess(p,src,data(src,Source.class),actor,action);}
 }
 private void issueValid(HrmPayrollRequirementProjectDO p,Issue i){valid(i.getRequirementIds()!=null&&i.getSourceIds()!=null&&(!i.getRequirementIds().isEmpty()||!i.getSourceIds().isEmpty()||i.getRelationIds()!=null&&!i.getRelationIds().isEmpty()));valid(!Objects.equals(i.getHandlerId(),i.getReviewerId()));LocalDate.parse(i.getDueDate());for(Long id:i.getRequirementIds())requirement(p.getId(),id);for(Long id:i.getSourceIds())object(p.getId(),id,"SOURCE");issueAccess(p,i,i.getHandlerId(),"ISSUE_HANDLE");issueAccess(p,i,i.getReviewerId(),"ISSUE_REVIEW");}
 public Long createIssue(IssueSave c,Long actor){HrmPayrollRequirementProjectDO p=context(c,actor);valid(c.getId()==null);Issue i=c.getIssue();issueAccess(p,i,actor,"ISSUE_MANAGE");issueValid(p,i);i.setState("OPEN");i.setCycle(1);i.setWasOverdue(false);i.setClosedDate(null);i.setProcessing(null);i.setReview(null);HrmPayrollWorkbenchRecordDO o=insert(p.getId(),"ISSUE",i);Set<Long>a=issueImpact(p.getId(),i);event(p,o,"ISSUE_CREATE",actor,c.getReason(),a);todo(p,o,c.getReason(),a);return o.getId();}
 private Map<String,Object> issueBinding(HrmPayrollRequirementProjectDO p,Issue i){List<Object> refs=new ArrayList<>();for(Long id:issueImpact(p.getId(),i)){HrmPayrollRequirementDO r=requirement(p.getId(),id);refs.add(map("requirementId",id,"version",r.getVersion(),"semanticVersion",r.getSemanticVersion()));}for(Long id:i.getSourceIds()){HrmPayrollWorkbenchRecordDO o=object(p.getId(),id,"SOURCE");refs.add(map("sourceId",id,"version",o.getVersion()));}Set<Long> sourceIds=new HashSet<>(i.getSourceIds());for(HrmPayrollWorkbenchRecordDO o:relations(p.getId(),null,null,true)){Relation r=data(o,Relation.class);if(issueImpact(p.getId(),i).contains(r.getRequirementId())||sourceIds.contains(r.getSourceId()))refs.add(map("relationId",o.getId(),"version",o.getVersion()));}return map("projectId",p.getId(),"scopeVersion",p.getProjectScopeVersion(),"refs",refs,"cycle",i.getCycle());}
 public static Map<String,Object> dates(Issue i,Clock clock){LocalDate today=LocalDate.now(clock.withZone(SHANGHAI));boolean open=!"CLOSED".equals(i.getState());return map("unresolved",open,"overdue",open&&today.isAfter(LocalDate.parse(i.getDueDate())),"wasOverdue",Boolean.TRUE.equals(i.getWasOverdue())||open&&today.isAfter(LocalDate.parse(i.getDueDate())),"date",today.toString());}
 public void issueAction(IssueAction c,Long actor){
  HrmPayrollRequirementProjectDO p=context(c,actor);HrmPayrollWorkbenchRecordDO o=object(p.getId(),c.getId(),"ISSUE");Issue i=data(o,Issue.class);current(o.getVersion(),c.getVersion());String a=c.getAction();Set<Long> affected=issueImpact(p.getId(),i);
  String action=Arrays.asList("START","SUBMIT").contains(a)?"ISSUE_HANDLE":Arrays.asList("REJECT","CLOSE","RECLASSIFY").contains(a)?"ISSUE_REVIEW":"ISSUE_MANAGE";
  if("REOPEN".equals(a)&&Objects.equals(actor,i.getReviewerId()))action="ISSUE_REVIEW";
  issueAccess(p,i,actor,action);
  if("ISSUE_HANDLE".equals(action))denied(Objects.equals(actor,i.getHandlerId()));
  if("ISSUE_REVIEW".equals(action))denied(Objects.equals(actor,i.getReviewerId())&&!Objects.equals(actor,i.getHandlerId()));
  HrmPayrollReviewService.required("处理或复核依据",c.getEvidence());
  if(Boolean.TRUE.equals(dates(i,Clock.system(SHANGHAI)).get("overdue")))i.setWasOverdue(true);
  switch(a){
   case "START":valid("OPEN".equals(i.getState()));i.setState("PROCESSING");break;
   case "SUBMIT":valid("PROCESSING".equals(i.getState()));i.setProcessing(c.getEvidence());i.setState("REVIEW");break;
   case "REJECT":valid("REVIEW".equals(i.getState()));i.setReview(c.getEvidence());i.setState("PROCESSING");break;
   case "CLOSE":Map<String,Object> closeGovernance=governanceDecisions.forIssue(p.getId(),actor,p.getScopeJson(),affected);insert(p.getId(),"EVIDENCE",map("layer","ISSUE_CLOSE","objectId",o.getId(),"governance",closeGovernance,"reference",c.getEvidence(),"actorId",actor,"executedAt",HrmPayrollGovernanceStore.now()));valid("REVIEW".equals(i.getState()));current(digest(issueBinding(p,i)),c.getExpectedBinding());i.setReview(c.getEvidence());i.setState("CLOSED");i.setClosedDate(LocalDate.now(SHANGHAI).toString());break;
   case "REOPEN":valid("CLOSED".equals(i.getState()));HrmPayrollReviewService.required("新期限",c.getDueDate());try{LocalDate.parse(c.getDueDate());}catch(java.time.format.DateTimeParseException invalid){throw new cn.iocoder.yudao.framework.common.exception.ServiceException(PAYROLL_REVIEW_INCOMPLETE.getCode(),"请修正新期限：使用有效的YYYY-MM-DD日期");}i.setDueDate(c.getDueDate());i.setCycle(i.getCycle()+1);i.setState("OPEN");i.setWasOverdue(false);i.setClosedDate(null);i.setProcessing(null);i.setReview(null);break;
   case "RECLASSIFY":valid(!"CLOSED".equals(i.getState())&&c.getCategory()!=null&&c.getWholeProject()!=null&&c.getRequirementIds()!=null&&c.getSourceIds()!=null);i.setCategory(c.getCategory());i.setWholeProject(c.getWholeProject());i.setRequirementIds(c.getRequirementIds());i.setSourceIds(c.getSourceIds());issueAccess(p,i,actor,"ISSUE_REVIEW");issueValid(p,i);break;
   case "REASSIGN":valid(!"CLOSED".equals(i.getState())&&c.getHandlerId()!=null&&c.getReviewerId()!=null);i.setHandlerId(c.getHandlerId());i.setReviewerId(c.getReviewerId());issueValid(p,i);break;
   default:valid(false);
  }
  update(o,i);affected.addAll(issueImpact(p.getId(),i));event(p,o,"ISSUE_"+a,actor,c.getEvidence(),affected);todo(p,o,c.getReason(),affected);
 }
 private boolean dataBlocked(HrmPayrollRequirementProjectDO p,Long req,Long source){return all(p.getId(),"ISSUE").stream().anyMatch(o->{Issue i=data(o,Issue.class);return !"CLOSED".equals(i.getState())&&"DATA_READINESS".equals(i.getCategory())&&(Boolean.TRUE.equals(i.getWholeProject())||i.getSourceIds().contains(source)||issueImpact(p.getId(),i).contains(req));});}
 /** Real blocker data for S5; only project-wide readers receive complete project judgement. */
 public Map<String,Object> blockers(Long project,Long actor){return read(project,()->blockersRead(project,actor));}
    private Map<String,Object> blockersRead(Long project,Long actor){HrmPayrollRequirementProjectDO p=project(project,actor,false);denied(access.full(p,actor,"READ"));Set<Long> included=requirements.selectList(new LambdaQueryWrapperX<HrmPayrollRequirementDO>().eq(HrmPayrollRequirementDO::getProjectId,project)).stream().filter(r->"INCLUDED".equals(r.getScopeDecision())).map(HrmPayrollRequirementDO::getId).collect(Collectors.toSet());List<Long> blocks=new ArrayList<>();for(HrmPayrollWorkbenchRecordDO o:all(project,"ISSUE")){Issue i=data(o,Issue.class);if(!"CLOSED".equals(i.getState())&&"BASELINE_BLOCK".equals(i.getCategory())&&(Boolean.TRUE.equals(i.getWholeProject())||!Collections.disjoint(included,issueImpact(project,i))))blocks.add(o.getId());}return map("baselineIssueIds",blocks,"baselineBlocked",!blocks.isEmpty(),"includedRequirementIds",included,"formalReleaseGate","INDEPENDENT_PUBLISH_CHECK_REQUIRED");}
 private boolean visible(HrmPayrollRequirementProjectDO p,HrmPayrollWorkbenchRecordDO o,Long actor){try{switch(o.getKind()){
  case "SOURCE":sourceAccess(p,o,data(o,Source.class),actor,"READ");return true;
  case "RELATION":relationAccess(p,o,actor,"READ");return true;
  case "ISSUE":issueAccess(p,data(o,Issue.class),actor,"READ");return true;
  case "EVIDENCE":case "HISTORY":case "TODO":case "PROPOSAL":Map<String,Object> m=evidenceData(o);if("HISTORY".equals(o.getKind())&&"ISSUE".equals(m.get("kind"))){Issue old=JsonUtils.parseObject(JsonUtils.toJsonString(m.get("snapshot")),Issue.class);issueAccess(p,old,actor,"READ");}Long id=Long.valueOf(String.valueOf(m.get("objectId")));HrmPayrollWorkbenchRecordDO target=records.selectById(id);return target!=null&&Objects.equals(target.getProjectId(),p.getId())&&visible(p,target,actor);
  default:return false;
 }}catch(cn.iocoder.yudao.framework.common.exception.ServiceException denied){return false;}}
 private Object view(HrmPayrollRequirementProjectDO p,HrmPayrollWorkbenchRecordDO o,Long actor){
  Map<String,Object> values=evidenceData(o);Map<String,Object> result=map("id",o.getId(),"projectId",o.getProjectId(),"kind",o.getKind(),"version",o.getVersion(),"data",values);
  if("SOURCE".equals(o.getKind())){Source s=data(o,Source.class);result.put("bindings",map("SOURCE_SEMANTIC",digest(sourceBinding(p,o,s,"SOURCE_SEMANTIC")),"QS",digest(sourceBinding(p,o,s,"QS"))));result.put("sourceSemanticVerified",pass(latestEvidence(p.getId(),"SOURCE_SEMANTIC",o.getId(),digest(sourceBinding(p,o,s,"SOURCE_SEMANTIC")))));result.put("QSResult",pass(qs(p,o,s)));result.put("permittedActions",Arrays.asList("SOURCE_EDIT","SOURCE_VERIFY").stream().filter(a->sourceAllowed(p,o,s,actor,a)).collect(Collectors.toList()));}
  if("RELATION".equals(o.getKind())){Relation r=data(o,Relation.class);result.put("available",available(p,o));result.put("semanticVerified",Boolean.TRUE.equals(r.getActive())&&semanticReady(p,o,r));if(Boolean.TRUE.equals(r.getActive()))result.put("bindings",map("RELATION_SEMANTIC",digest(relationSemanticBinding(p,o,r)),"QC",digest(qcBinding(p,o,r))));}
  if("ISSUE".equals(o.getKind())){Issue i=data(o,Issue.class);result.put("binding",digest(issueBinding(p,i)));result.put("dates",dates(i,Clock.system(SHANGHAI)));result.put("affectedRequirementIds",issueImpact(p.getId(),i));}
  // Generic read access is not read access to authorization/decision evidence.
  values.remove("authority");if(values.remove("governance")!=null)values.put("governanceReferencesRestricted",true);if(values.containsKey("affectedRequirementIds")){List<?> ids=(List<?>)values.get("affectedRequirementIds");values.put("affectedRequirementIds",ids.stream().filter(id->access.row(p,requirement(p.getId(),Long.valueOf(String.valueOf(id))),actor,"READ")).collect(Collectors.toList()));}
  return result;
 }
 public List<Object> list(Long project,String kind,Long actor){return read(project,()->listRead(project,kind,actor));}
    private List<Object> listRead(Long project,String kind,Long actor){valid(KINDS.contains(kind));HrmPayrollRequirementProjectDO p=project(project,actor,false);return all(project,kind).stream().filter(o->visible(p,o,actor)).map(o->view(p,o,actor)).collect(Collectors.toList());}
 public Object detail(Long project,String kind,Long id,Long actor){return read(project,()->detailRead(project,kind,id,actor));}
    private Object detailRead(Long project,String kind,Long id,Long actor){HrmPayrollRequirementProjectDO p=project(project,actor,false);HrmPayrollWorkbenchRecordDO o=object(project,id,kind);denied(visible(p,o,actor));return view(p,o,actor);}
 private boolean exportable(HrmPayrollRequirementProjectDO p,HrmPayrollWorkbenchRecordDO o,Long actor){
  if(!visible(p,o,actor))return false;
  try{switch(o.getKind()){
   case "SOURCE":Source source=data(o,Source.class);return sourceAllowed(p,o,source,actor,"EXPORT");
   case "RELATION":Relation r=data(o,Relation.class);HrmPayrollWorkbenchRecordDO src=object(p.getId(),r.getSourceId(),"SOURCE");return access.row(p,requirement(p.getId(),r.getRequirementId()),actor,"EXPORT")&&sourceAllowed(p,src,data(src,Source.class),actor,"EXPORT");
   case "ISSUE":issueAccess(p,data(o,Issue.class),actor,"EXPORT");return true;
   default:Map<String,Object> m=evidenceData(o);HrmPayrollWorkbenchRecordDO target=records.selectById(Long.valueOf(String.valueOf(m.get("objectId"))));return target!=null&&exportable(p,target,actor);
  }}catch(cn.iocoder.yudao.framework.common.exception.ServiceException denied){return false;}
 }
 /** Current synchronous working list, explicitly not a frozen baseline or S5 six-table release. */
 public Map<String,Object> export(Long project,Long actor){return read(project,()->exportRead(project,actor));}
    private Map<String,Object> exportRead(Long project,Long actor){
  HrmPayrollRequirementProjectDO p=project(project,actor,true);platform(actor,"EXPORT");denied(access.project(p,actor,"EXPORT"));
  Map<String,Object> result=map("label",access.full(p,actor,"READ")&&access.full(p,actor,"EXPORT")?"当前来源关系工作清单（非发布基线）":"权限受限节选（非发布基线）","projectId",project,"projectScopeVersion",p.getProjectScopeVersion(),"exportedAt",LocalDateTime.now(SHANGHAI).toString());
  for(String kind:Arrays.asList("SOURCE","RELATION","ISSUE","EVIDENCE"))result.put(kind,all(project,kind).stream().filter(o->exportable(p,o,actor)).map(o->view(p,o,actor)).collect(Collectors.toList()));return result;
 }
 public List<Object> history(Long project,String kind,Long id,Long actor){return read(project,()->historyRead(project,kind,id,actor));}
    private List<Object> historyRead(Long project,String kind,Long id,Long actor){HrmPayrollRequirementProjectDO p=project(project,actor,false);HrmPayrollWorkbenchRecordDO o=object(project,id,kind);denied(visible(p,o,actor));return all(project,"HISTORY").stream().filter(h->String.valueOf(id).equals(String.valueOf(evidenceData(h).get("objectId")))&&visible(p,h,actor)).map(h->view(p,h,actor)).collect(Collectors.toList());}
}
