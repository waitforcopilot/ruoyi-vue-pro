package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.review;

import javax.validation.constraints.*;
import lombok.Data;

@Data
public class HrmPayrollReviewActionReqVO {
    @NotNull
    @Min(1)
    private Long batchId;

    @NotNull
    @Min(1)
    private Long runId;

    @NotNull
    @Min(1)
    private Integer revision;

    @NotBlank
    @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._:-]{7,63}")
    private String requestKey;

    @NotBlank
    @Pattern(regexp = "submit|approve|reject|cancel|admin-cancel|freeze|unfreeze")
    private String action;

    @NotBlank
    @Size(max = 2000)
    private String evidence;

    @Min(1)
    private Long cycleId;

    @Size(max = 128)
    private String taskId;

    @Min(1)
    private Long hrReviewerId;

    @Min(1)
    private Long financeReviewerId;
}
