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
    @Resource private cn.iocoder.yudao.module.hrm.dal.mysql.salary.slip.HrmSalarySlipSendRecordMapper slipSendMapper;
    @Resource private HrmPayrollWorkService workService;
    @Resource private HrmPayrollScopeService scope;
    @Resource private HrmPayrollTaxWithholdingService taxWithholding;
    @Resource private HrmPayrollInsurancePolicyService insurance;
    @Resource private HrmPayrollInputService inputs;
    @Resource private HrmPayrollSourceClaimService sourceClaims;
    @Resource private HrmPayrollAttendanceService attendanceService;
    @Resource private HrmPayrollRunMapper runMapper;
    @Resource private HrmPayrollBatchEventMapper eventMapper;
    @Resource private HrmSalaryMonthRecordMapper batchMapper;
    @Transactional(rollbackFor=Exception.class)
    public Long snapshot(Long batchId,Object inputs,Object rules,List<HrmSalaryMonthEmployeeRecordDO> results,Long actor) {
        HrmSalaryMonthRecordDO batch=batchMapper.selectByIdForUpdate(batchId);
        if(batch==null)throw exception(SALARY_MONTH_RECORD_NOT_EXISTS);
        scope.check(batch,"CALCULATE",actor);
        if(batch.getStatus()!=11)throw exception(SALARY_MONTH_RECORD_STATUS_INVALID);
        List<HrmPayrollRunDO> previous=rawRuns(batchId);
        HrmPayrollRunDO run=new HrmPayrollRunDO();run.setMonthRecordId(batchId);
        run.setVersion(previous.isEmpty()?1:previous.get(0).getVersion()+1);
        run.setExpectedPaymentDate(batch.getExpectedPaymentDate());
        List<String> taxContexts=new ArrayList<>();
        for(HrmSalaryMonthEmployeeRecordDO result:results)
            if(result.getTaxSourceSnapshot()!=null) taxContexts.add(result.getTaxSourceSnapshot());
        if(!taxContexts.isEmpty()) {
            if(!(inputs instanceof Map)) throw exception(PAYROLL_TAX_INVALID);
            Map<String,Object> combined=new LinkedHashMap<>((Map<String,Object>)inputs);
            combined.put("withholdingContexts",taxContexts);inputs=combined;
        }
        run.setInputSnapshot(JsonUtils.toJsonString(inputs));run.setRuleSnapshot(JsonUtils.toJsonString(rules));
        run.setResultSnapshot(JsonUtils.toJsonString(results));run.setComputedBy(actor);runMapper.insert(run);
        return run.getId();
    }
    @Transactional(rollbackFor=Exception.class,isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void paymentPlan(Long batchId,java.time.LocalDate date,String reason,Long actor) {
        HrmSalaryMonthRecordDO batch=batchMapper.selectByIdForUpdate(batchId);
        if(batch==null) throw exception(SALARY_MONTH_RECORD_NOT_EXISTS);
        scope.check(batch,"MANAGE",actor);
        if(batch.getEntityId()==null) throw exception(PAYROLL_ENTITY_INVALID);
        if(date==null || date.getYear()<2019 || date.getYear()>2099 || reason==null || reason.trim().isEmpty() || reason.length()>1000)
            throw exception(PAYROLL_PAYMENT_INVALID);
        if(!(Objects.equals(batch.getStatus(),5)||Objects.equals(batch.getStatus(),11))) throw exception(SALARY_MONTH_RECORD_STATUS_INVALID);
        if(Objects.equals(batch.getExpectedPaymentDate(),date)) return;
        Map<String,Object> facts=new LinkedHashMap<>();
        facts.put("before",batch.getExpectedPaymentDate()==null?"UNPLANNED":batch.getExpectedPaymentDate().toString());
        facts.put("after",date.toString());
        batch.setExpectedPaymentDate(date); batchMapper.updateById(batch);
        eventMapper.insert(new HrmPayrollBatchEventDO().setMonthRecordId(batchId).setAction("SET_PAYMENT_PLAN")
            .setFromStatus(batch.getStatus()).setToStatus(batch.getStatus()).setActorId(actor).setReason(reason).setSnapshot(JsonUtils.toJsonString(facts)));
    }

    public cn.iocoder.yudao.framework.common.pojo.PageResult<HrmSalaryMonthRecordDO> page(
            cn.iocoder.yudao.module.hrm.controller.admin.salary.vo.monthrecord.HrmSalaryMonthRecordPageReqVO request) {
        return scope.page(request,false);
    }
    public List<HrmPayrollRunDO> runs(Long batchId){
        scope.require(batchId,"READ"); return rawRuns(batchId);
    }
    private List<HrmPayrollRunDO> rawRuns(Long batchId){
        if(batchMapper.selectById(batchId)==null)throw exception(SALARY_MONTH_RECORD_NOT_EXISTS);
        return runMapper.selectList(new LambdaQueryWrapperX<HrmPayrollRunDO>().eq(HrmPayrollRunDO::getMonthRecordId,batchId).orderByDesc(HrmPayrollRunDO::getVersion));
    }
    public List<HrmPayrollBatchEventDO> events(Long batchId){
        scope.require(batchId,"READ"); return rawEvents(batchId);
    }
    private List<HrmPayrollBatchEventDO> rawEvents(Long batchId){
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
    @Transactional(rollbackFor=Exception.class,isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void transition(Long batchId,Long runId,String action,String reason,Long actor){
        if(reason==null||reason.trim().isEmpty())throw exception(PAYROLL_REVIEW_INCOMPLETE);
        HrmSalaryMonthRecordDO batch=batchMapper.selectByIdForUpdate(batchId);
        if(batch==null)throw exception(SALARY_MONTH_RECORD_NOT_EXISTS);
        String permission="review".equals(action)?"REVIEW": "submit".equals(action)?"CALCULATE":
            "reject".equals(action)?(batch.getStatus()==12?"REVIEW":"APPROVE"):
            "unfreeze".equals(action)?"MANAGE":"APPROVE";
        scope.check(batch,permission,actor);
        List<HrmPayrollRunDO> versions=rawRuns(batchId);
        if(versions.isEmpty()||!Objects.equals(versions.get(0).getId(),runId))throw exception(PAYROLL_VERSION_CONFLICT);
        HrmPayrollRunDO run=versions.get(0);
        if(Arrays.asList("submit","review","approve","freeze").contains(action) && !Objects.equals(run.getExpectedPaymentDate(),batch.getExpectedPaymentDate()))
            throw exception(PAYROLL_VERSION_CONFLICT);
        if(("review".equals(action)||"approve".equals(action)) && Objects.equals(run.getComputedBy(),actor))throw exception(PAYROLL_SELF_APPROVAL);
        if("approve".equals(action)){
            List<HrmPayrollBatchEventDO> events=rawEvents(batchId);
            HrmPayrollBatchEventDO review=events.stream().filter(e->Objects.equals(e.getRunId(),runId)&&"review".equals(e.getAction())).findFirst().orElse(null);
            if(review==null||Objects.equals(review.getActorId(),actor))throw exception(PAYROLL_SELF_APPROVAL);
        }
        if ("unfreeze".equals(action) && slipSendMapper.selectCount(
                new LambdaQueryWrapperX<cn.iocoder.yudao.module.hrm.dal.dataobject.salary.slip.HrmSalarySlipSendRecordDO>()
                        .eq(cn.iocoder.yudao.module.hrm.dal.dataobject.salary.slip.HrmSalarySlipSendRecordDO::getMonthRecordId, batchId)
                        .eq(cn.iocoder.yudao.module.hrm.dal.dataobject.salary.slip.HrmSalarySlipSendRecordDO::getWithdrawn, false)) > 0) {
            throw exception(SALARY_MONTH_RECORD_STATUS_INVALID);
        }
        int target=nextStatus(batch.getStatus(),action);
        if("freeze".equals(action)){
            sourceClaims.lockEmployees(run);
            HrmPayrollRunDO source=run;Set<Long> visited=new HashSet<>();
            while(source!=null){
                if(!visited.add(source.getId()))throw exception(PAYROLL_VERSION_CONFLICT);
                scope.validateInputSnapshot(source.getInputSnapshot());
                insurance.validateInputSnapshot(source.getInputSnapshot());
                inputs.validateInputSnapshot(source.getInputSnapshot());
                workService.validateInputSnapshot(source.getInputSnapshot());
                attendanceService.validateInputSnapshot(source.getInputSnapshot());
                taxWithholding.validateInputSnapshot(source.getInputSnapshot(),batch.getEntityId(),source.getExpectedPaymentDate());
                Map<String,Object> input=JsonUtils.parseObject(source.getInputSnapshot(),Map.class);
                Object parent=input.get("sourceRunId");if(parent==null)break;
                if(!(parent instanceof Number))throw exception(PAYROLL_VERSION_CONFLICT);
                Long parentId=((Number)parent).longValue();Integer currentVersion=source.getVersion();
                source=versions.stream().filter(r->Objects.equals(r.getId(),parentId)&&r.getVersion()<currentVersion).findFirst().orElse(null);
                if(source==null)throw exception(PAYROLL_VERSION_CONFLICT);
            }
        }
        if("freeze".equals(action))sourceClaims.reserve(batch,run,actor);
        if("unfreeze".equals(action))sourceClaims.release(batchId,runId,reason,actor);
        batchMapper.updateById(new HrmSalaryMonthRecordDO().setId(batchId).setStatus(target));
        HrmPayrollBatchEventDO event=new HrmPayrollBatchEventDO();event.setMonthRecordId(batchId);event.setRunId(runId);
        event.setAction(action);event.setActorId(actor);event.setReason(reason);event.setFromStatus(batch.getStatus());event.setToStatus(target);eventMapper.insert(event);
    }
}
