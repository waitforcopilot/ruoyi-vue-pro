package cn.iocoder.yudao.module.hrm.dal.dataobject.payroll;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
@Data @EqualsAndHashCode(callSuper=true) @TableName("hrm_payroll_requirement_template")
@KeySequence("hrm_payroll_requirement_template_seq")
public class HrmPayrollRequirementTemplateDO extends BaseDO { @TableId private Long id; private String catalogKey; private String templateItemId; private Integer templateVersion; private String originalCode; private String description; private Boolean withdrawn; }
