package cn.iocoder.yudao.module.bpm.service.message;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.web.config.WebProperties;
import cn.iocoder.yudao.module.bpm.service.message.dto.*;
import cn.iocoder.yudao.module.system.api.sms.SmsSendApi;
import cn.iocoder.yudao.module.system.api.sms.dto.send.SmsSendSingleToUserReqDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BpmMessageServiceImplTest extends BaseMockitoUnitTest {
    @InjectMocks private BpmMessageServiceImpl service;
    @Mock private SmsSendApi sms;
    @Mock private AdminUserApi users;
    @Mock private WebProperties properties;

    @BeforeEach
    void configureUi() {
        WebProperties.Ui ui = new WebProperties.Ui();
        ui.setUrl("http://localhost:4173");
        when(properties.getAdminUi()).thenReturn(ui);
    }

    private void sendAll() {
        service.sendMessageWhenProcessInstanceApprove(new BpmMessageSendWhenProcessInstanceApproveReqDTO()
                .setStartUserId(20L).setProcessInstanceId("synthetic").setProcessInstanceName("隔离加班"));
        service.sendMessageWhenProcessInstanceReject(new BpmMessageSendWhenProcessInstanceRejectReqDTO()
                .setStartUserId(20L).setProcessInstanceId("synthetic").setProcessInstanceName("隔离加班").setReason("退回"));
        service.sendMessageWhenTaskAssigned(new BpmMessageSendWhenTaskCreatedReqDTO()
                .setAssigneeUserId(20L).setProcessInstanceId("synthetic").setProcessInstanceName("隔离加班")
                .setTaskId("task").setTaskName("复核").setStartUserNickname("申请人"));
        service.sendMessageWhenTaskTimeout(new BpmMessageSendWhenTaskTimeoutReqDTO()
                .setAssigneeUserId(20L).setProcessInstanceId("synthetic").setProcessInstanceName("隔离加班")
                .setTaskId("task").setTaskName("复核"));
    }

    @Test
    void missingMobileDoesNotBlockAnyWorkflowMessage() {
        when(users.getUser(20L)).thenReturn(new AdminUserRespDTO().setId(20L));
        assertDoesNotThrow(this::sendAll);
        verifyNoInteractions(sms);
    }

    @Test
    void blankMobileDoesNotAttemptSms() {
        when(users.getUser(20L)).thenReturn(new AdminUserRespDTO().setId(20L).setMobile(" "));
        assertDoesNotThrow(this::sendAll);
        verifyNoInteractions(sms);
    }

    @Test
    void deletedUserDoesNotBlockApprovalResult() {
        assertDoesNotThrow(this::sendAll);
        verifyNoInteractions(sms);
    }

    @Test
    void configuredMobilePreservesAllFourNotificationsAndDetailLinks() {
        when(users.getUser(20L)).thenReturn(new AdminUserRespDTO().setId(20L).setMobile("13800000000"));
        sendAll();
        ArgumentCaptor<SmsSendSingleToUserReqDTO> sent = ArgumentCaptor.forClass(SmsSendSingleToUserReqDTO.class);
        verify(sms, times(4)).sendSingleSmsToAdmin(sent.capture());
        sent.getAllValues().forEach(dto -> {
            assertEquals(20L, dto.getUserId());
            assertEquals("http://localhost:4173/bpm/process-instance/detail?id=synthetic", dto.getTemplateParams().get("detailUrl"));
        });
        assertEquals(4, sent.getAllValues().stream().map(SmsSendSingleToUserReqDTO::getTemplateCode).distinct().count());
    }
}
