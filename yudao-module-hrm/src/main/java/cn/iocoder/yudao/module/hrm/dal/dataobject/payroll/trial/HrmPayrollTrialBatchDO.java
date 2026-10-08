package cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.trial;
import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import java.time.LocalDate;
@TableName("hrm_payroll_trial_batch") @KeySequence("hrm_payroll_trial_batch_seq")
@Data @EqualsAndHashCode(callSuper = true)
public class HrmPayrollTrialBatchDO extends TenantBaseDO {
 @TableId private Long id;
 private String code; private String title; private String entityCode; private String entityName;
 private String periodType; private LocalDate periodStart; private LocalDate periodEnd;
 private Long definitionId; private Integer revision; private Integer status; private Integer personCount;
 private Long schemeId;
 @TableField(updateStrategy=FieldStrategy.ALWAYS) private String ownerName;
 @TableField(updateStrategy=FieldStrategy.ALWAYS) private String reference;
 private String configurationJson;
 @TableField(updateStrategy=FieldStrategy.ALWAYS) private Long currentRunId;
 private Long latestRunId;
 @TableField(updateStrategy=FieldStrategy.ALWAYS) private Long activeReviewId;
 @TableField(updateStrategy=FieldStrategy.ALWAYS) private Long frozenRunId;
}
