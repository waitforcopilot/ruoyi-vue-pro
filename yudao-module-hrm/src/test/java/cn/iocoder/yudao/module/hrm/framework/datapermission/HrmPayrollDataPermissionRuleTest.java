package cn.iocoder.yudao.module.hrm.framework.datapermission;

import cn.iocoder.yudao.framework.common.biz.system.permission.PermissionCommonApi;
import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.datapermission.config.YudaoDataPermissionAutoConfiguration;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.HrmPayrollInsuranceBaseDO;
import cn.iocoder.yudao.module.hrm.dal.mysql.employee.info.HrmEmployeeMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.HrmPayrollInsuranceBaseMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.salary.monthrecord.HrmSalaryMonthRecordMapper;
import javax.annotation.Resource;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Import({HrmPayrollDataPermissionRule.class, YudaoDataPermissionAutoConfiguration.class})
class HrmPayrollDataPermissionRuleTest extends BaseDbUnitTest {
    @Resource private cn.iocoder.yudao.framework.datapermission.core.db.DataPermissionRuleHandler handler;
    @Resource private HrmEmployeeMapper employees;
    @Resource private HrmPayrollInsuranceBaseMapper bases;
    @Resource private HrmSalaryMonthRecordMapper batches;
    @Resource private javax.sql.DataSource dataSource;
    private JdbcTemplate jdbc;
    @MockBean private PermissionCommonApi permissions;
    private Long own, other, ownBase, otherBase;

    @BeforeEach
    void fixtures() {
        jdbc = new JdbcTemplate(dataSource);
        TenantContextHolder.setTenantId(1L);
        HrmEmployeeDO a = new HrmEmployeeDO().setName("scope103").setDeptId(103L).setUserId(76L);
        HrmEmployeeDO b = new HrmEmployeeDO().setName("scope104").setDeptId(104L).setUserId(77L);
        employees.insert(a); employees.insert(b); own=a.getId(); other=b.getId();
        HrmPayrollInsuranceBaseDO first = new HrmPayrollInsuranceBaseDO().setEntityId(1L).setEmployeeId(own);
        HrmPayrollInsuranceBaseDO second = new HrmPayrollInsuranceBaseDO().setEntityId(1L).setEmployeeId(other);
        bases.insert(first); bases.insert(second); ownBase=first.getId(); otherBase=second.getId();
        jdbc.update("UPDATE hrm_employee SET tenant_id=1");
        jdbc.update("UPDATE hrm_payroll_insurance_base SET tenant_id=1");
    }

    @AfterEach
    void clear() { SecurityContextHolder.clearContext(); TenantContextHolder.clear(); }

    private void scope(DeptDataPermissionRespDTO scope) {
        when(permissions.getDeptDataPermission(77L)).thenReturn(scope);
        SecurityFrameworkUtils.setLoginUser(new LoginUser().setId(77L).setUserType(2), new MockHttpServletRequest());
    }

    @Test
    void departmentFiltersCandidatesDetailsAndWrites() {
        scope(new DeptDataPermissionRespDTO().setDeptIds(new HashSet<>(Collections.singletonList(103L))));
        assertEquals(Collections.singletonList(own), ids());
        assertEquals(1, bases.selectList().size());
        assertNotNull(bases.selectById(ownBase));
        assertNull(bases.selectById(otherBase));
        assertNull(employees.selectOne(HrmEmployeeDO::getId, other));
        assertNull(employees.selectOneForUpdate(HrmEmployeeDO::getId, other));
        assertEquals(0, bases.updateById(new HrmPayrollInsuranceBaseDO().setId(otherBase).setEvidence("blocked")));
        assertEquals(0, bases.deleteById(otherBase));
    }

    @Test
    void selfScopeUsesBoundUserRatherThanDepartment() {
        scope(new DeptDataPermissionRespDTO().setSelf(true));
        assertEquals(Collections.singletonList(other), ids());
        assertNotNull(bases.selectById(otherBase));
        assertNull(bases.selectById(ownBase));
    }

    @Test
    void noDepartmentOrSelfMeansNoEmployeeAccess() {
        scope(new DeptDataPermissionRespDTO());
        assertTrue(ids().isEmpty());
        assertTrue(bases.selectList().isEmpty());
    }

    @Test
    void allScopeKeepsBothEmployees() {
        scope(new DeptDataPermissionRespDTO().setAll(true));
        assertEquals(2, ids().size());
        assertEquals(2, bases.selectList().size());
    }

    @Test
    void mixedBatchAggregateIsHidden() {
        jdbc.update("INSERT INTO hrm_salary_month_record(id,entity_id,title,year,month,start_time,end_time,tenant_id) VALUES(901,1,'mixed',2026,10,'2026-10-01','2026-10-31',1)");
        jdbc.update("INSERT INTO hrm_salary_month_employee_record(month_record_id,employee_id,year,month,tenant_id) VALUES(901,?,2026,10,1)", other);
        jdbc.update("INSERT INTO hrm_salary_month_employee_record(month_record_id,employee_id,year,month,tenant_id) VALUES(901,?,2026,10,1)", own);
        jdbc.update("INSERT INTO hrm_salary_month_record(id,entity_id,title,year,month,start_time,end_time,tenant_id) VALUES(902,1,'own',2026,10,'2026-10-01','2026-10-31',1),(903,1,'unknown',2026,10,'2026-10-01','2026-10-31',1)");
        jdbc.update("INSERT INTO hrm_salary_month_employee_record(month_record_id,employee_id,year,month,tenant_id) VALUES(902,?,2026,10,1)", own);
        scope(new DeptDataPermissionRespDTO().setDeptIds(new HashSet<>(Collections.singletonList(103L))));
        assertNull(batches.selectById(901L));
        assertNotNull(batches.selectById(902L));
        assertNull(batches.selectById(903L));
    }

    private List<Long> ids() {
        List<Long> result = new ArrayList<>();
        employees.selectList().forEach(e -> result.add(e.getId()));
        return result;
    }
}
