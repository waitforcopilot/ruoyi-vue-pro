package cn.iocoder.yudao.module.hrm.controller.admin.payroll;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.*;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.insurance.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.HrmPayrollReviewDO;
import cn.iocoder.yudao.module.hrm.enums.insurance.config.HrmInsuranceProjectTypeEnum;
import cn.iocoder.yudao.module.hrm.service.payroll.insurance.HrmPayrollInsurancePolicyService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import java.time.LocalDate;
import java.util.*;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - HRM 本地社保公积金政策")
@RestController @Validated @RequestMapping("/hrm/payroll/insurance-policies")
public class HrmPayrollInsurancePolicyController {
    @Resource private HrmPayrollInsurancePolicyService service;

    @GetMapping("/projects") @PreAuthorize("@ss.hasPermission('hrm:payroll:insurance-policy:query')")
    public CommonResult<List<Map<String, Object>>> projects() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (HrmInsuranceProjectTypeEnum value : HrmInsuranceProjectTypeEnum.values()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("type", value.getType()); row.put("name", value.getName()); row.put("custom", value.isCustom()); result.add(row);
        }
        return success(result);
    }
    @GetMapping("/page") @PreAuthorize("@ss.hasPermission('hrm:payroll:insurance-policy:query')")
    @ApiAccessLog(requestEnable = false, responseEnable = false)
    public CommonResult<PageResult<HrmPayrollInsuranceRespVO>> page(@Valid HrmPayrollInsurancePageReqVO req) { return success(service.page(req)); }
    @GetMapping("/get") @PreAuthorize("@ss.hasPermission('hrm:payroll:insurance-policy:query')")
    @ApiAccessLog(requestEnable = false, responseEnable = false)
    public CommonResult<HrmPayrollInsuranceRespVO> get(@RequestParam @Min(1) Long id) { return success(service.get(id)); }
    @PostMapping("/create") @PreAuthorize("@ss.hasPermission('hrm:payroll:insurance-policy:maintain')")
    @ApiAccessLog(requestEnable = false, responseEnable = false)
    public CommonResult<Long> create(@Valid @RequestBody HrmPayrollInsuranceSaveReqVO req) { return success(service.create(req)); }
    @PutMapping("/update") @PreAuthorize("@ss.hasPermission('hrm:payroll:insurance-policy:maintain')")
    @ApiAccessLog(requestEnable = false, responseEnable = false)
    public CommonResult<Boolean> update(@Valid @RequestBody HrmPayrollInsuranceSaveReqVO req) { service.update(req); return success(true); }
    @PostMapping("/new-version") @PreAuthorize("@ss.hasPermission('hrm:payroll:insurance-policy:maintain')")
    @ApiAccessLog(requestEnable = false, responseEnable = false)
    public CommonResult<Long> newVersion(@RequestParam @Min(1) Long id, @RequestParam @Min(1) Integer revision) { return success(service.newVersion(id, revision)); }
    @PostMapping("/review") @PreAuthorize("@ss.hasPermission('hrm:payroll:insurance-policy:review')")
    @ApiAccessLog(requestEnable = false, responseEnable = false)
    public CommonResult<Boolean> review(@Valid @RequestBody HrmPayrollInsuranceReviewReqVO req) { service.review(req); return success(true); }
    @GetMapping("/history") @PreAuthorize("@ss.hasPermission('hrm:payroll:insurance-policy:query')")
    @ApiAccessLog(requestEnable = false, responseEnable = false)
    public CommonResult<List<HrmPayrollReviewDO>> history(@RequestParam @Min(1) Long id) { return success(service.history(id)); }
    @GetMapping("/compare") @PreAuthorize("@ss.hasPermission('hrm:payroll:insurance-policy:query')")
    @ApiAccessLog(requestEnable = false, responseEnable = false)
    public CommonResult<HrmPayrollInsuranceCompareVO> compare(@RequestParam @Min(1) Long leftId, @RequestParam @Min(1) Long rightId) { return success(service.compare(leftId, rightId)); }
    @GetMapping("/resolve") @PreAuthorize("@ss.hasPermission('hrm:payroll:insurance-policy:query')")
    @ApiAccessLog(requestEnable = false, responseEnable = false)
    public CommonResult<HrmPayrollInsuranceRespVO> resolve(@RequestParam @Min(1) Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) { return success(service.resolve(id, start, end)); }
    @PostMapping("/preview") @PreAuthorize("@ss.hasPermission('hrm:payroll:insurance-policy:query')")
    @ApiAccessLog(requestEnable = false, responseEnable = false)
    public CommonResult<HrmPayrollInsurancePreviewVO> preview(@Valid @RequestBody HrmPayrollInsurancePreviewReqVO req) { return success(service.preview(req)); }
}
