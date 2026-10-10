package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo;
import lombok.Data;
import javax.validation.Valid;
import javax.validation.constraints.*;
import java.util.List;
@Data
public class HrmPayrollBankTemplateReqVO {
    @NotBlank @Size(max=100) private String name;
    @NotNull @Size(min=5,max=12) @Valid private List<Column> columns;
    @NotNull @Size(min=5,max=12) @Valid private List<Column> returnColumns;
    @NotBlank @Size(max=30) private String successValue;
    @NotBlank @Size(max=30) private String failedValue;
    @Data public static class Column {
        @NotBlank @Pattern(regexp="id|employeeName|bankName|bankAccount|amount|attempt|status|failureReason") private String field;
        @NotBlank @Size(max=100) private String label;
    }
}
