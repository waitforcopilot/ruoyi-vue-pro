package cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.scheme;
import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.*;
@Data @EqualsAndHashCode(callSuper=true) @TableName("hrm_payroll_scheme_version")
public class HrmPayrollSchemeDO extends TenantBaseDO {
    @TableId private Long id;
    private Long groupId;private String groupName;private String title;private Integer schemeVersion;private Integer revision;private Integer status;
    @TableField(updateStrategy=FieldStrategy.ALWAYS) private String ownerName;
    @TableField(updateStrategy=FieldStrategy.ALWAYS) private String reference;
    @TableField(updateStrategy=FieldStrategy.ALWAYS) private LocalDate effectiveFrom;
    @TableField(updateStrategy=FieldStrategy.ALWAYS) private LocalDate effectiveTo;
    private Integer optionCount;private String sourceHash;private String snapshotJson;private LocalDateTime capturedAt;
    @TableField(updateStrategy=FieldStrategy.ALWAYS) private Long reviewedBy;
    @TableField(updateStrategy=FieldStrategy.ALWAYS) private String reviewedByName;
    @TableField(updateStrategy=FieldStrategy.ALWAYS) private LocalDateTime reviewedTime;
    @TableField(updateStrategy=FieldStrategy.ALWAYS) private String evidence;
}
