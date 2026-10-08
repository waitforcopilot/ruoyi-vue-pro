package cn.iocoder.yudao.module.hrm.service.payroll.review;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.api.task.*;
import cn.iocoder.yudao.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.*;
import cn.iocoder.yudao.module.bpm.dal.dataobject.definition.BpmProcessDefinitionInfoDO;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.util.*;
import cn.iocoder.yudao.module.bpm.service.definition.BpmProcessDefinitionService;
import cn.iocoder.yudao.module.bpm.service.task.*;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.review.HrmPayrollReviewRespVO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.review.HrmPayrollReviewCycleDO;
import java.util.*;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import org.flowable.bpmn.model.*;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.stereotype.Component;

/**
 * Existing BPM carries control metadata; money and review evidence stay in protected HRM records.
 */
@Component
public class HrmPayrollReviewBpmGatewayImpl implements HrmPayrollReviewBpmGateway {
    @Resource private BpmProcessDefinitionService definitions;
    @Resource private BpmProcessInstanceApi api;
    @Resource private BpmProcessInstanceService instances;
    @Resource private BpmTaskService taskService;
    @Resource private TaskService flowableTasks;

    private void model(boolean ok) {
        if (!ok) throw exception(PAYROLL_REVIEW_BPM_UNSUPPORTED);
    }

    @Override
    public String definition() {
        ProcessDefinition d = definitions.getActiveProcessDefinition(PayrollBpmContext.KEY);
        model(d != null && !d.isSuspended());
        BpmProcessDefinitionInfoDO info = definitions.getProcessDefinitionInfo(d.getId());
        model(
                info != null
                        && Objects.equals(info.getAutoApprovalType(), 0)
                        && Objects.equals(info.getFormType(), 20)
                        && Boolean.TRUE.equals(info.getAllowCancelRunningProcess())
                        && !Boolean.TRUE.equals(info.getAllowWithdrawTask()));
        BpmnModel m = definitions.getProcessDefinitionBpmnModel(d.getId());
        model(m != null);
        org.flowable.bpmn.model.Process p = m.getMainProcess();
        model(
                p != null
                        && PayrollBpmContext.KEY.equals(p.getId())
                        && p.getFlowElements().size() == 7
                        && p.getExecutionListeners().isEmpty());
        Map<String, String> edges = new TreeMap<>();
        Set<String> users = new HashSet<>();
        for (FlowElement e : p.getFlowElements()) {
            if (e instanceof SequenceFlow) {
                SequenceFlow f = (SequenceFlow) e;
                model(f.getConditionExpression() == null);
                edges.put(f.getSourceRef(), f.getTargetRef());
            } else if (e instanceof UserTask) {
                UserTask u = (UserTask) e;
                users.add(u.getId());
                model(
                        u.getLoopCharacteristics() == null
                                && !u.isAsynchronous()
                                && Objects.equals(BpmnModelUtils.parseCandidateStrategy(u), 35)
                                && Objects.equals(BpmnModelUtils.parseApproveType(u), 1)
                                && Objects.equals(
                                        BpmnModelUtils.parseAssignStartUserHandlerType(u), 1)
                                && Objects.equals(BpmnModelUtils.parseAssignEmptyHandlerType(u), 2)
                                && !Boolean.TRUE.equals(
                                        BpmnModelUtils.parseSignEnable(m, u.getId()))
                                && u.getSkipExpression() == null
                                && u.getTaskListeners().isEmpty()
                                && u.getExecutionListeners().isEmpty());
            } else model(e instanceof StartEvent || e instanceof EndEvent);
        }
        Map<String, String> expected = new TreeMap<>();
        expected.put("StartEvent", "hrReview");
        expected.put("hrReview", "financeReview");
        expected.put("financeReview", "EndEvent");
        model(
                edges.equals(expected)
                        && users.equals(new HashSet<>(Arrays.asList("hrReview", "financeReview"))));
        return d.getId();
    }

    @Override
    public String start(HrmPayrollReviewCycleDO c) {
        Map<String, Object> vars = new LinkedHashMap<>();
        vars.put("payrollBatchId", String.valueOf(c.getBatchId()));
        vars.put("payrollRunId", String.valueOf(c.getRunId()));
        vars.put("payrollReviewId", String.valueOf(c.getId()));
        vars.put("payrollSourceHash", c.getSourceHash());
        Map<String, List<Long>> reviewers = new LinkedHashMap<>();
        reviewers.put("hrReview", Collections.singletonList(c.getHrReviewerId()));
        reviewers.put("financeReview", Collections.singletonList(c.getFinanceReviewerId()));
        try (PayrollBpmContext.Scope ignored =
                PayrollBpmContext.open(
                        new PayrollBpmContext.Frame()
                                .setAction("submit")
                                .setActorId(c.getStartedBy())
                                .setBusinessKey(String.valueOf(c.getId())))) {
            return api.createProcessInstance(
                    c.getStartedBy(),
                    new BpmProcessInstanceCreateReqDTO()
                            .setProcessDefinitionKey(PayrollBpmContext.KEY)
                            .setProcessDefinitionId(c.getProcessDefinitionId())
                            .setBusinessKey(String.valueOf(c.getId()))
                            .setVariables(vars)
                            .setStartUserSelectAssignees(reviewers));
        }
    }

    @Override
    public List<HrmPayrollReviewRespVO.Task> tasks(String id) {
        return flowableTasks.createTaskQuery().processInstanceId(id)
                .taskTenantId(String.valueOf(TenantContextHolder.getRequiredTenantId())).active()
                .list().stream()
                .map(
                        t ->
                                new HrmPayrollReviewRespVO.Task()
                                        .setId(t.getId())
                                        .setKey(t.getTaskDefinitionKey())
                                        .setName(t.getName())
                                        .setAssigneeId(
                                                t.getAssignee() == null
                                                        ? null
                                                        : Long.valueOf(t.getAssignee())))
                .collect(Collectors.toList());
    }

    @Override
    public void decide(String instanceId, String taskId, Long actor, String action) {
        try (PayrollBpmContext.Scope ignored =
                PayrollBpmContext.open(
                        new PayrollBpmContext.Frame()
                                .setAction(action)
                                .setActorId(actor)
                                .setInstanceId(instanceId)
                                .setTaskId(taskId))) {
            String reason = "薪酬版本复核：完整依据仅在有权查看的薪酬批次中保留";
            if ("approve".equals(action))
                taskService.approveTask(
                        actor,
                        new BpmTaskApproveReqVO()
                                .setId(taskId)
                                .setReason(reason)
                                .setVariables(Collections.emptyMap()));
            else
                taskService.rejectTask(
                        actor, new BpmTaskRejectReqVO().setId(taskId).setReason(reason));
        }
    }

    @Override
    public void cancel(String id, Long actor, boolean administrator) {
        try (PayrollBpmContext.Scope ignored =
                PayrollBpmContext.open(
                        new PayrollBpmContext.Frame()
                                .setAction("cancel")
                                .setActorId(actor)
                                .setInstanceId(id))) {
            if (administrator)
                instances.cancelProcessInstanceByAdmin(
                        actor,
                        new cn.iocoder.yudao.module.bpm.controller.admin.task.vo.instance
                                        .BpmProcessInstanceCancelReqVO()
                                .setId(id)
                                .setReason("薪酬复核管理撤销：完整依据保留在薪酬批次"));
            else api.cancelProcessInstanceByStartUser(actor, id, "薪酬复核撤销：完整依据保留在薪酬批次");
        }
    }

    @Override
    public Integer actualStatus(String id, String businessKey) {
        HistoricProcessInstance p = instances.getHistoricProcessInstance(id);
        if (p == null
                || !Objects.equals(
                        p.getTenantId(), String.valueOf(TenantContextHolder.getRequiredTenantId()))
                || !Objects.equals(p.getBusinessKey(), businessKey)
                || !p.getProcessDefinitionId().startsWith(PayrollBpmContext.KEY + ":")) return null;
        return FlowableUtils.getProcessInstanceStatus(p);
    }
}
