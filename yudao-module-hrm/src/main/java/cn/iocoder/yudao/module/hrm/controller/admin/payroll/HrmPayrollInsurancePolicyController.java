package cn.iocoder.yudao.module.hrm.controller.admin.payroll;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollInsuranceReqVO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.service.payroll.HrmPayrollEntityService;
import cn.iocoder.yudao.module.hrm.service.payroll.HrmPayrollInsurancePolicyService;
import java.time.YearMonth;
import java.util.*;
import javax.annotation.Resource;
import javax.validation.Valid;
import javax.validation.constraints.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/hrm/payroll/insurance-policy")
@Validated
public class HrmPayrollInsurancePolicyController {
  @Resource private HrmPayrollInsurancePolicyService service;
  @Resource private HrmPayrollEntityService entities;

  @Resource
  private cn.iocoder.yudao.module.hrm.service.payroll.HrmPayrollInsuranceImportService imports;

  @PostMapping("/import-preview")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:insurance:update')")
  public CommonResult<HrmPayrollInsuranceImportDO> importPreview(
      @RequestParam Long entityId,
      @RequestParam org.springframework.web.multipart.MultipartFile file)
      throws java.io.IOException {
    entities.check(entityId, "MANAGE", getLoginUserId());
    String name = file.getOriginalFilename();
    if (file.getSize() == 0
        || file.getSize() > 10 * 1024 * 1024
        || name == null
        || !name.toLowerCase(java.util.Locale.ROOT).matches(".*\\.(xlsx|xls)$"))
      throw cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception(
          cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.PAYROLL_INSURANCE_INVALID);
    return success(
        imports.preview(
            entityId,
            name,
            cn.hutool.crypto.digest.DigestUtil.sha256Hex(file.getBytes()),
            cn.iocoder.yudao.framework.excel.core.util.ExcelUtils.read(
                file,
                cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollInsuranceBaseRowVO
                    .class,
                50001),
            getLoginUserId()));
  }

  @PostMapping("/import-commit")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:insurance:update')")
  public CommonResult<List<Long>> commitImport(@RequestParam Long id) {
    return success(imports.commit(id, getLoginUserId()));
  }

  @GetMapping("/imports")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:insurance:query')")
  public CommonResult<List<HrmPayrollInsuranceImportDO>> imports(@RequestParam Long entityId) {
    return success(imports.list(entityId, getLoginUserId()));
  }

  @GetMapping("/import")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:insurance:query')")
  public CommonResult<HrmPayrollInsuranceImportDO> importDetails(@RequestParam Long id) {
    return success(imports.get(id, getLoginUserId()));
  }

  @GetMapping("/import-errors")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:insurance:export')")
  public void importErrors(@RequestParam Long id, javax.servlet.http.HttpServletResponse response)
      throws java.io.IOException {
    HrmPayrollInsuranceImportDO record = imports.get(id, getLoginUserId());
    entities.check(record.getEntityId(), "EXPORT", getLoginUserId());
    cn.iocoder.yudao.framework.excel.core.util.ExcelUtils.write(
        response,
        "缴费申报导入错误.xlsx",
        "行错误",
        cn.iocoder.yudao.module.hrm.service.payroll.HrmPayrollInsuranceImportService.RowError.class,
        cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseArray(
            record.getErrors(),
            cn.iocoder.yudao.module.hrm.service.payroll.HrmPayrollInsuranceImportService.RowError
                .class));
  }

  @GetMapping("/base-template")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:insurance:export')")
  public void baseTemplate(
      @RequestParam Long policyId, javax.servlet.http.HttpServletResponse response)
      throws java.io.IOException {
    HrmPayrollInsurancePolicyDO policy = service.policy(policyId, getLoginUserId());
    entities.check(policy.getEntityId(), "EXPORT", getLoginUserId());
    List<cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollInsuranceBaseRowVO>
        rows = new ArrayList<>();
    for (HrmPayrollInsuranceReqVO.Project project :
        cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseArray(
            policy.getProjects(), HrmPayrollInsuranceReqVO.Project.class))
      rows.add(
          new cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollInsuranceBaseRowVO()
              .setCoverage("INSURED")
              .setPolicyCode(policy.getCode())
              .setFromMonth(java.time.YearMonth.from(policy.getEffectiveFrom()).toString())
              .setToMonth(java.time.YearMonth.from(policy.getEffectiveTo()).toString())
              .setProjectCode(project.getCode()));
    cn.iocoder.yudao.framework.excel.core.util.ExcelUtils.write(
        response,
        "年度缴费申报模板.xlsx",
        "申报",
        cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollInsuranceBaseRowVO.class,
        rows);
  }

  @GetMapping("/policies")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:insurance:query')")
  public CommonResult<List<HrmPayrollInsurancePolicyDO>> policies(@RequestParam Long entityId) {
    return success(service.policies(entityId, getLoginUserId()));
  }

  @GetMapping("/bases")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:insurance:query')")
  public CommonResult<List<HrmPayrollInsuranceBaseDO>> bases(@RequestParam Long entityId) {
    return success(service.bases(entityId, getLoginUserId()));
  }

  @GetMapping("/history")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:insurance:query')")
  public CommonResult<List<HrmPayrollInsuranceEventDO>> history(@RequestParam Long entityId) {
    return success(service.history(entityId, getLoginUserId()));
  }

  @GetMapping("/options")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:insurance:update')")
  public CommonResult<Map<String, Object>> options(@RequestParam Long entityId) {
    return success(entities.options(entityId, getLoginUserId()));
  }

  @PostMapping("/policy")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:insurance:update')")
  public CommonResult<Long> createPolicy(@Valid @RequestBody HrmPayrollInsuranceReqVO request) {
    return success(service.createPolicy(request, getLoginUserId()));
  }

  @PostMapping("/policy/approve")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:insurance:approve')")
  public CommonResult<Boolean> approvePolicy(@RequestParam Long id, @RequestParam String reason) {
    service.approvePolicy(id, reason, getLoginUserId());
    return success(true);
  }

  @PostMapping("/policy/reject")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:insurance:approve')")
  public CommonResult<Boolean> rejectPolicy(@RequestParam Long id, @RequestParam String reason) {
    service.rejectPolicy(id, reason, getLoginUserId());
    return success(true);
  }

  @PostMapping("/base")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:insurance:update')")
  public CommonResult<Long> proposeBase(@Valid @RequestBody HrmPayrollInsuranceReqVO.Base request) {
    return success(service.proposeBase(request, getLoginUserId()));
  }

  @PostMapping("/base/review")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:insurance:review')")
  public CommonResult<Boolean> reviewBase(@RequestParam Long id, @RequestParam String reason) {
    service.reviewBase(id, reason, getLoginUserId());
    return success(true);
  }

  @PostMapping("/base/approve")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:insurance:approve')")
  public CommonResult<Boolean> approveBase(@RequestParam Long id, @RequestParam String reason) {
    service.approveBase(id, reason, getLoginUserId());
    return success(true);
  }

  @PostMapping("/base/reject")
  @PreAuthorize(
      "@ss.hasPermission('hrm:payroll:insurance:review') or"
          + " @ss.hasPermission('hrm:payroll:insurance:approve')")
  public CommonResult<Boolean> rejectBase(@RequestParam Long id, @RequestParam String reason) {
    service.rejectBase(id, reason, getLoginUserId());
    return success(true);
  }

  @GetMapping("/preview")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:insurance:query')")
  public CommonResult<Map<Long, HrmPayrollInsurancePolicyService.Quote>> preview(
      @RequestParam Long entityId,
      @RequestParam @Min(2000) @Max(2100) Integer year,
      @RequestParam @Min(1) @Max(12) Integer month) {
    return success(service.previewForEntity(entityId, YearMonth.of(year, month), getLoginUserId()));
  }
}
