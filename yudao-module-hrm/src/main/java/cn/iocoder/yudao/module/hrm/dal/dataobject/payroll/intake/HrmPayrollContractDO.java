package cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.intake;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hrm_payroll_source_contract")
public class HrmPayrollContractDO extends TenantBaseDO {
    @TableId private Long id;
    private Long sourceId;
    private String sourceCode;
    private String sourceName;
    private Integer contractVersion;
    private Integer revision;
    private Integer status;
    private String title;
    @TableField(updateStrategy = FieldStrategy.ALWAYS) private String actualSystem;
    @TableField(updateStrategy = FieldStrategy.ALWAYS) private String ownerName;
    @TableField(updateStrategy = FieldStrategy.ALWAYS) private String applicableScope;
    @TableField(updateStrategy = FieldStrategy.ALWAYS) private String evidence;
    private String schemaJson;
    private Integer fieldCount;
    @TableField(updateStrategy = FieldStrategy.ALWAYS) private Long reviewedBy;
    @TableField(updateStrategy = FieldStrategy.ALWAYS) private String reviewedByName;
    @TableField(updateStrategy = FieldStrategy.ALWAYS) private LocalDateTime reviewedTime;
}
