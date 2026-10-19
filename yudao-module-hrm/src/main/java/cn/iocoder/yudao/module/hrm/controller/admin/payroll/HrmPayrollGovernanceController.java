package cn.iocoder.yudao.module.hrm.controller.admin.payroll;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollGovernanceVO.*;
import cn.iocoder.yudao.module.hrm.service.payroll.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import javax.annotation.Resource;
import javax.validation.Valid;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@RestController
@RequestMapping("/hrm/payroll/governance")
@PreAuthorize("@ss.hasPermission('hrm:payroll:requirement:query')")
public class HrmPayrollGovernanceController {
    @Resource private HrmPayrollGovernanceRootService roots;
    @Resource private HrmPayrollGovernanceDecisionService decisions;
    @Resource private HrmPayrollGovernanceReleaseService releases;
    @Resource private HrmPayrollGovernanceProjectionService projection;
    @Resource private HrmPayrollGovernanceExportService exports;
    @Resource private HrmPayrollGovernanceExportWorker worker;
    @GetMapping("/anchors") public CommonResult<Object> anchors(){return success(roots.anchors(getLoginUserId()));}
    @GetMapping("/roots") public CommonResult<Object> roots(@RequestParam Long projectId){return success(roots.list(projectId,getLoginUserId()));}
    @PostMapping("/root/register") public CommonResult<Long> register(@Valid @RequestBody Root c){return success(roots.register(c,getLoginUserId()));}
    @PostMapping("/root/action") public CommonResult<Boolean> rootAction(@Valid @RequestBody Action c){roots.action(c,getLoginUserId());return success(true);}
    @GetMapping("/decisions") public CommonResult<Object> list(@RequestParam Long projectId){return success(decisions.list(projectId,getLoginUserId()));}
    @GetMapping("/decision/history") public CommonResult<Object> history(@RequestParam Long projectId,@RequestParam Long id){return success(decisions.history(projectId,id,getLoginUserId()));}
    @PostMapping("/decision/draft") public CommonResult<Long> draft(@Valid @RequestBody Decision c){return success(decisions.draft(c,getLoginUserId()));}
    @PostMapping("/decision/action") public CommonResult<Boolean> decisionAction(@Valid @RequestBody Action c){decisions.action(c,getLoginUserId());return success(true);}
    @GetMapping("/precheck") public CommonResult<Object> precheck(@RequestParam Long projectId){return success(releases.precheck(projectId,getLoginUserId()));}
    @GetMapping("/releases") public CommonResult<Object> releases(@RequestParam Long projectId){return success(releases.list(projectId,getLoginUserId()));}
    @PostMapping("/publish") public CommonResult<Long> publish(@Valid @RequestBody Publish c){return success(releases.publish(c,getLoginUserId()));}
    @GetMapping("/release") public CommonResult<Object> release(@RequestParam Long projectId,@RequestParam Long releaseId){return success(projection.project(projectId,releaseId,getLoginUserId(),"READ",null));}
    @PostMapping("/export/create") public CommonResult<Long> export(@Valid @RequestBody Job c){Long actor=getLoginUserId(),id=exports.create(c,actor);worker.submit(TenantContextHolder.getRequiredTenantId(),c.getProjectId(),id,actor);return success(id);}
    @GetMapping("/export/status") public CommonResult<Object> status(@RequestParam Long projectId,@RequestParam Long id){return success(exports.status(projectId,id,getLoginUserId()));}
    @GetMapping("/export/download") public void download(@RequestParam Long projectId,@RequestParam Long id,HttpServletResponse response) throws java.io.IOException {
        byte[] bytes=exports.claim(projectId,id,getLoginUserId());response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");response.setHeader("Content-Disposition","attachment; filename=hrm-review-job-"+id+".xlsx");response.setHeader("Cache-Control","no-store");response.setHeader("X-Content-Type-Options","nosniff");response.getOutputStream().write(bytes);
    }
}
