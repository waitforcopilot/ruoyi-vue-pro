package cn.iocoder.yudao.module.hrm.controller.admin.payroll;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollRetroReqVO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.service.payroll.HrmPayrollRetroService;
import java.util.*;
import javax.annotation.Resource;
import javax.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/hrm/payroll/retro")
public class HrmPayrollRetroController {
 @Resource private HrmPayrollRetroService service;
 @GetMapping("/list") @PreAuthorize("@ss.hasPermission('hrm:payroll:retro:query')")
 public CommonResult<List<HrmPayrollRetroDO>> list(@RequestParam Long batchId){return success(service.list(batchId,getLoginUserId()));}
 @GetMapping("/candidates") @PreAuthorize("@ss.hasPermission('hrm:payroll:retro:query')")
 public CommonResult<List<Map<String,Object>>> candidates(@RequestParam Long batchId){return success(service.candidates(batchId,getLoginUserId()));}
 @PostMapping("/create") @PreAuthorize("@ss.hasPermission('hrm:payroll:retro:update')")
 public CommonResult<Long> create(@Valid @RequestBody HrmPayrollRetroReqVO req){return success(service.create(req,getLoginUserId()));}
 @PostMapping("/review") @PreAuthorize("@ss.hasPermission('hrm:payroll:retro:approve')")
 public CommonResult<Boolean> review(@RequestParam Long id,@RequestParam boolean approved,@RequestParam String reason){service.review(id,approved,reason,getLoginUserId());return success(true);}
 @PostMapping("/cancel") @PreAuthorize("@ss.hasPermission('hrm:payroll:retro:approve')")
 public CommonResult<Boolean> cancel(@RequestParam Long id,@RequestParam String reason){service.cancel(id,reason,getLoginUserId());return success(true);}
 @GetMapping("/history") @PreAuthorize("@ss.hasPermission('hrm:payroll:retro:query')")
 public CommonResult<List<HrmPayrollRetroEventDO>> history(@RequestParam Long id){return success(service.history(id,getLoginUserId()));}
}
