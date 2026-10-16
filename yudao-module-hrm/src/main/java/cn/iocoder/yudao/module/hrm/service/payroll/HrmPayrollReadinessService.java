package cn.iocoder.yudao.module.hrm.service.payroll;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.salary.monthrecord.HrmSalaryMonthRecordDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.salary.config.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.employee.info.HrmEmployeeMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.HrmPayrollInputMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.salary.monthrecord.HrmSalaryMonthRecordMapper;
import cn.iocoder.yudao.module.hrm.service.salary.config.*;
import cn.iocoder.yudao.module.hrm.service.salary.employee.HrmSalaryEmployeeInfoService;
import cn.iocoder.yudao.module.hrm.enums.salary.config.HrmSalarySocialSecurityMonthTypeEnum;
import cn.iocoder.yudao.module.hrm.enums.salary.config.HrmSalaryTaxTypeEnum;
import cn.iocoder.yudao.module.hrm.enums.employee.info.HrmEmployeeEntryStatusEnum;
import java.time.*;
import java.util.*;
import javax.annotation.Resource;
import lombok.Data;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
/** Readiness contains identifiers and opaque evidence hashes, never salary/tax/bank values. */
@Service public class HrmPayrollReadinessService {
 public static final List<String> KINDS=Collections.unmodifiableList(Arrays.asList("PAYMENT_PLAN","SALARY_GROUP","SALARY_INFO","INSURANCE","TAX_POLICY","TAX_PROFILE","ATTENDANCE","OVERTIME","DEDUCTIONS","CUMULATIVE_TAX"));
 public static String label(String kind) {
  switch(kind){case "PAYMENT_PLAN":return "发放日期";case "SALARY_GROUP":return "薪资组/计税配置";case "SALARY_INFO":return "生效薪资";case "INSURANCE":return "社保公积金";case "TAX_POLICY":return "年度税务政策";case "TAX_PROFILE":return "年度税务资料";case "ATTENDANCE":return "考勤";case "OVERTIME":return "工时与加班";case "DEDUCTIONS":return "累计专项扣除";case "CUMULATIVE_TAX":return "已发税务累计";default:return "来源核实";}
 }
 @Resource private HrmSalaryMonthRecordMapper batches;
 @Resource private HrmEmployeeMapper employees;
 @Resource private HrmPayrollInputMapper inputMapper;
 @Resource private HrmPayrollScopeService scope;
 @Resource private HrmPayrollInputService inputs;
 @Resource private HrmSalaryGroupService groups;
 @Resource private HrmSalaryTaxRuleService taxRules;
 @Resource private HrmSalaryConfigService config;
 @Resource private HrmSalaryEmployeeInfoService salaries;
 @Resource private HrmPayrollInsurancePolicyService insurance;
 @Resource private HrmPayrollTaxPolicyService policies;
 @Resource private HrmPayrollTaxProfileService profiles;
 @Data public static class Signal {
  private Long batchId,employeeId,sourceId;
  private String employeeName,jobNumber,kind,status,reason,proofHash;
  private boolean ready;
 }
 private String hash(Object value){return cn.hutool.crypto.digest.DigestUtil.sha256Hex(JsonUtils.toJsonString(value));}
 private HrmSalaryGroupDO group(HrmEmployeeDO employee){return groups.getEmployeeSalaryGroupMap(Collections.singletonList(employee)).get(employee.getId());}
 private HrmSalaryTaxRuleDO taxRule(HrmSalaryGroupDO group){return group==null||group.getTaxRuleId()==null?null:taxRules.getSalaryTaxRuleMap(Collections.singleton(group.getTaxRuleId())).get(group.getTaxRuleId());}
 @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
 public Signal inspect(Long batchId,Long employeeId,String kind,Long actor) {
  HrmSalaryMonthRecordDO b=batches.selectById(batchId);scope.check(b,"READ",actor);
  if(b.getEntityId()==null||!"REGULAR".equals(b.getBatchKey())||!KINDS.contains(kind)||!scope.payrollDates(b).containsKey(employeeId))throw exception(PAYROLL_INPUT_ISSUE_INVALID);
  HrmEmployeeDO employee=employees.selectById(employeeId);
  if(employee==null||!(HrmEmployeeEntryStatusEnum.ACTIVE_STATUSES.contains(employee.getEntryStatus())||Objects.equals(employee.getEntryStatus(),HrmEmployeeEntryStatusEnum.LEFT.getStatus())&&employee.getLeaveTime()!=null&&!employee.getLeaveTime().toLocalDate().isBefore(b.getStartTime().toLocalDate())))throw exception(PAYROLL_INPUT_ISSUE_INVALID);
  Signal signal=new Signal().setBatchId(batchId).setEmployeeId(employeeId).setEmployeeName(employee.getName()).setJobNumber(employee.getJobNumber()).setKind(kind).setStatus("MISSING").setReady(false).setReason("来源或批准配置尚未就绪");
  Map<String,Object> evidence=new LinkedHashMap<>();evidence.put("batchId",batchId);evidence.put("employeeId",employeeId);evidence.put("kind",kind);evidence.put("start",b.getStartTime());evidence.put("end",b.getEndTime());
  try {
   if(HrmPayrollInputService.KINDS.contains(kind)) {
    HrmPayrollInputService.Readiness current=inputs.readiness(batchId,employeeId,kind,actor);signal.setSourceId(current.getInputId()).setStatus(current.getStatus()).setReady("APPROVED".equals(current.getStatus()));
    HrmPayrollInputDO row=current.getInputId()==null?null:inputMapper.selectById(current.getInputId());
    if(current.getInputId()!=null&&row==null)signal.setReady(false).setStatus("STALE");
    evidence.put("inputId",current.getInputId());evidence.put("inputHash",row==null?null:hash(Arrays.asList(row.getPayload(),row.getSourceSnapshot(),row.getStatus(),row.getVersion())));
    signal.setReason(signal.isReady()?"已独立复核且来源一致":"来源缺失、待复核或已变更，须补正并独立复核");
   } else if("PAYMENT_PLAN".equals(kind)) {
    signal.setReady(b.getExpectedPaymentDate()!=null).setReason("计划发放日期须明确确认");evidence.put("expectedPaymentDate",b.getExpectedPaymentDate());
   } else if("SALARY_GROUP".equals(kind)) {
    HrmSalaryGroupDO group=group(employee);HrmSalaryTaxRuleDO rule=taxRule(group);signal.setSourceId(group==null?null:group.getId()).setReady(group!=null&&rule!=null&&rule.getTaxEnabled()!=null&&(Boolean.FALSE.equals(rule.getTaxEnabled())||HrmSalaryTaxTypeEnum.valueOf(rule.getType())!=null));evidence.put("groupHash",hash(group));evidence.put("taxRuleHash",hash(rule));
   } else if("SALARY_INFO".equals(kind)) {
    Object values=salaries.getEffectiveSalaryOptionList(employee,cn.iocoder.yudao.framework.common.util.date.LocalDateTimeUtils.getDateTimeRange(b.getStartTime().toLocalDate(),b.getEndTime().toLocalDate()),scope.payrollDates(b).get(employeeId));
    signal.setReady(values instanceof Collection&&!((Collection<?>)values).isEmpty());evidence.put("effectiveSalaryHash",hash(values));
   } else if("INSURANCE".equals(kind)) {
    HrmSalaryConfigDO settings=config.getSalaryConfig();YearMonth month=YearMonth.of(b.getYear(),b.getMonth());Integer offset=settings==null?null:settings.getSocialSecurityMonthType();
    if(Objects.equals(offset,HrmSalarySocialSecurityMonthTypeEnum.PREVIOUS_MONTH.getType()))month=month.minusMonths(1);else if(Objects.equals(offset,HrmSalarySocialSecurityMonthTypeEnum.NEXT_MONTH.getType()))month=month.plusMonths(1);
    HrmPayrollInsurancePolicyService.Quote quote=insurance.preview(b.getEntityId(),Collections.singleton(employeeId),month,actor).get(employeeId);
    signal.setSourceId(quote==null?null:quote.getBaseId()).setReady(quote!=null&&Arrays.asList("INSURED","EXEMPT").contains(quote.getCoverage()));evidence.put("contributionMonth",month.toString());evidence.put("quoteHash",hash(quote));
   } else {
    HrmSalaryTaxRuleDO rule=taxRule(group(employee));evidence.put("taxRuleHash",hash(rule));
    if(rule==null||rule.getTaxEnabled()==null)throw exception(PAYROLL_TAX_CONFIGURATION_MISSING,employee.getJobNumber());
    if(Boolean.TRUE.equals(rule.getTaxEnabled())&&(rule.getType()==null||!Arrays.asList(HrmSalaryTaxTypeEnum.SALARY.getType(),HrmSalaryTaxTypeEnum.REMUNERATION.getType(),HrmSalaryTaxTypeEnum.NONE.getType()).contains(rule.getType())))throw exception(PAYROLL_TAX_INVALID);
    if(Boolean.FALSE.equals(rule.getTaxEnabled())||rule.getType()!=null&&!Objects.equals(rule.getType(),HrmSalaryTaxTypeEnum.SALARY.getType())){signal.setReady(true).setStatus("NOT_APPLICABLE").setReason("该计税方式不使用居民年度累计来源，保留适用配置依据");}
    else {
     if(b.getExpectedPaymentDate()==null)throw exception(PAYROLL_TAX_PAYMENT_DATE_MISSING);
     int year=b.getExpectedPaymentDate().getYear();evidence.put("cashYear",year);
     if("TAX_POLICY".equals(kind)){HrmPayrollTaxPolicyDO policy=policies.requireApproved(b.getEntityId(),year);signal.setReady(true).setSourceId(policy.getId());evidence.put("policyHash",hash(policy));}
     else {HrmPayrollTaxProfileDO profile=profiles.requireApproved(b.getEntityId(),employeeId,year);signal.setReady(Boolean.TRUE.equals(profile.getResidentThisYear())).setSourceId(profile.getId());evidence.put("profileHash",hash(profile));}
    }
   }
   if(signal.isReady()&&!"NOT_APPLICABLE".equals(signal.getStatus()))signal.setStatus("READY").setReason("当前适用来源已核实");
  } catch(cn.iocoder.yudao.framework.common.exception.ServiceException invalid) {
   signal.setReady(false).setStatus("MISSING").setReason("来源缺失、审批未完成或适用配置不符");evidence.put("failureCode",invalid.getCode());
  }
  evidence.put("status",signal.getStatus());signal.setProofHash(hash(evidence));return signal;
 }
}
