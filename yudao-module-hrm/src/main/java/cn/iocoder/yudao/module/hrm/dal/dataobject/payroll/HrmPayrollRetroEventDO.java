package cn.iocoder.yudao.module.hrm.dal.dataobject.payroll;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
@Data @EqualsAndHashCode(callSuper=true)
@TableName("hrm_payroll_retro_event") @KeySequence("hrm_payroll_retro_event_seq")
public class HrmPayrollRetroEventDO extends BaseDO {
 @TableId private Long id;
 private Long retroId, actorId;
 private String action, reason, snapshot;
}
