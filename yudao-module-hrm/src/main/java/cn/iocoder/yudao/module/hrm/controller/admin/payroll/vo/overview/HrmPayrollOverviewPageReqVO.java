package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.overview;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import java.time.LocalDate;
import javax.validation.constraints.*;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

@Data
@EqualsAndHashCode(callSuper = true)
public class HrmPayrollOverviewPageReqVO extends PageParam {
    @Size(max = 64)
    private String entityCode;

    @Size(max = 160)
    private String search;

    @Min(0)
    @Max(4)
    private Integer status;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate periodStart;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate periodEnd;
}
