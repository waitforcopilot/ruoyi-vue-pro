package cn.iocoder.yudao.module.hrm.service.payroll;

import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.HrmPayrollSalaryProposalMapper;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.HrmPayrollSalaryProposalDO;
import cn.iocoder.yudao.module.hrm.dal.mysql.employee.info.HrmEmployeeMapper;
import cn.iocoder.yudao.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO;
import cn.iocoder.yudao.module.hrm.service.salary.employee.HrmSalaryEmployeeInfoService;
import cn.iocoder.yudao.module.hrm.controller.admin.salary.vo.employeeinfo.HrmSalaryEmployeeInfoUpdateReqVO;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Lazy;
import org.springframework.transaction.annotation.Transactional;
import javax.annotation.Resource;
import java.util.*;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Service
public class HrmPayrollSalaryProposalService {
    @Resource private cn.iocoder.yudao.module.hrm.service.payroll.HrmPayrollConfigGuardService configGuard;

    @Resource private HrmPayrollSalaryProposalMapper mapper;
    @Resource private HrmEmployeeMapper employeeMapper;
    @Resource @Lazy private HrmSalaryEmployeeInfoService salaryService;
    public List<java.util.Map<String,Object>> employees(){return employeeMapper.selectList().stream().map(employee->{java.util.Map<String,Object> row=new LinkedHashMap<>();row.put("id",employee.getId());row.put("name",employee.getName());row.put("jobNumber",employee.getJobNumber());return row;}).collect(java.util.stream.Collectors.toList());}
    public List<HrmPayrollSalaryProposalDO> list(){return mapper.selectList(new LambdaQueryWrapperX<HrmPayrollSalaryProposalDO>().orderByDesc(HrmPayrollSalaryProposalDO::getId));}
    @Transactional(rollbackFor=Exception.class)
    public Long create(HrmSalaryEmployeeInfoUpdateReqVO request) {
        if(getLoginUserId()==null)throw exception(PAYROLL_REVIEW_INCOMPLETE);
        if(employeeMapper.selectOneForUpdate(HrmEmployeeDO::getId,request.getEmployeeId())==null)throw exception(EMPLOYEE_NOT_EXISTS);
        List<HrmPayrollSalaryProposalDO> pending=mapper.selectList(new LambdaQueryWrapperX<HrmPayrollSalaryProposalDO>()
                .eq(HrmPayrollSalaryProposalDO::getEmployeeId,request.getEmployeeId()).in(HrmPayrollSalaryProposalDO::getStatus,Arrays.asList("HR_REVIEW","FINANCE_REVIEW")));
        if(!pending.isEmpty())throw exception(SALARY_PENDING_CHANGE_RECORD_EXISTS);
        if(request.getEffectTime()==null&&Objects.equals(request.getRecordType(),1)&&request.getId()==null)
            request.setEffectTime(java.time.LocalDate.now().withDayOfMonth(1).atStartOfDay());
        HrmPayrollSalaryProposalDO proposal=new HrmPayrollSalaryProposalDO();proposal.setEmployeeId(request.getEmployeeId());
        proposal.setRequestSnapshot(JsonUtils.toJsonString(request));
        proposal.setBeforeSnapshot(JsonUtils.toJsonString(salaryService.getSalaryEmployeeInfoByEmployeeIdForUpdate(request.getEmployeeId())));
        proposal.setProposedBy(getLoginUserId());proposal.setStatus("HR_REVIEW");mapper.insert(proposal);return proposal.getId();
    }
    @Transactional(rollbackFor=Exception.class)
    public void transition(Long id,String action,String reason,Long actor) {
        configGuard.lock();
        HrmPayrollSalaryProposalDO proposal=mapper.selectById(id);
        if(proposal==null)throw exception(SALARY_CHANGE_RECORD_STATUS_INVALID);
        // Same employee lock as submission and application avoids conflicting approvals.
        if(employeeMapper.selectOneForUpdate(HrmEmployeeDO::getId,proposal.getEmployeeId())==null)throw exception(EMPLOYEE_NOT_EXISTS);
        proposal=mapper.selectOneForUpdate(HrmPayrollSalaryProposalDO::getId,id);
        if(actor==null||reason==null||reason.trim().isEmpty()||reason.length()>1000)throw exception(PAYROLL_REVIEW_INCOMPLETE);
        if("reject".equals(action)&&(Objects.equals("HR_REVIEW",proposal.getStatus())||Objects.equals("FINANCE_REVIEW",proposal.getStatus()))) {
            proposal.setStatus("REJECTED");proposal.setApprovalReason(reason);proposal.setApprovedBy(actor);
        } else if("review".equals(action)&&"HR_REVIEW".equals(proposal.getStatus())) {
            if(Objects.equals(proposal.getProposedBy(),actor))throw exception(PAYROLL_SELF_APPROVAL);
            proposal.setStatus("FINANCE_REVIEW");proposal.setReviewedBy(actor);proposal.setReviewReason(reason);
        } else if("approve".equals(action)&&"FINANCE_REVIEW".equals(proposal.getStatus())) {
            if(Objects.equals(proposal.getProposedBy(),actor)||Objects.equals(proposal.getReviewedBy(),actor))throw exception(PAYROLL_SELF_APPROVAL);
            // Block stale approval when another authorized change modified the salary baseline.
            if(!Objects.equals(proposal.getBeforeSnapshot(),JsonUtils.toJsonString(salaryService.getSalaryEmployeeInfoByEmployeeIdForUpdate(proposal.getEmployeeId()))))throw exception(PAYROLL_VERSION_CONFLICT);
            Long change=salaryService.applyApprovedSalaryEmployeeInfo(JsonUtils.parseObject(proposal.getRequestSnapshot(),HrmSalaryEmployeeInfoUpdateReqVO.class));
            proposal.setChangeRecordId(change);proposal.setStatus("APPROVED");proposal.setApprovedBy(actor);proposal.setApprovalReason(reason);
        } else throw exception(SALARY_CHANGE_RECORD_STATUS_INVALID);
        mapper.updateById(proposal);
    }
}
