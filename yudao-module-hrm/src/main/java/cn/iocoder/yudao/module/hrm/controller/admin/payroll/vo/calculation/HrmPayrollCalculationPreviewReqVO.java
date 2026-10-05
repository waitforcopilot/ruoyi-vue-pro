package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.calculation;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;
import javax.validation.constraints.*;
import java.time.LocalDate;
import java.util.Map;
@Data
public class HrmPayrollCalculationPreviewReqVO {
    @NotNull @Min(1) private Long definitionId;
    @NotNull @JsonFormat(pattern = "yyyy-MM-dd") private LocalDate start;
    @NotNull @JsonFormat(pattern = "yyyy-MM-dd") private LocalDate end;
    @NotNull @Size(max = 32) @JsonDeserialize(contentUsing = PlainDecimalStringDeserializer.class)
    private Map<@Size(max = 64) String, @Size(max = 40) String> inputs;
}
