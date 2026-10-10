package cn.iocoder.yudao.module.hrm.dal.dataobject.payroll;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** Immutable bank file format. Changing a format creates a new template. */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hrm_payroll_bank_template")
@KeySequence("hrm_payroll_bank_template_seq")
public class HrmPayrollBankTemplateDO extends BaseDO {
    @TableId private Long id;
    private String name;
    private String columns;
    private String returnColumns;
    private String successValue;
    private String failedValue;
}
