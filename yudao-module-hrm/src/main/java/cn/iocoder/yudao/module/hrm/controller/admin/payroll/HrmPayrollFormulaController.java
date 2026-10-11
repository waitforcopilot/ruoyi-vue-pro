package cn.iocoder.yudao.module.hrm.controller.admin.payroll;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.hrm.service.payroll.HrmPayrollFormulaService;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.HrmPayrollFormulaDO;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import javax.annotation.Resource;
import javax.validation.Valid;
import javax.validation.constraints.*;
import lombok.Data;
import java.util.List;
import java.time.LocalDate;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
@RestController @RequestMapping("/hrm/payroll/formula")
public class HrmPayrollFormulaController {
    @Resource private HrmPayrollFormulaService service;
    @GetMapping("/list") @PreAuthorize("@ss.hasPermission('hrm:payroll:formula:query')")
    public CommonResult<List<HrmPayrollFormulaDO>> list(){return success(service.list());}
    @PostMapping("/create") @PreAuthorize("@ss.hasPermission('hrm:payroll:formula:update')")
    public CommonResult<Long> create(@Valid @RequestBody Create request){return success(service.create(request.getOptionCode(),request.getExpression(),request.getEffectiveFrom(),request.getEffectiveTo(),request.getEvidence(),request.getSalaryGroupId()));}
    @PostMapping("/approve") @PreAuthorize("@ss.hasPermission('hrm:payroll:formula:approve')")
    public CommonResult<Boolean> approve(@RequestParam Long id){service.approve(id,getLoginUserId());return success(true);}
    @GetMapping("/groups") @PreAuthorize("@ss.hasPermission('hrm:payroll:formula:query')")
    public CommonResult<List<java.util.Map<String,Object>>> groups(){return success(service.groups().stream().map(group->{java.util.Map<String,Object> value=new java.util.LinkedHashMap<>();value.put("id",group.getId());value.put("name",group.getName());return value;}).collect(java.util.stream.Collectors.toList()));}
    @GetMapping("/variables") @PreAuthorize("@ss.hasPermission('hrm:payroll:formula:query')")
    public CommonResult<List<cn.iocoder.yudao.module.hrm.dal.dataobject.salary.config.HrmSalaryOptionDO>> variables(){return success(service.variables());}
    @PostMapping("/preview") @PreAuthorize("@ss.hasPermission('hrm:payroll:formula:query')")
    public CommonResult<java.util.Map<String,java.math.BigDecimal>> preview(@Valid @RequestBody Preview request){return success(service.preview(request.getId(),request.getDate(),request.getSalaryGroupId(),request.getInputs()));}
    @Data public static class Preview {
        @NotNull private Long id;
        @NotNull private LocalDate date;
        private Long salaryGroupId;
        @NotNull @Size(max=200) private java.util.Map<String,java.math.BigDecimal> inputs;
    }
    @Data public static class Create {
        private Long salaryGroupId;
        @NotNull private Integer optionCode;
        @NotBlank @Size(max=1000) private String expression;
        @NotNull private LocalDate effectiveFrom;
        @NotNull private LocalDate effectiveTo;
        @NotBlank @Size(max=1000) private String evidence;
    }
}
