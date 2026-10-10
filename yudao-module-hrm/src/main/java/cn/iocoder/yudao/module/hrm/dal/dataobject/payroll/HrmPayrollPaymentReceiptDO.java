package cn.iocoder.yudao.module.hrm.dal.dataobject.payroll;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
@Data @EqualsAndHashCode(callSuper=true)
@TableName("hrm_payroll_payment_receipt") @KeySequence("hrm_payroll_payment_receipt_seq")
public class HrmPayrollPaymentReceiptDO extends BaseDO {
    @TableId private Long id;
    private Long monthRecordId;
    private String fileName;
    private String digest;
    private Integer rowCount;
    private Long actorId;
    private String details;
}
