package cn.iocoder.yudao.module.hrm.service.payroll;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.hrm.service.salary.employee.*;
import cn.iocoder.yudao.module.hrm.service.salary.config.HrmSalaryOptionService;
import cn.iocoder.yudao.module.hrm.service.salary.monthrecord.HrmSalaryMonthRecordService;
import cn.iocoder.yudao.module.hrm.service.employee.info.HrmEmployeeService;
import cn.iocoder.yudao.module.hrm.dal.mysql.employee.info.HrmEmployeeMapper;
import cn.iocoder.yudao.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.salary.config.HrmSalaryOptionDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.salary.employee.HrmSalaryChangeRecordDO;
import cn.iocoder.yudao.module.hrm.controller.admin.salary.vo.employeeinfo.HrmSalaryEmployeeInfoUpdateReqVO;
import cn.iocoder.yudao.module.hrm.controller.admin.salary.vo.option.HrmSalaryOptionValueVO;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.junit.jupiter.api.*;
import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Arrays;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
@Import({HrmPayrollSalaryProposalService.class,HrmSalaryEmployeeInfoServiceImpl.class})
class HrmPayrollSalaryProposalIntegrationTest extends BaseDbUnitTest {
 @org.springframework.boot.test.mock.mockito.MockBean private cn.iocoder.yudao.module.hrm.service.payroll.HrmPayrollConfigGuardService configGuard;

    @Resource private HrmSalaryEmployeeInfoService salaryService;
    @Resource private HrmPayrollSalaryProposalService proposalService;
    @Resource private HrmEmployeeMapper employeeMapper;
    @MockBean private HrmSalaryOptionService options;
    @MockBean private HrmSalaryChangeRecordService changes;
    @MockBean private HrmEmployeeService employees;
    @MockBean private HrmSalaryMonthRecordService months;
    private HrmEmployeeDO employee;
    @BeforeEach void setup() {
        cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.setLoginUser(
                new cn.iocoder.yudao.framework.security.core.LoginUser().setId(1L).setUserType(2),new org.springframework.mock.web.MockHttpServletRequest());
        employee=new HrmEmployeeDO().setName("审批集成员工").setJobNumber("APPROVAL-INTEGRATION");employeeMapper.insert(employee);
        when(employees.validateEmployeeExists(employee.getId())).thenReturn(employee);
        when(options.getSalaryOptionList(true)).thenReturn(Collections.singletonList(HrmSalaryOptionDO.builder().code(10101).parentCode(10).name("基本工资").type(1).calculateEnabled(true).enabled(true).build()));
    }
    private HrmSalaryEmployeeInfoUpdateReqVO request(int code, String amount) {
        return new HrmSalaryEmployeeInfoUpdateReqVO().setEmployeeId(employee.getId()).setRecordType(1).setChangeReason(1)
                .setSalaryOptions(Collections.singletonList(new HrmSalaryOptionValueVO().setCode(code).setValue(new BigDecimal(amount))));
    }
    @AfterEach void clear(){org.springframework.security.core.context.SecurityContextHolder.clearContext();}
    @Test void publicSalaryUpdateSubmitsProposalAndOnlyFinalApprovalWritesSalary() {
        doAnswer(invocation->{((HrmSalaryChangeRecordDO)invocation.getArgument(0)).setId(99L);return null;}).when(changes).createSalaryChangeRecord(any());
        HrmSalaryEmployeeInfoUpdateReqVO request=new HrmSalaryEmployeeInfoUpdateReqVO().setEmployeeId(employee.getId()).setRecordType(1).setChangeReason(1)
                .setSalaryOptions(Collections.singletonList(new HrmSalaryOptionValueVO().setCode(10101).setValue(new BigDecimal("5000.00"))));
        Long proposal=salaryService.updateSalaryEmployeeInfo(request);
        assertNull(salaryService.getSalaryEmployeeInfoByEmployeeId(employee.getId()));
        proposalService.transition(proposal,"review","复核通过",2L);
        assertNull(salaryService.getSalaryEmployeeInfoByEmployeeId(employee.getId()));
        proposalService.transition(proposal,"approve","财务批准",3L);
        assertEquals(0,new BigDecimal("5000.00").compareTo(salaryService.getSalaryEmployeeInfoByEmployeeId(employee.getId()).getRegularSalary()));
    }
    @Test void computedItemIsRejectedAtSubmissionEvenWhenZero() {
        assertServiceException(() -> salaryService.updateSalaryEmployeeInfo(request(240101, "0")),
                SALARY_PROPOSAL_OPTION_INVALID, 240101);
        assertTrue(proposalService.list().isEmpty());
        assertNull(salaryService.getSalaryEmployeeInfoByEmployeeId(employee.getId()));
    }
    @Test void probationItemsAndAmountsAreValidatedBeforeSnapshot() {
        HrmSalaryEmployeeInfoUpdateReqVO request = request(10101, "6000");
        request.setProbationSalaryOptions(Collections.singletonList(new HrmSalaryOptionValueVO().setCode(230101).setValue(BigDecimal.ZERO)));
        assertServiceException(() -> salaryService.updateSalaryEmployeeInfo(request), SALARY_PROPOSAL_OPTION_INVALID, 230101);
        request.setProbationSalaryOptions(Collections.singletonList(new HrmSalaryOptionValueVO().setCode(10101).setValue(new BigDecimal("-1"))));
        assertServiceException(() -> salaryService.updateSalaryEmployeeInfo(request), SALARY_PROPOSAL_AMOUNT_INVALID, "基本工资(10101)");
        assertServiceException(() -> salaryService.updateSalaryEmployeeInfo(request(10101, "0.001")), SALARY_PROPOSAL_AMOUNT_INVALID, "基本工资(10101)");
        assertTrue(proposalService.list().isEmpty());
    }
    @Test void duplicateItemCannotInflateSalary() {
        HrmSalaryEmployeeInfoUpdateReqVO request = request(10101, "6000");
        request.setSalaryOptions(Arrays.asList(request.getSalaryOptions().get(0), request.getSalaryOptions().get(0)));
        assertServiceException(() -> salaryService.updateSalaryEmployeeInfo(request), SALARY_PROPOSAL_OPTION_DUPLICATE, 10101);
        assertTrue(proposalService.list().isEmpty());
    }
    @Test void itemDisabledAfterSubmissionStillBlocksFinalApplication() {
        Long id = salaryService.updateSalaryEmployeeInfo(request(10101, "6000"));
        proposalService.transition(id, "review", "复核通过", 2L);
        when(options.getSalaryOptionList(true)).thenReturn(Collections.emptyList());
        assertServiceException(() -> proposalService.transition(id, "approve", "财务批准", 3L), SALARY_DATA_ILLEGAL);
        assertEquals("FINANCE_REVIEW", proposalService.list().get(0).getStatus());
        assertNull(salaryService.getSalaryEmployeeInfoByEmployeeId(employee.getId()));
    }

}
