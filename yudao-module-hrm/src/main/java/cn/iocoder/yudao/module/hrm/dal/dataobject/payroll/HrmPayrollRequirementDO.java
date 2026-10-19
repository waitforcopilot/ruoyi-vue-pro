package cn.iocoder.yudao.module.hrm.dal.dataobject.payroll;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.*;
import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hrm_payroll_requirement")
@KeySequence("hrm_payroll_requirement_seq")
public class HrmPayrollRequirementDO extends BaseDO {
    @TableId private Long id;
    @TableField(exist=false) @cn.idev.excel.annotation.ExcelIgnore private java.util.List<String> permittedActions;
    @TableField(exist=false) @ExcelProperty("当前确认绑定有效") private Boolean currentConfirmationValid;
    @ExcelProperty("项目编号") private Long projectId;
    private String templateItemId;
    private Integer templateVersion;
    private String scopeMode;
    private String scopeJson;
    private String scopeState;
    private Integer projectScopeVersion;
    private Integer confirmedScopeVersion;
    private String confirmedScopeJson;
    @ExcelProperty("范围决策") private String scopeDecision;
    @ExcelProperty("范围决策依据") private String scopeDecisionJson;
    @ExcelProperty("业务场景与页面上下文") private String businessContextJson;
    @ExcelProperty("独立实施关联") private String implementationJson;
    @ExcelProperty("评审协作记录") private String collaborationJson;
    private String legacyReviewStatus;
    @ExcelProperty("来源适用性") private String sourceApplicability;
    @ExcelProperty("不适用理由") private String applicabilityReason;
    @ExcelProperty("规则引用版本") private String ruleVersion;
    private Integer semanticVersion;
    private String confirmedTupleJson;
    @ExcelProperty("需求编号") private String code;
    @ExcelProperty("模块") private String module;
    @ExcelProperty("功能") private String description;
    @ExcelProperty("字段映射") private String fieldMapping;
    @ExcelProperty("数据来源") private String sourceSystem;
    @ExcelProperty("来源负责人") private Long sourceOwnerId;
    @ExcelProperty("评审负责人") private Long reviewerId;
    @ExcelProperty("优先级") private Integer priority;
    @ExcelProperty("评审状态") private String status;
    @ExcelProperty("数据就绪") private String readiness;
    @ExcelProperty("验收条件") private String acceptance;
    @ExcelProperty("确认依据及异议") private String evidence;
    @ExcelProperty("版本") private Integer version;
    @ExcelProperty("确认人") private Long confirmedBy;
    @ExcelProperty("确认时间") private java.time.LocalDateTime confirmedAt;
}
