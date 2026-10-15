package cn.iocoder.yudao.module.hrm.dal.dataobject.payroll;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
@Data @EqualsAndHashCode(callSuper=true) @TableName("hrm_payroll_financial_event") @KeySequence("hrm_payroll_financial_event_seq")
public class HrmPayrollFinancialEventDO extends BaseDO {
 @TableId private Long id;
 private Long entityId,planId,actorId;
 private Integer year,month;
 private String action,reason,snapshot;
}
