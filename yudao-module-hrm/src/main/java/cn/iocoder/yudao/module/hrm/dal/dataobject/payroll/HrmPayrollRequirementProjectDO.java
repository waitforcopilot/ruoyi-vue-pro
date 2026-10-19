package cn.iocoder.yudao.module.hrm.dal.dataobject.payroll;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
@Data @EqualsAndHashCode(callSuper=true)
@TableName("hrm_payroll_requirement_project")
@KeySequence("hrm_payroll_requirement_project_seq")
public class HrmPayrollRequirementProjectDO extends BaseDO {
 @TableId private Long id;
 private String projectKey;
 private String name;
 private Integer metadataVersion;
 // Null means legacy scope has not been defined or approved, not scope version 1.
 private Integer projectScopeVersion;
 private Long ownerId;
 private Long customSequence;
 private String scopeJson;
 private String catalogKey;
 private Boolean archived;
}
