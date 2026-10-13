package cn.iocoder.yudao.module.hrm.controller.admin.payroll;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.service.payroll.*;
import java.util.List;
import javax.annotation.Resource;
import javax.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/hrm/payroll/tax-policy")
public class HrmPayrollTaxPolicyController {
  @Resource private HrmPayrollTaxPolicyService service;
  @Resource private HrmPayrollEntityService entities;

  @GetMapping("/entities")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:tax:query')")
  public CommonResult<List<HrmPayrollLegalEntityDO>> entities() {
    return success(entities.list(getLoginUserId()));
  }

  @GetMapping("/defaults")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:tax:update')")
  public CommonResult<HrmPayrollTaxPolicyReqVO> defaults(
      @RequestParam Long entityId, @RequestParam Integer year) {
    return success(service.defaults(entityId, year, getLoginUserId()));
  }

  @GetMapping("/list")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:tax:query')")
  public CommonResult<List<HrmPayrollTaxPolicyDO>> list(
      @RequestParam Long entityId, @RequestParam(required = false) Integer year) {
    return success(service.list(entityId, year, getLoginUserId()));
  }

  @PostMapping("/create")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:tax:update')")
  public CommonResult<Long> create(@Valid @RequestBody HrmPayrollTaxPolicyReqVO request) {
    return success(service.create(request, getLoginUserId()));
  }

  @PostMapping("/review")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:tax:approve')")
  public CommonResult<Boolean> review(
      @RequestParam Long id, @RequestParam boolean approved, @RequestParam String reason) {
    service.review(id, approved, reason, getLoginUserId());
    return success(true);
  }

  @PostMapping("/trial")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:tax:query')")
  public CommonResult<PayrollTaxPolicyEngine.Quote> trial(
      @Valid @RequestBody HrmPayrollTaxPolicyTrialReqVO request) {
    return success(service.trial(request, getLoginUserId()));
  }

  @GetMapping("/history")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:tax:query')")
  public CommonResult<List<HrmPayrollTaxPolicyEventDO>> history(@RequestParam Long id) {
    return success(service.history(id, getLoginUserId()));
  }
}
