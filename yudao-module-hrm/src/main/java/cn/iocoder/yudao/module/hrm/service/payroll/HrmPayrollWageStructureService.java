package cn.iocoder.yudao.module.hrm.service.payroll;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.salary.monthrecord.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.salary.config.HrmSalaryOptionDO;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.salary.monthrecord.HrmSalaryMonthRecordMapper;
import cn.iocoder.yudao.module.hrm.enums.salary.config.HrmSalaryOptionCodeEnum;
import cn.iocoder.yudao.module.hrm.enums.salary.config.HrmSalaryOptionTypeEnum;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import javax.annotation.Resource;
import lombok.Data;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
/** Signed wage components reconcile exactly to frozen gross pay; current catalog changes are irrelevant. */
@Service public class HrmPayrollWageStructureService {
 @Resource private HrmSalaryMonthRecordMapper batches;
 @Resource private HrmPayrollRunMapper runs;
 @Resource private HrmPayrollPaymentMapper payments;
 @Resource private HrmPayrollFinancialEventMapper events;
 @Resource private HrmPayrollEntityService entities;
 @Resource private HrmPayrollScopeService scope;
 @Data public static class Result {
  private Long entityId,deptId;
  private Integer year,month;
  private String employeeCategory;
  private boolean ready,previousReady;
  private Integer employees;
  private BigDecimal gross,previousGross,delta,deltaRate;
  private List<Long> runIds,previousRunIds;
  private String sourceStatus;
  private List<HrmPayrollWageStructureRowVO> rows;
 }
 private static class Period {
  boolean ready;BigDecimal gross=BigDecimal.ZERO;Set<Long> employees=new HashSet<>();List<Long> runIds=new ArrayList<>();
  Map<Integer,BigDecimal> components=new TreeMap<>();Map<Integer,String> names=new HashMap<>();
 }
 private HrmPayrollRunDO latest(Long batch) {HrmPayrollRunDO r=runs.selectOne(new LambdaQueryWrapperX<HrmPayrollRunDO>().eq(HrmPayrollRunDO::getMonthRecordId,batch).orderByDesc(HrmPayrollRunDO::getVersion).last("LIMIT 1"));if(r==null)throw exception(PAYROLL_COST_INVALID);return r;}
 private HrmPayrollRunDO root(HrmPayrollRunDO current) {
  Set<Long> seen=new HashSet<>();HrmPayrollRunDO row=current;
  while(true){if(row==null||!seen.add(row.getId()))throw exception(PAYROLL_VERSION_CONFLICT);Object parent=JsonUtils.parseObject(row.getInputSnapshot(),Map.class).get("sourceRunId");if(parent==null)return row;if(!(parent instanceof Number))throw exception(PAYROLL_VERSION_CONFLICT);HrmPayrollRunDO previous=runs.selectById(((Number)parent).longValue());if(previous==null||!Objects.equals(previous.getMonthRecordId(),current.getMonthRecordId())||previous.getVersion()>=row.getVersion())throw exception(PAYROLL_VERSION_CONFLICT);row=previous;}
 }
 private LocalDate lastDate(HrmSalaryMonthRecordDO b,Map<String,Object> input,Long employee,Set<Long> visited) {
  if(!visited.add(b.getId()))throw exception(PAYROLL_VERSION_CONFLICT);
  Object dates=input.get("entityPayrollDates");if(dates instanceof Map){Object values=((Map<?,?>)dates).get(employee.toString());if(values instanceof List){List<LocalDate> all=JsonUtils.parseArray(JsonUtils.toJsonString(values),LocalDate.class);if(!all.isEmpty())return Collections.max(all);}}
  if(HrmPayrollSupplementService.isSupplement(b)&&input.get("retroSources") instanceof List){LocalDate latest=null;for(HrmPayrollRetroDO r:JsonUtils.parseArray(JsonUtils.toJsonString(input.get("retroSources")),HrmPayrollRetroDO.class))if(Objects.equals(r.getEmployeeId(),employee)) {
    HrmPayrollPaymentDO payment=payments.selectById(r.getPaymentId());if(payment==null)throw exception(PAYROLL_COST_INVALID);HrmSalaryMonthRecordDO parent=batches.selectById(payment.getMonthRecordId());HrmPayrollRunDO original=runs.selectById(payment.getRunId());if(parent==null||original==null||!Objects.equals(original.getMonthRecordId(),parent.getId()))throw exception(PAYROLL_COST_INVALID);
    LocalDate day=lastDate(parent,JsonUtils.parseObject(root(original).getInputSnapshot(),Map.class),employee,new HashSet<>(visited));if(latest==null||day.isAfter(latest))latest=day;
   }if(latest!=null)return latest;}
  throw exception(PAYROLL_COST_INVALID);
 }
 private Period period(Long entity,YearMonth period,Long dept,String category,Long actor) {
  Period out=new Period();List<HrmSalaryMonthRecordDO> source=batches.selectList(new LambdaQueryWrapperX<HrmSalaryMonthRecordDO>().eq(HrmSalaryMonthRecordDO::getEntityId,entity).eq(HrmSalaryMonthRecordDO::getYear,period.getYear()).eq(HrmSalaryMonthRecordDO::getMonth,period.getMonthValue()).in(HrmSalaryMonthRecordDO::getStatus,15,16,17,10));
  out.ready=!source.isEmpty();
  for(HrmSalaryMonthRecordDO b:source){scope.check(b,"READ",actor);HrmPayrollRunDO run=latest(b.getId()),root=root(run);out.runIds.add(run.getId());Map<String,Object> input=JsonUtils.parseObject(root.getInputSnapshot(),Map.class),rules=JsonUtils.parseObject(root.getRuleSnapshot(),Map.class);
   Object subject=input.get("entityScope");if(!(subject instanceof Map))throw exception(PAYROLL_COST_INVALID);
   List<HrmPayrollEntityMembershipDO> memberships=JsonUtils.parseArray(JsonUtils.toJsonString(((Map<?,?>)subject).get("memberships")),HrmPayrollEntityMembershipDO.class);if(memberships==null)throw exception(PAYROLL_COST_INVALID);
   List<HrmSalaryOptionDO> options=rules.get("options") instanceof List?JsonUtils.parseArray(JsonUtils.toJsonString(rules.get("options")),HrmSalaryOptionDO.class):HrmPayrollSupplementService.isSupplement(b)&&rules.get("option")!=null?Collections.singletonList(JsonUtils.parseObject(JsonUtils.toJsonString(rules.get("option")),HrmSalaryOptionDO.class)):null;
   if(options==null)throw exception(PAYROLL_COST_INVALID);Map<Integer,HrmSalaryOptionDO> catalog=new HashMap<>();for(HrmSalaryOptionDO o:options)if(catalog.put(o.getCode(),o)!=null)throw exception(PAYROLL_COST_INVALID);
   Set<Long> seen=new HashSet<>();
   for(HrmSalaryMonthEmployeeRecordDO salary:JsonUtils.parseArray(run.getResultSnapshot(),HrmSalaryMonthEmployeeRecordDO.class)) {
    if(salary.getEmployeeId()==null||!seen.add(salary.getEmployeeId())||salary.getExpectedPaySalary()==null||salary.getOptionValues()==null)throw exception(PAYROLL_COST_INVALID);
    LocalDate last=lastDate(b,input,salary.getEmployeeId(),new HashSet<>());
    List<HrmPayrollEntityMembershipDO> matched=new ArrayList<>();for(HrmPayrollEntityMembershipDO m:memberships)if(Objects.equals(m.getEmployeeId(),salary.getEmployeeId())&&Objects.equals(m.getEntityId(),entity)&&!m.getEffectiveFrom().isAfter(last)&&!m.getEffectiveTo().isBefore(last))matched.add(m);
    if(matched.size()!=1)throw exception(PAYROLL_COST_INVALID);HrmPayrollEntityMembershipDO membership=matched.get(0);
    if(dept!=null&&!Objects.equals(dept,membership.getDeptId()))continue;
    String employeeCategory=membership.getEmployeeCategory()==null?"UNSPECIFIED":membership.getEmployeeCategory().trim();
    if(category!=null&&!category.equalsIgnoreCase(employeeCategory))continue;
    Map<Integer,BigDecimal> components=new TreeMap<>();BigDecimal gross=BigDecimal.ZERO;Set<Integer> codes=new HashSet<>();
    for(HrmSalaryMonthEmployeeRecordDO.OptionValue value:salary.getOptionValues()) {
     if(value.getCode()==null||value.getValue()==null||!codes.add(value.getCode()))throw exception(PAYROLL_COST_INVALID);
     HrmSalaryOptionDO option=catalog.get(value.getCode());if(option==null||!HrmSalaryOptionCodeEnum.EXPECTED_PAY_PARENT_CODES.contains(option.getParentCode()))continue;
     // Use the same type contract as payroll calculation: MINUS=0, ADD=1; CALCULATED=2 is not a component.
     if(!Objects.equals(option.getType(),HrmSalaryOptionTypeEnum.ADD.getType())&&!Objects.equals(option.getType(),HrmSalaryOptionTypeEnum.MINUS.getType()))throw exception(PAYROLL_COST_INVALID);
     BigDecimal signed=Objects.equals(option.getType(),HrmSalaryOptionTypeEnum.MINUS.getType())?value.getValue().negate():value.getValue();components.put(value.getCode(),signed);gross=gross.add(signed);out.names.put(value.getCode(),option.getName());
    }
    if(gross.compareTo(salary.getExpectedPaySalary())!=0)throw exception(PAYROLL_COST_INVALID);
    out.gross=out.gross.add(gross);out.employees.add(salary.getEmployeeId());components.forEach((code,amount)->out.components.merge(code,amount,BigDecimal::add));
   }
  }
  return out;
 }
 @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
 public Result analyze(Long entity,Integer year,Integer month,Long dept,String category,Long actor) {
  if(year==null||month==null||year<2019||year>2099||month<1||month>12||dept!=null&&dept<=0||category!=null&&category.length()>100)throw exception(PAYROLL_FINANCIAL_INVALID);
  entities.check(entity,"READ",actor);if(category!=null){category=category.trim();if(category.isEmpty())category=null;}
  YearMonth selected=YearMonth.of(year,month);Period current=period(entity,selected,dept,category,actor),previous=period(entity,selected.minusMonths(1),dept,category,actor);
  Set<Integer> codes=new TreeSet<>(current.components.keySet());codes.addAll(previous.components.keySet());List<HrmPayrollWageStructureRowVO> rows=new ArrayList<>();String name=entities.name(entity);
  for(Integer code:codes) {
   BigDecimal amount=current.ready?current.components.getOrDefault(code,BigDecimal.ZERO):null,before=previous.ready?previous.components.getOrDefault(code,BigDecimal.ZERO):null,delta=amount==null||before==null?null:amount.subtract(before);
   rows.add(new HrmPayrollWageStructureRowVO().setEntityId(entity).setEntityName(name).setYear(year).setMonth(month).setDeptId(dept).setEmployeeCategory(category).setCode(code).setName(current.names.getOrDefault(code,previous.names.get(code))).setPreviousName(previous.names.get(code))
    .setAmount(amount).setShare(HrmPayrollFinancialService.ratio(amount,current.ready?current.gross:null,true)).setPrevious(before).setDelta(delta).setDeltaRate(HrmPayrollFinancialService.ratio(delta,before,true))
    .setRunIds(join(current.runIds)).setPreviousRunIds(join(previous.runIds)).setSourceStatus(!current.ready?"本期无可用冻结来源":!previous.ready?"上期无可用冻结来源":before.signum()<=0?"上期该项非正，变化率不可计算":current.names.containsKey(code)&&previous.names.containsKey(code)&&!Objects.equals(current.names.get(code),previous.names.get(code))?"工资项名称变化，请核对前后口径":"冻结来源可比"));
  }
  BigDecimal delta=current.ready&&previous.ready?current.gross.subtract(previous.gross):null;
  return new Result().setEntityId(entity).setYear(year).setMonth(month).setDeptId(dept).setEmployeeCategory(category).setReady(current.ready).setPreviousReady(previous.ready).setEmployees(current.ready?current.employees.size():null).setGross(current.ready?current.gross:null).setPreviousGross(previous.ready?previous.gross:null).setDelta(delta).setDeltaRate(HrmPayrollFinancialService.ratio(delta,previous.ready?previous.gross:null,true))
    .setRunIds(current.runIds).setPreviousRunIds(previous.runIds).setRows(rows).setSourceStatus(!current.ready?"本期无可用冻结工资；不能把缺失期间当作零":current.employees.isEmpty()?"所选历史归属范围内无计薪人员":current.gross.signum()<=0?"本期应发非正，结构占比不可计算；已保留实际金额":"工资项已逐员工核对到冻结应发；扣减为负数，比例以应发为分母");
 }
 private String join(List<Long> ids){return ids.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));}
 @Transactional(rollbackFor=Exception.class,isolation=Isolation.REPEATABLE_READ)
 public List<HrmPayrollWageStructureRowVO> export(Long entity,Integer year,Integer month,Long dept,String category,Long actor) {
  entities.check(entity,"EXPORT",actor);Result result=analyze(entity,year,month,dept,category,actor);if(!result.isReady())throw exception(PAYROLL_FINANCIAL_INVALID);
  Set<Long> runIds=new HashSet<>(result.getRunIds());runIds.addAll(result.getPreviousRunIds());for(Long id:runIds){HrmPayrollRunDO run=runs.selectById(id);scope.check(batches.selectById(run.getMonthRecordId()),"EXPORT",actor);}
  events.insert(new HrmPayrollFinancialEventDO().setEntityId(entity).setYear(year).setMonth(month).setAction("EXPORT_WAGE_STRUCTURE").setActorId(actor).setReason("按历史归属导出冻结工资结构及环比").setSnapshot(JsonUtils.toJsonString(result)));return result.getRows();
 }
}
