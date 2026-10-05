package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.calculation;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDate;
@Data
public class HrmPayrollCalculationPreviewVO {
    private HrmPayrollCalculationRespVO definition;
    private String programHash;
    @JsonFormat(pattern = "yyyy-MM-dd") private LocalDate start;
    @JsonFormat(pattern = "yyyy-MM-dd") private LocalDate end;
    private HrmPayrollCalculationResultVO result;
    private String explanation;
}
