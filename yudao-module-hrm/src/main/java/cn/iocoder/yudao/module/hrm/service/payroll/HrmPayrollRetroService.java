package cn.iocoder.yudao.module.hrm.service.payroll;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollRetroReqVO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.salary.monthrecord.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.employee.info.HrmEmployeeMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.salary.monthrecord.HrmSalaryMonthRecordMapper;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** Approved wage differences enter a later payroll; original wage and cash facts are immutable. */
@Service public class HrmPayrollRetroService {
 @Resource private HrmPayrollRetroMapper retros;
 @Resource private HrmPayrollRetroEventMapper events;
 @Resource private HrmPayrollPaymentMapper payments;
 @Resource private HrmPayrollRunMapper runs;
 @Resource private HrmSalaryMonthRecordMapper batches;
 @Resource private HrmEmployeeMapper employees;
 @Resource private HrmPayrollScopeService scope;
 @Resource private HrmPayrollTaxProfileService profiles;
 @Resource private javax.validation.Validator validator;

 private HrmSalaryMonthRecordDO batch(Long id,String action,Long actor,boolean edit) {
  HrmSalaryMonthRecordDO b=edit?batches.selectByIdForUpdate(id):batches.selectById(id);
  scope.check(b,action,actor);
  if(b.getEntityId()==null || !("REGULAR".equals(b.getBatchKey())||HrmPayrollSupplementService.isSupplement(b))) throw exception(PAYROLL_ENTITY_INVALID);
  if(edit && !(Objects.equals(b.getStatus(),5)||Objects.equals(b.getStatus(),11))) throw exception(SALARY_MONTH_RECORD_STATUS_INVALID);
  if(edit && b.getExpectedPaymentDate()==null) throw exception(PAYROLL_TAX_PAYMENT_DATE_MISSING);
  return b;
 }
 private HrmPayrollRetroDO latest(Long payment) {
  return retros.selectOne(new LambdaQueryWrapperX<HrmPayrollRetroDO>().eq(HrmPayrollRetroDO::getPaymentId,payment)
    .eq(HrmPayrollRetroDO::getStatus,"APPROVED").orderByDesc(HrmPayrollRetroDO::getVersion).last("LIMIT 1"));
 }
 private HrmPayrollPaymentDO paid(Long id) {
  HrmPayrollPaymentDO p=payments.selectOneForUpdate(HrmPayrollPaymentDO::getId,id);
  if(p==null || !"SUCCESS".equals(p.getStatus()) || p.getValueDate()==null) throw exception(PAYROLL_RETRO_INVALID);
  if(p.getAmount()==null || (p.getAmount().signum()==0 && (p.getZeroSettlementReference()==null || p.getZeroSettlementEvidence()==null))) throw exception(PAYROLL_RETRO_INVALID);
  return p;
 }
 private HrmSalaryMonthEmployeeRecordDO result(HrmPayrollPaymentDO p) {
  HrmPayrollRunDO run=runs.selectById(p.getRunId());
  if(run==null || !Objects.equals(run.getMonthRecordId(),p.getMonthRecordId())) throw exception(PAYROLL_RETRO_INVALID);
  List<HrmSalaryMonthEmployeeRecordDO> matched=new ArrayList<>();
  for(HrmSalaryMonthEmployeeRecordDO row:JsonUtils.parseArray(run.getResultSnapshot(),HrmSalaryMonthEmployeeRecordDO.class))
   if(Objects.equals(row.getEmployeeId(),p.getEmployeeId())) matched.add(row);
  if(matched.size()!=1 || matched.get(0).getRealPaySalary()==null || matched.get(0).getRealPaySalary().compareTo(p.getAmount())!=0) throw exception(PAYROLL_RETRO_INVALID);
  return matched.get(0);
 }
 private HrmPayrollTaxWithholdingService.Source tax(HrmPayrollPaymentDO p) {
  HrmPayrollTaxWithholdingService.Source t=JsonUtils.parseObject(result(p).getTaxSourceSnapshot(),HrmPayrollTaxWithholdingService.Source.class);
  if(t==null || t.getCurrentIncome()==null || t.getProfile()==null || !Objects.equals(t.getEmployeeId(),p.getEmployeeId())) throw exception(PAYROLL_RETRO_INVALID);
  return t;
 }
 private String digest(HrmPayrollPaymentDO p) {
  // Account details are deliberately excluded from audit materials.
  return cn.hutool.crypto.digest.DigestUtil.sha256Hex(JsonUtils.toJsonString(Arrays.asList(p.getId(),p.getRunId(),p.getMonthRecordId(),p.getEmployeeId(),p.getAmount(),p.getValueDate(),p.getZeroSettlementReference(),result(p))));
 }
 private void eligible(HrmSalaryMonthRecordDO b,HrmPayrollPaymentDO p) {
  HrmSalaryMonthRecordDO original=batches.selectById(p.getMonthRecordId());
  if(original==null || !"REGULAR".equals(original.getBatchKey()) || !Objects.equals(original.getEntityId(),b.getEntityId()) || Objects.equals(b.getId(),original.getId())
    || b.getExpectedPaymentDate()==null || b.getExpectedPaymentDate().isBefore(p.getValueDate())
    || !scope.payrollDates(b).containsKey(p.getEmployeeId())) throw exception(PAYROLL_RETRO_INVALID);
  if(HrmPayrollSupplementService.isSupplement(b) && !Objects.equals(b.getParentBatchId(),original.getId())) throw exception(PAYROLL_RETRO_INVALID);
  HrmPayrollTaxWithholdingService.Source t=tax(p);
  HrmPayrollTaxProfileDO current=profiles.requireApproved(b.getEntityId(),p.getEmployeeId(),b.getExpectedPaymentDate().getYear());
  if(!Objects.equals(t.getEntityId(),b.getEntityId()) || t.getProfile().getTaxpayerKey()==null
    || !Objects.equals(t.getProfile().getTaxpayerKey(),current.getTaxpayerKey())) throw exception(PAYROLL_RETRO_INVALID);
 }
 private BigDecimal originalIncome(HrmPayrollPaymentDO payment) {
  HrmSalaryMonthEmployeeRecordDO salary=result(payment);
  if(salary.getOptionValues()==null)throw exception(PAYROLL_RETRO_INVALID);
  BigDecimal historical=BigDecimal.ZERO;boolean seen=false;
  for(HrmSalaryMonthEmployeeRecordDO.OptionValue value:salary.getOptionValues())if(Objects.equals(value.getCode(),10901)) {
   if(seen||value.getValue()==null)throw exception(PAYROLL_RETRO_INVALID);seen=true;historical=value.getValue();
  }
  BigDecimal basis=tax(payment).getCurrentIncome().subtract(historical);
  if(basis.signum()<0)throw exception(PAYROLL_RETRO_INVALID);return basis;
 }
 private BigDecimal baseline(HrmPayrollPaymentDO p,HrmPayrollRetroDO prior) {
  if(prior==null) return originalIncome(p);
  // A successor may only follow actual settlement of its predecessor.
  List<HrmPayrollPaymentDO> settled=payments.selectList(new LambdaQueryWrapperX<HrmPayrollPaymentDO>()
    .eq(HrmPayrollPaymentDO::getMonthRecordId,prior.getBatchId()).eq(HrmPayrollPaymentDO::getEmployeeId,prior.getEmployeeId()).eq(HrmPayrollPaymentDO::getStatus,"SUCCESS"));
  if(settled.size()!=1 || settled.get(0).getValueDate()==null) throw exception(PAYROLL_RETRO_INVALID);
  HrmPayrollRunDO run=runs.selectById(settled.get(0).getRunId());
  if(run==null || !snapshotIds(run).contains(prior.getId())) throw exception(PAYROLL_RETRO_INVALID);
  return prior.getCorrectedIncome();
 }
 private Set<Long> snapshotIds(HrmPayrollRunDO latest) {
  Set<Long> ids=new HashSet<>(),visited=new HashSet<>();
  HrmPayrollRunDO row=latest;
  while(row!=null) {
   if(!visited.add(row.getId())) throw exception(PAYROLL_RETRO_INVALID);
   Map<String,Object> map=JsonUtils.parseObject(row.getInputSnapshot(),Map.class);
   Object values=map.get("retroSources");
   if(values!=null) for(HrmPayrollRetroDO r:JsonUtils.parseArray(JsonUtils.toJsonString(values),HrmPayrollRetroDO.class)) ids.add(r.getId());
   Object parent=map.get("sourceRunId");if(parent==null) break;
   if(!(parent instanceof Number)) throw exception(PAYROLL_RETRO_INVALID);
   HrmPayrollRunDO previous=runs.selectById(((Number)parent).longValue());
   if(previous==null || !Objects.equals(previous.getMonthRecordId(),latest.getMonthRecordId()) || previous.getVersion()>=row.getVersion()) throw exception(PAYROLL_RETRO_INVALID);
   row=previous;
  }
  return ids;
 }
 private void event(HrmPayrollRetroDO r,String action,String reason,Long actor) {
  events.insert(new HrmPayrollRetroEventDO().setRetroId(r.getId()).setActorId(actor).setAction(action).setReason(reason).setSnapshot(JsonUtils.toJsonString(r)));
 }
 @Transactional(rollbackFor=Exception.class,isolation=Isolation.READ_COMMITTED)
 public Long create(HrmPayrollRetroReqVO req,Long actor) {
  if(!validator.validate(req).isEmpty()) throw exception(PAYROLL_RETRO_INVALID);
  HrmSalaryMonthRecordDO b=batch(req.getBatchId(),"MANAGE",actor,true);
  HrmPayrollRetroDO replay=retros.selectOne(HrmPayrollRetroDO::getRequestKey,req.getRequestKey());
  String snapshot=JsonUtils.toJsonString(req);
  if(replay!=null) {
   if(!Objects.equals(replay.getSubmittedBy(),actor)||!Objects.equals(replay.getRequestSnapshot(),snapshot)) throw exception(PAYROLL_RETRO_INVALID);
   return replay.getId();
  }
  HrmPayrollPaymentDO p=paid(req.getPaymentId()); eligible(b,p);
  // Also require access to the original batch; current-period access alone is insufficient.
  scope.check(batches.selectById(p.getMonthRecordId()),"READ",actor);
  HrmPayrollRetroDO prior=latest(p.getId());
  if(!Objects.equals(req.getPriorRetroId(),prior==null?null:prior.getId())) throw exception(PAYROLL_VERSION_CONFLICT);
  BigDecimal before=baseline(p,prior), delta=req.getCorrectedIncome().subtract(before);
  if((HrmPayrollSupplementService.isSupplement(b)&&delta.signum()<=0) || delta.signum()==0 || (delta.signum()<0 && p.getValueDate().getYear()!=b.getExpectedPaymentDate().getYear())) throw exception(PAYROLL_RETRO_INVALID);
  HrmPayrollRetroDO row=new HrmPayrollRetroDO().setBatchId(b.getId()).setEntityId(b.getEntityId()).setEmployeeId(p.getEmployeeId())
    .setPaymentId(p.getId()).setPriorRetroId(req.getPriorRetroId()).setVersion(retros.selectList(new LambdaQueryWrapperX<HrmPayrollRetroDO>().eq(HrmPayrollRetroDO::getPaymentId,p.getId())).stream().mapToInt(HrmPayrollRetroDO::getVersion).max().orElse(0)+1)
    .setOriginalIncome(before).setCorrectedIncome(req.getCorrectedIncome()).setDelta(delta).setOriginalDigest(digest(p))
    .setEvidence(req.getEvidence()).setRequestKey(req.getRequestKey()).setRequestSnapshot(snapshot).setStatus("PENDING").setSubmittedBy(actor);
  retros.insert(row);event(row,"SUBMIT",req.getEvidence(),actor);return row.getId();
 }
 @Transactional(rollbackFor=Exception.class,isolation=Isolation.READ_COMMITTED)
 public void review(Long id,boolean approve,String reason,Long actor) {
  if(reason==null||reason.trim().isEmpty()||reason.length()>1000) throw exception(PAYROLL_RETRO_INVALID);
  HrmPayrollRetroDO candidate=retros.selectById(id);
  if(candidate==null) throw exception(PAYROLL_RETRO_INVALID);
  HrmSalaryMonthRecordDO b=batch(candidate.getBatchId(),"REVIEW",actor,true);
  HrmPayrollPaymentDO p=paid(candidate.getPaymentId());
  HrmPayrollRetroDO row=retros.selectOneForUpdate(HrmPayrollRetroDO::getId,id);
  if(!"PENDING".equals(row.getStatus())) throw exception(PAYROLL_RETRO_INVALID);
  HrmEmployeeDO employee=employees.selectById(row.getEmployeeId());
  if(employee==null || Objects.equals(actor,employee.getUserId()) || Objects.equals(actor,row.getSubmittedBy())) throw exception(PAYROLL_SELF_APPROVAL);
  scope.check(batches.selectById(p.getMonthRecordId()),"READ",actor);
  if(approve) {
   eligible(b,p);
   HrmPayrollRetroDO prior=latest(p.getId());
   if(!Objects.equals(row.getPriorRetroId(),prior==null?null:prior.getId()) || !Objects.equals(row.getOriginalDigest(),digest(p))
     || baseline(p,prior).compareTo(row.getOriginalIncome())!=0
     || (row.getDelta().signum()<0 && p.getValueDate().getYear()!=b.getExpectedPaymentDate().getYear())) throw exception(PAYROLL_VERSION_CONFLICT);
  }
  row.setStatus(approve?"APPROVED":"REJECTED").setReviewedBy(actor).setReviewReason(reason).setReviewedTime(LocalDateTime.now(ZoneId.of("Asia/Shanghai")));
  retros.updateById(row);event(row,approve?"APPROVE":"REJECT",reason,actor);
 }
 @Transactional(rollbackFor=Exception.class,isolation=Isolation.READ_COMMITTED)
 public void cancel(Long id,String reason,Long actor) {
  if(reason==null||reason.trim().isEmpty()||reason.length()>1000) throw exception(PAYROLL_RETRO_INVALID);
  HrmPayrollRetroDO candidate=retros.selectById(id);
  if(candidate==null) throw exception(PAYROLL_RETRO_INVALID);
  batch(candidate.getBatchId(),"REVIEW",actor,true);
  HrmPayrollPaymentDO p=paid(candidate.getPaymentId());
  scope.check(batches.selectById(p.getMonthRecordId()),"READ",actor);
  HrmPayrollRetroDO row=retros.selectOneForUpdate(HrmPayrollRetroDO::getId,id);
  HrmEmployeeDO e=employees.selectById(row.getEmployeeId());
  if(!"APPROVED".equals(row.getStatus())) throw exception(PAYROLL_RETRO_INVALID);
  if(e==null||Objects.equals(actor,e.getUserId())||Objects.equals(actor,row.getSubmittedBy())) throw exception(PAYROLL_SELF_APPROVAL);
  row.setStatus("CANCELLED");retros.updateById(row);event(row,"CANCEL",reason,actor);
 }
 /** Internal source. Authorization is enforced by the enclosing payroll operation. */
 public List<HrmPayrollRetroDO> sources(Long batchId) {
  List<HrmPayrollRetroDO> list=retros.selectList(new LambdaQueryWrapperX<HrmPayrollRetroDO>().eq(HrmPayrollRetroDO::getBatchId,batchId).eq(HrmPayrollRetroDO::getStatus,"APPROVED").orderByAsc(HrmPayrollRetroDO::getId));
  HrmSalaryMonthRecordDO b=batches.selectById(batchId);
  for(HrmPayrollRetroDO row:list) {
   HrmPayrollPaymentDO p=payments.selectById(row.getPaymentId());
   if(p==null || !"SUCCESS".equals(p.getStatus()) || !Objects.equals(row.getOriginalDigest(),digest(p)) || !Objects.equals(row.getEntityId(),b.getEntityId())) throw exception(PAYROLL_VERSION_CONFLICT);
   eligible(b,p);
   if(row.getDelta().signum()<0 && p.getValueDate().getYear()!=b.getExpectedPaymentDate().getYear()) throw exception(PAYROLL_RETRO_INVALID);
  }
  return list;
 }
 public BigDecimal amount(Long batch,Long employee) {
  return sources(batch).stream().filter(r->Objects.equals(r.getEmployeeId(),employee)).map(HrmPayrollRetroDO::getDelta).reduce(BigDecimal.ZERO,BigDecimal::add);
 }
 private Object proof(List<HrmPayrollRetroDO> rows) {
  List<Object> proof=new ArrayList<>();
  for(HrmPayrollRetroDO r:rows) proof.add(Arrays.asList(r.getId(),r.getEntityId(),r.getBatchId(),r.getEmployeeId(),r.getPaymentId(),
    r.getPriorRetroId(),r.getVersion(),r.getOriginalIncome(),r.getCorrectedIncome(),r.getDelta(),r.getOriginalDigest(),r.getEvidence(),
    r.getStatus(),r.getSubmittedBy(),r.getReviewedBy(),r.getReviewReason()));
  return JsonUtils.parseTree(JsonUtils.toJsonString(proof));
 }
 public void validateInputSnapshot(String input,Long batch) {
  Map<String,Object> map=JsonUtils.parseObject(input,Map.class);
  // Adjustment runs inherit and validate the original source run separately.
  if(map.get("sourceRunId")!=null) return;
  List<HrmPayrollRetroDO> current=sources(batch);
  Object saved=map.get("retroSources");
  if(saved==null && current.isEmpty()) return;
  if(!(saved instanceof List) || !Objects.equals(proof(JsonUtils.parseArray(JsonUtils.toJsonString(saved),HrmPayrollRetroDO.class)),proof(current))) throw exception(PAYROLL_VERSION_CONFLICT);
 }
 public List<HrmPayrollRetroDO> list(Long batchId,Long actor) {
  batch(batchId,"READ",actor,false);
  List<HrmPayrollRetroDO> rows=retros.selectList(new LambdaQueryWrapperX<HrmPayrollRetroDO>().eq(HrmPayrollRetroDO::getBatchId,batchId).orderByDesc(HrmPayrollRetroDO::getId));
  for(HrmPayrollRetroDO row:rows) {
   HrmPayrollPaymentDO p=payments.selectById(row.getPaymentId());
   if(p==null) throw exception(PAYROLL_RETRO_INVALID);
   scope.check(batches.selectById(p.getMonthRecordId()),"READ",actor);
  }
  return rows;
 }
 public List<HrmPayrollRetroEventDO> history(Long id,Long actor) {
  HrmPayrollRetroDO r=retros.selectById(id);if(r==null) throw exception(PAYROLL_RETRO_INVALID);
  batch(r.getBatchId(),"READ",actor,false);
  HrmPayrollPaymentDO p=payments.selectById(r.getPaymentId());
  if(p==null) throw exception(PAYROLL_RETRO_INVALID);
  scope.check(batches.selectById(p.getMonthRecordId()),"READ",actor);
  return events.selectList(new LambdaQueryWrapperX<HrmPayrollRetroEventDO>().eq(HrmPayrollRetroEventDO::getRetroId,id).orderByAsc(HrmPayrollRetroEventDO::getId));
 }
 public Map<String,Object> supplementBaseline(Long paymentId,Long actor) {
  HrmPayrollPaymentDO p=payments.selectById(paymentId);
  if(p==null||!"SUCCESS".equals(p.getStatus())||p.getValueDate()==null) throw exception(PAYROLL_RETRO_INVALID);
  HrmSalaryMonthRecordDO original=batches.selectById(p.getMonthRecordId());scope.check(original,"READ",actor);
  if(original==null||!"REGULAR".equals(original.getBatchKey()))throw exception(PAYROLL_RETRO_INVALID);
  HrmPayrollRetroDO prior=latest(paymentId);
  Map<String,Object> out=new LinkedHashMap<>();out.put("income",baseline(p,prior));out.put("priorRetroId",prior==null?null:prior.getId());out.put("originalBatch",original.getTitle());return out;
 }
 public List<Map<String,Object>> candidates(Long batchId,Long actor) {
  HrmSalaryMonthRecordDO b=batch(batchId,"READ",actor,false);
  Set<Long> roster=scope.payrollDates(b).keySet();List<Map<String,Object>> out=new ArrayList<>();
  for(HrmPayrollPaymentDO p:payments.selectList(new LambdaQueryWrapperX<HrmPayrollPaymentDO>().eq(HrmPayrollPaymentDO::getStatus,"SUCCESS").orderByDesc(HrmPayrollPaymentDO::getId))) {
   HrmSalaryMonthRecordDO original=batches.selectById(p.getMonthRecordId());
   if(original==null||!"REGULAR".equals(original.getBatchKey())||!Objects.equals(original.getEntityId(),b.getEntityId())||Objects.equals(original.getId(),batchId)||!roster.contains(p.getEmployeeId())) continue;
   scope.check(original,"READ",actor);
   HrmPayrollRetroDO prior=latest(p.getId());
   try {
    BigDecimal before=baseline(p,prior); HrmEmployeeDO e=employees.selectById(p.getEmployeeId());
    Map<String,Object> item=new LinkedHashMap<>();item.put("paymentId",p.getId());item.put("employeeName",e==null?null:e.getName());
    item.put("originalBatch",original.getTitle());item.put("valueDate",p.getValueDate());item.put("income",before);item.put("priorRetroId",prior==null?null:prior.getId());out.add(item);
   } catch(cn.iocoder.yudao.framework.common.exception.ServiceException ignored) { /* Unsettled predecessor is not selectable. */ }
  }
  return out;
 }
}
