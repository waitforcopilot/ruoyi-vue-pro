package cn.iocoder.yudao.module.hrm.dal.dataobject.payroll;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
@TableName("hrm_payroll_tax_settlement_event") @KeySequence("hrm_payroll_tax_settlement_event_seq")
@Data @EqualsAndHashCode(callSuper=true)
public class HrmPayrollTaxSettlementEventDO extends BaseDO {
 @TableId private Long id;
 private Long settlementId;
 private Long actorId;
 private String action;
 private String reason;
 private String snapshot;
}
