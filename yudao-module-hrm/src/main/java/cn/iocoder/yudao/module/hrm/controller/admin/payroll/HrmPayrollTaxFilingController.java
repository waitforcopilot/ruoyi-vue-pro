package cn.iocoder.yudao.module.hrm.controller.admin.payroll;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.service.payroll.HrmPayrollTaxFilingService;
import java.time.LocalDate;
import java.util.*;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/hrm/payroll/tax-filing")
public class HrmPayrollTaxFilingController {
  @Resource private HrmPayrollTaxFilingService service;

  @GetMapping("/list")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:tax:query')")
  public CommonResult<List<HrmPayrollTaxFilingDO>> list(
      @RequestParam Long entityId,
      @RequestParam Integer year,
      @RequestParam(required = false) Integer month) {
    return success(service.list(entityId, year, month, getLoginUserId()));
  }

  @PostMapping("/create")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:tax:update')")
  public CommonResult<Long> create(@Valid @RequestBody HrmPayrollTaxFilingReqVO req) {
    return success(service.create(req, getLoginUserId()));
  }

  @PostMapping("/review")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:tax:approve')")
  public CommonResult<Boolean> review(
      @RequestParam Long id, @RequestParam boolean approved, @RequestParam String reason) {
    service.review(id, approved, reason, getLoginUserId());
    return success(true);
  }

  @GetMapping("/detail")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:tax:query')")
  public CommonResult<Map<String, Object>> detail(@RequestParam Long id) {
    return success(service.detail(id, getLoginUserId()));
  }

  @cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog(
      requestEnable = false,
      responseEnable = false)
  @GetMapping("/export")
  @PreAuthorize(
      "@ss.hasPermission('hrm:payroll:tax:query') && @ss.hasPermission('hrm:payroll:tax:private')")
  public void export(@RequestParam Long id, HttpServletResponse response)
      throws java.io.IOException {
    ExcelUtils.write(
        response,
        "个税申报准备.xlsx",
        "申报来源",
        HrmPayrollTaxFilingExportRowVO.class,
        service.export(id, getLoginUserId()));
  }

  @PostMapping("/filed")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:tax:update')")
  public CommonResult<Boolean> filed(
      @RequestParam Long id,
      @RequestParam String reference,
      @RequestParam
          @org.springframework.format.annotation.DateTimeFormat(
              iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
          LocalDate date,
      @RequestParam String evidence) {
    service.filed(id, reference, date, evidence, getLoginUserId());
    return success(true);
  }

  @PostMapping("/receipt")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:tax:update')")
  public CommonResult<Long> receipt(@Valid @RequestBody HrmPayrollTaxReceiptReqVO req) {
    return success(service.receipt(req, getLoginUserId()));
  }

  @PostMapping("/receipt-review")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:tax:approve')")
  public CommonResult<Boolean> reviewReceipt(
      @RequestParam Long id, @RequestParam boolean approved, @RequestParam String reason) {
    service.reviewReceipt(id, approved, reason, getLoginUserId());
    return success(true);
  }

  @PostMapping("/reconcile")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:tax:approve')")
  public CommonResult<Boolean> reconcile(@RequestParam Long id, @RequestParam String reason) {
    service.reconcile(id, reason, getLoginUserId());
    return success(true);
  }

  @GetMapping("/receipt-template")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:tax:update')")
  public void receiptTemplate(HttpServletResponse response) throws java.io.IOException {
    ExcelUtils.write(
        response,
        "完税凭证导入模板.xlsx",
        "凭证",
        HrmPayrollTaxReceiptImportRowVO.class,
        Collections.singletonList(new HrmPayrollTaxReceiptImportRowVO().setKind("缴税")));
  }

  @PostMapping("/receipt-import")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:tax:update')")
  public CommonResult<List<Long>> receiptImport(
      @RequestParam Long id, @RequestParam MultipartFile file) throws java.io.IOException {
    String digest = cn.hutool.crypto.digest.DigestUtil.sha256Hex(file.getBytes());
    return success(
        service.importReceipts(
            id,
            ExcelUtils.read(file, HrmPayrollTaxReceiptImportRowVO.class),
            digest,
            getLoginUserId()));
  }

  @GetMapping("/receipt-export")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:tax:query')")
  public void exportReceipts(@RequestParam Long id, HttpServletResponse response)
      throws java.io.IOException {
    List<HrmPayrollTaxReceiptExportRowVO> output = new ArrayList<>();
    for (HrmPayrollTaxReceiptDO row : service.exportReceipts(id, getLoginUserId()))
      output.add(
          BeanUtils.toBean(row, HrmPayrollTaxReceiptExportRowVO.class)
              .setPaymentDate(row.getPaymentDate().toString())
              .setKind("REFUND".equals(row.getKind()) ? "退税" : "缴税"));
    ExcelUtils.write(
        response, "完税对账凭证.xlsx", "完税凭证", HrmPayrollTaxReceiptExportRowVO.class, output);
  }
}
