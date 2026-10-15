package cn.iocoder.yudao.module.hrm.dal.dataobject.payroll;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
@Data @EqualsAndHashCode(callSuper=true) @TableName("hrm_payroll_financial_plan") @KeySequence("hrm_payroll_financial_plan_seq")
public class HrmPayrollFinancialPlanDO extends BaseDO {
 @TableId private Long id;
 private Long entityId,priorPlanId;
 private Integer year,month,version;
 private String dimension,dimensionKey,evidence,sourceReference,status,requestKey,reviewReason;
 @com.fasterxml.jackson.annotation.JsonIgnore private String requestSnapshot;
 private java.math.BigDecimal budget,revenue,headcount;
 private Long submittedBy,reviewedBy;
 private java.time.LocalDateTime reviewedTime;
}
