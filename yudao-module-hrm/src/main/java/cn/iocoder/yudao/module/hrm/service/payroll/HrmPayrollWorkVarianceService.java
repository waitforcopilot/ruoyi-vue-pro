package cn.iocoder.yudao.module.hrm.service.payroll;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollWorkVarianceReqVO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.employee.info.HrmEmployeeMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.*;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import java.time.*;
import java.util.*;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class HrmPayrollWorkVarianceService {
  @Resource private HrmPayrollWorkVarianceMapper cases;
  @Resource private HrmPayrollWorkVarianceEventMapper events;
  @Resource private HrmPayrollWorkService work;
  @Resource private HrmPayrollEntityService entities;
  @Resource private HrmEmployeeMapper employees;
  @Resource private AdminUserApi users;
  @Resource private cn.iocoder.yudao.module.system.api.notify.NotifyMessageSendApi notifications;
  @Resource private javax.validation.Validator validator;

  private LocalDateTime now() {
    return LocalDateTime.now(ZoneId.of("Asia/Shanghai"));
  }

  private void text(String reason) {
    if (reason == null || reason.trim().isEmpty() || reason.length() > 1000)
      throw exception(PAYROLL_WORK_INVALID);
  }

  private void event(HrmPayrollWorkVarianceDO row, String action, String reason, Long actor) {
    events.insert(
        new HrmPayrollWorkVarianceEventDO()
            .setVarianceId(row.getId())
            .setAction(action)
            .setActorId(actor)
            .setReason(reason)
            .setSnapshot(JsonUtils.toJsonString(row)));
  }

  private HrmEmployeeDO employee(Long id) {
    HrmEmployeeDO employee = employees.selectByIdForUpdate(id);
    if (employee == null) throw exception(PAYROLL_WORK_INVALID);
    return employee;
  }

  private HrmPayrollWorkVarianceDO locked(Long id, Long actor, String action) {
    HrmPayrollWorkVarianceDO row = cases.selectById(id);
    if (row == null) throw exception(PAYROLL_WORK_INVALID);
    entities.check(row.getEntityId(), action, actor);
    employee(row.getEmployeeId());
    return cases.selectOneForUpdate(HrmPayrollWorkVarianceDO::getId, id);
  }

  private Map<String, Object> capture(HrmPayrollWorkVarianceDO row, Long actor) {
    return work.reconcileDay(row.getEntityId(), row.getEmployeeId(), row.getWorkDate(), actor);
  }

  @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
  public Long create(HrmPayrollWorkVarianceReqVO request, Long actor) {
    if (actor == null
        || !validator.validate(request).isEmpty()
        || !request.getDueTime().isAfter(now())) throw exception(PAYROLL_WORK_INVALID);
    entities.check(request.getEntityId(), "MANAGE", actor);
    HrmEmployeeDO employee = employee(request.getEmployeeId());
    if (Objects.equals(request.getOwnerId(), request.getReviewerId())
        || Objects.equals(request.getReviewerId(), actor)
        || Objects.equals(request.getReviewerId(), employee.getUserId()))
      throw exception(PAYROLL_SELF_APPROVAL);
    users.validateUserList(Arrays.asList(request.getOwnerId(), request.getReviewerId()));
    entities.check(request.getEntityId(), "MANAGE", request.getOwnerId());
    entities.check(request.getEntityId(), "REVIEW", request.getReviewerId());
    HrmPayrollWorkVarianceDO row = BeanUtils.toBean(request, HrmPayrollWorkVarianceDO.class);
    HrmPayrollWorkVarianceDO old =
        cases.selectOne(HrmPayrollWorkVarianceDO::getRequestKey, request.getRequestKey());
    if (old != null) {
      if (!Objects.equals(old.getSubmittedBy(), actor)
          || !Objects.equals(old.getEntityId(), row.getEntityId())
          || !Objects.equals(old.getEmployeeId(), row.getEmployeeId())
          || !Objects.equals(old.getWorkDate(), row.getWorkDate())
          || !Objects.equals(old.getOwnerId(), row.getOwnerId())
          || !Objects.equals(old.getReviewerId(), row.getReviewerId())
          || !Objects.equals(old.getReason(), row.getReason())
          || !Objects.equals(old.getDueTime(), row.getDueTime()))
        throw exception(PAYROLL_WORK_INVALID);
      return old.getId();
    }
    Map<String, Object> source = capture(row, actor);
    if (Boolean.TRUE.equals(source.get("ready"))
        && Objects.equals(source.get("differenceMinutes"), 0))
      throw exception(PAYROLL_WORK_INVALID);
    if (cases.selectCount(
            new LambdaQueryWrapperX<HrmPayrollWorkVarianceDO>()
                .eq(HrmPayrollWorkVarianceDO::getEntityId, row.getEntityId())
                .eq(HrmPayrollWorkVarianceDO::getEmployeeId, row.getEmployeeId())
                .eq(HrmPayrollWorkVarianceDO::getWorkDate, row.getWorkDate())
                .in(HrmPayrollWorkVarianceDO::getStatus, "OPEN", "SUBMITTED"))
        > 0) throw exception(PAYROLL_WORK_INVALID);
    row.setSourceSnapshot(JsonUtils.toJsonString(source));
    row.setStatus("OPEN");
    row.setSubmittedBy(actor);
    cases.insert(row);
    event(row, "CREATE", request.getReason(), actor);
    return row.getId();
  }

  @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
  public void submit(Long id, String evidence, Long actor) {
    text(evidence);
    HrmPayrollWorkVarianceDO row = locked(id, actor, "MANAGE");
    if (!"OPEN".equals(row.getStatus()) || !Objects.equals(actor, row.getOwnerId()))
      throw exception(PAYROLL_WORK_INVALID);
    row.setSubmittedSnapshot(JsonUtils.toJsonString(capture(row, actor)));
    row.setEvidence(evidence);
    row.setStatus("SUBMITTED");
    cases.updateById(row);
    event(row, "SUBMIT", evidence, actor);
  }

  @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
  public void review(Long id, boolean approved, String reason, Long actor) {
    text(reason);
    HrmPayrollWorkVarianceDO row = locked(id, actor, "REVIEW");
    if (!"SUBMITTED".equals(row.getStatus()) || !Objects.equals(actor, row.getReviewerId()))
      throw exception(PAYROLL_WORK_INVALID);
    HrmEmployeeDO employee = employees.selectById(row.getEmployeeId());
    if (Objects.equals(actor, row.getOwnerId())
        || Objects.equals(actor, row.getSubmittedBy())
        || Objects.equals(actor, employee.getUserId())) throw exception(PAYROLL_SELF_APPROVAL);
    if (approved) {
      Map<String, Object> current = capture(row, actor);
      if (!Boolean.TRUE.equals(current.get("ready"))
          || !Objects.equals(JsonUtils.toJsonString(current), row.getSubmittedSnapshot()))
        throw exception(PAYROLL_WORK_INVALID);
    }
    row.setReviewOpinion(reason);
    row.setStatus(approved ? "CLOSED" : "OPEN");
    if (approved) row.setClosedTime(now());
    cases.updateById(row);
    event(row, approved ? "CLOSE" : "REJECT", reason, actor);
  }

  @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
  public void reopen(Long id, String reason, Long actor) {
    text(reason);
    HrmPayrollWorkVarianceDO row = locked(id, actor, "REVIEW");
    if (!"CLOSED".equals(row.getStatus()) || !Objects.equals(actor, row.getReviewerId()))
      throw exception(PAYROLL_WORK_INVALID);
    if (cases.selectCount(
            new LambdaQueryWrapperX<HrmPayrollWorkVarianceDO>()
                .eq(HrmPayrollWorkVarianceDO::getEntityId, row.getEntityId())
                .eq(HrmPayrollWorkVarianceDO::getEmployeeId, row.getEmployeeId())
                .eq(HrmPayrollWorkVarianceDO::getWorkDate, row.getWorkDate())
                .in(HrmPayrollWorkVarianceDO::getStatus, "OPEN", "SUBMITTED"))
        > 0) throw exception(PAYROLL_WORK_INVALID);
    row.setStatus("OPEN");
    cases.updateById(row);
    event(row, "REOPEN", reason, actor);
  }

  @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
  public void reassign(
      cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollWorkVarianceAssignReqVO
          request,
      Long actor) {
    if (!validator.validate(request).isEmpty() || !request.getDueTime().isAfter(now()))
      throw exception(PAYROLL_WORK_INVALID);
    HrmPayrollWorkVarianceDO row = locked(request.getId(), actor, "MANAGE");
    if (!Arrays.asList("OPEN", "SUBMITTED").contains(row.getStatus()))
      throw exception(PAYROLL_WORK_INVALID);
    HrmEmployeeDO subject = employees.selectById(row.getEmployeeId());
    if (Objects.equals(request.getOwnerId(), request.getReviewerId())
        || Objects.equals(request.getReviewerId(), actor)
        || Objects.equals(request.getReviewerId(), row.getSubmittedBy())
        || Objects.equals(request.getReviewerId(), subject.getUserId()))
      throw exception(PAYROLL_SELF_APPROVAL);
    users.validateUserList(Arrays.asList(request.getOwnerId(), request.getReviewerId()));
    entities.check(row.getEntityId(), "MANAGE", request.getOwnerId());
    entities.check(row.getEntityId(), "REVIEW", request.getReviewerId());
    row.setOwnerId(request.getOwnerId());
    row.setReviewerId(request.getReviewerId());
    row.setDueTime(request.getDueTime());
    row.setStatus("OPEN");
    cases.updateById(row);
    event(row, "REASSIGN", request.getReason(), actor);
  }

  public List<Long> overdueIds() {
    List<Long> ids = new ArrayList<>();
    cases
        .selectList(
            new LambdaQueryWrapperX<HrmPayrollWorkVarianceDO>()
                .in(HrmPayrollWorkVarianceDO::getStatus, "OPEN", "SUBMITTED")
                .lt(HrmPayrollWorkVarianceDO::getDueTime, now()))
        .forEach(row -> ids.add(row.getId()));
    return ids;
  }

  @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
  public int remind(Long id) {
    HrmPayrollWorkVarianceDO initial = cases.selectById(id);
    if (initial == null) return 0;
    employee(initial.getEmployeeId());
    HrmPayrollWorkVarianceDO row = cases.selectOneForUpdate(HrmPayrollWorkVarianceDO::getId, id);
    if (!Arrays.asList("OPEN", "SUBMITTED").contains(row.getStatus())
        || !row.getDueTime().isBefore(now())) return 0;
    Map<String, Object> state =
        row.getReminderState() == null
            ? new LinkedHashMap<>()
            : JsonUtils.parseObject(row.getReminderState(), Map.class);
    String today = now().toLocalDate().toString();
    int sent = 0;
    List<String> errors = new ArrayList<>();
    for (Long recipient : new TreeSet<>(Arrays.asList(row.getOwnerId(), row.getReviewerId()))) {
      if (Objects.equals(state.get(recipient.toString()), today)) continue;
      try {
        users.validateUserList(Collections.singletonList(recipient));
        entities.check(row.getEntityId(), "READ", recipient);
        entities.check(
            row.getEntityId(),
            Objects.equals(recipient, row.getOwnerId()) ? "MANAGE" : "REVIEW",
            recipient);
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("caseId", row.getId());
        params.put("workDate", row.getWorkDate().toString());
        params.put("dueTime", row.getDueTime().toString());
        Long messageId =
            notifications.sendSingleMessageToAdmin(
                new cn.iocoder.yudao.module.system.api.notify.dto.NotifySendSingleToUserReqDTO()
                    .setUserId(recipient)
                    .setTemplateCode("hrm_work_variance_overdue")
                    .setTemplateParams(params));
        if (messageId == null) throw exception(PAYROLL_WORK_INVALID);
        state.put(recipient.toString(), today);
        sent++;
      } catch (RuntimeException failure) {
        errors.add("用户" + recipient + "：授权或站内信配置需检查");
      }
    }
    row.setReminderState(JsonUtils.toJsonString(state));
    row.setReminderError(String.join("；", errors));
    cases.updateById(row);
    if (sent > 0 || !errors.isEmpty())
      event(row, "REMIND", sent + "条已送达；" + row.getReminderError(), null);
    return sent;
  }

  public List<HrmPayrollWorkVarianceDO> list(Long entityId, Long employeeId, Long actor) {
    entities.check(entityId, "READ", actor);
    List<HrmPayrollWorkVarianceDO> rows =
        cases.selectList(
            new LambdaQueryWrapperX<HrmPayrollWorkVarianceDO>()
                .eq(HrmPayrollWorkVarianceDO::getEntityId, entityId)
                .eqIfPresent(HrmPayrollWorkVarianceDO::getEmployeeId, employeeId)
                .orderByDesc(HrmPayrollWorkVarianceDO::getId));
    for (HrmPayrollWorkVarianceDO row : rows) {
      row.setOverdue(!"CLOSED".equals(row.getStatus()) && row.getDueTime().isBefore(now()));
      try {
        Map<String, Object> current = capture(row, actor);
        row.setSourceReady(Boolean.TRUE.equals(current.get("ready")));
        row.setSourceFresh(
            Objects.equals(
                row.getSubmittedSnapshot() == null
                    ? row.getSourceSnapshot()
                    : row.getSubmittedSnapshot(),
                JsonUtils.toJsonString(current)));
      } catch (cn.iocoder.yudao.framework.common.exception.ServiceException invalid) {
        row.setSourceFresh(false);
        row.setSourceReady(false);
      }
    }
    return rows;
  }

  public List<HrmPayrollWorkVarianceEventDO> history(Long id, Long actor) {
    HrmPayrollWorkVarianceDO row = cases.selectById(id);
    if (row == null) throw exception(PAYROLL_WORK_INVALID);
    entities.check(row.getEntityId(), "READ", actor);
    return events.selectList(
        new LambdaQueryWrapperX<HrmPayrollWorkVarianceEventDO>()
            .eq(HrmPayrollWorkVarianceEventDO::getVarianceId, id)
            .orderByAsc(HrmPayrollWorkVarianceEventDO::getId));
  }
}
