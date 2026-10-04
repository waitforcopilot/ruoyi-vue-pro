package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class HrmPayrollBaselineRespVO {
    private Long id;
    private Integer requirementCount;
    private Integer sourceCount;
    private Long exportedBy;
    private String exportedByName;
    private LocalDateTime createTime;
}
