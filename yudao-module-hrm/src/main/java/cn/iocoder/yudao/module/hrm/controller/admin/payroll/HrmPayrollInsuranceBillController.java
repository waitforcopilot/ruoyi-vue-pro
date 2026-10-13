package cn.iocoder.yudao.module.hrm.controller.admin.payroll;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.PAYROLL_INSURANCE_INVALID;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.servlet.ServletUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.service.payroll.*;
import java.io.IOException;
import java.time.YearMonth;
import java.util.*;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/hrm/payroll/insurance-bill")
@Validated
public class HrmPayrollInsuranceBillController {
  @Resource private HrmPayrollInsuranceBillService service;
  @Resource private HrmPayrollEntityService entities;

  @GetMapping("/list")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:insurance-bill:query')")
  public CommonResult<List<HrmPayrollInsuranceBillDO>> list(@RequestParam Long entityId) {
    return success(service.list(entityId, getLoginUserId()));
  }

  @GetMapping("/get")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:insurance-bill:query')")
  public CommonResult<HrmPayrollInsuranceBillDO> get(@RequestParam Long id) {
    return success(service.get(id, getLoginUserId()));
  }

  @GetMapping("/lines")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:insurance-bill:query')")
  public CommonResult<List<HrmPayrollInsuranceBillLineDO>> lines(@RequestParam Long id) {
    return success(service.lines(id, getLoginUserId()));
  }

  @GetMapping("/settlements")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:insurance-bill:query')")
  public CommonResult<List<HrmPayrollInsuranceSettlementDO>> settlements(@RequestParam Long id) {
    return success(service.settlements(id, getLoginUserId()));
  }

  @PostMapping("/preview")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:insurance-bill:update')")
  public CommonResult<HrmPayrollInsuranceBillDO> preview(
      @RequestParam Long entityId,
      @RequestParam @Min(2000) @Max(2100) Integer year,
      @RequestParam @Min(1) @Max(12) Integer month,
      @RequestParam String reference,
      @RequestParam String evidence,
      @RequestParam MultipartFile file)
      throws IOException {
    entities.check(entityId, "MANAGE", getLoginUserId());
    String name = file.getOriginalFilename();
    if (file.isEmpty()
        || file.getSize() > 10 * 1024 * 1024
        || name == null
        || !name.toLowerCase(Locale.ROOT).matches(".*\\.(xlsx|xls)$"))
      throw exception(PAYROLL_INSURANCE_INVALID);
    return success(
        service.preview(
            entityId,
            YearMonth.of(year, month),
            reference,
            evidence,
            name,
            cn.hutool.crypto.digest.DigestUtil.sha256Hex(file.getBytes()),
            ExcelUtils.read(file, HrmPayrollInsuranceBillRowVO.class, 50001),
            getLoginUserId()));
  }

  @PostMapping("/revalidate")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:insurance-bill:update')")
  public CommonResult<HrmPayrollInsuranceBillDO> revalidate(@RequestParam Long id) {
    return success(service.revalidate(id, getLoginUserId()));
  }

  @PostMapping("/submit")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:insurance-bill:update')")
  public CommonResult<Boolean> submit(@RequestParam Long id) {
    service.submit(id, getLoginUserId());
    return success(true);
  }

  @PostMapping("/review")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:insurance-bill:review')")
  public CommonResult<Boolean> review(@RequestParam Long id, @RequestParam String reason) {
    service.review(id, reason, getLoginUserId());
    return success(true);
  }

  @PostMapping("/approve")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:insurance-bill:approve')")
  public CommonResult<Boolean> approve(@RequestParam Long id, @RequestParam String reason) {
    service.approve(id, reason, getLoginUserId());
    return success(true);
  }

  @PostMapping("/reject")
  @PreAuthorize(
      "@ss.hasPermission('hrm:payroll:insurance-bill:review') or"
          + " @ss.hasPermission('hrm:payroll:insurance-bill:approve')")
  public CommonResult<Boolean> reject(@RequestParam Long id, @RequestParam String reason) {
    service.reject(id, reason, getLoginUserId());
    return success(true);
  }

  @PostMapping("/settlement")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:insurance-bill:update')")
  public CommonResult<Long> proposeSettlement(
      @RequestParam String metadata, @RequestParam MultipartFile proof) throws IOException {
    HrmPayrollInsuranceSettlementReqVO request =
        JsonUtils.parseObject(metadata, HrmPayrollInsuranceSettlementReqVO.class);
    if (request == null || proof.isEmpty() || proof.getSize() > 2 * 1024 * 1024)
      throw exception(PAYROLL_INSURANCE_INVALID);
    return success(
        service.propose(request, proof.getBytes(), proof.getOriginalFilename(), getLoginUserId()));
  }

  @PostMapping("/settlement/review")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:insurance-bill:review')")
  public CommonResult<Boolean> reviewSettlement(
      @RequestParam Long id, @RequestParam String reason) {
    service.transitionSettlement(id, "review", reason, getLoginUserId());
    return success(true);
  }

  @PostMapping("/settlement/approve")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:insurance-bill:approve')")
  public CommonResult<Boolean> approveSettlement(
      @RequestParam Long id, @RequestParam String reason) {
    service.transitionSettlement(id, "approve", reason, getLoginUserId());
    return success(true);
  }

  @PostMapping("/settlement/reject")
  @PreAuthorize(
      "@ss.hasPermission('hrm:payroll:insurance-bill:review') or"
          + " @ss.hasPermission('hrm:payroll:insurance-bill:approve')")
  public CommonResult<Boolean> rejectSettlement(
      @RequestParam Long id, @RequestParam String reason) {
    service.transitionSettlement(id, "reject", reason, getLoginUserId());
    return success(true);
  }

  @GetMapping("/settlement/proof")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:insurance-bill:export')")
  public void proof(@RequestParam Long id, HttpServletResponse response) throws IOException {
    HrmPayrollInsuranceSettlementDO s = service.proof(id, getLoginUserId());
    ServletUtils.writeAttachment(
        response, s.getProofName(), Base64.getDecoder().decode(s.getProofContent()));
  }

  @GetMapping("/template")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:insurance-bill:export')")
  public void template(@RequestParam Long entityId, HttpServletResponse response)
      throws IOException {
    entities.check(entityId, "EXPORT", getLoginUserId());
    ExcelUtils.write(
        response, "实缴账单模板.xlsx", "账单", HrmPayrollInsuranceBillRowVO.class, Collections.emptyList());
  }

  @GetMapping("/errors")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:insurance-bill:export')")
  public void errors(@RequestParam Long id, HttpServletResponse response) throws IOException {
    HrmPayrollInsuranceBillDO b = service.getForExport(id, getLoginUserId());
    entities.check(b.getEntityId(), "EXPORT", getLoginUserId());
    ExcelUtils.write(
        response,
        "实缴账单错误行.xlsx",
        "错误行",
        HrmPayrollInsuranceImportService.RowError.class,
        JsonUtils.parseArray(b.getErrors(), HrmPayrollInsuranceImportService.RowError.class));
  }

  @GetMapping("/export")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:insurance-bill:export')")
  public void export(@RequestParam Long id, HttpServletResponse response) throws IOException {
    HrmPayrollInsuranceBillDO b = service.getForExport(id, getLoginUserId());
    entities.check(b.getEntityId(), "EXPORT", getLoginUserId());
    List<HrmPayrollInsuranceBillLineDO> data = service.exportLines(id, getLoginUserId());
    ExcelUtils.write(response, "缴费应实差异.xlsx", "应实对账", HrmPayrollInsuranceBillLineDO.class, data);
  }
}
