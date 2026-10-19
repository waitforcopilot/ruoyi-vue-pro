package cn.iocoder.yudao.module.hrm.dal.dataobject.payroll;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
@Data @EqualsAndHashCode(callSuper=true) @TableName("hrm_payroll_requirement_project_grant")
@KeySequence("hrm_payroll_requirement_project_grant_seq")
public class HrmPayrollRequirementProjectGrantDO extends BaseDO { @TableId private Long id; private Long projectId; private Long userId; private String actions; private String ceilingJson; private String evidence; private java.time.LocalDate validFrom; private java.time.LocalDate validTo; }
