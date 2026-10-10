package cn.iocoder.yudao.module.hrm.service.payroll;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollBankTemplateReqVO;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollBankRowVO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.HrmPayrollBankTemplateDO;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.HrmPayrollBankTemplateMapper;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.util.*;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.PAYROLL_PAYMENT_INVALID;

@Service
public class HrmPayrollBankTemplateService {
    @Resource private HrmPayrollBankTemplateMapper mapper;
    private static final Set<String> ALLOWED=new HashSet<>(Arrays.asList("id","employeeName","bankName","bankAccount","amount","attempt","status","failureReason"));
    public List<HrmPayrollBankTemplateDO> list() {
        return mapper.selectList(new LambdaQueryWrapperX<HrmPayrollBankTemplateDO>().orderByDesc(HrmPayrollBankTemplateDO::getId));
    }
    public Long create(HrmPayrollBankTemplateReqVO request) {
        validateColumns(request.getColumns(),Arrays.asList("id","employeeName","bankAccount","amount","attempt"));
        validateColumns(request.getReturnColumns(),Arrays.asList("id","bankAccount","amount","attempt","status"));
        if(request.getSuccessValue()==null||request.getFailedValue()==null||request.getSuccessValue().equals(request.getFailedValue()))throw exception(PAYROLL_PAYMENT_INVALID);
        HrmPayrollBankTemplateDO record=new HrmPayrollBankTemplateDO();
        record.setName(request.getName());record.setColumns(JsonUtils.toJsonString(request.getColumns()));
        record.setReturnColumns(JsonUtils.toJsonString(request.getReturnColumns()));record.setSuccessValue(request.getSuccessValue());record.setFailedValue(request.getFailedValue());
        mapper.insert(record);return record.getId();
    }
    private void validateColumns(List<HrmPayrollBankTemplateReqVO.Column> columns,List<String> required) {
        if(columns==null||columns.size()>12)throw exception(PAYROLL_PAYMENT_INVALID);
        Set<String> fields=new HashSet<>(),labels=new HashSet<>();
        for(HrmPayrollBankTemplateReqVO.Column column:columns) {
            if(!ALLOWED.contains(column.getField())||!fields.add(column.getField())||column.getLabel()==null||column.getLabel().trim().isEmpty()||!labels.add(column.getLabel()))throw exception(PAYROLL_PAYMENT_INVALID);
        }
        if(!fields.containsAll(required))throw exception(PAYROLL_PAYMENT_INVALID);
    }
    public List<HrmPayrollBankRowVO> readCallback(Long id,org.springframework.web.multipart.MultipartFile file)throws java.io.IOException {
        HrmPayrollBankTemplateDO template=mapper.selectById(id);
        if(template==null||file==null||file.isEmpty())throw exception(PAYROLL_PAYMENT_INVALID);
        List<HrmPayrollBankTemplateReqVO.Column> columns=JsonUtils.parseArray(template.getReturnColumns(),HrmPayrollBankTemplateReqVO.Column.class);
        List<HrmPayrollBankRowVO> result=new ArrayList<>();
        try(java.io.InputStream stream=file.getInputStream()) {
            cn.idev.excel.FastExcelFactory.read(stream,new cn.idev.excel.event.AnalysisEventListener<Map<Integer,String>>() {
                @Override public void invokeHeadMap(Map<Integer,String> head,cn.idev.excel.context.AnalysisContext context) {
                    for(int i=0;i<columns.size();i++)if(!Objects.equals(columns.get(i).getLabel(),head.get(i)))throw exception(PAYROLL_PAYMENT_INVALID);
                }
                @Override public void invoke(Map<Integer,String> data,cn.idev.excel.context.AnalysisContext context) {
                    if(result.size()>=10000)throw exception(PAYROLL_PAYMENT_INVALID);
                    HrmPayrollBankRowVO row=new HrmPayrollBankRowVO();
                    try {
                        for(int i=0;i<columns.size();i++){
                            String value=data.get(i);
                            switch(columns.get(i).getField()) {
                                case "id": row.setId(Long.valueOf(value));break;
                                case "attempt": row.setAttempt(Integer.valueOf(value));break;
                                case "bankAccount": row.setBankAccount(value);break;
                                case "amount": row.setAmount(new java.math.BigDecimal(value));break;
                                case "status":
                                    if(Objects.equals(value,template.getSuccessValue()))row.setStatus("SUCCESS");
                                    else if(Objects.equals(value,template.getFailedValue()))row.setStatus("FAILED");
                                    else throw exception(PAYROLL_PAYMENT_INVALID);
                                    break;
                                case "failureReason": row.setFailureReason(value);break;
                            }
                        }
                    }catch(NumberFormatException ex){throw exception(PAYROLL_PAYMENT_INVALID);}
                    if("FAILED".equals(row.getStatus())&&(row.getFailureReason()==null||row.getFailureReason().trim().isEmpty()))row.setFailureReason("银行返回失败："+template.getFailedValue());
                    result.add(row);
                }
                @Override public void doAfterAllAnalysed(cn.idev.excel.context.AnalysisContext context) {}
            }).headRowNumber(1).sheet().doRead();
        }
        return result;
    }
    public List<HrmPayrollBankTemplateReqVO.Column> columns(Long id) {
        HrmPayrollBankTemplateDO template=mapper.selectById(id);
        if(template==null)throw exception(PAYROLL_PAYMENT_INVALID);
        return JsonUtils.parseArray(template.getColumns(),HrmPayrollBankTemplateReqVO.Column.class);
    }
    public List<List<Object>> format(List<HrmPayrollBankRowVO> rows,List<HrmPayrollBankTemplateReqVO.Column> columns) {
        List<List<Object>> result=new ArrayList<>();
        for(HrmPayrollBankRowVO row:rows){
            List<Object> data=new ArrayList<>();
            for(HrmPayrollBankTemplateReqVO.Column column:columns){
                switch(column.getField()) {
                    case "id": data.add(row.getId());break;
                    case "employeeName": data.add(row.getEmployeeName());break;
                    case "bankName": data.add(row.getBankName());break;
                    case "bankAccount": data.add(row.getBankAccount());break;
                    case "amount": data.add(row.getAmount());break;
                    case "attempt": data.add(row.getAttempt());break;
                    case "status": data.add("");break;
                    case "failureReason": data.add("");break;
                    default: throw exception(PAYROLL_PAYMENT_INVALID);
                }
            }
            result.add(data);
        }
        return result;
    }
}
