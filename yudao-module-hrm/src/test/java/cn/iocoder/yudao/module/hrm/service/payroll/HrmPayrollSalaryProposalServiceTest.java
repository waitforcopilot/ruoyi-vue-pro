package cn.iocoder.yudao.module.hrm.service.payroll;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.HrmPayrollSalaryProposalDO;
import cn.iocoder.yudao.module.hrm.dal.mysql.employee.info.HrmEmployeeMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.HrmPayrollSalaryProposalMapper;
import cn.iocoder.yudao.module.hrm.service.salary.employee.HrmSalaryEmployeeInfoService;
import cn.iocoder.yudao.module.hrm.controller.admin.salary.vo.employeeinfo.HrmSalaryEmployeeInfoUpdateReqVO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.salary.employee.HrmSalaryEmployeeInfoDO;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.junit.jupiter.api.*;
import javax.annotation.Resource;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;
@Import(HrmPayrollSalaryProposalService.class)
class HrmPayrollSalaryProposalServiceTest extends BaseDbUnitTest {
 @org.springframework.boot.test.mock.mockito.MockBean private cn.iocoder.yudao.module.hrm.service.payroll.HrmPayrollConfigGuardService configGuard;

    @Resource private HrmPayrollSalaryProposalService service;
    @Resource private HrmPayrollSalaryProposalMapper mapper;
    @Resource private HrmEmployeeMapper employeeMapper;
    @MockBean private HrmSalaryEmployeeInfoService salaryService;
    private Long employeeId;
    @BeforeEach void employee(){
        HrmEmployeeDO employee=new HrmEmployeeDO().setName("审批测试员工").setJobNumber("APPROVAL-01");employeeMapper.insert(employee);employeeId=employee.getId();
        cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.setLoginUser(
                new cn.iocoder.yudao.framework.security.core.LoginUser().setId(1L).setUserType(2),new org.springframework.mock.web.MockHttpServletRequest());
    }
    @AfterEach void clearIdentity(){org.springframework.security.core.context.SecurityContextHolder.clearContext();}
    private Long create(){return service.create(new HrmSalaryEmployeeInfoUpdateReqVO().setEmployeeId(employeeId).setRecordType(1).setChangeReason(1));}
    @Test void salaryDoesNotChangeUntilDifferentHrAndFinanceActorsApprove(){
        Long id=create();verify(salaryService,never()).applyApprovedSalaryEmployeeInfo(any());
        assertServiceException(()->service.transition(id,"approve","批准",3L),SALARY_CHANGE_RECORD_STATUS_INVALID);
        assertServiceException(()->service.transition(id,"review","复核",1L),PAYROLL_SELF_APPROVAL);
        service.transition(id,"review","HR复核通过",2L);
        assertServiceException(()->service.transition(id,"approve","批准",2L),PAYROLL_SELF_APPROVAL);
        when(salaryService.applyApprovedSalaryEmployeeInfo(any())).thenReturn(99L);
        service.transition(id,"approve","财务批准",3L);
        assertEquals("APPROVED",mapper.selectById(id).getStatus());assertEquals(99L,mapper.selectById(id).getChangeRecordId());
        verify(salaryService,times(1)).applyApprovedSalaryEmployeeInfo(any());
        assertServiceException(()->service.transition(id,"approve","重复批准",3L),SALARY_CHANGE_RECORD_STATUS_INVALID);
    }
    @Test void rejectionKeepsSnapshotAndAllowsNewSubmission(){
        Long id=create();String snapshot=mapper.selectById(id).getRequestSnapshot();
        assertServiceException(this::create,SALARY_PENDING_CHANGE_RECORD_EXISTS);
        service.transition(id,"reject","金额依据不足",2L);
        assertEquals(snapshot,mapper.selectById(id).getRequestSnapshot());assertEquals("REJECTED",mapper.selectById(id).getStatus());
        assertNotEquals(id,create());verify(salaryService,never()).applyApprovedSalaryEmployeeInfo(any());
    }
    @Test void staleBaselineBlocksApproval(){
        Long id=create();service.transition(id,"review","复核",2L);
        when(salaryService.getSalaryEmployeeInfoByEmployeeIdForUpdate(employeeId)).thenReturn(HrmSalaryEmployeeInfoDO.builder().employeeId(employeeId).regularSalary(BigDecimal.TEN).build());
        assertServiceException(()->service.transition(id,"approve","批准",3L),PAYROLL_VERSION_CONFLICT);
        assertEquals("FINANCE_REVIEW",mapper.selectById(id).getStatus());verify(salaryService,never()).applyApprovedSalaryEmployeeInfo(any());
    }
    @Test void applicationFailureDoesNotMarkProposalApproved(){
        Long id=create();service.transition(id,"review","复核",2L);
        when(salaryService.applyApprovedSalaryEmployeeInfo(any())).thenThrow(new IllegalArgumentException("invalid wage item"));
        assertThrows(IllegalArgumentException.class,()->service.transition(id,"approve","批准",3L));
        assertEquals("FINANCE_REVIEW",mapper.selectById(id).getStatus());
    }
}
