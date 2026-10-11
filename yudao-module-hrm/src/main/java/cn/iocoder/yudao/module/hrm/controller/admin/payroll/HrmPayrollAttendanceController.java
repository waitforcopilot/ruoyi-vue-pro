package cn.iocoder.yudao.module.hrm.controller.admin.payroll;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollAttendanceIssueReqVO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.service.payroll.*;
import java.util.*;
import javax.annotation.Resource;
import javax.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/hrm/payroll/attendance")
public class HrmPayrollAttendanceController {
  @Resource private HrmPayrollAttendanceIssueService service;
  @Resource private HrmPayrollSalaryProposalService proposals;

  @GetMapping("/employees")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:attendance:query')")
  public CommonResult<List<Map<String, Object>>> employees() {
    return success(proposals.employees());
  }

  @GetMapping("/users")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:attendance:query')")
  public CommonResult<List<Map<String, Object>>> users() {
    return success(service.users());
  }

  @GetMapping("/issues")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:attendance:query')")
  public CommonResult<List<HrmPayrollAttendanceIssueDO>> list(
      @RequestParam(required = false) Long employeeId,
      @RequestParam(required = false) String status) {
    return success(service.display(service.list(employeeId, status)));
  }

  @GetMapping("/history")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:attendance:query')")
  public CommonResult<List<HrmPayrollAttendanceIssueEventDO>> history(@RequestParam Long id) {
    return success(service.history(id));
  }

  @PostMapping("/issue")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:attendance:update')")
  public CommonResult<Long> create(@Valid @RequestBody HrmPayrollAttendanceIssueReqVO request) {
    return success(service.create(request, getLoginUserId()));
  }

  @PostMapping("/submit")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:attendance:update')")
  public CommonResult<Boolean> submit(
      @Valid @RequestBody HrmPayrollAttendanceIssueReqVO.Submit request) {
    service.submit(request, getLoginUserId());
    return success(true);
  }

  @PostMapping("/review")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:attendance:review')")
  public CommonResult<Boolean> review(
      @RequestParam Long id, @RequestParam boolean approve, @RequestParam String reason) {
    service.review(id, approve, reason, getLoginUserId());
    return success(true);
  }

  @PostMapping("/close")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:attendance:review')")
  public CommonResult<Boolean> close(@RequestParam Long id, @RequestParam String reason) {
    service.close(id, reason, getLoginUserId());
    return success(true);
  }

  @PostMapping("/reopen")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:attendance:review')")
  public CommonResult<Boolean> reopen(@RequestParam Long id, @RequestParam String reason) {
    service.reopen(id, reason, getLoginUserId());
    return success(true);
  }
}
