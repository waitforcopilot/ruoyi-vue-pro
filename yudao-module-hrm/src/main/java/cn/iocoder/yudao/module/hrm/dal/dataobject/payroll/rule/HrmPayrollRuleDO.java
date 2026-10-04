package cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.rule;
import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;
import java.time.LocalDateTime;
@Data
@EqualsAndHashCode(callSuper=true)
@TableName("hrm_payroll_rule")
public class HrmPayrollRuleDO extends TenantBaseDO {
    @TableId private Long id;
    private String code;
    private String title;
    private String category;
    @TableField(updateStrategy=FieldStrategy.ALWAYS) private String questionCode;
    private Integer ruleVersion;
    private Integer revision;
    private Integer status;
    private Boolean builtIn;
    @TableField(updateStrategy=FieldStrategy.ALWAYS) private String ownerName;
    @TableField(updateStrategy=FieldStrategy.ALWAYS) private String scopeCode;
    @TableField(updateStrategy=FieldStrategy.ALWAYS) private String applicableScope;
    @TableField(updateStrategy=FieldStrategy.ALWAYS) private LocalDate effectiveFrom;
    @TableField(updateStrategy=FieldStrategy.ALWAYS) private LocalDate effectiveTo;
    @TableField(updateStrategy=FieldStrategy.ALWAYS) private String definition;
    @TableField(updateStrategy=FieldStrategy.ALWAYS) private String reference;
    private String parametersJson;
    private String casesJson;
    private Integer parameterCount;
    private Integer caseCount;
    @TableField(updateStrategy=FieldStrategy.ALWAYS) private Long reviewedBy;
    @TableField(updateStrategy=FieldStrategy.ALWAYS) private String reviewedByName;
    @TableField(updateStrategy=FieldStrategy.ALWAYS) private LocalDateTime reviewedTime;
    @TableField(updateStrategy=FieldStrategy.ALWAYS) private String evidence;
}
