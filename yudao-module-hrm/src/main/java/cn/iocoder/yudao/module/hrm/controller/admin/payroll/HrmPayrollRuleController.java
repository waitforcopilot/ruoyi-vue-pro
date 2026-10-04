package cn.iocoder.yudao.module.hrm.controller.admin.payroll;
import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.*;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.rule.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.HrmPayrollReviewDO;
import cn.iocoder.yudao.module.hrm.service.payroll.rule.HrmPayrollRuleService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import java.util.List;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name="管理后台 - HRM 薪酬规则台账")
@RestController
@RequestMapping("/hrm/payroll/rules")
@Validated
public class HrmPayrollRuleController {
    @Resource private HrmPayrollRuleService service;
    @PostMapping("/initialize")
    @PreAuthorize("@ss.hasPermission('hrm:payroll:rule:maintain')")
    public CommonResult<Integer> initialize() { return success(service.initialize()); }
    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermission('hrm:payroll:rule:query')")
    @ApiAccessLog(requestEnable=false,responseEnable=false)
    public CommonResult<PageResult<HrmPayrollRuleRespVO>> page(@Valid HrmPayrollRulePageReqVO query) { return success(service.page(query)); }
    @GetMapping("/get")
    @PreAuthorize("@ss.hasPermission('hrm:payroll:rule:query')")
    @ApiAccessLog(requestEnable=false,responseEnable=false)
    public CommonResult<HrmPayrollRuleRespVO> get(@RequestParam Long id) { return success(service.get(id)); }
    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermission('hrm:payroll:rule:maintain')")
    @ApiAccessLog(requestEnable=false,responseEnable=false)
    public CommonResult<Long> create(@Valid @RequestBody HrmPayrollRuleSaveReqVO request) { return success(service.create(request)); }
    @PostMapping("/new-version")
    @PreAuthorize("@ss.hasPermission('hrm:payroll:rule:maintain')")
    public CommonResult<Long> version(@RequestParam Long id,@RequestParam @Min(1) Integer revision) { return success(service.newVersion(id,revision)); }
    @PutMapping("/update")
    @PreAuthorize("@ss.hasPermission('hrm:payroll:rule:maintain')")
    @ApiAccessLog(requestEnable=false,responseEnable=false)
    public CommonResult<Boolean> update(@Valid @RequestBody HrmPayrollRuleSaveReqVO request) { service.update(request);return success(true); }
    @PostMapping("/review")
    @PreAuthorize("@ss.hasPermission('hrm:payroll:rule:review')")
    @ApiAccessLog(requestEnable=false,responseEnable=false)
    public CommonResult<Boolean> review(@Valid @RequestBody HrmPayrollRuleReviewReqVO request) { service.review(request);return success(true); }
    @GetMapping("/history")
    @PreAuthorize("@ss.hasPermission('hrm:payroll:rule:query')")
    @ApiAccessLog(requestEnable=false,responseEnable=false)
    public CommonResult<List<HrmPayrollReviewDO>> history(@RequestParam Long id) { return success(service.history(id)); }
}
