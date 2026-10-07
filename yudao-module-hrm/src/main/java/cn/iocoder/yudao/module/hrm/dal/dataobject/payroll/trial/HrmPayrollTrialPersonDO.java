package cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.trial;
import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
/** Retain every personnel snapshot ever involved in this batch's confidentiality boundary. */
@TableName("hrm_payroll_trial_person") @KeySequence("hrm_payroll_trial_person_seq")
@Data @EqualsAndHashCode(callSuper = true)
public class HrmPayrollTrialPersonDO extends TenantBaseDO {
 @TableId private Long id;
 private Long batchId; private Long employeeId; private Long snapshotDeptId; private Long snapshotUserId;
 private String employeeFingerprint;
}
