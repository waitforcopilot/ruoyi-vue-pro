package cn.iocoder.yudao.module.hrm.service.payroll;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.hrm.controller.admin.attendance.vo.clock.HrmAttendanceClockSaveReqVO;
import cn.iocoder.yudao.module.hrm.controller.admin.attendance.vo.statistics.*;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollAttendanceIssueReqVO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.attendance.record.HrmAttendanceClockDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.attendance.record.HrmAttendanceClockMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.employee.info.HrmEmployeeMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.*;
import cn.iocoder.yudao.module.hrm.service.attendance.config.HrmAttendanceGroupService;
import cn.iocoder.yudao.module.hrm.service.attendance.record.HrmAttendanceClockService;
import cn.iocoder.yudao.module.hrm.service.attendance.statistics.HrmAttendanceStatisticsService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import java.time.*;
import java.util.*;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HrmPayrollAttendanceIssueService {
  @Resource private HrmPayrollAttendanceIssueMapper mapper;
  @Resource private HrmPayrollAttendanceIssueEventMapper events;
  @Resource private HrmEmployeeMapper employees;
  @Resource private HrmAttendanceStatisticsService statistics;
  @Resource private HrmAttendanceGroupService groups;
  @Resource private HrmAttendanceClockService clockService;
  @Resource private HrmAttendanceClockMapper clocks;
  @Resource private AdminUserApi users;
  @Resource private javax.validation.Validator validator;

  private HrmEmployeeDO employee(Long id) {
    HrmEmployeeDO row = employees.selectOneForUpdate(HrmEmployeeDO::getId, id);
    if (row == null) throw exception(EMPLOYEE_NOT_EXISTS);
    return row;
  }

  private HrmAttendanceDailyDetailRespVO daily(HrmPayrollAttendanceIssueDO issue) {
    return statistics.getAttendanceDailyDetail(
        new HrmAttendanceDailyDetailReqVO()
            .setEmployeeId(issue.getEmployeeId())
            .setAttendanceTime(issue.getAttendanceDate().atStartOfDay()));
  }

  private boolean abnormal(HrmPayrollAttendanceIssueDO issue, HrmAttendanceDailyDetailRespVO day) {
    if (day == null || !Boolean.TRUE.equals(day.getScheduled())) return false;
    switch (issue.getType()) {
      case "MISSCARD":
        return day.getMisscardCount() != null
            && day.getMisscardCount() > 0
            && (day.getClockList() == null
                || day.getClockList().stream()
                    .noneMatch(
                        c ->
                            Objects.equals(c.getType(), issue.getClockType())
                                && c.getClockTime() != null));
      case "LATE":
        return day.getLateMinutes() != null && day.getLateMinutes() > 0;
      case "EARLY":
        return day.getEarlyMinutes() != null && day.getEarlyMinutes() > 0;
      case "ABSENT":
        return Boolean.TRUE.equals(day.getAbsenteeism());
      default:
        throw exception(PAYROLL_ATTENDANCE_ISSUE_INVALID);
    }
  }

  private void event(
      HrmPayrollAttendanceIssueDO issue,
      String action,
      String previous,
      String reason,
      Long actor) {
    HrmPayrollAttendanceIssueEventDO event = new HrmPayrollAttendanceIssueEventDO();
    event.setIssueId(issue.getId());
    event.setAction(action);
    event.setFromStatus(previous);
    event.setToStatus(issue.getStatus());
    event.setReason(reason);
    event.setActorId(actor);
    event.setSnapshot(JsonUtils.toJsonString(issue));
    events.insert(event);
  }

  public List<HrmPayrollAttendanceIssueDO> list(Long employeeId, String status) {
    return mapper.selectList(
        new LambdaQueryWrapperX<HrmPayrollAttendanceIssueDO>()
            .eqIfPresent(HrmPayrollAttendanceIssueDO::getEmployeeId, employeeId)
            .eqIfPresent(HrmPayrollAttendanceIssueDO::getStatus, status)
            .orderByDesc(HrmPayrollAttendanceIssueDO::getId));
  }

  private List<Long> myEmployeeIds(Long actor) {
    if (actor == null) throw exception(PAYROLL_ATTENDANCE_ISSUE_INVALID);
    List<Long> ids = new ArrayList<>();
    employees.selectList(HrmEmployeeDO::getUserId, actor).forEach(e -> ids.add(e.getId()));
    return ids;
  }

  public List<HrmPayrollAttendanceIssueDO> mine(Long actor) {
    List<Long> own = myEmployeeIds(actor);
    return mapper.selectList(
        new LambdaQueryWrapperX<HrmPayrollAttendanceIssueDO>()
            .and(
                w ->
                    w.eq(HrmPayrollAttendanceIssueDO::getOwnerId, actor)
                        .or()
                        .eq(HrmPayrollAttendanceIssueDO::getReviewerId, actor)
                        .or(
                            !own.isEmpty(),
                            z -> z.in(HrmPayrollAttendanceIssueDO::getEmployeeId, own)))
            .orderByAsc(HrmPayrollAttendanceIssueDO::getDueTime));
  }

  public List<HrmPayrollAttendanceIssueEventDO> history(Long id) {
    if (mapper.selectById(id) == null) throw exception(PAYROLL_ATTENDANCE_ISSUE_INVALID);
    return events.selectList(
        new LambdaQueryWrapperX<HrmPayrollAttendanceIssueEventDO>()
            .eq(HrmPayrollAttendanceIssueEventDO::getIssueId, id)
            .orderByAsc(HrmPayrollAttendanceIssueEventDO::getId));
  }

  @Transactional(rollbackFor = Exception.class)
  public Long create(HrmPayrollAttendanceIssueReqVO request, Long actor) {
    if (actor == null
        || !validator.validate(request).isEmpty()
        || request.getAttendanceDate().isAfter(LocalDate.now(ZoneId.of("Asia/Shanghai")))
        || Objects.equals(request.getOwnerId(), request.getReviewerId())
        || ("MISSCARD".equals(request.getType()) && request.getClockType() == null))
      throw exception(PAYROLL_ATTENDANCE_ISSUE_INVALID);
    HrmEmployeeDO subject=employee(request.getEmployeeId());
    if(Objects.equals(subject.getUserId(),request.getReviewerId()))throw exception(PAYROLL_SELF_APPROVAL);
    users.validateUserList(Arrays.asList(request.getOwnerId(), request.getReviewerId()));
    HrmPayrollAttendanceIssueDO existing =
        mapper.selectOne(HrmPayrollAttendanceIssueDO::getRequestKey, request.getRequestKey());
    if (existing != null) {
      if (!Objects.equals(existing.getEmployeeId(), request.getEmployeeId())
          || !Objects.equals(existing.getAttendanceDate(), request.getAttendanceDate())
          || !Objects.equals(existing.getType(), request.getType())
          || !Objects.equals(existing.getClockType(), request.getClockType())
          || !Objects.equals(existing.getOwnerId(), request.getOwnerId())
          || !Objects.equals(existing.getReviewerId(), request.getReviewerId()))
        throw exception(PAYROLL_ATTENDANCE_ISSUE_INVALID);
      return existing.getId();
    }
    HrmPayrollAttendanceIssueDO issue =
        BeanUtils.toBean(request, HrmPayrollAttendanceIssueDO.class);
    issue.setStatus("OPEN");
    HrmAttendanceDailyDetailRespVO day = daily(issue);
    // All-day absence can also be resolved through two punch correction work orders.
    boolean missing =
        "MISSCARD".equals(issue.getType())
            && day != null
            && Boolean.TRUE.equals(day.getAbsenteeism());
    if (!abnormal(issue, day) && !missing) throw exception(PAYROLL_ATTENDANCE_ISSUE_INVALID);
    issue.setInitialSnapshot(JsonUtils.toJsonString(day));
    mapper.insert(issue);
    event(issue, "CREATE", null, "分配异常处理责任人", actor);
    return issue.getId();
  }

  @Transactional(rollbackFor = Exception.class)
  public void submit(HrmPayrollAttendanceIssueReqVO.Submit request, Long actor) {
    HrmPayrollAttendanceIssueDO issue =
        mapper.selectOneForUpdate(HrmPayrollAttendanceIssueDO::getId, request.getId());
    if (actor == null || issue == null || !validator.validate(request).isEmpty())
      throw exception(PAYROLL_ATTENDANCE_ISSUE_INVALID);
    HrmEmployeeDO employee = employee(issue.getEmployeeId());
    if (!Objects.equals(actor, issue.getOwnerId()) && !Objects.equals(actor, employee.getUserId()))
      throw exception(PAYROLL_ATTENDANCE_ISSUE_INVALID);
    if (!"OPEN".equals(issue.getStatus())
        || ("MISSCARD".equals(issue.getType()) && request.getCorrectionTime() == null))
      throw exception(PAYROLL_ATTENDANCE_ISSUE_INVALID);
    if (request.getCorrectionTime() != null
        && (request.getCorrectionTime().isBefore(issue.getAttendanceDate().atStartOfDay())
            || !request
                .getCorrectionTime()
                .isBefore(issue.getAttendanceDate().plusDays(2).atStartOfDay())))
      throw exception(PAYROLL_ATTENDANCE_ISSUE_INVALID);
    issue.setEvidence(request.getEvidence());
    issue.setCorrectionTime(request.getCorrectionTime());
    issue.setSubmittedBy(actor);
    issue.setStatus("SUBMITTED");
    mapper.updateById(issue);
    event(issue, "SUBMIT", "OPEN", request.getEvidence(), actor);
  }

  @Transactional(rollbackFor = Exception.class)
  public void review(Long id, boolean approve, String reason, Long actor) {
    HrmPayrollAttendanceIssueDO issue =
        mapper.selectOneForUpdate(HrmPayrollAttendanceIssueDO::getId, id);
    if (issue == null
        || !"SUBMITTED".equals(issue.getStatus())
        || reason == null
        || reason.trim().isEmpty()
        || !Objects.equals(actor, issue.getReviewerId())
        || Objects.equals(actor, issue.getSubmittedBy())) throw exception(PAYROLL_ATTENDANCE_ISSUE_INVALID);
    HrmEmployeeDO employee = employee(issue.getEmployeeId());
    if (Objects.equals(actor, employee.getUserId())) throw exception(PAYROLL_SELF_APPROVAL);
    if (approve && "MISSCARD".equals(issue.getType())) {
      cn.iocoder.yudao.module.hrm.dal.dataobject.attendance.config.HrmAttendanceGroupDO.Shift
          shift =
              groups.getEmployeeAttendanceShift(
                  issue.getEmployeeId(), issue.getAttendanceDate().atStartOfDay());
      if (shift == null) throw exception(PAYROLL_ATTENDANCE_ISSUE_INVALID);
      LocalDateTime on = issue.getAttendanceDate().atTime(shift.getStartTime()),
          off = issue.getAttendanceDate().atTime(shift.getEndTime());
      if (!off.isAfter(on)) off = off.plusDays(1);
      HrmAttendanceClockSaveReqVO clock =
          new HrmAttendanceClockSaveReqVO()
              .setEmployeeId(issue.getEmployeeId())
              .setType(issue.getClockType())
              .setClockTime(issue.getCorrectionTime())
              .setAttendanceTime(issue.getClockType() == 1 ? on : off)
              .setRemark("异常工单批准补卡 #" + id);
      Long clockId = clockService.createAttendanceClock(clock);
      HrmAttendanceClockDO persisted = clocks.selectById(clockId);
      if (persisted == null) throw exception(PAYROLL_ATTENDANCE_ISSUE_INVALID);
      persisted.setSourceType(3);
      clocks.updateById(persisted);
      issue.setCorrectionClockId(clockId);
    }
    issue.setReviewOpinion(reason);
    issue.setStatus(approve ? "APPROVED" : "OPEN");
    mapper.updateById(issue);
    event(issue, approve ? "APPROVE" : "REJECT", "SUBMITTED", reason, actor);
  }

  @Transactional(rollbackFor = Exception.class)
  public void close(Long id, String reason, Long actor) {
    HrmPayrollAttendanceIssueDO issue =
        mapper.selectOneForUpdate(HrmPayrollAttendanceIssueDO::getId, id);
    if (issue == null
        || !"APPROVED".equals(issue.getStatus())
        || reason == null
        || reason.trim().isEmpty()
        || !Objects.equals(actor, issue.getReviewerId())) throw exception(PAYROLL_ATTENDANCE_ISSUE_INVALID);
    employee(issue.getEmployeeId());
    HrmAttendanceDailyDetailRespVO day = daily(issue);
    if (day == null || !Boolean.TRUE.equals(day.getScheduled()))
      throw exception(PAYROLL_ATTENDANCE_ISSUE_INVALID);
    if ("MISSCARD".equals(issue.getType())) {
      if (day == null
          || day.getClockList() == null
          || day.getClockList().stream()
              .noneMatch(
                  c ->
                      Objects.equals(c.getType(), issue.getClockType())
                          && c.getClockTime() != null)) throw exception(PAYROLL_ATTENDANCE_ISSUE_INVALID);
    } else if (abnormal(issue, day)) throw exception(PAYROLL_ATTENDANCE_ISSUE_INVALID);
    issue.setStatus("CLOSED");
    issue.setClosedTime(LocalDateTime.now(ZoneId.of("Asia/Shanghai")));
    mapper.updateById(issue);
    event(issue, "CLOSE", "APPROVED", reason, actor);
  }

  public List<HrmPayrollAttendanceIssueDO> overdue(Long actor) {
    List<Long> own = myEmployeeIds(actor);
    return mapper.selectList(
        new LambdaQueryWrapperX<HrmPayrollAttendanceIssueDO>()
            .and(
                w ->
                    w.eq(HrmPayrollAttendanceIssueDO::getOwnerId, actor)
                        .or()
                        .eq(HrmPayrollAttendanceIssueDO::getReviewerId, actor)
                        .or(
                            !own.isEmpty(),
                            z -> z.in(HrmPayrollAttendanceIssueDO::getEmployeeId, own)))
            .ne(HrmPayrollAttendanceIssueDO::getStatus, "CLOSED")
            .lt(
                HrmPayrollAttendanceIssueDO::getDueTime,
                LocalDateTime.now(ZoneId.of("Asia/Shanghai")))
            .orderByAsc(HrmPayrollAttendanceIssueDO::getDueTime));
  }

  public List<HrmPayrollAttendanceIssueEventDO> myHistory(Long id, Long actor) {
    HrmPayrollAttendanceIssueDO issue = mapper.selectById(id);
    if (issue == null) throw exception(PAYROLL_ATTENDANCE_ISSUE_INVALID);
    HrmEmployeeDO employee = employees.selectById(issue.getEmployeeId());
    if (actor == null
        || (!Objects.equals(actor, issue.getOwnerId())
            && !Objects.equals(actor, issue.getReviewerId())
            && (employee == null || !Objects.equals(actor, employee.getUserId()))))
      throw exception(PAYROLL_ATTENDANCE_ISSUE_INVALID);
    return history(id);
  }

  public List<Map<String, Object>> users() {
    List<Map<String, Object>> result = new ArrayList<>();
    for (cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO user :
        users.getUserListByStatus(0)) {
      Map<String, Object> item = new LinkedHashMap<>();
      item.put("id", user.getId());
      item.put("name", user.getNickname());
      result.add(item);
    }
    return result;
  }

  @Transactional(rollbackFor = Exception.class)
  public void reopen(Long id, String reason, Long actor) {
    HrmPayrollAttendanceIssueDO issue =
        mapper.selectOneForUpdate(HrmPayrollAttendanceIssueDO::getId, id);
    if (issue == null
        || (!"APPROVED".equals(issue.getStatus()) && !"CLOSED".equals(issue.getStatus()))
        || !Objects.equals(actor, issue.getReviewerId())
        || reason == null
        || reason.trim().isEmpty()) throw exception(PAYROLL_ATTENDANCE_ISSUE_INVALID);
    employee(issue.getEmployeeId());
    String previous = issue.getStatus();
    if (issue.getCorrectionClockId() != null) {
      HrmAttendanceClockDO clock =
          clocks.selectOneForUpdate(HrmAttendanceClockDO::getId, issue.getCorrectionClockId());
      if (clock == null || !Objects.equals(clock.getSourceType(), 3))
        throw exception(PAYROLL_ATTENDANCE_ISSUE_INVALID);
      clock.setStatus(3);
      clocks.updateById(clock);
    }
    issue.setStatus("OPEN");
    issue.setReviewOpinion(reason);
    mapper.updateById(issue);
    event(issue, "REOPEN", previous, reason, actor);
  }

  public List<HrmPayrollAttendanceIssueDO> display(List<HrmPayrollAttendanceIssueDO> rows) {
    if (rows.isEmpty()) return rows;
    Set<Long> userIds = new HashSet<>(), employeeIds = new HashSet<>();
    for (HrmPayrollAttendanceIssueDO row : rows) {
      userIds.add(row.getOwnerId());
      userIds.add(row.getReviewerId());
      employeeIds.add(row.getEmployeeId());
    }
    Map<Long, cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO> userMap =
        users.getUserMap(userIds);
    Map<Long, HrmEmployeeDO> employeeMap = new HashMap<>();
    employees.selectBatchIds(employeeIds).forEach(e -> employeeMap.put(e.getId(), e));
    for (HrmPayrollAttendanceIssueDO row : rows) {
      if (employeeMap.containsKey(row.getEmployeeId()))
        row.setEmployeeName(employeeMap.get(row.getEmployeeId()).getName());
      if (userMap.containsKey(row.getOwnerId()))
        row.setOwnerName(userMap.get(row.getOwnerId()).getNickname());
      if (userMap.containsKey(row.getReviewerId()))
        row.setReviewerName(userMap.get(row.getReviewerId()).getNickname());
    }
    return rows;
  }
}
