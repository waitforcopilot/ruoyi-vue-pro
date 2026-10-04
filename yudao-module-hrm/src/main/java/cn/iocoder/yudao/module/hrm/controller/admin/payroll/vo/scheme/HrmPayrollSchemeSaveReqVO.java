package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.scheme;
import lombok.Data;
import javax.validation.constraints.*;
import java.time.LocalDate;
@Data
public class HrmPayrollSchemeSaveReqVO {
    private Long id;private Integer revision;
    @NotNull @Min(1) private Long groupId;
    @NotBlank @Size(max=160) private String title;
    @Size(max=120) private String ownerName;
    @Size(max=2000) private String reference;
    private LocalDate effectiveFrom;private LocalDate effectiveTo;
    @Size(max=64) private String expectedSourceHash;
}
