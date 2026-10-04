package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.intake;

import lombok.Data;
import javax.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

@Data
public class HrmPayrollPreviewReqVO {
    @NotNull private Long contractId;
    @NotBlank @Size(max = 120) private String declaredScope;
    @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) private LocalDate periodStart;
    @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) private LocalDate periodEnd;
}
