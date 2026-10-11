package cn.iocoder.yudao.module.hrm.controller.admin.payroll;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.service.payroll.HrmPayrollWorkService;
import java.time.LocalDate;
import java.util.*;
import javax.annotation.Resource;
import javax.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/hrm/payroll/work")
public class HrmPayrollWorkController {
  @Resource private HrmPayrollWorkService service;

  @Resource
  private cn.iocoder.yudao.module.hrm.service.payroll.HrmPayrollSalaryProposalService proposals;

  @GetMapping("/employees")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:work:query')")
  public CommonResult<List<Map<String, Object>>> employees() {
    return success(proposals.employees());
  }

  @GetMapping("/rules")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:work:query')")
  public CommonResult<List<HrmPayrollWorkRuleDO>> rules(
      @RequestParam(required = false) Long employeeId) {
    return success(service.rules(employeeId));
  }

  @PostMapping("/rule")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:work:update')")
  public CommonResult<Long> rule(@Valid @RequestBody HrmPayrollWorkRuleReqVO request) {
    return success(service.createRule(request));
  }

  @PostMapping("/rule/approve")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:work:approve')")
  public CommonResult<Boolean> approveRule(@RequestParam Long id) {
    service.approveRule(id, getLoginUserId());
    return success(true);
  }

  @PostMapping("/confirm")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:work:confirm')")
  public CommonResult<List<Long>> confirm(@Valid @RequestBody HrmPayrollTimeEntryReqVO request) {
    return success(service.confirm(request, getLoginUserId()));
  }

  @GetMapping("/entries")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:work:query')")
  public CommonResult<List<HrmPayrollTimeEntryDO>> entries(
      @RequestParam(required = false) Long employeeId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return success(service.entries(employeeId, from, to));
  }

  @GetMapping("/summary")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:work:query')")
  public CommonResult<Map<String, Object>> summary(
      @RequestParam Long employeeId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return success(service.summary(employeeId, from, to));
  }

  @PostMapping("/time-off/use")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:work:confirm')")
  public CommonResult<Long> use(
      @RequestParam Long employeeId,
      @RequestParam int minutes,
      @RequestParam String key,
      @RequestParam String reason) {
    return success(service.useTimeOff(employeeId, minutes, key, reason, getLoginUserId()));
  }

  @PostMapping("/time-off/refund")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:work:confirm')")
  public CommonResult<Long> refund(
      @RequestParam Long debitId, @RequestParam String key, @RequestParam String reason) {
    return success(service.refundTimeOff(debitId, key, reason, getLoginUserId()));
  }

  @GetMapping("/time-off")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:work:query')")
  public CommonResult<List<HrmPayrollTimeOffDO>> timeOff(@RequestParam Long employeeId) {
    return success(service.timeOff(employeeId));
  }

  @PostMapping("/time-off/consume")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:work:confirm')")
  public CommonResult<Boolean> consume(@RequestParam Long debitId) {
    service.consumeTimeOff(debitId, getLoginUserId());
    return success(true);
  }

  @PostMapping("/cycle/settle")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:work:confirm')")
  public CommonResult<Long> settle(
      @RequestParam Long ruleId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from) {
    return success(service.settleCycle(ruleId, from, getLoginUserId()));
  }
}
