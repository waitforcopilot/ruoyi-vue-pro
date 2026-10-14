package cn.iocoder.yudao.module.hrm.controller.admin.payroll;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.PAYROLL_INPUT_INVALID;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.service.payroll.*;
import java.io.IOException;
import java.util.*;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/hrm/payroll/input")
@Validated
public class HrmPayrollInputController {
  @Resource private HrmPayrollInputService service;
  @Resource private HrmPayrollInputImportService imports;
  @Resource private HrmPayrollEntityService entities;

  @GetMapping("/readiness")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:input:query')")
  public CommonResult<List<HrmPayrollInputService.Readiness>> readiness(
      @RequestParam Long batchId) {
    return success(service.readiness(batchId, getLoginUserId()));
  }

  @GetMapping("/list")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:input:query')")
  public CommonResult<List<HrmPayrollInputDO>> list(@RequestParam Long batchId) {
    return success(service.list(batchId, getLoginUserId()));
  }

  @PostMapping("/inspect")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:input:update')")
  public CommonResult<HrmPayrollInputService.Prepared> inspect(
      @Valid @RequestBody HrmPayrollInputReqVO request) {
    return success(service.inspect(request, getLoginUserId()));
  }

  @PostMapping("/submit")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:input:update')")
  public CommonResult<Long> submit(@Valid @RequestBody HrmPayrollInputReqVO request) {
    return success(service.submit(request, getLoginUserId()));
  }

  @PostMapping("/review")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:input:review')")
  public CommonResult<Boolean> review(
      @RequestParam Long id, @RequestParam boolean approve, @RequestParam String reason) {
    service.review(id, approve, reason, getLoginUserId());
    return success(true);
  }

  @PostMapping("/import-preview")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:input:update')")
  public CommonResult<HrmPayrollInputImportDO> preview(
      @RequestParam Long batchId, @RequestParam MultipartFile file) throws IOException {
    service.batch(batchId, "MANAGE", getLoginUserId(), false);
    String name = file.getOriginalFilename();
    if (file.isEmpty()
        || file.getSize() > 10 * 1024 * 1024
        || name == null
        || !name.toLowerCase(Locale.ROOT).matches(".*\\.(xlsx|xls)$"))
      throw exception(PAYROLL_INPUT_INVALID);
    return success(
        imports.preview(
            batchId,
            name,
            cn.hutool.crypto.digest.DigestUtil.sha256Hex(file.getBytes()),
            ExcelUtils.read(file, HrmPayrollInputRowVO.class, 50001),
            getLoginUserId()));
  }

  @GetMapping("/imports")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:input:query')")
  public CommonResult<List<HrmPayrollInputImportDO>> imports(@RequestParam Long batchId) {
    return success(imports.list(batchId, getLoginUserId()));
  }

  @GetMapping("/import")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:input:query')")
  public CommonResult<HrmPayrollInputImportDO> getImport(@RequestParam Long id) {
    return success(imports.get(id, getLoginUserId()));
  }

  @PostMapping("/import-commit")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:input:update')")
  public CommonResult<List<Long>> commit(@RequestParam Long id) {
    return success(imports.commit(id, getLoginUserId()));
  }

  @PostMapping("/import-revalidate")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:input:update')")
  public CommonResult<HrmPayrollInputImportDO> revalidate(@RequestParam Long id) {
    return success(imports.revalidate(id, getLoginUserId()));
  }

  @GetMapping("/import-errors")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:input:export')")
  public void errors(@RequestParam Long id, HttpServletResponse response) throws IOException {
    HrmPayrollInputImportDO record = imports.getForExport(id, getLoginUserId());
    ExcelUtils.write(
        response,
        "核算输入错误行.xlsx",
        "错误行",
        HrmPayrollInsuranceImportService.RowError.class,
        JsonUtils.parseArray(record.getErrors(), HrmPayrollInsuranceImportService.RowError.class));
  }

  @GetMapping("/template")
  @PreAuthorize("@ss.hasPermission('hrm:payroll:input:export')")
  public void template(
      @RequestParam Long batchId,
      @RequestParam String kind,
      @RequestParam String mode,
      HttpServletResponse response)
      throws IOException {
    cn.iocoder.yudao.module.hrm.dal.dataobject.salary.monthrecord.HrmSalaryMonthRecordDO b =
        service.batch(batchId, "EXPORT", getLoginUserId(), false);
    if (!HrmPayrollInputService.KINDS.contains(kind)
        || !Arrays.asList("SYNC", "ZERO", "IMPORT", "CASH").contains(mode)
        || "OVERTIME".equals(kind) && "IMPORT".equals(mode)
        || "ATTENDANCE".equals(kind) && "ZERO".equals(mode)
        || "CASH".equals(mode) && !"CUMULATIVE_TAX".equals(kind)) throw exception(PAYROLL_INPUT_INVALID);
    String code = entities.approved(b.getEntityId()).getCode();
    List<HrmPayrollInputRowVO> rows = new ArrayList<>();
    for (cn.iocoder.yudao.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO e :
        service.roster(b))
      for (String field :
          "IMPORT".equals(mode) ? service.fields(kind) : Collections.singletonList(""))
        rows.add(
            new HrmPayrollInputRowVO()
                .setJobNumber(e.getJobNumber())
                .setEntityCode(code)
                .setPeriodFrom(b.getStartTime().toLocalDate().toString())
                .setPeriodTo(b.getEndTime().toLocalDate().toString())
                .setKind(kind)
                .setMode(mode)
                .setField(field));
    ExcelUtils.write(response, "核算输入模板.xlsx", "核算输入", HrmPayrollInputRowVO.class, rows);
  }
}
