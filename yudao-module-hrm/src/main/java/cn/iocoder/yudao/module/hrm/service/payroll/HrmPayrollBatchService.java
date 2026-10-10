package cn.iocoder.yudao.module.hrm.service.payroll;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.salary.monthrecord.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.salary.monthrecord.HrmSalaryMonthRecordMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.annotation.Resource;
import java.util.*;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;
@Service
public class HrmPayrollBatchService {
    @Resource private HrmPayrollRunMapper runMapper;
    @Resource private HrmPayrollBatchEventMapper eventMapper;
    @Resource private HrmSalaryMonthRecordMapper batchMapper;
    @Transactional(rollbackFor=Exception.class)
    public Long snapshot(Long batchId,Object inputs,Object rules,List<HrmSalaryMonthEmployeeRecordDO> results,Long actor) {
        HrmSalaryMonthRecordDO batch=batchMapper.selectByIdForUpdate(batchId);
        if(batch==null)throw exception(SALARY_MONTH_RECORD_NOT_EXISTS);
        if(batch.getStatus()!=11)throw exception(SALARY_MONTH_RECORD_STATUS_INVALID);
        List<HrmPayrollRunDO> previous=runs(batchId);
        HrmPayrollRunDO run=new HrmPayrollRunDO();run.setMonthRecordId(batchId);
        run.setVersion(previous.isEmpty()?1:previous.get(0).getVersion()+1);
        run.setInputSnapshot(JsonUtils.toJsonString(inputs));run.setRuleSnapshot(JsonUtils.toJsonString(rules));
        run.setResultSnapshot(JsonUtils.toJsonString(results));run.setComputedBy(actor);runMapper.insert(run);
        return run.getId();
    }
    public cn.iocoder.yudao.framework.common.pojo.PageResult<HrmSalaryMonthRecordDO> page(
            cn.iocoder.yudao.module.hrm.controller.admin.salary.vo.monthrecord.HrmSalaryMonthRecordPageReqVO request) {
        return batchMapper.selectPage(request);
    }
    public List<HrmPayrollRunDO> runs(Long batchId){
        if(batchMapper.selectById(batchId)==null)throw exception(SALARY_MONTH_RECORD_NOT_EXISTS);
        return runMapper.selectList(new LambdaQueryWrapperX<HrmPayrollRunDO>().eq(HrmPayrollRunDO::getMonthRecordId,batchId).orderByDesc(HrmPayrollRunDO::getVersion));
    }
    public List<HrmPayrollBatchEventDO> events(Long batchId){
        if(batchMapper.selectById(batchId)==null)throw exception(SALARY_MONTH_RECORD_NOT_EXISTS);
        return eventMapper.selectList(new LambdaQueryWrapperX<HrmPayrollBatchEventDO>().eq(HrmPayrollBatchEventDO::getMonthRecordId,batchId).orderByDesc(HrmPayrollBatchEventDO::getId));
    }
    public static int nextStatus(int status,String action){
        if(status==11 && "submit".equals(action))return 12;
        if(status==12 && "review".equals(action))return 13;
        if(status==13 && "approve".equals(action))return 14;
        if(status==14 && "freeze".equals(action))return 15;
        if((status==12||status==13||status==14) && "reject".equals(action))return 11;
        if(status==15 && "unfreeze".equals(action))return 11;
        if(status==15 && "pay".equals(action))return 16;
        if(status==17 && "archive".equals(action))return 10;
        throw exception(SALARY_MONTH_RECORD_STATUS_INVALID);
    }
    @Transactional(rollbackFor=Exception.class)
    public void transition(Long batchId,Long runId,String action,String reason,Long actor){
        if(reason==null||reason.trim().isEmpty())throw exception(PAYROLL_REVIEW_INCOMPLETE);
        HrmSalaryMonthRecordDO batch=batchMapper.selectByIdForUpdate(batchId);
        if(batch==null)throw exception(SALARY_MONTH_RECORD_NOT_EXISTS);
        List<HrmPayrollRunDO> versions=runs(batchId);
        if(versions.isEmpty()||!Objects.equals(versions.get(0).getId(),runId))throw exception(PAYROLL_VERSION_CONFLICT);
        HrmPayrollRunDO run=versions.get(0);
        if(("review".equals(action)||"approve".equals(action)) && Objects.equals(run.getComputedBy(),actor))throw exception(PAYROLL_SELF_APPROVAL);
        if("approve".equals(action)){
            List<HrmPayrollBatchEventDO> events=events(batchId);
            HrmPayrollBatchEventDO review=events.stream().filter(e->Objects.equals(e.getRunId(),runId)&&"review".equals(e.getAction())).findFirst().orElse(null);
            if(review==null||Objects.equals(review.getActorId(),actor))throw exception(PAYROLL_SELF_APPROVAL);
        }
        int target=nextStatus(batch.getStatus(),action);
        batchMapper.updateById(new HrmSalaryMonthRecordDO().setId(batchId).setStatus(target));
        HrmPayrollBatchEventDO event=new HrmPayrollBatchEventDO();event.setMonthRecordId(batchId);event.setRunId(runId);
        event.setAction(action);event.setActorId(actor);event.setReason(reason);event.setFromStatus(batch.getStatus());event.setToStatus(target);eventMapper.insert(event);
    }
}
