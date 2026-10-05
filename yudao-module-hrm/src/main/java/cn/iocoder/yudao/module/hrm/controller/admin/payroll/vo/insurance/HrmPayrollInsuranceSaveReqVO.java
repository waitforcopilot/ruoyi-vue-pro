package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.insurance;

import cn.iocoder.yudao.framework.common.validation.InEnum;
import cn.iocoder.yudao.module.hrm.enums.insurance.config.HrmInsuranceProjectTypeEnum;
import lombok.Data;
import javax.validation.Valid;
import javax.validation.constraints.*;
import java.time.LocalDate;

@Data
public class HrmPayrollInsuranceSaveReqVO {
    private Long id;
    private Integer revision;
    @NotNull @Min(1)
    private Integer cityAreaId;
    @NotBlank @Pattern(regexp = "[A-Z0-9][A-Z0-9_-]{0,63}")
    private String scopeCode;
    @Size(max = 120)
    private String scopeName;
    @NotNull @InEnum(HrmInsuranceProjectTypeEnum.class)
    private Integer projectType;
    @Pattern(regexp = "[A-Z0-9][A-Z0-9_-]{0,63}")
    private String customProjectCode;
    @NotBlank @Size(max = 160)
    private String title;
    @Size(max = 120)
    private String ownerName;
    @Size(max = 5000)
    private String reference;
    @Size(max = 2048)
    private String sourceUrl;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    @Valid
    private HrmPayrollInsuranceConfigVO config;
}
