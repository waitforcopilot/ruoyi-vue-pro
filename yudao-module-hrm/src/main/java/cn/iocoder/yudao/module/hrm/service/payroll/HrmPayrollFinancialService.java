package cn.iocoder.yudao.module.hrm.service.payroll;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.salary.monthrecord.HrmSalaryMonthRecordDO;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.salary.monthrecord.HrmSalaryMonthRecordMapper;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import java.math.*;
import java.time.*;
import java.util.*;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import lombok.Data;
@Service public class HrmPayrollFinancialService {
 @Resource private HrmPayrollFinancialPlanMapper plans;
 @Resource private HrmPayrollFinancialEventMapper events;
 @Resource private HrmSalaryMonthRecordMapper batches;
 @Resource private HrmPayrollEntityService entities;
 @Resource private HrmPayrollScopeService scope;
 @Resource private HrmPayrollCostService costs;
 @Resource private DeptApi departments;
 @Resource private javax.validation.Validator validator;
 @Data public static class Analysis {private boolean ready;private List<Long> missingBatchIds,reportIds;private String sourceStatus;private List<HrmPayrollCostAnalysisRowVO> rows;}
 private void period(Integer year,Integer month){if(year==null||month==null||year<2019||year>2099||month<1||month>12)throw exception(PAYROLL_FINANCIAL_INVALID);}
 private void reason(String value){if(value==null||value.trim().isEmpty()||value.length()>1000)throw exception(PAYROLL_FINANCIAL_INVALID);}
 private String key(String dimension,String key){return JsonUtils.toJsonString(Arrays.asList(dimension,key));}
 private void event(Long entity,Integer year,Integer month,Long id,String action,String reason,Object snapshot,Long actor){events.insert(new HrmPayrollFinancialEventDO().setEntityId(entity).setYear(year).setMonth(month).setPlanId(id).setAction(action).setReason(reason).setSnapshot(JsonUtils.toJsonString(snapshot)).setActorId(actor));}
 private HrmPayrollFinancialPlanDO active(Long entity,Integer year,Integer month,String dimension,String key) {
  return plans.selectOne(new LambdaQueryWrapperX<HrmPayrollFinancialPlanDO>().eq(HrmPayrollFinancialPlanDO::getEntityId,entity).eq(HrmPayrollFinancialPlanDO::getYear,year).eq(HrmPayrollFinancialPlanDO::getMonth,month)
    .eq(HrmPayrollFinancialPlanDO::getDimension,dimension).eq(HrmPayrollFinancialPlanDO::getDimensionKey,key).eq(HrmPayrollFinancialPlanDO::getStatus,"APPROVED"));
 }
 private void normalize(HrmPayrollFinancialPlanReqVO req) {
  req.setDimensionKey(req.getDimensionKey().trim());
  if("DEPARTMENT".equals(req.getDimension())){try{req.setDimensionKey(Long.valueOf(req.getDimensionKey()).toString());}catch(NumberFormatException e){throw exception(PAYROLL_FINANCIAL_INVALID);}}
  if(Arrays.asList("COST_CENTER","PROJECT","CATEGORY").contains(req.getDimension()))req.setDimensionKey(req.getDimensionKey().toUpperCase(Locale.ROOT));
  if(req.getBudget()!=null)req.setBudget(req.getBudget().setScale(2,RoundingMode.UNNECESSARY));if(req.getRevenue()!=null)req.setRevenue(req.getRevenue().setScale(2,RoundingMode.UNNECESSARY));if(req.getHeadcount()!=null){try{req.setHeadcount(req.getHeadcount().setScale(0,RoundingMode.UNNECESSARY));}catch(ArithmeticException e){throw exception(PAYROLL_FINANCIAL_INVALID);}}
 }
 private void validate(HrmPayrollFinancialPlanReqVO req) {
  if(!validator.validate(req).isEmpty()||(req.getBudget()==null&&req.getRevenue()==null&&req.getHeadcount()==null))throw exception(PAYROLL_FINANCIAL_INVALID);
  normalize(req);if(req.getDimensionKey().isEmpty())throw exception(PAYROLL_FINANCIAL_INVALID);
  if("ENTITY".equals(req.getDimension())&&!"ALL".equals(req.getDimensionKey()))throw exception(PAYROLL_FINANCIAL_INVALID);
  if("DEPARTMENT".equals(req.getDimension())){try{Long id=Long.valueOf(req.getDimensionKey());if(id<=0)throw exception(PAYROLL_FINANCIAL_INVALID);departments.validateDeptList(Collections.singleton(id));req.setDimensionKey(id.toString());}catch(NumberFormatException e){throw exception(PAYROLL_FINANCIAL_INVALID);}}
  HrmPayrollFinancialPlanDO previous=active(req.getEntityId(),req.getYear(),req.getMonth(),req.getDimension(),req.getDimensionKey());
  if(!Objects.equals(req.getPriorPlanId(),previous==null?null:previous.getId()))throw exception(PAYROLL_VERSION_CONFLICT);
 }
 @Transactional(rollbackFor=Exception.class,isolation=Isolation.READ_COMMITTED)
 public Long create(HrmPayrollFinancialPlanReqVO req,Long actor) {
  if(!validator.validate(req).isEmpty())throw exception(PAYROLL_FINANCIAL_INVALID);
  entities.check(req.getEntityId(),"MANAGE",actor);entities.lockApproved(req.getEntityId());
  // Normalize before request comparison; null numeric cells remain unknown.
  normalize(req);String snapshot=JsonUtils.toJsonString(req);
  HrmPayrollFinancialPlanDO replay=plans.selectOne(HrmPayrollFinancialPlanDO::getRequestKey,req.getRequestKey());
  if(replay!=null){if(!Objects.equals(replay.getSubmittedBy(),actor)||!Objects.equals(replay.getRequestSnapshot(),snapshot))throw exception(PAYROLL_FINANCIAL_INVALID);return replay.getId();}
  validate(req);snapshot=JsonUtils.toJsonString(req);
  int version=plans.selectList(new LambdaQueryWrapperX<HrmPayrollFinancialPlanDO>().eq(HrmPayrollFinancialPlanDO::getEntityId,req.getEntityId()).eq(HrmPayrollFinancialPlanDO::getYear,req.getYear()).eq(HrmPayrollFinancialPlanDO::getMonth,req.getMonth()).eq(HrmPayrollFinancialPlanDO::getDimension,req.getDimension()).eq(HrmPayrollFinancialPlanDO::getDimensionKey,req.getDimensionKey())).stream().mapToInt(HrmPayrollFinancialPlanDO::getVersion).max().orElse(0)+1;
  HrmPayrollFinancialPlanDO row=BeanUtils.toBean(req,HrmPayrollFinancialPlanDO.class).setVersion(version).setStatus("PENDING").setSubmittedBy(actor).setRequestSnapshot(snapshot);try{plans.insert(row);}catch(org.springframework.dao.DuplicateKeyException e){throw exception(PAYROLL_FINANCIAL_INVALID);}event(row.getEntityId(),row.getYear(),row.getMonth(),row.getId(),"SUBMIT",row.getEvidence(),row,actor);return row.getId();
 }
 @Transactional(rollbackFor=Exception.class,isolation=Isolation.READ_COMMITTED)
 public void review(Long id,boolean approve,String reason,Long actor) {
  reason(reason);HrmPayrollFinancialPlanDO candidate=plans.selectById(id);if(candidate==null)throw exception(PAYROLL_FINANCIAL_INVALID);
  entities.check(candidate.getEntityId(),"APPROVE",actor);entities.lockApproved(candidate.getEntityId());HrmPayrollFinancialPlanDO row=plans.selectOneForUpdate(HrmPayrollFinancialPlanDO::getId,id);
  if(!"PENDING".equals(row.getStatus()))throw exception(PAYROLL_FINANCIAL_INVALID);if(Objects.equals(actor,row.getSubmittedBy()))throw exception(PAYROLL_SELF_APPROVAL);
  if(approve){HrmPayrollFinancialPlanReqVO request=JsonUtils.parseObject(row.getRequestSnapshot(),HrmPayrollFinancialPlanReqVO.class);validate(request);HrmPayrollFinancialPlanDO previous=active(row.getEntityId(),row.getYear(),row.getMonth(),row.getDimension(),row.getDimensionKey());if(previous!=null){previous.setStatus("SUPERSEDED");plans.updateById(previous);event(previous.getEntityId(),previous.getYear(),previous.getMonth(),previous.getId(),"SUPERSEDE","接续版本"+id,previous,actor);}}
  row.setStatus(approve?"APPROVED":"REJECTED").setReviewedBy(actor).setReviewedTime(LocalDateTime.now(ZoneId.of("Asia/Shanghai"))).setReviewReason(reason);plans.updateById(row);event(row.getEntityId(),row.getYear(),row.getMonth(),id,approve?"APPROVE":"REJECT",reason,row,actor);
 }
 public List<HrmPayrollFinancialPlanDO> list(Long entity,Integer year,Integer month,Long actor){period(year,month);entities.check(entity,"READ",actor);return plans.selectList(new LambdaQueryWrapperX<HrmPayrollFinancialPlanDO>().eq(HrmPayrollFinancialPlanDO::getEntityId,entity).eq(HrmPayrollFinancialPlanDO::getYear,year).eq(HrmPayrollFinancialPlanDO::getMonth,month).orderByDesc(HrmPayrollFinancialPlanDO::getId));}
 public List<HrmPayrollFinancialEventDO> history(Long entity,Integer year,Integer month,Long actor){period(year,month);entities.check(entity,"READ",actor);return events.selectList(new LambdaQueryWrapperX<HrmPayrollFinancialEventDO>().eq(HrmPayrollFinancialEventDO::getEntityId,entity).eq(HrmPayrollFinancialEventDO::getYear,year).eq(HrmPayrollFinancialEventDO::getMonth,month).orderByAsc(HrmPayrollFinancialEventDO::getId));}
 private BigDecimal number(String value,String field){if(value==null||value.trim().isEmpty())return null;try{return new BigDecimal(value.trim());}catch(NumberFormatException e){throw new IllegalArgumentException(field+"无法解析为金额/人数");}}
 private HrmPayrollFinancialPlanReqVO row(Long entity,Integer year,Integer month,HrmPayrollFinancialImportRowVO row,String requestKey,String source) {
  int y,m;try{y=Integer.parseInt(row.getYear().trim());m=Integer.parseInt(row.getMonth().trim());}catch(RuntimeException e){throw new IllegalArgumentException("年度/月份须填写整数");}if(y!=year||m!=month)throw new IllegalArgumentException("年度/月份与所选期间不一致");
  return new HrmPayrollFinancialPlanReqVO().setEntityId(entity).setYear(y).setMonth(m).setDimension(row.getDimension()==null?null:row.getDimension().trim()).setDimensionKey(row.getDimensionKey()==null?null:row.getDimensionKey().trim())
   .setPriorPlanId(row.getPriorPlanId()==null||row.getPriorPlanId().trim().isEmpty()?null:Long.valueOf(row.getPriorPlanId().trim())).setBudget(number(row.getBudget(),"预算")).setRevenue(number(row.getRevenue(),"营收")).setHeadcount(number(row.getHeadcount(),"编制")).setEvidence(row.getEvidence())
   .setSourceReference(source+(row.getSourceSheet()==null?"":":"+row.getSourceSheet()+":"+row.getSourceRow())).setRequestKey(cn.hutool.crypto.digest.DigestUtil.sha256Hex(requestKey+":"+row.getDimension()+":"+row.getDimensionKey()));
 }
 public List<HrmPayrollFinancialPreviewVO> preview(Long entity,Integer year,Integer month,List<HrmPayrollFinancialImportRowVO> rows,Long actor) {
  period(year,month);entities.check(entity,"MANAGE",actor);if(rows==null||rows.isEmpty()||rows.size()>5000)throw exception(PAYROLL_FINANCIAL_INVALID);
  List<HrmPayrollFinancialPreviewVO> out=new ArrayList<>();Set<String> seen=new HashSet<>();int index=1;
  for(HrmPayrollFinancialImportRowVO source:rows) {
   HrmPayrollFinancialPreviewVO line=new HrmPayrollFinancialPreviewVO().setRow(source.getSourceRow()==null?++index:source.getSourceRow()).setSheet(source.getSourceSheet()).setDimension(source.getDimension()).setDimensionKey(source.getDimensionKey());
   try {
    HrmPayrollFinancialPlanReqVO req=row(entity,year,month,source,"PREVIEW","UPLOAD");
    List<String> failures=new ArrayList<>();Map<String,String> labels=new HashMap<>();labels.put("dimension","范围类型");labels.put("dimensionKey","范围编号");labels.put("budget","预算");labels.put("revenue","营收");labels.put("headcount","编制");labels.put("evidence","核实依据");
    validator.validate(req).forEach(v->failures.add(labels.getOrDefault(v.getPropertyPath().toString(),v.getPropertyPath().toString())+"："+v.getMessage()));
    if(!failures.isEmpty())throw new IllegalArgumentException(String.join("；",failures));validate(req);
    if(!seen.add(key(req.getDimension(),req.getDimensionKey())))throw new IllegalArgumentException("范围类型/编号在文件中重复");line.setError("");
   }catch(cn.iocoder.yudao.framework.common.exception.ServiceException e){line.setError(e.getCode().equals(PAYROLL_VERSION_CONFLICT.getCode())?"接续批准版本已变化，请重新导出模板核对":"范围/来源/金额未就绪，请确认至少填写预算、营收或编制之一");}
   catch(IllegalArgumentException e){line.setError(e instanceof NumberFormatException?"接续批准版本编号须填写整数":e.getMessage());}
   out.add(line);
  }
  return out;
 }
 @Transactional(rollbackFor=Exception.class,isolation=Isolation.READ_COMMITTED)
 public List<Long> importRows(Long entity,Integer year,Integer month,List<HrmPayrollFinancialImportRowVO> rows,String requestKey,String source,Long actor){
  if(requestKey==null||requestKey.trim().isEmpty()||requestKey.length()>100||source==null||source.length()>200)throw exception(PAYROLL_FINANCIAL_INVALID);
  period(year,month);entities.check(entity,"MANAGE",actor);entities.lockApproved(entity);
  if(rows==null||rows.isEmpty()||rows.size()>5000)throw exception(PAYROLL_FINANCIAL_INVALID);
  // Full replay is checked by each immutable request; new files still validate all rows atomically.
  Set<String> seen=new HashSet<>();List<Long> out=new ArrayList<>();
  for(HrmPayrollFinancialImportRowVO item:rows){HrmPayrollFinancialPlanReqVO request;try{request=row(entity,year,month,item,requestKey,source);}catch(RuntimeException e){throw exception(PAYROLL_FINANCIAL_INVALID);}if(!seen.add(key(request.getDimension(),request.getDimensionKey())))throw exception(PAYROLL_FINANCIAL_INVALID);out.add(create(request,actor));}return out;
 }
 public List<HrmPayrollFinancialImportRowVO> template(Long entity,Integer year,Integer month,Long actor){
  period(year,month);entities.check(entity,"EXPORT",actor);List<HrmPayrollFinancialImportRowVO> out=new ArrayList<>();
  for(HrmPayrollFinancialPlanDO plan:list(entity,year,month,actor))if("APPROVED".equals(plan.getStatus()))out.add(new HrmPayrollFinancialImportRowVO().setYear(year.toString()).setMonth(month.toString()).setDimension(plan.getDimension()).setDimensionKey(plan.getDimensionKey()).setPriorPlanId(plan.getId().toString()).setBudget(plan.getBudget()==null?null:plan.getBudget().toPlainString()).setRevenue(plan.getRevenue()==null?null:plan.getRevenue().toPlainString()).setHeadcount(plan.getHeadcount()==null?null:plan.getHeadcount().stripTrailingZeros().toPlainString()).setEvidence(plan.getEvidence()));
  if(out.isEmpty())out.add(new HrmPayrollFinancialImportRowVO().setYear(year.toString()).setMonth(month.toString()).setDimension("ENTITY").setDimensionKey("ALL"));return out;
 }
 static BigDecimal ratio(BigDecimal numerator,BigDecimal denominator,boolean percent){if(numerator==null||denominator==null||denominator.signum()<=0)return null;return numerator.multiply(percent?new BigDecimal("100"):BigDecimal.ONE).divide(denominator,percent?4:2,RoundingMode.HALF_UP);}
 private void add(Map<String,HrmPayrollCostAnalysisRowVO> groups,Map<String,Set<Long>> people,String dimension,String dimensionKey,String name,HrmPayrollCostRowVO row) {
  String key=key(dimension,dimensionKey);HrmPayrollCostAnalysisRowVO target=groups.computeIfAbsent(key,k->new HrmPayrollCostAnalysisRowVO().setDimension(dimension).setDimensionKey(dimensionKey).setName(name).setActualCost(BigDecimal.ZERO));target.setActualCost(target.getActualCost().add(row.getTotal()));people.computeIfAbsent(key,k->new HashSet<>()).add(row.getEmployeeId());
 }
 @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
 public Analysis analyze(Long entity,Integer year,Integer month,Long actor) {
  period(year,month);entities.check(entity,"READ",actor);String entityName=entities.name(entity);List<HrmSalaryMonthRecordDO> periodBatches=batches.selectList(new LambdaQueryWrapperX<HrmSalaryMonthRecordDO>().eq(HrmSalaryMonthRecordDO::getEntityId,entity).eq(HrmSalaryMonthRecordDO::getYear,year).eq(HrmSalaryMonthRecordDO::getMonth,month).in(HrmSalaryMonthRecordDO::getStatus,15,16,17,10));
  Map<String,HrmPayrollCostAnalysisRowVO> groups=new LinkedHashMap<>();Map<String,Set<Long>> people=new HashMap<>();List<Long> missing=new ArrayList<>(),reportIds=new ArrayList<>();
  for(HrmSalaryMonthRecordDO batch:periodBatches){scope.check(batch,"READ",actor);HrmPayrollCostReportDO report=costs.approvedReport(batch.getId(),actor);if(report==null){missing.add(batch.getId());continue;}reportIds.add(report.getId());
   for(HrmPayrollCostRowVO row:JsonUtils.parseArray(report.getResultSnapshot(),HrmPayrollCostRowVO.class)){
    add(groups,people,"ENTITY","ALL",entityName,row);add(groups,people,"DEPARTMENT",row.getDeptId().toString(),row.getDeptName(),row);
    String center=row.getCostCenter()==null||row.getCostCenter().trim().isEmpty()?"UNASSIGNED":row.getCostCenter().trim().toUpperCase(Locale.ROOT);String project=row.getProjectCode()==null||row.getProjectCode().trim().isEmpty()?"UNASSIGNED":row.getProjectCode().trim().toUpperCase(Locale.ROOT);String category=row.getEmployeeCategory()==null||row.getEmployeeCategory().trim().isEmpty()?"UNSPECIFIED":row.getEmployeeCategory().trim().toUpperCase(Locale.ROOT);
    add(groups,people,"COST_CENTER",center,"UNASSIGNED".equals(center)?"未配置成本中心":center,row);add(groups,people,"PROJECT",project,"UNASSIGNED".equals(project)?"未配置项目":project,row);add(groups,people,"CATEGORY",category,"UNSPECIFIED".equals(category)?"人员类别未明确":category,row);
   }
  }
  boolean ready=!periodBatches.isEmpty()&&missing.isEmpty();
  for(HrmPayrollFinancialPlanDO plan:list(entity,year,month,actor))if("APPROVED".equals(plan.getStatus())){
   String key=key(plan.getDimension(),plan.getDimensionKey());HrmPayrollCostAnalysisRowVO row=groups.computeIfAbsent(key,k->new HrmPayrollCostAnalysisRowVO().setDimension(plan.getDimension()).setDimensionKey(plan.getDimensionKey()).setName(plan.getDimensionKey()).setActualCost(BigDecimal.ZERO));
   row.setBudget(plan.getBudget()).setRevenue(plan.getRevenue()).setHeadcount(plan.getHeadcount()).setPlanId(plan.getId()).setPlanVersion(plan.getVersion());
  }
  if(groups.isEmpty())groups.put(key("ENTITY","ALL"),new HrmPayrollCostAnalysisRowVO().setDimension("ENTITY").setDimensionKey("ALL").setName(entityName));
  for(Map.Entry<String,HrmPayrollCostAnalysisRowVO> entry:groups.entrySet()){
   HrmPayrollCostAnalysisRowVO row=entry.getValue();row.setEntityId(entity).setEntityName(entityName).setYear(year).setMonth(month).setCostReportIds(reportIds.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(",")));List<String> issues=new ArrayList<>();if(!ready){row.setActualCost(null);row.setEmployees(null);issues.add("冻结工资/批准成本报表未齐");}else{row.setEmployees(people.getOrDefault(entry.getKey(),Collections.emptySet()).size());row.setCostPerEmployee(ratio(row.getActualCost(),BigDecimal.valueOf(row.getEmployees()),false));if(row.getEmployees()==0)issues.add("本范围无实际计薪人员");}
   if(row.getRevenue()==null)issues.add("未提供营收");else if(row.getRevenue().signum()==0)issues.add("已确认营收为零");
   if(row.getBudget()==null)issues.add("未提供预算");else if(row.getBudget().signum()==0)issues.add("已确认预算为零");
   if(row.getHeadcount()==null)issues.add("未确认编制");else if(row.getHeadcount().signum()==0)issues.add("已确认编制为零");
   row.setBudgetVariance(row.getActualCost()==null||row.getBudget()==null?null:row.getActualCost().subtract(row.getBudget())).setBudgetVarianceRate(ratio(row.getBudgetVariance(),row.getBudget(),true))
    .setCostRevenueRate(ratio(row.getActualCost(),row.getRevenue(),true)).setCostPerPosition(ratio(row.getActualCost(),row.getHeadcount(),false)).setStaffingVariance(row.getEmployees()==null||row.getHeadcount()==null?null:BigDecimal.valueOf(row.getEmployees()).subtract(row.getHeadcount())).setSourceStatus(issues.isEmpty()?"资料齐备":String.join("；",issues));
  }
  return new Analysis().setReady(ready).setMissingBatchIds(missing).setReportIds(reportIds).setRows(new ArrayList<>(groups.values())).setSourceStatus(periodBatches.isEmpty()?"本期间无可用冻结工资":missing.isEmpty()?"已批准冻结成本口径":"成本报表未齐，请先编制并复核缺失批次");
 }
 @Transactional(rollbackFor=Exception.class,isolation=Isolation.REPEATABLE_READ)
 public List<HrmPayrollCostAnalysisRowVO> export(Long entity,Integer year,Integer month,String dimension,Long actor) {
  if(!Arrays.asList("ENTITY","DEPARTMENT","COST_CENTER","PROJECT","CATEGORY").contains(dimension))throw exception(PAYROLL_FINANCIAL_INVALID);
  entities.check(entity,"EXPORT",actor);Analysis result=analyze(entity,year,month,actor);if(!result.isReady())throw exception(PAYROLL_FINANCIAL_INVALID);
  for(HrmSalaryMonthRecordDO b:batches.selectList(new LambdaQueryWrapperX<HrmSalaryMonthRecordDO>().eq(HrmSalaryMonthRecordDO::getEntityId,entity).eq(HrmSalaryMonthRecordDO::getYear,year).eq(HrmSalaryMonthRecordDO::getMonth,month).in(HrmSalaryMonthRecordDO::getStatus,15,16,17,10)))scope.check(b,"EXPORT",actor);
  result.setRows(result.getRows().stream().filter(r->dimension.equals(r.getDimension())).collect(java.util.stream.Collectors.toList()));
  event(entity,year,month,null,"EXPORT","导出批准预算与实际成本分析："+dimension,result,actor);return result.getRows();
 }
}
