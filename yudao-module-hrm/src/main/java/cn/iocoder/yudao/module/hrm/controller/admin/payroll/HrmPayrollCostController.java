package cn.iocoder.yudao.module.hrm.controller.admin.payroll;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.service.payroll.HrmPayrollCostService;
import java.util.*;
import javax.annotation.Resource;
import javax.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/hrm/payroll/cost")
public class HrmPayrollCostController {
 @Resource private HrmPayrollCostService service;
 @GetMapping("/options") @PreAuthorize("@ss.hasPermission('hrm:payroll:cost:query')")
 public CommonResult<Map<String,Object>> options(@RequestParam(required=false) Long entityId){return success(service.options(entityId,getLoginUserId()));}
 @GetMapping("/rules") @PreAuthorize("@ss.hasPermission('hrm:payroll:cost:query')")
 public CommonResult<List<HrmPayrollCostRuleDO>> rules(@RequestParam Long entityId,@RequestParam(required=false) Long employeeId){return success(service.rules(entityId,employeeId,getLoginUserId()));}
 @PostMapping("/rule") @PreAuthorize("@ss.hasPermission('hrm:payroll:cost:update')")
 public CommonResult<Long> createRule(@Valid @RequestBody HrmPayrollCostRuleReqVO req){return success(service.createRule(req,getLoginUserId()));}
 @PostMapping("/review-rule") @PreAuthorize("@ss.hasPermission('hrm:payroll:cost:approve')")
 public CommonResult<Boolean> reviewRule(@RequestParam Long id,@RequestParam boolean approved,@RequestParam String reason){service.reviewRule(id,approved,reason,getLoginUserId());return success(true);}
 @GetMapping("/reports") @PreAuthorize("@ss.hasPermission('hrm:payroll:cost:query')")
 public CommonResult<List<HrmPayrollCostReportDO>> reports(@RequestParam Long batchId){return success(service.reports(batchId,getLoginUserId()));}
 @PostMapping("/build") @PreAuthorize("@ss.hasPermission('hrm:payroll:cost:update')")
 public CommonResult<Long> build(@RequestParam Long batchId,@RequestParam String reason){return success(service.build(batchId,reason,getLoginUserId()));}
 @PostMapping("/review-report") @PreAuthorize("@ss.hasPermission('hrm:payroll:cost:approve')")
 public CommonResult<Boolean> reviewReport(@RequestParam Long id,@RequestParam boolean approved,@RequestParam String reason){service.reviewReport(id,approved,reason,getLoginUserId());return success(true);}
 @GetMapping("/rows") @PreAuthorize("@ss.hasPermission('hrm:payroll:cost:query')")
 public CommonResult<List<HrmPayrollCostRowVO>> rows(@RequestParam Long id){return success(service.rows(id,false,getLoginUserId()));}
 @GetMapping("/export") @PreAuthorize("@ss.hasPermission('hrm:payroll:cost:export')")
 @cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog(requestEnable=false,responseEnable=false)
 public void export(@RequestParam Long id,javax.servlet.http.HttpServletResponse response)throws java.io.IOException{ExcelUtils.write(response,"薪酬成本分摊.xlsx","分摊明细",HrmPayrollCostRowVO.class,service.rows(id,true,getLoginUserId()));}
 @GetMapping("/history") @PreAuthorize("@ss.hasPermission('hrm:payroll:cost:query')")
 public CommonResult<List<HrmPayrollCostEventDO>> history(@RequestParam String kind,@RequestParam Long id){return success(service.history(kind,id,getLoginUserId()));}
}
