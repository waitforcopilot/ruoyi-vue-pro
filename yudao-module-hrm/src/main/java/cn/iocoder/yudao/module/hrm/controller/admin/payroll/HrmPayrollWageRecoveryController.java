package cn.iocoder.yudao.module.hrm.controller.admin.payroll;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.service.payroll.HrmPayrollWageRecoveryService;
import java.util.*;
import javax.annotation.Resource;
import javax.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/hrm/payroll/wage-recovery")
public class HrmPayrollWageRecoveryController {
 @Resource private HrmPayrollWageRecoveryService service;
 @cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog(requestEnable=false,responseEnable=false)
 @GetMapping("/entities") @PreAuthorize("@ss.hasPermission('hrm:payroll:recovery:query')")
 public CommonResult<List<Map<String,Object>>> entities(){return success(service.entities(getLoginUserId()));}
 @cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog(requestEnable=false,responseEnable=false)
 @GetMapping("/list") @PreAuthorize("@ss.hasPermission('hrm:payroll:recovery:query')")
 public CommonResult<List<HrmPayrollWageRecoveryDO>> list(@RequestParam Long entityId){return success(service.list(entityId,getLoginUserId()));}
 @cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog(requestEnable=false,responseEnable=false)
 @GetMapping("/payments") @PreAuthorize("@ss.hasPermission('hrm:payroll:recovery:query')")
 public CommonResult<List<Map<String,Object>>> payments(@RequestParam Long entityId){return success(service.payments(entityId,getLoginUserId()));}
 @cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog(requestEnable=false,responseEnable=false)
 @GetMapping("/baseline") @PreAuthorize("@ss.hasPermission('hrm:payroll:recovery:query')")
 public CommonResult<Map<String,Object>> baseline(@RequestParam Long paymentId){return success(service.baseline(paymentId,getLoginUserId()));}
 @cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog(requestEnable=false,responseEnable=false)
 @GetMapping("/history") @PreAuthorize("@ss.hasPermission('hrm:payroll:recovery:query')")
 public CommonResult<List<HrmPayrollWageRecoveryEventDO>> history(@RequestParam Long id){return success(service.history(id,getLoginUserId()));}
 @cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog(requestEnable=false,responseEnable=false)
 @PostMapping("/create") @PreAuthorize("@ss.hasPermission('hrm:payroll:recovery:update')")
 public CommonResult<Long> create(@Valid @RequestBody HrmPayrollWageRecoveryReqVO req){return success(service.create(req,getLoginUserId()));}
 @cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog(requestEnable=false,responseEnable=false)
 @PostMapping("/review") @PreAuthorize("@ss.hasPermission('hrm:payroll:recovery:review')")
 public CommonResult<Boolean> review(@Valid @RequestBody HrmPayrollWageRecoveryActionReqVO req){if(req.getApproved()==null)throw cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception(cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.PAYROLL_RECOVERY_INVALID);service.review(req.getId(),req.getApproved(),req.getReason(),getLoginUserId());return success(true);}
 @cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog(requestEnable=false,responseEnable=false)
 @PostMapping("/confirm") @PreAuthorize("@ss.hasPermission('hrm:payroll:recovery:confirm')")
 public CommonResult<Boolean> confirm(@Valid @RequestBody HrmPayrollWageRecoveryActionReqVO req){service.confirm(req,getLoginUserId());return success(true);}
 @cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog(requestEnable=false,responseEnable=false)
 @PostMapping("/cancel") @PreAuthorize("@ss.hasPermission('hrm:payroll:recovery:confirm')")
 public CommonResult<Boolean> cancel(@Valid @RequestBody HrmPayrollWageRecoveryActionReqVO req){service.cancel(req.getId(),req.getReason(),req.getEvidence(),getLoginUserId());return success(true);}
}
