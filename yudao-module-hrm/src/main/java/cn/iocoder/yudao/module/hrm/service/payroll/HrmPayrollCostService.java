package cn.iocoder.yudao.module.hrm.service.payroll;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.salary.monthrecord.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.employee.info.HrmEmployeeMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.salary.monthrecord.HrmSalaryMonthRecordMapper;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import java.math.*;
import java.time.*;
import java.util.*;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** Versioned allocation rules and immutable, independently approved reports from frozen payroll. */
@Service public class HrmPayrollCostService {
 @Resource private HrmPayrollCostRuleMapper rules;
 @Resource private HrmPayrollCostReportMapper reports;
 @Resource private HrmPayrollCostEventMapper events;
 @Resource private HrmPayrollRunMapper runs;
 @Resource private HrmPayrollPaymentMapper payments;
 @Resource private HrmPayrollEntityMembershipMapper membershipMapper;
 @Resource private HrmSalaryMonthRecordMapper batches;
 @Resource private HrmEmployeeMapper employees;
 @Resource private HrmPayrollScopeService scope;
 @Resource private HrmPayrollEntityService entities;
 @Resource private DeptApi departments;
 @Resource private javax.validation.Validator validator;
 private void event(String kind,Long id,String action,String reason,Object value,Long actor) {
  events.insert(new HrmPayrollCostEventDO().setKind(kind).setTargetId(id).setAction(action).setReason(reason).setSnapshot(JsonUtils.toJsonString(value)).setActorId(actor));
 }
 private void reason(String reason){if(reason==null||reason.trim().isEmpty()||reason.length()>1000)throw exception(PAYROLL_COST_INVALID);}
 private void validateParts(List<HrmPayrollCostRuleReqVO.Part> parts) {validateDistribution(parts);departments.validateDeptList(parts.stream().map(HrmPayrollCostRuleReqVO.Part::getDeptId).collect(java.util.stream.Collectors.toSet()));}
 private void validateDistribution(List<HrmPayrollCostRuleReqVO.Part> parts) {
  if(parts==null||parts.isEmpty()||parts.size()>100)throw exception(PAYROLL_COST_INVALID);
  BigDecimal sum=BigDecimal.ZERO;Set<String> used=new HashSet<>();Set<Long> ids=new HashSet<>();
  for(HrmPayrollCostRuleReqVO.Part part:parts) {
   if(!validator.validate(part).isEmpty()) throw exception(PAYROLL_COST_INVALID);
   part.setCostCenter(part.getCostCenter()==null?"":part.getCostCenter().trim());part.setProjectCode(part.getProjectCode()==null?"":part.getProjectCode().trim());
   if(!used.add(JsonUtils.toJsonString(Arrays.asList(part.getDeptId(),part.getCostCenter(),part.getProjectCode()))))throw exception(PAYROLL_COST_INVALID);
   sum=sum.add(part.getPercent());ids.add(part.getDeptId());
  }
  if(sum.compareTo(new BigDecimal("100"))!=0)throw exception(PAYROLL_COST_INVALID);

 }
 @Transactional(rollbackFor=Exception.class,isolation=Isolation.READ_COMMITTED)
 public Long createRule(HrmPayrollCostRuleReqVO request,Long actor) {
  if(!validator.validate(request).isEmpty()||request.getEffectiveTo().isBefore(request.getEffectiveFrom())||request.getEffectiveFrom().getYear()<2019||request.getEffectiveTo().getYear()>2099) throw exception(PAYROLL_COST_INVALID);
  entities.check(request.getEntityId(),"MANAGE",actor);entities.approved(request.getEntityId());
  if(employees.selectOneForUpdate(HrmEmployeeDO::getId,request.getEmployeeId())==null)throw exception(PAYROLL_ENTITY_INVALID);
  if(membershipMapper.selectCount(new LambdaQueryWrapperX<HrmPayrollEntityMembershipDO>().eq(HrmPayrollEntityMembershipDO::getEntityId,request.getEntityId())
    .eq(HrmPayrollEntityMembershipDO::getEmployeeId,request.getEmployeeId()).in(HrmPayrollEntityMembershipDO::getStatus,"APPROVED","SUPERSEDED")
    .le(HrmPayrollEntityMembershipDO::getEffectiveFrom,request.getEffectiveTo()).ge(HrmPayrollEntityMembershipDO::getEffectiveTo,request.getEffectiveFrom()))==0) throw exception(PAYROLL_ENTITY_INVALID);
  validateParts(request.getParts());String snapshot=JsonUtils.toJsonString(request);
  HrmPayrollCostRuleDO replay=rules.selectOne(HrmPayrollCostRuleDO::getRequestKey,request.getRequestKey());
  if(replay!=null) {
   if(!Objects.equals(replay.getSubmittedBy(),actor)||!Objects.equals(replay.getRequestSnapshot(),snapshot))throw exception(PAYROLL_COST_INVALID);
   return replay.getId();
  }
  if(request.getPriorRuleId()!=null) {
   HrmPayrollCostRuleDO prior=rules.selectById(request.getPriorRuleId());
   if(prior==null||!Objects.equals(prior.getEntityId(),request.getEntityId())||!Objects.equals(prior.getEmployeeId(),request.getEmployeeId())||!"APPROVED".equals(prior.getStatus()))throw exception(PAYROLL_VERSION_CONFLICT);
  }
  int version=rules.selectList(new LambdaQueryWrapperX<HrmPayrollCostRuleDO>().eq(HrmPayrollCostRuleDO::getEntityId,request.getEntityId()).eq(HrmPayrollCostRuleDO::getEmployeeId,request.getEmployeeId())).stream().mapToInt(HrmPayrollCostRuleDO::getVersion).max().orElse(0)+1;
  HrmPayrollCostRuleDO row=new HrmPayrollCostRuleDO().setEntityId(request.getEntityId()).setEmployeeId(request.getEmployeeId()).setPriorRuleId(request.getPriorRuleId())
    .setEffectiveFrom(request.getEffectiveFrom()).setEffectiveTo(request.getEffectiveTo()).setEvidence(request.getEvidence()).setRequestKey(request.getRequestKey()).setEmployerCost(request.getEmployerCost()).setParts(JsonUtils.toJsonString(request.getParts())).setVersion(version).setStatus("PENDING").setSubmittedBy(actor).setRequestSnapshot(snapshot);
  rules.insert(row);event("RULE",row.getId(),"SUBMIT",request.getEvidence(),row,actor);return row.getId();
 }
 @Transactional(rollbackFor=Exception.class,isolation=Isolation.READ_COMMITTED)
 public void reviewRule(Long id,boolean approve,String reason,Long actor) {
  reason(reason);HrmPayrollCostRuleDO candidate=rules.selectById(id);if(candidate==null)throw exception(PAYROLL_COST_INVALID);
  entities.check(candidate.getEntityId(),"REVIEW",actor);
  HrmEmployeeDO employee=employees.selectOneForUpdate(HrmEmployeeDO::getId,candidate.getEmployeeId());
  HrmPayrollCostRuleDO row=rules.selectOneForUpdate(HrmPayrollCostRuleDO::getId,id);
  if(!"PENDING".equals(row.getStatus()))throw exception(PAYROLL_COST_INVALID);
  if(employee==null||Objects.equals(actor,employee.getUserId())||Objects.equals(actor,row.getSubmittedBy()))throw exception(PAYROLL_SELF_APPROVAL);
  if(approve) {
   validateParts(JsonUtils.parseArray(row.getParts(),HrmPayrollCostRuleReqVO.Part.class));
   HrmPayrollCostRuleDO prior=row.getPriorRuleId()==null?null:rules.selectById(row.getPriorRuleId());
   if(row.getPriorRuleId()!=null&&(prior==null||!"APPROVED".equals(prior.getStatus())))throw exception(PAYROLL_VERSION_CONFLICT);
   Set<Long> ancestors=new HashSet<>();HrmPayrollCostRuleDO ancestor=prior;
   while(ancestor!=null){if(!ancestors.add(ancestor.getId())||!Objects.equals(ancestor.getEntityId(),row.getEntityId())||!Objects.equals(ancestor.getEmployeeId(),row.getEmployeeId()))throw exception(PAYROLL_VERSION_CONFLICT);ancestor=ancestor.getPriorRuleId()==null?null:rules.selectById(ancestor.getPriorRuleId());}
   for(HrmPayrollCostRuleDO existing:rules.selectList(new LambdaQueryWrapperX<HrmPayrollCostRuleDO>().eq(HrmPayrollCostRuleDO::getEntityId,row.getEntityId()).eq(HrmPayrollCostRuleDO::getEmployeeId,row.getEmployeeId()).in(HrmPayrollCostRuleDO::getStatus,"APPROVED","SUPERSEDED")))
    if(!ancestors.contains(existing.getId())&&!existing.getEffectiveTo().isBefore(row.getEffectiveFrom())&&!existing.getEffectiveFrom().isAfter(row.getEffectiveTo()))throw exception(PAYROLL_VERSION_CONFLICT);
   if(prior!=null){prior.setStatus("SUPERSEDED");rules.updateById(prior);event("RULE",prior.getId(),"SUPERSEDE","接续规则"+id,prior,actor);}
  }
  row.setStatus(approve?"APPROVED":"REJECTED").setReviewedBy(actor).setReviewReason(reason).setReviewedTime(LocalDateTime.now(ZoneId.of("Asia/Shanghai")));rules.updateById(row);event("RULE",id,approve?"APPROVE":"REJECT",reason,row,actor);
 }
 public Map<String,Object> options(Long entityId,Long actor) {
  Map<String,Object> out=new LinkedHashMap<>();List<Object> entityOptions=new ArrayList<>();
  for(Long id:entities.accessible("READ",actor)) {Map<String,Object> e=new LinkedHashMap<>();e.put("id",id);e.put("name",entities.name(id));entityOptions.add(e);}
  out.put("entities",entityOptions);if(entityId==null)return out;entities.check(entityId,"READ",actor);
  Set<Long> employeeIds=new TreeSet<>(),deptIds=new TreeSet<>();
  for(HrmPayrollEntityMembershipDO m:membershipMapper.selectList(new LambdaQueryWrapperX<HrmPayrollEntityMembershipDO>().eq(HrmPayrollEntityMembershipDO::getEntityId,entityId).in(HrmPayrollEntityMembershipDO::getStatus,"APPROVED","SUPERSEDED"))) {
   employeeIds.add(m.getEmployeeId());if(m.getDeptId()!=null)deptIds.add(m.getDeptId());
  }
  List<Object> people=new ArrayList<>();for(Long id:employeeIds){HrmEmployeeDO e=employees.selectById(id);if(e!=null){Map<String,Object> person=new LinkedHashMap<>();person.put("id",id);person.put("name",e.getName());person.put("jobNumber",e.getJobNumber());people.add(person);}}
  out.put("employees",people);out.put("departments",departments.getDeptList(deptIds));
  List<Object> batchOptions=new ArrayList<>();for(HrmSalaryMonthRecordDO b:batches.selectList(new LambdaQueryWrapperX<HrmSalaryMonthRecordDO>().eq(HrmSalaryMonthRecordDO::getEntityId,entityId).in(HrmSalaryMonthRecordDO::getStatus,15,16,17,10).orderByDesc(HrmSalaryMonthRecordDO::getYear).orderByDesc(HrmSalaryMonthRecordDO::getMonth).orderByDesc(HrmSalaryMonthRecordDO::getId))) if(Arrays.asList(15,16,17,10).contains(b.getStatus())) {
   Map<String,Object> item=new LinkedHashMap<>();item.put("id",b.getId());item.put("title",b.getTitle());item.put("year",b.getYear());item.put("month",b.getMonth());batchOptions.add(item);
  }
  out.put("batches",batchOptions);return out;
 }
 public List<HrmPayrollCostRuleDO> rules(Long entityId,Long employeeId,Long actor) {
  entities.check(entityId,"READ",actor);
  return rules.selectList(new LambdaQueryWrapperX<HrmPayrollCostRuleDO>().eq(HrmPayrollCostRuleDO::getEntityId,entityId).eqIfPresent(HrmPayrollCostRuleDO::getEmployeeId,employeeId).orderByDesc(HrmPayrollCostRuleDO::getId));
 }
 private HrmPayrollCostRuleDO effectiveRule(Long entity,Long employee,LocalDate date) {
  return rules.selectOne(new LambdaQueryWrapperX<HrmPayrollCostRuleDO>().eq(HrmPayrollCostRuleDO::getEntityId,entity).eq(HrmPayrollCostRuleDO::getEmployeeId,employee)
   .in(HrmPayrollCostRuleDO::getStatus,"APPROVED","SUPERSEDED").le(HrmPayrollCostRuleDO::getEffectiveFrom,date).ge(HrmPayrollCostRuleDO::getEffectiveTo,date).orderByDesc(HrmPayrollCostRuleDO::getVersion).last("LIMIT 1"));
 }
 private HrmPayrollRunDO latest(Long batchId) {
  HrmPayrollRunDO r=runs.selectOne(new LambdaQueryWrapperX<HrmPayrollRunDO>().eq(HrmPayrollRunDO::getMonthRecordId,batchId).orderByDesc(HrmPayrollRunDO::getVersion).last("LIMIT 1"));
  if(r==null)throw exception(PAYROLL_VERSION_CONFLICT);return r;
 }
 private HrmSalaryMonthRecordDO frozen(Long batchId,String action,Long actor) {
  HrmSalaryMonthRecordDO b=batches.selectByIdForUpdate(batchId);scope.check(b,action,actor);
  if(b.getEntityId()==null||!Arrays.asList(15,16,17,10).contains(b.getStatus()))throw exception(SALARY_MONTH_RECORD_STATUS_INVALID);
  return b;
 }
 private Map<String,Object> root(HrmPayrollRunDO run) {
  Set<Long> visited=new HashSet<>();HrmPayrollRunDO row=run;
  while(true) {
   if(!visited.add(row.getId()))throw exception(PAYROLL_VERSION_CONFLICT);
   Map<String,Object> input=JsonUtils.parseObject(row.getInputSnapshot(),Map.class);Object parent=input.get("sourceRunId");if(parent==null)return input;
   if(!(parent instanceof Number))throw exception(PAYROLL_VERSION_CONFLICT);
   HrmPayrollRunDO previous=runs.selectById(((Number)parent).longValue());
   if(previous==null||!Objects.equals(previous.getMonthRecordId(),run.getMonthRecordId())||previous.getVersion()>=row.getVersion())throw exception(PAYROLL_VERSION_CONFLICT);row=previous;
  }
 }
 private String digest(HrmPayrollRunDO run) {
  List<Object> evidence=new ArrayList<>();Set<Long> visited=new HashSet<>();Deque<HrmPayrollRunDO> queue=new ArrayDeque<>();queue.add(run);
  while(!queue.isEmpty()) {
   HrmPayrollRunDO current=queue.remove();if(!visited.add(current.getId()))continue;
   evidence.add(Arrays.asList(current.getId(),current.getInputSnapshot(),current.getRuleSnapshot(),current.getResultSnapshot()));
   Map<String,Object> input=JsonUtils.parseObject(current.getInputSnapshot(),Map.class);Object parent=input.get("sourceRunId");
   if(parent instanceof Number){HrmPayrollRunDO previous=runs.selectById(((Number)parent).longValue());if(previous==null)throw exception(PAYROLL_COST_INVALID);queue.add(previous);}
   if(input.get("retroSources") instanceof List)for(HrmPayrollRetroDO r:JsonUtils.parseArray(JsonUtils.toJsonString(input.get("retroSources")),HrmPayrollRetroDO.class)){
    HrmPayrollPaymentDO payment=payments.selectById(r.getPaymentId());if(payment==null)throw exception(PAYROLL_COST_INVALID);HrmPayrollRunDO original=runs.selectById(payment.getRunId());if(original==null)throw exception(PAYROLL_COST_INVALID);queue.add(original);
   }
  }
  return cn.hutool.crypto.digest.DigestUtil.sha256Hex(JsonUtils.toJsonString(evidence));
 }
 private BigDecimal amount(Map<Integer,BigDecimal> values,int code){BigDecimal v=values.get(code);if(v==null)throw exception(PAYROLL_COST_INVALID);return v;}
 /** Largest-remainder allocation preserves every cent without negative rounding tails. */
 static List<BigDecimal> allocate(BigDecimal total,List<HrmPayrollCostRuleReqVO.Part> parts) {
  total=total.setScale(2,RoundingMode.UNNECESSARY);List<BigDecimal> out=new ArrayList<>(),remainders=new ArrayList<>();
  BigDecimal used=BigDecimal.ZERO;
  for(HrmPayrollCostRuleReqVO.Part part:parts) {
   BigDecimal exact=total.abs().multiply(part.getPercent()).movePointLeft(2),floor=exact.setScale(2,RoundingMode.DOWN);
   out.add(floor);remainders.add(exact.subtract(floor));used=used.add(floor);
  }
  int pennies=total.abs().subtract(used).movePointRight(2).intValueExact();
  List<Integer> order=new ArrayList<>();for(int i=0;i<parts.size();i++)order.add(i);
  order.sort((a,b)->remainders.get(b).compareTo(remainders.get(a)));
  if(pennies<0||pennies>parts.size())throw exception(PAYROLL_COST_INVALID);
  for(int i=0;i<pennies;i++){int index=order.get(i);out.set(index,out.get(index).add(new BigDecimal("0.01")));}
  if(total.signum()<0)for(int i=0;i<out.size();i++)out.set(i,out.get(i).negate());return out;
 }
 private LocalDate lastDate(HrmSalaryMonthRecordDO batch,Map<String,Object> input,Long employee) {
  Object dates=input.get("entityPayrollDates");
  if(dates instanceof Map) {
   Object values=((Map<?,?>)dates).get(employee.toString());
   if(values instanceof List){List<LocalDate> paid=JsonUtils.parseArray(JsonUtils.toJsonString(values),LocalDate.class);if(!paid.isEmpty())return Collections.max(paid);}
  }
  if(HrmPayrollSupplementService.isSupplement(batch)) {
   Object sources=input.get("retroSources");if(!(sources instanceof List))throw exception(PAYROLL_COST_INVALID);
   LocalDate latest=null;
   for(HrmPayrollRetroDO source:JsonUtils.parseArray(JsonUtils.toJsonString(sources),HrmPayrollRetroDO.class)) if(Objects.equals(source.getEmployeeId(),employee)) {
    HrmPayrollPaymentDO payment=payments.selectById(source.getPaymentId());if(payment==null)throw exception(PAYROLL_COST_INVALID);
    HrmPayrollRunDO original=runs.selectById(payment.getRunId());if(original==null)throw exception(PAYROLL_COST_INVALID);
    HrmSalaryMonthRecordDO parent=batches.selectById(payment.getMonthRecordId());if(parent==null||HrmPayrollSupplementService.isSupplement(parent))throw exception(PAYROLL_COST_INVALID);
    LocalDate day=lastDate(parent,root(original),employee);if(latest==null||day.isAfter(latest))latest=day;
   }
   if(latest!=null)return latest;
  }
  throw exception(PAYROLL_COST_INVALID);
 }
 @Transactional(rollbackFor=Exception.class,isolation=Isolation.READ_COMMITTED)
 public Long build(Long batchId,String reason,Long actor) {
  reason(reason);HrmSalaryMonthRecordDO b=frozen(batchId,"CALCULATE",actor);HrmPayrollRunDO run=latest(batchId);Map<String,Object> input=root(run);
  Object source=input.get("entityScope");if(!(source instanceof Map))throw exception(PAYROLL_COST_INVALID);
  List<HrmPayrollEntityMembershipDO> memberships=JsonUtils.parseArray(JsonUtils.toJsonString(((Map<?,?>)source).get("memberships")),HrmPayrollEntityMembershipDO.class);
  if(memberships==null)throw exception(PAYROLL_COST_INVALID);
  List<HrmEmployeeDO> historical=input.get("employees")==null?Collections.emptyList():JsonUtils.parseArray(JsonUtils.toJsonString(input.get("employees")),HrmEmployeeDO.class);
  Map<Long,HrmEmployeeDO> names=new HashMap<>();historical.forEach(e->names.put(e.getId(),e));
  List<HrmPayrollCostRowVO> result=new ArrayList<>();List<Object> selectedRules=new ArrayList<>();Set<Long> seen=new HashSet<>();
  for(HrmSalaryMonthEmployeeRecordDO salary:JsonUtils.parseArray(run.getResultSnapshot(),HrmSalaryMonthEmployeeRecordDO.class)) {
   if(!seen.add(salary.getEmployeeId())||salary.getExpectedPaySalary()==null||salary.getOptionValues()==null)throw exception(PAYROLL_COST_INVALID);
   LocalDate paidDate=lastDate(b,input,salary.getEmployeeId());
   HrmPayrollEntityMembershipDO membership=memberships.stream().filter(m->Objects.equals(m.getEmployeeId(),salary.getEmployeeId())&&Objects.equals(m.getEntityId(),b.getEntityId())&&!m.getEffectiveFrom().isAfter(paidDate)&&!m.getEffectiveTo().isBefore(paidDate))
    .max(Comparator.comparing(HrmPayrollEntityMembershipDO::getEffectiveFrom)).orElseThrow(()->exception(PAYROLL_COST_INVALID));
   if(membership.getDeptId()==null)throw exception(PAYROLL_COST_INVALID);
   LocalDate attribution=paidDate;
   HrmPayrollCostRuleDO rule=effectiveRule(b.getEntityId(),salary.getEmployeeId(),attribution);
   List<HrmPayrollCostRuleReqVO.Part> parts=rule==null?Collections.singletonList(new HrmPayrollCostRuleReqVO.Part().setDeptId(membership.getDeptId()).setPercent(new BigDecimal("100"))):JsonUtils.parseArray(rule.getParts(),HrmPayrollCostRuleReqVO.Part.class);
   validateDistribution(parts);
   Map<Integer,BigDecimal> values=new HashMap<>();for(HrmSalaryMonthEmployeeRecordDO.OptionValue v:salary.getOptionValues())if(v.getCode()==null||v.getValue()==null||values.put(v.getCode(),v.getValue())!=null)throw exception(PAYROLL_COST_INVALID);
   BigDecimal insurance=BigDecimal.ZERO,fund=BigDecimal.ZERO,extra=rule==null||HrmPayrollSupplementService.isSupplement(b)?BigDecimal.ZERO:rule.getEmployerCost();
   if(!HrmPayrollSupplementService.isSupplement(b)) {
    Object fees=input.get("insurancePolicySources");if(!(fees instanceof Map))throw exception(PAYROLL_COST_INVALID);
    HrmPayrollInsurancePolicyService.Quote q=JsonUtils.parseObject(JsonUtils.toJsonString(((Map<?,?>)fees).get(salary.getEmployeeId().toString())),HrmPayrollInsurancePolicyService.Quote.class);
    if(q==null||q.getCorporateInsurance()==null||q.getCorporateFund()==null||!Objects.equals(q.getEntityId(),b.getEntityId())||!Objects.equals(q.getEmployeeId(),salary.getEmployeeId())||!Arrays.asList("INSURED","EXEMPT").contains(q.getCoverage())||(!Objects.equals(q.getPayerEntityId(),b.getEntityId())&&q.getCorporateInsurance().add(q.getCorporateFund()).signum()!=0))throw exception(PAYROLL_COST_INVALID);
    insurance=q.getCorporateInsurance();fund=q.getCorporateFund();
    if(amount(values,110101).compareTo(insurance)!=0||amount(values,120101).compareTo(fund)!=0)throw exception(PAYROLL_COST_INVALID);
   }
   if(extra==null||insurance.signum()<0||fund.signum()<0)throw exception(PAYROLL_COST_INVALID);
   List<BigDecimal> wages=allocate(salary.getExpectedPaySalary(),parts),ins=allocate(insurance,parts),funds=allocate(fund,parts),extras=allocate(extra,parts);
   HrmEmployeeDO e=names.containsKey(salary.getEmployeeId())?names.get(salary.getEmployeeId()):employees.selectById(salary.getEmployeeId());
   for(int i=0;i<parts.size();i++) {
    HrmPayrollCostRuleReqVO.Part part=parts.get(i);DeptRespDTO dept=departments.getDept(part.getDeptId());if(dept==null)throw exception(PAYROLL_COST_INVALID);
    result.add(new HrmPayrollCostRowVO().setEmployeeId(salary.getEmployeeId()).setEmployeeName(e==null?null:e.getName()).setEmployeeCategory(membership.getEmployeeCategory()).setSourceDeptId(membership.getDeptId())
     .setDeptId(part.getDeptId()).setDeptName(dept.getName()).setCostCenter(part.getCostCenter()).setProjectCode(part.getProjectCode()).setPercent(part.getPercent()).setRuleId(rule==null?null:rule.getId())
     .setGross(wages.get(i)).setInsurance(ins.get(i)).setFund(funds.get(i)).setEmployerCost(extras.get(i)).setTotal(wages.get(i).add(ins.get(i)).add(funds.get(i)).add(extras.get(i))));
   }
   Map<String,Object> evidence=new LinkedHashMap<>();evidence.put("employeeId",salary.getEmployeeId());evidence.put("membership",membership);evidence.put("rule",rule);evidence.put("attributionDate",attribution);selectedRules.add(evidence);
  }
  if(result.isEmpty())throw exception(PAYROLL_COST_INVALID);
  int version=reports.selectList(HrmPayrollCostReportDO::getBatchId,batchId).stream().mapToInt(HrmPayrollCostReportDO::getVersion).max().orElse(0)+1;
  HrmPayrollCostReportDO report=new HrmPayrollCostReportDO().setBatchId(batchId).setRunId(run.getId()).setVersion(version).setStatus("PENDING").setSourceDigest(digest(run))
    .setInputSnapshot(JsonUtils.toJsonString(selectedRules)).setResultSnapshot(JsonUtils.toJsonString(result)).setReason(reason).setComputedBy(actor);
  reports.insert(report);event("REPORT",report.getId(),"BUILD",reason,report,actor);return report.getId();
 }
 private void validate(HrmPayrollCostReportDO report) {
  HrmPayrollRunDO run=runs.selectById(report.getRunId());
  if(run==null||!Objects.equals(run.getMonthRecordId(),report.getBatchId())||!Objects.equals(digest(run),report.getSourceDigest())||!Objects.equals(latest(report.getBatchId()).getId(),run.getId()))throw exception(PAYROLL_VERSION_CONFLICT);
 }
 private void validateRules(HrmPayrollCostReportDO report) {
  HrmSalaryMonthRecordDO batch=batches.selectById(report.getBatchId());
  for(Map<String,Object> entry:JsonUtils.parseArray(report.getInputSnapshot(),Map.class)) {
   Long employee=((Number)entry.get("employeeId")).longValue();LocalDate date=JsonUtils.parseObject(JsonUtils.toJsonString(entry.get("attributionDate")),LocalDate.class);
   HrmPayrollCostRuleDO saved=entry.get("rule")==null?null:JsonUtils.parseObject(JsonUtils.toJsonString(entry.get("rule")),HrmPayrollCostRuleDO.class);
   HrmPayrollCostRuleDO current=effectiveRule(batch.getEntityId(),employee,date);
   if(!Objects.equals(saved==null?null:saved.getId(),current==null?null:current.getId()))throw exception(PAYROLL_VERSION_CONFLICT);
  }
 }
 @Transactional(rollbackFor=Exception.class,isolation=Isolation.READ_COMMITTED)
 public void reviewReport(Long id,boolean approve,String reason,Long actor) {
  reason(reason);HrmPayrollCostReportDO r=reports.selectById(id);if(r==null)throw exception(PAYROLL_COST_INVALID);
  frozen(r.getBatchId(),"APPROVE",actor);r=reports.selectOneForUpdate(HrmPayrollCostReportDO::getId,id);
  if(!"PENDING".equals(r.getStatus()))throw exception(PAYROLL_COST_INVALID);
  if(Objects.equals(r.getComputedBy(),actor))throw exception(PAYROLL_SELF_APPROVAL);
  if(approve){validate(r);validateRules(r);}
  r.setStatus(approve?"APPROVED":"REJECTED").setReviewedBy(actor).setReviewedTime(LocalDateTime.now(ZoneId.of("Asia/Shanghai"))).setReviewReason(reason);
  reports.updateById(r);event("REPORT",id,approve?"APPROVE":"REJECT",reason,r,actor);
 }
 public List<HrmPayrollCostReportDO> reports(Long batchId,Long actor) {
  scope.check(batches.selectById(batchId),"READ",actor);
  return reports.selectList(new LambdaQueryWrapperX<HrmPayrollCostReportDO>().eq(HrmPayrollCostReportDO::getBatchId,batchId).orderByDesc(HrmPayrollCostReportDO::getVersion));
 }
 @Transactional(rollbackFor=Exception.class,isolation=Isolation.REPEATABLE_READ)
 public List<HrmPayrollCostRowVO> rows(Long id,boolean export,Long actor) {
  HrmPayrollCostReportDO r=reports.selectById(id);if(r==null)throw exception(PAYROLL_COST_INVALID);
  scope.check(batches.selectById(r.getBatchId()),export?"EXPORT":"READ",actor);
  if(export){frozen(r.getBatchId(),"EXPORT",actor);if(!"APPROVED".equals(r.getStatus()))throw exception(PAYROLL_COST_INVALID);validate(r);event("REPORT",id,"EXPORT","导出批准成本报表",Collections.singletonMap("runId",r.getRunId()),actor);}
  return JsonUtils.parseArray(r.getResultSnapshot(),HrmPayrollCostRowVO.class);
 }
 public List<HrmPayrollCostEventDO> history(String kind,Long id,Long actor) {
  if("RULE".equals(kind)){HrmPayrollCostRuleDO r=rules.selectById(id);if(r==null)throw exception(PAYROLL_COST_INVALID);entities.check(r.getEntityId(),"READ",actor);}
  else if("REPORT".equals(kind)){HrmPayrollCostReportDO r=reports.selectById(id);if(r==null)throw exception(PAYROLL_COST_INVALID);scope.check(batches.selectById(r.getBatchId()),"READ",actor);}
  else throw exception(PAYROLL_COST_INVALID);
  return events.selectList(new LambdaQueryWrapperX<HrmPayrollCostEventDO>().eq(HrmPayrollCostEventDO::getKind,kind).eq(HrmPayrollCostEventDO::getTargetId,id).orderByAsc(HrmPayrollCostEventDO::getId));
 }
}
