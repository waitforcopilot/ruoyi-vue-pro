package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.preparation;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Preparation metadata only: never include a CSV snapshot, salary, or rule parameter. */
@Data
public class HrmPayrollPreparationRespVO {
    private String generatedAt;
    private Section requirements;
    private Section sources;
    private Section contracts;
    private Section rules;
    private Section batches;
    @Data
    public static class Section {
        private Boolean authorized;
        // Null when unauthorized, rather than a misleading zero.
        private Map<String, Long> counts;
        private List<ModuleCount> modules;
        private List<SourceState> sources;
        private List<Batch> recentBatches;
    }
    @Data
    public static class ModuleCount {
        private String code;
        private String name;
        private Long total;
        private Long pending;
        private Long confirmed;
        private Long disputed;
        private Long inScope;
        private Long inScopeUnconfirmed;
        private Long undecided;
    }
    @Data
    public static class SourceState {
        private String code;
        private String name;
        private Integer readiness;
    }
    @Data
    public static class Batch {
        private Long id;
        private String sourceCode;
        private Integer contractVersion;
        @JsonFormat(shape=JsonFormat.Shape.STRING, pattern="yyyy-MM-dd") private LocalDate periodStart;
        @JsonFormat(shape=JsonFormat.Shape.STRING, pattern="yyyy-MM-dd") private LocalDate periodEnd;
        private Integer rowCount;
        private Integer validCount;
        private Integer errorCount;
        private Integer status;
    }
}
