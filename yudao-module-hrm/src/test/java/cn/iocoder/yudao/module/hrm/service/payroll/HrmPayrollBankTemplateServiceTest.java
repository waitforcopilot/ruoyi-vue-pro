package cn.iocoder.yudao.module.hrm.service.payroll;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.*;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.PAYROLL_PAYMENT_INVALID;
@Import(HrmPayrollBankTemplateService.class)
class HrmPayrollBankTemplateServiceTest extends BaseDbUnitTest {
    @Resource private HrmPayrollBankTemplateService service;
    private HrmPayrollBankTemplateReqVO request() {
        HrmPayrollBankTemplateReqVO request=new HrmPayrollBankTemplateReqVO();request.setName("银行 A 工资导入");
        List<HrmPayrollBankTemplateReqVO.Column> columns=new ArrayList<>();
        for(String name:Arrays.asList("bankAccount","amount","employeeName","id","attempt")){
            HrmPayrollBankTemplateReqVO.Column column=new HrmPayrollBankTemplateReqVO.Column();column.setField(name);column.setLabel(name);columns.add(column);
        }
        request.setColumns(columns);
        List<HrmPayrollBankTemplateReqVO.Column> returned=new ArrayList<>();
        for(String name:Arrays.asList("id","attempt","bankAccount","amount","status")){
            HrmPayrollBankTemplateReqVO.Column column=new HrmPayrollBankTemplateReqVO.Column();column.setField(name);column.setLabel(name);returned.add(column);
        }
        request.setReturnColumns(returned);request.setSuccessValue("成功");request.setFailedValue("失败");return request;
    }
    @Test void templatesAreImmutableAndFormattingPreservesAccountAndDecimal() {
        HrmPayrollBankTemplateReqVO request=request();Long first=service.create(request);
        request.getColumns().get(0).setLabel("收款账户");Long second=service.create(request);
        assertNotEquals(first,second);assertEquals("bankAccount",service.columns(first).get(0).getLabel());
        HrmPayrollBankRowVO row=new HrmPayrollBankRowVO();row.setId(1L);row.setAttempt(1);row.setBankAccount("00001234567890123456");row.setEmployeeName("员工");row.setAmount(new BigDecimal("123.45"));
        List<Object> values=service.format(Collections.singletonList(row),service.columns(second)).get(0);
        assertEquals("00001234567890123456",values.get(0));assertEquals(new BigDecimal("123.45"),values.get(1));
    }
    @Test void missingReferencesDuplicateOrUnknownColumnsAreRejected() {
        HrmPayrollBankTemplateReqVO request=request();request.getColumns().get(4).setField("bankName");
        assertServiceException(()->service.create(request),PAYROLL_PAYMENT_INVALID);
        request.getColumns().get(4).setField("id");assertServiceException(()->service.create(request),PAYROLL_PAYMENT_INVALID);
        request.getColumns().get(4).setField("expression");assertServiceException(()->service.create(request),PAYROLL_PAYMENT_INVALID);
    }
    @Test void callbackUsesConfiguredHeadingsAndStatusValues() throws Exception {
        Long id=service.create(request());
        java.io.ByteArrayOutputStream output=new java.io.ByteArrayOutputStream();
        List<List<String>> head=new ArrayList<>();for(HrmPayrollBankTemplateReqVO.Column column:request().getReturnColumns())head.add(Collections.singletonList(column.getLabel()));
        cn.idev.excel.FastExcelFactory.write(output).head(head).sheet("回盘").doWrite(Collections.singletonList(Arrays.asList("1","2","00001234567890123456","100.20","成功")));
        org.springframework.mock.web.MockMultipartFile file=new org.springframework.mock.web.MockMultipartFile("file","bank.xlsx","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",output.toByteArray());
        HrmPayrollBankRowVO row=service.readCallback(id,file).get(0);
        assertEquals("SUCCESS",row.getStatus());assertEquals(2,row.getAttempt());assertEquals("00001234567890123456",row.getBankAccount());assertEquals(new BigDecimal("100.20"),row.getAmount());
        java.io.ByteArrayOutputStream wrong=new java.io.ByteArrayOutputStream();head.set(0,Collections.singletonList("错误表头"));
        cn.idev.excel.FastExcelFactory.write(wrong).head(head).sheet("回盘").doWrite(Collections.singletonList(Arrays.asList("1","2","00001234567890123456","100.20","成功")));
        org.springframework.mock.web.MockMultipartFile mismatch=new org.springframework.mock.web.MockMultipartFile("file","bank.xlsx","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",wrong.toByteArray());
        assertThrows(RuntimeException.class,()->service.readCallback(id,mismatch));
    }
}
