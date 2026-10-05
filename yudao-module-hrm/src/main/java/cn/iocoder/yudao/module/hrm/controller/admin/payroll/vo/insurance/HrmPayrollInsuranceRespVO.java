package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.insurance;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class HrmPayrollInsuranceRespVO {
    private Long id;
    private String title;
    private Integer cityAreaId;
    private String cityName;
    private String scopeCode;
    private String scopeName;
    private Integer projectType;
    private String projectCode;
    private String projectName;
    private String customProjectCode;
    private Integer policyVersion;
    private Integer revision;
    private Integer status;
    private String ownerName;
    private String reference;
    private String sourceUrl;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate effectiveFrom;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate effectiveTo;
    private Integer configSchemaVersion;
    private HrmPayrollInsuranceConfigVO config;
    private Long reviewedBy;
    private String reviewedByName;
    private LocalDateTime reviewedTime;
    private String evidence;
    private LocalDateTime createTime;
}
