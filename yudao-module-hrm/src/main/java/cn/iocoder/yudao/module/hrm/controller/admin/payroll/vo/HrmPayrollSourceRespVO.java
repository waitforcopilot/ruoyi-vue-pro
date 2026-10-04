package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo;
import lombok.Data;
import lombok.EqualsAndHashCode;
@Data
@EqualsAndHashCode(callSuper = true)
public class HrmPayrollSourceRespVO extends HrmPayrollSourceSaveReqVO {
    private Boolean builtIn;
    private Long confirmedBy;
    private String confirmedByName;
    private java.time.LocalDateTime confirmedTime;
    private java.time.LocalDateTime createTime;
    private java.time.LocalDateTime updateTime;
}
