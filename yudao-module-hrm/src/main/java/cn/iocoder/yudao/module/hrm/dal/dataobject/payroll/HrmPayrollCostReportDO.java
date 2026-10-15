package cn.iocoder.yudao.module.hrm.dal.dataobject.payroll;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
@Data @EqualsAndHashCode(callSuper=true) @TableName("hrm_payroll_cost_report") @KeySequence("hrm_payroll_cost_report_seq")
public class HrmPayrollCostReportDO extends BaseDO {
 @TableId private Long id;
 private Long batchId,runId;
 private Integer version;
 private String status,inputSnapshot,resultSnapshot,sourceDigest,reason,reviewReason;
 private Long computedBy,reviewedBy;
 private java.time.LocalDateTime reviewedTime;
}
