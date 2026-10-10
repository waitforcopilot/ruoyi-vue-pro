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
    @GetMapping("/list") @PreAuthorize("@ss.hasPermission('hrm:payroll:requirement:query')")
    public CommonResult<List<HrmPayrollRequirementDO>> list(@RequestParam(required=false) String module){return success(service.list(module));}
    @PostMapping("/save") @PreAuthorize("@ss.hasPermission('hrm:payroll:requirement:update')")
    public CommonResult<Long> save(@Valid @RequestBody HrmPayrollRequirementSaveReqVO request){return success(service.save(request,getLoginUserId()));}
    @PostMapping("/initialize") @PreAuthorize("@ss.hasPermission('hrm:payroll:requirement:update')")
    public CommonResult<Integer> initialize(){return success(service.initialize(getLoginUserId()));}
    @GetMapping("/history") @PreAuthorize("@ss.hasPermission('hrm:payroll:requirement:query')")
    public CommonResult<List<HrmPayrollRequirementHistoryDO>> history(@RequestParam Long id){return success(service.history(id));}
    @GetMapping("/export") @PreAuthorize("@ss.hasPermission('hrm:payroll:requirement:export')")
    public void export(@RequestParam(required=false) String module,HttpServletResponse response) throws java.io.IOException {
        ExcelUtils.write(response,"薪酬需求评审.xlsx","评审清单",HrmPayrollRequirementDO.class,service.list(module));
    }
}
