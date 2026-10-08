package cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.review;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

@TableName("hrm_payroll_review_command")
@KeySequence("hrm_payroll_review_command_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class HrmPayrollReviewCommandDO extends TenantBaseDO {
    @TableId private Long id;
    private Long batchId;
    private String requestKey;
    private Long actorId;
    private String requestHash;
    private String responseJson;
}
