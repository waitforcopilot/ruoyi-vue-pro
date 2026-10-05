package cn.iocoder.yudao.module.hrm.controller.admin.payroll;
import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.*;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.eligibility.*;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.identity.HrmPayrollMappingRespVO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.HrmPayrollReviewDO;
import cn.iocoder.yudao.module.hrm.service.payroll.eligibility.HrmPayrollEligibilityService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import java.util.List;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
@Tag(name="管理后台 - HRM 计薪人员资格与期间")
@RestController @RequestMapping("/hrm/payroll/employee-eligibilities") @Validated
public class HrmPayrollEligibilityController {
    @Resource private HrmPayrollEligibilityService service;
    @GetMapping("/employee") @PreAuthorize("@ss.hasPermission('hrm:payroll:eligibility:query') && @ss.hasPermission('hrm:employee:query')")
    @ApiAccessLog(requestEnable=false,responseEnable=false)
    public CommonResult<HrmPayrollMappingRespVO> employee(@RequestParam @Min(1) Long id) { return success(service.employee(id)); }
    @GetMapping("/page") @PreAuthorize("@ss.hasPermission('hrm:payroll:eligibility:query') && @ss.hasPermission('hrm:employee:query')")
    @ApiAccessLog(requestEnable=false,responseEnable=false)
    public CommonResult<PageResult<HrmPayrollEligibilityRespVO>> page(@Valid HrmPayrollEligibilityPageReqVO query) { return success(service.page(query)); }
    @GetMapping("/get") @PreAuthorize("@ss.hasPermission('hrm:payroll:eligibility:query') && @ss.hasPermission('hrm:employee:query')")
    @ApiAccessLog(requestEnable=false,responseEnable=false)
    public CommonResult<HrmPayrollEligibilityRespVO> get(@RequestParam @Min(1) Long id) { return success(service.get(id)); }
    @PostMapping("/create") @PreAuthorize("@ss.hasPermission('hrm:payroll:eligibility:maintain') && @ss.hasPermission('hrm:payroll:eligibility:query') && @ss.hasPermission('hrm:employee:query')")
    @ApiAccessLog(requestEnable=false,responseEnable=false)
    public CommonResult<Long> create(@Valid @RequestBody HrmPayrollEligibilitySaveReqVO request) { return success(service.create(request)); }
    @PutMapping("/update") @PreAuthorize("@ss.hasPermission('hrm:payroll:eligibility:maintain') && @ss.hasPermission('hrm:payroll:eligibility:query') && @ss.hasPermission('hrm:employee:query')")
    @ApiAccessLog(requestEnable=false,responseEnable=false)
    public CommonResult<Boolean> update(@Valid @RequestBody HrmPayrollEligibilitySaveReqVO request) { service.update(request);return success(true); }
    @PostMapping("/new-version") @PreAuthorize("@ss.hasPermission('hrm:payroll:eligibility:maintain') && @ss.hasPermission('hrm:payroll:eligibility:query') && @ss.hasPermission('hrm:employee:query')")
    @ApiAccessLog(requestEnable=false,responseEnable=false)
    public CommonResult<Long> version(@RequestParam @Min(1) Long id,@RequestParam @Min(1) Integer revision) { return success(service.newVersion(id,revision)); }
    @PostMapping("/review") @PreAuthorize("@ss.hasPermission('hrm:payroll:eligibility:review') && @ss.hasPermission('hrm:payroll:eligibility:query') && @ss.hasPermission('hrm:employee:query')")
    @ApiAccessLog(requestEnable=false,responseEnable=false)
    public CommonResult<Boolean> review(@Valid @RequestBody HrmPayrollEligibilityReviewReqVO request) { service.review(request);return success(true); }
    @GetMapping("/history") @PreAuthorize("@ss.hasPermission('hrm:payroll:eligibility:query') && @ss.hasPermission('hrm:employee:query')")
    @ApiAccessLog(requestEnable=false,responseEnable=false)
    public CommonResult<List<HrmPayrollReviewDO>> history(@RequestParam @Min(1) Long id) { return success(service.history(id)); }
    @PostMapping("/lookup") @PreAuthorize("@ss.hasPermission('hrm:payroll:eligibility:query') && @ss.hasPermission('hrm:employee:query')")
    @ApiAccessLog(requestEnable=false,responseEnable=false)
    public CommonResult<HrmPayrollEligibilityLookupVO> lookup(@Valid @RequestBody HrmPayrollEligibilityLookupReqVO request) { return success(service.lookup(request)); }
}
