package cn.iocoder.yudao.module.hrm.controller.admin.portal.payroll;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollAttendanceIssueReqVO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.service.payroll.HrmPayrollAttendanceIssueService;
import java.util.*;
import javax.annotation.Resource;
import javax.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/hrm/portal/payroll/attendance")
public class HrmPortalPayrollAttendanceController {
  @Resource private HrmPayrollAttendanceIssueService service;

  @GetMapping("/mine")
  @PreAuthorize("@ss.hasPermission('hrm:portal:query')")
  public CommonResult<List<HrmPayrollAttendanceIssueDO>> mine() {
    return success(service.display(service.mine(getLoginUserId())));
  }

  @GetMapping("/overdue")
  @PreAuthorize("@ss.hasPermission('hrm:portal:query')")
  public CommonResult<List<HrmPayrollAttendanceIssueDO>> overdue() {
    return success(service.display(service.overdue(getLoginUserId())));
  }

  @GetMapping("/history")
  @PreAuthorize("@ss.hasPermission('hrm:portal:query')")
  public CommonResult<List<HrmPayrollAttendanceIssueEventDO>> history(@RequestParam Long id) {
    return success(service.myHistory(id, getLoginUserId()));
  }

  @PostMapping("/submit")
  @PreAuthorize("@ss.hasPermission('hrm:portal:attendance:leave')")
  public CommonResult<Boolean> submit(
      @Valid @RequestBody HrmPayrollAttendanceIssueReqVO.Submit request) {
    service.submit(request, getLoginUserId());
    return success(true);
  }
}
