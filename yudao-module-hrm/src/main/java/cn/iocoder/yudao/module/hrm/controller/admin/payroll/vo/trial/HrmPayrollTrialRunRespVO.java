package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.trial;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;
@Data
public class HrmPayrollTrialRunRespVO {
 private Long id; private Long batchId; private Integer runVersion;
 private String sourceHash; private Integer expectedRevision; private Integer includedCount; private Integer excludedCount;
 private Long executedBy;
 private String executedByName;
 @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using=com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer.class)
 @JsonFormat(shape=JsonFormat.Shape.STRING,pattern="yyyy-MM-dd'T'HH:mm:ss") private LocalDateTime executedAt;
 private HrmPayrollTrialResultVO result;
}
