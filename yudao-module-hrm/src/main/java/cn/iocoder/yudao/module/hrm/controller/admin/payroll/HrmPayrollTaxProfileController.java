package cn.iocoder.yudao.module.hrm.controller.admin.payroll;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollTaxProfileReqVO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.service.payroll.*;
import java.time.LocalDate;
import java.util.*;
import javax.annotation.Resource;
import javax.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/hrm/payroll/tax-profile")
public class HrmPayrollTaxProfileController {
  @Resource private HrmPayrollTaxProfileService service;
  @Resource private HrmPayrollWorkService work;

  @GetMapping("/employees")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:tax:query')")
  public CommonResult<List<Map<String, Object>>> employees(
      @RequestParam Long entityId, @RequestParam Integer year) {
    if (year == null || year < 2019 || year > 2099)
      throw cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception(
          cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.PAYROLL_TAX_INVALID);
    return success(
        work.scopedEmployees(
            entityId, LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31), getLoginUserId()));
  }

  @GetMapping("/list")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:tax:query')")
  public CommonResult<List<HrmPayrollTaxProfileDO>> list(
      @RequestParam Long entityId,
      @RequestParam Integer year,
      @RequestParam(required = false) Long employeeId) {
    return success(service.list(entityId, year, employeeId, getLoginUserId()));
  }

  @cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog(
      requestEnable = false,
      responseEnable = false)
  @PostMapping("/create")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:tax:update')")
  public CommonResult<Long> create(@Valid @RequestBody HrmPayrollTaxProfileReqVO request) {
    return success(service.create(request, getLoginUserId()));
  }

  @PostMapping("/review")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:tax:approve')")
  public CommonResult<Boolean> review(
      @RequestParam Long id, @RequestParam boolean approved, @RequestParam String reason) {
    service.review(id, approved, reason, getLoginUserId());
    return success(true);
  }

  @cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog(
      requestEnable = false,
      responseEnable = false)
  @GetMapping("/private-identifier")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:tax:private')")
  public CommonResult<String> identifier(@RequestParam Long id) {
    return success(service.privateIdentifier(id, getLoginUserId()));
  }

  @GetMapping("/history")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:tax:query')")
  public CommonResult<List<HrmPayrollTaxProfileEventDO>> history(@RequestParam Long id) {
    return success(service.history(id, getLoginUserId()));
  }
}
