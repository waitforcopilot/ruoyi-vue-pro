package cn.iocoder.yudao.module.hrm.controller.admin.payroll;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollBankRowVO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.HrmPayrollPaymentDO;
import cn.iocoder.yudao.module.hrm.service.payroll.HrmPayrollPaymentService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Validator;
import java.util.*;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
@RestController @RequestMapping("/hrm/payroll/payment")
public class HrmPayrollPaymentController {
    @Resource private HrmPayrollPaymentService service;
    @Resource private Validator validator;
    @Resource private cn.iocoder.yudao.module.hrm.service.payroll.HrmPayrollBankTemplateService templates;
    @GetMapping("/batches") @PreAuthorize("@ss.hasPermission('hrm:payroll:payment:query')")
    public CommonResult<cn.iocoder.yudao.framework.common.pojo.PageResult<cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollPaymentBatchRespVO>> batches(
            @javax.validation.Valid cn.iocoder.yudao.module.hrm.controller.admin.salary.vo.monthrecord.HrmSalaryMonthRecordPageReqVO request) {
        return success(service.batches(request));
    }
    @GetMapping("/templates") @PreAuthorize("@ss.hasPermission('hrm:payroll:payment:query')")
    public CommonResult<List<cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.HrmPayrollBankTemplateDO>> templates(){return success(templates.list());}
    @PostMapping("/template") @PreAuthorize("@ss.hasPermission('hrm:payroll:payment:config')")
    public CommonResult<Long> createTemplate(@javax.validation.Valid @RequestBody cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollBankTemplateReqVO request){return success(templates.create(request));}
    @PostMapping("/prepare") @PreAuthorize("@ss.hasPermission('hrm:payroll:batch:pay')")
    public CommonResult<Boolean> prepare(@RequestParam Long batchId,@RequestParam Long runId){service.prepare(batchId,runId,getLoginUserId());return success(true);}
    @GetMapping("/list") @PreAuthorize("@ss.hasPermission('hrm:payroll:payment:query')")
    public CommonResult<List<HrmPayrollPaymentDO>> list(@RequestParam Long batchId){return success(service.list(batchId));}
    @GetMapping("/export") @PreAuthorize("@ss.hasPermission('hrm:payroll:payment:export')")
    public void export(@RequestParam Long batchId,@RequestParam(required=false) Long templateId,HttpServletResponse response)throws java.io.IOException{
        List<HrmPayrollBankRowVO> rows=service.export(batchId);
        if(templateId==null){ExcelUtils.write(response,"银行代发.xlsx","代发明细",HrmPayrollBankRowVO.class,rows);return;}
        List<cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollBankTemplateReqVO.Column> columns=templates.columns(templateId);
        List<List<String>> head=new ArrayList<>();for(cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollBankTemplateReqVO.Column column:columns)head.add(Collections.singletonList(column.getLabel()));
        ExcelUtils.write(response,"银行代发.xlsx","代发明细",head,templates.format(rows,columns));
    }
    @GetMapping("/import-template") @PreAuthorize("@ss.hasPermission('hrm:payroll:payment:query')")
    public void template(HttpServletResponse response)throws java.io.IOException{ExcelUtils.write(response,"银行回盘模板.xlsx","回盘",HrmPayrollBankRowVO.class,Collections.emptyList());}
    @GetMapping("/receipts") @PreAuthorize("@ss.hasPermission('hrm:payroll:payment:query')")
    public CommonResult<List<cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.HrmPayrollPaymentReceiptDO>> receipts(@RequestParam Long batchId){return success(service.receipts(batchId));}
    @PostMapping("/validate") @PreAuthorize("@ss.hasPermission('hrm:payroll:payment:reconcile')")
    public CommonResult<List<cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollBankDifferenceVO>> validate(
            @RequestParam Long batchId,@RequestParam(required=false) Long templateId,@RequestParam MultipartFile file)throws java.io.IOException {
        return success(service.validateRows(batchId,readRows(templateId,file)));
    }
    private List<HrmPayrollBankRowVO> readRows(Long templateId,MultipartFile file)throws java.io.IOException {
        List<HrmPayrollBankRowVO> rows=templateId==null?ExcelUtils.read(file,HrmPayrollBankRowVO.class,10001):templates.readCallback(templateId,file);
        if(rows.isEmpty()||rows.size()>10000)throw cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception(cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.PAYROLL_PAYMENT_INVALID);
        for(HrmPayrollBankRowVO row:rows)if(!validator.validate(row).isEmpty())throw new javax.validation.ConstraintViolationException(validator.validate(row));
        return rows;
    }
    @PostMapping("/reconcile") @PreAuthorize("@ss.hasPermission('hrm:payroll:payment:reconcile')")
    public CommonResult<Boolean> reconcile(@RequestParam Long batchId,@RequestParam(required=false) Long templateId,@RequestParam MultipartFile file)throws java.io.IOException{
        List<HrmPayrollBankRowVO> rows=readRows(templateId,file);
        String digest=cn.hutool.crypto.digest.DigestUtil.sha256Hex(file.getBytes());
        String fileName=file.getOriginalFilename();if(fileName!=null&&fileName.length()>255)fileName=fileName.substring(0,255);
        service.reconcileFile(batchId,fileName,digest,rows,getLoginUserId());return success(true);
    }
    @PostMapping("/retry") @PreAuthorize("@ss.hasPermission('hrm:payroll:batch:pay')")
    public CommonResult<Boolean> retry(@RequestParam Long id){service.retry(id,getLoginUserId());return success(true);}
}
