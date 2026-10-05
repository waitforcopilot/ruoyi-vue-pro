package cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.eligibility;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.*;

/** An explicitly declared entity/person qualification, retaining the observed personnel snapshot. */
@TableName("hrm_payroll_employee_eligibility")
@KeySequence("hrm_payroll_employee_eligibility_seq")
@Data @EqualsAndHashCode(callSuper = true)
public class HrmPayrollEligibilityDO extends TenantBaseDO {
    @TableId private Long id;
    private String entityCode;
    private String entityName;
    private Long employeeId;
    private Integer eligibilityVersion;
    private Integer revision;
    private Integer status;
    @TableField(updateStrategy = FieldStrategy.ALWAYS) private String qualification;
    @TableField(updateStrategy = FieldStrategy.ALWAYS) private String ownerName;
    @TableField(updateStrategy = FieldStrategy.ALWAYS) private String reference;
    @TableField(updateStrategy = FieldStrategy.ALWAYS) private String reason;
    @TableField(updateStrategy = FieldStrategy.ALWAYS) private LocalDate effectiveFrom;
    @TableField(updateStrategy = FieldStrategy.ALWAYS) private LocalDate effectiveTo;
    @TableField(updateStrategy = FieldStrategy.ALWAYS) private String snapshotName;
    @TableField(updateStrategy = FieldStrategy.ALWAYS) private String snapshotJobNumber;
    @TableField(updateStrategy = FieldStrategy.ALWAYS) private Long snapshotDeptId;
    @TableField(updateStrategy = FieldStrategy.ALWAYS) private Long snapshotUserId;
    @TableField(updateStrategy = FieldStrategy.ALWAYS) private LocalDateTime snapshotEntryTime;
    @TableField(updateStrategy = FieldStrategy.ALWAYS) private LocalDateTime snapshotLeaveTime;
    @TableField(updateStrategy = FieldStrategy.ALWAYS) private Integer snapshotEmployeeStatus;
    private LocalDateTime snapshotCapturedAt;
    private String employeeFingerprint;
    @TableField(updateStrategy = FieldStrategy.ALWAYS) private Long reviewedBy;
    @TableField(updateStrategy = FieldStrategy.ALWAYS) private String reviewedByName;
    @TableField(updateStrategy = FieldStrategy.ALWAYS) private LocalDateTime reviewedTime;
    @TableField(updateStrategy = FieldStrategy.ALWAYS) private String evidence;
}
