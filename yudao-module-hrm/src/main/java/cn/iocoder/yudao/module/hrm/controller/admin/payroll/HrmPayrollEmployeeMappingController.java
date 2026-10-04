package cn.iocoder.yudao.module.hrm.controller.admin.payroll;
import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.*;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.identity.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.HrmPayrollSourceMapper;
import cn.iocoder.yudao.module.hrm.service.payroll.identity.HrmPayrollEmployeeMappingService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import java.util.*;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
@Tag(name="管理后台 - HRM 薪酬人员编号映射")
@RestController
@RequestMapping("/hrm/payroll/identity")
@Validated
public class HrmPayrollEmployeeMappingController {
    @Resource private HrmPayrollEmployeeMappingService service;
    @Resource private HrmPayrollSourceMapper sourceMapper;
    @GetMapping("/sources") @PreAuthorize("@ss.hasPermission('hrm:payroll:identity:query') && @ss.hasPermission('hrm:employee:query')")
    public CommonResult<List<HrmPayrollSourceDO>> sources() { return success(sourceMapper.selectList(new LambdaQueryWrapperX<HrmPayrollSourceDO>()
            .eq(HrmPayrollSourceDO::getTenantId,TenantContextHolder.getRequiredTenantId()).select(HrmPayrollSourceDO::getId,HrmPayrollSourceDO::getCode,HrmPayrollSourceDO::getName).orderByAsc(HrmPayrollSourceDO::getId))); }
    @GetMapping("/employee") @PreAuthorize("@ss.hasPermission('hrm:payroll:identity:query') && @ss.hasPermission('hrm:employee:query')")
    @ApiAccessLog(requestEnable=false,responseEnable=false)
    public CommonResult<HrmPayrollMappingRespVO> employee(@RequestParam @Min(1) Long id) { return success(service.employee(id)); }
    @GetMapping("/page") @PreAuthorize("@ss.hasPermission('hrm:payroll:identity:query') && @ss.hasPermission('hrm:employee:query')")
    @ApiAccessLog(requestEnable=false,responseEnable=false)
    public CommonResult<PageResult<HrmPayrollMappingRespVO>> page(@Valid HrmPayrollMappingPageReqVO query) { return success(service.page(query)); }
    @GetMapping("/get") @PreAuthorize("@ss.hasPermission('hrm:payroll:identity:query') && @ss.hasPermission('hrm:employee:query')")
    @ApiAccessLog(requestEnable=false,responseEnable=false)
    public CommonResult<HrmPayrollMappingRespVO> get(@RequestParam Long id) { return success(service.get(id)); }
    @PostMapping("/create") @PreAuthorize("@ss.hasPermission('hrm:payroll:identity:maintain') && @ss.hasPermission('hrm:employee:query')")
    @ApiAccessLog(requestEnable=false,responseEnable=false)
    public CommonResult<Long> create(@Valid @RequestBody HrmPayrollMappingSaveReqVO req) { return success(service.create(req)); }
    @PutMapping("/update") @PreAuthorize("@ss.hasPermission('hrm:payroll:identity:maintain') && @ss.hasPermission('hrm:employee:query')")
    @ApiAccessLog(requestEnable=false,responseEnable=false)
    public CommonResult<Boolean> update(@Valid @RequestBody HrmPayrollMappingSaveReqVO req) { service.update(req);return success(true); }
    @PostMapping("/new-version") @PreAuthorize("@ss.hasPermission('hrm:payroll:identity:maintain') && @ss.hasPermission('hrm:employee:query')")
    @ApiAccessLog(requestEnable=false,responseEnable=false)
    public CommonResult<Long> version(@RequestParam Long id,@RequestParam @Min(1) Integer revision,@RequestParam(required=false) @Min(1) Long employeeId) { return success(service.newVersion(id,revision,employeeId)); }
    @PostMapping("/review") @PreAuthorize("@ss.hasPermission('hrm:payroll:identity:review') && @ss.hasPermission('hrm:employee:query')")
    @ApiAccessLog(requestEnable=false,responseEnable=false)
    public CommonResult<Boolean> review(@Valid @RequestBody HrmPayrollMappingReviewReqVO req) { service.review(req);return success(true); }
    @GetMapping("/history") @PreAuthorize("@ss.hasPermission('hrm:payroll:identity:query') && @ss.hasPermission('hrm:employee:query')")
    @ApiAccessLog(requestEnable=false,responseEnable=false)
    public CommonResult<List<HrmPayrollReviewDO>> history(@RequestParam Long id) { return success(service.history(id)); }
    @PostMapping("/resolve") @PreAuthorize("@ss.hasPermission('hrm:payroll:identity:query') && @ss.hasPermission('hrm:employee:query')")
    @ApiAccessLog(requestEnable=false,responseEnable=false)
    public CommonResult<HrmPayrollMappingLookupVO.Match> resolve(@RequestParam Long sourceId,@RequestParam String namespace,@Valid @RequestBody HrmPayrollMappingLookupVO lookup) {
        return success(service.resolve(sourceId,namespace,Collections.singletonList(lookup)).get(0));
    }
}
