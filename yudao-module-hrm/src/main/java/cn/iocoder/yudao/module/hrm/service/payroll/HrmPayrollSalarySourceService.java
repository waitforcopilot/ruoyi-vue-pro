package cn.iocoder.yudao.module.hrm.service.payroll;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.date.LocalDateTimeUtils;
import cn.iocoder.yudao.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.salary.config.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.salary.monthrecord.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.HrmPayrollRunDO;
import cn.iocoder.yudao.module.hrm.dal.mysql.employee.info.HrmEmployeeMapper;
import cn.iocoder.yudao.module.hrm.service.salary.config.*;
import cn.iocoder.yudao.module.hrm.service.salary.employee.HrmSalaryEmployeeInfoService;
import java.math.BigDecimal;
import java.util.*;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;
/** Verifies the actual raw effective salary and configuration used by a trial, before reservation. */
@Service public class HrmPayrollSalarySourceService {
 @Resource private HrmEmployeeMapper employees;
 @Resource private HrmPayrollScopeService scope;
 @Resource private HrmSalaryEmployeeInfoService salaries;
 @Resource private HrmSalaryGroupService groups;
 @Resource private HrmSalaryTaxRuleService taxRules;
 @Resource private HrmSalaryOptionService options;
 @Resource private HrmSalaryConfigService config;
 @Resource private HrmPayrollFormulaService formulas;
 private Object canonical(Object value){
  if(value instanceof Map){Map<String,Object> sorted=new TreeMap<>();((Map<?,?>)value).forEach((k,v)->sorted.put(k.toString(),canonical(v)));return sorted;}
  if(value instanceof Collection){List<Object> sorted=new ArrayList<>();for(Object v:(Collection<?>)value)sorted.add(canonical(v));sorted.sort(Comparator.comparing(JsonUtils::toJsonString));return sorted;}
  if(value instanceof Number)return new BigDecimal(value.toString()).stripTrailingZeros().toPlainString();return value;
 }
 public String digest(Object raw,Object group,Object rules,Object catalog,Object formula,Object settings) {
  List<Object> values=Arrays.asList(raw,group,rules,catalog,formula,settings);Object parsed=JsonUtils.parseObject(JsonUtils.toJsonString(Collections.singletonMap("proof",values)),Map.class);
  return cn.hutool.crypto.digest.DigestUtil.sha256Hex(JsonUtils.toJsonString(canonical(parsed)));
 }
 public void validate(HrmSalaryMonthRecordDO batch,HrmPayrollRunDO run) {
  if(batch.getEntityId()==null||HrmPayrollSupplementService.isSupplement(batch))return;
  Map<String,Object> input=JsonUtils.parseObject(run.getInputSnapshot(),Map.class);if(input.get("sourceRunId")!=null)return;
  if(!(input.get("effectiveSalaryValues") instanceof Map)||!(input.get("salarySourceDigest") instanceof String))throw exception(PAYROLL_SALARY_SOURCE_STALE);
  Map<String,Object> savedRules=JsonUtils.parseObject(run.getRuleSnapshot(),Map.class);
  if(savedRules==null||!Objects.equals(input.get("salarySourceDigest"),digest(input.get("effectiveSalaryValues"),savedRules.get("salaryGroups"),savedRules.get("taxRules"),savedRules.get("options"),savedRules.get("formulas"),input.get("salarySourceConfig"))))throw exception(PAYROLL_SALARY_SOURCE_STALE);
  Map<Long,Set<java.time.LocalDate>> dates=scope.payrollDates(batch);Map<Long,Object> raw=new LinkedHashMap<>();List<HrmEmployeeDO> people=new ArrayList<>();
  for(HrmSalaryMonthEmployeeRecordDO row:JsonUtils.parseArray(run.getResultSnapshot(),HrmSalaryMonthEmployeeRecordDO.class)) {
   HrmEmployeeDO employee=employees.selectById(row.getEmployeeId());if(employee==null||!dates.containsKey(employee.getId()))throw exception(PAYROLL_SALARY_SOURCE_STALE);people.add(employee);
   raw.put(employee.getId(),salaries.getEffectiveSalaryOptionList(employee,LocalDateTimeUtils.getDateTimeRange(batch.getStartTime().toLocalDate(),batch.getEndTime().toLocalDate()),dates.get(employee.getId())));
  }
  Map<Long,HrmSalaryGroupDO> assigned=groups.getEmployeeSalaryGroupMap(people);Set<Long> ruleIds=new HashSet<>();assigned.values().forEach(g->ruleIds.add(g.getTaxRuleId()));Map<Long,HrmSalaryTaxRuleDO> configured=taxRules.getSalaryTaxRuleMap(ruleIds),byEmployee=new LinkedHashMap<>();assigned.forEach((id,g)->byEmployee.put(id,configured.get(g.getTaxRuleId())));
  String current=digest(raw,assigned,byEmployee,options.getSalaryOptionList(false),formulas.effective(batch.getStartTime().toLocalDate()),config.getSalaryConfig());
  if(!Objects.equals(input.get("salarySourceDigest"),current))throw exception(PAYROLL_SALARY_SOURCE_STALE);
 }
}
