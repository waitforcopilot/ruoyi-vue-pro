package cn.iocoder.yudao.module.hrm.service.payroll;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.hrm.dal.dataobject.salary.monthrecord.HrmSalaryMonthRecordDO;
import cn.iocoder.yudao.module.hrm.dal.mysql.salary.monthrecord.HrmSalaryMonthRecordMapper;
import org.springframework.context.annotation.Import;
import org.junit.jupiter.api.Test;
import javax.annotation.Resource;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;
@Import(HrmPayrollBatchService.class)
class HrmPayrollBatchServiceTest extends BaseDbUnitTest {
 @org.springframework.boot.test.mock.mockito.MockBean private HrmPayrollSourceClaimService sourceClaims;

    @org.springframework.boot.test.mock.mockito.MockBean private cn.iocoder.yudao.module.hrm.service.payroll.HrmPayrollInputService payrollInputs;

    @org.springframework.boot.test.mock.mockito.MockBean private cn.iocoder.yudao.module.hrm.service.payroll.HrmPayrollAttendanceService payrollAttendanceService;
    @org.springframework.boot.test.mock.mockito.MockBean private HrmPayrollWorkService workService;
    @org.springframework.boot.test.mock.mockito.MockBean private cn.iocoder.yudao.module.hrm.service.payroll.HrmPayrollScopeService payrollScope;
    @org.springframework.boot.test.mock.mockito.MockBean private cn.iocoder.yudao.module.hrm.service.payroll.HrmPayrollInsurancePolicyService payrollInsurancePolicies;
    @Resource private HrmPayrollBatchService service;
    @Resource private HrmSalaryMonthRecordMapper mapper;
    @Test void explicitPaymentPlanIsAuditedAndOldRunCannotApproveChangedDate() {
        HrmSalaryMonthRecordDO batch = new HrmSalaryMonthRecordDO().setEntityId(10L).setYear(2026).setMonth(12).setStatus(11);
        mapper.insert(batch);
        java.time.LocalDate firstDate=java.time.LocalDate.of(2027,1,10), nextDate=java.time.LocalDate.of(2027,2,10);
        service.paymentPlan(batch.getId(),firstDate,"明确下年计划发放日",1L);
        service.paymentPlan(batch.getId(),firstDate,"同日期重试",1L);
        assertEquals(1,service.events(batch.getId()).size());
        assertTrue(service.events(batch.getId()).get(0).getSnapshot().contains("UNPLANNED"));
        Long first=service.snapshot(batch.getId(),Collections.emptyMap(),Collections.emptyMap(),Collections.emptyList(),1L);
        service.paymentPlan(batch.getId(),nextDate,"根据发放安排修改，重新核算",1L);
        assertEquals(firstDate,service.runs(batch.getId()).get(0).getExpectedPaymentDate());
        assertServiceException(()->service.transition(batch.getId(),first,"submit","旧试算",1L),PAYROLL_VERSION_CONFLICT);
        Long second=service.snapshot(batch.getId(),Collections.emptyMap(),Collections.emptyMap(),Collections.emptyList(),1L);
        assertEquals(nextDate,service.runs(batch.getId()).get(0).getExpectedPaymentDate());
        service.transition(batch.getId(),second,"submit","提交新日期版本",1L);
        assertServiceException(()->service.paymentPlan(batch.getId(),firstDate,"在审修改",1L),SALARY_MONTH_RECORD_STATUS_INVALID);
    }

    @Test void paymentPlanRequiresSubjectScopeExplicitDateAndEditableState() {
        HrmSalaryMonthRecordDO batch=new HrmSalaryMonthRecordDO().setEntityId(10L).setYear(2026).setMonth(10).setStatus(5);mapper.insert(batch);
        java.time.LocalDate date=java.time.LocalDate.of(2026,10,14);
        assertServiceException(()->service.paymentPlan(batch.getId(),null,"缺日期",1L),PAYROLL_PAYMENT_INVALID);
        assertServiceException(()->service.paymentPlan(batch.getId(),date,"",1L),PAYROLL_PAYMENT_INVALID);
        for(int status: new int[]{10,12,13,14,15,16,17}) {
            mapper.updateById(new HrmSalaryMonthRecordDO().setId(batch.getId()).setStatus(status));
            assertServiceException(()->service.paymentPlan(batch.getId(),date,"不可编辑",1L),SALARY_MONTH_RECORD_STATUS_INVALID);
        }
        mapper.updateById(new HrmSalaryMonthRecordDO().setId(batch.getId()).setStatus(5));
        org.mockito.Mockito.doThrow(new IllegalStateException("denied")).when(payrollScope).check(org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.eq("MANAGE"),org.mockito.ArgumentMatchers.eq(9L));
        assertThrows(IllegalStateException.class,()->service.paymentPlan(batch.getId(),date,"无维护授权",9L));
        HrmSalaryMonthRecordDO unbound=new HrmSalaryMonthRecordDO().setYear(2026).setMonth(10).setStatus(5);mapper.insert(unbound);
        assertServiceException(()->service.paymentPlan(unbound.getId(),date,"未绑定主体",1L),PAYROLL_ENTITY_INVALID);
    }

    @Test void versionsRemainImmutableAndApprovalUsesLatestVersion() {
        HrmSalaryMonthRecordDO batch=new HrmSalaryMonthRecordDO().setYear(2026).setMonth(10).setStatus(11);
        mapper.insert(batch);
        Long first=service.snapshot(batch.getId(),Collections.singletonMap("base",100),Collections.emptyMap(),Collections.emptyList(),1L);
        Long second=service.snapshot(batch.getId(),Collections.singletonMap("base",200),Collections.emptyMap(),Collections.emptyList(),1L);
        assertEquals(2,service.runs(batch.getId()).size());
        assertTrue(service.runs(batch.getId()).get(1).getInputSnapshot().contains("100"));
        assertServiceException(()->service.transition(batch.getId(),first,"submit","提交",1L),PAYROLL_VERSION_CONFLICT);
        service.transition(batch.getId(),second,"submit","提交",1L);
        assertServiceException(()->service.transition(batch.getId(),second,"review","复核",1L),PAYROLL_SELF_APPROVAL);
        service.transition(batch.getId(),second,"review","复核",2L);
        assertServiceException(()->service.transition(batch.getId(),second,"approve","审批",2L),PAYROLL_SELF_APPROVAL);
        service.transition(batch.getId(),second,"approve","审批",3L);
        service.transition(batch.getId(),second,"freeze","冻结",3L);
        assertEquals(15,mapper.selectById(batch.getId()).getStatus());
        assertServiceException(()->service.transition(batch.getId(),second,"archive","归档",3L),SALARY_MONTH_RECORD_STATUS_INVALID);
        assertEquals(4,service.events(batch.getId()).size());
    }
    @Resource private cn.iocoder.yudao.module.hrm.dal.mysql.salary.slip.HrmSalarySlipSendRecordMapper slipMapper;
    @Test void publishedSlipsMustBeWithdrawnBeforeUnfreezing() {
        HrmSalaryMonthRecordDO batch=new HrmSalaryMonthRecordDO().setYear(2026).setMonth(10).setStatus(11);
        mapper.insert(batch);
        Long run=service.snapshot(batch.getId(),Collections.emptyMap(),Collections.emptyMap(),Collections.emptyList(),1L);
        mapper.updateById(new HrmSalaryMonthRecordDO().setId(batch.getId()).setStatus(15));
        cn.iocoder.yudao.module.hrm.dal.dataobject.salary.slip.HrmSalarySlipSendRecordDO slip =
                cn.iocoder.yudao.module.hrm.dal.dataobject.salary.slip.HrmSalarySlipSendRecordDO.builder()
                        .monthRecordId(batch.getId()).year(2026).month(10).withdrawn(false).build();
        slipMapper.insert(slip);
        assertServiceException(()->service.transition(batch.getId(),run,"unfreeze","修订",3L),SALARY_MONTH_RECORD_STATUS_INVALID);
        slip.setWithdrawn(true); slipMapper.updateById(slip);
        service.transition(batch.getId(),run,"unfreeze","已撤回工资条",3L);
        assertEquals(11,mapper.selectById(batch.getId()).getStatus());
    }
    @Test void stateMachineDoesNotAllowSkippingApprovalOrPayment() {
        assertServiceException(()->HrmPayrollBatchService.nextStatus(11,"pay"),SALARY_MONTH_RECORD_STATUS_INVALID);
        assertServiceException(()->HrmPayrollBatchService.nextStatus(16,"archive"),SALARY_MONTH_RECORD_STATUS_INVALID);
        assertEquals(11,HrmPayrollBatchService.nextStatus(15,"unfreeze"));
        assertEquals(10,HrmPayrollBatchService.nextStatus(17,"archive"));
    }
    @Test void lockedBatchCannotReceiveANewSnapshotAndRejectionPreservesHistory() {
        HrmSalaryMonthRecordDO batch=new HrmSalaryMonthRecordDO().setYear(2026).setMonth(10).setStatus(11);
        mapper.insert(batch);
        Long run=service.snapshot(batch.getId(),Collections.emptyMap(),Collections.emptyMap(),Collections.emptyList(),1L);
        service.transition(batch.getId(),run,"submit","提交",1L);
        assertServiceException(()->service.snapshot(batch.getId(),Collections.emptyMap(),Collections.emptyMap(),Collections.emptyList(),1L),SALARY_MONTH_RECORD_STATUS_INVALID);
        service.transition(batch.getId(),run,"reject","数据需核实",2L);
        assertEquals(11,mapper.selectById(batch.getId()).getStatus());
        assertEquals(1,service.runs(batch.getId()).size());
        assertEquals(2,service.events(batch.getId()).size());
    }
    @Test void changedOvertimeSourceBlocksFreezeAndLeavesApprovalIntact(){
        HrmSalaryMonthRecordDO batch=new HrmSalaryMonthRecordDO().setYear(2026).setMonth(10).setStatus(11);mapper.insert(batch);
        Long run=service.snapshot(batch.getId(),Collections.singletonMap("overtime",Collections.emptyMap()),Collections.emptyMap(),Collections.emptyList(),1L);
        mapper.updateById(new HrmSalaryMonthRecordDO().setId(batch.getId()).setStatus(14));
        org.mockito.Mockito.doThrow(cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception(PAYROLL_WORK_INVALID)).when(workService).validateInputSnapshot(org.mockito.ArgumentMatchers.anyString());
        assertServiceException(()->service.transition(batch.getId(),run,"freeze","冻结",3L),PAYROLL_WORK_INVALID);
        assertEquals(14,mapper.selectById(batch.getId()).getStatus());assertTrue(service.events(batch.getId()).isEmpty());
    }
    @Test void manualAdjustmentStillValidatesOriginalWorkSources(){
        HrmSalaryMonthRecordDO batch=new HrmSalaryMonthRecordDO().setYear(2026).setMonth(10).setStatus(11);mapper.insert(batch);
        Long source=service.snapshot(batch.getId(),Collections.singletonMap("overtime",Collections.emptyMap()),Collections.emptyMap(),Collections.emptyList(),1L);
        Long adjusted=service.snapshot(batch.getId(),Collections.singletonMap("sourceRunId",source),Collections.emptyMap(),Collections.emptyList(),1L);
        mapper.updateById(new HrmSalaryMonthRecordDO().setId(batch.getId()).setStatus(14));
        String original=service.runs(batch.getId()).get(1).getInputSnapshot();
        org.mockito.Mockito.doThrow(cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception(PAYROLL_WORK_INVALID)).when(workService).validateInputSnapshot(original);
        assertServiceException(()->service.transition(batch.getId(),adjusted,"freeze","冻结",3L),PAYROLL_WORK_INVALID);
        assertEquals(14,mapper.selectById(batch.getId()).getStatus());
    }
}
