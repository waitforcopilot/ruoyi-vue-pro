package cn.iocoder.yudao.module.hrm.dal.dataobject.payroll;
import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "hrm_payroll_requirement", autoResultMap = true)
public class HrmPayrollRequirementDO extends TenantBaseDO {
    @TableId private Long id;
    private Integer version;
    private String code;
    private String moduleCode;
    private String title;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String description;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String ownerName;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Integer priority;
    private Integer scopeDecision;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String scopeReason;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String applicableScope;
    @TableField(typeHandler = JacksonTypeHandler.class, updateStrategy = FieldStrategy.ALWAYS)
    private List<String> sourceCodes;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String fieldMapping;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String acceptanceCriteria;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String remark;
    private Boolean builtIn;
    private String origin;
    private Integer status;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long reviewedBy;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String reviewedByName;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private java.time.LocalDateTime reviewedTime;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String evidence;
}
