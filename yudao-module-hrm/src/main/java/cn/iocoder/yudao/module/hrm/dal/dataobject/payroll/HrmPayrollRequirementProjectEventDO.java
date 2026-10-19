package cn.iocoder.yudao.module.hrm.dal.dataobject.payroll;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
@Data @EqualsAndHashCode(callSuper=true) @TableName("hrm_payroll_requirement_project_event")
@KeySequence("hrm_payroll_requirement_project_event_seq")
public class HrmPayrollRequirementProjectEventDO extends BaseDO { @TableId private Long id; private Long projectId; private Long actorId; private String action; private String snapshot; }
