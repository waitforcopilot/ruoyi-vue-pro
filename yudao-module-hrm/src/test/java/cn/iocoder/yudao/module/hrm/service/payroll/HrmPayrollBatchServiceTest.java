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
    @Resource private HrmPayrollBatchService service;
    @Resource private HrmSalaryMonthRecordMapper mapper;
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
}
