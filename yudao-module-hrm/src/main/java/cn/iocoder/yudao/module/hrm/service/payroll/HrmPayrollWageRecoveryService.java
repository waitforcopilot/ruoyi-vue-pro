package cn.iocoder.yudao.module.hrm.service.payroll;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.*;
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
import javax.validation.Validator;
import lombok.Data;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
/** Independently approved gross recovery. Tax offsets and bank payroll facts are separate evidence. */
@Service public class HrmPayrollWageRecoveryService {
 @Resource private HrmPayrollWageRecoveryMapper recoveries;
 @Resource private HrmPayrollWageRecoveryEventMapper events;
 @Resource private HrmPayrollRetroMapper retros;
 @Resource private HrmPayrollTaxCorrectionMapper corrections;
 @Resource private HrmPayrollTaxPolicyMapper policies;
 @Resource private HrmPayrollPaymentMapper payments;
 @Resource private HrmPayrollRunMapper runs;
 @Resource private HrmSalaryMonthRecordMapper batches;
 @Resource private HrmEmployeeMapper employees;
 @Resource private HrmPayrollRetroService retro;
 @Resource private HrmPayrollEntityService entities;
 @Resource private HrmPayrollScopeService scope;
 @Resource private HrmPayrollTaxProfileService profiles;
 @Resource private Validator validator;
 @Data public static class Allocation {
  private Long paymentId,correctionId,employeeId;
  private Integer cashYear;
  private BigDecimal originalIncome,correctedIncome,incomeReduction;
  private String sourceDigest;
 }
 private LocalDateTime now(){return LocalDateTime.now(ZoneId.of("Asia/Shanghai"));}
 private void text(String value){if(value==null||value.trim().isEmpty()||value.length()>1000)throw exception(PAYROLL_RECOVERY_INVALID);}
 private HrmPayrollWageRecoveryDO row(Long id){HrmPayrollWageRecoveryDO r=recoveries.selectById(id);if(r==null)throw exception(PAYROLL_RECOVERY_INVALID);return r;}
 private HrmSalaryMonthRecordDO original(Long paymentId,String action,Long actor,boolean lock){
  HrmPayrollPaymentDO p=payments.selectById(paymentId);if(p==null)throw exception(PAYROLL_RECOVERY_INVALID);HrmSalaryMonthRecordDO b=batches.selectById(p.getMonthRecordId());if(b==null)throw exception(PAYROLL_RECOVERY_INVALID);scope.check(b,"READ",actor);scope.check(b,action,actor);
  if(b.getEntityId()==null||!"REGULAR".equals(b.getBatchKey())||!Arrays.asList(15,16,17,10).contains(b.getStatus()))throw exception(PAYROLL_RECOVERY_INVALID);
  if(lock){entities.lockApproved(b.getEntityId());b=batches.selectByIdForUpdate(b.getId());}return b;
 }
 private void independent(HrmPayrollWageRecoveryDO row,Long actor,boolean cashier){HrmEmployeeDO e=employees.selectById(row.getEmployeeId());if(actor==null||e==null||Objects.equals(actor,e.getUserId())||Objects.equals(actor,row.getSubmittedBy())||cashier&&Objects.equals(actor,row.getReviewedBy()))throw exception(PAYROLL_SELF_APPROVAL);}
 private String digest(Object value){return cn.hutool.crypto.digest.DigestUtil.sha256Hex(JsonUtils.toJsonString(value));}
 private void event(HrmPayrollWageRecoveryDO r,String action,String reason,Long actor){events.insert(new HrmPayrollWageRecoveryEventDO().setRecoveryId(r.getId()).setActorId(actor).setAction(action).setReason(reason).setSnapshot(JsonUtils.toJsonString(r)));}
 private Allocation allocation(Long id,HrmSalaryMonthRecordDO original,HrmPayrollPaymentDO root,Set<Long> allowed){
  HrmPayrollTaxCorrectionDO c=corrections.selectById(id);if(c==null||!"APPROVED".equals(c.getStatus())||!allowed.contains(c.getPaymentId())||!Objects.equals(c.getEmployeeId(),root.getEmployeeId())||!Objects.equals(c.getEntityId(),original.getEntityId()))throw exception(PAYROLL_RECOVERY_INVALID);
  HrmPayrollPaymentDO p=payments.selectById(c.getPaymentId());HrmPayrollRunDO run=p==null?null:runs.selectById(p.getRunId());HrmSalaryMonthRecordDO owner=p==null?null:batches.selectById(p.getMonthRecordId());
  if(p==null||run==null||owner==null||!Objects.equals(owner.getEntityId(),original.getEntityId())||!"SUCCESS".equals(p.getStatus())||p.getValueDate()==null||!Objects.equals(c.getValueDate(),p.getValueDate())||!Objects.equals(c.getSourceDigest(),HrmPayrollTaxCorrectionService.digest(p,run)))throw exception(PAYROLL_RECOVERY_INVALID);
  HrmPayrollTaxProfileDO profile=profiles.requireApproved(c.getEntityId(),c.getEmployeeId(),p.getValueDate().getYear());HrmPayrollTaxPolicyDO policy=policies.selectById(c.getPolicyId());
  if(!Objects.equals(p.getEmployeeId(),c.getEmployeeId())||!Objects.equals(retro.tax(p).getProfile().getTaxpayerKey(),c.getTaxpayerKey())||!Objects.equals(profile.getVersion(),c.getProfileVersion())||!Objects.equals(profile.getId(),c.getProfileId())||!Objects.equals(profile.getTaxpayerKey(),c.getTaxpayerKey())||!Objects.equals(profile.getTaxRegistrationNumber(),c.getTaxRegistrationNumber())||policy==null||!Objects.equals(policy.getEntityId(),c.getEntityId())||!Objects.equals(policy.getVersion(),c.getPolicyVersion())||!Objects.equals(policy.getTaxRegistrationNumber(),c.getTaxRegistrationNumber())||!"APPROVED".equals(policy.getStatus())||!Objects.equals(policy.getYear(),p.getValueDate().getYear()))throw exception(PAYROLL_RECOVERY_INVALID);
  BigDecimal previous;
  if(c.getPriorCorrectionId()==null){HrmPayrollTaxCashLedgerVO.Row raw=new HrmPayrollTaxCashLedgerVO.Row();HrmPayrollTaxCashLedgerService.populate(raw,p,run,false);previous=raw.getIncome();}
  else {HrmPayrollTaxCorrectionDO prior=corrections.selectById(c.getPriorCorrectionId());if(prior==null||!Objects.equals(prior.getPaymentId(),p.getId())||prior.getVersion()>=c.getVersion())throw exception(PAYROLL_RECOVERY_INVALID);previous=prior.getIncome();}
  if(previous==null||c.getIncome()==null||previous.compareTo(c.getIncome())<=0)throw exception(PAYROLL_RECOVERY_INVALID);
  return new Allocation().setPaymentId(p.getId()).setCorrectionId(c.getId()).setEmployeeId(p.getEmployeeId()).setCashYear(p.getValueDate().getYear()).setOriginalIncome(previous).setCorrectedIncome(c.getIncome()).setIncomeReduction(previous.subtract(c.getIncome())).setSourceDigest(c.getSourceDigest());
 }
 private List<Allocation> allocations(HrmPayrollWageRecoveryReqVO req,HrmSalaryMonthRecordDO b,HrmPayrollPaymentDO p,HrmPayrollRetroDO prior,Long exclude){
  Set<Long> allowed=retro.relatedPayments(p,prior),seen=new HashSet<>();List<Allocation> out=new ArrayList<>();
  for(Long id:req.getTaxCorrectionIds()){if(id==null||!seen.add(id))throw exception(PAYROLL_RECOVERY_INVALID);out.add(allocation(id,b,p,allowed));}
  for(HrmPayrollWageRecoveryDO other:recoveries.selectList(new LambdaQueryWrapperX<HrmPayrollWageRecoveryDO>().eq(HrmPayrollWageRecoveryDO::getEntityId,b.getEntityId()).in(HrmPayrollWageRecoveryDO::getStatus,"PENDING","APPROVED","SETTLED"))){if(Objects.equals(other.getId(),exclude))continue;for(Allocation used:JsonUtils.parseArray(other.getAllocationSnapshot(),Allocation.class))if(seen.contains(used.getCorrectionId()))throw exception(PAYROLL_RECOVERY_INVALID);}
  Map<Long,BigDecimal> accounted=new HashMap<>();for(Allocation a:out)accounted.merge(a.getPaymentId(),a.getIncomeReduction(),BigDecimal::add);
  for(HrmPayrollWageRecoveryDO settled:recoveries.selectList(new LambdaQueryWrapperX<HrmPayrollWageRecoveryDO>().eq(HrmPayrollWageRecoveryDO::getEntityId,b.getEntityId()).eq(HrmPayrollWageRecoveryDO::getEmployeeId,p.getEmployeeId()).eq(HrmPayrollWageRecoveryDO::getStatus,"SETTLED")))for(Allocation a:JsonUtils.parseArray(settled.getAllocationSnapshot(),Allocation.class))accounted.merge(a.getPaymentId(),a.getIncomeReduction(),BigDecimal::add);
  for(Long paymentId:allowed){HrmPayrollTaxCorrectionDO active=corrections.selectOne(new LambdaQueryWrapperX<HrmPayrollTaxCorrectionDO>().eq(HrmPayrollTaxCorrectionDO::getPaymentId,paymentId).eq(HrmPayrollTaxCorrectionDO::getStatus,"APPROVED"));if(active!=null){HrmPayrollPaymentDO source=payments.selectById(paymentId);HrmPayrollTaxCashLedgerVO.Row raw=new HrmPayrollTaxCashLedgerVO.Row();HrmPayrollTaxCashLedgerService.populate(raw,source,runs.selectById(source.getRunId()),false);if(raw.getIncome().subtract(active.getIncome()).compareTo(accounted.getOrDefault(paymentId,BigDecimal.ZERO))!=0)throw exception(PAYROLL_RECOVERY_TAX_UNMATCHED);}}
  out.sort(Comparator.comparing(Allocation::getCorrectionId));return out;
 }
 private void fresh(HrmPayrollWageRecoveryDO recovery,HrmSalaryMonthRecordDO b,HrmPayrollPaymentDO p){
  HrmPayrollRetroDO entitlement=retros.selectById(recovery.getRetroId());if(entitlement==null)throw exception(PAYROLL_RECOVERY_INVALID);HrmPayrollWageRecoveryReqVO req=JsonUtils.parseObject(recovery.getRequestSnapshot(),HrmPayrollWageRecoveryReqVO.class);if(req==null||!validator.validate(req).isEmpty())throw exception(PAYROLL_RECOVERY_INVALID);HrmPayrollRetroDO prior=entitlement.getPriorRetroId()==null?null:retros.selectById(entitlement.getPriorRetroId());HrmPayrollRetroDO latest=retro.latest(p.getId());
  if(entitlement==null||!"EXTERNAL_RECOVERY".equals(entitlement.getSettlementMode())||!Objects.equals(entitlement.getStatus(),recovery.getStatus())||!Objects.equals(entitlement.getOriginalDigest(),retro.digest(p)))throw exception(PAYROLL_VERSION_CONFLICT);
  if("APPROVED".equals(entitlement.getStatus())){if(latest==null||!Objects.equals(latest.getId(),entitlement.getId()))throw exception(PAYROLL_VERSION_CONFLICT);}
  else if(!Objects.equals(entitlement.getPriorRetroId(),latest==null?null:latest.getId()))throw exception(PAYROLL_VERSION_CONFLICT);
  if(retro.baseline(p,prior).compareTo(entitlement.getOriginalIncome())!=0)throw exception(PAYROLL_VERSION_CONFLICT);
  List<Allocation> proof=allocations(req,b,p,prior,recovery.getId());BigDecimal total=proof.stream().map(Allocation::getIncomeReduction).reduce(BigDecimal.ZERO,BigDecimal::add);
  if(total.compareTo(recovery.getAmount())!=0||recovery.getAmount().compareTo(entitlement.getDelta().negate())!=0||!Objects.equals(digest(proof),recovery.getAllocationDigest()))throw exception(PAYROLL_RECOVERY_INVALID);
 }
 @Transactional(rollbackFor=Exception.class,isolation=Isolation.READ_COMMITTED)
 public Long create(HrmPayrollWageRecoveryReqVO req,Long actor){
  if(actor==null||!validator.validate(req).isEmpty())throw exception(PAYROLL_RECOVERY_INVALID);HrmSalaryMonthRecordDO b=original(req.getPaymentId(),"MANAGE",actor,true);HrmPayrollPaymentDO p=retro.paid(req.getPaymentId());
  HrmPayrollWageRecoveryDO replay=recoveries.selectOne(HrmPayrollWageRecoveryDO::getRequestKey,req.getRequestKey());String request=JsonUtils.toJsonString(req);
  if(replay!=null){if(!Objects.equals(replay.getSubmittedBy(),actor)||!Objects.equals(replay.getRequestSnapshot(),request))throw exception(PAYROLL_RECOVERY_INVALID);return replay.getId();}
  HrmPayrollRetroDO prior=retro.latest(p.getId());if(!Objects.equals(req.getPriorRetroId(),prior==null?null:prior.getId()))throw exception(PAYROLL_VERSION_CONFLICT);BigDecimal basis=retro.baseline(p,prior),amount=basis.subtract(req.getCorrectedIncome());if(amount.signum()<=0)throw exception(PAYROLL_RECOVERY_INVALID);
  List<Allocation> proof=allocations(req,b,p,prior,null);if(proof.stream().map(Allocation::getIncomeReduction).reduce(BigDecimal.ZERO,BigDecimal::add).compareTo(amount)!=0)throw exception(PAYROLL_RECOVERY_INVALID);
  HrmPayrollRetroDO entitlement=new HrmPayrollRetroDO().setSettlementMode("EXTERNAL_RECOVERY").setBatchId(b.getId()).setEntityId(b.getEntityId()).setEmployeeId(p.getEmployeeId()).setPaymentId(p.getId()).setPriorRetroId(req.getPriorRetroId()).setVersion(retros.selectList(HrmPayrollRetroDO::getPaymentId,p.getId()).stream().mapToInt(HrmPayrollRetroDO::getVersion).max().orElse(0)+1).setOriginalIncome(basis).setCorrectedIncome(req.getCorrectedIncome()).setDelta(amount.negate()).setOriginalDigest(retro.digest(p)).setEvidence(req.getEvidence()).setRequestKey("RECOVERY:"+digest(req.getRequestKey())).setRequestSnapshot(request).setStatus("PENDING").setSubmittedBy(actor);try{retros.insert(entitlement);}catch(org.springframework.dao.DuplicateKeyException conflict){throw exception(PAYROLL_RECOVERY_INVALID);}
  HrmPayrollWageRecoveryDO recovery=new HrmPayrollWageRecoveryDO().setRetroId(entitlement.getId()).setEntityId(b.getEntityId()).setEmployeeId(p.getEmployeeId()).setPaymentId(p.getId()).setAmount(amount).setEmployeeAcknowledgement(req.getEmployeeAcknowledgement()).setEvidence(req.getEvidence()).setAllocationSnapshot(JsonUtils.toJsonString(proof)).setAllocationDigest(digest(proof)).setStatus("PENDING").setSubmittedBy(actor).setRequestKey(req.getRequestKey()).setRequestSnapshot(request);try{recoveries.insert(recovery);}catch(org.springframework.dao.DuplicateKeyException conflict){throw exception(PAYROLL_RECOVERY_INVALID);}event(recovery,"SUBMIT",req.getEvidence(),actor);return recovery.getId();
 }
 @Transactional(rollbackFor=Exception.class,isolation=Isolation.READ_COMMITTED)
 public void review(Long id,boolean approve,String reason,Long actor){text(reason);HrmPayrollWageRecoveryDO candidate=row(id);HrmSalaryMonthRecordDO b=original(candidate.getPaymentId(),"REVIEW",actor,true);HrmPayrollPaymentDO p=retro.paid(candidate.getPaymentId());HrmPayrollWageRecoveryDO r=recoveries.selectOneForUpdate(HrmPayrollWageRecoveryDO::getId,id);independent(r,actor,false);if(!"PENDING".equals(r.getStatus()))throw exception(PAYROLL_RECOVERY_INVALID);if(approve)fresh(r,b,p);r.setStatus(approve?"APPROVED":"REJECTED").setReviewedBy(actor).setReviewReason(reason).setReviewedTime(now());recoveries.updateById(r);retros.updateById(new HrmPayrollRetroDO().setId(r.getRetroId()).setStatus(r.getStatus()).setReviewedBy(actor).setReviewReason(reason).setReviewedTime(r.getReviewedTime()));event(r,approve?"APPROVE":"REJECT",reason,actor);}
 @Transactional(rollbackFor=Exception.class,isolation=Isolation.READ_COMMITTED)
 public void confirm(HrmPayrollWageRecoveryActionReqVO req,Long actor){
  if(!validator.validate(req).isEmpty()||req.getAmount()==null||req.getValueDate()==null||req.getReference()==null||req.getReference().trim().isEmpty())throw exception(PAYROLL_RECOVERY_INVALID);text(req.getEvidence());HrmPayrollWageRecoveryDO candidate=row(req.getId());HrmSalaryMonthRecordDO b=original(candidate.getPaymentId(),"APPROVE",actor,true);HrmPayrollPaymentDO p=retro.paid(candidate.getPaymentId());HrmPayrollWageRecoveryDO r=recoveries.selectOneForUpdate(HrmPayrollWageRecoveryDO::getId,req.getId());independent(r,actor,true);
  if("SETTLED".equals(r.getStatus())){if(Objects.equals(r.getValueDate(),req.getValueDate())&&Objects.equals(r.getReceiptReference(),req.getReference())&&Objects.equals(r.getReceiptEvidence(),req.getEvidence())&&r.getAmount().compareTo(req.getAmount())==0)return;throw exception(PAYROLL_VERSION_CONFLICT);}
  if(!"APPROVED".equals(r.getStatus())||r.getAmount().compareTo(req.getAmount())!=0||req.getValueDate().isAfter(now().toLocalDate())||req.getValueDate().isBefore(r.getReviewedTime().toLocalDate())||req.getValueDate().isBefore(p.getValueDate()))throw exception(PAYROLL_RECOVERY_INVALID);fresh(r,b,p);
  if(recoveries.selectOne(HrmPayrollWageRecoveryDO::getReceiptReference,req.getReference())!=null)throw exception(PAYROLL_RECOVERY_INVALID);r.setStatus("SETTLED").setValueDate(req.getValueDate()).setReceiptReference(req.getReference()).setReceiptEvidence(req.getEvidence()).setConfirmedBy(actor).setConfirmedTime(now());try{recoveries.updateById(r);}catch(org.springframework.dao.DuplicateKeyException conflict){throw exception(PAYROLL_RECOVERY_INVALID);}event(r,"SETTLE",req.getEvidence(),actor);
 }
 @Transactional(rollbackFor=Exception.class,isolation=Isolation.READ_COMMITTED)
 public void cancel(Long id,String reason,String bankVoidEvidence,Long actor){text(reason);HrmPayrollWageRecoveryDO candidate=row(id);original(candidate.getPaymentId(),"APPROVE",actor,true);retro.paid(candidate.getPaymentId());HrmPayrollWageRecoveryDO r=recoveries.selectOneForUpdate(HrmPayrollWageRecoveryDO::getId,id);independent(r,actor,false);if(!Arrays.asList("PENDING","APPROVED").contains(r.getStatus()))throw exception(PAYROLL_RECOVERY_INVALID);if("APPROVED".equals(r.getStatus()))text(bankVoidEvidence);r.setStatus("CANCELLED").setReviewReason(reason);recoveries.updateById(r);retros.updateById(new HrmPayrollRetroDO().setId(r.getRetroId()).setStatus("CANCELLED"));event(r,"CANCEL",reason+(bankVoidEvidence==null?"":"；退款指令作废："+bankVoidEvidence),actor);}
 public List<Map<String,Object>> entities(Long actor){List<Map<String,Object>> out=new ArrayList<>();for(Long id:entities.accessible("READ",actor)){Map<String,Object> item=new LinkedHashMap<>();item.put("id",id);item.put("name",entities.name(id));out.add(item);}return out;}
 public List<HrmPayrollWageRecoveryDO> list(Long entity,Long actor){entities.check(entity,"READ",actor);List<HrmPayrollWageRecoveryDO> rows=recoveries.selectList(new LambdaQueryWrapperX<HrmPayrollWageRecoveryDO>().eq(HrmPayrollWageRecoveryDO::getEntityId,entity).orderByDesc(HrmPayrollWageRecoveryDO::getId));for(HrmPayrollWageRecoveryDO recovery:rows){HrmPayrollRetroDO entitlement=retros.selectById(recovery.getRetroId());if(entitlement==null)throw exception(PAYROLL_RECOVERY_INVALID);recovery.setOriginalIncome(entitlement.getOriginalIncome()).setCorrectedIncome(entitlement.getCorrectedIncome()).setPriorRetroId(entitlement.getPriorRetroId());}return rows;}
 public List<HrmPayrollWageRecoveryEventDO> history(Long id,Long actor){HrmPayrollWageRecoveryDO r=row(id);original(r.getPaymentId(),"READ",actor,false);return events.selectList(new LambdaQueryWrapperX<HrmPayrollWageRecoveryEventDO>().eq(HrmPayrollWageRecoveryEventDO::getRecoveryId,id).orderByAsc(HrmPayrollWageRecoveryEventDO::getId));}
 public List<Map<String,Object>> payments(Long entity,Long actor){entities.check(entity,"READ",actor);List<Map<String,Object>> out=new ArrayList<>();for(HrmSalaryMonthRecordDO b:batches.selectList(new LambdaQueryWrapperX<HrmSalaryMonthRecordDO>().eq(HrmSalaryMonthRecordDO::getEntityId,entity).eq(HrmSalaryMonthRecordDO::getBatchKey,"REGULAR").in(HrmSalaryMonthRecordDO::getStatus,15,16,17,10))){scope.check(b,"READ",actor);for(HrmPayrollPaymentDO p:payments.selectList(new LambdaQueryWrapperX<HrmPayrollPaymentDO>().eq(HrmPayrollPaymentDO::getMonthRecordId,b.getId()).eq(HrmPayrollPaymentDO::getStatus,"SUCCESS"))){Map<String,Object> item=new LinkedHashMap<>();item.put("id",p.getId());item.put("name",b.getTitle()+" · "+p.getEmployeeName()+" · "+p.getId());out.add(item);}}return out;}
 @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
 public Map<String,Object> baseline(Long paymentId,Long actor){HrmSalaryMonthRecordDO b=original(paymentId,"READ",actor,false);HrmPayrollPaymentDO p=payments.selectById(paymentId);if(!"SUCCESS".equals(p.getStatus())||p.getValueDate()==null)throw exception(PAYROLL_RECOVERY_INVALID);HrmPayrollRetroDO prior=retro.latest(paymentId);BigDecimal basis=retro.baseline(p,prior);Map<String,Object> out=new LinkedHashMap<>();out.put("income",basis);out.put("priorRetroId",prior==null?null:prior.getId());out.put("employeeName",p.getEmployeeName());out.put("originalCashDate",p.getValueDate());List<Allocation> options=new ArrayList<>();Set<Long> allowed=retro.relatedPayments(p,prior);
  Set<Long> used=new HashSet<>();for(HrmPayrollWageRecoveryDO recovery:recoveries.selectList(new LambdaQueryWrapperX<HrmPayrollWageRecoveryDO>().eq(HrmPayrollWageRecoveryDO::getEntityId,b.getEntityId()).in(HrmPayrollWageRecoveryDO::getStatus,"PENDING","APPROVED","SETTLED")))for(Allocation allocation:JsonUtils.parseArray(recovery.getAllocationSnapshot(),Allocation.class))used.add(allocation.getCorrectionId());
  for(HrmPayrollTaxCorrectionDO c:corrections.selectList(new LambdaQueryWrapperX<HrmPayrollTaxCorrectionDO>().eq(HrmPayrollTaxCorrectionDO::getEntityId,b.getEntityId()).eq(HrmPayrollTaxCorrectionDO::getEmployeeId,p.getEmployeeId()).eq(HrmPayrollTaxCorrectionDO::getStatus,"APPROVED")))if(allowed.contains(c.getPaymentId())&&!used.contains(c.getId())){try{options.add(allocation(c.getId(),b,p,allowed));}catch(cn.iocoder.yudao.framework.common.exception.ServiceException unavailable){/* Only ready formal income reductions can fund a recovery. */}}out.put("taxAllocations",options);return out;}
}
