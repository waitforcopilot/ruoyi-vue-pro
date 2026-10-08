package cn.iocoder.yudao.module.hrm.controller.admin.payroll;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.review.*;
import cn.iocoder.yudao.module.hrm.service.payroll.review.HrmPayrollReviewService;
import javax.annotation.Resource;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequestMapping("/hrm/payroll/trial-batches/review")
public class HrmPayrollReviewController {
    @Resource private HrmPayrollReviewService service;

    @GetMapping("/get")
    @PreAuthorize(
            "@ss.hasPermission('hrm:payroll:trial:query') and @ss.hasPermission('hrm:payroll:calculation:query') and @ss.hasPermission('hrm:payroll:eligibility:query') and @ss.hasPermission('hrm:employee:query')")
    @ApiAccessLog(requestEnable = false, responseEnable = false)
    public CommonResult<HrmPayrollReviewRespVO> get(@RequestParam @Min(1) Long batchId) {
        return success(service.get(batchId));
    }

    @PostMapping("/sync")
    @PreAuthorize(
            "@ss.hasPermission('hrm:payroll:trial:query') and @ss.hasPermission('hrm:payroll:calculation:query') and @ss.hasPermission('hrm:payroll:eligibility:query') and @ss.hasPermission('hrm:employee:query')")
    @ApiAccessLog(requestEnable = false, responseEnable = false)
    public CommonResult<HrmPayrollReviewRespVO> sync(@RequestParam @Min(1) Long batchId) {
        return success(service.sync(batchId));
    }

    @PostMapping("/action")
    @PreAuthorize(
            "@ss.hasPermission('hrm:payroll:trial:query') and @ss.hasPermission('hrm:payroll:calculation:query') and @ss.hasPermission('hrm:payroll:eligibility:query') and @ss.hasPermission('hrm:employee:query')")
    @ApiAccessLog(requestEnable = false, responseEnable = false)
    public CommonResult<HrmPayrollReviewRespVO> action(
            @Valid @RequestBody HrmPayrollReviewActionReqVO request) {
        return success(service.action(request));
    }
}
