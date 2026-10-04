package cn.iocoder.yudao.module.hrm.controller.admin.payroll;
import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.*;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.scheme.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.HrmPayrollReviewDO;
import cn.iocoder.yudao.module.hrm.service.payroll.scheme.HrmPayrollSchemeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import javax.validation.Valid;
import javax.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
@Tag(name="管理后台 - HRM 薪酬方案配置版本") @RestController @RequestMapping("/hrm/payroll/schemes") @Validated
public class HrmPayrollSchemeController {
    @Resource private HrmPayrollSchemeService service;
    @GetMapping("/groups") @PreAuthorize("@ss.hasPermission('hrm:payroll:scheme:query')")
    public CommonResult<List<HrmPayrollSchemeSnapshotVO.Group>> groups(@RequestParam(required=false) @Size(max=64) String search) { return success(service.groups(search)); }
    @GetMapping("/capture") @PreAuthorize("@ss.hasPermission('hrm:payroll:scheme:query')") @ApiAccessLog(requestEnable=false,responseEnable=false)
    public CommonResult<HrmPayrollSchemeRespVO> capture(@RequestParam @Min(1) Long groupId) { return success(service.capture(groupId)); }
    @GetMapping("/page") @PreAuthorize("@ss.hasPermission('hrm:payroll:scheme:query')") @ApiAccessLog(requestEnable=false,responseEnable=false)
    public CommonResult<PageResult<HrmPayrollSchemeRespVO>> page(@Valid HrmPayrollSchemePageReqVO req) { return success(service.page(req)); }
    @GetMapping("/get") @PreAuthorize("@ss.hasPermission('hrm:payroll:scheme:query')") @ApiAccessLog(requestEnable=false,responseEnable=false)
    public CommonResult<HrmPayrollSchemeRespVO> get(@RequestParam Long id) { return success(service.get(id)); }
    @PostMapping("/create") @PreAuthorize("@ss.hasPermission('hrm:payroll:scheme:maintain')") @ApiAccessLog(requestEnable=false,responseEnable=false)
    public CommonResult<Long> create(@Valid @RequestBody HrmPayrollSchemeSaveReqVO req) { return success(service.create(req)); }
    @PutMapping("/update") @PreAuthorize("@ss.hasPermission('hrm:payroll:scheme:maintain')") @ApiAccessLog(requestEnable=false,responseEnable=false)
    public CommonResult<Boolean> update(@Valid @RequestBody HrmPayrollSchemeSaveReqVO req) { service.update(req);return success(true); }
    @PostMapping("/new-version") @PreAuthorize("@ss.hasPermission('hrm:payroll:scheme:maintain')") @ApiAccessLog(requestEnable=false,responseEnable=false)
    public CommonResult<Long> version(@RequestParam Long id,@RequestParam @Min(1) Integer revision) { return success(service.newVersion(id,revision)); }
    @PostMapping("/review") @PreAuthorize("@ss.hasPermission('hrm:payroll:scheme:review')") @ApiAccessLog(requestEnable=false,responseEnable=false)
    public CommonResult<Boolean> review(@Valid @RequestBody HrmPayrollSchemeReviewReqVO req) { service.review(req);return success(true); }
    @GetMapping("/history") @PreAuthorize("@ss.hasPermission('hrm:payroll:scheme:query')") @ApiAccessLog(requestEnable=false,responseEnable=false)
    public CommonResult<List<HrmPayrollReviewDO>> history(@RequestParam Long id) { return success(service.history(id)); }
    @GetMapping("/compare") @PreAuthorize("@ss.hasPermission('hrm:payroll:scheme:query')") @ApiAccessLog(requestEnable=false,responseEnable=false)
    public CommonResult<HrmPayrollSchemeCompareVO> compare(@RequestParam Long leftId,@RequestParam Long rightId) { return success(service.compare(leftId,rightId)); }
    @GetMapping("/resolve") @PreAuthorize("@ss.hasPermission('hrm:payroll:scheme:query')") @ApiAccessLog(requestEnable=false,responseEnable=false)
    public CommonResult<HrmPayrollSchemeRespVO> resolve(@RequestParam @Min(1) Long groupId,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate start,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate end) { return success(service.resolve(groupId,start,end)); }
}
