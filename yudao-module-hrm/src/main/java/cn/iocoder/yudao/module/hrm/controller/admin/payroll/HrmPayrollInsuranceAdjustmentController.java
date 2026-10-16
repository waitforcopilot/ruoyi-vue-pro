package cn.iocoder.yudao.module.hrm.controller.admin.payroll;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.service.payroll.HrmPayrollInsuranceAdjustmentService;
import java.util.*;
import javax.annotation.Resource;
import javax.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/hrm/payroll/insurance-adjustment")
public class HrmPayrollInsuranceAdjustmentController {
 @Resource private HrmPayrollInsuranceAdjustmentService service;
 @cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog(requestEnable=false,responseEnable=false)
 @GetMapping("/list") @PreAuthorize("@ss.hasPermission('hrm:payroll:insurance-adjustment:query')")
 public CommonResult<List<HrmPayrollInsuranceAdjustmentDO>> list(@RequestParam Long entityId){return success(service.list(entityId,getLoginUserId()));}
 @cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog(requestEnable=false,responseEnable=false)
 @GetMapping("/history") @PreAuthorize("@ss.hasPermission('hrm:payroll:insurance-adjustment:query')")
 public CommonResult<List<HrmPayrollInsuranceAdjustmentEventDO>> history(@RequestParam Long id){return success(service.history(id,getLoginUserId()));}
 @cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog(requestEnable=false,responseEnable=false)
 @PostMapping("/create") @PreAuthorize("@ss.hasPermission('hrm:payroll:insurance-adjustment:update')")
 public CommonResult<Long> create(@Valid @RequestBody HrmPayrollInsuranceAdjustmentReqVO req){return success(service.create(req,getLoginUserId()));}
 @cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog(requestEnable=false,responseEnable=false)
 @PostMapping("/review") @PreAuthorize("@ss.hasPermission('hrm:payroll:insurance-adjustment:review')")
 public CommonResult<Boolean> review(@Valid @RequestBody HrmPayrollInsuranceAdjustmentActionReqVO req){if(req.getApproved()==null)throw cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception(cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.PAYROLL_INSURANCE_ADJUSTMENT_INVALID);service.review(req.getId(),req.getApproved(),req.getReason(),getLoginUserId());return success(true);}
 @cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog(requestEnable=false,responseEnable=false)
 @PostMapping("/confirm") @PreAuthorize("@ss.hasPermission('hrm:payroll:insurance-adjustment:confirm')")
 public CommonResult<Boolean> confirm(@Valid @RequestBody HrmPayrollInsuranceAdjustmentActionReqVO req){service.confirm(req,getLoginUserId());return success(true);}
 @cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog(requestEnable=false,responseEnable=false)
 @PostMapping("/cancel") @PreAuthorize("@ss.hasPermission('hrm:payroll:insurance-adjustment:confirm')")
 public CommonResult<Boolean> cancel(@Valid @RequestBody HrmPayrollInsuranceAdjustmentActionReqVO req){service.cancel(req.getId(),req.getReason(),req.getEvidence(),getLoginUserId());return success(true);}
 @cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog(requestEnable=false,responseEnable=false)
 @GetMapping("/entities") @PreAuthorize("@ss.hasPermission('hrm:payroll:insurance-adjustment:query')")
 public CommonResult<List<Map<String,Object>>> entities(){return success(service.entities(getLoginUserId()));}
 @cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog(requestEnable=false,responseEnable=false)
 @GetMapping("/options") @PreAuthorize("@ss.hasPermission('hrm:payroll:insurance-adjustment:query')")
 public CommonResult<Map<String,Object>> options(@RequestParam Long entityId){return success(service.options(entityId,getLoginUserId()));}
 @cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog(requestEnable=false,responseEnable=false)
 @PostMapping("/preview") @PreAuthorize("@ss.hasPermission('hrm:payroll:insurance-adjustment:query')")
 public CommonResult<HrmPayrollInsuranceAdjustmentService.Proof> preview(@Valid @RequestBody HrmPayrollInsuranceAdjustmentReqVO req){return success(service.preview(req,getLoginUserId()));}
}
