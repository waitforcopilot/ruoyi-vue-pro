package cn.iocoder.yudao.module.hrm.service.payroll;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollBankRowVO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.salary.monthrecord.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.employee.employment.HrmEmployeeSalaryCardDO;
import cn.iocoder.yudao.module.hrm.dal.mysql.salary.monthrecord.HrmSalaryMonthRecordMapper;
import cn.iocoder.yudao.module.hrm.service.employee.info.HrmEmployeeService;
import cn.iocoder.yudao.module.hrm.service.employee.employment.HrmEmployeeSalaryCardService;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.junit.jupiter.api.Test;
import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;
@Import({HrmPayrollPaymentService.class,HrmPayrollBatchService.class})
class HrmPayrollPaymentServiceTest extends BaseDbUnitTest {
    @Resource private HrmPayrollPaymentService service;
    @Resource private HrmPayrollBatchService batches;
    @Resource private HrmSalaryMonthRecordMapper mapper;
    @MockBean private HrmEmployeeService employees;
    @MockBean private HrmEmployeeSalaryCardService cards;
    @Test void partialFailureOnlyRetriesFailedRowsAndCannotReverseSuccess() {
        HrmSalaryMonthRecordDO batch=new HrmSalaryMonthRecordDO().setYear(2026).setMonth(10).setStatus(11);mapper.insert(batch);
        List<HrmSalaryMonthEmployeeRecordDO> results=new ArrayList<>();
        for(long id=1;id<=2;id++){
            when(employees.getEmployee(id)).thenReturn(new HrmEmployeeDO().setId(id).setName("员工"+id));
            when(cards.getSalaryCardByEmployeeId(id)).thenReturn(new HrmEmployeeSalaryCardDO().setEmployeeId(id).setBankCardNumber("123456"+id));
            results.add(new HrmSalaryMonthEmployeeRecordDO().setEmployeeId(id).setRealPaySalary(new BigDecimal("100.00")));
        }
        Long run=batches.snapshot(batch.getId(),Collections.emptyMap(),Collections.emptyMap(),results,1L);
        mapper.updateById(new HrmSalaryMonthRecordDO().setId(batch.getId()).setStatus(15));
        service.prepare(batch.getId(),run,3L);service.prepare(batch.getId(),run,3L);
        List<HrmPayrollBankRowVO> rows=service.export(batch.getId());assertEquals(2,rows.size());
        rows.get(0).setStatus("SUCCESS");rows.get(1).setStatus("FAILED");rows.get(1).setFailureReason("银行拒绝");
        Long receipt=service.reconcileFile(batch.getId(),"bank.xlsx","checksum-v1",rows,3L);
        assertEquals(receipt,service.reconcileFile(batch.getId(),"bank-copy.xlsx","checksum-v1",rows,3L));
        assertEquals(1,service.receipts(batch.getId()).size());assertNull(service.receipts(batch.getId()).get(0).getDetails());
        assertEquals(16,mapper.selectById(batch.getId()).getStatus());
        assertServiceException(()->service.retry(rows.get(0).getId(),3L),PAYROLL_PAYMENT_INVALID);
        service.retry(rows.get(1).getId(),3L);
        assertServiceException(()->service.reconcile(batch.getId(),Collections.singletonList(rows.get(1)),3L),PAYROLL_PAYMENT_INVALID);
        List<HrmPayrollBankRowVO> retries=service.export(batch.getId());assertEquals(1,retries.size());
        assertEquals(rows.get(1).getId(),retries.get(0).getId());retries.get(0).setStatus("SUCCESS");
        service.reconcile(batch.getId(),retries,3L);assertEquals(17,mapper.selectById(batch.getId()).getStatus());
        service.reconcile(batch.getId(),retries,3L);
        assertTrue(service.list(batch.getId()).get(0).getBankAccount().startsWith("****"));
        assertNull(service.list(batch.getId()).get(1).getFailureReason());
        batches.transition(batch.getId(),run,"archive","财务台账已核对",3L);
        assertEquals(10,mapper.selectById(batch.getId()).getStatus());
        assertEquals(1L,service.batches(new cn.iocoder.yudao.module.hrm.controller.admin.salary.vo.monthrecord.HrmSalaryMonthRecordPageReqVO()).getTotal());
    }
    @Test void accountOrAmountMismatchRollsBack() {
        HrmSalaryMonthRecordDO batch=new HrmSalaryMonthRecordDO().setYear(2026).setMonth(10).setStatus(11);mapper.insert(batch);
        when(employees.getEmployee(1L)).thenReturn(new HrmEmployeeDO().setId(1L).setName("员工"));
        when(cards.getSalaryCardByEmployeeId(1L)).thenReturn(new HrmEmployeeSalaryCardDO().setBankCardNumber("1234567"));
        Long run=batches.snapshot(batch.getId(),Collections.emptyMap(),Collections.emptyMap(),Collections.singletonList(new HrmSalaryMonthEmployeeRecordDO().setEmployeeId(1L).setRealPaySalary(new BigDecimal("100"))),1L);
        mapper.updateById(new HrmSalaryMonthRecordDO().setId(batch.getId()).setStatus(15));
        service.prepare(batch.getId(),run,3L);
        HrmPayrollBankRowVO row=service.export(batch.getId()).get(0);row.setStatus("SUCCESS");row.setAmount(new BigDecimal("101"));
        assertEquals("金额不匹配",service.validateRows(batch.getId(),Collections.singletonList(row)).get(0).getReason());
        assertServiceException(()->service.reconcile(batch.getId(),Collections.singletonList(row),3L),PAYROLL_PAYMENT_INVALID);
        assertEquals("PENDING",service.list(batch.getId()).get(0).getStatus());
        row.setAmount(new BigDecimal("100"));
        assertServiceException(()->service.reconcile(batch.getId(),Arrays.asList(row,row),3L),PAYROLL_PAYMENT_INVALID);
        assertEquals("PENDING",service.list(batch.getId()).get(0).getStatus());
    }
}
