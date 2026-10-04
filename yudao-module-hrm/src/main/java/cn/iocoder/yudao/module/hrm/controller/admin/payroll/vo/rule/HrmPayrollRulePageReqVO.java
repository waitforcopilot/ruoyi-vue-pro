package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.rule;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;
import javax.validation.constraints.*;
@Data
@EqualsAndHashCode(callSuper=true)
public class HrmPayrollRulePageReqVO extends PageParam {
    @Size(max=200) private String search;
    @Size(max=32) private String category;
    @Min(0) @Max(2) private Integer status;
    @Size(max=64) private String scopeCode;
}
