package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.eligibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
@Data @EqualsAndHashCode(callSuper = true)
public class HrmPayrollEligibilityRespVO extends HrmPayrollEligibilitySaveReqVO {
    private Integer eligibilityVersion;
    private Integer status;
    private String snapshotName;
    private String snapshotJobNumber;
    private Long snapshotDeptId;
    private Long snapshotUserId;
    private Integer snapshotEmployeeStatus;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss") private LocalDateTime snapshotEntryTime;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss") private LocalDateTime snapshotLeaveTime;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss") private LocalDateTime snapshotCapturedAt;
    private String reviewedByName;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss") private LocalDateTime reviewedTime;
    private String evidence;
}
