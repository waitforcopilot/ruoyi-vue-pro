package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.trial;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import lombok.*;
import javax.validation.constraints.*;
@Data @EqualsAndHashCode(callSuper=true)
public class HrmPayrollTrialPageReqVO extends PageParam {
 @Size(max=64) private String entityCode;
 @Size(max=160) private String search;
 @Min(0) @Max(4) private Integer status;
}
