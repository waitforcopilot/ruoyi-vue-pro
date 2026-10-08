package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.scheme;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.*;
@Data
public class HrmPayrollSchemeRespVO {
    private Long id;private Long groupId;private String groupName;private String title;private Integer schemeVersion;private Integer revision;private Integer status;
    private String ownerName;private String reference;private Integer optionCount;private String sourceHash;
    @JsonFormat(shape=JsonFormat.Shape.STRING,pattern="yyyy-MM-dd") private LocalDate effectiveFrom;
    @JsonFormat(shape=JsonFormat.Shape.STRING,pattern="yyyy-MM-dd") private LocalDate effectiveTo;
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using=com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer.class)
    @JsonFormat(shape=JsonFormat.Shape.STRING,pattern="yyyy-MM-dd'T'HH:mm:ss") private LocalDateTime capturedAt;
    private String reviewedByName;
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using=com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer.class)
    @JsonFormat(shape=JsonFormat.Shape.STRING,pattern="yyyy-MM-dd'T'HH:mm:ss") private LocalDateTime reviewedTime;
    private String evidence;private HrmPayrollSchemeSnapshotVO snapshot;
}
