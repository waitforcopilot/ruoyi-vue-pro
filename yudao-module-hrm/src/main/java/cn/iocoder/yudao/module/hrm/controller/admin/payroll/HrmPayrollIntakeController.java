package cn.iocoder.yudao.module.hrm.controller.admin.payroll;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.intake.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.HrmPayrollReviewDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.HrmPayrollSourceDO;
import cn.iocoder.yudao.module.hrm.service.payroll.intake.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import javax.annotation.Resource;
import javax.validation.Valid;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.List;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.PAYROLL_INTAKE_INVALID;

@Tag(name = "管理后台 - HRM 薪酬数据接入准备")
@RestController
@RequestMapping("/hrm/payroll/intake")
@Validated
public class HrmPayrollIntakeController {
    @Resource private HrmPayrollIntakeService service;

    @GetMapping("/sources")
    @PreAuthorize("@ss.hasPermission('hrm:payroll:intake:query')")
    public CommonResult<List<HrmPayrollSourceDO>> sources() { return success(service.sourceOptions()); }

    @GetMapping("/contracts/page")
    @PreAuthorize("@ss.hasPermission('hrm:payroll:intake:query')")
    public CommonResult<PageResult<HrmPayrollContractRespVO>> contracts(@Valid HrmPayrollContractPageReqVO query) { return success(service.contracts(query)); }

    @GetMapping("/contracts/get")
    @PreAuthorize("@ss.hasPermission('hrm:payroll:intake:query')")
    public CommonResult<HrmPayrollContractRespVO> get(@RequestParam Long id) { return success(service.getContract(id)); }

    @PostMapping("/contracts/create")
    @PreAuthorize("@ss.hasPermission('hrm:payroll:intake:contract')")
    public CommonResult<Long> create(@Valid @RequestBody HrmPayrollContractSaveReqVO request) { return success(service.createContract(request)); }

    @PutMapping("/contracts/update")
    @PreAuthorize("@ss.hasPermission('hrm:payroll:intake:contract')")
    public CommonResult<Boolean> update(@Valid @RequestBody HrmPayrollContractSaveReqVO request) { service.updateContract(request); return success(true); }

    @PostMapping("/contracts/review")
    @PreAuthorize("@ss.hasPermission('hrm:payroll:intake:confirm')")
    public CommonResult<Boolean> review(@Valid @RequestBody HrmPayrollContractReviewReqVO request) { service.reviewContract(request); return success(true); }

    @GetMapping("/contracts/history")
    @PreAuthorize("@ss.hasPermission('hrm:payroll:intake:query')")
    public CommonResult<List<HrmPayrollReviewDO>> history(@RequestParam Long id) { return success(service.history(id)); }

    @GetMapping("/contracts/template")
    @PreAuthorize("@ss.hasPermission('hrm:payroll:intake:query')")
    public void template(@RequestParam Long id, HttpServletResponse response) throws IOException {
        byte[] bytes = service.template(id);
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=payroll-contract-" + id + ".csv");
        response.setContentLength(bytes.length);
        response.getOutputStream().write(bytes);
    }

    @PostMapping("/batches/preview")
    @PreAuthorize("@ss.hasPermission('hrm:payroll:intake:preview')")
    @ApiAccessLog(requestEnable = false, responseEnable = false)
    public CommonResult<Long> preview(@Valid HrmPayrollPreviewReqVO request, @RequestParam MultipartFile file) throws IOException {
        if (file.getSize() > HrmPayrollCsvValidator.MAX_BYTES) throw exception(PAYROLL_INTAKE_INVALID, "文件超过 1 MiB 上限");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (InputStream input = file.getInputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) {
                if (bytes.size() + count > HrmPayrollCsvValidator.MAX_BYTES) throw exception(PAYROLL_INTAKE_INVALID, "文件超过 1 MiB 上限");
                bytes.write(buffer, 0, count);
            }
        }
        return success(service.preview(request, file.getOriginalFilename(), bytes.toByteArray()));
    }

    @GetMapping("/batches/page")
    @PreAuthorize("@ss.hasPermission('hrm:payroll:intake:query')")
    @ApiAccessLog(requestEnable = false, responseEnable = false)
    public CommonResult<PageResult<HrmPayrollBatchRespVO>> batches(@Valid HrmPayrollBatchPageReqVO query) { return success(service.batches(query)); }

    @GetMapping("/batches/get")
    @PreAuthorize("@ss.hasPermission('hrm:payroll:intake:query')")
    @ApiAccessLog(requestEnable = false, responseEnable = false)
    public CommonResult<HrmPayrollBatchDetailRespVO> batch(@RequestParam Long id) { return success(service.batch(id)); }
}
