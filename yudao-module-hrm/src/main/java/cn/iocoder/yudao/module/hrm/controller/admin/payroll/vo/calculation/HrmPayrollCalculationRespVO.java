package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.calculation;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
@Data @EqualsAndHashCode(callSuper = true)
public class HrmPayrollCalculationRespVO extends HrmPayrollCalculationSaveReqVO {
    private Integer definitionVersion;
    private Integer schemaVersion;
    private Integer status;
    private Integer inputCount;
    private Integer itemCount;
    private Integer caseCount;
    private HrmPayrollCalculationCasesVO verifiedCases;
    private Long reviewedBy;
    private String reviewedByName;
    private LocalDateTime reviewedTime;
    private LocalDateTime createTime;
    private String evidence;
}
