package cn.iocoder.yudao.module.hrm.service.salary.slip;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.hrm.dal.dataobject.salary.slip.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.salary.monthrecord.HrmSalaryMonthRecordMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.salary.slip.*;
import cn.iocoder.yudao.module.hrm.service.payroll.HrmPayrollScopeService;
import cn.iocoder.yudao.module.system.api.mail.MailSendApi;
import cn.iocoder.yudao.module.system.api.notify.NotifyMessageSendApi;
import cn.iocoder.yudao.module.system.api.sms.SmsSendApi;
import java.time.LocalDateTime;
import java.util.*;
import javax.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;

@Import(HrmSalarySlipDeliveryService.class)
class HrmSalarySlipDeliveryServiceTest extends BaseDbUnitTest {
  @Resource private HrmSalarySlipDeliveryService service;
  @Resource private HrmSalarySlipDeliveryMapper deliveries;
  @Resource private HrmSalarySlipDeliveryAttemptMapper attempts;
  @Resource private HrmSalarySlipSendRecordMapper publications;
  @Resource private HrmSalarySlipMapper slips;
  @MockBean private HrmSalaryMonthRecordMapper batches;
  @MockBean private HrmPayrollScopeService scope;
  @MockBean private NotifyMessageSendApi notify;
  @MockBean private MailSendApi mail;
  @MockBean private SmsSendApi sms;
  private HrmSalarySlipSendRecordDO publication;
  private HrmSalarySlipDO slip;

  @BeforeEach
  void setup() {
    ReflectionTestUtils.setField(
        service, "portalUrl", "https://payroll.example/hrm/portal/salary/slip");
    publication =
        new HrmSalarySlipSendRecordDO()
            .setMonthRecordId(99L)
            .setYear(2026)
            .setMonth(10)
            .setWithdrawn(false)
            .setEmployeeCount(1)
            .setSendEmployeeCount(1);
    publications.insert(publication);
    slip =
        new HrmSalarySlipDO()
            .setSendRecordId(publication.getId())
            .setMonthEmployeeRecordId(44L)
            .setEmployeeId(12L)
            .setYear(2026)
            .setMonth(10)
            .setOptions(Collections.emptyList())
            .setRemark("")
            .setReadStatus(0)
            .setRealPaySalary(new java.math.BigDecimal("9000"));
    slips.insert(slip);
  }

  private HrmSalarySlipDeliveryDO enqueue(String channel) {
    service.enqueue(slip, 123L, Collections.singletonList(channel));
    return deliveries.selectList().get(0);
  }

  @Test
  void enqueueIsDurableDoesNotSend() {
    service.enqueue(slip, 123L, Arrays.asList("STATION", "EMAIL", "SMS"));
    assertEquals(3, deliveries.selectList().size());
    verifyNoInteractions(notify, mail, sms);
    assertEquals(3, service.pending().size());
  }

  @Test
  void claimOnceAndNotificationContainsNoWage() {
    HrmSalarySlipDeliveryDO row = enqueue("STATION");
    when(notify.sendSingleMessageToAdmin(any())).thenReturn(51L);
    HrmSalarySlipDeliveryDO claimed = service.claim(row.getId());
    assertNotNull(claimed);
    assertNull(service.claim(row.getId()));
    service.dispatch(claimed);
    assertEquals("DELIVERED", deliveries.selectById(row.getId()).getStatus());
    verify(notify)
        .sendSingleMessageToAdmin(
            argThat(
                req ->
                    req.getUserId().equals(123L)
                        && req.getTemplateParams().size() == 3
                        && !req.getTemplateParams().containsKey("realSalary")
                        && req.getTemplateParams().get("year").equals(2026)));
    assertEquals(51L, attempts.selectList().get(0).getProviderLogId());
  }

  @Test
  void emailWaitsForGatewayFailureThenRetryRetainsHistory() {
    HrmSalarySlipDeliveryDO row = enqueue("EMAIL");
    when(mail.sendSingleMailToAdmin(any())).thenReturn(81L, 82L);
    service.dispatch(service.claim(row.getId()));
    assertEquals("WAITING", deliveries.selectById(row.getId()).getStatus());
    when(mail.getSendStatus(81L)).thenReturn(20);
    service.poll(row.getId());
    assertEquals("FAILED", deliveries.selectById(row.getId()).getStatus());
    service.retry(row.getId());
    service.dispatch(service.claim(row.getId()));
    when(mail.getSendStatus(82L)).thenReturn(10);
    service.poll(row.getId());
    assertEquals("DELIVERED", deliveries.selectById(row.getId()).getStatus());
    assertEquals(2, attempts.selectList().size());
    assertEquals(81L, attempts.selectList().get(0).getProviderLogId());
    verify(scope).require(99L, "EXPORT");
  }

  @Test
  void ignoredSmsIsFailedAndNeverReportedDelivered() {
    HrmSalarySlipDeliveryDO row = enqueue("SMS");
    when(sms.sendSingleSmsToAdmin(any())).thenReturn(91L);
    service.dispatch(service.claim(row.getId()));
    when(sms.getSendStatus(91L)).thenReturn(30);
    service.poll(row.getId());
    assertEquals("FAILED", deliveries.selectById(row.getId()).getStatus());
  }

  @Test
  void unknownSendDoesNotAllowBlindRetry() {
    HrmSalarySlipDeliveryDO row = enqueue("EMAIL");
    when(mail.sendSingleMailToAdmin(any()))
        .thenThrow(new IllegalStateException("sensitive provider payload"));
    service.dispatch(service.claim(row.getId()));
    assertEquals("UNKNOWN", deliveries.selectById(row.getId()).getStatus());
    assertEquals("IllegalStateException", deliveries.selectById(row.getId()).getFailureCode());
    assertThrows(RuntimeException.class, () -> service.retry(row.getId()));
  }

  @Test
  void interruptedDispatchBecomesUnknownAndWillNotSendAgain() {
    HrmSalarySlipDeliveryDO row = enqueue("EMAIL");
    service.claim(row.getId());
    deliveries.updateById(
        new HrmSalarySlipDeliveryDO()
            .setId(row.getId())
            .setLastAttemptAt(LocalDateTime.now().minusMinutes(11)));
    service.poll(row.getId());
    assertEquals("UNKNOWN", deliveries.selectById(row.getId()).getStatus());
    assertNull(service.claim(row.getId()));
    verifyNoInteractions(mail);
  }

  @Test
  void withdrawnPublicationCancelsQueuedAndRevokesRetry() {
    HrmSalarySlipDeliveryDO row = enqueue("EMAIL");
    publications.updateById(
        new HrmSalarySlipSendRecordDO().setId(publication.getId()).setWithdrawn(true));
    assertNull(service.claim(row.getId()));
    assertEquals("CANCELLED", deliveries.selectById(row.getId()).getStatus());
    assertThrows(RuntimeException.class, () -> service.retry(row.getId()));
    verifyNoInteractions(mail);
  }

  @Test
  void withdrawAfterClaimDoesNotSend() {
    HrmSalarySlipDeliveryDO row = enqueue("EMAIL");
    HrmSalarySlipDeliveryDO claimed = service.claim(row.getId());
    publications.updateById(
        new HrmSalarySlipSendRecordDO().setId(publication.getId()).setWithdrawn(true));
    service.dispatch(claimed);
    verifyNoInteractions(mail);
    assertEquals("CANCELLED", deliveries.selectById(row.getId()).getStatus());
  }

  @Test
  void missingPortalUrlIsRetryableConfigurationFailure() {
    HrmSalarySlipDeliveryDO row = enqueue("EMAIL");
    ReflectionTestUtils.setField(service, "portalUrl", "");
    service.dispatch(service.claim(row.getId()));
    assertEquals("FAILED", deliveries.selectById(row.getId()).getStatus());
    verifyNoInteractions(mail);
  }

  @Test
  void cancellationCannotBeOverwrittenByLateGatewayResponse() {
    HrmSalarySlipDeliveryDO row = enqueue("EMAIL");
    service.claim(row.getId());
    service.cancel(publication.getId());
    service.finish(row.getId(), 88L, "WAITING", null, 1);
    assertEquals("CANCELLED", deliveries.selectById(row.getId()).getStatus());
  }

  @Test
  void scopeIsRequiredForListAndRetry() {
    HrmSalarySlipDeliveryDO row = enqueue("EMAIL");
    doThrow(new IllegalStateException()).when(scope).require(99L, "EXPORT");
    assertThrows(RuntimeException.class, () -> service.list(publication.getId()));
    deliveries.updateById(new HrmSalarySlipDeliveryDO().setId(row.getId()).setStatus("FAILED"));
    assertThrows(RuntimeException.class, () -> service.retry(row.getId()));
    assertEquals("FAILED", deliveries.selectById(row.getId()).getStatus());
  }

  @Test
  void queueUniquenessAndInputValidation() {
    enqueue("EMAIL");
    assertThrows(
        RuntimeException.class,
        () -> service.enqueue(slip, 123L, Collections.singletonList("EMAIL")));
    assertThrows(
        RuntimeException.class,
        () -> HrmSalarySlipDeliveryService.channels(Collections.emptyList()));
    assertThrows(
        RuntimeException.class,
        () -> HrmSalarySlipDeliveryService.channels(Arrays.asList("SMS", "SMS")));
    assertThrows(
        RuntimeException.class,
        () -> HrmSalarySlipDeliveryService.channels(Collections.singletonList("OTHER")));
  }

  @Test
  void missingTemplateIsRetryableAndDoesNotExposeMessage() {
    HrmSalarySlipDeliveryDO row = enqueue("EMAIL");
    when(mail.sendSingleMailToAdmin(any()))
        .thenThrow(
            cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception(
                cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.MAIL_TEMPLATE_NOT_EXISTS));
    service.dispatch(service.claim(row.getId()));
    assertEquals("FAILED", deliveries.selectById(row.getId()).getStatus());
  }

  @Test
  void stalePreviousAttemptCannotOverrideNewAttempt() {
    HrmSalarySlipDeliveryDO row = enqueue("EMAIL");
    service.claim(row.getId());
    service.finish(row.getId(), 44L, "FAILED", "PROVIDER_20", 1);
    service.retry(row.getId());
    service.claim(row.getId());
    service.finish(row.getId(), 44L, "DELIVERED", null, 1);
    HrmSalarySlipDeliveryDO current = deliveries.selectById(row.getId());
    assertEquals(2, current.getAttempts());
    assertEquals("SENDING", current.getStatus());
  }

  @Test
  void disabledStationTemplateCanBeRetriedAfterEnablement() {
    HrmSalarySlipDeliveryDO row = enqueue("STATION");
    when(notify.sendSingleMessageToAdmin(any())).thenReturn(null);
    service.dispatch(service.claim(row.getId()));
    assertEquals("FAILED", deliveries.selectById(row.getId()).getStatus());
  }
}
