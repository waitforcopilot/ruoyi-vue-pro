package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.overview;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.trial.HrmPayrollTrialCheckVO;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.*;
import java.util.*;
import lombok.Data;

@Data
public class HrmPayrollOverviewRespVO {
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime generatedAt;

    private Long total;
    private Map<Integer, Long> states = new LinkedHashMap<>();
    private Long assignedHr;
    private Long assignedFinance;
    private PageResult<Row> batches;

    @Data
    public static class Row {
        private Long id;
        private String code;
        private String title;
        private String entityCode;
        private String entityName;
        private String periodType;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        private LocalDate periodStart;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        private LocalDate periodEnd;

        private Integer status;
        private Integer revision;
        private Integer personCount;
        private String ownerName;
        private Long currentRunId;
        private Long latestRunId;
        private Long activeReviewId;
        private Long frozenRunId;
        private String stage;
        private Boolean assignedToMe = false;
    }

    @Data
    public static class Detail {
        private Row batch;
        private String availability;
        private Long runId;
        private Integer runVersion;
        private String executedByName;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime executedAt;

        private Integer includedCount;
        private Integer excludedCount;
        private Map<String, String> amounts;
        private HrmPayrollTrialCheckVO check;
    }
}
