package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.identity;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import javax.validation.constraints.*;
import java.time.*;
@Data
public class HrmPayrollMappingLookupVO {
    @NotBlank @Size(max=128) private String externalCode;
    @NotNull private LocalDate start;
    @NotNull private LocalDate end;
    @Data
    public static class Match {
        private String externalCode;
        private Long mappingId;
        private Integer mappingVersion;
        private Long employeeId;
        @JsonFormat(shape=JsonFormat.Shape.STRING,pattern="yyyy-MM-dd'T'HH:mm:ss") private LocalDateTime snapshotCapturedAt;
        private String issueCode;
        private String message;
    }
}
