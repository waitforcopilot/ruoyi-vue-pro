package cn.iocoder.yudao.module.hrm.dal.dataobject.payroll;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
@Data @EqualsAndHashCode(callSuper=true) @TableName("hrm_payroll_cost_rule") @KeySequence("hrm_payroll_cost_rule_seq")
public class HrmPayrollCostRuleDO extends BaseDO {
 @TableId private Long id;
 private Long entityId,employeeId,priorRuleId;
 private Integer version;
 private java.time.LocalDate effectiveFrom,effectiveTo;
 private String parts,evidence,status,requestKey,reviewReason;
 @com.fasterxml.jackson.annotation.JsonIgnore private String requestSnapshot;
 private java.math.BigDecimal employerCost;
 private Long submittedBy,reviewedBy;
 private java.time.LocalDateTime reviewedTime;
}
