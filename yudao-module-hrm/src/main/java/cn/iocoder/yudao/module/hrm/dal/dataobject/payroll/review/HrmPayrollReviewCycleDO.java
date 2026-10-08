package cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.review;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
import lombok.*;

@TableName("hrm_payroll_review_cycle")
@KeySequence("hrm_payroll_review_cycle_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class HrmPayrollReviewCycleDO extends TenantBaseDO {
    @TableId private Long id;
    private Long batchId;
    private Long runId;
    private Integer cycleVersion;
    private Integer status;
    private String sourceHash;
    private String processInstanceId;
    private String processDefinitionId;
    private Long startedBy;
    private String startedByName;
    private String submitEvidence;
    private LocalDateTime startedAt;
    private Long hrReviewerId;
    private String hrReviewerName;
    private Long financeReviewerId;
    private String financeReviewerName;
    private LocalDateTime hrReviewedAt;
    private String hrEvidence;
    private LocalDateTime financeReviewedAt;
    private String financeEvidence;
    private String outcome;
    private LocalDateTime finishedAt;
    private Long frozenBy;
    private String frozenByName;
    private String freezeEvidence;
    private LocalDateTime frozenAt;
    private Long unfrozenBy;
    private String unfrozenByName;
    private String unfreezeEvidence;
    private LocalDateTime unfrozenAt;
}
