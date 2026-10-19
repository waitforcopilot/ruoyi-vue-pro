package cn.iocoder.yudao.module.hrm.controller.admin.payroll;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollReviewVO.*;
import cn.iocoder.yudao.module.hrm.service.payroll.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.*;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
@RestController @RequestMapping("/hrm/payroll/review")
@PreAuthorize("@ss.hasPermission('hrm:payroll:requirement:query')")
public class HrmPayrollReviewController {
 @Resource private HrmPayrollReviewService service;
 @GetMapping("/detail") public CommonResult<Object> detail(@RequestParam Long projectId,@RequestParam Long id){return success(service.detail(projectId,id,getLoginUserId()));}
 @GetMapping("/reminders") public CommonResult<Object> reminders(@RequestParam Long projectId){return success(service.reminders(projectId,getLoginUserId()));}
 @GetMapping("/statistics") public CommonResult<Object> statistics(@RequestParam Long projectId,@RequestParam(required=false) String module,@RequestParam(required=false) String status,@RequestParam(required=false) String decision,@RequestParam(required=false) String text){return success(service.statistics(projectId,getLoginUserId(),"READ",module,status,decision,text));}
 @GetMapping("/page") public CommonResult<Object> page(@RequestParam Long projectId,@RequestParam(required=false) String module,@RequestParam(required=false) String status,@RequestParam(required=false) String decision,@RequestParam(required=false) String text,@RequestParam(defaultValue="1") int pageNo,@RequestParam(defaultValue="10") int pageSize,@RequestParam(defaultValue="code") String sort){return success(service.page(projectId,getLoginUserId(),module,status,decision,text,pageNo,pageSize,sort));}
 @PostMapping("/decision") @PreAuthorize("@ss.hasPermission('hrm:payroll:requirement:query') and @ss.hasPermission('hrm:payroll:requirement:scope-decide')") public CommonResult<Boolean> decide(@Valid @RequestBody Decision c){service.decide(c,getLoginUserId());return success(true);}
 @PostMapping("/implementation") @PreAuthorize("@ss.hasPermission('hrm:payroll:requirement:update')") public CommonResult<Boolean> implementation(@Valid @RequestBody Implementation c){service.implementation(c,getLoginUserId());return success(true);}
 @PostMapping("/collaboration") @PreAuthorize("@ss.hasPermission('hrm:payroll:requirement:update')") public CommonResult<Boolean> collaboration(@Valid @RequestBody Collaboration c){service.collaborate(c,getLoginUserId());return success(true);}
 @PostMapping("/batch-assign") @PreAuthorize("@ss.hasPermission('hrm:payroll:requirement:assign')") public CommonResult<Object> batch(@Valid @RequestBody BatchAssignment c){return success(service.batch(c,getLoginUserId()));}
 @GetMapping("/export") @PreAuthorize("@ss.hasPermission('hrm:payroll:requirement:query') and @ss.hasPermission('hrm:payroll:requirement:export')") public CommonResult<Object> export(@RequestParam Long projectId,@RequestParam(required=false) String module,@RequestParam(required=false) String status,@RequestParam(required=false) String decision,@RequestParam(required=false) String text){return success(service.export(projectId,getLoginUserId(),module,status,decision,text));}
}
