package cn.iocoder.yudao.module.hrm.controller.admin.payroll;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollTaxCorrectionReqVO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.HrmPayrollTaxCorrectionDO;
import cn.iocoder.yudao.module.hrm.service.payroll.HrmPayrollTaxCorrectionService;
import java.util.List;
import javax.annotation.Resource;
import javax.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/hrm/payroll/tax-correction")
public class HrmPayrollTaxCorrectionController {
  @Resource private HrmPayrollTaxCorrectionService service;

  @PostMapping("/preview")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:tax:update')")
  public CommonResult<HrmPayrollTaxCorrectionDO> preview(
      @Valid @RequestBody HrmPayrollTaxCorrectionReqVO r) {
    return success(service.preview(r, getLoginUserId()));
  }

  @PostMapping("/submit")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:tax:update')")
  public CommonResult<Long> submit(@Valid @RequestBody HrmPayrollTaxCorrectionReqVO r) {
    return success(service.submit(r, getLoginUserId()));
  }

  @PostMapping("/review")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:tax:approve')")
  public CommonResult<Boolean> review(
      @RequestParam Long id, @RequestParam boolean approve, @RequestParam String reason) {
    service.review(id, approve, reason, getLoginUserId());
    return success(true);
  }

  @GetMapping("/list")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:tax:query')")
  public CommonResult<List<HrmPayrollTaxCorrectionDO>> list(@RequestParam Long paymentId) {
    return success(service.list(paymentId, getLoginUserId()));
  }
}
