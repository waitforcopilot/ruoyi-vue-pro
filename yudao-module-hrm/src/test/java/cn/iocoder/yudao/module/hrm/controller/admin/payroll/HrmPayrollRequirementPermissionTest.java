package cn.iocoder.yudao.module.hrm.controller.admin.payroll;

import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollRequirementConfirmReqVO;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollRequirementSaveReqVO;
import cn.iocoder.yudao.module.hrm.service.payroll.HrmPayrollRequirementService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import javax.annotation.Resource;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringJUnitConfig(HrmPayrollRequirementPermissionTest.Config.class)
class HrmPayrollRequirementPermissionTest {
    private static final String PREFIX = "hrm:payroll:requirement:";
    @Resource private HrmPayrollRequirementController controller;
    @Resource private HrmPayrollRequirementService service;
    @Resource private Permissions ss;

    @BeforeEach void setup() {
        reset(service);
        ss.granted.clear();
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("tester", "unused", "ROLE_TEST"));
    }
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }
    private HrmPayrollRequirementConfirmReqVO request() {
        HrmPayrollRequirementConfirmReqVO r = new HrmPayrollRequirementConfirmReqVO();
        r.setId(47L); r.setVersion(15); return r;
    }
    private void permissions(String... actions) {
        Arrays.stream(actions).forEach(action -> ss.granted.add(PREFIX + action));
    }
    @Test void editorCannotConfirmEvenWithReadPermission() {
        permissions("query", "update");
        assertThrows(AccessDeniedException.class, () -> controller.confirm(request()));
        verifyNoInteractions(service);
    }
    @Test void confirmationRequiresReadPermissionToo() {
        permissions("confirm");
        assertThrows(AccessDeniedException.class, () -> controller.confirm(request()));
        verifyNoInteractions(service);
    }
    @Test void reviewerCanConfirmWithoutEditPermission() {
        permissions("query", "confirm");
        assertTrue(controller.confirm(request()).getData());
        verify(service).confirm(47L, 15, null);
    }
    @Test void editorRetainsSavePermission() {
        permissions("query", "update");
        HrmPayrollRequirementSaveReqVO request = new HrmPayrollRequirementSaveReqVO();
        controller.save(request);
        verify(service).save(request, null);
    }
    @Test void confirmationDoesNotGrantEditing() {
        permissions("query", "confirm");
        assertThrows(AccessDeniedException.class, () -> controller.save(new HrmPayrollRequirementSaveReqVO()));
        verifyNoInteractions(service);
    }
    private cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollRequirementAssignReqVO assignment() {
        cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollRequirementAssignReqVO r=new cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollRequirementAssignReqVO();
        r.setId(47L);r.setVersion(23);r.setReviewerId(249L);return r;
    }
    @Test void assignmentDoesNotRequireEditPermission() {
        permissions("query","assign");assertTrue(controller.assign(assignment()).getData());verify(service).assign(47L,23,249L,null);
    }
    @Test void editingDoesNotGrantAssignment() {
        permissions("query","update");assertThrows(AccessDeniedException.class,()->controller.assign(assignment()));verifyNoInteractions(service);
    }
    @Test void assignmentRequiresReadPermission() {
        permissions("assign");assertThrows(AccessDeniedException.class,()->controller.assign(assignment()));verifyNoInteractions(service);
    }
    @Test void assignmentDoesNotGrantContentEditing() {
        permissions("query","assign");assertThrows(AccessDeniedException.class,()->controller.save(new HrmPayrollRequirementSaveReqVO()));verifyNoInteractions(service);
    }
    public static class Permissions {
        final Set<String> granted = new HashSet<>();
        public boolean hasPermission(String permission) { return granted.contains(permission); }
    }
    @Configuration
    @EnableGlobalMethodSecurity(prePostEnabled = true, proxyTargetClass = true)
    static class Config {
        @Bean HrmPayrollRequirementController controller() { return new HrmPayrollRequirementController(); }
        @Bean HrmPayrollRequirementService service() { return mock(HrmPayrollRequirementService.class); }
        @Bean cn.iocoder.yudao.module.hrm.service.payroll.HrmPayrollRequirementProjectService projectServiceTestDouble(){return mock(cn.iocoder.yudao.module.hrm.service.payroll.HrmPayrollRequirementProjectService.class);}
        @Bean cn.iocoder.yudao.module.hrm.dal.mysql.payroll.HrmPayrollRequirementMapper requirements(){return mock(cn.iocoder.yudao.module.hrm.dal.mysql.payroll.HrmPayrollRequirementMapper.class);}
        @Bean cn.iocoder.yudao.module.hrm.dal.mysql.payroll.HrmPayrollRequirementProjectMapper dependency0(){return mock(cn.iocoder.yudao.module.hrm.dal.mysql.payroll.HrmPayrollRequirementProjectMapper.class);}
        @Bean cn.iocoder.yudao.module.hrm.dal.mysql.payroll.HrmPayrollRequirementProjectGrantMapper dependency1(){return mock(cn.iocoder.yudao.module.hrm.dal.mysql.payroll.HrmPayrollRequirementProjectGrantMapper.class);}
        @Bean cn.iocoder.yudao.module.hrm.dal.mysql.payroll.HrmPayrollRequirementProjectEventMapper dependency2(){return mock(cn.iocoder.yudao.module.hrm.dal.mysql.payroll.HrmPayrollRequirementProjectEventMapper.class);}
        @Bean cn.iocoder.yudao.module.hrm.dal.mysql.payroll.HrmPayrollRequirementTemplateMapper dependency3(){return mock(cn.iocoder.yudao.module.hrm.dal.mysql.payroll.HrmPayrollRequirementTemplateMapper.class);}
        @Bean cn.iocoder.yudao.module.hrm.dal.mysql.payroll.HrmPayrollRequirementHistoryMapper dependency4(){return mock(cn.iocoder.yudao.module.hrm.dal.mysql.payroll.HrmPayrollRequirementHistoryMapper.class);}
        @Bean cn.iocoder.yudao.module.system.api.user.AdminUserApi dependency5(){return mock(cn.iocoder.yudao.module.system.api.user.AdminUserApi.class);}
        @Bean cn.iocoder.yudao.module.hrm.service.payroll.HrmPayrollRequirementAccessService access(){return mock(cn.iocoder.yudao.module.hrm.service.payroll.HrmPayrollRequirementAccessService.class);}
        @Bean cn.iocoder.yudao.module.system.api.permission.PermissionApi permissionApi(){return mock(cn.iocoder.yudao.module.system.api.permission.PermissionApi.class);}
        @Bean(name = "ss") Permissions permissions() { return new Permissions(); }
    }
}
