package cn.iocoder.yudao.module.hrm.service.payroll.review;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.PAYROLL_REVIEW_PERMISSION;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.*;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;

class HrmPayrollBpmGuardTest {
    private final HrmPayrollBpmGuard guard = new HrmPayrollBpmGuard();

    private Task task(String key) {
        Task t = mock(Task.class);
        when(t.getProcessDefinitionId()).thenReturn(key + ":1:qa");
        when(t.getId()).thenReturn("task-1");
        when(t.getProcessInstanceId()).thenReturn("instance-1");
        return t;
    }

    @Test
    void payrollUsesProtectedTaskPageWithoutMandatoryGenericSms() {
        assertFalse(guard.useGenericSms(PayrollBpmContext.KEY));
        assertTrue(guard.useGenericSms("oa_leave"));
    }

    @Test
    void genericTaskOperationsCannotBypassVersionBoundReview() {
        assertServiceException(
                () -> guard.beforeTaskOperation(task(PayrollBpmContext.KEY), 11L),
                PAYROLL_REVIEW_PERMISSION);
    }

    @Test
    void genericStartsAndStarterOrAdminCancellationAreDenied() {
        assertServiceException(
                () -> guard.beforeStart(PayrollBpmContext.KEY, 10L, "1"),
                PAYROLL_REVIEW_PERMISSION);
        ProcessInstance p = mock(ProcessInstance.class);
        when(p.getProcessDefinitionId()).thenReturn(PayrollBpmContext.KEY + ":1:qa");
        assertServiceException(() -> guard.beforeCancel(p, 10L), PAYROLL_REVIEW_PERMISSION);
    }

    @Test
    void unrelatedBpmDefinitionsKeepTheirExistingBehavior() {
        assertDoesNotThrow(() -> guard.beforeTaskOperation(task("oa_leave"), 11L));
        assertDoesNotThrow(() -> guard.beforeStart("oa_leave", 10L, null));
        ProcessInstance p = mock(ProcessInstance.class);
        when(p.getProcessDefinitionId()).thenReturn("oa_leave:1:qa");
        assertDoesNotThrow(() -> guard.beforeCancel(p, 10L));
    }

    @Test
    void validatedCommandMustMatchActorTaskAndProcessInstance() {
        PayrollBpmContext.Frame f =
                new PayrollBpmContext.Frame()
                        .setAction("approve")
                        .setActorId(11L)
                        .setTaskId("task-1")
                        .setInstanceId("instance-1");
        try (PayrollBpmContext.Scope scope = PayrollBpmContext.open(f)) {
            assertDoesNotThrow(() -> guard.beforeTaskOperation(task(PayrollBpmContext.KEY), 11L));
            assertServiceException(
                    () -> guard.beforeTaskOperation(task(PayrollBpmContext.KEY), 12L),
                    PAYROLL_REVIEW_PERMISSION);
            f.setTaskId("another-task");
            assertServiceException(
                    () -> guard.beforeTaskOperation(task(PayrollBpmContext.KEY), 11L),
                    PAYROLL_REVIEW_PERMISSION);
        }
        assertNull(PayrollBpmContext.current());
    }

    @Test
    void nestedContextRestoresPreviousFrameAndAlwaysCleansUp() {
        PayrollBpmContext.Frame outer = new PayrollBpmContext.Frame().setAction("submit");
        try (PayrollBpmContext.Scope first = PayrollBpmContext.open(outer)) {
            assertThrows(
                    IllegalStateException.class,
                    () -> {
                        try (PayrollBpmContext.Scope second =
                                PayrollBpmContext.open(
                                        new PayrollBpmContext.Frame().setAction("approve"))) {
                            throw new IllegalStateException("synthetic");
                        }
                    });
            assertSame(outer, PayrollBpmContext.current());
        }
        assertNull(PayrollBpmContext.current());
    }

    @Test
    void trustedFrameNeverLeaksToAnotherThread() throws Exception {
        try (PayrollBpmContext.Scope scope =
                PayrollBpmContext.open(new PayrollBpmContext.Frame().setAction("approve"))) {
            ExecutorService pool = Executors.newSingleThreadExecutor();
            try {
                assertNull(pool.submit(PayrollBpmContext::current).get(5, TimeUnit.SECONDS));
            } finally {
                pool.shutdownNow();
            }
        }
    }
}
