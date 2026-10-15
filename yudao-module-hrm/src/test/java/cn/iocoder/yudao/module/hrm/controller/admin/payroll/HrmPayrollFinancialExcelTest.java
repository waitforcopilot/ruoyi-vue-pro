package cn.iocoder.yudao.module.hrm.controller.admin.payroll;
import static org.junit.jupiter.api.Assertions.*;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollFinancialImportRowVO;
import cn.idev.excel.FastExcelFactory;
import java.io.ByteArrayOutputStream;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
class HrmPayrollFinancialExcelTest {
 @Test void workbookKeepsActualSheetAndRowAfterBlankRows() throws Exception {
  ByteArrayOutputStream bytes=new ByteArrayOutputStream();
  try(cn.idev.excel.ExcelWriter writer=FastExcelFactory.write(bytes,HrmPayrollFinancialImportRowVO.class).build()) {
   HrmPayrollFinancialImportRowVO row=new HrmPayrollFinancialImportRowVO().setYear("2026").setMonth("10").setDimension("ENTITY").setDimensionKey("ALL").setBudget("9000").setEvidence("真实来源");
   writer.write(Arrays.asList(new HrmPayrollFinancialImportRowVO(),row),FastExcelFactory.writerSheet(0,"资料A").build());
   writer.write(Collections.singletonList(row),FastExcelFactory.writerSheet(1,"资料B").build());
  }
  List<HrmPayrollFinancialImportRowVO> rows=ReflectionTestUtils.invokeMethod(new HrmPayrollFinancialController(),"read",new MockMultipartFile("file","计划.xlsx","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",bytes.toByteArray()));
  assertNotNull(rows);assertEquals(2,rows.size());assertEquals("资料A",rows.get(0).getSourceSheet());assertEquals(3,rows.get(0).getSourceRow());assertEquals("资料B",rows.get(1).getSourceSheet());assertEquals(2,rows.get(1).getSourceRow());assertEquals("9000",rows.get(0).getBudget());
 }
 @Test void financialAndCostResponsesUseOnlyDedicatedScopedAudit() {
  for(Class<?> controller:Arrays.asList(HrmPayrollFinancialController.class,HrmPayrollCostController.class))for(java.lang.reflect.Method method:controller.getDeclaredMethods())
   if(method.isAnnotationPresent(org.springframework.web.bind.annotation.GetMapping.class)||method.isAnnotationPresent(org.springframework.web.bind.annotation.PostMapping.class)) {
    cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog log=method.getAnnotation(cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog.class);
    assertNotNull(log,method.getName());assertFalse(log.requestEnable());assertFalse(log.responseEnable());
   }
 }
 @Test void changedCurrencyHeadersCannotSilentlyDiscardAnAmount() {
  List<List<String>> headers=new ArrayList<>();for(java.lang.reflect.Field field:HrmPayrollFinancialImportRowVO.class.getDeclaredFields()) {
   cn.idev.excel.annotation.ExcelProperty column=field.getAnnotation(cn.idev.excel.annotation.ExcelProperty.class);if(column!=null)headers.add(Arrays.asList(column.value()));
  }
  headers.set(5,Collections.singletonList("工资成本预算（万元）"));
  ByteArrayOutputStream bytes=new ByteArrayOutputStream();FastExcelFactory.write(bytes).head(headers).sheet("计划").doWrite(Collections.singletonList(Arrays.asList("2026","10","ENTITY","ALL","","1","2","3","正式资料")));
  cn.iocoder.yudao.framework.common.exception.ServiceException error=assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->ReflectionTestUtils.invokeMethod(new HrmPayrollFinancialController(),"read",new MockMultipartFile("file","错单位.xlsx","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",bytes.toByteArray())));
  assertEquals(cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.PAYROLL_FINANCIAL_HEADER_INVALID.getCode(),error.getCode());
 }
}
