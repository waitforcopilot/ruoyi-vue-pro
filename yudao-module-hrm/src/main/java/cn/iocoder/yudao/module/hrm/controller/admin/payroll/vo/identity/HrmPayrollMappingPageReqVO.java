package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.identity;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;
import javax.validation.constraints.*;
@Data
@EqualsAndHashCode(callSuper=true)
public class HrmPayrollMappingPageReqVO extends PageParam {
    private Long sourceId;
    @Size(max=64) private String namespace;
    @Size(max=128) private String externalCode;
    @Min(0) @Max(2) private Integer status;
}
