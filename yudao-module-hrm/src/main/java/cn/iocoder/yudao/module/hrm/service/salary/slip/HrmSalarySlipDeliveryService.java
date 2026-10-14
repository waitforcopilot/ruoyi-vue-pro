package cn.iocoder.yudao.module.hrm.service.salary.slip;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.PAYROLL_INPUT_INVALID;

import cn.iocoder.yudao.module.hrm.dal.dataobject.salary.slip.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.salary.monthrecord.HrmSalaryMonthRecordMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.salary.slip.*;
import cn.iocoder.yudao.module.hrm.service.payroll.HrmPayrollScopeService;
import cn.iocoder.yudao.module.system.api.mail.*;
import cn.iocoder.yudao.module.system.api.mail.dto.MailSendSingleToUserReqDTO;
import cn.iocoder.yudao.module.system.api.notify.*;
import cn.iocoder.yudao.module.system.api.notify.dto.NotifySendSingleToUserReqDTO;
import cn.iocoder.yudao.module.system.api.sms.*;
import cn.iocoder.yudao.module.system.api.sms.dto.send.SmsSendSingleToUserReqDTO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.LocalDateTime;
import java.util.*;
import javax.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HrmSalarySlipDeliveryService {
  @Resource private HrmSalarySlipDeliveryMapper deliveries;
  @Resource private HrmSalarySlipDeliveryAttemptMapper attempts;
  @Resource private HrmSalarySlipMapper slips;
  @Resource private HrmSalarySlipSendRecordMapper publications;
  @Resource private HrmSalaryMonthRecordMapper batches;
  @Resource private HrmPayrollScopeService scope;
  @Resource private NotifyMessageSendApi notify;
  @Resource private MailSendApi mail;
  @Resource private SmsSendApi sms;

  @Value("${hrm.salary-slip.portal-url:}")
  private String portalUrl;

  public static List<String> channels(List<String> requested) {
    List<String> values = requested == null ? Collections.singletonList("STATION") : requested;
    if (values.isEmpty()
        || values.size() > 3
        || new HashSet<>(values).size() != values.size()
        || !Arrays.asList("STATION", "EMAIL", "SMS").containsAll(values))
      throw exception(PAYROLL_INPUT_INVALID);
    return values;
  }

  @Transactional(rollbackFor = Exception.class)
  public void enqueue(HrmSalarySlipDO slip, Long userId, List<String> requested) {
    if (userId == null) throw exception(PAYROLL_INPUT_INVALID);
    for (String channel : channels(requested)) {
      HrmSalarySlipDeliveryDO row = new HrmSalarySlipDeliveryDO();
      row.setSlipId(slip.getId());
      row.setSendRecordId(slip.getSendRecordId());
      row.setUserId(userId);
      row.setNextCheckAt(LocalDateTime.now());
      row.setChannel(channel);
      row.setStatus("QUEUED");
      row.setAttempts(0);
      deliveries.insert(row);
    }
  }

  public List<Long> pending() {
    List<Long> result = new ArrayList<>();
    for (HrmSalarySlipDeliveryDO row :
        deliveries.selectList(
            new LambdaQueryWrapper<HrmSalarySlipDeliveryDO>()
                .in(
                    HrmSalarySlipDeliveryDO::getStatus,
                    Arrays.asList("QUEUED", "SENDING", "WAITING"))
                .le(HrmSalarySlipDeliveryDO::getNextCheckAt, LocalDateTime.now())
                .orderByAsc(HrmSalarySlipDeliveryDO::getNextCheckAt, HrmSalarySlipDeliveryDO::getId)
                .last("LIMIT 200"))) result.add(row.getId());
    return result;
  }

  private HrmSalarySlipSendRecordDO lockPublication(Long id) {
    HrmSalarySlipSendRecordDO p = publications.selectById(id);
    if (p == null) throw exception(PAYROLL_INPUT_INVALID);
    batches.selectByIdForUpdate(p.getMonthRecordId());
    return publications.selectById(id);
  }

  @Transactional(rollbackFor = Exception.class)
  public HrmSalarySlipDeliveryDO claim(Long id) {
    HrmSalarySlipDeliveryDO row = deliveries.selectById(id);
    if (row == null) return null;
    HrmSalarySlipSendRecordDO p = lockPublication(row.getSendRecordId());
    row = deliveries.lock(id);
    if (Boolean.TRUE.equals(p.getWithdrawn()) || slips.selectById(row.getSlipId()) == null) {
      row.setStatus("CANCELLED");
      deliveries.updateById(row);
      return null;
    }
    if (!"QUEUED".equals(row.getStatus())) return null;
    row.setNextCheckAt(LocalDateTime.now().plusMinutes(1));
    row.setStatus("SENDING");
    row.setAttempts(row.getAttempts() + 1);
    row.setLastAttemptAt(LocalDateTime.now());
    deliveries.updateById(row);
    HrmSalarySlipDeliveryAttemptDO attempt = new HrmSalarySlipDeliveryAttemptDO();
    attempt.setDeliveryId(row.getId());
    attempt.setAttemptNumber(row.getAttempts());
    attempt.setStatus("SENDING");
    attempt.setStartedAt(row.getLastAttemptAt());
    attempts.insert(attempt);
    return row;
  }

  // Called outside a database transaction: the committed SENDING state prevents duplicate dispatch.
  public void dispatch(HrmSalarySlipDeliveryDO row) {
    try {
      HrmSalarySlipSendRecordDO p = publications.selectById(row.getSendRecordId());
      if (p == null
          || Boolean.TRUE.equals(p.getWithdrawn())
          || slips.selectById(row.getSlipId()) == null) {
        finish(row.getId(), null, "CANCELLED", null, row.getAttempts());
        return;
      }
      if (portalUrl == null || !portalUrl.matches("https://[^\\s]+")) {
        finish(row.getId(), null, "FAILED", "PORTAL_URL_NOT_CONFIGURED", row.getAttempts());
        return;
      }
      Map<String, Object> params = new LinkedHashMap<>();
      params.put("year", p.getYear());
      params.put("month", p.getMonth());
      params.put("url", portalUrl);
      Long logId;
      if ("STATION".equals(row.getChannel()))
        logId =
            notify.sendSingleMessageToAdmin(
                new NotifySendSingleToUserReqDTO()
                    .setUserId(row.getUserId())
                    .setTemplateCode("hrm_salary_slip_delivery")
                    .setTemplateParams(params));
      else if ("EMAIL".equals(row.getChannel()))
        logId =
            mail.sendSingleMailToAdmin(
                new MailSendSingleToUserReqDTO()
                    .setUserId(row.getUserId())
                    .setTemplateCode("hrm_salary_slip_delivery")
                    .setTemplateParams(params));
      else
        logId =
            sms.sendSingleSmsToAdmin(
                new SmsSendSingleToUserReqDTO()
                    .setUserId(row.getUserId())
                    .setTemplateCode("hrm_salary_slip_delivery")
                    .setTemplateParams(params));
      finish(
          row.getId(),
          logId,
          logId == null
              ? ("STATION".equals(row.getChannel()) ? "FAILED" : "UNKNOWN")
              : ("STATION".equals(row.getChannel()) ? "DELIVERED" : "WAITING"),
          null,
          row.getAttempts());
    } catch (cn.iocoder.yudao.framework.common.exception.ServiceException failure) {
      java.util.Set<Integer> configurationErrors =
          new java.util.HashSet<>(
              java.util.Arrays.asList(
                  cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.MAIL_ACCOUNT_NOT_EXISTS
                      .getCode(),
                  cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.MAIL_TEMPLATE_NOT_EXISTS
                      .getCode(),
                  cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.MAIL_SEND_MAIL_NOT_EXISTS
                      .getCode(),
                  cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.NOTICE_NOT_FOUND
                      .getCode(),
                  cn.iocoder.yudao.module.system.enums.ErrorCodeConstants
                      .NOTIFY_SEND_TEMPLATE_PARAM_MISS
                      .getCode(),
                  cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.SMS_SEND_MOBILE_NOT_EXISTS
                      .getCode(),
                  cn.iocoder.yudao.module.system.enums.ErrorCodeConstants
                      .SMS_SEND_MOBILE_TEMPLATE_PARAM_MISS
                      .getCode(),
                  cn.iocoder.yudao.module.system.enums.ErrorCodeConstants
                      .SMS_SEND_TEMPLATE_NOT_EXISTS
                      .getCode()));
      finish(
          row.getId(),
          null,
          configurationErrors.contains(failure.getCode()) ? "FAILED" : "UNKNOWN",
          "BUSINESS_" + failure.getCode(),
          row.getAttempts());
    } catch (RuntimeException failure) {
      finish(row.getId(), null, "UNKNOWN", failure.getClass().getSimpleName(), row.getAttempts());
    }
  }

  public void cancel(Long publicationId) {
    deliveries.update(
        new HrmSalarySlipDeliveryDO().setStatus("CANCELLED"),
        new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<
                HrmSalarySlipDeliveryDO>()
            .eq(HrmSalarySlipDeliveryDO::getSendRecordId, publicationId)
            .notIn(HrmSalarySlipDeliveryDO::getStatus, Arrays.asList("DELIVERED", "CANCELLED")));
  }

  public List<HrmSalarySlipDeliveryAttemptDO> history(Long deliveryId) {
    HrmSalarySlipDeliveryDO row = deliveries.selectById(deliveryId);
    if (row == null) throw exception(PAYROLL_INPUT_INVALID);
    list(row.getSendRecordId());
    return attempts.selectList(
        new LambdaQueryWrapper<HrmSalarySlipDeliveryAttemptDO>()
            .eq(HrmSalarySlipDeliveryAttemptDO::getDeliveryId, deliveryId)
            .orderByAsc(HrmSalarySlipDeliveryAttemptDO::getAttemptNumber));
  }

  public void finish(Long id, Long logId, String status, String error, Integer attemptNumber) {
    deliveries.update(
        new HrmSalarySlipDeliveryDO()
            .setProviderLogId(logId)
            .setStatus(status),
        new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<
                HrmSalarySlipDeliveryDO>()
            .eq(HrmSalarySlipDeliveryDO::getId, id)
            .set(HrmSalarySlipDeliveryDO::getFailureCode, error)
            .eq(HrmSalarySlipDeliveryDO::getAttempts, attemptNumber)
            .in(
                HrmSalarySlipDeliveryDO::getStatus,
                Arrays.asList("SENDING", "WAITING", "UNKNOWN")));
    attempts.update(
        new HrmSalarySlipDeliveryAttemptDO()
            .setStatus(status)
            .setProviderLogId(logId)
            .setFailureCode(error),
        new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<
                HrmSalarySlipDeliveryAttemptDO>()
            .eq(HrmSalarySlipDeliveryAttemptDO::getDeliveryId, id)
            .eq(HrmSalarySlipDeliveryAttemptDO::getAttemptNumber, attemptNumber));
  }

  public void poll(Long id) {
    HrmSalarySlipDeliveryDO row = deliveries.selectById(id);
    if (row == null) return;
    deliveries.updateById(
        new HrmSalarySlipDeliveryDO().setId(id).setNextCheckAt(LocalDateTime.now().plusMinutes(1)));
    HrmSalarySlipSendRecordDO p = publications.selectById(row.getSendRecordId());
    if (p == null || Boolean.TRUE.equals(p.getWithdrawn())) {
      finish(id, row.getProviderLogId(), "CANCELLED", null, row.getAttempts());
      return;
    }
    if ("SENDING".equals(row.getStatus())
        && row.getLastAttemptAt().isBefore(LocalDateTime.now().minusMinutes(10))) {
      finish(id, null, "UNKNOWN", "DISPATCH_INTERRUPTED", row.getAttempts());
      return;
    }
    if (!"WAITING".equals(row.getStatus())) return;
    Integer result =
        "EMAIL".equals(row.getChannel())
            ? mail.getSendStatus(row.getProviderLogId())
            : sms.getSendStatus(row.getProviderLogId());
    if (Objects.equals(result, 10))
      finish(id, row.getProviderLogId(), "DELIVERED", null, row.getAttempts());
    else if (Objects.equals(result, 20) || Objects.equals(result, 30))
      finish(id, row.getProviderLogId(), "FAILED", "PROVIDER_" + result, row.getAttempts());
  }

  public List<HrmSalarySlipDeliveryDO> list(Long publicationId) {
    HrmSalarySlipSendRecordDO p = publications.selectById(publicationId);
    if (p == null) throw exception(PAYROLL_INPUT_INVALID);
    scope.require(p.getMonthRecordId(), "EXPORT");
    return deliveries.selectList(
        new LambdaQueryWrapper<HrmSalarySlipDeliveryDO>()
            .eq(HrmSalarySlipDeliveryDO::getSendRecordId, publicationId)
            .orderByAsc(HrmSalarySlipDeliveryDO::getId));
  }

  @Transactional(rollbackFor = Exception.class)
  public void retry(Long id) {
    HrmSalarySlipDeliveryDO row = deliveries.selectById(id);
    if (row == null) throw exception(PAYROLL_INPUT_INVALID);
    HrmSalarySlipSendRecordDO p = lockPublication(row.getSendRecordId());
    scope.require(p.getMonthRecordId(), "EXPORT");
    row = deliveries.lock(id);
    if (Boolean.TRUE.equals(p.getWithdrawn())
        || !"FAILED".equals(row.getStatus())
        || row.getAttempts() >= 5) throw exception(PAYROLL_INPUT_INVALID);
    // Previous provider results remain in the independent attempt history.
    deliveries.update(
        new HrmSalarySlipDeliveryDO().setStatus("QUEUED").setNextCheckAt(LocalDateTime.now()),
        new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<
                HrmSalarySlipDeliveryDO>()
            .eq(HrmSalarySlipDeliveryDO::getId, id)
            .set(HrmSalarySlipDeliveryDO::getProviderLogId, null)
            .set(HrmSalarySlipDeliveryDO::getFailureCode, null));
  }
}
