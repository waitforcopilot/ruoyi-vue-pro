package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.review;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.*;
import lombok.Data;

@Data
public class HrmPayrollReviewRespVO {
    private Long batchId;
    private Integer revision;
    private Integer batchStatus;
    private Long activeReviewId;
    private Long frozenRunId;
    private Cycle cycle;
    private List<Task> tasks = new ArrayList<>();
    private List<Cycle> cycles = new ArrayList<>();

    @Data
    public static class Task {
        private String id;
        private String key;
        private String name;
        private Long assigneeId;
    }

    @Data
    public static class Cycle {
        private Long id;
        private Long runId;
        private Integer cycleVersion;
        private Integer status;
        private String sourceHash;
        private String processInstanceId;
        private String processDefinitionId;
        private Long startedBy;
        private String startedByName;
        private String submitEvidence;
        private Long hrReviewerId;
        private String hrReviewerName;
        private Long financeReviewerId;
        private String financeReviewerName;
        private String hrEvidence;
        private String financeEvidence;
        private String outcome;
        private Long frozenBy;
        private String frozenByName;
        private String freezeEvidence;
        private Long unfrozenBy;
        private String unfrozenByName;
        private String unfreezeEvidence;

        @com.fasterxml.jackson.databind.annotation.JsonDeserialize(
                using = com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer.class)
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime startedAt;

        @com.fasterxml.jackson.databind.annotation.JsonDeserialize(
                using = com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer.class)
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime hrReviewedAt;

        @com.fasterxml.jackson.databind.annotation.JsonDeserialize(
                using = com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer.class)
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime financeReviewedAt;

        @com.fasterxml.jackson.databind.annotation.JsonDeserialize(
                using = com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer.class)
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime finishedAt;

        @com.fasterxml.jackson.databind.annotation.JsonDeserialize(
                using = com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer.class)
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime frozenAt;

        @com.fasterxml.jackson.databind.annotation.JsonDeserialize(
                using = com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer.class)
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime unfrozenAt;
    }
}
