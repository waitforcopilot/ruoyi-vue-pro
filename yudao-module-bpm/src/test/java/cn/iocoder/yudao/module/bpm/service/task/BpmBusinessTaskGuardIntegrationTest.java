package cn.iocoder.yudao.module.bpm.service.task;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.api.task.BpmBusinessTaskGuard;
import cn.iocoder.yudao.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.instance.BpmProcessInstanceCancelReqVO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.definition.BpmProcessDefinitionInfoDO;
import cn.iocoder.yudao.module.bpm.service.definition.BpmProcessDefinitionService;
import java.util.*;
import org.flowable.engine.TaskService;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Verify the optional business guard is reached by real BPM service entry points before mutation.
 */
class BpmBusinessTaskGuardIntegrationTest {
    private final BpmBusinessTaskGuard guard = mock(BpmBusinessTaskGuard.class);
    private final BpmProcessDefinitionService definitions = mock(BpmProcessDefinitionService.class);
    private final BpmProcessInstanceServiceImpl processes =
            spy(new BpmProcessInstanceServiceImpl());
    private final BpmTaskServiceImpl tasks = new BpmTaskServiceImpl();
    private final IllegalStateException denied = new IllegalStateException("business guard denied");

    @BeforeEach
    void setup() {
        TenantContextHolder.setTenantId(1L);
        ReflectionTestUtils.setField(
                processes, "businessTaskGuards", Collections.singletonList(guard));
        ReflectionTestUtils.setField(processes, "processDefinitionService", definitions);
        ReflectionTestUtils.setField(tasks, "businessTaskGuards", Collections.singletonList(guard));
    }

    @AfterEach
    void clear() {
        TenantContextHolder.clear();
    }

    private ProcessDefinition definition() {
        ProcessDefinition d = mock(ProcessDefinition.class);
        when(d.getId()).thenReturn("pinned-deployment");
        when(d.getKey()).thenReturn("guarded");
        when(d.getTenantId()).thenReturn("1");
        return d;
    }

    @Test
    void definitionIdIsPinnedAndStartGuardRunsBeforeFlowableMutation() {
        ProcessDefinition d = definition();
        when(definitions.getProcessDefinition("pinned-deployment")).thenReturn(d);
        doThrow(denied).when(guard).beforeStart("guarded", 10L, "cycle-1");
        assertSame(
                denied,
                assertThrows(
                                RuntimeException.class,
                                () ->
                                        processes.createProcessInstance(
                                                10L,
                                                new BpmProcessInstanceCreateReqDTO()
                                                        .setProcessDefinitionId("pinned-deployment")
                                                        .setProcessDefinitionKey("guarded")
                                                        .setBusinessKey("cycle-1")))
                        .getCause());
        verify(definitions, never()).getActiveProcessDefinition(anyString());
    }

    @Test
    void previousDtoCallersStillResolveTheirKeyBeforeGuard() {
        ProcessDefinition d = definition();
        when(definitions.getActiveProcessDefinition("guarded")).thenReturn(d);
        doThrow(denied).when(guard).beforeStart("guarded", 10L, "cycle-1");
        assertSame(
                denied,
                assertThrows(
                                RuntimeException.class,
                                () ->
                                        processes.createProcessInstance(
                                                10L,
                                                new BpmProcessInstanceCreateReqDTO()
                                                        .setProcessDefinitionKey("guarded")
                                                        .setBusinessKey("cycle-1")))
                        .getCause());
        verify(definitions).getActiveProcessDefinition("guarded");
    }

    @Test
    void definitionFromAnotherTenantCannotReachStartGuard() {
        ProcessDefinition d = definition();
        when(d.getTenantId()).thenReturn("999");
        when(definitions.getProcessDefinition("pinned-deployment")).thenReturn(d);
        assertThrows(
                RuntimeException.class,
                () ->
                        processes.createProcessInstance(
                                10L,
                                new BpmProcessInstanceCreateReqDTO()
                                        .setProcessDefinitionId("pinned-deployment")
                                        .setProcessDefinitionKey("guarded")
                                        .setBusinessKey("cycle-1")));
        verify(guard, never()).beforeStart(anyString(), anyLong(), anyString());
    }

    @Test
    void centralTaskValidationAlwaysCallsGuard() {
        TaskService engine = mock(TaskService.class);
        TaskQuery q = mock(TaskQuery.class);
        when(q.taskId("task-1")).thenReturn(q);
        when(q.includeTaskLocalVariables()).thenReturn(q);
        Task t = mock(Task.class);
        when(engine.createTaskQuery()).thenReturn(q);
        when(q.singleResult()).thenReturn(t);
        when(t.getAssignee()).thenReturn("11");
        ReflectionTestUtils.setField(tasks, "taskService", engine);
        doThrow(denied).when(guard).beforeTaskOperation(t, 11L);
        assertSame(
                denied,
                assertThrows(IllegalStateException.class, () -> tasks.validateTask(11L, "task-1")));
        verify(engine, never()).complete(anyString());
    }

    @Test
    void bothStarterAndAdministratorCancellationReachBusinessGuard() {
        ProcessInstance p = mock(ProcessInstance.class);
        when(p.getStartUserId()).thenReturn("10");
        when(p.getProcessDefinitionId()).thenReturn("pinned-deployment");
        doReturn(p).when(processes).getProcessInstance("instance-1");
        when(definitions.getProcessDefinitionInfo("pinned-deployment"))
                .thenReturn(new BpmProcessDefinitionInfoDO().setAllowCancelRunningProcess(true));
        doThrow(denied).when(guard).beforeCancel(p, 10L);
        BpmProcessInstanceCancelReqVO cmd =
                new BpmProcessInstanceCancelReqVO().setId("instance-1").setReason("test");
        assertSame(
                denied,
                assertThrows(
                        IllegalStateException.class,
                        () -> processes.cancelProcessInstanceByStartUser(10L, cmd)));
        assertSame(
                denied,
                assertThrows(
                        IllegalStateException.class,
                        () -> processes.cancelProcessInstanceByAdmin(10L, cmd)));
        verify(guard, times(2)).beforeCancel(p, 10L);
    }
    @Test
    void smsOptOutStillPublishesCompletionEventWithoutARequiredPhone() {
        cn.iocoder.yudao.module.bpm.service.message.BpmMessageService messages = mock(cn.iocoder.yudao.module.bpm.service.message.BpmMessageService.class);
        cn.iocoder.yudao.module.bpm.framework.flowable.core.event.BpmProcessInstanceEventPublisher publisher = mock(cn.iocoder.yudao.module.bpm.framework.flowable.core.event.BpmProcessInstanceEventPublisher.class);
        ReflectionTestUtils.setField(processes, "messageService", messages);
        ReflectionTestUtils.setField(processes, "processInstanceEventPublisher", publisher);
        ProcessInstance p = completed("guarded");
        when(guard.useGenericSms("guarded")).thenReturn(false);
        processes.processProcessInstanceCompleted(p);
        verifyNoInteractions(messages);
        verify(publisher).sendProcessInstanceResultEvent(argThat(e -> e.getStatus() == 2 && "cycle-1".equals(e.getBusinessKey())));
    }

    @Test
    void unregisteredBusinessKeysKeepDefaultSmsAndCompletionEvents() {
        cn.iocoder.yudao.module.bpm.service.message.BpmMessageService messages = mock(cn.iocoder.yudao.module.bpm.service.message.BpmMessageService.class);
        cn.iocoder.yudao.module.bpm.framework.flowable.core.event.BpmProcessInstanceEventPublisher publisher = mock(cn.iocoder.yudao.module.bpm.framework.flowable.core.event.BpmProcessInstanceEventPublisher.class);
        ReflectionTestUtils.setField(processes, "messageService", messages);
        ReflectionTestUtils.setField(processes, "processInstanceEventPublisher", publisher);
        BpmBusinessTaskGuard defaultPolicy = new BpmBusinessTaskGuard() { public void beforeTaskOperation(Task task, Long actor) {} };
        ReflectionTestUtils.setField(processes, "businessTaskGuards", Collections.singletonList(defaultPolicy));
        processes.processProcessInstanceCompleted(completed("oa_leave"));
        verify(messages).sendMessageWhenProcessInstanceApprove(any());
        verify(publisher).sendProcessInstanceResultEvent(any());
    }

    private ProcessInstance completed(String key) {
        ProcessInstance p = mock(ProcessInstance.class);
        when(p.getId()).thenReturn("instance-1");
        when(p.getBusinessKey()).thenReturn("cycle-1");
        when(p.getProcessDefinitionKey()).thenReturn(key);
        when(p.getProcessDefinitionId()).thenReturn("pinned-deployment");
        when(p.getStartUserId()).thenReturn("10");
        when(p.getProcessVariables()).thenReturn(Collections.singletonMap("PROCESS_STATUS", 2));
        return p;
    }

}
