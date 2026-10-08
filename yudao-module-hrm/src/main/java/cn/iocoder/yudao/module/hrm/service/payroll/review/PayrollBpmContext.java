package cn.iocoder.yudao.module.hrm.service.payroll.review;

import lombok.Data;

/** Only server-validated payroll commands may mutate their BPM instance. Never supplied by HTTP. */
public final class PayrollBpmContext {
    public static final String KEY = "hrm_payroll_trial_review";
    private static final ThreadLocal<Frame> LOCAL = new ThreadLocal<>();

    private PayrollBpmContext() {}

    @Data
    public static class Frame {
        private String action;
        private Long actorId;
        private String businessKey;
        private String instanceId;
        private String taskId;
    }

    public static Frame current() {
        return LOCAL.get();
    }

    public static Scope open(Frame value) {
        Frame prior = LOCAL.get();
        LOCAL.set(value);
        return new Scope(prior);
    }

    public static final class Scope implements AutoCloseable {
        private final Frame prior;

        private Scope(Frame value) {
            prior = value;
        }

        @Override
        public void close() {
            if (prior == null) LOCAL.remove();
            else LOCAL.set(prior);
        }
    }
}
