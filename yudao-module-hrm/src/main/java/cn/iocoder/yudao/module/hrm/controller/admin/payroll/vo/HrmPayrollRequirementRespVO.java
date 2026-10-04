package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo;
import lombok.Data;
import lombok.EqualsAndHashCode;
@Data
@EqualsAndHashCode(callSuper = true)
public class HrmPayrollRequirementRespVO extends HrmPayrollRequirementSaveReqVO {
    private Boolean builtIn;
    private String origin;
    private Integer status;
    private Long reviewedBy;
    private String reviewedByName;
    private java.time.LocalDateTime reviewedTime;
    private String evidence;
    private java.time.LocalDateTime createTime;
    private java.time.LocalDateTime updateTime;
}
