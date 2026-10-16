package cn.iocoder.yudao.module.hrm.dal.dataobject.payroll;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
@Data @EqualsAndHashCode(callSuper=true) @TableName("hrm_payroll_input_issue") @KeySequence("hrm_payroll_input_issue_seq")
public class HrmPayrollInputIssueDO extends BaseDO {
 @TableId private Long id;
 private Long batchId,entityId,employeeId,ownerId,reviewerId,submittedBy,proofBy;
 private String kind,status,reason,evidence,reviewOpinion,requestKey,sourceSnapshot,submittedSnapshot,closedSnapshot,reminderState,reminderError;
 @com.fasterxml.jackson.annotation.JsonIgnore private String requestSnapshot;
 private java.time.LocalDateTime dueTime,closedTime;
 @TableField(exist=false) private Boolean overdue,sourceReady;
 @TableField(exist=false) private Boolean canSubmit,canReview,canReassign;
 @TableField(exist=false) private String sourceMode;
}
