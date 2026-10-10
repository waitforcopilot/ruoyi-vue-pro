package cn.iocoder.yudao.module.hrm.dal.dataobject.payroll;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hrm_payroll_requirement_history")
@KeySequence("hrm_payroll_requirement_history_seq")
public class HrmPayrollRequirementHistoryDO extends BaseDO {
    @TableId private Long id;
    private Long requirementId;
    private Integer version;
    private String snapshot;
    private Long actorId;
}
