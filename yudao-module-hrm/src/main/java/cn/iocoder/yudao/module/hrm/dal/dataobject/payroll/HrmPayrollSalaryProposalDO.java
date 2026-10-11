package cn.iocoder.yudao.module.hrm.dal.dataobject.payroll;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
@Data @EqualsAndHashCode(callSuper=true) @TableName("hrm_payroll_salary_proposal") @KeySequence("hrm_payroll_salary_proposal_seq")
public class HrmPayrollSalaryProposalDO extends BaseDO {
    @TableId private Long id;
    private Long employeeId;
    private String requestSnapshot;
    private String beforeSnapshot;
    private String status;
    private Long proposedBy;
    private Long reviewedBy;
    private Long approvedBy;
    private String reviewReason;
    private String approvalReason;
    private Long changeRecordId;
}
