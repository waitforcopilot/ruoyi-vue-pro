package cn.iocoder.yudao.module.hrm.controller.admin.payroll;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollEntityReqVO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.service.payroll.HrmPayrollEntityService;
import java.util.*;
import javax.annotation.Resource;
import javax.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/hrm/payroll/entity")
public class HrmPayrollEntityController {
  @Resource private HrmPayrollEntityService service;

  @GetMapping("/list")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:entity:query')")
  public CommonResult<List<HrmPayrollLegalEntityDO>> list() {
    return success(service.list(getLoginUserId()));
  }

  @GetMapping("/options")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:entity:query')")
  public CommonResult<Map<String, Object>> options(@RequestParam Long id) {
    return success(service.options(id, getLoginUserId()));
  }

  @PostMapping("/create")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:entity:update')")
  public CommonResult<Long> create(@Valid @RequestBody HrmPayrollEntityReqVO request) {
    return success(service.create(request, getLoginUserId()));
  }

  @PostMapping("/revise")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:entity:update')")
  public CommonResult<Boolean> revise(
      @RequestParam Long id, @Valid @RequestBody HrmPayrollEntityReqVO request) {
    service.revise(id, request, getLoginUserId());
    return success(true);
  }

  @PostMapping("/approve")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:entity:approve')")
  public CommonResult<Boolean> approve(@RequestParam Long id, @RequestParam String reason) {
    service.approve(id, reason, getLoginUserId());
    return success(true);
  }

  @PostMapping("/grant")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:entity:grant')")
  public CommonResult<Long> grant(@Valid @RequestBody HrmPayrollEntityReqVO.Grant request) {
    return success(service.grant(request, getLoginUserId()));
  }

  @PostMapping("/revoke")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:entity:grant')")
  public CommonResult<Boolean> revoke(@RequestParam Long id, @RequestParam String reason) {
    service.revoke(id, reason, getLoginUserId());
    return success(true);
  }

  @GetMapping("/grants")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:entity:grant')")
  public CommonResult<List<HrmPayrollEntityGrantDO>> grants(@RequestParam Long id) {
    return success(service.grants(id, getLoginUserId()));
  }

  @GetMapping("/history")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:entity:query')")
  public CommonResult<List<HrmPayrollEntityEventDO>> history(@RequestParam Long id) {
    return success(service.history(id, getLoginUserId()));
  }

  @GetMapping("/memberships")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:entity:query')")
  public CommonResult<List<HrmPayrollEntityMembershipDO>> memberships(@RequestParam Long id) {
    return success(service.memberships(id, getLoginUserId()));
  }

  @PostMapping("/assign")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:entity:update')")
  public CommonResult<Long> assign(@Valid @RequestBody HrmPayrollEntityReqVO.Membership request) {
    return success(service.assign(request, getLoginUserId()));
  }

  @PostMapping("/membership/approve")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:entity:approve')")
  public CommonResult<Boolean> approveMembership(
      @RequestParam Long id, @RequestParam String reason) {
    service.approveMembership(id, reason, getLoginUserId());
    return success(true);
  }

  @PostMapping("/membership/reject")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:entity:approve')")
  public CommonResult<Boolean> rejectMembership(
      @RequestParam Long id, @RequestParam String reason) {
    service.rejectMembership(id, reason, getLoginUserId());
    return success(true);
  }
}
