package cn.iocoder.yudao.module.hrm.dal.dataobject.payroll;
import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.EqualsAndHashCode;
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hrm_payroll_review")
public class HrmPayrollReviewDO extends TenantBaseDO {
    @TableId
    private Long id;
    private String objectType;
    private Long objectId;
    private String action;
    private Integer fromVersion;
    private Integer toVersion;
    private Long actorId;
    private String actorName;
    private String reason;
    private String beforeSnapshot;
    private String afterSnapshot;
}
