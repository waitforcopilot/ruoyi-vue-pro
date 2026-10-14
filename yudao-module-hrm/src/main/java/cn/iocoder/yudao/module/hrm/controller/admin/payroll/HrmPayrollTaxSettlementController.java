package cn.iocoder.yudao.module.hrm.controller.admin.payroll;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.service.payroll.HrmPayrollTaxSettlementService;
import java.util.List;
import javax.annotation.Resource;
import javax.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/hrm/payroll/tax-settlement")
public class HrmPayrollTaxSettlementController {
  @Resource private HrmPayrollTaxSettlementService service;

  @GetMapping("/balances")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:tax:query')")
  public CommonResult<List<HrmPayrollTaxSettlementService.Balance>> balances(
      @RequestParam Long filingId) {
    return success(service.balances(filingId, getLoginUserId()));
  }

  @GetMapping("/list")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:tax:query')")
  public CommonResult<List<HrmPayrollTaxSettlementDO>> list(@RequestParam Long filingId) {
    return success(service.list(filingId, getLoginUserId()));
  }

  @PostMapping("/submit")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:tax:update')")
  public CommonResult<Long> submit(@Valid @RequestBody HrmPayrollTaxSettlementReqVO r) {
    return success(service.submit(r, getLoginUserId()));
  }

  @PostMapping("/review")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:tax:approve')")
  public CommonResult<Boolean> review(
      @RequestParam Long id, @RequestParam boolean approve, @RequestParam String reason) {
    service.review(id, approve, reason, getLoginUserId());
    return success(true);
  }

  @PostMapping("/confirm")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:payment:reconcile')")
  public CommonResult<Boolean> confirm(@Valid @RequestBody HrmPayrollTaxSettlementConfirmReqVO r) {
    service.confirm(r, getLoginUserId());
    return success(true);
  }

  @PostMapping("/cancel")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:tax:approve')")
  public CommonResult<Boolean> cancel(
      @RequestParam Long id,
      @RequestParam String reason,
      @RequestParam(required = false) String bankVoidEvidence) {
    service.cancel(id, reason, bankVoidEvidence, getLoginUserId());
    return success(true);
  }
}
