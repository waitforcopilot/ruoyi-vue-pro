package cn.iocoder.yudao.module.bpm.api.task;

import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;

/**
 * Optional business-module checks before BPM mutations; unregistered process keys are unaffected.
 */
public interface BpmBusinessTaskGuard {
    void beforeTaskOperation(Task task, Long actorId);

    default void beforeStart(String processKey, Long actorId, String businessKey) {}

    default void beforeCancel(ProcessInstance instance, Long actorId) {}

    /** Business workflows may use their own task page instead of generic external SMS. */
    default boolean useGenericSms(String processKey) { return true; }
}
