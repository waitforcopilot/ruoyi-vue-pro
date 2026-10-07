package cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.trial;
import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import java.time.LocalDateTime;
@TableName("hrm_payroll_trial_run") @KeySequence("hrm_payroll_trial_run_seq")
@Data @EqualsAndHashCode(callSuper = true)
public class HrmPayrollTrialRunDO extends TenantBaseDO {
 @TableId private Long id;
 private Long batchId; private Integer runVersion; private String requestKey; private Integer expectedRevision;
 private String sourceHash; private String resultJson; private Integer includedCount; private Integer excludedCount;
 private Long executedBy; private String executedByName; private LocalDateTime executedAt;
}
