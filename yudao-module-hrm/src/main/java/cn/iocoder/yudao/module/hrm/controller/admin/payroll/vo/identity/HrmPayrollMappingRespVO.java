package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.identity;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.*;
@Data
public class HrmPayrollMappingRespVO {
    private Long id; private Long sourceId; private String sourceCode; private String namespace; private String externalCode;
    private Integer mappingVersion; private Integer revision; private Integer status; private Long employeeId;
    private String ownerName; private String reference;
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using=com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer.class)
 @JsonFormat(shape=JsonFormat.Shape.STRING,pattern="yyyy-MM-dd") private LocalDate effectiveFrom;
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using=com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer.class)
 @JsonFormat(shape=JsonFormat.Shape.STRING,pattern="yyyy-MM-dd") private LocalDate effectiveTo;
    private String snapshotName; private String snapshotJobNumber; private Long snapshotDeptId; private Long snapshotUserId;
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using=com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer.class)
 @JsonFormat(shape=JsonFormat.Shape.STRING,pattern="yyyy-MM-dd'T'HH:mm:ss") private LocalDateTime snapshotEntryTime;
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using=com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer.class)
 @JsonFormat(shape=JsonFormat.Shape.STRING,pattern="yyyy-MM-dd'T'HH:mm:ss") private LocalDateTime snapshotLeaveTime;
    private Integer snapshotEmployeeStatus;
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using=com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer.class)
 @JsonFormat(shape=JsonFormat.Shape.STRING,pattern="yyyy-MM-dd'T'HH:mm:ss") private LocalDateTime snapshotCapturedAt;
    private String employeeFingerprint; private String reviewedByName;
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using=com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer.class)
 @JsonFormat(shape=JsonFormat.Shape.STRING,pattern="yyyy-MM-dd'T'HH:mm:ss") private LocalDateTime reviewedTime;
    private String evidence;
}
