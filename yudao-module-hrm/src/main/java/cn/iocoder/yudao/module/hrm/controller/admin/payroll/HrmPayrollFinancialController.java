package cn.iocoder.yudao.module.hrm.controller.admin.payroll;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.service.payroll.*;
import java.util.*;
import javax.annotation.Resource;
import javax.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
@RestController @RequestMapping("/hrm/payroll/financial")
public class HrmPayrollFinancialController {
 @Resource private HrmPayrollFinancialService service;
 @Resource private HrmPayrollCostService costs;
 @Resource private HrmPayrollWageStructureService structure;
 @cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog(requestEnable=false,responseEnable=false)
 @GetMapping("/wage-structure") @PreAuthorize("@ss.hasPermission('hrm:payroll:analysis:query')")
 public CommonResult<HrmPayrollWageStructureService.Result> structure(@RequestParam Long entityId,@RequestParam Integer year,@RequestParam Integer month,@RequestParam(required=false) Long deptId,@RequestParam(required=false) String employeeCategory){return success(structure.analyze(entityId,year,month,deptId,employeeCategory,getLoginUserId()));}
 @cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog(requestEnable=false,responseEnable=false)
 @GetMapping("/wage-structure-export") @PreAuthorize("@ss.hasPermission('hrm:payroll:analysis:export')")
 public void structureExport(@RequestParam Long entityId,@RequestParam Integer year,@RequestParam Integer month,@RequestParam(required=false) Long deptId,@RequestParam(required=false) String employeeCategory,javax.servlet.http.HttpServletResponse response)throws java.io.IOException{ExcelUtils.write(response,"冻结工资结构及环比.xlsx","工资结构",HrmPayrollWageStructureRowVO.class,structure.export(entityId,year,month,deptId,employeeCategory,getLoginUserId()));}
 @cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog(requestEnable=false,responseEnable=false)
 @GetMapping("/options") @PreAuthorize("@ss.hasPermission('hrm:payroll:analysis:query')")
 public CommonResult<Map<String,Object>> options(@RequestParam(required=false) Long entityId){return success(costs.options(entityId,getLoginUserId()));}
 @cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog(requestEnable=false,responseEnable=false)
 @GetMapping("/plans") @PreAuthorize("@ss.hasPermission('hrm:payroll:analysis:query')")
 public CommonResult<List<HrmPayrollFinancialPlanDO>> plans(@RequestParam Long entityId,@RequestParam Integer year,@RequestParam Integer month){return success(service.list(entityId,year,month,getLoginUserId()));}
 @cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog(requestEnable=false,responseEnable=false)
 @PostMapping("/create") @PreAuthorize("@ss.hasPermission('hrm:payroll:analysis:update')")
 public CommonResult<Long> create(@Valid @RequestBody HrmPayrollFinancialPlanReqVO req){return success(service.create(req,getLoginUserId()));}
 @cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog(requestEnable=false,responseEnable=false)
 @PostMapping("/review") @PreAuthorize("@ss.hasPermission('hrm:payroll:analysis:approve')")
 public CommonResult<Boolean> review(@RequestParam Long id,@RequestParam boolean approved,@RequestParam String reason){service.review(id,approved,reason,getLoginUserId());return success(true);}
 @cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog(requestEnable=false,responseEnable=false)
 @GetMapping("/history") @PreAuthorize("@ss.hasPermission('hrm:payroll:analysis:query')")
 public CommonResult<List<HrmPayrollFinancialEventDO>> history(@RequestParam Long entityId,@RequestParam Integer year,@RequestParam Integer month){return success(service.history(entityId,year,month,getLoginUserId()));}
 @cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog(requestEnable=false,responseEnable=false)
 @GetMapping("/analysis") @PreAuthorize("@ss.hasPermission('hrm:payroll:analysis:query')")
 public CommonResult<HrmPayrollFinancialService.Analysis> analysis(@RequestParam Long entityId,@RequestParam Integer year,@RequestParam Integer month){return success(service.analyze(entityId,year,month,getLoginUserId()));}
 @cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog(requestEnable=false,responseEnable=false)
 @GetMapping("/export") @PreAuthorize("@ss.hasPermission('hrm:payroll:analysis:export')")
 public void export(@RequestParam Long entityId,@RequestParam Integer year,@RequestParam Integer month,@RequestParam(defaultValue="ENTITY") String dimension,javax.servlet.http.HttpServletResponse response)throws java.io.IOException{ExcelUtils.write(response,"预算与成本分析.xlsx","期间分析",HrmPayrollCostAnalysisRowVO.class,service.export(entityId,year,month,dimension,getLoginUserId()));}
 @cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog(requestEnable=false,responseEnable=false)
 @GetMapping("/template") @PreAuthorize("@ss.hasPermission('hrm:payroll:analysis:export')")
 public void template(@RequestParam Long entityId,@RequestParam Integer year,@RequestParam Integer month,javax.servlet.http.HttpServletResponse response)throws java.io.IOException{ExcelUtils.write(response,"预算营收编制模板.xlsx","计划资料",HrmPayrollFinancialImportRowVO.class,service.template(entityId,year,month,getLoginUserId()));}
 private List<HrmPayrollFinancialImportRowVO> read(MultipartFile file)throws java.io.IOException {
  if(file==null||file.isEmpty()||file.getSize()>10*1024*1024)throw cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception(cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.PAYROLL_FINANCIAL_INVALID);
  List<HrmPayrollFinancialImportRowVO> rows=new ArrayList<>();
  try(java.io.InputStream input=file.getInputStream()) {
   cn.idev.excel.FastExcelFactory.read(input,HrmPayrollFinancialImportRowVO.class,new cn.idev.excel.event.AnalysisEventListener<HrmPayrollFinancialImportRowVO>() {
    @Override public void invokeHeadMap(Map<Integer,String> head,cn.idev.excel.context.AnalysisContext context) {
     Set<String> expected=new HashSet<>();for(java.lang.reflect.Field field:HrmPayrollFinancialImportRowVO.class.getDeclaredFields()) {
      cn.idev.excel.annotation.ExcelProperty column=field.getAnnotation(cn.idev.excel.annotation.ExcelProperty.class);if(column!=null)expected.add(column.value()[0]);
     }
     Set<String> actual=new HashSet<>(head.values());
     if(actual.size()!=head.size()||!actual.equals(expected))throw cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception(cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.PAYROLL_FINANCIAL_HEADER_INVALID);
    }
    @Override public void invoke(HrmPayrollFinancialImportRowVO row,cn.idev.excel.context.AnalysisContext context) {
     if(rows.size()>=5000)throw cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception(cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.PAYROLL_FINANCIAL_INVALID);
     row.setSourceRow(context.readRowHolder().getRowIndex()+1).setSourceSheet(context.readSheetHolder().getSheetName());rows.add(row);
    }
    @Override public void doAfterAllAnalysed(cn.idev.excel.context.AnalysisContext context) {}
   }).autoCloseStream(false).doReadAll();
  } catch(RuntimeException e) {
   Throwable cause=e;while(cause!=null){if(cause instanceof cn.iocoder.yudao.framework.common.exception.ServiceException)throw (cn.iocoder.yudao.framework.common.exception.ServiceException)cause;cause=cause.getCause();}
   throw cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception(cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.PAYROLL_FINANCIAL_INVALID);
  }
  return rows;
 }

 @cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog(requestEnable=false,responseEnable=false)
 @PostMapping("/preview") @PreAuthorize("@ss.hasPermission('hrm:payroll:analysis:update')")
 public CommonResult<List<HrmPayrollFinancialPreviewVO>> preview(@RequestParam Long entityId,@RequestParam Integer year,@RequestParam Integer month,@RequestParam MultipartFile file)throws java.io.IOException{return success(service.preview(entityId,year,month,read(file),getLoginUserId()));}
 @cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog(requestEnable=false,responseEnable=false)
 @PostMapping("/import") @PreAuthorize("@ss.hasPermission('hrm:payroll:analysis:update')")
 public CommonResult<List<Long>> importData(@RequestParam Long entityId,@RequestParam Integer year,@RequestParam Integer month,@RequestParam String requestKey,@RequestParam MultipartFile file)throws java.io.IOException{return success(service.importRows(entityId,year,month,read(file),requestKey,"UPLOAD:"+cn.hutool.crypto.digest.DigestUtil.sha256Hex(file.getBytes()),getLoginUserId()));}
 @cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog(requestEnable=false,responseEnable=false)
 @PostMapping("/preview-errors") @PreAuthorize("@ss.hasPermission('hrm:payroll:analysis:update')")
 public void errors(@RequestParam Long entityId,@RequestParam Integer year,@RequestParam Integer month,@RequestParam MultipartFile file,javax.servlet.http.HttpServletResponse response)throws java.io.IOException{ExcelUtils.write(response,"计划导入逐行校验.xlsx","校验结果",HrmPayrollFinancialPreviewVO.class,service.preview(entityId,year,month,read(file),getLoginUserId()));}
}
