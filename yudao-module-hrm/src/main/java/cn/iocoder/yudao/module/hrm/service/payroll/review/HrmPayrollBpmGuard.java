package cn.iocoder.yudao.module.hrm.service.payroll.review;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.PAYROLL_REVIEW_PERMISSION;

import cn.iocoder.yudao.module.bpm.api.task.BpmBusinessTaskGuard;
import java.util.*;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Component;

@Component
public class HrmPayrollBpmGuard implements BpmBusinessTaskGuard {
    @Override
    public boolean useGenericSms(String key) {
        // Payroll approval cannot depend on an optional phone number or SMS provider.
        // Assigned BPM tasks and the protected payroll page remain the first-release work queue.
        return !PayrollBpmContext.KEY.equals(key);
    }

    private boolean applies(String id) {
        return id != null && id.startsWith(PayrollBpmContext.KEY + ":");
    }

    @Override
    public void beforeTaskOperation(Task task, Long actor) {
        if (!applies(task.getProcessDefinitionId())) return;
        PayrollBpmContext.Frame f = PayrollBpmContext.current();
        if (f == null
                || !Arrays.asList("approve", "reject").contains(f.getAction())
                || !Objects.equals(actor, f.getActorId())
                || !Objects.equals(task.getId(), f.getTaskId())
                || !Objects.equals(task.getProcessInstanceId(), f.getInstanceId()))
            throw exception(PAYROLL_REVIEW_PERMISSION);
    }

    @Override
    public void beforeStart(String key, Long actor, String businessKey) {
        if (!PayrollBpmContext.KEY.equals(key)) return;
        PayrollBpmContext.Frame f = PayrollBpmContext.current();
        if (f == null
                || !"submit".equals(f.getAction())
                || !Objects.equals(actor, f.getActorId())
                || !Objects.equals(businessKey, f.getBusinessKey()))
            throw exception(PAYROLL_REVIEW_PERMISSION);
    }

    @Override
    public void beforeCancel(ProcessInstance instance, Long actor) {
        if (!applies(instance.getProcessDefinitionId())) return;
        PayrollBpmContext.Frame f = PayrollBpmContext.current();
        if (f == null
                || !"cancel".equals(f.getAction())
                || !Objects.equals(actor, f.getActorId())
                || !Objects.equals(instance.getId(), f.getInstanceId()))
            throw exception(PAYROLL_REVIEW_PERMISSION);
    }
}
