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
import cn.iocoder.yudao.module.hrm.dal.mysql.salary.monthrecord.*;
import cn.iocoder.yudao.module.hrm.service.salary.config.HrmSalaryOptionService;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** Dedicated positive cash supplements reuse payroll approvals, tax, bank reconciliation and slips. */
@Service public class HrmPayrollSupplementService {
 @Resource private HrmSalaryMonthRecordMapper batches;
 @Resource private HrmSalaryMonthEmployeeRecordMapper employeeRecords;
 @Resource private HrmPayrollPaymentMapper payments;
 @Resource private HrmPayrollRetroMapper retros;
 @Resource private HrmPayrollRunMapper runs;
 @Resource private HrmPayrollBatchEventMapper events;
 @Resource private HrmPayrollScopeService scope;
 @Resource private HrmPayrollRetroService retro;
 @Resource private HrmPayrollTaxWithholdingService tax;
 @Resource private HrmPayrollTaxDeductionService deductions;
 @Resource private HrmPayrollBatchService batchService;
 @Resource private HrmSalaryOptionService options;
 @Resource private javax.validation.Validator validator;
 public static boolean isSupplement(HrmSalaryMonthRecordDO b) {return b!=null && b.getBatchKey()!=null && b.getBatchKey().startsWith("SUPPLEMENT:") && b.getParentBatchId()!=null;}
 @Transactional(rollbackFor=Exception.class,isolation=Isolation.READ_COMMITTED)
 public Long create(HrmPayrollSupplementReqVO request,Long actor) {
  if(!validator.validate(request).isEmpty()||request.getExpectedPaymentDate().getYear()<2019||request.getExpectedPaymentDate().getYear()>2099) throw exception(PAYROLL_RETRO_INVALID);
  HrmPayrollPaymentDO p=payments.selectById(request.getPaymentId());
  if(p==null||!"SUCCESS".equals(p.getStatus())||p.getValueDate()==null) throw exception(PAYROLL_RETRO_INVALID);
  HrmSalaryMonthRecordDO original=batches.selectByIdForUpdate(p.getMonthRecordId());
  scope.check(original,"READ",actor);scope.check(original,"CALCULATE",actor);scope.check(original,"MANAGE",actor);
  if(original.getEntityId()==null) throw exception(PAYROLL_ENTITY_INVALID);
  // Serializing on the original batch also makes independent request creation idempotent.
  String key="SUPPLEMENT:"+cn.hutool.crypto.digest.DigestUtil.sha256Hex(request.getRequestKey()).substring(0,40);
  HrmSalaryMonthRecordDO b=batches.selectOne(HrmSalaryMonthRecordDO::getBatchKey,key);
  if(b==null) {
   b=new HrmSalaryMonthRecordDO().setEntityId(original.getEntityId()).setParentBatchId(original.getId()).setBatchKey(key)
     .setTitle(original.getYear()+"年"+original.getMonth()+"月工资独立补发").setYear(original.getYear()).setMonth(original.getMonth())
     .setStartTime(original.getStartTime()).setEndTime(original.getEndTime()).setExpectedPaymentDate(request.getExpectedPaymentDate())
     .setStatus(5).setEmployeeCount(0).setExpectedPaySalary(BigDecimal.ZERO).setPersonalTax(BigDecimal.ZERO).setRealPaySalary(BigDecimal.ZERO);
   batches.insert(b);
  } else if(!Objects.equals(b.getParentBatchId(),original.getId()) || !Objects.equals(b.getExpectedPaymentDate(),request.getExpectedPaymentDate())) throw exception(PAYROLL_VERSION_CONFLICT);
  HrmPayrollRetroReqVO req=new HrmPayrollRetroReqVO().setBatchId(b.getId()).setPaymentId(request.getPaymentId()).setPriorRetroId(request.getPriorRetroId())
    .setCorrectedIncome(request.getCorrectedIncome()).setEvidence(request.getEvidence()).setRequestKey(request.getRequestKey());
  retro.create(req,actor);
  return b.getId();
 }
 private HrmSalaryMonthRecordDO require(Long id,Long actor) {
  HrmSalaryMonthRecordDO b=batches.selectByIdForUpdate(id);scope.check(b,"CALCULATE",actor);
  if(!isSupplement(b)) throw exception(PAYROLL_RETRO_INVALID);
  if(!(Objects.equals(b.getStatus(),5)||Objects.equals(b.getStatus(),11))) throw exception(SALARY_MONTH_RECORD_STATUS_INVALID);
  return b;
 }
 private HrmSalaryMonthRecordDO.OptionHeader header(int group,String name,int code,String label) {
  return HrmSalaryMonthRecordDO.OptionHeader.builder().code(group).name(name).children(Collections.singletonList(HrmSalaryMonthRecordDO.OptionHeader.builder().code(code).name(label).build())).build();
 }
 private HrmSalaryMonthEmployeeRecordDO.OptionValue value(int code,String name,BigDecimal amount) {
  return HrmSalaryMonthEmployeeRecordDO.OptionValue.builder().code(code).name(name).value(amount).build();
 }
 @Transactional(rollbackFor=Exception.class,isolation=Isolation.READ_COMMITTED)
 public Long compute(Long batchId,Long actor) {
  HrmSalaryMonthRecordDO b=require(batchId,actor);
  List<HrmPayrollRetroDO> sources=retro.sources(batchId);
  if(sources.isEmpty() || retros.selectCount(new LambdaQueryWrapperX<HrmPayrollRetroDO>().eq(HrmPayrollRetroDO::getBatchId,batchId).eq(HrmPayrollRetroDO::getStatus,"PENDING"))>0) throw exception(PAYROLL_RETRO_INVALID);
  HrmSalaryOptionDO option=options.getSalaryOptionList(false).stream().filter(o->Objects.equals(o.getCode(),10901)).findFirst().orElse(null);
  if(option==null||!Objects.equals(option.getParentCode(),10)||!Boolean.TRUE.equals(option.getTaxEnabled())||!Objects.equals(option.getType(),1)||Boolean.FALSE.equals(option.getEnabled())) throw exception(PAYROLL_RETRO_INVALID);
  Map<Long,BigDecimal> amounts=new TreeMap<>();for(HrmPayrollRetroDO row:sources) {
   if(row.getDelta().signum()<=0) throw exception(PAYROLL_RETRO_INVALID);
   amounts.merge(row.getEmployeeId(),row.getDelta(),BigDecimal::add);
  }
  List<HrmSalaryMonthEmployeeRecordDO> results=new ArrayList<>();BigDecimal gross=BigDecimal.ZERO,withheld=BigDecimal.ZERO,net=BigDecimal.ZERO;
  for(Map.Entry<Long,BigDecimal> item:amounts.entrySet()) {
   Long employee=item.getKey();BigDecimal amount=item.getValue();LocalDate date=b.getExpectedPaymentDate();
   HrmPayrollTaxDeductionDO declaration=deductions.source(b.getEntityId(),employee,date.getYear(),date.getMonthValue());
   if(declaration==null) throw exception(PAYROLL_TAX_INVALID);
   BigDecimal extra=deductions.values(declaration).values().stream().reduce(BigDecimal.ZERO,BigDecimal::add);
   HrmPayrollTaxWithholdingService.Source source=tax.calculateForBatch(batchId,b.getEntityId(),employee,date,amount,BigDecimal.ZERO,extra);
   if(source==null||source.bonusAmount().signum()!=0||source.getCurrentIncome().compareTo(amount)!=0||source.getCurrentSocialDeduction().signum()!=0) throw exception(PAYROLL_RETRO_INVALID);
   BigDecimal currentTax=source.getQuote().getCurrentWithholding();BigDecimal cash=amount.subtract(currentTax);
   BigDecimal taxable=amount.subtract(source.getQuote().getBasicDeduction().subtract(source.getPaid().getBasicDeduction())).max(BigDecimal.ZERO);
   if(cash.signum()<0) throw exception(PAYROLL_RETRO_INVALID);
   List<HrmSalaryMonthEmployeeRecordDO.OptionValue> values=new ArrayList<>();
   values.add(value(10901,"批准工资补发",amount));values.add(value(210101,"应发工资",amount));values.add(value(220101,"应税工资",taxable));
   values.add(value(230101,"个人所得税",currentTax));values.add(value(240101,"实发工资",cash));
   values.add(value(250101,"此前收入",source.getPaid().getIncome()));values.add(value(250102,"此前减除费用",source.getPaid().getBasicDeduction()));
   values.add(value(250103,"此前专项扣除",source.getPaid().getSocialDeduction()));values.add(value(250105,"此前预缴",source.getPaid().getWithheld()));
   values.add(value(270101,"累计收入",source.getPaid().getIncome().add(amount)));values.add(value(270102,"累计减除费用",source.getQuote().getBasicDeduction()));
   values.add(value(270103,"累计专项扣除",source.getPaid().getSocialDeduction()));values.add(value(270104,"累计附加扣除",extra));
   values.add(value(270105,"累计应税",source.getQuote().getSalaryTaxable()));values.add(value(270106,"累计税额",source.getQuote().getCumulativeSalaryTax()));
   for(Map.Entry<String,BigDecimal> d:source.getDeductionValues().entrySet()) values.add(value(Integer.parseInt(d.getKey()),"批准累计扣除",d.getValue()));
   HrmSalaryMonthEmployeeRecordDO row=new HrmSalaryMonthEmployeeRecordDO().setMonthRecordId(batchId).setEntityId(b.getEntityId()).setEmployeeId(employee)
     .setYear(b.getYear()).setMonth(b.getMonth()).setActualWorkDay(BigDecimal.ZERO).setNeedWorkDay(BigDecimal.ZERO)
     .setExpectedPaySalary(amount).setTaxableSalary(taxable).setPersonalTax(currentTax).setRealPaySalary(cash)
     .setTaxPaymentDate(date).setTaxSourceSnapshot(JsonUtils.toJsonString(source)).setOptionValues(values);
   results.add(row);gross=gross.add(amount);withheld=withheld.add(currentTax);net=net.add(cash);
  }
  // Draft rows may be replaced; immutable run snapshots retain every previous calculation.
  employeeRecords.delete(new LambdaQueryWrapperX<HrmSalaryMonthEmployeeRecordDO>().eq(HrmSalaryMonthEmployeeRecordDO::getMonthRecordId,batchId));
  for(HrmSalaryMonthEmployeeRecordDO row:results) employeeRecords.insert(row);
  b.setStatus(11).setEmployeeCount(results.size()).setExpectedPaySalary(gross).setPersonalTax(withheld).setRealPaySalary(net)
    .setPersonalInsuranceAmount(BigDecimal.ZERO).setPersonalProvidentFundAmount(BigDecimal.ZERO).setCorporateInsuranceAmount(BigDecimal.ZERO).setCorporateProvidentFundAmount(BigDecimal.ZERO)
    .setOptionHeaders(Arrays.asList(header(10,"工资补发",10901,"批准工资补发"),header(210,"应发",210101,"应发工资"),header(220,"应税",220101,"应税工资"),header(230,"税额",230101,"个人所得税"),header(240,"实发",240101,"实发工资")));
  batches.updateById(b);
  Map<String,Object> input=new LinkedHashMap<>();input.put("entityScope",scope.snapshot(b));input.put("retroSources",sources);input.put("approvedInputs",Collections.emptyMap());
  Map<String,Object> rules=new LinkedHashMap<>();rules.put("mode","APPROVED_WAGE_SUPPLEMENT");rules.put("parentBatchId",b.getParentBatchId());rules.put("option",option);
  return batchService.snapshot(batchId,input,rules,results,actor);
 }
}
