package cn.iocoder.yudao.module.hrm.dal.dataobject.payroll;
import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.EqualsAndHashCode;
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hrm_payroll_baseline")
public class HrmPayrollBaselineDO extends TenantBaseDO {
    @TableId
    private Long id;
    private Integer requirementCount;
    private Integer sourceCount;
    private Long exportedBy;
    private String exportedByName;
    private String snapshot;
}
