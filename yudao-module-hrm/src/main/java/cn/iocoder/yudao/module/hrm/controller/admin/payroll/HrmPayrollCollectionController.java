package cn.iocoder.yudao.module.hrm.controller.admin.payroll;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.service.payroll.HrmPayrollCollectionService;
import cn.iocoder.yudao.module.hrm.service.payroll.HrmPayrollBaselineExporter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - HRM 薪酬需求征集")
@RestController
@RequestMapping("/hrm/payroll/requirements")
@Validated
public class HrmPayrollCollectionController {
    @Resource private HrmPayrollCollectionService service;

    @PostMapping("/initialize")
    @Operation(summary = "按租户登记候选需求与来源，不覆盖已有修改")
    @PreAuthorize("@ss.hasPermission('hrm:payroll:requirements:create')")
    public CommonResult<Map<String, Integer>> initialize() { return success(service.initialize()); }

    @GetMapping("/page")
    @Operation(summary = "按模块、状态、优先级及范围查询需求")
    @PreAuthorize("@ss.hasPermission('hrm:payroll:requirements:query')")
    public CommonResult<PageResult<HrmPayrollRequirementRespVO>> page(@Valid HrmPayrollRequirementPageReqVO query) {
        return success(BeanUtils.toBean(service.page(query), HrmPayrollRequirementRespVO.class));
    }

    @GetMapping("/get")
    @Operation(summary = "读取需求详情")
    @PreAuthorize("@ss.hasPermission('hrm:payroll:requirements:query')")
    public CommonResult<HrmPayrollRequirementRespVO> get(@RequestParam Long id) {
        return success(BeanUtils.toBean(service.get(id), HrmPayrollRequirementRespVO.class));
    }

    @GetMapping("/summary")
    @Operation(summary = "真实确认进度和来源就绪统计")
    @PreAuthorize("@ss.hasPermission('hrm:payroll:requirements:query')")
    public CommonResult<Map<String, Long>> summary() { return success(service.summary()); }

    @PostMapping("/create")
    @Operation(summary = "补充自定义需求")
    @PreAuthorize("@ss.hasPermission('hrm:payroll:requirements:create')")
    public CommonResult<Long> create(@Valid @RequestBody HrmPayrollRequirementSaveReqVO request) { return success(service.create(request)); }

    @PutMapping("/update")
    @Operation(summary = "保存新版本并重新进入待确认")
    @PreAuthorize("@ss.hasPermission('hrm:payroll:requirements:update')")
    public CommonResult<Boolean> update(@Valid @RequestBody HrmPayrollRequirementSaveReqVO request) {
        service.update(request); return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除业务补充需求，原型候选以范围决定维护")
    @PreAuthorize("@ss.hasPermission('hrm:payroll:requirements:delete')")
    public CommonResult<Boolean> delete(@RequestParam Long id, @RequestParam @Min(1) Integer version) {
        service.delete(id, version); return success(true);
    }

    @PostMapping("/review")
    @Operation(summary = "记录评审状态、操作者、时间和依据")
    @PreAuthorize("@ss.hasPermission('hrm:payroll:requirements:review')")
    public CommonResult<Boolean> review(@Valid @RequestBody HrmPayrollReviewReqVO request) {
        service.review(request); return success(true);
    }

    @GetMapping("/sources/list")
    @Operation(summary = "数据来源及独立就绪状态")
    @PreAuthorize("@ss.hasPermission('hrm:payroll:requirements:query')")
    public CommonResult<List<HrmPayrollSourceRespVO>> sources() { return success(BeanUtils.toBean(service.sources(), HrmPayrollSourceRespVO.class)); }

    @PostMapping("/sources/create")
    @Operation(summary = "补充数据来源")
    @PreAuthorize("@ss.hasPermission('hrm:payroll:source:update')")
    public CommonResult<Long> createSource(@Valid @RequestBody HrmPayrollSourceSaveReqVO request) { return success(service.createSource(request)); }

    @PutMapping("/sources/update")
    @Operation(summary = "维护字段映射、负责人及核验依据")
    @PreAuthorize("@ss.hasPermission('hrm:payroll:source:update')")
    public CommonResult<Boolean> updateSource(@Valid @RequestBody HrmPayrollSourceSaveReqVO request) {
        service.updateSource(request); return success(true);
    }

    @GetMapping("/history")
    @Operation(summary = "读取评审及变更版本记录")
    @PreAuthorize("@ss.hasPermission('hrm:payroll:requirements:query')")
    public CommonResult<List<HrmPayrollReviewDO>> history(@RequestParam String objectType, @RequestParam Long objectId) {
        return success(service.history(objectType, objectId));
    }

    @PostMapping("/baselines/create")
    @Operation(summary = "生成不可变评审基线")
    @PreAuthorize("@ss.hasPermission('hrm:payroll:requirements:export')")
    public CommonResult<Long> createBaseline() { return success(service.createBaseline()); }

    @GetMapping("/baselines/list")
    @Operation(summary = "最近一百个评审基线")
    @PreAuthorize("@ss.hasPermission('hrm:payroll:requirements:query')")
    public CommonResult<List<HrmPayrollBaselineRespVO>> baselines() {
        return success(BeanUtils.toBean(service.baselines(), HrmPayrollBaselineRespVO.class));
    }

    @GetMapping("/baselines/export")
    @Operation(summary = "下载指定基线的需求、来源及说明工作表")
    @PreAuthorize("@ss.hasPermission('hrm:payroll:requirements:export')")
    public void export(@RequestParam Long id, HttpServletResponse response) throws IOException {
        HrmPayrollBaselineExporter.write(service.baseline(id), response);
    }
}
