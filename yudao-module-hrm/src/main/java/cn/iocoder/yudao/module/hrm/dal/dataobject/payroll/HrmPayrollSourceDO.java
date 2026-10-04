package cn.iocoder.yudao.module.hrm.dal.dataobject.payroll;
import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "hrm_payroll_source", autoResultMap = true)
public class HrmPayrollSourceDO extends TenantBaseDO {
    @TableId private Long id;
    private Integer version;
    private String code;
    private String name;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String description;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String requiredFields;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String actualSystem;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String fieldMapping;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String ownerName;
    private Integer readiness;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String evidence;
    private Boolean builtIn;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long confirmedBy;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String confirmedByName;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private java.time.LocalDateTime confirmedTime;
}
