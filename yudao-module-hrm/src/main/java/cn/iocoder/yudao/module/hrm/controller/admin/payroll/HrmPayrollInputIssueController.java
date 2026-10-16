package cn.iocoder.yudao.module.hrm.controller.admin.payroll;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.service.payroll.*;
import java.util.*;
import javax.annotation.Resource;
import javax.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/hrm/payroll/input-issue")
public class HrmPayrollInputIssueController {
 @Resource private HrmPayrollInputIssueService service;
 @cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog(requestEnable=false,responseEnable=false)
 @GetMapping("/selectors") @PreAuthorize("@ss.hasPermission('hrm:payroll:issue:query')")
 public CommonResult<Map<String,Object>> selectors(@RequestParam(required=false) Long entityId){return success(service.selectors(entityId,getLoginUserId()));}
 @cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog(requestEnable=false,responseEnable=false)
 @GetMapping("/employees") @PreAuthorize("@ss.hasPermission('hrm:payroll:issue:query')")
 public CommonResult<List<Map<String,Object>>> employees(@RequestParam Long batchId){return success(service.employees(batchId,getLoginUserId()));}
 @cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog(requestEnable=false,responseEnable=false)
 @GetMapping("/signals") @PreAuthorize("@ss.hasPermission('hrm:payroll:issue:query')")
 public CommonResult<List<HrmPayrollReadinessService.Signal>> signals(@RequestParam Long batchId,@RequestParam Long employeeId){return success(service.signals(batchId,employeeId,getLoginUserId()));}
 @cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog(requestEnable=false,responseEnable=false)
 @GetMapping("/list") @PreAuthorize("@ss.hasPermission('hrm:payroll:issue:query')")
 public CommonResult<List<HrmPayrollInputIssueDO>> list(@RequestParam Long batchId){return success(service.list(batchId,getLoginUserId()));}
 @cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog(requestEnable=false,responseEnable=false)
 @GetMapping("/history") @PreAuthorize("@ss.hasPermission('hrm:payroll:issue:query')")
 public CommonResult<List<HrmPayrollInputIssueEventDO>> history(@RequestParam Long id){return success(service.history(id,getLoginUserId(),false));}
 @cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog(requestEnable=false,responseEnable=false)
 @GetMapping("/options") @PreAuthorize("@ss.hasPermission('hrm:payroll:issue:update')")
 public CommonResult<Map<String,Object>> options(@RequestParam Long batchId,@RequestParam Long employeeId){return success(service.options(batchId,employeeId,getLoginUserId()));}
 @cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog(requestEnable=false,responseEnable=false)
 @PostMapping("/create") @PreAuthorize("@ss.hasPermission('hrm:payroll:issue:update')")
 public CommonResult<Long> create(@Valid @RequestBody HrmPayrollInputIssueReqVO req){return success(service.create(req,getLoginUserId()));}
 @cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog(requestEnable=false,responseEnable=false)
 @PostMapping("/submit") @PreAuthorize("@ss.hasPermission('hrm:payroll:issue:update')")
 public CommonResult<Boolean> submit(@Valid @RequestBody HrmPayrollInputIssueActionReqVO req){service.submit(req.getId(),req.getEvidence(),getLoginUserId());return success(true);}
 @cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog(requestEnable=false,responseEnable=false)
 @PostMapping("/review") @PreAuthorize("@ss.hasPermission('hrm:payroll:issue:review')")
 public CommonResult<Boolean> review(@Valid @RequestBody HrmPayrollInputIssueActionReqVO req){if(req.getApproved()==null)throw cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception(cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.PAYROLL_INPUT_ISSUE_INVALID);service.review(req.getId(),req.getApproved(),req.getReason(),getLoginUserId());return success(true);}
 @cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog(requestEnable=false,responseEnable=false)
 @PostMapping("/reassign") @PreAuthorize("@ss.hasPermission('hrm:payroll:issue:update')")
 public CommonResult<Boolean> reassign(@Valid @RequestBody HrmPayrollInputIssueAssignReqVO req){service.reassign(req,getLoginUserId());return success(true);}
 @cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog(requestEnable=false,responseEnable=false)
 @PostMapping("/reopen") @PreAuthorize("@ss.hasPermission('hrm:payroll:issue:review')")
 public CommonResult<Boolean> reopen(@Valid @RequestBody HrmPayrollInputIssueActionReqVO req){service.reopen(req.getId(),req.getReason(),getLoginUserId());return success(true);}
}
