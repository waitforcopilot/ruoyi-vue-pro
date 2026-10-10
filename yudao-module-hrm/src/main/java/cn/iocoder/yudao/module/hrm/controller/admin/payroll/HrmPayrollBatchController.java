package cn.iocoder.yudao.module.hrm.controller.admin.payroll;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.service.payroll.HrmPayrollBatchService;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import javax.annotation.Resource;
import javax.validation.Valid;
import javax.validation.constraints.*;
import lombok.Data;
import java.util.List;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
@RestController @RequestMapping("/hrm/payroll/batch")
public class HrmPayrollBatchController {
    @Resource private HrmPayrollBatchService service;
    @GetMapping("/page") @PreAuthorize("@ss.hasPermission('hrm:payroll:batch:query')")
    public CommonResult<cn.iocoder.yudao.framework.common.pojo.PageResult<cn.iocoder.yudao.module.hrm.dal.dataobject.salary.monthrecord.HrmSalaryMonthRecordDO>> page(
            @Valid cn.iocoder.yudao.module.hrm.controller.admin.salary.vo.monthrecord.HrmSalaryMonthRecordPageReqVO request) {
        return success(service.page(request));
    }
    @GetMapping("/versions") @PreAuthorize("@ss.hasPermission('hrm:payroll:batch:query')")
    public CommonResult<List<HrmPayrollRunDO>> versions(@RequestParam Long batchId){return success(service.runs(batchId));}
    @GetMapping("/events") @PreAuthorize("@ss.hasPermission('hrm:payroll:batch:query')")
    public CommonResult<List<HrmPayrollBatchEventDO>> events(@RequestParam Long batchId){return success(service.events(batchId));}
    @PostMapping("/transition/{action}") @PreAuthorize("@ss.hasPermission('hrm:payroll:batch:' + #action)")
    public CommonResult<Boolean> transition(@PathVariable String action,@Valid @RequestBody Transition request){
        if (!java.util.Arrays.asList("submit","review","approve","reject","freeze","unfreeze","archive").contains(action)) {
            throw cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception(
                    cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.SALARY_MONTH_RECORD_STATUS_INVALID);
        }
        service.transition(request.getBatchId(),request.getRunId(),action,request.getReason(),getLoginUserId());return success(true);
    }
    @Data public static class Transition {
        @NotNull private Long batchId;
        @NotNull private Long runId;
        @NotBlank @Size(max=1000) private String reason;
    }
}
