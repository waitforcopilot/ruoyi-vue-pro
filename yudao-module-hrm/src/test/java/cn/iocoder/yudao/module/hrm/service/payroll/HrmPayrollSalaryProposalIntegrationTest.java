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
    @AfterEach void clear(){org.springframework.security.core.context.SecurityContextHolder.clearContext();}
    @Test void publicSalaryUpdateSubmitsProposalAndOnlyFinalApprovalWritesSalary() {
        cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.setLoginUser(
                new cn.iocoder.yudao.framework.security.core.LoginUser().setId(1L).setUserType(2),new org.springframework.mock.web.MockHttpServletRequest());
        HrmEmployeeDO employee=new HrmEmployeeDO().setName("审批集成员工").setJobNumber("APPROVAL-INTEGRATION");employeeMapper.insert(employee);
        when(employees.validateEmployeeExists(employee.getId())).thenReturn(employee);
        when(options.getSalaryOptionList(true)).thenReturn(Collections.singletonList(HrmSalaryOptionDO.builder().code(10101).parentCode(10).name("基本工资").type(1).calculateEnabled(true).enabled(true).build()));
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
}
