package cn.iocoder.yudao.module.hrm.dal.dataobject.payroll;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
@Data @EqualsAndHashCode(callSuper=true)
@TableName("hrm_payroll_run") @KeySequence("hrm_payroll_run_seq")
public class HrmPayrollRunDO extends BaseDO {
    @TableId private Long id;
    private Long monthRecordId;
    private Integer version;
    private String inputSnapshot;
    private String ruleSnapshot;
    private String resultSnapshot;
    private Long computedBy;
}
