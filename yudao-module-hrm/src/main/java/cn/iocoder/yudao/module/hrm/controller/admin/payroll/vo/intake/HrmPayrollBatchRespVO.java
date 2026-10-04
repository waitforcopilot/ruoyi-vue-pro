package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.intake;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class HrmPayrollBatchRespVO {
    private Long id;
    private Long contractId;
    private String sourceCode;
    private Integer contractVersion;
    private String fileName;
    private String fileHash;
    private String declaredScope;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") private LocalDate periodStart;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") private LocalDate periodEnd;
    private Integer rowCount;
    private Integer validCount;
    private Integer errorCount;
    private Integer status;
    private String createdByName;
    private LocalDateTime createTime;
}
