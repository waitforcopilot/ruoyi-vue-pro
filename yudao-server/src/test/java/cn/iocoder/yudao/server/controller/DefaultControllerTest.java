package cn.iocoder.yudao.server.controller;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class DefaultControllerTest {
    @Test
    void activeHrmReportsUnknownRouteInsteadOfDisabledModule() {
        StaticApplicationContext context = new StaticApplicationContext();
        DefaultController controller = new DefaultController();
        ReflectionTestUtils.setField(controller, "applicationContext", context);
        context.getBeanFactory().registerSingleton("hrmSalaryOptionController", new Object());
        assertEquals(404, controller.hrm404().getCode());
        assertFalse(controller.hrm404().getMsg().contains("已禁用"));
        context.getBeanFactory().destroySingletons();
        assertEquals(501, controller.hrm404().getCode());
    }
}
