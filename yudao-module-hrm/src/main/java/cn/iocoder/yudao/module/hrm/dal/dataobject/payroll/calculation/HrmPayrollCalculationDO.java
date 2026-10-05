package cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.calculation;
import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.*;
@Data @EqualsAndHashCode(callSuper = true) @TableName("hrm_payroll_calculation_definition")
public class HrmPayrollCalculationDO extends TenantBaseDO {
    @TableId private Long id;
    private String code;
    private String title;
    private Integer definitionVersion;
    private Integer schemaVersion;
    private Integer revision;
    private Integer status;
    @TableField(updateStrategy = FieldStrategy.ALWAYS) private String ownerName;
    private String scopeCode;
    @TableField(updateStrategy = FieldStrategy.ALWAYS) private String applicableScope;
    @TableField(updateStrategy = FieldStrategy.ALWAYS) private String description;
    @TableField(updateStrategy = FieldStrategy.ALWAYS) private String reference;
    @TableField(updateStrategy = FieldStrategy.ALWAYS) private LocalDate effectiveFrom;
    @TableField(updateStrategy = FieldStrategy.ALWAYS) private LocalDate effectiveTo;
    private String programJson;
    private Integer inputCount;
    private Integer itemCount;
    private Integer caseCount;
    @TableField(updateStrategy = FieldStrategy.ALWAYS) private String verificationJson;
    @TableField(updateStrategy = FieldStrategy.ALWAYS) private Long reviewedBy;
    @TableField(updateStrategy = FieldStrategy.ALWAYS) private String reviewedByName;
    @TableField(updateStrategy = FieldStrategy.ALWAYS) private LocalDateTime reviewedTime;
    @TableField(updateStrategy = FieldStrategy.ALWAYS) private String evidence;
}
