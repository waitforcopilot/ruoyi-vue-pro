package cn.iocoder.yudao.module.hrm.service.payroll;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollCostRowVO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.salary.monthrecord.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.salary.monthrecord.HrmSalaryMonthRecordMapper;
import java.math.BigDecimal;
import java.util.*;
import javax.annotation.Resource;
import lombok.Data;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
/** Subject-scoped summary never serializes employee monetary/tax/bank source objects. */
@Service public class HrmPayrollOverviewService {
 @Resource private HrmSalaryMonthRecordMapper batches;
 @Resource private HrmPayrollRunMapper runs;
 @Resource private HrmPayrollPaymentMapper payments;
 @Resource private HrmPayrollInputIssueMapper issues;
 @Resource private HrmPayrollScopeService scope;
 @Resource private HrmPayrollEntityService entities;
 @Resource private HrmPayrollInputService inputs;
 @Resource private HrmPayrollReadinessService readiness;
 @Resource private HrmPayrollCostService costs;
 @Resource private HrmPayrollSupplementService supplements;
 @Resource private HrmPayrollOffsetService offsets;
 @Data public static class Summary {
  private Long batchId,entityId,parentBatchId,runId,costReportId;
  private String title,batchKey,sourceMode,sourceStatus,costStatus;
  private Integer year,month,status,version,employeeCount,openIssues,paymentSuccess,paymentPending,paymentFailed;
  private java.time.LocalDate expectedPaymentDate;
  private BigDecimal gross,tax,net,personalInsurance,personalFund,corporateInsurance,corporateFund,approvedCost,paidAmount;
 }
 @Data public static class Detail {
  private Summary summary;
  private List<Map<String,Object>> employees;
  private List<HrmPayrollReadinessService.Signal> signals;
  private int readyCount,signalCount;
 }
 private boolean frozen(HrmSalaryMonthRecordDO b){return Arrays.asList(15,16,17,10).contains(b.getStatus());}
 private HrmPayrollRunDO latest(Long batch){return runs.selectOne(new LambdaQueryWrapperX<HrmPayrollRunDO>().eq(HrmPayrollRunDO::getMonthRecordId,batch).orderByDesc(HrmPayrollRunDO::getVersion).last("LIMIT 1"));}
 private HrmPayrollRunDO root(HrmPayrollRunDO run) {
  HrmPayrollRunDO row=run;Set<Long> seen=new HashSet<>();
  while(true){if(row==null||!seen.add(row.getId()))throw exception(PAYROLL_VERSION_CONFLICT);Object parent=map(row.getInputSnapshot()).get("sourceRunId");if(parent==null)return row;if(!(parent instanceof Number))throw exception(PAYROLL_VERSION_CONFLICT);HrmPayrollRunDO prior=runs.selectById(((Number)parent).longValue());if(prior==null||!Objects.equals(prior.getMonthRecordId(),run.getMonthRecordId())||prior.getVersion()>=row.getVersion())throw exception(PAYROLL_VERSION_CONFLICT);row=prior;}
 }
 private Map<String,Object> map(String json){Map<String,Object> v=JsonUtils.parseObject(json,Map.class);if(v==null)throw exception(PAYROLL_VERSION_CONFLICT);return v;}
 private Object entry(Object value,Long employee){return value instanceof Map?((Map<?,?>)value).get(employee.toString()):null;}
 private Summary summarize(HrmSalaryMonthRecordDO b,Set<Long> selected,Long actor) {
  scope.check(b,"READ",actor);if(b.getEntityId()==null)throw exception(PAYROLL_ENTITY_INVALID);
  Summary out=new Summary().setBatchId(b.getId()).setEntityId(b.getEntityId()).setParentBatchId(b.getParentBatchId()).setTitle(b.getTitle()).setBatchKey(b.getBatchKey()).setYear(b.getYear()).setMonth(b.getMonth()).setStatus(b.getStatus()).setExpectedPaymentDate(b.getExpectedPaymentDate()).setSourceMode(frozen(b)?"FROZEN_SNAPSHOT":"CURRENT_TRIAL");
  out.setOpenIssues(issues.selectCount(new LambdaQueryWrapperX<HrmPayrollInputIssueDO>().eq(HrmPayrollInputIssueDO::getBatchId,b.getId()).inIfPresent(HrmPayrollInputIssueDO::getEmployeeId,selected).in(HrmPayrollInputIssueDO::getStatus,"OPEN","SUBMITTED")).intValue());
  HrmPayrollRunDO run=latest(b.getId());
  if(run==null){out.setSourceStatus("尚无不可变核算版本，金额未知").setCostStatus("尚无冻结工资来源");return out;}
  out.setRunId(run.getId()).setVersion(run.getVersion()).setExpectedPaymentDate(run.getExpectedPaymentDate()).setSourceStatus(frozen(b)?"金额来自冻结核算快照":"金额来自最新试算；当前来源须另行检查");
  if(Objects.equals(b.getStatus(),5))out.setSourceStatus("输入已修改，金额为上次试算，须重新核算");
  List<HrmSalaryMonthEmployeeRecordDO> rows=JsonUtils.parseArray(run.getResultSnapshot(),HrmSalaryMonthEmployeeRecordDO.class);if(rows==null||rows.isEmpty())throw exception(PAYROLL_VERSION_CONFLICT);
  BigDecimal gross=BigDecimal.ZERO,tax=BigDecimal.ZERO,net=BigDecimal.ZERO;BigDecimal[] fees={BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO};Set<Long> seen=new HashSet<>();boolean feesKnown=true;int count=0;
  for(HrmSalaryMonthEmployeeRecordDO row:rows){if(row.getEmployeeId()==null||!seen.add(row.getEmployeeId())||row.getExpectedPaySalary()==null||row.getPersonalTax()==null||row.getRealPaySalary()==null)throw exception(PAYROLL_VERSION_CONFLICT);if(selected!=null&&!selected.contains(row.getEmployeeId()))continue;count++;gross=gross.add(row.getExpectedPaySalary());tax=tax.add(row.getPersonalTax());net=net.add(row.getRealPaySalary());
   if(!HrmPayrollSupplementService.isSupplement(b)){Map<Integer,BigDecimal> values=new HashMap<>();if(row.getOptionValues()!=null)for(HrmSalaryMonthEmployeeRecordDO.OptionValue value:row.getOptionValues())if(values.put(value.getCode(),value.getValue())!=null)throw exception(PAYROLL_VERSION_CONFLICT);int[] codes={100101,100102,110101,120101};for(int i=0;i<4;i++){BigDecimal value=values.get(codes[i]);if(value==null)feesKnown=false;else fees[i]=fees[i].add(value);}}
  }
  if(selected!=null&&!seen.containsAll(selected)){
   if(frozen(b))throw exception(PAYROLL_ENTITY_INVALID);
   Set<Long> eligible=new HashSet<>();if(HrmPayrollSupplementService.isSupplement(b))eligible.addAll(supplements.readiness(b.getId(),actor).keySet());else for(HrmEmployeeDO employee:inputs.roster(b))eligible.add(employee.getId());if(!eligible.containsAll(selected))throw exception(PAYROLL_ENTITY_INVALID);
   return out.setSourceStatus("所选人员尚未全部进入该试算版本，请重新核算").setCostStatus("来源不完整，成本未知");
  }
  out.setEmployeeCount(count).setGross(gross).setTax(tax).setNet(net);if(feesKnown)out.setPersonalInsurance(fees[0]).setPersonalFund(fees[1]).setCorporateInsurance(fees[2]).setCorporateFund(fees[3]);
  out.setCostStatus("待冻结及成本报表独立复核");
  if(frozen(b)){HrmPayrollCostReportDO report=costs.approvedReport(b.getId(),actor);if(report==null)out.setCostStatus("缺少批准成本报告，不能将应发当作总成本");else{BigDecimal total=BigDecimal.ZERO;for(HrmPayrollCostRowVO row:JsonUtils.parseArray(report.getResultSnapshot(),HrmPayrollCostRowVO.class))if(selected==null||selected.contains(row.getEmployeeId()))total=total.add(row.getTotal());out.setCostReportId(report.getId()).setApprovedCost(total).setCostStatus("批准成本报告：应发 + 单位缴费 + 配置单位成本 + 结清工资收回调整");}}
  int success=0,pending=0,failed=0;BigDecimal paid=BigDecimal.ZERO;
  for(HrmPayrollPaymentDO payment:payments.selectList(HrmPayrollPaymentDO::getMonthRecordId,b.getId())){if(!Objects.equals(payment.getRunId(),run.getId()))throw exception(PAYROLL_VERSION_CONFLICT);if(selected!=null&&!selected.contains(payment.getEmployeeId()))continue;if("SUCCESS".equals(payment.getStatus())){success++;paid=paid.add(payment.getAmount());}else if("FAILED".equals(payment.getStatus()))failed++;else pending++;}
  return out.setPaymentSuccess(success).setPaymentPending(pending).setPaymentFailed(failed).setPaidAmount(paid);
 }
 public List<Map<String,Object>> entities(Long actor){List<Map<String,Object>> out=new ArrayList<>();for(Long id:entities.accessible("READ",actor)){Map<String,Object> row=new LinkedHashMap<>();row.put("id",id);row.put("name",entities.name(id));out.add(row);}return out;}
 @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
 public List<Summary> history(Long entity,Integer year,Integer month,Integer status,String type,Long actor){
  if(year==null||year<2019||year>2099||month!=null&&(month<1||month>12)||status!=null&&!Arrays.asList(5,10,11,12,13,14,15,16,17).contains(status)||type!=null&&!Arrays.asList("REGULAR","SUPPLEMENT").contains(type))throw exception(PAYROLL_FINANCIAL_INVALID);
  entities.check(entity,"READ",actor);List<Summary> out=new ArrayList<>();for(HrmSalaryMonthRecordDO b:batches.selectList(new LambdaQueryWrapperX<HrmSalaryMonthRecordDO>().eq(HrmSalaryMonthRecordDO::getEntityId,entity).eq(HrmSalaryMonthRecordDO::getYear,year).eqIfPresent(HrmSalaryMonthRecordDO::getMonth,month).eqIfPresent(HrmSalaryMonthRecordDO::getStatus,status).orderByDesc(HrmSalaryMonthRecordDO::getMonth).orderByDesc(HrmSalaryMonthRecordDO::getId))){if(type!=null&&!("SUPPLEMENT".equals(type)?HrmPayrollSupplementService.isSupplement(b):"REGULAR".equals(b.getBatchKey())))continue;out.add(summarize(b,null,actor));}return out;
 }
 private HrmPayrollReadinessService.Signal historical(HrmSalaryMonthRecordDO b,HrmPayrollRunDO run,HrmPayrollRunDO original,HrmSalaryMonthEmployeeRecordDO row,String kind){
  Map<String,Object> input=map(original.getInputSnapshot()),rules=map(original.getRuleSnapshot());Object proof=null;Long employee=row.getEmployeeId();String status="SNAPSHOT_MISSING";
  if(HrmPayrollInputService.KINDS.contains(kind)){proof=entry(input.get("approvedInputs"),employee);if(proof instanceof Map){proof=((Map<?,?>)proof).get(kind);if(proof instanceof Map&&"APPROVED".equals(((Map<?,?>)proof).get("status")))status="SNAPSHOT_PRESENT";}}
  else if("PAYMENT_PLAN".equals(kind)){proof=run.getExpectedPaymentDate();if(proof!=null)status="SNAPSHOT_PRESENT";}
  else if("SALARY_GROUP".equals(kind)){proof=entry(rules.get("salaryGroups"),employee);if(proof!=null&&entry(rules.get("taxRules"),employee)!=null)status="SNAPSHOT_PRESENT";}
  else if("SALARY_INFO".equals(kind)){proof=entry(input.get("salaryValues"),employee);if(proof instanceof Collection&&!((Collection<?>)proof).isEmpty())status="SNAPSHOT_PRESENT";}
  else if("INSURANCE".equals(kind)){proof=entry(input.get("insurancePolicySources"),employee);if(proof!=null)status="SNAPSHOT_PRESENT";}
  else{Object rule=entry(rules.get("taxRules"),employee);boolean exempt=rule instanceof Map&&(Boolean.FALSE.equals(((Map<?,?>)rule).get("taxEnabled"))||Arrays.asList(2,3).contains(((Map<?,?>)rule).get("type")));if(exempt){proof=rule;status="NOT_APPLICABLE";}else if(row.getTaxSourceSnapshot()!=null){proof=map(row.getTaxSourceSnapshot()).get("TAX_POLICY".equals(kind)?"policy":"profile");if(proof!=null)status="SNAPSHOT_PRESENT";}}
  boolean ready=!"SNAPSHOT_MISSING".equals(status);return new HrmPayrollReadinessService.Signal().setBatchId(b.getId()).setEmployeeId(employee).setKind(kind).setStatus(status).setReady(ready).setReason(ready?"冻结版本保留的适用证据，不重评当前来源":"历史版本未保存此项完整证据，保持未知").setProofHash(cn.hutool.crypto.digest.DigestUtil.sha256Hex(JsonUtils.toJsonString(proof)));
 }
 @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
 public Detail detail(Long batchId,Set<Long> selected,Long actor){
  HrmSalaryMonthRecordDO b=batches.selectById(batchId);scope.check(b,"READ",actor);if(b.getEntityId()==null)throw exception(PAYROLL_ENTITY_INVALID);if(selected!=null&&(selected.isEmpty()||selected.size()>5000||selected.contains(null)))throw exception(PAYROLL_FINANCIAL_INVALID);
  Detail out=new Detail().setSummary(summarize(b,selected,actor)).setEmployees(new ArrayList<>()).setSignals(new ArrayList<>());HrmPayrollRunDO run=latest(batchId);Map<Long,HrmSalaryMonthEmployeeRecordDO> salaries=new LinkedHashMap<>();Map<Long,HrmEmployeeDO> people=new LinkedHashMap<>();HrmPayrollRunDO original=null;
  if(run!=null){original=root(run);for(HrmSalaryMonthEmployeeRecordDO row:JsonUtils.parseArray(run.getResultSnapshot(),HrmSalaryMonthEmployeeRecordDO.class))salaries.put(row.getEmployeeId(),row);Object stored=map(original.getInputSnapshot()).get("employees");if(stored instanceof List)for(HrmEmployeeDO e:JsonUtils.parseArray(JsonUtils.toJsonString(stored),HrmEmployeeDO.class))people.put(e.getId(),e);}
  if(!frozen(b)&&"REGULAR".equals(b.getBatchKey())){people.clear();for(HrmEmployeeDO e:inputs.roster(b))people.put(e.getId(),e);}
  Map<Long,Boolean> supplementReadiness=HrmPayrollSupplementService.isSupplement(b)&&!frozen(b)?supplements.readiness(batchId,actor):Collections.emptyMap();
  Set<Long> ids=new LinkedHashSet<>(salaries.keySet());ids.addAll(supplementReadiness.keySet());if(!frozen(b))ids.addAll(people.keySet());if(selected!=null&&!ids.containsAll(selected))throw exception(PAYROLL_ENTITY_INVALID);
  for(Long id:ids){HrmEmployeeDO e=people.get(id);Map<String,Object> item=new LinkedHashMap<>();item.put("id",id);item.put("name",e==null?"历史员工 #"+id:e.getName());item.put("jobNumber",e==null?null:e.getJobNumber());out.getEmployees().add(item);if(selected!=null&&!selected.contains(id))continue;
   if(HrmPayrollSupplementService.isSupplement(b)){
    boolean ready=Boolean.TRUE.equals(supplementReadiness.get(id));
    if(frozen(b)){
     Object saved=map(original.getInputSnapshot()).get("retroSources");boolean approved=false;
     if(saved instanceof List)for(HrmPayrollRetroDO source:JsonUtils.parseArray(JsonUtils.toJsonString(saved),HrmPayrollRetroDO.class))if(Objects.equals(source.getEmployeeId(),id)&&"APPROVED".equals(source.getStatus())&&source.getDelta()!=null&&source.getDelta().signum()>0)approved=true;
     HrmSalaryMonthEmployeeRecordDO row=salaries.get(id);Map<String,Object> tax=row.getTaxSourceSnapshot()==null?Collections.emptyMap():map(row.getTaxSourceSnapshot());
     ready=approved&&run.getExpectedPaymentDate()!=null&&tax.get("policy")!=null&&tax.get("profile")!=null;
    }
    out.getSignals().add(new HrmPayrollReadinessService.Signal().setBatchId(batchId).setEmployeeId(id).setKind("SUPPLEMENT").setStatus(frozen(b)?ready?"SNAPSHOT_PRESENT":"SNAPSHOT_MISSING":ready?"READY":"MISSING").setReady(ready).setReason(frozen(b)?ready?"冻结版本保留批准正差额、发放日期与税务证据":"历史补发证据不完整，保持未知":ready?"批准差额、原付款及税务来源已通过实际补发准备校验":"批准差额、原付款、配置或税务来源尚未全部就绪"));
   }
   else for(String kind:HrmPayrollReadinessService.KINDS){HrmPayrollReadinessService.Signal signal=frozen(b)?historical(b,run,original,salaries.get(id),kind):e==null?new HrmPayrollReadinessService.Signal().setBatchId(batchId).setEmployeeId(id).setKind(kind).setStatus("STALE_SCOPE").setReady(false).setReason("员工不再属于当前计薪范围，须重新核算"):readiness.inspect(batchId,id,kind,actor);signal.setEmployeeName(e==null?null:e.getName()).setJobNumber(e==null?null:e.getJobNumber());out.getSignals().add(signal);}
  }
  if(!frozen(b)&&"REGULAR".equals(b.getBatchKey())){
   Map<Long,List<HrmPayrollOffsetDO>> grouped=new LinkedHashMap<>();for(HrmPayrollOffsetDO row:offsets.list(batchId,actor))if(Arrays.asList("PENDING","APPROVED").contains(row.getStatus())&&(selected==null||selected.contains(row.getEmployeeId())))grouped.computeIfAbsent(row.getEmployeeId(),key->new ArrayList<>()).add(row);
   for(Map.Entry<Long,List<HrmPayrollOffsetDO>> group:grouped.entrySet()){boolean ready=group.getValue().stream().allMatch(row->"APPROVED".equals(row.getStatus())&&Boolean.TRUE.equals(row.getSourceFresh()));HrmEmployeeDO employee=people.get(group.getKey());out.getSignals().add(new HrmPayrollReadinessService.Signal().setBatchId(batchId).setEmployeeId(group.getKey()).setEmployeeName(employee==null?null:employee.getName()).setKind("SETTLEMENT_OFFSETS").setReady(ready).setStatus(ready?"READY":"MISSING").setReason(ready?"批准工资收退差额来源已核实，累计输入须与本批次基数一致":"工资收退差额分配待审或来源失效，阻断核算"));}
  }else if(frozen(b)&&original!=null){Object savedOffsets=map(original.getInputSnapshot()).get("settlementOffsets");if(savedOffsets instanceof List)for(HrmPayrollOffsetService.Source source:JsonUtils.parseArray(JsonUtils.toJsonString(savedOffsets),HrmPayrollOffsetService.Source.class))if(selected==null||selected.contains(source.getEmployeeId()))out.getSignals().add(new HrmPayrollReadinessService.Signal().setBatchId(batchId).setEmployeeId(source.getEmployeeId()).setKind("SETTLEMENT_OFFSETS").setReady(true).setStatus("SNAPSHOT_PRESENT").setReason("冻结版本保留的批准工资收退差额分配，不改判当前来源"));}
  out.setSignalCount(out.getSignals().size());out.setReadyCount((int)out.getSignals().stream().filter(HrmPayrollReadinessService.Signal::isReady).count());return out;
 }
}
