package cn.iocoder.yudao.module.hrm.service.payroll;

import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.HrmPayrollFormulaDO;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.HrmPayrollFormulaMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.salary.config.HrmSalaryOptionMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.salary.config.HrmSalaryGroupMapper;
import cn.iocoder.yudao.module.hrm.dal.dataobject.salary.config.HrmSalaryOptionDO;
import cn.iocoder.yudao.module.hrm.enums.salary.config.HrmSalaryOptionCodeEnum;
import cn.iocoder.yudao.module.hrm.controller.admin.salary.vo.option.HrmSalaryOptionValueVO;
import cn.iocoder.yudao.module.hrm.service.payroll.rule.PayrollFormulaEngine;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.annotation.Resource;
import java.util.*;
import java.time.LocalDate;
import java.math.BigDecimal;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;

/** Immutable, separately approved arithmetic rules effective at payroll period start. */
@Service
public class HrmPayrollFormulaService {
    @Resource private HrmPayrollFormulaMapper mapper;
    @Resource private HrmSalaryOptionMapper optionMapper;
    @Resource private HrmSalaryGroupMapper groupMapper;

    public List<HrmPayrollFormulaDO> list() {
        return mapper.selectList(new LambdaQueryWrapperX<HrmPayrollFormulaDO>().orderByDesc(HrmPayrollFormulaDO::getId));
    }
    public java.util.List<cn.iocoder.yudao.module.hrm.dal.dataobject.salary.config.HrmSalaryGroupDO> groups() {return groupMapper.selectList();}
    public List<HrmSalaryOptionDO> variables() {
        List<HrmSalaryOptionDO> catalog=optionMapper.selectList();
        Set<Integer> parents=new HashSet<>();catalog.forEach(item->parents.add(item.getParentCode()));
        catalog.removeIf(item->Boolean.FALSE.equals(item.getEnabled()) || parents.contains(item.getCode())
                || HrmSalaryOptionCodeEnum.COMPUTED_CODES.contains(item.getCode()));
        return catalog;
    }
    private Set<String> names() {
        Set<String> names=new HashSet<>();variables().forEach(item->names.add("item_"+item.getCode()));
        names.add("actual_work_days");names.add("need_work_days");return names;
    }
    @Transactional(rollbackFor=Exception.class)
    public Long create(Integer code,String expression,LocalDate from,LocalDate to,String evidence,Long salaryGroupId) {
        Set<String> names=names();
        if(HrmSalaryOptionCodeEnum.INSURANCE_SOURCE_CODES.contains(code)||!names.contains("item_"+code)||from==null||to==null||to.isBefore(from)
                ||evidence==null||evidence.trim().isEmpty()||evidence.length()>1000
                ||(salaryGroupId!=null&&groupMapper.selectById(salaryGroupId)==null))throw exception(PAYROLL_FORMULA_INVALID);
        try {PayrollFormulaEngine.validateGraph(Collections.singletonMap("item_"+code,expression),names);}
        catch(IllegalArgumentException ex){throw exception(PAYROLL_FORMULA_INVALID);}
        HrmPayrollFormulaDO record=new HrmPayrollFormulaDO();record.setOptionCode(code);record.setExpression(expression);
        record.setEffectiveFrom(from);record.setEffectiveTo(to);record.setEvidence(evidence);
        record.setSalaryGroupId(salaryGroupId);record.setStatus("PENDING");mapper.insert(record);return record.getId();
    }
    @Transactional(rollbackFor=Exception.class)
    public void approve(Long id,Long actor) {
        // A common ordered catalog lock prevents simultaneous approvals of mutually dependent outputs.
        optionMapper.selectList(new LambdaQueryWrapperX<HrmSalaryOptionDO>().orderByAsc(HrmSalaryOptionDO::getId).last("FOR UPDATE"));
        HrmPayrollFormulaDO rule=mapper.selectOneForUpdate(HrmPayrollFormulaDO::getId,id);
        if(rule==null||HrmSalaryOptionCodeEnum.INSURANCE_SOURCE_CODES.contains(rule.getOptionCode())||!"PENDING".equals(rule.getStatus())||actor==null)throw exception(PAYROLL_FORMULA_INVALID);
        if(Objects.equals(rule.getCreator(),String.valueOf(actor)))throw exception(PAYROLL_SELF_APPROVAL);
        List<HrmPayrollFormulaDO> approved=mapper.selectList(new LambdaQueryWrapperX<HrmPayrollFormulaDO>().eq(HrmPayrollFormulaDO::getStatus,"APPROVED"));
        for(HrmPayrollFormulaDO existing:approved)if(Objects.equals(existing.getOptionCode(),rule.getOptionCode())
                &&Objects.equals(existing.getSalaryGroupId(),rule.getSalaryGroupId())
                &&!existing.getEffectiveFrom().isAfter(rule.getEffectiveTo())&&!existing.getEffectiveTo().isBefore(rule.getEffectiveFrom()))throw exception(PAYROLL_FORMULA_INVALID);
        approved.add(rule);
        Set<Long> groups=new HashSet<>();groups.add(null);approved.forEach(value->groups.add(value.getSalaryGroupId()));
        Set<LocalDate> boundaries=new TreeSet<>();boundaries.add(rule.getEffectiveFrom());
        for(HrmPayrollFormulaDO existing:approved){
            if(!existing.getEffectiveFrom().isBefore(rule.getEffectiveFrom())&&!existing.getEffectiveFrom().isAfter(rule.getEffectiveTo()))boundaries.add(existing.getEffectiveFrom());
            if(existing.getEffectiveTo().isBefore(rule.getEffectiveTo())&&!existing.getEffectiveTo().isBefore(rule.getEffectiveFrom()))boundaries.add(existing.getEffectiveTo().plusDays(1));
        }
        try {for(Long group:groups)for(LocalDate date:boundaries)PayrollFormulaEngine.validateGraph(expressions(select(approved,date,group)),names());}
        catch(IllegalArgumentException ex){throw exception(PAYROLL_FORMULA_INVALID);}
        rule.setStatus("APPROVED");rule.setApprovedBy(actor);mapper.updateById(rule);
    }
    public List<HrmPayrollFormulaDO> effective(LocalDate date) {
        return mapper.selectList(new LambdaQueryWrapperX<HrmPayrollFormulaDO>().eq(HrmPayrollFormulaDO::getStatus,"APPROVED")
                .le(HrmPayrollFormulaDO::getEffectiveFrom,date).ge(HrmPayrollFormulaDO::getEffectiveTo,date));
    }
    private List<HrmPayrollFormulaDO> select(List<HrmPayrollFormulaDO> rules,LocalDate date,Long group) {
        Map<Integer,HrmPayrollFormulaDO> selected=new LinkedHashMap<>();
        for(HrmPayrollFormulaDO rule:rules)if(!date.isBefore(rule.getEffectiveFrom())&&!date.isAfter(rule.getEffectiveTo())&&rule.getSalaryGroupId()==null)selected.put(rule.getOptionCode(),rule);
        if(group!=null)for(HrmPayrollFormulaDO rule:rules)if(!date.isBefore(rule.getEffectiveFrom())&&!date.isAfter(rule.getEffectiveTo())&&Objects.equals(group,rule.getSalaryGroupId()))selected.put(rule.getOptionCode(),rule);
        return new ArrayList<>(selected.values());
    }
    private Map<String,String> expressions(List<HrmPayrollFormulaDO> rules) {
        Map<String,String> result=new LinkedHashMap<>();rules.forEach(rule->result.put("item_"+rule.getOptionCode(),rule.getExpression()));return result;
    }
    private Map<String,BigDecimal> evaluate(List<HrmPayrollFormulaDO> rules,Map<String,BigDecimal> inputs) {
        if(rules.stream().anyMatch(rule->HrmSalaryOptionCodeEnum.INSURANCE_SOURCE_CODES.contains(rule.getOptionCode())))throw exception(PAYROLL_FORMULA_INVALID);
        Map<String,String> formulas=expressions(rules);
        PayrollFormulaEngine.validateGraph(formulas,names());
        Set<String> referenced=new HashSet<>();formulas.values().forEach(expression->referenced.addAll(PayrollFormulaEngine.variables(expression)));
        Map<String,BigDecimal> selected=new LinkedHashMap<>();
        inputs.forEach((key,value)->{if(referenced.contains(key)&&!formulas.containsKey(key))selected.put(key,value);});
        return PayrollFormulaEngine.evaluate(formulas,selected);
    }
    public Map<String,BigDecimal> preview(Long id,LocalDate date,Long group,Map<String,BigDecimal> inputs) {
        HrmPayrollFormulaDO candidate=mapper.selectById(id);
        if(candidate==null||date==null||date.isBefore(candidate.getEffectiveFrom())||date.isAfter(candidate.getEffectiveTo())
                ||(candidate.getSalaryGroupId()!=null&&!Objects.equals(group,candidate.getSalaryGroupId())))throw exception(PAYROLL_FORMULA_INVALID);
        List<HrmPayrollFormulaDO> rules=select(effective(date),date,group);
        rules.removeIf(rule->Objects.equals(rule.getOptionCode(),candidate.getOptionCode()));rules.add(candidate);
        try {return evaluate(rules,inputs);}catch(IllegalArgumentException ex){throw exception(PAYROLL_FORMULA_INVALID);}
    }
    public List<HrmSalaryOptionValueVO> calculate(List<HrmSalaryOptionValueVO> values,Map<Integer,HrmSalaryOptionDO> options,LocalDate date,Long group,BigDecimal actualDays,BigDecimal needDays) {
        List<HrmPayrollFormulaDO> rules=select(effective(date),date,group);if(rules.isEmpty())return values;
        Map<String,BigDecimal> inputs=new LinkedHashMap<>();
        for(HrmSalaryOptionValueVO value:values)if(value.getValue()!=null)inputs.put("item_"+value.getCode(),value.getValue());
        if(actualDays!=null)inputs.put("actual_work_days",actualDays);if(needDays!=null)inputs.put("need_work_days",needDays);
        Map<String,BigDecimal> computed;
        try {computed=evaluate(rules,inputs);}catch(IllegalArgumentException ex){throw exception(PAYROLL_FORMULA_INVALID);}
        List<HrmSalaryOptionValueVO> result=new ArrayList<>(values);
        for(HrmPayrollFormulaDO rule:rules){
            HrmSalaryOptionDO option=options.get(rule.getOptionCode());if(option==null||Boolean.FALSE.equals(option.getEnabled()))throw exception(PAYROLL_FORMULA_INVALID);
            result.removeIf(value->Objects.equals(value.getCode(),rule.getOptionCode()));
            result.add(new HrmSalaryOptionValueVO().setCode(rule.getOptionCode()).setName(option.getName()).setValue(computed.get("item_"+rule.getOptionCode())));
        }
        return result;
    }
}
