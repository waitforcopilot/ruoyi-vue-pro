package cn.iocoder.yudao.module.hrm.controller.admin.payroll;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollTaxDeductionReqVO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.service.payroll.HrmPayrollTaxDeductionService;
import java.util.List;
import javax.annotation.Resource;
import javax.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/hrm/payroll/tax-deduction")
public class HrmPayrollTaxDeductionController {
  @Resource private HrmPayrollTaxDeductionService service;

  @GetMapping("/list")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:tax:query')")
  public CommonResult<List<HrmPayrollTaxDeductionDO>> list(
      @RequestParam Long entityId,
      @RequestParam Integer year,
      @RequestParam(required = false) Integer month,
      @RequestParam(required = false) Long employeeId) {
    return success(service.list(entityId, year, month, employeeId, getLoginUserId()));
  }

  @PostMapping("/create")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:tax:update')")
  public CommonResult<Long> create(@Valid @RequestBody HrmPayrollTaxDeductionReqVO request) {
    return success(service.create(request, getLoginUserId()));
  }

  @PostMapping("/review")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:tax:approve')")
  public CommonResult<Boolean> review(
      @RequestParam Long id, @RequestParam boolean approved, @RequestParam String reason) {
    service.review(id, approved, reason, getLoginUserId());
    return success(true);
  }

  @GetMapping("/history")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:tax:query')")
  public CommonResult<List<HrmPayrollTaxDeductionEventDO>> history(@RequestParam Long id) {
    return success(service.history(id, getLoginUserId()));
  }
}
