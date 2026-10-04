package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.intake;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class HrmPayrollContractRespVO extends HrmPayrollContractSaveReqVO {
    private Integer contractVersion;
    private Integer fieldCount;
    private String sourceCode;
    private String sourceName;
    private Integer status;
    private String reviewedByName;
    private LocalDateTime reviewedTime;
    private LocalDateTime createTime;
}
