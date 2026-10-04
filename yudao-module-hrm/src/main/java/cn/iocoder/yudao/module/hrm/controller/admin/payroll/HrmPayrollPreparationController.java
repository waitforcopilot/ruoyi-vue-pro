package cn.iocoder.yudao.module.hrm.controller.admin.payroll;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.preparation.HrmPayrollPreparationRespVO;
import cn.iocoder.yudao.module.hrm.service.payroll.preparation.HrmPayrollPreparationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name="管理后台 - HRM 薪酬资料准备总览")
@RestController
@RequestMapping("/hrm/payroll/preparation")
public class HrmPayrollPreparationController {
    @Resource private HrmPayrollPreparationService service;
    @GetMapping("/summary")
    @PreAuthorize("@ss.hasPermission('hrm:payroll:preparation:query')")
    @ApiAccessLog(requestEnable=false,responseEnable=false)
    public CommonResult<HrmPayrollPreparationRespVO> summary() { return success(service.summary()); }
}
