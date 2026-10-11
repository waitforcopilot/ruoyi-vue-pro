package cn.iocoder.yudao.module.hrm.service.payroll;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.HrmPayrollGradePolicyDO;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.HrmPayrollGradePolicyMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.employee.info.HrmEmployeeMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.salary.config.HrmSalaryOptionMapper;
import cn.iocoder.yudao.module.hrm.dal.dataobject.salary.config.HrmSalaryOptionDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollGradePolicyReqVO;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollGradePolicyReqVO.*;
import cn.iocoder.yudao.module.hrm.controller.admin.salary.vo.employeeinfo.HrmSalaryEmployeeInfoUpdateReqVO;
import cn.iocoder.yudao.module.hrm.controller.admin.salary.vo.option.HrmSalaryOptionValueVO;
import cn.iocoder.yudao.module.hrm.service.salary.employee.HrmSalaryEmployeeInfoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.annotation.Resource;
import javax.validation.Validator;
import java.util.*;
import java.math.*;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;
@Service
public class HrmPayrollGradePolicyService {
    @Resource private HrmPayrollGradePolicyMapper mapper;
    @Resource private HrmSalaryOptionMapper optionMapper;
    @Resource private HrmEmployeeMapper employeeMapper;
    @Resource private HrmPayrollFormulaService formulaService;
    @Resource private HrmSalaryEmployeeInfoService salaryService;
    @Resource private Validator validator;
    public List<HrmPayrollGradePolicyDO> list(){return mapper.selectList(new LambdaQueryWrapperX<HrmPayrollGradePolicyDO>().orderByDesc(HrmPayrollGradePolicyDO::getId));}
    public List<cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollGradeImportRowVO> exportRows(Long id) {
        HrmPayrollGradePolicyDO policy=mapper.selectById(id);if(policy==null)throw exception(PAYROLL_GRADE_INVALID);
        List<cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollGradeImportRowVO> result=new ArrayList<>();
        for(Grade grade:JsonUtils.parseArray(policy.getGrades(),Grade.class)){cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollGradeImportRowVO row=new cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollGradeImportRowVO();row.setType("GRADE");row.setGradeCode(grade.getGradeCode());row.setStepCode(grade.getStepCode());row.setMinimum(grade.getMinimum());row.setStandard(grade.getStandard());row.setMaximum(grade.getMaximum());result.add(row);}
        for(PostCoefficient post:JsonUtils.parseArray(policy.getPostCoefficients(),PostCoefficient.class)){cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollGradeImportRowVO row=new cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollGradeImportRowVO();row.setType("POST");row.setPostName(post.getPostName());row.setCoefficient(post.getCoefficient());result.add(row);}return result;
    }
    @Transactional(rollbackFor=Exception.class)
    public Long create(HrmPayrollGradePolicyReqVO request) {
        if(!validator.validate(request).isEmpty()||request.getEffectiveTo().isBefore(request.getEffectiveFrom())
                ||formulaService.variables().stream().noneMatch(option->Objects.equals(option.getCode(),request.getOptionCode())))throw exception(PAYROLL_GRADE_INVALID);
        Set<String> keys=new HashSet<>();for(Grade grade:request.getGrades())if(!keys.add(grade.getGradeCode()+"|"+grade.getStepCode())
                ||grade.getMinimum().compareTo(grade.getStandard())>0||grade.getStandard().compareTo(grade.getMaximum())>0)throw exception(PAYROLL_GRADE_INVALID);
        request.getGrades().forEach(grade->{grade.setMinimum(grade.getMinimum().setScale(2));grade.setStandard(grade.getStandard().setScale(2));grade.setMaximum(grade.getMaximum().setScale(2));});
        keys.clear();for(PostCoefficient post:request.getPostCoefficients())if(!keys.add(post.getPostName().trim())||!post.getPostName().equals(post.getPostName().trim()))throw exception(PAYROLL_GRADE_INVALID);
        optionMapper.selectList(new LambdaQueryWrapperX<HrmSalaryOptionDO>().orderByAsc(HrmSalaryOptionDO::getId).last("FOR UPDATE"));
        List<HrmPayrollGradePolicyDO> versions=mapper.selectList(new LambdaQueryWrapperX<HrmPayrollGradePolicyDO>().eq(HrmPayrollGradePolicyDO::getCode,request.getCode()).orderByDesc(HrmPayrollGradePolicyDO::getVersion));
        HrmPayrollGradePolicyDO policy=new HrmPayrollGradePolicyDO();policy.setCode(request.getCode());policy.setName(request.getName());policy.setOptionCode(request.getOptionCode());
        policy.setVersion(versions.isEmpty()?1:versions.get(0).getVersion()+1);policy.setEffectiveFrom(request.getEffectiveFrom());policy.setEffectiveTo(request.getEffectiveTo());
        policy.setGrades(JsonUtils.toJsonString(request.getGrades()));policy.setPostCoefficients(JsonUtils.toJsonString(request.getPostCoefficients()));
        policy.setEvidence(request.getEvidence());policy.setStatus("PENDING");mapper.insert(policy);return policy.getId();
    }
    public List<Map<String,Object>> validateRows(List<cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollGradeImportRowVO> rows) {
        List<Map<String,Object>> errors=new ArrayList<>();Set<String> grades=new HashSet<>(),posts=new HashSet<>();
        if(rows==null||rows.isEmpty()||rows.size()>2000){Map<String,Object> error=new LinkedHashMap<>();error.put("row",0);error.put("reason","文件应包含1至2000行数据");errors.add(error);return errors;}
        for(int index=0;index<rows.size();index++){
            cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollGradeImportRowVO row=rows.get(index);String reason=null;
            if("GRADE".equals(row.getType())){
                Grade grade=new Grade();grade.setGradeCode(row.getGradeCode());grade.setStepCode(row.getStepCode());grade.setMinimum(row.getMinimum());grade.setStandard(row.getStandard());grade.setMaximum(row.getMaximum());
                if(!validator.validate(grade).isEmpty())reason="薪级、薪档及金额必填，金额须为最多两位小数的正数";
                else if(!grades.add(grade.getGradeCode()+"|"+grade.getStepCode()))reason="薪级与薪档重复";
                else if(grade.getMinimum().compareTo(grade.getStandard())>0||grade.getStandard().compareTo(grade.getMaximum())>0)reason="最低、标准、最高金额顺序不合法";
            } else if("POST".equals(row.getType())){
                PostCoefficient post=new PostCoefficient();post.setPostName(row.getPostName());post.setCoefficient(row.getCoefficient());
                if(!validator.validate(post).isEmpty())reason="岗位名称必填，系数须为0.01至100之间的数值";
                else if(!post.getPostName().equals(post.getPostName().trim())||!posts.add(post.getPostName()))reason="岗位重复或名称存在首尾空格";
            } else reason="类型必须为 GRADE 或 POST";
            if(reason!=null){Map<String,Object> error=new LinkedHashMap<>();error.put("row",index+2);error.put("reason",reason);errors.add(error);}
        }
        return errors;
    }
    @Transactional(rollbackFor=Exception.class)
    public Long importRows(HrmPayrollGradePolicyReqVO request,List<cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollGradeImportRowVO> rows) {
        if(!validateRows(rows).isEmpty())throw exception(PAYROLL_GRADE_INVALID);
        List<Grade> grades=new ArrayList<>();List<PostCoefficient> posts=new ArrayList<>();
        for(cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollGradeImportRowVO row:rows){
            if("GRADE".equals(row.getType())){Grade grade=new Grade();grade.setGradeCode(row.getGradeCode());grade.setStepCode(row.getStepCode());grade.setMinimum(row.getMinimum());grade.setStandard(row.getStandard());grade.setMaximum(row.getMaximum());grades.add(grade);}
            else if("POST".equals(row.getType())){PostCoefficient post=new PostCoefficient();post.setPostName(row.getPostName());post.setCoefficient(row.getCoefficient());posts.add(post);}
            else throw exception(PAYROLL_GRADE_INVALID);
        }
        request.setGrades(grades);request.setPostCoefficients(posts);return create(request);
    }
    @Transactional(rollbackFor=Exception.class)
    public void approve(Long id,Long actor) {
        optionMapper.selectList(new LambdaQueryWrapperX<HrmSalaryOptionDO>().orderByAsc(HrmSalaryOptionDO::getId).last("FOR UPDATE"));
        HrmPayrollGradePolicyDO policy=mapper.selectOneForUpdate(HrmPayrollGradePolicyDO::getId,id);
        if(policy==null||!"PENDING".equals(policy.getStatus())||actor==null)throw exception(PAYROLL_GRADE_INVALID);
        if(Objects.equals(policy.getCreator(),String.valueOf(actor)))throw exception(PAYROLL_SELF_APPROVAL);
        if(!mapper.selectList(new LambdaQueryWrapperX<HrmPayrollGradePolicyDO>().eq(HrmPayrollGradePolicyDO::getCode,policy.getCode())
                .eq(HrmPayrollGradePolicyDO::getStatus,"APPROVED").le(HrmPayrollGradePolicyDO::getEffectiveFrom,policy.getEffectiveTo()).ge(HrmPayrollGradePolicyDO::getEffectiveTo,policy.getEffectiveFrom())).isEmpty())throw exception(PAYROLL_GRADE_INVALID);
        policy.setStatus("APPROVED");policy.setApprovedBy(actor);mapper.updateById(policy);
    }
    @Transactional(rollbackFor=Exception.class)
    public Long apply(Apply request) {
        if(!validator.validate(request).isEmpty())throw exception(PAYROLL_GRADE_INVALID);
        HrmPayrollGradePolicyDO policy=mapper.selectById(request.getPolicyId());
        if(policy==null||!"APPROVED".equals(policy.getStatus())||request.getEffectiveDate().isBefore(policy.getEffectiveFrom())||request.getEffectiveDate().isAfter(policy.getEffectiveTo()))throw exception(PAYROLL_GRADE_INVALID);
        HrmEmployeeDO employee=employeeMapper.selectOneForUpdate(HrmEmployeeDO::getId,request.getEmployeeId());if(employee==null)throw exception(EMPLOYEE_NOT_EXISTS);
        Grade grade=JsonUtils.parseArray(policy.getGrades(),Grade.class).stream().filter(value->Objects.equals(value.getGradeCode(),request.getGradeCode())&&Objects.equals(value.getStepCode(),request.getStepCode())).findFirst().orElse(null);
        if(grade==null)throw exception(PAYROLL_GRADE_INVALID);
        List<PostCoefficient> posts=JsonUtils.parseArray(policy.getPostCoefficients(),PostCoefficient.class);
        PostCoefficient post=posts.stream().filter(value->Objects.equals(value.getPostName(),employee.getPostName())).findFirst().orElse(null);
        if(!posts.isEmpty()&&post==null)throw exception(PAYROLL_GRADE_INVALID);
        BigDecimal base=request.getBaseAmount()==null?grade.getStandard():request.getBaseAmount();
        if(base.compareTo(grade.getMinimum())<0||base.compareTo(grade.getMaximum())>0)throw exception(PAYROLL_GRADE_INVALID);
        BigDecimal coefficient=post==null?BigDecimal.ONE:post.getCoefficient();
        BigDecimal amount=base.multiply(coefficient).setScale(2,RoundingMode.HALF_UP);
        HrmSalaryOptionDO option=optionMapper.selectOne(HrmSalaryOptionDO::getCode,policy.getOptionCode());
        if(option==null||Boolean.FALSE.equals(option.getEnabled())||amount.signum()<=0||amount.precision()-amount.scale()>10)throw exception(PAYROLL_GRADE_INVALID);
        HrmSalaryEmployeeInfoUpdateReqVO proposal=new HrmSalaryEmployeeInfoUpdateReqVO();proposal.setEmployeeId(employee.getId());
        proposal.setRecordType(salaryService.getSalaryEmployeeInfoByEmployeeIdForUpdate(employee.getId())==null?1:2);proposal.setChangeReason(1);proposal.setEffectTime(request.getEffectiveDate().atStartOfDay());
        proposal.setSalaryOptions(Collections.singletonList(new HrmSalaryOptionValueVO().setCode(option.getCode()).setName(option.getName()).setValue(amount)));
        proposal.setProbationSalaryOptions(Collections.singletonList(new HrmSalaryOptionValueVO().setCode(option.getCode()).setName(option.getName()).setValue(amount.multiply(request.getProbationFactor()).setScale(2,RoundingMode.HALF_UP))));
        proposal.setRemark("薪级方案 "+policy.getCode()+" v"+policy.getVersion()+" (#"+policy.getId()+")；薪级/薪档 "+grade.getGradeCode()+"/"+grade.getStepCode()+"；基数 "+base+"；岗位系数 "+coefficient);
        return salaryService.updateSalaryEmployeeInfo(proposal);
    }
}
