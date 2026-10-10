package cn.iocoder.yudao.module.hrm.dal.dataobject.payroll;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
@Data @EqualsAndHashCode(callSuper=true)
@TableName("hrm_payroll_payment") @KeySequence("hrm_payroll_payment_seq")
public class HrmPayrollPaymentDO extends BaseDO {
    @TableId private Long id;
    private Long monthRecordId;
    private Long runId;
    private Long employeeId;
    private String employeeName;
    private String bankAccount;
    private String bankName;
    private java.math.BigDecimal amount;
    private String status;
    private String failureReason;
    private Integer attempt;
}
