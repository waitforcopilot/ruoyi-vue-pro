package cn.iocoder.yudao.module.hrm.controller.admin.payroll;
import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.*;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.calculation.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.HrmPayrollReviewDO;
import cn.iocoder.yudao.module.hrm.service.payroll.calculation.HrmPayrollCalculationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import java.util.List;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - HRM 规则表达式执行与核对")
@RestController @Validated @RequestMapping("/hrm/payroll/calculation-definitions")
public class HrmPayrollCalculationController {
    @Resource private HrmPayrollCalculationService service;
    @GetMapping("/wage-template") @PreAuthorize("@ss.hasPermission('hrm:payroll:calculation:query')")
    @ApiAccessLog(requestEnable = false, responseEnable = false)
    public CommonResult<HrmPayrollCalculationSpecVO> wageTemplate() { return success(cn.iocoder.yudao.module.hrm.service.payroll.calculation.HrmPayrollWageTemplate.program()); }
    @GetMapping("/page") @PreAuthorize("@ss.hasPermission('hrm:payroll:calculation:query')")
    @ApiAccessLog(requestEnable = false, responseEnable = false)
    public CommonResult<PageResult<HrmPayrollCalculationRespVO>> page(@Valid HrmPayrollCalculationPageReqVO req) { return success(service.page(req)); }
    @GetMapping("/get") @PreAuthorize("@ss.hasPermission('hrm:payroll:calculation:query')")
    @ApiAccessLog(requestEnable = false, responseEnable = false)
    public CommonResult<HrmPayrollCalculationRespVO> get(@RequestParam @Min(1) Long id) { return success(service.get(id)); }
    @PostMapping("/create") @PreAuthorize("@ss.hasPermission('hrm:payroll:calculation:query') and @ss.hasPermission('hrm:payroll:calculation:maintain')")
    @ApiAccessLog(requestEnable = false, responseEnable = false)
    public CommonResult<Long> create(@Valid @RequestBody HrmPayrollCalculationSaveReqVO req) { return success(service.create(req)); }
    @PutMapping("/update") @PreAuthorize("@ss.hasPermission('hrm:payroll:calculation:query') and @ss.hasPermission('hrm:payroll:calculation:maintain')")
    @ApiAccessLog(requestEnable = false, responseEnable = false)
    public CommonResult<Boolean> update(@Valid @RequestBody HrmPayrollCalculationSaveReqVO req) { service.update(req); return success(true); }
    @PostMapping("/new-version") @PreAuthorize("@ss.hasPermission('hrm:payroll:calculation:query') and @ss.hasPermission('hrm:payroll:calculation:maintain')")
    @ApiAccessLog(requestEnable = false, responseEnable = false)
    public CommonResult<Long> newVersion(@RequestParam @Min(1) Long id, @RequestParam @Min(1) Integer revision) { return success(service.newVersion(id, revision)); }
    @PostMapping("/review") @PreAuthorize("@ss.hasPermission('hrm:payroll:calculation:query') and @ss.hasPermission('hrm:payroll:calculation:review')")
    @ApiAccessLog(requestEnable = false, responseEnable = false)
    public CommonResult<Boolean> review(@Valid @RequestBody HrmPayrollCalculationReviewReqVO req) { service.review(req); return success(true); }
    @GetMapping("/cases") @PreAuthorize("@ss.hasPermission('hrm:payroll:calculation:query')")
    @ApiAccessLog(requestEnable = false, responseEnable = false)
    public CommonResult<HrmPayrollCalculationCasesVO> cases(@RequestParam @Min(1) Long id) { return success(service.cases(id)); }
    @PostMapping("/preview") @PreAuthorize("@ss.hasPermission('hrm:payroll:calculation:query')")
    @ApiAccessLog(requestEnable = false, responseEnable = false)
    public CommonResult<HrmPayrollCalculationPreviewVO> preview(@Valid @RequestBody HrmPayrollCalculationPreviewReqVO req) { return success(service.preview(req)); }
    @GetMapping("/history") @PreAuthorize("@ss.hasPermission('hrm:payroll:calculation:query')")
    @ApiAccessLog(requestEnable = false, responseEnable = false)
    public CommonResult<List<HrmPayrollReviewDO>> history(@RequestParam @Min(1) Long id) { return success(service.history(id)); }
    @GetMapping("/compare") @PreAuthorize("@ss.hasPermission('hrm:payroll:calculation:query')")
    @ApiAccessLog(requestEnable = false, responseEnable = false)
    public CommonResult<HrmPayrollCalculationCompareVO> compare(@RequestParam @Min(1) Long leftId, @RequestParam @Min(1) Long rightId) { return success(service.compare(leftId, rightId)); }
}
