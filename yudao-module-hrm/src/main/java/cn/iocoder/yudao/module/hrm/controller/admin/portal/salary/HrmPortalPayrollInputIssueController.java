package cn.iocoder.yudao.module.hrm.controller.admin.portal.salary;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.service.payroll.HrmPayrollInputIssueService;
import java.util.*;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollInputIssueActionReqVO;
import javax.validation.Valid;
import javax.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/hrm/portal/payroll-input-issue")
public class HrmPortalPayrollInputIssueController {
 @Resource private HrmPayrollInputIssueService service;
 @cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog(requestEnable=false,responseEnable=false)
 @GetMapping("/list") @PreAuthorize("@ss.hasPermission('hrm:portal:query')")
 public CommonResult<List<HrmPayrollInputIssueDO>> list(){return success(service.mine(getLoginUserId()));}
 @cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog(requestEnable=false,responseEnable=false)
 @GetMapping("/history") @PreAuthorize("@ss.hasPermission('hrm:portal:query')")
 public CommonResult<List<HrmPayrollInputIssueEventDO>> history(@RequestParam Long id){return success(service.history(id,getLoginUserId(),true));}
 @cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog(requestEnable=false,responseEnable=false)
 @PostMapping("/submit") @PreAuthorize("@ss.hasPermission('hrm:portal:query')")
 public CommonResult<Boolean> submit(@Valid @RequestBody HrmPayrollInputIssueActionReqVO req){service.submit(req.getId(),req.getEvidence(),getLoginUserId());return success(true);}
}
