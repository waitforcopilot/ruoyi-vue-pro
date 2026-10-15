package cn.iocoder.yudao.module.hrm.dal.dataobject.payroll;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
@Data @EqualsAndHashCode(callSuper=true) @TableName("hrm_payroll_cost_event") @KeySequence("hrm_payroll_cost_event_seq")
public class HrmPayrollCostEventDO extends BaseDO {
 @TableId private Long id;
 private Long targetId,actorId;
 private String kind,action,reason,snapshot;
}
