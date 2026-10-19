package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo;

import lombok.Data;
import javax.validation.constraints.*;
import java.util.*;

public interface HrmPayrollGovernanceVO {
    @Data class Root {
        @NotNull private Long projectId;
        @NotNull private Long anchorId;
        @NotNull private Long userId;
        @NotEmpty private List<String> actions;
        private List<String> delegableActions=Collections.emptyList();
        @NotBlank private String scopeJson;
        @NotBlank private String validFrom;
        @NotBlank private String validTo;
        @NotBlank @Size(max=128) private String materialVersion;
        @NotBlank @Size(max=100000) private String body;
        @NotBlank @Size(max=2000) private String reason;
    }
    @Data class Action {
        @NotNull private Long projectId;
        @NotNull private Long id;
        @NotNull private Integer version;
        @NotBlank private String action;
        @NotBlank @Size(max=4000) private String reason;
    }
    @Data class Decision {
        @NotNull private Long projectId;
        @NotNull private Integer projectScopeVersion;
        private Long previousId;
        @NotBlank @Size(max=200) private String title;
        @NotEmpty private List<String> questions;
        @NotBlank @Size(max=2000) private String clauses;
        @NotBlank @Size(max=128) private String clauseVersion;
        @NotBlank @Size(max=100000) private String body;
        @NotBlank private String scopeJson;
        @NotBlank private String validFrom;
        @NotBlank private String validTo;
        @NotNull private Long approverId;
        @NotBlank @Size(max=1000) private String approvalConditions;
        @NotNull private Boolean semanticChange;
        @NotNull private Map<String,String> otherQuestions;
        @NotBlank @Size(max=2000) private String reason;
    }
    @Data class Publish {
        @NotNull private Long projectId;
        @NotNull private Integer projectScopeVersion;
        @NotBlank @Size(max=100) private String requestKey;
        @NotBlank private String dependencyToken;
        @NotBlank @Size(max=4000) private String reason;
        @NotNull private Boolean draft;
    }
    @Data class Job {
        @NotNull private Long projectId;
        @NotNull private Long releaseId;
        private List<Long> requirementIds;
    }
}
