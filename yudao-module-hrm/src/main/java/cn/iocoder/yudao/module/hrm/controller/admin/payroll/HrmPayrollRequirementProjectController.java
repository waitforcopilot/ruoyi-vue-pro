package cn.iocoder.yudao.module.hrm.controller.admin.payroll;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.hrm.service.payroll.HrmPayrollRequirementProjectService;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollRequirementProjectReqVO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import javax.annotation.Resource;
import javax.validation.Valid;
import javax.validation.constraints.*;
import lombok.Data;
import java.util.*;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
@RestController @RequestMapping("/hrm/payroll/requirement-project")
@PreAuthorize("@ss.hasPermission('hrm:payroll:requirement:query')")
public class HrmPayrollRequirementProjectController {
 @Resource private HrmPayrollRequirementProjectService service;
 @GetMapping("/list") public CommonResult<List<HrmPayrollRequirementProjectDO>> list(){return success(service.list(getLoginUserId()));}
 @PostMapping("/create") @PreAuthorize("@ss.hasPermission('hrm:payroll:requirement:query') and @ss.hasPermission('hrm:payroll:requirement:update')")
 public CommonResult<Long> create(@Valid @RequestBody HrmPayrollRequirementProjectReqVO r){return success(service.create(r,getLoginUserId()));}
 @PostMapping("/metadata")
 public CommonResult<Boolean> metadata(@Valid @RequestBody HrmPayrollRequirementProjectReqVO r){service.metadata(r,getLoginUserId());return success(true);}
 @Data public static class ScopeRequest { @NotNull @Positive private Long projectId; @Min(1) private Integer expectedVersion; @NotBlank @Size(max=12000) private String scopeJson; @NotBlank @Size(max=2000) private String reason; }
 @PostMapping("/scope")
 public CommonResult<Boolean> scope(@Valid @RequestBody ScopeRequest r){service.scope(r.projectId,r.expectedVersion,r.scopeJson,r.reason,getLoginUserId());return success(true);}
 @GetMapping("/events") public CommonResult<List<HrmPayrollRequirementProjectEventDO>> events(@RequestParam Long projectId){return success(service.events(projectId,getLoginUserId()));}
 @GetMapping("/template-differences") public CommonResult<List<Map<String,Object>>> differences(@RequestParam Long projectId){return success(service.differences(projectId,getLoginUserId()));}
 @Data public static class DecisionRequest { @NotNull @Positive private Long projectId; @NotNull @Positive private Long requirementId; @NotNull @Min(1) private Integer version; @NotNull @Min(1) private Integer templateVersion; @NotNull private Boolean accept; }
 @PostMapping("/template-decision") @PreAuthorize("@ss.hasPermission('hrm:payroll:requirement:query') and @ss.hasPermission('hrm:payroll:requirement:update')")
 public CommonResult<Boolean> decision(@Valid @RequestBody DecisionRequest r){service.adopt(r.projectId,r.requirementId,r.version,r.templateVersion,r.accept,getLoginUserId());return success(true);}

 @GetMapping("/eligible-reviewers") public CommonResult<List<Long>> reviewers(@RequestParam Long projectId,@RequestParam Long requirementId){return success(service.reviewers(projectId,requirementId,getLoginUserId()));}
 @GetMapping("/capabilities") public CommonResult<Map<String,Boolean>> capabilities(@RequestParam Long projectId){return success(service.capabilities(projectId,getLoginUserId()));}
 @GetMapping("/members") public CommonResult<List<Map<String,Object>>> members(@RequestParam Long projectId){return success(service.members(projectId,getLoginUserId()));}
 @PostMapping("/member-grant") public CommonResult<Long> grant(@Valid @RequestBody cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollRequirementMemberReqVO request){
  HrmPayrollRequirementProjectGrantDO g=cn.iocoder.yudao.framework.common.util.object.BeanUtils.toBean(request,HrmPayrollRequirementProjectGrantDO.class);
  return success(service.grant(g,getLoginUserId()));
 }
 @Data public static class RevokeRequest { @NotNull @Positive private Long projectId; @NotNull @Positive private Long grantId; @NotNull @Min(1) private Integer version; @NotBlank @Size(max=2000) private String reason; }
 @PostMapping("/member-revoke") public CommonResult<Boolean> revoke(@Valid @RequestBody RevokeRequest r){service.revoke(r.projectId,r.grantId,r.version,r.reason,getLoginUserId());return success(true);}
}
