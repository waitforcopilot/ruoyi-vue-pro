package cn.iocoder.yudao.module.hrm.dal.dataobject.payroll;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
@Data @EqualsAndHashCode(callSuper=true) @TableName("hrm_payroll_formula") @KeySequence("hrm_payroll_formula_seq")
public class HrmPayrollFormulaDO extends BaseDO {
    @TableId private Long id;
    private Integer optionCode;
    private Long salaryGroupId;
    private String expression;
    private java.time.LocalDate effectiveFrom;
    private java.time.LocalDate effectiveTo;
    private String evidence;
    private Long approvedBy;
    private String status;
}
