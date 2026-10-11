package cn.iocoder.yudao.module.hrm.controller.admin.payroll;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.hrm.service.payroll.HrmPayrollGradePolicyService;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.HrmPayrollGradePolicyDO;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;
import javax.annotation.Resource;
import javax.validation.Valid;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
@RestController @RequestMapping("/hrm/payroll/grade-policy")
public class HrmPayrollGradePolicyController {
    @Resource private HrmPayrollGradePolicyService service;
    @Resource private cn.iocoder.yudao.module.hrm.service.payroll.HrmPayrollFormulaService formulas;
    @Resource private cn.iocoder.yudao.module.hrm.service.payroll.HrmPayrollSalaryProposalService proposals;
    @GetMapping("/list") @PreAuthorize("@ss.hasPermission('hrm:payroll:grade:query')")
    public CommonResult<List<HrmPayrollGradePolicyDO>> list(){return success(service.list());}
    @GetMapping("/variables") @PreAuthorize("@ss.hasPermission('hrm:payroll:grade:query')")
    public CommonResult<List<cn.iocoder.yudao.module.hrm.dal.dataobject.salary.config.HrmSalaryOptionDO>> variables(){return success(formulas.variables());}
    @GetMapping("/employees") @PreAuthorize("@ss.hasPermission('hrm:payroll:grade:update')")
    public CommonResult<List<Map<String,Object>>> employees(){return success(proposals.employees());}
    @PostMapping("/create") @PreAuthorize("@ss.hasPermission('hrm:payroll:grade:update')")
    public CommonResult<Long> create(@Valid @RequestBody HrmPayrollGradePolicyReqVO request){return success(service.create(request));}
    @PostMapping("/approve") @PreAuthorize("@ss.hasPermission('hrm:payroll:grade:approve')")
    public CommonResult<Boolean> approve(@RequestParam Long id){service.approve(id,getLoginUserId());return success(true);}
    @PostMapping("/apply") @PreAuthorize("@ss.hasPermission('hrm:payroll:grade:update')")
    public CommonResult<Long> apply(@Valid @RequestBody HrmPayrollGradePolicyReqVO.Apply request){return success(service.apply(request));}
    @PostMapping("/validate-import") @PreAuthorize("@ss.hasPermission('hrm:payroll:grade:update')")
    public CommonResult<List<Map<String,Object>>> validateImport(@RequestParam MultipartFile file)throws java.io.IOException {return success(service.validateRows(ExcelUtils.read(file,HrmPayrollGradeImportRowVO.class,2001)));}
    @PostMapping("/import") @PreAuthorize("@ss.hasPermission('hrm:payroll:grade:update')")
    public CommonResult<Long> importRows(@RequestParam String metadata,@RequestParam MultipartFile file)throws java.io.IOException {
        return success(service.importRows(JsonUtils.parseObject(metadata,HrmPayrollGradePolicyReqVO.class),ExcelUtils.read(file,HrmPayrollGradeImportRowVO.class,2001)));
    }
    @GetMapping("/export") @PreAuthorize("@ss.hasPermission('hrm:payroll:grade:query')")
    public void export(@RequestParam Long id,HttpServletResponse response)throws java.io.IOException {ExcelUtils.write(response,"薪级方案标准.xlsx","标准",HrmPayrollGradeImportRowVO.class,service.exportRows(id));}
    @GetMapping("/import-template") @PreAuthorize("@ss.hasPermission('hrm:payroll:grade:query')")
    public void template(HttpServletResponse response)throws java.io.IOException {
        HrmPayrollGradeImportRowVO grade=new HrmPayrollGradeImportRowVO();grade.setType("GRADE");grade.setGradeCode("L1");grade.setStepCode("S1");grade.setMinimum(new java.math.BigDecimal("5000"));grade.setStandard(new java.math.BigDecimal("6000"));grade.setMaximum(new java.math.BigDecimal("7000"));
        HrmPayrollGradeImportRowVO post=new HrmPayrollGradeImportRowVO();post.setType("POST");post.setPostName("示例岗位");post.setCoefficient(java.math.BigDecimal.ONE);
        ExcelUtils.write(response,"薪级薪档与岗位系数模板.xlsx","标准",HrmPayrollGradeImportRowVO.class,Arrays.asList(grade,post));
    }
}
