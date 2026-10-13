package cn.iocoder.yudao.module.hrm.controller.admin.payroll;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollWorkVarianceReqVO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.service.payroll.*;
import java.time.LocalDate;
import java.util.*;
import javax.annotation.Resource;
import javax.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/hrm/payroll/work/variance")
public class HrmPayrollWorkVarianceController {
  @Resource private HrmPayrollWorkVarianceService service;
  @Resource private HrmPayrollWorkService work;

  @GetMapping("/preview")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:work:query')")
  public CommonResult<Map<String, Object>> preview(
      @RequestParam Long entityId,
      @RequestParam Long employeeId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    return success(work.reconcileDay(entityId, employeeId, date, getLoginUserId()));
  }

  @GetMapping("/list")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:work:query')")
  public CommonResult<List<HrmPayrollWorkVarianceDO>> list(
      @RequestParam Long entityId, @RequestParam(required = false) Long employeeId) {
    return success(service.list(entityId, employeeId, getLoginUserId()));
  }

  @PostMapping("/create")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:work:confirm')")
  public CommonResult<Long> create(@Valid @RequestBody HrmPayrollWorkVarianceReqVO request) {
    return success(service.create(request, getLoginUserId()));
  }

  @PostMapping("/submit")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:work:confirm')")
  public CommonResult<Boolean> submit(@RequestParam Long id, @RequestParam String evidence) {
    service.submit(id, evidence, getLoginUserId());
    return success(true);
  }

  @PostMapping("/review")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:work:approve')")
  public CommonResult<Boolean> review(
      @RequestParam Long id, @RequestParam boolean approved, @RequestParam String reason) {
    service.review(id, approved, reason, getLoginUserId());
    return success(true);
  }

  @PostMapping("/reopen")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:work:approve')")
  public CommonResult<Boolean> reopen(@RequestParam Long id, @RequestParam String reason) {
    service.reopen(id, reason, getLoginUserId());
    return success(true);
  }

  @PostMapping("/reassign")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:work:confirm')")
  public CommonResult<Boolean> reassign(
      @Valid @RequestBody
          cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollWorkVarianceAssignReqVO
              request) {
    service.reassign(request, getLoginUserId());
    return success(true);
  }

  @GetMapping("/history")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:work:query')")
  public CommonResult<List<HrmPayrollWorkVarianceEventDO>> history(@RequestParam Long id) {
    return success(service.history(id, getLoginUserId()));
  }
}
