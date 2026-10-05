package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.insurance;

import lombok.Data;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class HrmPayrollInsurancePreviewReqVO {
    @NotNull @Min(1)
    private Long policyId;
    @NotNull
    private LocalDate start;
    @NotNull
    private LocalDate end;
    @NotNull @DecimalMin("0") @Digits(integer = 10, fraction = 2)
    private BigDecimal baseAmount;
}
