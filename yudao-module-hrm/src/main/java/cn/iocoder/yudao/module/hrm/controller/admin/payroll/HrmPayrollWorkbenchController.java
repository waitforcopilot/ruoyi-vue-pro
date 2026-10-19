package cn.iocoder.yudao.module.hrm.controller.admin.payroll;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.hrm.service.payroll.HrmPayrollWorkbenchService;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollWorkbenchVO.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.*;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
@RestController @RequestMapping("/hrm/payroll/workbench")
@PreAuthorize("@ss.hasPermission('hrm:payroll:requirement:query')")
public class HrmPayrollWorkbenchController {
 @Resource private HrmPayrollWorkbenchService service;
 @GetMapping("/list") public CommonResult<List<Object>> list(@RequestParam Long projectId,@RequestParam String kind){return success(service.list(projectId,kind,getLoginUserId()));}
 @GetMapping("/detail") public CommonResult<Object> detail(@RequestParam Long projectId,@RequestParam String kind,@RequestParam Long id){return success(service.detail(projectId,kind,id,getLoginUserId()));}
 @GetMapping("/history") public CommonResult<List<Object>> history(@RequestParam Long projectId,@RequestParam String kind,@RequestParam Long id){return success(service.history(projectId,kind,id,getLoginUserId()));}
 @GetMapping("/export") @PreAuthorize("@ss.hasPermission('hrm:payroll:requirement:query') and @ss.hasPermission('hrm:payroll:requirement:export')") public CommonResult<Map<String,Object>> export(@RequestParam Long projectId){return success(service.export(projectId,getLoginUserId()));}
 @GetMapping("/readiness") public CommonResult<Map<String,Object>> readiness(@RequestParam Long projectId){return success(service.readinessSnapshot(projectId,getLoginUserId()));}
 @GetMapping("/blockers") public CommonResult<Map<String,Object>> blockers(@RequestParam Long projectId){return success(service.blockers(projectId,getLoginUserId()));}
 @PostMapping("/source/save") public CommonResult<Long> source(@Valid @RequestBody SourceSave c){return success(service.saveSource(c,getLoginUserId()));}
 @PostMapping("/source/copy") public CommonResult<Long> copy(@Valid @RequestBody Copy c){return success(service.copy(c,getLoginUserId()));}
 @PostMapping("/source/supply") public CommonResult<Boolean> supply(@Valid @RequestBody Supply c){service.supply(c,getLoginUserId());return success(true);}
 @PostMapping("/source/propose") public CommonResult<Long> propose(@Valid @RequestBody Proposal c){return success(service.propose(c,getLoginUserId()));}
 @PostMapping("/relation/save") public CommonResult<Long> relation(@Valid @RequestBody RelationSave c){return success(service.saveRelation(c,getLoginUserId()));}
 @PostMapping("/relation/unlink") public CommonResult<Boolean> unlink(@Valid @RequestBody Context c){service.unlink(c,getLoginUserId());return success(true);}
 @PostMapping("/verify") public CommonResult<Long> verify(@Valid @RequestBody Verify c){return success(service.verify(c,getLoginUserId()));}
 @PostMapping("/issue/create") public CommonResult<Long> issue(@Valid @RequestBody IssueSave c){return success(service.createIssue(c,getLoginUserId()));}
 @PostMapping("/issue/action") public CommonResult<Boolean> issueAction(@Valid @RequestBody IssueAction c){service.issueAction(c,getLoginUserId());return success(true);}
}
