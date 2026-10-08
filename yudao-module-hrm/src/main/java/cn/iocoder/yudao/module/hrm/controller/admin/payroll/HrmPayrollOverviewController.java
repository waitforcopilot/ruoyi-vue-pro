package cn.iocoder.yudao.module.hrm.controller.admin.payroll;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.overview.*;
import cn.iocoder.yudao.module.hrm.service.payroll.overview.HrmPayrollOverviewService;
import javax.annotation.Resource;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequestMapping("/hrm/payroll/overview")
@PreAuthorize(
        "@ss.hasPermission('hrm:payroll:overview:query') and @ss.hasPermission('hrm:payroll:trial:query') and @ss.hasPermission('hrm:payroll:calculation:query') and @ss.hasPermission('hrm:payroll:eligibility:query') and @ss.hasPermission('hrm:employee:query')")
public class HrmPayrollOverviewController {
    @Resource private HrmPayrollOverviewService service;

    @GetMapping("/page")
    @ApiAccessLog(requestEnable = false, responseEnable = false)
    public CommonResult<HrmPayrollOverviewRespVO> page(@Valid HrmPayrollOverviewPageReqVO req) {
        return success(service.page(req));
    }

    @GetMapping("/batch")
    @ApiAccessLog(requestEnable = false, responseEnable = false)
    public CommonResult<HrmPayrollOverviewRespVO.Detail> batch(@RequestParam @Min(1) Long id) {
        return success(service.detail(id));
    }
}
