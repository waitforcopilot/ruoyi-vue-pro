package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.rule;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
@Data
@EqualsAndHashCode(callSuper=true)
public class HrmPayrollRuleRespVO extends HrmPayrollRuleSaveReqVO {
    private Integer ruleVersion;
    private Integer status;
    private Integer parameterCount;
    private Integer caseCount;
    private Boolean builtIn;
    private String reviewedByName;
    private LocalDateTime reviewedTime;
    private String evidence;
}
