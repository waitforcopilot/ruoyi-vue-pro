package cn.iocoder.yudao.module.hrm.dal.dataobject.payroll;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
@Data @EqualsAndHashCode(callSuper=true) @TableName("hrm_payroll_input_issue_event") @KeySequence("hrm_payroll_input_issue_event_seq")
public class HrmPayrollInputIssueEventDO extends BaseDO {
 @TableId private Long id;
 private Long issueId,actorId;
 private String action,reason,snapshot;
}
