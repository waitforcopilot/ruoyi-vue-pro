package cn.iocoder.yudao.module.hrm.dal.dataobject.payroll;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** Controlled roots, immutable decision versions, release snapshots and export jobs. */
@Data @EqualsAndHashCode(callSuper=true)
@TableName("hrm_payroll_governance_record")
@KeySequence("hrm_payroll_governance_record_seq")
public class HrmPayrollGovernanceRecordDO extends BaseDO {
    @TableId(type=IdType.AUTO) private Long id;
    private Long projectId;
    private String kind;
    private String objectKey;
    private Integer version;
    private String dataJson;
}
