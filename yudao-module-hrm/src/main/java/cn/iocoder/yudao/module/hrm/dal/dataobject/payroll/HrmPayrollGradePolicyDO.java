package cn.iocoder.yudao.module.hrm.dal.dataobject.payroll;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
@Data @EqualsAndHashCode(callSuper=true) @TableName("hrm_payroll_grade_policy") @KeySequence("hrm_payroll_grade_policy_seq")
public class HrmPayrollGradePolicyDO extends BaseDO {
    @TableId private Long id;
    private String code;
    private String name;
    private Integer version;
    private Integer optionCode;
    private java.time.LocalDate effectiveFrom;
    private java.time.LocalDate effectiveTo;
    private String grades;
    private String postCoefficients;
    private String evidence;
    private String status;
    private Long approvedBy;
}
