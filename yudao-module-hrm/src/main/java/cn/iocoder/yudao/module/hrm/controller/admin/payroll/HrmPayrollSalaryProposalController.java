package cn.iocoder.yudao.module.hrm.controller.admin.payroll;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.hrm.service.payroll.HrmPayrollSalaryProposalService;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.HrmPayrollSalaryProposalDO;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import javax.annotation.Resource;
import javax.validation.Valid;
import javax.validation.constraints.*;
import lombok.Data;
import java.util.List;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
@RestController @RequestMapping("/hrm/payroll/salary-proposal")
public class HrmPayrollSalaryProposalController {
    @Resource private HrmPayrollSalaryProposalService service;
    @GetMapping("/list") @PreAuthorize("@ss.hasPermission('hrm:payroll:salary-proposal:query')")
    public CommonResult<List<HrmPayrollSalaryProposalDO>> list(){return success(service.list());}
    @GetMapping("/employees") @PreAuthorize("@ss.hasPermission('hrm:payroll:salary-proposal:query')")
    public CommonResult<List<java.util.Map<String,Object>>> employees(){return success(service.employees());}
    @PostMapping("/review") @PreAuthorize("@ss.hasPermission('hrm:payroll:salary-proposal:review')")
    public CommonResult<Boolean> review(@Valid @RequestBody Decision request){service.transition(request.getId(),"review",request.getReason(),getLoginUserId());return success(true);}
    @PostMapping("/approve") @PreAuthorize("@ss.hasPermission('hrm:payroll:salary-proposal:approve')")
    public CommonResult<Boolean> approve(@Valid @RequestBody Decision request){service.transition(request.getId(),"approve",request.getReason(),getLoginUserId());return success(true);}
    @PostMapping("/reject") @PreAuthorize("@ss.hasPermission('hrm:payroll:salary-proposal:reject')")
    public CommonResult<Boolean> reject(@Valid @RequestBody Decision request){service.transition(request.getId(),"reject",request.getReason(),getLoginUserId());return success(true);}
    @Data public static class Decision {@NotNull private Long id;@NotBlank @Size(max=1000) private String reason;}
}
