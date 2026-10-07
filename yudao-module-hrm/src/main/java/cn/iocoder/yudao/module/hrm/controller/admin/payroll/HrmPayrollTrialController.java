package cn.iocoder.yudao.module.hrm.controller.admin.payroll;
import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.*;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.trial.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.HrmPayrollReviewDO;
import cn.iocoder.yudao.module.hrm.service.payroll.trial.HrmPayrollTrialService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import java.util.List;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
@Tag(name="管理后台 - HRM 批次核验与版本化试算")
@RestController @Validated @RequestMapping("/hrm/payroll/trial-batches")
public class HrmPayrollTrialController {
 @Resource private HrmPayrollTrialService service;
 @GetMapping("/page") @PreAuthorize("@ss.hasPermission('hrm:payroll:trial:query') and @ss.hasPermission('hrm:payroll:calculation:query') and @ss.hasPermission('hrm:payroll:eligibility:query') and @ss.hasPermission('hrm:employee:query')")
 @ApiAccessLog(requestEnable=false,responseEnable=false)
 public CommonResult<PageResult<HrmPayrollTrialRespVO>> page(@Valid HrmPayrollTrialPageReqVO req) { return success(service.page(req)); }
 @GetMapping("/get") @PreAuthorize("@ss.hasPermission('hrm:payroll:trial:query') and @ss.hasPermission('hrm:payroll:calculation:query') and @ss.hasPermission('hrm:payroll:eligibility:query') and @ss.hasPermission('hrm:employee:query')")
 @ApiAccessLog(requestEnable=false,responseEnable=false)
 public CommonResult<HrmPayrollTrialRespVO> get(@RequestParam @Min(1) Long id) { return success(service.get(id)); }
 @PostMapping("/create") @PreAuthorize("@ss.hasPermission('hrm:payroll:trial:query') and @ss.hasPermission('hrm:payroll:calculation:query') and @ss.hasPermission('hrm:payroll:eligibility:query') and @ss.hasPermission('hrm:employee:query') and @ss.hasPermission('hrm:payroll:trial:maintain')")
 @ApiAccessLog(requestEnable=false,responseEnable=false)
 public CommonResult<Long> create(@Valid @RequestBody HrmPayrollTrialSaveReqVO req) { return success(service.create(req)); }
 @PutMapping("/update") @PreAuthorize("@ss.hasPermission('hrm:payroll:trial:query') and @ss.hasPermission('hrm:payroll:calculation:query') and @ss.hasPermission('hrm:payroll:eligibility:query') and @ss.hasPermission('hrm:employee:query') and @ss.hasPermission('hrm:payroll:trial:maintain')")
 @ApiAccessLog(requestEnable=false,responseEnable=false)
 public CommonResult<Boolean> update(@Valid @RequestBody HrmPayrollTrialSaveReqVO req) { service.update(req); return success(true); }
 @GetMapping("/check") @PreAuthorize("@ss.hasPermission('hrm:payroll:trial:query') and @ss.hasPermission('hrm:payroll:calculation:query') and @ss.hasPermission('hrm:payroll:eligibility:query') and @ss.hasPermission('hrm:employee:query')")
 @ApiAccessLog(requestEnable=false,responseEnable=false)
 public CommonResult<HrmPayrollTrialCheckVO> check(@RequestParam @Min(1) Long id) { return success(service.check(id)); }
 @PostMapping("/execute") @PreAuthorize("@ss.hasPermission('hrm:payroll:trial:query') and @ss.hasPermission('hrm:payroll:calculation:query') and @ss.hasPermission('hrm:payroll:eligibility:query') and @ss.hasPermission('hrm:employee:query') and @ss.hasPermission('hrm:payroll:trial:execute')")
 @ApiAccessLog(requestEnable=false,responseEnable=false)
 public CommonResult<HrmPayrollTrialRunRespVO> execute(@Valid @RequestBody HrmPayrollTrialExecuteReqVO req) { return success(service.execute(req)); }
 @GetMapping("/runs") @PreAuthorize("@ss.hasPermission('hrm:payroll:trial:query') and @ss.hasPermission('hrm:payroll:calculation:query') and @ss.hasPermission('hrm:payroll:eligibility:query') and @ss.hasPermission('hrm:employee:query')")
 @ApiAccessLog(requestEnable=false,responseEnable=false)
 public CommonResult<List<HrmPayrollTrialRunRespVO>> runs(@RequestParam @Min(1) Long id) { return success(service.runs(id)); }
 @GetMapping("/run") @PreAuthorize("@ss.hasPermission('hrm:payroll:trial:query') and @ss.hasPermission('hrm:payroll:calculation:query') and @ss.hasPermission('hrm:payroll:eligibility:query') and @ss.hasPermission('hrm:employee:query')")
 @ApiAccessLog(requestEnable=false,responseEnable=false)
 public CommonResult<HrmPayrollTrialRunRespVO> run(@RequestParam @Min(1) Long id) { return success(service.run(id)); }
 @GetMapping("/compare") @PreAuthorize("@ss.hasPermission('hrm:payroll:trial:query') and @ss.hasPermission('hrm:payroll:calculation:query') and @ss.hasPermission('hrm:payroll:eligibility:query') and @ss.hasPermission('hrm:employee:query')")
 @ApiAccessLog(requestEnable=false,responseEnable=false)
 public CommonResult<HrmPayrollTrialCompareVO> compare(@RequestParam @Min(1) Long leftId,@RequestParam @Min(1) Long rightId) { return success(service.compare(leftId,rightId)); }
 @GetMapping("/history") @PreAuthorize("@ss.hasPermission('hrm:payroll:trial:query') and @ss.hasPermission('hrm:payroll:calculation:query') and @ss.hasPermission('hrm:payroll:eligibility:query') and @ss.hasPermission('hrm:employee:query')")
 @ApiAccessLog(requestEnable=false,responseEnable=false)
 public CommonResult<List<HrmPayrollReviewDO>> history(@RequestParam @Min(1) Long id) { return success(service.history(id)); }
}
