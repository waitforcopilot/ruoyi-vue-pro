package cn.iocoder.yudao.module.hrm.controller.admin.payroll;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.service.payroll.HrmPayrollOffsetService;
import java.util.*;
import javax.annotation.Resource;
import javax.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/hrm/payroll/offset")
public class HrmPayrollOffsetController {
 @Resource private HrmPayrollOffsetService service;
 @cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog(requestEnable=false,responseEnable=false)
 @GetMapping("/options") @PreAuthorize("@ss.hasPermission('hrm:payroll:offset:query')")
 public CommonResult<Map<String,Object>> options(@RequestParam Long batchId){return success(service.options(batchId,getLoginUserId()));}
 @cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog(requestEnable=false,responseEnable=false)
 @GetMapping("/list") @PreAuthorize("@ss.hasPermission('hrm:payroll:offset:query')")
 public CommonResult<List<HrmPayrollOffsetDO>> list(@RequestParam Long batchId){return success(service.list(batchId,getLoginUserId()));}
 @cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog(requestEnable=false,responseEnable=false)
 @GetMapping("/history") @PreAuthorize("@ss.hasPermission('hrm:payroll:offset:query')")
 public CommonResult<List<HrmPayrollOffsetEventDO>> history(@RequestParam Long id){return success(service.history(id,getLoginUserId()));}
 @cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog(requestEnable=false,responseEnable=false)
 @PostMapping("/create") @PreAuthorize("@ss.hasPermission('hrm:payroll:offset:update')")
 public CommonResult<Long> create(@Valid @RequestBody HrmPayrollOffsetReqVO req){return success(service.create(req,getLoginUserId()));}
 @cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog(requestEnable=false,responseEnable=false)
 @PostMapping("/review") @PreAuthorize("@ss.hasPermission('hrm:payroll:offset:review')")
 public CommonResult<Boolean> review(@Valid @RequestBody HrmPayrollOffsetActionReqVO req){if(req.getApproved()==null)throw cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception(cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.PAYROLL_OFFSET_INVALID);service.review(req.getId(),req.getApproved(),req.getReason(),getLoginUserId());return success(true);}
 @cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog(requestEnable=false,responseEnable=false)
 @PostMapping("/cancel") @PreAuthorize("@ss.hasPermission('hrm:payroll:offset:update')")
 public CommonResult<Boolean> cancel(@Valid @RequestBody HrmPayrollOffsetActionReqVO req){service.cancel(req.getId(),req.getReason(),getLoginUserId());return success(true);}
}
