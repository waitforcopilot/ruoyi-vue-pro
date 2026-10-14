package cn.iocoder.yudao.module.hrm.controller.admin.payroll;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollBonusReqVO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.service.payroll.HrmPayrollBonusService;
import java.util.List;
import javax.annotation.Resource;
import javax.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/hrm/payroll/bonus")
public class HrmPayrollBonusController {
  @Resource private HrmPayrollBonusService service;

  @GetMapping("/list")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:tax:query')")
  public CommonResult<List<HrmPayrollBonusDO>> list(@RequestParam Long batchId) {
    return success(service.list(batchId, getLoginUserId()));
  }

  @GetMapping("/employees")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:tax:query')")
  public CommonResult<List<java.util.Map<String, Object>>> employees(@RequestParam Long batchId) {
    return success(service.employees(batchId, getLoginUserId()));
  }

  @PostMapping("/create")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:tax:update')")
  public CommonResult<Long> create(@Valid @RequestBody HrmPayrollBonusReqVO request) {
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
  public CommonResult<List<HrmPayrollBonusEventDO>> history(@RequestParam Long id) {
    return success(service.history(id, getLoginUserId()));
  }
}
