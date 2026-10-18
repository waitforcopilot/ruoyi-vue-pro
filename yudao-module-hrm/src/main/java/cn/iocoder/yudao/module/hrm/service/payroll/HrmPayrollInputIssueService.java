package cn.iocoder.yudao.module.hrm.service.payroll;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.salary.monthrecord.HrmSalaryMonthRecordDO;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.employee.info.HrmEmployeeMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.salary.monthrecord.HrmSalaryMonthRecordMapper;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.notify.NotifyMessageSendApi;
import cn.iocoder.yudao.module.system.api.notify.dto.NotifySendSingleToUserReqDTO;
import java.time.*;
import java.util.*;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
@Service public class HrmPayrollInputIssueService {
 @Resource private HrmPayrollInputIssueMapper cases;
 @Resource private HrmPayrollInputIssueEventMapper events;
 @Resource private HrmSalaryMonthRecordMapper batches;
 @Resource private HrmEmployeeMapper employees;
 @Resource private HrmPayrollScopeService scope;
 @Resource private HrmPayrollEntityService entities;
 @Resource private HrmPayrollReadinessService readiness;
 @Resource private HrmPayrollInputService inputService;
 @Resource private AdminUserApi users;
 @Resource private NotifyMessageSendApi notifications;
 @Resource private javax.validation.Validator validator;
 private LocalDateTime now(){return LocalDateTime.now(ZoneId.of("Asia/Shanghai"));}
 private void text(String value){if(value==null||value.trim().isEmpty()||value.length()>1000)throw exception(PAYROLL_INPUT_ISSUE_INVALID);}
 private boolean editable(HrmSalaryMonthRecordDO b){return b!=null&&"REGULAR".equals(b.getBatchKey())&&Arrays.asList(5,11,12,13,14).contains(b.getStatus());}
 private HrmSalaryMonthRecordDO batch(Long id,String action,Long actor,boolean edit) {
  HrmSalaryMonthRecordDO b=edit?batches.selectByIdForUpdate(id):batches.selectById(id);scope.check(b,action,actor);
  if(b.getEntityId()==null||edit&&!editable(b))throw exception(PAYROLL_INPUT_ISSUE_INVALID);return b;
 }
 private HrmPayrollInputIssueDO get(Long id){HrmPayrollInputIssueDO r=cases.selectById(id);if(r==null)throw exception(PAYROLL_INPUT_ISSUE_INVALID);return r;}
 private HrmEmployeeDO employee(Long id){HrmEmployeeDO e=employees.selectOneForUpdate(HrmEmployeeDO::getId,id);if(e==null)throw exception(PAYROLL_INPUT_ISSUE_INVALID);return e;}
 private boolean self(HrmPayrollInputIssueDO row,Long actor){HrmEmployeeDO e=employees.selectById(row.getEmployeeId());return actor!=null&&e!=null&&Objects.equals(e.getUserId(),actor);}
 private boolean ownerAccess(HrmPayrollInputIssueDO row,Long actor) {
  return self(row,actor)||Objects.equals(row.getOwnerId(),actor)&&entities.allowed(row.getEntityId(),"READ",actor)&&entities.allowed(row.getEntityId(),"MANAGE",actor);
 }
 private boolean portalAccess(HrmPayrollInputIssueDO row,Long actor){return ownerAccess(row,actor)||Objects.equals(row.getReviewerId(),actor)&&entities.allowed(row.getEntityId(),"READ",actor)&&entities.allowed(row.getEntityId(),"REVIEW",actor);}
 private void event(HrmPayrollInputIssueDO row,String action,String reason,Long actor){events.insert(new HrmPayrollInputIssueEventDO().setIssueId(row.getId()).setAction(action).setActorId(actor).setReason(reason).setSnapshot(JsonUtils.toJsonString(row)));}
 private void assignment(HrmPayrollInputIssueDO row,Long actor) {
  HrmEmployeeDO employee=employee(row.getEmployeeId());
  if(Objects.equals(row.getOwnerId(),row.getReviewerId())||Objects.equals(row.getReviewerId(),actor)||Objects.equals(row.getReviewerId(),row.getSubmittedBy())||Objects.equals(row.getReviewerId(),employee.getUserId()))throw exception(PAYROLL_SELF_APPROVAL);
  users.validateUserList(Arrays.asList(row.getOwnerId(),row.getReviewerId()));
  if(!Objects.equals(row.getOwnerId(),employee.getUserId())){entities.check(row.getEntityId(),"READ",row.getOwnerId());entities.check(row.getEntityId(),"MANAGE",row.getOwnerId());}
  entities.check(row.getEntityId(),"READ",row.getReviewerId());entities.check(row.getEntityId(),"REVIEW",row.getReviewerId());
 }
 @Transactional(rollbackFor=Exception.class,isolation=Isolation.READ_COMMITTED)
 public Long create(HrmPayrollInputIssueReqVO req,Long actor) {
  if(actor==null||!validator.validate(req).isEmpty()||!HrmPayrollReadinessService.KINDS.contains(req.getKind()))throw exception(PAYROLL_INPUT_ISSUE_INVALID);
  HrmSalaryMonthRecordDO b=batches.selectByIdForUpdate(req.getBatchId());if(b==null)throw exception(SALARY_MONTH_RECORD_NOT_EXISTS);scope.check(b,"MANAGE",actor);if(b.getEntityId()==null)throw exception(PAYROLL_INPUT_ISSUE_INVALID);
  HrmPayrollInputIssueDO replay=cases.selectOne(HrmPayrollInputIssueDO::getRequestKey,req.getRequestKey());String request=JsonUtils.toJsonString(req);
  if(replay!=null){if(!Objects.equals(replay.getSubmittedBy(),actor)||!Objects.equals(replay.getRequestSnapshot(),request))throw exception(PAYROLL_INPUT_ISSUE_INVALID);return replay.getId();}
  if(!editable(b)||!req.getDueTime().isAfter(now()))throw exception(PAYROLL_INPUT_ISSUE_INVALID);
  HrmPayrollInputIssueDO row=BeanUtils.toBean(req,HrmPayrollInputIssueDO.class).setEntityId(b.getEntityId()).setSubmittedBy(actor).setStatus("OPEN").setRequestSnapshot(request);assignment(row,actor);
  HrmPayrollReadinessService.Signal signal=readiness.inspect(b.getId(),req.getEmployeeId(),req.getKind(),actor);if(signal.isReady())throw exception(PAYROLL_INPUT_ISSUE_INVALID);
  row.setSourceSnapshot(JsonUtils.toJsonString(signal));
  if(cases.selectCount(new LambdaQueryWrapperX<HrmPayrollInputIssueDO>().eq(HrmPayrollInputIssueDO::getBatchId,b.getId()).eq(HrmPayrollInputIssueDO::getEmployeeId,row.getEmployeeId()).eq(HrmPayrollInputIssueDO::getKind,row.getKind()).in(HrmPayrollInputIssueDO::getStatus,"OPEN","SUBMITTED"))>0)throw exception(PAYROLL_INPUT_ISSUE_INVALID);
  try{cases.insert(row);}catch(org.springframework.dao.DuplicateKeyException e){throw exception(PAYROLL_INPUT_ISSUE_INVALID);}event(row,"CREATE",req.getReason(),actor);return row.getId();
 }
 @Transactional(rollbackFor=Exception.class,isolation=Isolation.READ_COMMITTED)
 public void submit(Long id,String evidence,Long actor) {
  text(evidence);HrmPayrollInputIssueDO candidate=get(id);
  // Assigned employees may supply their own proof without gaining subject-wide salary access.
  if(!ownerAccess(candidate,actor))throw exception(PAYROLL_INPUT_ISSUE_INVALID);
  HrmSalaryMonthRecordDO b=batches.selectByIdForUpdate(candidate.getBatchId());if(!editable(b))throw exception(PAYROLL_INPUT_ISSUE_INVALID);
  employee(candidate.getEmployeeId());HrmPayrollInputIssueDO row=cases.selectOneForUpdate(HrmPayrollInputIssueDO::getId,id);
  if(!ownerAccess(row,actor)||!"OPEN".equals(row.getStatus()))throw exception(PAYROLL_INPUT_ISSUE_INVALID);
  row.setReviewOpinion(null).setEvidence(evidence).setProofBy(actor).setStatus("SUBMITTED").setSubmittedSnapshot(JsonUtils.toJsonString(Arrays.asList(row.getSourceSnapshot(),actor,evidence)));
  // Null fields are ignored by updateById; explicitly clear the previous round's opinion.
  cases.update(row,new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<HrmPayrollInputIssueDO>()
    .eq(HrmPayrollInputIssueDO::getId,id).set(HrmPayrollInputIssueDO::getReviewOpinion,null));
  event(row,"SUBMIT",evidence,actor);
 }
 @Transactional(rollbackFor=Exception.class,isolation=Isolation.READ_COMMITTED)
 public void review(Long id,boolean approved,String reason,Long actor) {
  text(reason);HrmPayrollInputIssueDO candidate=get(id);batch(candidate.getBatchId(),"REVIEW",actor,true);scope.check(batches.selectById(candidate.getBatchId()),"READ",actor);
  HrmEmployeeDO employee=employee(candidate.getEmployeeId());HrmPayrollInputIssueDO row=cases.selectOneForUpdate(HrmPayrollInputIssueDO::getId,id);
  if(!"SUBMITTED".equals(row.getStatus())||!Objects.equals(row.getReviewerId(),actor))throw exception(PAYROLL_INPUT_ISSUE_INVALID);
  if(Objects.equals(actor,row.getOwnerId())||Objects.equals(actor,row.getSubmittedBy())||Objects.equals(actor,row.getProofBy())||Objects.equals(actor,employee.getUserId()))throw exception(PAYROLL_SELF_APPROVAL);
  if(approved){HrmPayrollReadinessService.Signal current=readiness.inspect(row.getBatchId(),row.getEmployeeId(),row.getKind(),actor);if(!current.isReady())throw exception(PAYROLL_INPUT_ISSUE_INVALID);row.setClosedSnapshot(JsonUtils.toJsonString(current)).setClosedTime(now());}
  row.setStatus(approved?"CLOSED":"OPEN").setReviewOpinion(reason);cases.updateById(row);event(row,approved?"CLOSE":"REJECT",reason,actor);
 }
 private void clearProof(HrmPayrollInputIssueDO row) {
  row.setEvidence(null).setProofBy(null).setSubmittedSnapshot(null).setClosedSnapshot(null).setClosedTime(null).setReviewOpinion(null);
  cases.update(null,new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<HrmPayrollInputIssueDO>().eq(HrmPayrollInputIssueDO::getId,row.getId()).set(HrmPayrollInputIssueDO::getEvidence,null).set(HrmPayrollInputIssueDO::getProofBy,null).set(HrmPayrollInputIssueDO::getSubmittedSnapshot,null).set(HrmPayrollInputIssueDO::getClosedSnapshot,null).set(HrmPayrollInputIssueDO::getClosedTime,null).set(HrmPayrollInputIssueDO::getReviewOpinion,null));
 }
 @Transactional(rollbackFor=Exception.class,isolation=Isolation.READ_COMMITTED)
 public void reassign(HrmPayrollInputIssueAssignReqVO req,Long actor) {
  if(!validator.validate(req).isEmpty()||!req.getDueTime().isAfter(now()))throw exception(PAYROLL_INPUT_ISSUE_INVALID);
  HrmPayrollInputIssueDO candidate=get(req.getId());batch(candidate.getBatchId(),"MANAGE",actor,true);HrmPayrollInputIssueDO row=cases.selectOneForUpdate(HrmPayrollInputIssueDO::getId,req.getId());
  if(!Arrays.asList("OPEN","SUBMITTED").contains(row.getStatus()))throw exception(PAYROLL_INPUT_ISSUE_INVALID);
  row.setOwnerId(req.getOwnerId()).setReviewerId(req.getReviewerId()).setDueTime(req.getDueTime());assignment(row,actor);clearProof(row);row.setStatus("OPEN");cases.updateById(row);event(row,"REASSIGN",req.getReason(),actor);
 }
 @Transactional(rollbackFor=Exception.class,isolation=Isolation.READ_COMMITTED)
 public void reopen(Long id,String reason,Long actor) {
  text(reason);HrmPayrollInputIssueDO candidate=get(id);batch(candidate.getBatchId(),"REVIEW",actor,true);HrmPayrollInputIssueDO row=cases.selectOneForUpdate(HrmPayrollInputIssueDO::getId,id);
  if(!"CLOSED".equals(row.getStatus())||!Objects.equals(actor,row.getReviewerId()))throw exception(PAYROLL_INPUT_ISSUE_INVALID);
  if(self(row,actor)||Objects.equals(actor,row.getOwnerId())||Objects.equals(actor,row.getSubmittedBy())||Objects.equals(actor,row.getProofBy()))throw exception(PAYROLL_SELF_APPROVAL);
  HrmPayrollReadinessService.Signal current=readiness.inspect(row.getBatchId(),row.getEmployeeId(),row.getKind(),actor);if(current.isReady())throw exception(PAYROLL_INPUT_ISSUE_INVALID);
  if(cases.selectCount(new LambdaQueryWrapperX<HrmPayrollInputIssueDO>().eq(HrmPayrollInputIssueDO::getBatchId,row.getBatchId()).eq(HrmPayrollInputIssueDO::getEmployeeId,row.getEmployeeId()).eq(HrmPayrollInputIssueDO::getKind,row.getKind()).in(HrmPayrollInputIssueDO::getStatus,"OPEN","SUBMITTED"))>0)throw exception(PAYROLL_INPUT_ISSUE_INVALID);
  clearProof(row);row.setStatus("OPEN").setSourceSnapshot(JsonUtils.toJsonString(current));cases.updateById(row);event(row,"REOPEN",reason,actor);
 }
 public void assertResolved(Long batchId) {
  if(cases.selectCount(new LambdaQueryWrapperX<HrmPayrollInputIssueDO>().eq(HrmPayrollInputIssueDO::getBatchId,batchId).in(HrmPayrollInputIssueDO::getStatus,"OPEN","SUBMITTED"))>0)throw exception(PAYROLL_INPUT_ISSUE_INVALID);
 }
 private void decorate(HrmPayrollInputIssueDO row,Long actor,boolean portal) {
  row.setOverdue(!"CLOSED".equals(row.getStatus())&&row.getDueTime().isBefore(now()));HrmSalaryMonthRecordDO b=batches.selectById(row.getBatchId());
  boolean edit=editable(b);
  row.setCanSubmit(edit&&"OPEN".equals(row.getStatus())&&ownerAccess(row,actor));
  row.setCanReview(edit&&Objects.equals(row.getReviewerId(),actor)&&!Objects.equals(row.getSubmittedBy(),actor)&&!Objects.equals(row.getProofBy(),actor)&&!Objects.equals(row.getOwnerId(),actor)&&!self(row,actor)&&entities.allowed(row.getEntityId(),"READ",actor)&&entities.allowed(row.getEntityId(),"REVIEW",actor));
  row.setCanReassign(edit&&!"CLOSED".equals(row.getStatus())&&entities.allowed(row.getEntityId(),"READ",actor)&&entities.allowed(row.getEntityId(),"MANAGE",actor));
  if(!edit){row.setSourceMode("FROZEN_HISTORY").setSourceReady("CLOSED".equals(row.getStatus()));return;}
  if(portal&&!entities.allowed(row.getEntityId(),"READ",actor)){row.setSourceMode("ASSIGNED_PROOF").setSourceReady(null);return;}
  row.setSourceMode("CURRENT");try{row.setSourceReady(readiness.inspect(row.getBatchId(),row.getEmployeeId(),row.getKind(),actor).isReady());}catch(cn.iocoder.yudao.framework.common.exception.ServiceException e){row.setSourceReady(false);}
 }
 @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
 public List<HrmPayrollInputIssueDO> list(Long batchId,Long actor) {batch(batchId,"READ",actor,false);List<HrmPayrollInputIssueDO> rows=cases.selectList(new LambdaQueryWrapperX<HrmPayrollInputIssueDO>().eq(HrmPayrollInputIssueDO::getBatchId,batchId).orderByDesc(HrmPayrollInputIssueDO::getId));rows.forEach(row->decorate(row,actor,false));return rows;}
 @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
 public List<HrmPayrollInputIssueDO> mine(Long actor) {
  if(actor==null)throw exception(PAYROLL_INPUT_ISSUE_INVALID);Set<Long> ids=new HashSet<>();employees.selectList(HrmEmployeeDO::getUserId,actor).forEach(e->ids.add(e.getId()));
  List<HrmPayrollInputIssueDO> out=cases.selectList(new LambdaQueryWrapperX<HrmPayrollInputIssueDO>().and(w->{w.eq(HrmPayrollInputIssueDO::getOwnerId,actor).or().eq(HrmPayrollInputIssueDO::getReviewerId,actor);if(!ids.isEmpty())w.or().in(HrmPayrollInputIssueDO::getEmployeeId,ids);}).orderByDesc(HrmPayrollInputIssueDO::getId));
  out.removeIf(row->!portalAccess(row,actor));out.forEach(row->decorate(row,actor,true));return out;
 }
 public List<HrmPayrollInputIssueEventDO> history(Long id,Long actor,boolean portal){HrmPayrollInputIssueDO row=get(id);if(portal){if(!portalAccess(row,actor))throw exception(PAYROLL_INPUT_ISSUE_INVALID);}else batch(row.getBatchId(),"READ",actor,false);return events.selectList(new LambdaQueryWrapperX<HrmPayrollInputIssueEventDO>().eq(HrmPayrollInputIssueEventDO::getIssueId,id).orderByAsc(HrmPayrollInputIssueEventDO::getId));}
 public Map<String,Object> selectors(Long entity,Long actor) {
  List<Object> entityOptions=new ArrayList<>();for(Long id:entities.accessible("READ",actor)){Map<String,Object> item=new LinkedHashMap<>();item.put("id",id);item.put("name",entities.name(id));entityOptions.add(item);}
  Map<String,Object> out=new LinkedHashMap<>();out.put("entities",entityOptions);if(entity==null)return out;entities.check(entity,"READ",actor);
  List<Object> batchOptions=new ArrayList<>();for(HrmSalaryMonthRecordDO b:batches.selectList(new LambdaQueryWrapperX<HrmSalaryMonthRecordDO>().eq(HrmSalaryMonthRecordDO::getEntityId,entity).eq(HrmSalaryMonthRecordDO::getBatchKey,"REGULAR").orderByDesc(HrmSalaryMonthRecordDO::getYear).orderByDesc(HrmSalaryMonthRecordDO::getMonth).orderByDesc(HrmSalaryMonthRecordDO::getId))) {
   Map<String,Object> item=new LinkedHashMap<>();item.put("id",b.getId());item.put("title",b.getTitle());item.put("status",b.getStatus());batchOptions.add(item);
  }
  out.put("batches",batchOptions);return out;
 }
 public List<Map<String,Object>> employees(Long batchId,Long actor) {
  HrmSalaryMonthRecordDO b=batch(batchId,"READ",actor,false);List<Map<String,Object>> out=new ArrayList<>();for(HrmEmployeeDO e:inputService.roster(b)){Map<String,Object> item=new LinkedHashMap<>();item.put("id",e.getId());item.put("name",e.getName());item.put("jobNumber",e.getJobNumber());out.add(item);}return out;
 }
 @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
 public List<HrmPayrollReadinessService.Signal> signals(Long batchId,Long employeeId,Long actor) {
  batch(batchId,"READ",actor,false);List<HrmPayrollReadinessService.Signal> out=new ArrayList<>();for(String kind:HrmPayrollReadinessService.KINDS)out.add(readiness.inspect(batchId,employeeId,kind,actor));return out;
 }
 public Map<String,Object> options(Long batchId,Long employeeId,Long actor) {
  HrmSalaryMonthRecordDO b=batch(batchId,"MANAGE",actor,false);if(!scope.payrollDates(b).containsKey(employeeId))throw exception(PAYROLL_INPUT_ISSUE_INVALID);HrmEmployeeDO employee=employees.selectById(employeeId);if(employee==null)throw exception(PAYROLL_INPUT_ISSUE_INVALID);
  List<Object> owners=new ArrayList<>(),reviewers=new ArrayList<>();
  for(cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO user:users.getUserListByStatus(0)) {
   Map<String,Object> item=new LinkedHashMap<>();item.put("id",user.getId());item.put("name",user.getNickname());boolean read=entities.allowed(b.getEntityId(),"READ",user.getId());
   if(Objects.equals(user.getId(),employee.getUserId())||read&&entities.allowed(b.getEntityId(),"MANAGE",user.getId()))owners.add(item);
   if(!Objects.equals(user.getId(),actor)&&!Objects.equals(user.getId(),employee.getUserId())&&read&&entities.allowed(b.getEntityId(),"REVIEW",user.getId()))reviewers.add(item);
  }
  Map<String,Object> out=new LinkedHashMap<>();out.put("owners",owners);out.put("reviewers",reviewers);out.put("kinds",HrmPayrollReadinessService.KINDS);return out;
 }
 public List<Long> overdueIds(){List<Long> ids=new ArrayList<>();cases.selectList(new LambdaQueryWrapperX<HrmPayrollInputIssueDO>().in(HrmPayrollInputIssueDO::getStatus,"OPEN","SUBMITTED").lt(HrmPayrollInputIssueDO::getDueTime,now())).forEach(row->ids.add(row.getId()));return ids;}
 @Transactional(rollbackFor=Exception.class,isolation=Isolation.READ_COMMITTED)
 public int remind(Long id) {
  HrmPayrollInputIssueDO candidate=cases.selectById(id);if(candidate==null)return 0;HrmSalaryMonthRecordDO b=batches.selectByIdForUpdate(candidate.getBatchId());HrmPayrollInputIssueDO row=cases.selectOneForUpdate(HrmPayrollInputIssueDO::getId,id);
  if(!editable(b)||!Arrays.asList("OPEN","SUBMITTED").contains(row.getStatus())||!row.getDueTime().isBefore(now()))return 0;
  Map<String,Object> state=row.getReminderState()==null?new LinkedHashMap<>():JsonUtils.parseObject(row.getReminderState(),Map.class);String today=now().toLocalDate().toString();List<String> errors=new ArrayList<>();int count=0;
  for(Long recipient:new TreeSet<>(Arrays.asList(row.getOwnerId(),row.getReviewerId()))) {
   if(Objects.equals(state.get(recipient.toString()),today))continue;
   try{users.validateUserList(Collections.singletonList(recipient));if(Objects.equals(recipient,row.getOwnerId())){if(!ownerAccess(row,recipient))throw exception(PAYROLL_ENTITY_INVALID);}else{entities.check(row.getEntityId(),"READ",recipient);entities.check(row.getEntityId(),"REVIEW",recipient);}
    Map<String,Object> params=new LinkedHashMap<>();params.put("caseId",row.getId());params.put("period",b.getYear()+"-"+String.format("%02d",b.getMonth()));params.put("kind",HrmPayrollReadinessService.label(row.getKind()));params.put("dueTime",row.getDueTime().toString());
    Long message=notifications.sendSingleMessageToAdmin(new NotifySendSingleToUserReqDTO().setUserId(recipient).setTemplateCode("hrm_payroll_input_issue_overdue").setTemplateParams(params));if(message==null)throw exception(PAYROLL_INPUT_ISSUE_INVALID);state.put(recipient.toString(),today);count++;
   }catch(RuntimeException failure){errors.add("用户"+recipient+"的授权或站内信配置需检查");}
  }
  row.setReminderState(JsonUtils.toJsonString(state)).setReminderError(String.join("；",errors));cases.updateById(row);if(count>0||!errors.isEmpty())event(row,"REMIND",count+"条站内提醒；"+row.getReminderError(),null);return count;
 }
}
