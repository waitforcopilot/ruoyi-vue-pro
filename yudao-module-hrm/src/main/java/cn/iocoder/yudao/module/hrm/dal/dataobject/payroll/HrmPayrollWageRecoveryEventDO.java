package cn.iocoder.yudao.module.hrm.dal.dataobject.payroll;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
@Data @EqualsAndHashCode(callSuper=true) @TableName("hrm_payroll_wage_recovery_event") @KeySequence("hrm_payroll_wage_recovery_event_seq")
public class HrmPayrollWageRecoveryEventDO extends BaseDO { @TableId private Long id;private Long recoveryId,actorId;private String action,reason,snapshot; }
