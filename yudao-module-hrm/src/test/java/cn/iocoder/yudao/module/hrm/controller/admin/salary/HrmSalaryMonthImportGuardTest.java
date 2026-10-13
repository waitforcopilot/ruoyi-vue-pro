package cn.iocoder.yudao.module.hrm.controller.admin.salary;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.salary.monthrecord.HrmSalaryMonthRecordDO;
import cn.iocoder.yudao.module.hrm.service.payroll.HrmPayrollScopeService;
import cn.iocoder.yudao.module.hrm.service.salary.monthrecord.HrmSalaryMonthRecordService;
import java.util.Collections;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

class HrmSalaryMonthImportGuardTest {
 private HrmSalaryMonthRecordController controller;
 private HrmPayrollScopeService scope;
 private HrmSalaryMonthRecordService service;
 @BeforeEach void setup(){controller=new HrmSalaryMonthRecordController();scope=mock(HrmPayrollScopeService.class);service=mock(HrmSalaryMonthRecordService.class);ReflectionTestUtils.setField(controller,"payrollScope",scope);ReflectionTestUtils.setField(controller,"salaryMonthRecordService",service);when(scope.require(1L,"CALCULATE")).thenReturn(new HrmSalaryMonthRecordDO().setId(1L).setEntityId(2L).setStatus(5));}
 @Test void unapprovedScopedFileIsRejectedBeforeExcelParserRuns(){
  MockMultipartFile file=new MockMultipartFile("attendanceFile","not-excel.xlsx","application/octet-stream",new byte[]{1,2,3});
  assertServiceException(()->controller.computeMonthRecordWithImport(1L,true,false,file,null,null),PAYROLL_INPUT_INVALID);verifyNoInteractions(service);
 }
 @Test void scopeIsCheckedBeforeFileContentAndApprovedInputCalculationStillWorks()throws Exception{
  controller.computeMonthRecordWithImport(1L,true,false,null,null,null);
  verify(service).computeMonthRecord(1L,true,false,Collections.emptyList(),Collections.emptyList(),Collections.emptyList());
  reset(service);when(scope.require(1L,"CALCULATE")).thenThrow(cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception(PAYROLL_ENTITY_INVALID));
  assertServiceException(()->controller.computeMonthRecordWithImport(1L,true,false,new MockMultipartFile("file",new byte[]{1}),null,null),PAYROLL_ENTITY_INVALID);verifyNoInteractions(service);
 }
}
