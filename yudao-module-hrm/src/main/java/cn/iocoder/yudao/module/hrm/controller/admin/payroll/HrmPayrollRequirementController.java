package cn.iocoder.yudao.module.hrm.controller.admin.payroll;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollRequirementSaveReqVO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.service.payroll.HrmPayrollRequirementService;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.annotation.Resource;
import javax.validation.Valid;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
@RestController
@RequestMapping("/hrm/payroll/requirement")
@Tag(name="管理后台 - 薪酬需求征集")
public class HrmPayrollRequirementController {
    @Resource private HrmPayrollRequirementService service;
    @Resource private cn.iocoder.yudao.module.hrm.service.payroll.HrmPayrollRequirementProjectService projects;
    @Resource private cn.iocoder.yudao.module.hrm.dal.mysql.payroll.HrmPayrollRequirementMapper requirements;
    @GetMapping("/legacy-project") @PreAuthorize("@ss.hasPermission('hrm:payroll:requirement:query')")
    public CommonResult<HrmPayrollRequirementProjectDO> legacyProject(){HrmPayrollRequirementProjectDO p=service.legacyProject();if(p!=null){projects.check(p.getId(),getLoginUserId(),"READ",false);if(!projects.full(p.getId(),getLoginUserId(),"READ"))p.setScopeJson(null);}return success(p);}
    @GetMapping("/list") @PreAuthorize("@ss.hasPermission('hrm:payroll:requirement:query')")
    public CommonResult<List<HrmPayrollRequirementDO>> list(@RequestParam(required=false) String module,@RequestParam(required=false) Long projectId){
        if(projectId==null)return success(service.list(module));
        return success(projects.rows(projectId,module,getLoginUserId(),"READ"));
    }
    @PostMapping("/save") @PreAuthorize("@ss.hasPermission('hrm:payroll:requirement:update')")
    public CommonResult<Long> save(@Valid @RequestBody HrmPayrollRequirementSaveReqVO request){return success(service.save(request,getLoginUserId()));}
    @PostMapping("/confirm") @PreAuthorize("@ss.hasPermission('hrm:payroll:requirement:query') and @ss.hasPermission('hrm:payroll:requirement:confirm')")
    public CommonResult<Boolean> confirm(@Valid @RequestBody cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollRequirementConfirmReqVO request){
        if(request.getProjectId()!=null)projects.target(request.getProjectId(),request.getId(),getLoginUserId(),"CONFIRM",request.getProjectScopeVersion(),false);
        service.confirm(request.getId(),request.getVersion(),getLoginUserId());return success(true);
    }
    @PostMapping("/assign") @PreAuthorize("@ss.hasPermission('hrm:payroll:requirement:query') and @ss.hasPermission('hrm:payroll:requirement:assign')")
    public CommonResult<Boolean> assign(@Valid @RequestBody cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollRequirementAssignReqVO request){
        if(request.getProjectId()!=null)projects.target(request.getProjectId(),request.getId(),getLoginUserId(),"ASSIGN",request.getProjectScopeVersion(),false);
        service.assign(request.getId(),request.getVersion(),request.getReviewerId(),getLoginUserId());return success(true);
    }
    @PostMapping("/initialize") @PreAuthorize("@ss.hasPermission('hrm:payroll:requirement:update')")
    public CommonResult<Integer> initialize(@RequestParam(required=false) Long projectId){return success(projectId==null?service.initialize(getLoginUserId()):projects.initialize(projectId,getLoginUserId()));}
    @GetMapping("/history") @PreAuthorize("@ss.hasPermission('hrm:payroll:requirement:query')")
    public CommonResult<List<HrmPayrollRequirementHistoryDO>> history(@RequestParam Long id,@RequestParam(required=false) Long projectId){
        if(projectId!=null){projects.check(projectId,getLoginUserId(),"READ",false);HrmPayrollRequirementDO r=requirements.selectById(id);if(r==null || !projectId.equals(r.getProjectId()))throw cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception(cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.PAYROLL_PROJECT_DENIED);}
        return success(service.history(id));}
    @GetMapping("/export") @PreAuthorize("@ss.hasPermission('hrm:payroll:requirement:query') and @ss.hasPermission('hrm:payroll:requirement:export')")
    public void export(@RequestParam(required=false) String module,@RequestParam(required=false) Long projectId,HttpServletResponse response) throws java.io.IOException {
        Long target=projectId==null?(service.legacyProject()==null?null:service.legacyProject().getId()):projectId;
        List<HrmPayrollRequirementDO> rows=target==null?java.util.Collections.emptyList():projects.rows(target,module,getLoginUserId(),"EXPORT");
        boolean restricted=target!=null && (!projects.full(target,getLoginUserId(),"READ") || !projects.full(target,getLoginUserId(),"EXPORT"));
        ExcelUtils.write(response,restricted?"薪酬需求评审-权限受限节选.xlsx":"薪酬需求评审.xlsx",restricted?"权限受限节选":"评审清单",HrmPayrollRequirementDO.class,rows);
    }
}
