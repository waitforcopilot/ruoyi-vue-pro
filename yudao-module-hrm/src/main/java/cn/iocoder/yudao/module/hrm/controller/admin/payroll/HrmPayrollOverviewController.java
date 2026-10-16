package cn.iocoder.yudao.module.hrm.controller.admin.payroll;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.hrm.service.payroll.HrmPayrollOverviewService;
import java.util.*;
import javax.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/hrm/payroll/overview")
public class HrmPayrollOverviewController {
 @Resource private HrmPayrollOverviewService service;
 @cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog(requestEnable=false,responseEnable=false)
 @GetMapping("/entities") @PreAuthorize("@ss.hasPermission('hrm:payroll:overview:query')")
 public CommonResult<List<Map<String,Object>>> entities(){return success(service.entities(getLoginUserId()));}
 @cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog(requestEnable=false,responseEnable=false)
 @GetMapping("/history") @PreAuthorize("@ss.hasPermission('hrm:payroll:overview:query')")
 public CommonResult<List<HrmPayrollOverviewService.Summary>> history(@RequestParam Long entityId,@RequestParam Integer year,@RequestParam(required=false) Integer month,@RequestParam(required=false) Integer status,@RequestParam(required=false) String type){return success(service.history(entityId,year,month,status,type,getLoginUserId()));}
 @cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog(requestEnable=false,responseEnable=false)
 @GetMapping("/detail") @PreAuthorize("@ss.hasPermission('hrm:payroll:overview:query')")
 public CommonResult<HrmPayrollOverviewService.Detail> detail(@RequestParam Long batchId,@RequestParam(required=false) Set<Long> employeeIds){return success(service.detail(batchId,employeeIds,getLoginUserId()));}
}
