package cn.iocoder.yudao.module.hrm.dal.dataobject.payroll;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
@Data @EqualsAndHashCode(callSuper=true)
@TableName("hrm_payroll_batch_event") @KeySequence("hrm_payroll_batch_event_seq")
public class HrmPayrollBatchEventDO extends BaseDO {
    @TableId private Long id;
    private Long monthRecordId;
    private Long runId;
    private String action;
    private Integer fromStatus;
    private Integer toStatus;
    private Long actorId;
    private String reason;
}
