package cn.iocoder.yudao.module.hrm.service.payroll;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.attendance.config.HrmAttendanceGroupDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.attendance.config.HrmAttendanceHolidayDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.attendance.record.HrmAttendanceClockDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.employee.info.HrmEmployeeMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.*;
import java.math.*;
import java.time.*;
import java.util.*;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HrmPayrollWorkService {
  @Resource private HrmPayrollWorkRuleMapper ruleMapper;
  @Resource private HrmPayrollTimeEntryMapper entryMapper;
  @Resource private HrmPayrollTimeOffMapper offMapper;
  @Resource private HrmPayrollWorkCycleMapper cycleMapper;
  @Resource private HrmPayrollNoOvertimeMapper noOvertime;
  @Resource private HrmEmployeeMapper employeeMapper;
  @Resource private HrmPayrollEntityService entities;
  @Resource private javax.validation.Validator validator;
  @Resource private cn.iocoder.yudao.module.oa.api.payroll.OaPayrollSourceApi oaSource;

  @Resource
  private cn.iocoder.yudao.module.hrm.dal.mysql.attendance.record.HrmAttendanceClockMapper
      clockMapper;

  @Resource
  private cn.iocoder.yudao.module.hrm.dal.mysql.attendance.config.HrmAttendanceHolidayMapper
      holidayMapper;

  @Resource
  private cn.iocoder.yudao.module.hrm.service.attendance.config.HrmAttendanceGroupService
      attendanceGroups;

  private cn.iocoder.yudao.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO lockEmployee(
      Long id) {
    cn.iocoder.yudao.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO employee =
        employeeMapper.selectOneForUpdate(
            cn.iocoder.yudao.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO::getId, id);
    if (employee == null) throw exception(PAYROLL_WORK_INVALID);
    return employee;
  }

  public void requireMembership(Long entityId, Long employeeId, LocalDate from, LocalDate to) {
    if (entityId == null
        || from == null
        || to == null
        || to.isBefore(from)
        || java.time.temporal.ChronoUnit.DAYS.between(from, to) > 1095)
      throw exception(PAYROLL_WORK_INVALID);
    List<HrmPayrollEntityMembershipDO> memberships =
        entities.effectiveMemberships(entityId, from, to);
    for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
      final LocalDate d = day;
      if (memberships.stream()
          .noneMatch(
              m ->
                  Objects.equals(m.getEmployeeId(), employeeId)
                      && !d.isBefore(m.getEffectiveFrom())
                      && !d.isAfter(m.getEffectiveTo()))) throw exception(PAYROLL_WORK_INVALID);
    }
  }

  public List<Map<String, Object>> scopedEmployees(
      Long entityId, LocalDate from, LocalDate to, Long actor) {
    entities.check(entityId, "READ", actor);
    Set<Long> ids = new TreeSet<>();
    entities.effectiveMemberships(entityId, from, to).forEach(m -> ids.add(m.getEmployeeId()));
    List<Map<String, Object>> out = new ArrayList<>();
    for (Long id : ids) {
      cn.iocoder.yudao.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO e =
          employeeMapper.selectById(id);
      if (e == null) continue;
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("id", e.getId());
      row.put("name", e.getName());
      row.put("jobNumber", e.getJobNumber());
      out.add(row);
    }
    return out;
  }

  public void checkEmployeeScope(
      Long entityId, Long employeeId, LocalDate from, LocalDate to, String action, Long actor) {
    entities.check(entityId, action, actor);
    if (from == null
        || to == null
        || to.isBefore(from)
        || java.time.temporal.ChronoUnit.DAYS.between(from, to) > 1095
        || entities.effectiveMemberships(entityId, from, to).stream()
            .noneMatch(m -> Objects.equals(m.getEmployeeId(), employeeId)))
      throw exception(PAYROLL_WORK_INVALID);
  }

  public void checkDebitScope(Long entityId, Long debitId, Long actor) {
    HrmPayrollTimeOffDO debit = offMapper.selectById(debitId);
    if (debit == null) throw exception(PAYROLL_WORK_INVALID);
    LocalDate today = LocalDate.now(ZoneId.of("Asia/Shanghai"));
    checkEmployeeScope(entityId, debit.getEmployeeId(), today, today, "MANAGE", actor);
  }

  public void checkScope(Long entityId, String action, Long actor) {
    entities.check(entityId, action, actor);
  }

  public List<HrmPayrollWorkRuleDO> scopedRules(Long entityId, Long employeeId, Long actor) {
    entities.check(entityId, "READ", actor);
    List<HrmPayrollWorkRuleDO> rows = rules(employeeId);
    rows.removeIf(r -> !Objects.equals(r.getEntityId(), entityId));
    return rows;
  }

  public List<HrmPayrollWorkRuleDO> rules(Long employeeId) {
    return ruleMapper.selectList(
        new LambdaQueryWrapperX<HrmPayrollWorkRuleDO>()
            .eqIfPresent(HrmPayrollWorkRuleDO::getEmployeeId, employeeId)
            .orderByDesc(HrmPayrollWorkRuleDO::getEffectiveFrom));
  }

  public static boolean applies(HrmPayrollWorkRuleDO rule, LocalDate date) {
    return "APPROVED".equals(rule.getStatus())
        && !date.isBefore(rule.getEffectiveFrom())
        && !date.isAfter(rule.getEffectiveTo())
        && (rule.getSupersededFrom() == null || date.isBefore(rule.getSupersededFrom()));
  }

  private HrmPayrollWorkRuleDO replacementParent(
      Long id, Long entityId, Long employeeId, LocalDate from) {
    HrmPayrollWorkRuleDO prior = ruleMapper.selectOneForUpdate(HrmPayrollWorkRuleDO::getId, id);
    if (prior == null
        || !"APPROVED".equals(prior.getStatus())
        || prior.getSupersededFrom() != null
        || !Objects.equals(prior.getEntityId(), entityId)
        || !Objects.equals(prior.getEmployeeId(), employeeId)
        || from.isBefore(LocalDate.now(ZoneId.of("Asia/Shanghai")).plusDays(1))
        || from.isBefore(prior.getEffectiveFrom())
        || from.isAfter(prior.getEffectiveTo().plusDays(1))) throw exception(PAYROLL_WORK_INVALID);
    if (entryMapper.selectCount(
            new LambdaQueryWrapperX<HrmPayrollTimeEntryDO>()
                .eq(HrmPayrollTimeEntryDO::getRuleId, id)
                .ge(HrmPayrollTimeEntryDO::getStartTime, from.atStartOfDay()))
        > 0) throw exception(PAYROLL_WORK_INVALID);
    if ("COMPREHENSIVE".equals(prior.getSystemType())) {
      long months = java.time.temporal.ChronoUnit.MONTHS.between(prior.getEffectiveFrom(), from);
      if (months <= 0
          || months % prior.getCycleMonths() != 0
          || !prior.getEffectiveFrom().plusMonths(months).equals(from))
        throw exception(PAYROLL_WORK_INVALID);
    }
    return prior;
  }

  private boolean sameNoOvertimeSource(String old, String current) {
    com.fasterxml.jackson.databind.JsonNode a =
        cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseTree(old).deepCopy();
    com.fasterxml.jackson.databind.JsonNode b =
        cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseTree(current).deepCopy();
    for (com.fasterxml.jackson.databind.JsonNode root : Arrays.asList(a, b)) {
      com.fasterxml.jackson.databind.JsonNode rule = root.get("rule");
      if (rule instanceof com.fasterxml.jackson.databind.node.ObjectNode)
        ((com.fasterxml.jackson.databind.node.ObjectNode) rule)
            .remove(Arrays.asList("updater", "updateTime", "supersededFrom", "supersededBy"));
    }
    return a.equals(b);
  }

  @Transactional(
      rollbackFor = Exception.class,
      isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
  public Long createRule(HrmPayrollWorkRuleReqVO request, Long actor) {
    entities.check(request.getEntityId(), "MANAGE", actor);
    if (actor == null || !validator.validate(request).isEmpty())
      throw exception(PAYROLL_WORK_INVALID);
    lockEmployee(request.getEmployeeId());
    requireMembership(
        request.getEntityId(),
        request.getEmployeeId(),
        request.getEffectiveFrom(),
        request.getEffectiveTo());
    if ((!"IRREGULAR".equals(request.getSystemType())
            && request.getWorkdayRate().compareTo(new BigDecimal("1.5")) < 0)
        || ("STANDARD".equals(request.getSystemType())
            && request.getRestdayRate().compareTo(new BigDecimal("2")) < 0)
        || request.getHolidayRate().compareTo(new BigDecimal("3")) < 0)
      throw exception(PAYROLL_WORK_INVALID);
    if (request.getEffectiveTo().isBefore(request.getEffectiveFrom()))
      throw exception(PAYROLL_WORK_INVALID);
    List<HrmPayrollWorkRuleDO> overlap =
        ruleMapper.selectList(
            new LambdaQueryWrapperX<HrmPayrollWorkRuleDO>()
                .in(HrmPayrollWorkRuleDO::getStatus, Arrays.asList("PENDING", "APPROVED"))
                .eq(HrmPayrollWorkRuleDO::getEmployeeId, request.getEmployeeId())
                .eq(HrmPayrollWorkRuleDO::getEntityId, request.getEntityId())
                .le(HrmPayrollWorkRuleDO::getEffectiveFrom, request.getEffectiveTo())
                .ge(HrmPayrollWorkRuleDO::getEffectiveTo, request.getEffectiveFrom())
                .last("FOR UPDATE"));
    overlap.removeIf(
        r ->
            r.getSupersededFrom() != null
                && !request.getEffectiveFrom().isBefore(r.getSupersededFrom()));
    if (request.getPriorRuleId() != null) {
      replacementParent(
          request.getPriorRuleId(),
          request.getEntityId(),
          request.getEmployeeId(),
          request.getEffectiveFrom());
      overlap.removeIf(r -> Objects.equals(r.getId(), request.getPriorRuleId()));
    }
    if (!overlap.isEmpty()) throw exception(PAYROLL_WORK_INVALID);
    HrmPayrollWorkRuleDO rule = BeanUtils.toBean(request, HrmPayrollWorkRuleDO.class);
    rule.setStatus("PENDING");
    rule.setSubmittedBy(actor);
    ruleMapper.insert(rule);
    return rule.getId();
  }

  @Transactional(
      rollbackFor = Exception.class,
      isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
  public void approveRule(Long id, Long actor) {
    HrmPayrollWorkRuleDO rule = ruleMapper.selectById(id);
    if (rule == null) throw exception(PAYROLL_WORK_INVALID);
    entities.check(rule.getEntityId(), "APPROVE", actor);
    cn.iocoder.yudao.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO subject =
        lockEmployee(rule.getEmployeeId());
    requireMembership(
        rule.getEntityId(), rule.getEmployeeId(), rule.getEffectiveFrom(), rule.getEffectiveTo());
    rule = ruleMapper.selectOneForUpdate(HrmPayrollWorkRuleDO::getId, id);
    if (!"PENDING".equals(rule.getStatus()) || actor == null) throw exception(PAYROLL_WORK_INVALID);
    if (Objects.equals(subject.getUserId(), actor)
        || Objects.equals(rule.getSubmittedBy(), actor)
        || Objects.equals(rule.getCreator(), String.valueOf(actor)))
      throw exception(PAYROLL_SELF_APPROVAL);
    if (rule.getPriorRuleId() != null) {
      HrmPayrollWorkRuleDO prior =
          replacementParent(
              rule.getPriorRuleId(),
              rule.getEntityId(),
              rule.getEmployeeId(),
              rule.getEffectiveFrom());
      HrmPayrollWorkRuleDO edge = new HrmPayrollWorkRuleDO();
      edge.setId(prior.getId());
      edge.setSupersededFrom(rule.getEffectiveFrom());
      edge.setSupersededBy(rule.getId());
      ruleMapper.updateById(edge);
    }
    rule.setStatus("APPROVED");
    rule.setApprovedBy(actor);
    ruleMapper.updateById(rule);
  }

  @Transactional(
      rollbackFor = Exception.class,
      isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
  public void rejectRule(Long id, String reason, Long actor) {
    HrmPayrollWorkRuleDO rule = ruleMapper.selectById(id);
    if (rule == null || reason == null || reason.trim().isEmpty() || reason.length() > 1000)
      throw exception(PAYROLL_WORK_INVALID);
    entities.check(rule.getEntityId(), "APPROVE", actor);
    cn.iocoder.yudao.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO subject =
        lockEmployee(rule.getEmployeeId());
    rule = ruleMapper.selectOneForUpdate(HrmPayrollWorkRuleDO::getId, id);
    if (!"PENDING".equals(rule.getStatus()) || actor == null) throw exception(PAYROLL_WORK_INVALID);
    if (Objects.equals(subject.getUserId(), actor)
        || Objects.equals(rule.getSubmittedBy(), actor)
        || Objects.equals(rule.getCreator(), String.valueOf(actor)))
      throw exception(PAYROLL_SELF_APPROVAL);
    rule.setStatus("REJECTED");
    rule.setRejectedBy(actor);
    rule.setRejectionReason(reason);
    ruleMapper.updateById(rule);
  }

  public static int recognizedMinutes(
      LocalDateTime approvedStart,
      LocalDateTime approvedEnd,
      LocalDateTime clockStart,
      LocalDateTime clockEnd,
      int breaks) {
    if (approvedStart == null
        || approvedEnd == null
        || clockStart == null
        || clockEnd == null
        || !approvedEnd.isAfter(approvedStart)
        || !clockEnd.isAfter(clockStart)
        || breaks < 0) throw exception(PAYROLL_WORK_INVALID);
    LocalDateTime start = approvedStart.isAfter(clockStart) ? approvedStart : clockStart;
    LocalDateTime end = approvedEnd.isBefore(clockEnd) ? approvedEnd : clockEnd;
    long minutes = Math.max(0, Duration.between(start, end).toMinutes());
    if (minutes > 24 * 60 || breaks > minutes) throw exception(PAYROLL_WORK_INVALID);
    return (int) minutes - breaks;
  }

  @Transactional(
      rollbackFor = Exception.class,
      isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
  public List<Long> confirm(HrmPayrollTimeEntryReqVO request, Long actor) {
    if (actor == null
        || !validator.validate(request).isEmpty()
        || !request.getEndTime().isAfter(request.getStartTime())
        || Duration.between(request.getStartTime(), request.getEndTime()).toDays() > 31)
      throw exception(PAYROLL_WORK_INVALID);
    entities.check(request.getEntityId(), "MANAGE", actor);
    requireMembership(
        request.getEntityId(),
        request.getEmployeeId(),
        request.getStartTime().toLocalDate(),
        request.getEndTime().minusNanos(1).toLocalDate());
    List<Long> ids = new ArrayList<>();
    LocalDateTime cursor = request.getStartTime();
    boolean crossesDay =
        !cursor.toLocalDate().equals(request.getEndTime().minusNanos(1).toLocalDate());
    if (crossesDay && request.getBreakMinutes() != 0) throw exception(PAYROLL_WORK_SPLIT_REQUIRED);
    while (cursor.isBefore(request.getEndTime())) {
      LocalDateTime boundary = cursor.toLocalDate().plusDays(1).atStartOfDay();
      HrmPayrollTimeEntryReqVO segment = BeanUtils.toBean(request, HrmPayrollTimeEntryReqVO.class);
      segment.setStartTime(cursor);
      segment.setEndTime(boundary.isBefore(request.getEndTime()) ? boundary : request.getEndTime());
      ids.add(confirmSingle(segment, actor));
      cursor = segment.getEndTime();
    }
    return ids;
  }

  private Long confirmSingle(HrmPayrollTimeEntryReqVO request, Long actor) {
    cn.iocoder.yudao.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO employee =
        lockEmployee(request.getEmployeeId());
    requireMembership(
        request.getEntityId(),
        request.getEmployeeId(),
        request.getStartTime().toLocalDate(),
        request.getEndTime().minusNanos(1).toLocalDate());
    if (!"OA".equals(request.getSourceSystem())
        || employee.getUserId() == null
        || request.getClockInId() == null
        || request.getClockOutId() == null) throw exception(PAYROLL_WORK_INVALID);
    Long sourceId;
    try {
      sourceId = Long.valueOf(request.getSourceRecordId());
    } catch (NumberFormatException ex) {
      throw exception(PAYROLL_WORK_INVALID);
    }
    if (noOvertime.selectCount(
            new LambdaQueryWrapperX<HrmPayrollNoOvertimeDO>()
                .eq(HrmPayrollNoOvertimeDO::getSourceApplyId, sourceId)
                .eq(HrmPayrollNoOvertimeDO::getWorkDate, request.getStartTime().toLocalDate())
                .eq(HrmPayrollNoOvertimeDO::getStatus, "PENDING"))
        > 0) throw exception(PAYROLL_WORK_INVALID);
    cn.iocoder.yudao.module.oa.api.payroll.dto.OaPayrollOvertimeDTO approved =
        oaSource.getApprovedOvertime(sourceId);
    if (approved == null
        || !Objects.equals(employee.getUserId(), approved.getApplicantUserId())
        || approved.getStartTime() == null
        || approved.getEndTime() == null
        || request.getStartTime().isBefore(approved.getStartTime())
        || request.getEndTime().isAfter(approved.getEndTime()))
      throw exception(PAYROLL_WORK_INVALID);
    cn.iocoder.yudao.module.hrm.dal.dataobject.attendance.record.HrmAttendanceClockDO in =
        clockMapper.selectOneForUpdate(
            cn.iocoder.yudao.module.hrm.dal.dataobject.attendance.record.HrmAttendanceClockDO
                ::getId,
            request.getClockInId());
    cn.iocoder.yudao.module.hrm.dal.dataobject.attendance.record.HrmAttendanceClockDO out =
        clockMapper.selectOneForUpdate(
            cn.iocoder.yudao.module.hrm.dal.dataobject.attendance.record.HrmAttendanceClockDO
                ::getId,
            request.getClockOutId());
    if (in == null
        || out == null
        || !Objects.equals(in.getEmployeeId(), employee.getId())
        || !Objects.equals(out.getEmployeeId(), employee.getId())
        || !Objects.equals(in.getType(), 1)
        || !Objects.equals(out.getType(), 2)
        || Objects.equals(in.getStatus(), 3)
        || Objects.equals(out.getStatus(), 3)
        || in.getClockTime() == null
        || out.getClockTime() == null) throw exception(PAYROLL_WORK_INVALID);
    request.setClockStart(in.getClockTime());
    request.setClockEnd(out.getClockTime());
    request.setSourceRecordId(sourceId + ":" + request.getStartTime().toLocalDate());
    List<HrmPayrollTimeEntryDO> duplicate =
        entryMapper.selectList(
            new LambdaQueryWrapperX<HrmPayrollTimeEntryDO>()
                .eq(HrmPayrollTimeEntryDO::getSourceSystem, request.getSourceSystem())
                .eq(HrmPayrollTimeEntryDO::getSourceRecordId, request.getSourceRecordId()));
    if (!duplicate.isEmpty()) throw exception(PAYROLL_WORK_INVALID);
    if (!request
        .getStartTime()
        .toLocalDate()
        .equals(request.getEndTime().minusNanos(1).toLocalDate()))
      throw exception(PAYROLL_WORK_SPLIT_REQUIRED);
    List<HrmPayrollTimeEntryDO> overlap =
        entryMapper.selectList(
            new LambdaQueryWrapperX<HrmPayrollTimeEntryDO>()
                .eq(HrmPayrollTimeEntryDO::getEmployeeId, request.getEmployeeId())
                .lt(HrmPayrollTimeEntryDO::getStartTime, request.getEndTime())
                .gt(HrmPayrollTimeEntryDO::getEndTime, request.getStartTime()));
    if (!overlap.isEmpty()) throw exception(PAYROLL_WORK_INVALID);
    LocalDate date = request.getStartTime().toLocalDate();
    List<HrmPayrollWorkRuleDO> rules =
        ruleMapper.selectList(
            new LambdaQueryWrapperX<HrmPayrollWorkRuleDO>()
                .eq(HrmPayrollWorkRuleDO::getStatus, "APPROVED")
                .eq(HrmPayrollWorkRuleDO::getEmployeeId, request.getEmployeeId())
                .eq(HrmPayrollWorkRuleDO::getEntityId, request.getEntityId())
                .le(HrmPayrollWorkRuleDO::getEffectiveFrom, date)
                .ge(HrmPayrollWorkRuleDO::getEffectiveTo, date));
    rules.removeIf(r -> !applies(r, date));
    if (rules.size() != 1) throw exception(PAYROLL_WORK_INVALID);
    HrmPayrollWorkRuleDO rule = rules.get(0);
    int minutes =
        recognizedMinutes(
            request.getStartTime(),
            request.getEndTime(),
            request.getClockStart(),
            request.getClockEnd(),
            request.getBreakMinutes());
    if (minutes == 0) throw exception(PAYROLL_WORK_INVALID);
    if (!attendanceGroups
        .getAttendanceGroupMap(Collections.singleton(employee.getId()))
        .containsKey(employee.getId())) throw exception(PAYROLL_WORK_INVALID);
    cn.iocoder.yudao.module.hrm.dal.dataobject.attendance.config.HrmAttendanceHolidayDO holiday =
        holidayMapper.selectOne(
            new LambdaQueryWrapperX<
                    cn.iocoder.yudao.module.hrm.dal.dataobject.attendance.config
                        .HrmAttendanceHolidayDO>()
                .ge(
                    cn.iocoder.yudao.module.hrm.dal.dataobject.attendance.config
                            .HrmAttendanceHolidayDO
                        ::getDate,
                    date.atStartOfDay())
                .lt(
                    cn.iocoder.yudao.module.hrm.dal.dataobject.attendance.config
                            .HrmAttendanceHolidayDO
                        ::getDate,
                    date.plusDays(1).atStartOfDay()));
    request.setDayType(
        holiday != null && Objects.equals(holiday.getType(), 3)
            ? "HOLIDAY"
            : attendanceGroups.getEmployeeAttendanceShift(employee.getId(), date.atStartOfDay())
                    == null
                ? "RESTDAY"
                : "WORKDAY");
    cn.iocoder.yudao.module.hrm.dal.dataobject.attendance.config.HrmAttendanceGroupDO.Shift shift =
        attendanceGroups.getEmployeeAttendanceShift(employee.getId(), date.atStartOfDay());
    if ("WORKDAY".equals(request.getDayType()) && !"IRREGULAR".equals(rule.getSystemType())) {
      LocalDateTime a = date.atTime(shift.getStartTime()), b = date.atTime(shift.getEndTime());
      if (!b.isAfter(a)) b = b.plusDays(1);
      LocalDateTime start =
          request.getStartTime().isAfter(in.getClockTime())
              ? request.getStartTime()
              : in.getClockTime();
      LocalDateTime end =
          request.getEndTime().isBefore(out.getClockTime())
              ? request.getEndTime()
              : out.getClockTime();
      LocalDateTime overlapStart = a.isAfter(start) ? a : start,
          overlapEnd = b.isBefore(end) ? b : end;
      int regular = (int) Math.max(0, Duration.between(overlapStart, overlapEnd).toMinutes());
      if (regular > 0) regular -= shiftBreak(shift, date, overlapStart, overlapEnd);
      minutes = Math.max(0, minutes - regular);
      if (minutes == 0) throw exception(PAYROLL_WORK_INVALID);
    }
    if ("TIME_OFF".equals(request.getCompensation())
        && (!"RESTDAY".equals(request.getDayType()) || !"STANDARD".equals(rule.getSystemType())))
      throw exception(PAYROLL_WORK_INVALID);
    BigDecimal multiplier =
        "HOLIDAY".equals(request.getDayType())
            ? rule.getHolidayRate()
            : "RESTDAY".equals(request.getDayType())
                ? rule.getRestdayRate()
                : rule.getWorkdayRate();
    if (!"HOLIDAY".equals(request.getDayType()) && "IRREGULAR".equals(rule.getSystemType()))
      multiplier = BigDecimal.ZERO;
    if (!"HOLIDAY".equals(request.getDayType()) && "COMPREHENSIVE".equals(rule.getSystemType()))
      multiplier = BigDecimal.ZERO;
    HrmPayrollTimeEntryDO entry = BeanUtils.toBean(request, HrmPayrollTimeEntryDO.class);
    entry.setRuleId(rule.getId());
    entry.setSourceApplyId(sourceId);
    Map<String, Object> source = new LinkedHashMap<>();
    source.put("approval", approved);
    source.put("clockInId", in.getId());
    source.put("clockOutId", out.getId());
    source.put("clockStart", in.getClockTime());
    source.put("clockEnd", out.getClockTime());
    source.put("dayType", request.getDayType());
    source.put("shift", shift);
    entry.setSourceSnapshot(
        cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(source));
    entry.setRecognizedMinutes(minutes);
    entry.setConfirmedBy(actor);
    entry.setAmount(
        "TIME_OFF".equals(request.getCompensation())
            ? BigDecimal.ZERO
            : rule.getHourlyBase()
                .multiply(multiplier)
                .multiply(BigDecimal.valueOf(minutes))
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP));
    entryMapper.insert(entry);
    if ("TIME_OFF".equals(entry.getCompensation())) {
      HrmPayrollTimeOffDO credit = new HrmPayrollTimeOffDO();
      credit.setEmployeeId(entry.getEmployeeId());
      credit.setTimeEntryId(entry.getId());
      credit.setMinutes(minutes);
      credit.setActorId(actor);
      credit.setRequestKey("CREDIT-" + entry.getId());
      credit.setReason("批准加班转调休");
      credit.setStatus("CREDIT");
      credit.setExpiresOn(entry.getStartTime().toLocalDate().plusDays(rule.getTimeOffExpiryDays()));
      offMapper.insert(credit);
    }
    return entry.getId();
  }

  private int intersectionMinutes(
      LocalDateTime from, LocalDateTime to, LocalDateTime a, LocalDateTime b) {
    LocalDateTime start = from.isAfter(a) ? from : a, end = to.isBefore(b) ? to : b;
    return (int) Math.max(0, Duration.between(start, end).toMinutes());
  }

  private Map<String, Object> noOvertimeSource(
      Long entityId, Long employeeId, Long applyId, LocalDate day) {
    requireMembership(entityId, employeeId, day, day);
    cn.iocoder.yudao.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO e =
        employeeMapper.selectById(employeeId);
    cn.iocoder.yudao.module.oa.api.payroll.dto.OaPayrollOvertimeDTO a =
        oaSource.getApprovedOvertime(applyId);
    if (e == null
        || e.getUserId() == null
        || a == null
        || !Objects.equals(e.getUserId(), a.getApplicantUserId())
        || a.getStartTime() == null
        || a.getEndTime() == null
        || !a.getEndTime().isAfter(a.getStartTime())
        || !a.getStartTime().isBefore(day.plusDays(1).atStartOfDay())
        || !a.getEndTime().isAfter(day.atStartOfDay())) throw exception(PAYROLL_WORK_INVALID);
    LocalDateTime start =
        a.getStartTime().isAfter(day.atStartOfDay()) ? a.getStartTime() : day.atStartOfDay();
    LocalDateTime end =
        a.getEndTime().isBefore(day.plusDays(1).atStartOfDay())
            ? a.getEndTime()
            : day.plusDays(1).atStartOfDay();
    if (end.isAfter(LocalDateTime.now(ZoneId.of("Asia/Shanghai"))))
      throw exception(PAYROLL_WORK_INVALID);
    List<HrmPayrollWorkRuleDO> applicable = rules(employeeId);
    applicable.removeIf(
        r ->
            !Objects.equals(r.getEntityId(), entityId)
                || !"APPROVED".equals(r.getStatus())
                || day.isBefore(r.getEffectiveFrom())
                || day.isAfter(r.getEffectiveTo())
                || (r.getSupersededFrom() != null && !day.isBefore(r.getSupersededFrom())));
    if (applicable.size() != 1) throw exception(PAYROLL_WORK_INVALID);
    List<cn.iocoder.yudao.module.hrm.dal.dataobject.attendance.record.HrmAttendanceClockDO> clocks =
        clockMapper.selectList(
            new LambdaQueryWrapperX<
                    cn.iocoder.yudao.module.hrm.dal.dataobject.attendance.record
                        .HrmAttendanceClockDO>()
                .eq(
                    cn.iocoder.yudao.module.hrm.dal.dataobject.attendance.record
                            .HrmAttendanceClockDO
                        ::getEmployeeId,
                    employeeId)
                .ge(
                    cn.iocoder.yudao.module.hrm.dal.dataobject.attendance.record
                            .HrmAttendanceClockDO
                        ::getClockTime,
                    day.minusDays(1).atStartOfDay())
                .lt(
                    cn.iocoder.yudao.module.hrm.dal.dataobject.attendance.record
                            .HrmAttendanceClockDO
                        ::getClockTime,
                    day.plusDays(2).atStartOfDay())
                .orderByAsc(
                    cn.iocoder.yudao.module.hrm.dal.dataobject.attendance.record
                            .HrmAttendanceClockDO
                        ::getClockTime)
                .orderByAsc(
                    cn.iocoder.yudao.module.hrm.dal.dataobject.attendance.record
                            .HrmAttendanceClockDO
                        ::getId));
    cn.iocoder.yudao.module.hrm.dal.dataobject.attendance.config.HrmAttendanceHolidayDO holiday =
        holidayMapper.selectOne(
            new LambdaQueryWrapperX<
                    cn.iocoder.yudao.module.hrm.dal.dataobject.attendance.config
                        .HrmAttendanceHolidayDO>()
                .ge(
                    cn.iocoder.yudao.module.hrm.dal.dataobject.attendance.config
                            .HrmAttendanceHolidayDO
                        ::getDate,
                    day.atStartOfDay())
                .lt(
                    cn.iocoder.yudao.module.hrm.dal.dataobject.attendance.config
                            .HrmAttendanceHolidayDO
                        ::getDate,
                    day.plusDays(1).atStartOfDay()));
    Map<
            LocalDate,
            cn.iocoder.yudao.module.hrm.dal.dataobject.attendance.config.HrmAttendanceGroupDO.Shift>
        shifts = new TreeMap<>();
    for (LocalDate d = day.minusDays(1); !d.isAfter(day); d = d.plusDays(1))
      shifts.put(d, attendanceGroups.getEmployeeAttendanceShift(employeeId, d.atStartOfDay()));
    LocalDateTime scheduledEnd = null;
    for (Map.Entry<
            LocalDate,
            cn.iocoder.yudao.module.hrm.dal.dataobject.attendance.config.HrmAttendanceGroupDO.Shift>
        item : shifts.entrySet()) {
      cn.iocoder.yudao.module.hrm.dal.dataobject.attendance.config.HrmAttendanceGroupDO.Shift
          shift = item.getValue();
      if (shift == null) continue;
      if (shift.getStartTime() == null || shift.getEndTime() == null)
        throw exception(PAYROLL_WORK_INVALID);
      LocalDateTime p = item.getKey().atTime(shift.getStartTime()),
          q = item.getKey().atTime(shift.getEndTime());
      if (!q.isAfter(p)) q = q.plusDays(1);
      if (scheduledEnd != null && p.isBefore(scheduledEnd)) throw exception(PAYROLL_WORK_INVALID);
      scheduledEnd = q;
    }
    LocalDateTime open = null;
    cn.iocoder.yudao.module.hrm.dal.dataobject.attendance.record.HrmAttendanceClockDO openClock =
        null;
    List<cn.iocoder.yudao.module.hrm.dal.dataobject.attendance.record.HrmAttendanceClockDO>
        relevantClocks = new ArrayList<>();
    for (cn.iocoder.yudao.module.hrm.dal.dataobject.attendance.record.HrmAttendanceClockDO clock :
        clocks) {
      if (Objects.equals(clock.getStatus(), 3)
          && !clock.getClockTime().isBefore(start)
          && clock.getClockTime().isBefore(end)) throw exception(PAYROLL_WORK_INVALID);
      if (Objects.equals(clock.getType(), 1)) {
        if (open != null && open.isBefore(end) && clock.getClockTime().isAfter(start))
          throw exception(PAYROLL_WORK_INVALID);
        open = clock.getClockTime();
        openClock = clock;
      } else if (Objects.equals(clock.getType(), 2) && open != null) {
        LocalDateTime stop = clock.getClockTime();
        int worked = intersectionMinutes(start, end, open, stop);
        if (worked > 0) {
          if (!stop.isAfter(open)
              || Duration.between(open, stop).toHours() > 24
              || Objects.equals(clock.getStatus(), 3)
              || Objects.equals(openClock.getStatus(), 3)) throw exception(PAYROLL_WORK_INVALID);
          relevantClocks.add(openClock);
          relevantClocks.add(clock);
        }
        if (worked > 0 && (holiday == null || !Objects.equals(holiday.getType(), 3))) {
          for (Map.Entry<
                  LocalDate,
                  cn.iocoder.yudao.module.hrm.dal.dataobject.attendance.config.HrmAttendanceGroupDO
                      .Shift>
              item : shifts.entrySet()) {
            cn.iocoder.yudao.module.hrm.dal.dataobject.attendance.config.HrmAttendanceGroupDO.Shift
                shift = item.getValue();
            if (shift == null) continue;
            if (shift.getStartTime() == null || shift.getEndTime() == null)
              throw exception(PAYROLL_WORK_INVALID);
            LocalDateTime p = item.getKey().atTime(shift.getStartTime()),
                q = item.getKey().atTime(shift.getEndTime());
            if (!q.isAfter(p)) q = q.plusDays(1);
            LocalDateTime actualStart = start.isAfter(open) ? start : open,
                actualEnd = end.isBefore(stop) ? end : stop;
            worked -= intersectionMinutes(actualStart, actualEnd, p, q);
            worked -=
                shiftBreak(shift, item.getKey(), actualStart, actualEnd)
                    - shiftBreak(
                        shift,
                        item.getKey(),
                        actualStart.isAfter(p) ? actualStart : p,
                        actualEnd.isBefore(q) ? actualEnd : q);
          }
        }
        if (worked > 0) throw exception(PAYROLL_WORK_INVALID);
        open = null;
        openClock = null;
      } else if (!clock.getClockTime().isBefore(start) && clock.getClockTime().isBefore(end))
        relevantClocks.add(clock);
    }
    if (openClock != null && open.isBefore(end) && !open.isBefore(start.minusHours(24)))
      relevantClocks.add(openClock);
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("entityId", entityId);
    out.put("employeeId", employeeId);
    out.put("userId", e.getUserId());
    out.put("approval", a);
    out.put("date", day);
    out.put("from", start);
    out.put("to", end);
    out.put("rule", applicable.get(0));
    out.put("clocks", relevantClocks);
    out.put("shifts", shifts);
    out.put("holiday", holiday);
    return out;
  }

  public List<HrmPayrollNoOvertimeDO> noOvertimeCases(Long entityId, Long employeeId, Long actor) {
    entities.check(entityId, "READ", actor);
    List<HrmPayrollNoOvertimeDO> rows =
        noOvertime.selectList(
            new LambdaQueryWrapperX<HrmPayrollNoOvertimeDO>()
                .eq(HrmPayrollNoOvertimeDO::getEntityId, entityId)
                .eqIfPresent(HrmPayrollNoOvertimeDO::getEmployeeId, employeeId)
                .orderByDesc(HrmPayrollNoOvertimeDO::getId));
    for (HrmPayrollNoOvertimeDO row : rows) {
      if ("REJECTED".equals(row.getStatus())) continue;
      try {
        String current =
            cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(
                noOvertimeSource(
                    row.getEntityId(),
                    row.getEmployeeId(),
                    row.getSourceApplyId(),
                    row.getWorkDate()));
        row.setSourceFresh(sameNoOvertimeSource(row.getSourceSnapshot(), current));
      } catch (cn.iocoder.yudao.framework.common.exception.ServiceException ex) {
        row.setSourceFresh(false);
      }
    }
    return rows;
  }

  @Transactional(
      rollbackFor = Exception.class,
      isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
  public Long submitNoOvertime(HrmPayrollNoOvertimeReqVO r, Long actor) {
    entities.check(r.getEntityId(), "MANAGE", actor);
    if (actor == null || !validator.validate(r).isEmpty()) throw exception(PAYROLL_WORK_INVALID);
    lockEmployee(r.getEmployeeId());
    HrmPayrollNoOvertimeDO old =
        noOvertime.selectOne(HrmPayrollNoOvertimeDO::getRequestKey, r.getRequestKey());
    if (old != null) {
      if (!Objects.equals(old.getEntityId(), r.getEntityId())
          || !Objects.equals(old.getEmployeeId(), r.getEmployeeId())
          || !Objects.equals(old.getSourceApplyId(), r.getSourceApplyId())
          || !Objects.equals(old.getWorkDate(), r.getWorkDate())
          || !Objects.equals(old.getSubmittedBy(), actor)
          || !Objects.equals(old.getEvidence(), r.getEvidence())
          || !Objects.equals(old.getReason(), r.getReason())) throw exception(PAYROLL_WORK_INVALID);
      return old.getId();
    }
    String key = r.getSourceApplyId() + ":" + r.getWorkDate();
    if (entryMapper.selectOne(
                new LambdaQueryWrapperX<HrmPayrollTimeEntryDO>()
                    .eq(HrmPayrollTimeEntryDO::getSourceSystem, "OA")
                    .eq(HrmPayrollTimeEntryDO::getSourceRecordId, key))
            != null
        || noOvertime.selectCount(
                new LambdaQueryWrapperX<HrmPayrollNoOvertimeDO>()
                    .eq(HrmPayrollNoOvertimeDO::getSourceApplyId, r.getSourceApplyId())
                    .eq(HrmPayrollNoOvertimeDO::getWorkDate, r.getWorkDate())
                    .in(HrmPayrollNoOvertimeDO::getStatus, "PENDING", "APPROVED"))
            > 0) throw exception(PAYROLL_WORK_INVALID);
    HrmPayrollNoOvertimeDO row = BeanUtils.toBean(r, HrmPayrollNoOvertimeDO.class);
    row.setSourceSnapshot(
        cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(
            noOvertimeSource(
                r.getEntityId(), r.getEmployeeId(), r.getSourceApplyId(), r.getWorkDate())));
    row.setStatus("PENDING");
    row.setSubmittedBy(actor);
    noOvertime.insert(row);
    return row.getId();
  }

  @Transactional(
      rollbackFor = Exception.class,
      isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
  public void reviewNoOvertime(Long id, boolean approved, String opinion, Long actor) {
    HrmPayrollNoOvertimeDO row = noOvertime.selectById(id);
    if (row == null || opinion == null || opinion.trim().isEmpty() || opinion.length() > 1000)
      throw exception(PAYROLL_WORK_INVALID);
    entities.check(row.getEntityId(), "REVIEW", actor);
    cn.iocoder.yudao.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO employee =
        lockEmployee(row.getEmployeeId());
    row = noOvertime.selectOneForUpdate(HrmPayrollNoOvertimeDO::getId, id);
    if (actor == null
        || Objects.equals(actor, row.getSubmittedBy())
        || Objects.equals(actor, employee.getUserId())) throw exception(PAYROLL_SELF_APPROVAL);
    if ("APPROVED".equals(row.getStatus()) && approved) {
      validateNoOvertime(entryMapper.selectById(row.getTimeEntryId()));
      return;
    }
    if (!"PENDING".equals(row.getStatus())) throw exception(PAYROLL_WORK_INVALID);
    if (approved) {
      Map<String, Object> source =
          noOvertimeSource(
              row.getEntityId(), row.getEmployeeId(), row.getSourceApplyId(), row.getWorkDate());
      if (!sameNoOvertimeSource(
          row.getSourceSnapshot(),
          cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(source)))
        throw exception(PAYROLL_WORK_INVALID);
      HrmPayrollTimeEntryDO entry = new HrmPayrollTimeEntryDO();
      entry.setEntityId(row.getEntityId());
      entry.setEmployeeId(row.getEmployeeId());
      entry.setRuleId(((HrmPayrollWorkRuleDO) source.get("rule")).getId());
      entry.setSourceApplyId(row.getSourceApplyId());
      entry.setSourceSystem("OA");
      entry.setSourceRecordId(row.getSourceApplyId() + ":" + row.getWorkDate());
      entry.setStartTime((LocalDateTime) source.get("from"));
      entry.setEndTime((LocalDateTime) source.get("to"));
      entry.setRecognizedMinutes(0);
      entry.setBreakMinutes(0);
      entry.setCompensation("VERIFIED_ZERO");
      entry.setDayType("VERIFIED_ZERO");
      entry.setAmount(BigDecimal.ZERO);
      entry.setConfirmedBy(actor);
      entry.setEvidence(row.getEvidence());
      Map<String, Object> link = new LinkedHashMap<>();
      link.put("noOvertimeCaseId", id);
      link.put(
          "source",
          cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseTree(row.getSourceSnapshot()));
      link.put("reason", row.getReason());
      link.put("evidence", row.getEvidence());
      entry.setSourceSnapshot(
          cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(link));
      entryMapper.insert(entry);
      row.setTimeEntryId(entry.getId());
    }
    row.setStatus(approved ? "APPROVED" : "REJECTED");
    row.setReviewedBy(actor);
    row.setReviewOpinion(opinion);
    row.setReviewedTime(LocalDateTime.now(ZoneId.of("Asia/Shanghai")));
    noOvertime.updateById(row);
  }

  private void validateNoOvertime(HrmPayrollTimeEntryDO entry) {
    if (entry == null || entry.getSourceSnapshot() == null) throw exception(PAYROLL_WORK_INVALID);
    com.fasterxml.jackson.databind.JsonNode proof =
        cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseTree(entry.getSourceSnapshot());
    if (!proof.has("noOvertimeCaseId")) throw exception(PAYROLL_WORK_INVALID);
    HrmPayrollNoOvertimeDO row = noOvertime.selectById(proof.get("noOvertimeCaseId").longValue());
    if (row == null
        || !"APPROVED".equals(row.getStatus())
        || !Objects.equals(row.getTimeEntryId(), entry.getId())
        || !Objects.equals(row.getEntityId(), entry.getEntityId())
        || !Objects.equals(row.getEmployeeId(), entry.getEmployeeId())
        || !Objects.equals(row.getSourceApplyId(), entry.getSourceApplyId())
        || entry.getRecognizedMinutes() != 0
        || entry.getAmount().signum() != 0) throw exception(PAYROLL_WORK_INVALID);
    Map<String, Object> currentSource =
        noOvertimeSource(
            row.getEntityId(), row.getEmployeeId(), row.getSourceApplyId(), row.getWorkDate());
    if (!Objects.equals(entry.getStartTime(), currentSource.get("from"))
        || !Objects.equals(entry.getEndTime(), currentSource.get("to"))
        || !Objects.equals(
            entry.getRuleId(), ((HrmPayrollWorkRuleDO) currentSource.get("rule")).getId())
        || !Objects.equals(
            entry.getSourceRecordId(), row.getSourceApplyId() + ":" + row.getWorkDate())
        || !cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseTree(row.getSourceSnapshot())
            .equals(proof.get("source"))) throw exception(PAYROLL_WORK_INVALID);
    String current =
        cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(currentSource);
    if (!sameNoOvertimeSource(row.getSourceSnapshot(), current))
      throw exception(PAYROLL_WORK_INVALID);
  }

  public void validateSources(List<HrmPayrollTimeEntryDO> rows) {
    for (HrmPayrollTimeEntryDO row : rows) {
      if ("VERIFIED_ZERO".equals(row.getCompensation())) {
        validateNoOvertime(row);
        continue;
      }
      if (row.getEntityId() != null) {
        HrmPayrollWorkRuleDO rule = ruleMapper.selectById(row.getRuleId());
        if (rule == null
            || !"APPROVED".equals(rule.getStatus())
            || !Objects.equals(rule.getEntityId(), row.getEntityId())
            || !Objects.equals(rule.getEmployeeId(), row.getEmployeeId())
            || row.getStartTime().toLocalDate().isBefore(rule.getEffectiveFrom())
            || row.getEndTime().minusNanos(1).toLocalDate().isAfter(rule.getEffectiveTo())
            || !applies(rule, row.getStartTime().toLocalDate()))
          throw exception(PAYROLL_WORK_INVALID);
        requireMembership(
            row.getEntityId(),
            row.getEmployeeId(),
            row.getStartTime().toLocalDate(),
            row.getEndTime().minusNanos(1).toLocalDate());
      }
      cn.iocoder.yudao.module.oa.api.payroll.dto.OaPayrollOvertimeDTO approved =
          oaSource.getApprovedOvertime(row.getSourceApplyId());
      cn.iocoder.yudao.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO employee =
          employeeMapper.selectById(row.getEmployeeId());
      cn.iocoder.yudao.module.hrm.dal.dataobject.attendance.record.HrmAttendanceClockDO in =
          clockMapper.selectOneForUpdate(
              cn.iocoder.yudao.module.hrm.dal.dataobject.attendance.record.HrmAttendanceClockDO
                  ::getId,
              row.getClockInId());
      cn.iocoder.yudao.module.hrm.dal.dataobject.attendance.record.HrmAttendanceClockDO out =
          clockMapper.selectOneForUpdate(
              cn.iocoder.yudao.module.hrm.dal.dataobject.attendance.record.HrmAttendanceClockDO
                  ::getId,
              row.getClockOutId());
      if (approved == null
          || employee == null
          || !Objects.equals(employee.getUserId(), approved.getApplicantUserId())
          || approved.getStartTime() == null
          || approved.getEndTime() == null
          || row.getStartTime().isBefore(approved.getStartTime())
          || row.getEndTime().isAfter(approved.getEndTime())
          || in == null
          || out == null
          || !Objects.equals(in.getEmployeeId(), row.getEmployeeId())
          || !Objects.equals(out.getEmployeeId(), row.getEmployeeId())
          || !Objects.equals(in.getType(), 1)
          || !Objects.equals(out.getType(), 2)
          || Objects.equals(in.getStatus(), 3)
          || Objects.equals(out.getStatus(), 3)
          || !Objects.equals(in.getClockTime(), row.getClockStart())
          || !Objects.equals(out.getClockTime(), row.getClockEnd()))
        throw exception(PAYROLL_WORK_INVALID);
    }
  }

  private Map<String, Object> cycleFacts(Long employeeId, LocalDate from, LocalDate to) {
    Map<String, Object> facts = new LinkedHashMap<>();
    List<HrmAttendanceClockDO> window =
        clockMapper.selectList(
            new LambdaQueryWrapperX<HrmAttendanceClockDO>()
                .eq(HrmAttendanceClockDO::getEmployeeId, employeeId)
                .ge(HrmAttendanceClockDO::getClockTime, from.atStartOfDay())
                .lt(HrmAttendanceClockDO::getClockTime, to.plusDays(2).atStartOfDay())
                .orderByAsc(HrmAttendanceClockDO::getClockTime, HrmAttendanceClockDO::getId));
    List<Map<String, Object>> clockFacts = new ArrayList<>();
    boolean open = false;
    for (HrmAttendanceClockDO clock : window) {
      if (!clock.getClockTime().isBefore(to.plusDays(1).atStartOfDay()) && !open) break;
      if (Objects.equals(clock.getType(), 1)) open = true;
      else if (Objects.equals(clock.getType(), 2)) open = false;
      Map<String, Object> fact = new LinkedHashMap<>();
      fact.put("id", clock.getId());
      fact.put("time", clock.getClockTime().toString());
      fact.put("type", clock.getType());
      fact.put("status", clock.getStatus());
      clockFacts.add(fact);
    }
    facts.put("clocks", clockFacts);
    List<Map<String, Object>> plans = new ArrayList<>();
    for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
      Map<String, Object> plan = new LinkedHashMap<>();
      plan.put("date", day.toString());
      HrmAttendanceGroupDO.Shift shift =
          attendanceGroups.getEmployeeAttendanceShift(employeeId, day.atStartOfDay());
      plan.put("shift", shift);
      List<HrmAttendanceHolidayDO> holidays =
          holidayMapper.selectList(
              new LambdaQueryWrapperX<HrmAttendanceHolidayDO>()
                  .ge(HrmAttendanceHolidayDO::getDate, day.atStartOfDay())
                  .lt(HrmAttendanceHolidayDO::getDate, day.plusDays(1).atStartOfDay()));
      if (holidays.size() > 1) throw exception(PAYROLL_WORK_INVALID);
      plan.put("holidayType", holidays.isEmpty() ? null : holidays.get(0).getType());
      plans.add(plan);
    }
    facts.put("plans", plans);
    return facts;
  }

  private String entryFacts(Object records) {
    List<Map> rows = JsonUtils.parseArray(JsonUtils.toJsonString(records), Map.class);
    List<Map<String, Object>> facts = new ArrayList<>();
    for (Map row : rows) {
      Map<String, Object> fact = new TreeMap<>(row);
      for (String audit :
          Arrays.asList("creator", "createTime", "updater", "updateTime", "transMap"))
        fact.remove(audit);
      facts.add(fact);
    }
    facts.sort(Comparator.comparingLong(fact -> ((Number) fact.get("id")).longValue()));
    return JsonUtils.toJsonString(facts);
  }

  private void validateCycle(HrmPayrollWorkCycleDO cycle) {
    Map<String, Object> source = JsonUtils.parseObject(cycle.getSourceSnapshot(), Map.class);
    if (source == null
        || source.get("cycleFacts") == null
        || !JsonUtils.toJsonString(source.get("cycleFacts"))
            .equals(
                JsonUtils.toJsonString(
                    cycleFacts(cycle.getEmployeeId(), cycle.getCycleFrom(), cycle.getCycleTo()))))
      throw exception(PAYROLL_WORK_INVALID);
    List<HrmPayrollTimeEntryDO> currentApprovals =
        entries(cycle.getEmployeeId(), cycle.getCycleFrom(), cycle.getCycleTo());
    currentApprovals.removeIf(entry -> !Objects.equals(entry.getEntityId(), cycle.getEntityId()));
    if (source.get("approvals") == null
        || !entryFacts(source.get("approvals")).equals(entryFacts(currentApprovals)))
      throw exception(PAYROLL_WORK_INVALID);
    validateSources(currentApprovals);
    HrmPayrollWorkRuleDO rule = ruleMapper.selectById(cycle.getRuleId());
    if (rule == null
        || !Objects.equals(rule.getEmployeeId(), cycle.getEmployeeId())
        || !Objects.equals(rule.getEntityId(), cycle.getEntityId())
        || !applies(rule, cycle.getCycleFrom())
        || !applies(rule, cycle.getCycleTo())) throw exception(PAYROLL_WORK_INVALID);
    requireMembership(
        cycle.getEntityId(), cycle.getEmployeeId(), cycle.getCycleFrom(), cycle.getCycleTo());
  }

  public void validateInputSnapshot(String snapshot) {
    Map<String, Object> input =
        cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(snapshot, Map.class);
    Object overtime = input.get("overtime");
    if (overtime instanceof Map) {
      for (Object rows : ((Map<?, ?>) overtime).values()) {
        validateSources(
            cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseArray(
                cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(rows),
                HrmPayrollTimeEntryDO.class));
      }
    }
    Object cycles = input.get("workCycles");
    if (cycles instanceof Map) {
      for (Object rows : ((Map<?, ?>) cycles).values()) {
        List<HrmPayrollWorkCycleDO> settlements =
            cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseArray(
                cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(rows),
                HrmPayrollWorkCycleDO.class);
        for (HrmPayrollWorkCycleDO cycle : settlements) {
          validateCycle(cycle);
          Map<String, Object> source =
              cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(
                  cycle.getSourceSnapshot(), Map.class);
          validateSources(
              cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseArray(
                  cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(
                      source.get("approvals")),
                  HrmPayrollTimeEntryDO.class));
          List<cn.iocoder.yudao.module.hrm.dal.dataobject.attendance.record.HrmAttendanceClockDO>
              clocks =
                  cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseArray(
                      cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(
                          source.get("clocks")),
                      cn.iocoder.yudao.module.hrm.dal.dataobject.attendance.record
                          .HrmAttendanceClockDO.class);
          for (cn.iocoder.yudao.module.hrm.dal.dataobject.attendance.record.HrmAttendanceClockDO
              original : clocks) {
            cn.iocoder.yudao.module.hrm.dal.dataobject.attendance.record.HrmAttendanceClockDO
                current =
                    clockMapper.selectOneForUpdate(
                        cn.iocoder.yudao.module.hrm.dal.dataobject.attendance.record
                                .HrmAttendanceClockDO
                            ::getId,
                        original.getId());
            if (current == null
                || !Objects.equals(current.getClockTime(), original.getClockTime())
                || !Objects.equals(current.getEmployeeId(), original.getEmployeeId())
                || !Objects.equals(current.getType(), original.getType())
                || !Objects.equals(current.getStatus(), original.getStatus()))
              throw exception(PAYROLL_WORK_INVALID);
          }
        }
      }
    }
  }

  public boolean hasSources(Long employeeId, LocalDate from, LocalDate to) {
    return !entries(employeeId, from, to).isEmpty() || !cycles(employeeId, from, to).isEmpty();
  }

  public BigDecimal earnings(Long employeeId, LocalDate from, LocalDate to) {
    return earningsForEntity(employeeId, null, from, to);
  }

  public BigDecimal earningsForEntity(
      Long employeeId, Long entityId, LocalDate from, LocalDate to) {
    List<HrmPayrollTimeEntryDO> rows = entries(employeeId, from, to);
    if (entityId != null) rows.removeIf(e -> !Objects.equals(e.getEntityId(), entityId));
    validateSources(rows);
    for (HrmPayrollWorkRuleDO rule : rules(employeeId)) {
      if (entityId != null && !Objects.equals(rule.getEntityId(), entityId)) continue;
      if (!"APPROVED".equals(rule.getStatus()) || !"COMPREHENSIVE".equals(rule.getSystemType()))
        continue;
      for (int offset = 0; ; offset += rule.getCycleMonths()) {
        LocalDate start = rule.getEffectiveFrom().plusMonths(offset);
        if (start.isAfter(to) || start.isAfter(rule.getEffectiveTo())) break;
        LocalDate end =
            rule.getEffectiveFrom().plusMonths(offset + rule.getCycleMonths()).minusDays(1);
        if (end.isBefore(from) || end.isAfter(to) || !applies(rule, end)) continue;
        if (cycleMapper.selectOne(
                new LambdaQueryWrapperX<HrmPayrollWorkCycleDO>()
                    .eq(HrmPayrollWorkCycleDO::getRuleId, rule.getId())
                    .eq(HrmPayrollWorkCycleDO::getCycleFrom, start))
            == null) throw exception(PAYROLL_WORK_INVALID);
      }
    }
    List<HrmPayrollWorkCycleDO> settlements = cycles(employeeId, from, to);
    settlements.removeIf(
        cycle -> entityId != null && !Objects.equals(cycle.getEntityId(), entityId));
    settlements.forEach(this::validateCycle);
    return rows.stream()
        .map(HrmPayrollTimeEntryDO::getAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add)
        .add(
            settlements.stream()
                .map(HrmPayrollWorkCycleDO::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
  }

  public List<HrmPayrollTimeEntryDO> entries(Long employeeId, LocalDate from, LocalDate to) {
    return entryMapper.selectList(
        new LambdaQueryWrapperX<HrmPayrollTimeEntryDO>()
            .eqIfPresent(HrmPayrollTimeEntryDO::getEmployeeId, employeeId)
            .ge(HrmPayrollTimeEntryDO::getStartTime, from.atStartOfDay())
            .lt(HrmPayrollTimeEntryDO::getStartTime, to.plusDays(1).atStartOfDay())
            .orderByAsc(HrmPayrollTimeEntryDO::getStartTime));
  }

  public Map<String, Object> plannedTimeOff(Long employeeId, LocalDateTime from, LocalDateTime to) {
    if (from == null
        || to == null
        || !to.isAfter(from)
        || from.getSecond() != 0
        || from.getNano() != 0
        || to.getSecond() != 0
        || to.getNano() != 0
        || Duration.between(from, to).toDays() > 366) throw exception(PAYROLL_WORK_INVALID);
    int minutes = 0;
    BigDecimal days = BigDecimal.ZERO;
    List<Map<String, Object>> shifts = new ArrayList<>();
    LocalDateTime lastEnd = null;
    for (LocalDate day = from.toLocalDate().minusDays(1);
        !day.isAfter(to.toLocalDate());
        day = day.plusDays(1)) {
      cn.iocoder.yudao.module.hrm.dal.dataobject.attendance.config.HrmAttendanceGroupDO.Shift
          shift = attendanceGroups.getEmployeeAttendanceShift(employeeId, day.atStartOfDay());
      if (shift == null) continue;
      if (shift.getStartTime() == null || shift.getEndTime() == null)
        throw exception(PAYROLL_WORK_INVALID);
      LocalDateTime a = day.atTime(shift.getStartTime()), b = day.atTime(shift.getEndTime());
      if (!b.isAfter(a)) b = b.plusDays(1);
      LocalDateTime start = a.isAfter(from) ? a : from, end = b.isBefore(to) ? b : to;
      if (!end.isAfter(start)) continue;
      if (lastEnd != null && start.isBefore(lastEnd)) throw exception(PAYROLL_WORK_INVALID);
      int duration =
          Math.toIntExact(Duration.between(start, end).toMinutes())
              - shiftBreak(shift, day, start, end);
      int fullShift =
          Math.toIntExact(Duration.between(a, b).toMinutes()) - shiftBreak(shift, day, a, b);
      if (duration < 0 || fullShift <= 0) throw exception(PAYROLL_WORK_INVALID);
      days =
          days.add(
              BigDecimal.valueOf(duration)
                  .divide(BigDecimal.valueOf(fullShift), 8, RoundingMode.HALF_UP));
      Map<String, Object> source = new LinkedHashMap<>();
      source.put("date", day);
      source.put("shift", shift);
      source.put("from", start);
      source.put("to", end);
      source.put("minutes", duration);
      shifts.add(source);
      minutes = Math.addExact(minutes, duration);
      lastEnd = end;
    }
    if (minutes <= 0) throw exception(PAYROLL_WORK_INVALID);
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("minutes", minutes);
    result.put("shifts", shifts);
    result.put("day", days.setScale(2, RoundingMode.HALF_UP));
    return result;
  }

  public List<HrmPayrollTimeOffDO> timeOff(Long employeeId) {
    return offMapper.selectList(
        new LambdaQueryWrapperX<HrmPayrollTimeOffDO>()
            .eq(HrmPayrollTimeOffDO::getEmployeeId, employeeId)
            .orderByDesc(HrmPayrollTimeOffDO::getId));
  }

  private Map<Long, Integer> availableLots(Long employeeId, LocalDate date) {
    List<HrmPayrollTimeOffDO> ledger = timeOff(employeeId);
    Map<Long, Integer> lots = new LinkedHashMap<>();
    ledger.stream()
        .filter(r -> "CREDIT".equals(r.getStatus()) && !r.getExpiresOn().isBefore(date))
        .sorted(
            Comparator.comparing(HrmPayrollTimeOffDO::getExpiresOn)
                .thenComparing(HrmPayrollTimeOffDO::getId))
        .forEach(r -> lots.put(r.getId(), r.getMinutes()));
    for (HrmPayrollTimeOffDO row : ledger) {
      if (row.getAllocations() == null) continue;
      Map<String, Object> allocation =
          cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(
              row.getAllocations(), Map.class);
      int sign = "REFUND".equals(row.getStatus()) ? 1 : -1;
      allocation.forEach(
          (id, value) -> {
            Long lot = Long.valueOf(id);
            if (lots.containsKey(lot))
              lots.compute(lot, (k, v) -> v + sign * ((Number) value).intValue());
          });
    }
    return lots;
  }

  public int timeOffBalance(Long employeeId) {
    return availableLots(employeeId, LocalDate.now(java.time.ZoneId.of("Asia/Shanghai")))
        .values()
        .stream()
        .mapToInt(Integer::intValue)
        .sum();
  }

  @Transactional(
      rollbackFor = Exception.class,
      isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
  public Long useTimeOff(Long employeeId, int minutes, String key, String reason, Long actor) {
    if (key != null && key.startsWith("BPM-LEAVE-")) throw exception(PAYROLL_WORK_INVALID);
    return reserveTimeOff(
        employeeId,
        minutes,
        key,
        reason,
        actor,
        LocalDate.now(java.time.ZoneId.of("Asia/Shanghai")));
  }

  @Transactional(
      rollbackFor = Exception.class,
      isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
  public Long reserveTimeOff(
      Long employeeId, int minutes, String key, String reason, Long actor, LocalDate validOn) {
    lockEmployee(employeeId);
    if (validOn == null || validOn.isBefore(LocalDate.now(java.time.ZoneId.of("Asia/Shanghai"))))
      throw exception(PAYROLL_WORK_INVALID);
    if (minutes <= 0
        || key == null
        || key.trim().isEmpty()
        || key.length() > 100
        || reason == null
        || reason.trim().isEmpty()
        || actor == null) throw exception(PAYROLL_WORK_INVALID);
    HrmPayrollTimeOffDO duplicate = offMapper.selectOne(HrmPayrollTimeOffDO::getRequestKey, key);
    if (duplicate != null) {
      if (!Objects.equals(duplicate.getEmployeeId(), employeeId)
          || duplicate.getMinutes() != -minutes
          || !Objects.equals(duplicate.getReservedForDate(), validOn))
        throw exception(PAYROLL_WORK_INVALID);
      return duplicate.getId();
    }
    Map<Long, Integer> lots = availableLots(employeeId, validOn);
    if (lots.values().stream().mapToInt(Integer::intValue).sum() < minutes)
      throw exception(PAYROLL_WORK_INVALID);
    Map<Long, Integer> allocation = new LinkedHashMap<>();
    int remaining = minutes;
    for (Map.Entry<Long, Integer> lot : lots.entrySet()) {
      int take = Math.min(remaining, Math.max(0, lot.getValue()));
      if (take > 0) allocation.put(lot.getKey(), take);
      remaining -= take;
      if (remaining == 0) break;
    }
    HrmPayrollTimeOffDO debit = new HrmPayrollTimeOffDO();
    debit.setEmployeeId(employeeId);
    debit.setMinutes(-minutes);
    debit.setRequestKey(key);
    debit.setReason(reason);
    debit.setActorId(actor);
    debit.setStatus("RESERVED");
    debit.setReservedForDate(validOn);
    debit.setAllocations(
        cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(allocation));
    offMapper.insert(debit);
    return debit.getId();
  }

  @Transactional(
      rollbackFor = Exception.class,
      isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
  public void consumeTimeOff(Long debitId, Long actor) {
    rejectLinkedLeave(debitId);
    consumeTimeOffInternal(debitId, actor);
  }

  private void rejectLinkedLeave(Long debitId) {
    HrmPayrollTimeOffDO debit = offMapper.selectById(debitId);
    if (debit != null
        && debit.getRequestKey() != null
        && debit.getRequestKey().startsWith("BPM-LEAVE-")) throw exception(PAYROLL_WORK_INVALID);
  }

  @Transactional(
      rollbackFor = Exception.class,
      isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
  public void completeLeaveTimeOff(
      Long debitId, Long leaveId, boolean approved, String reason, Long actor) {
    HrmPayrollTimeOffDO debit = offMapper.selectById(debitId);
    if (debit == null || !Objects.equals(debit.getRequestKey(), "BPM-LEAVE-" + leaveId))
      throw exception(PAYROLL_WORK_INVALID);
    if (approved) consumeTimeOffInternal(debitId, actor);
    else refundTimeOffInternal(debitId, reason, actor);
  }

  private void consumeTimeOffInternal(Long debitId, Long actor) {
    HrmPayrollTimeOffDO debit = offMapper.selectById(debitId);
    if (debit == null) throw exception(PAYROLL_WORK_INVALID);
    lockEmployee(debit.getEmployeeId());
    debit = offMapper.selectOneForUpdate(HrmPayrollTimeOffDO::getId, debitId);
    if (actor == null
        || offMapper.selectOne(HrmPayrollTimeOffDO::getRequestKey, "REFUND-" + debitId) != null)
      throw exception(PAYROLL_WORK_INVALID);
    if ("CONSUMED".equals(debit.getStatus())) return;
    if (!"RESERVED".equals(debit.getStatus())) throw exception(PAYROLL_WORK_INVALID);
    Map<String, Object> allocations =
        cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(
            debit.getAllocations(), Map.class);
    for (String lotId : allocations.keySet()) {
      HrmPayrollTimeOffDO lot = offMapper.selectById(Long.valueOf(lotId));
      if (lot == null
          || lot.getExpiresOn()
              .isBefore(
                  debit.getReservedForDate() == null
                          || debit.getRequestKey() == null
                          || !debit.getRequestKey().startsWith("BPM-LEAVE-")
                      ? LocalDate.now(java.time.ZoneId.of("Asia/Shanghai"))
                      : debit.getReservedForDate())) throw exception(PAYROLL_WORK_INVALID);
    }
    debit.setStatus("CONSUMED");
    debit.setActorId(actor);
    offMapper.updateById(debit);
  }

  @Transactional(
      rollbackFor = Exception.class,
      isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
  public Long refundTimeOff(Long debitId, String key, String reason, Long actor) {
    rejectLinkedLeave(debitId);
    return refundTimeOffInternal(debitId, reason, actor);
  }

  private Long refundTimeOffInternal(Long debitId, String reason, Long actor) {
    HrmPayrollTimeOffDO debit = offMapper.selectById(debitId);
    if (debit == null || debit.getMinutes() >= 0 || actor == null)
      throw exception(PAYROLL_WORK_INVALID);
    lockEmployee(debit.getEmployeeId());
    String refundKey = "REFUND-" + debitId;
    HrmPayrollTimeOffDO duplicate =
        offMapper.selectOne(HrmPayrollTimeOffDO::getRequestKey, refundKey);
    if (duplicate != null) return duplicate.getId();
    if (reason == null || reason.trim().isEmpty()) throw exception(PAYROLL_WORK_INVALID);
    HrmPayrollTimeOffDO refund = new HrmPayrollTimeOffDO();
    refund.setEmployeeId(debit.getEmployeeId());
    refund.setMinutes(-debit.getMinutes());
    refund.setRequestKey(refundKey);
    refund.setReason(reason);
    refund.setActorId(actor);
    refund.setStatus("REFUND");
    refund.setAllocations(debit.getAllocations());
    offMapper.insert(refund);
    return refund.getId();
  }

  public List<HrmPayrollWorkCycleDO> cycles(Long employeeId, LocalDate from, LocalDate to) {
    return cycleMapper.selectList(
        new LambdaQueryWrapperX<HrmPayrollWorkCycleDO>()
            .eqIfPresent(HrmPayrollWorkCycleDO::getEmployeeId, employeeId)
            .ge(HrmPayrollWorkCycleDO::getCycleTo, from)
            .le(HrmPayrollWorkCycleDO::getCycleTo, to)
            .orderByAsc(HrmPayrollWorkCycleDO::getCycleFrom));
  }

  public static int cyclePayableMinutes(int actual, int standard, int approved) {
    if (actual < 0 || standard <= 0 || approved < 0) throw exception(PAYROLL_WORK_INVALID);
    return Math.min(Math.max(0, actual - standard), approved);
  }

  private int shiftBreak(
      cn.iocoder.yudao.module.hrm.dal.dataobject.attendance.config.HrmAttendanceGroupDO.Shift shift,
      LocalDate date,
      LocalDateTime start,
      LocalDateTime end) {
    if (shift == null
        || !Boolean.TRUE.equals(shift.getExcludeRestTime())
        || shift.getRestStartTime() == null
        || shift.getRestEndTime() == null) return 0;
    LocalDateTime a = date.atTime(shift.getRestStartTime()),
        b = date.atTime(shift.getRestEndTime());
    if (shift.getStartTime() != null
        && shift.getEndTime() != null
        && !shift.getEndTime().isAfter(shift.getStartTime())
        && shift.getRestStartTime().isBefore(shift.getStartTime())) {
      a = a.plusDays(1);
      b = b.plusDays(1);
    }
    if (!b.isAfter(a)) b = b.plusDays(1);
    LocalDateTime intersectionStart = a.isAfter(start) ? a : start,
        intersectionEnd = b.isBefore(end) ? b : end;
    return (int) Math.max(0, Duration.between(intersectionStart, intersectionEnd).toMinutes());
  }

  @Transactional(
      rollbackFor = Exception.class,
      isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
  public Long settleCycle(Long ruleId, LocalDate from, Long actor) {
    HrmPayrollWorkRuleDO rule = ruleMapper.selectById(ruleId);
    if (rule == null
        || actor == null
        || !"APPROVED".equals(rule.getStatus())
        || !"COMPREHENSIVE".equals(rule.getSystemType())) throw exception(PAYROLL_WORK_INVALID);
    entities.check(rule.getEntityId(), "MANAGE", actor);
    lockEmployee(rule.getEmployeeId());
    rule = ruleMapper.selectOneForUpdate(HrmPayrollWorkRuleDO::getId, ruleId);
    if (rule == null
        || !"APPROVED".equals(rule.getStatus())
        || !"COMPREHENSIVE".equals(rule.getSystemType())) throw exception(PAYROLL_WORK_INVALID);
    long offset =
        java.time.temporal.ChronoUnit.MONTHS.between(
            rule.getEffectiveFrom().withDayOfMonth(1), from.withDayOfMonth(1));
    if (offset < 0
        || offset % rule.getCycleMonths() != 0
        || !rule.getEffectiveFrom().plusMonths(offset).equals(from))
      throw exception(PAYROLL_WORK_INVALID);
    LocalDate to = rule.getEffectiveFrom().plusMonths(offset + rule.getCycleMonths()).minusDays(1);
    if (!applies(rule, to) || !to.isBefore(LocalDate.now(java.time.ZoneId.of("Asia/Shanghai"))))
      throw exception(PAYROLL_WORK_INVALID);
    requireMembership(rule.getEntityId(), rule.getEmployeeId(), from, to);
    HrmPayrollWorkCycleDO existing =
        cycleMapper.selectOne(
            new LambdaQueryWrapperX<HrmPayrollWorkCycleDO>()
                .eq(HrmPayrollWorkCycleDO::getRuleId, ruleId)
                .eq(HrmPayrollWorkCycleDO::getCycleFrom, from));
    if (existing != null) {
      validateCycle(existing);
      return existing.getId();
    }
    Map<String, Object> originalFacts =
        JsonUtils.parseObject(
            JsonUtils.toJsonString(cycleFacts(rule.getEmployeeId(), from, to)), Map.class);
    List<cn.iocoder.yudao.module.hrm.dal.dataobject.attendance.record.HrmAttendanceClockDO> clocks =
        clockMapper.selectList(
            new LambdaQueryWrapperX<
                    cn.iocoder.yudao.module.hrm.dal.dataobject.attendance.record
                        .HrmAttendanceClockDO>()
                .eq(
                    cn.iocoder.yudao.module.hrm.dal.dataobject.attendance.record
                            .HrmAttendanceClockDO
                        ::getEmployeeId,
                    rule.getEmployeeId())
                .ge(
                    cn.iocoder.yudao.module.hrm.dal.dataobject.attendance.record
                            .HrmAttendanceClockDO
                        ::getClockTime,
                    from.atStartOfDay())
                .lt(
                    cn.iocoder.yudao.module.hrm.dal.dataobject.attendance.record
                            .HrmAttendanceClockDO
                        ::getClockTime,
                    to.plusDays(2).atStartOfDay())
                .orderByAsc(
                    cn.iocoder.yudao.module.hrm.dal.dataobject.attendance.record
                            .HrmAttendanceClockDO
                        ::getClockTime));
    int actual = 0, planned = 0;
    LocalDateTime open = null;
    List<Long> used = new ArrayList<>();
    for (cn.iocoder.yudao.module.hrm.dal.dataobject.attendance.record.HrmAttendanceClockDO clock :
        clocks) {
      if (!clock.getClockTime().isBefore(to.plusDays(1).atStartOfDay()) && open == null) break;
      if (Objects.equals(clock.getStatus(), 3)) throw exception(PAYROLL_WORK_INVALID);
      if (Objects.equals(clock.getType(), 1)) {
        if (open != null) throw exception(PAYROLL_WORK_INVALID);
        open = clock.getClockTime();
        used.add(clock.getId());
      } else if (Objects.equals(clock.getType(), 2)) {
        if (open == null) throw exception(PAYROLL_WORK_INVALID);
        LocalDateTime end = clock.getClockTime();
        if (!end.isAfter(open) || Duration.between(open, end).toHours() > 24)
          throw exception(PAYROLL_WORK_INVALID);
        LocalDateTime cursor = open;
        while (cursor.isBefore(end) && cursor.isBefore(to.plusDays(1).atStartOfDay())) {
          LocalDate date = cursor.toLocalDate();
          LocalDateTime boundary = date.plusDays(1).atStartOfDay();
          LocalDateTime stop = end.isBefore(boundary) ? end : boundary;
          cn.iocoder.yudao.module.hrm.dal.dataobject.attendance.config.HrmAttendanceHolidayDO
              holiday =
                  holidayMapper.selectOne(
                      new LambdaQueryWrapperX<
                              cn.iocoder.yudao.module.hrm.dal.dataobject.attendance.config
                                  .HrmAttendanceHolidayDO>()
                          .ge(
                              cn.iocoder.yudao.module.hrm.dal.dataobject.attendance.config
                                      .HrmAttendanceHolidayDO
                                  ::getDate,
                              date.atStartOfDay())
                          .lt(
                              cn.iocoder.yudao.module.hrm.dal.dataobject.attendance.config
                                      .HrmAttendanceHolidayDO
                                  ::getDate,
                              boundary));
          if (holiday == null || !Objects.equals(holiday.getType(), 3))
            actual +=
                (int) Duration.between(cursor, stop).toMinutes()
                    - shiftBreak(
                        attendanceGroups.getEmployeeAttendanceShift(
                            rule.getEmployeeId(), date.atStartOfDay()),
                        date,
                        cursor,
                        stop);
          cursor = stop;
        }
        used.add(clock.getId());
        open = null;
      } else throw exception(PAYROLL_WORK_INVALID);
    }
    if (open != null) throw exception(PAYROLL_WORK_INVALID);
    for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
      cn.iocoder.yudao.module.hrm.dal.dataobject.attendance.config.HrmAttendanceGroupDO.Shift
          shift =
              attendanceGroups.getEmployeeAttendanceShift(rule.getEmployeeId(), day.atStartOfDay());
      if (shift == null) continue;
      LocalDateTime a = day.atTime(shift.getStartTime()), b = day.atTime(shift.getEndTime());
      if (!b.isAfter(a)) b = b.plusDays(1);
      planned += (int) Duration.between(a, b).toMinutes() - shiftBreak(shift, day, a, b);
    }
    List<HrmPayrollTimeEntryDO> approved = entries(rule.getEmployeeId(), from, to);
    Long cycleEntityId = rule.getEntityId();
    approved.removeIf(entry -> !Objects.equals(entry.getEntityId(), cycleEntityId));
    validateSources(approved);
    int recognized =
        approved.stream()
            .filter(
                e ->
                    Objects.equals(e.getRuleId(), ruleId)
                        && !"HOLIDAY".equals(e.getDayType())
                        && "PAY".equals(e.getCompensation()))
            .mapToInt(HrmPayrollTimeEntryDO::getRecognizedMinutes)
            .sum();
    int payable = cyclePayableMinutes(actual, rule.getCycleMinutes(), recognized);
    HrmPayrollWorkCycleDO cycle = new HrmPayrollWorkCycleDO();
    cycle.setEmployeeId(rule.getEmployeeId());
    cycle.setRuleId(ruleId);
    cycle.setEntityId(rule.getEntityId());
    cycle.setCycleFrom(from);
    cycle.setCycleTo(to);
    cycle.setPlannedMinutes(planned);
    cycle.setActualMinutes(actual);
    cycle.setRecognizedMinutes(recognized);
    cycle.setPayableMinutes(payable);
    cycle.setSettledBy(actor);
    cycle.setAmount(
        rule.getHourlyBase()
            .multiply(rule.getWorkdayRate())
            .multiply(BigDecimal.valueOf(payable))
            .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP));
    Map<String, Object> snapshot = new LinkedHashMap<>();
    snapshot.put("rule", rule);
    if (!JsonUtils.toJsonString(originalFacts)
        .equals(JsonUtils.toJsonString(cycleFacts(rule.getEmployeeId(), from, to))))
      throw exception(PAYROLL_WORK_INVALID);
    snapshot.put("cycleFacts", originalFacts);
    snapshot.put(
        "clocks",
        clocks.stream()
            .filter(c -> used.contains(c.getId()))
            .collect(java.util.stream.Collectors.toList()));
    snapshot.put("approvals", approved);
    cycle.setSourceSnapshot(
        cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(snapshot));
    cycleMapper.insert(cycle);
    return cycle.getId();
  }

  public Map<String, Object> summary(Long employeeId, LocalDate from, LocalDate to) {
    return summaryForEntity(employeeId, null, from, to);
  }

  public Map<String, Object> summaryForEntity(
      Long employeeId, Long entityId, LocalDate from, LocalDate to) {
    List<HrmPayrollTimeEntryDO> rows = entries(employeeId, from, to);
    if (entityId != null) rows.removeIf(e -> !Objects.equals(e.getEntityId(), entityId));
    int total = rows.stream().mapToInt(HrmPayrollTimeEntryDO::getRecognizedMinutes).sum();
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("recognizedMinutes", total);
    result.put(
        "amount",
        rows.stream()
            .map(HrmPayrollTimeEntryDO::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add));
    result.put("timeOffBalance", timeOffBalance(employeeId));
    List<HrmPayrollTimeEntryDO> monthlyRows =
        entries(employeeId, from.withDayOfMonth(1), to.withDayOfMonth(to.lengthOfMonth()));
    if (entityId != null) monthlyRows.removeIf(e -> !Objects.equals(e.getEntityId(), entityId));
    Map<String, Integer> monthly = new LinkedHashMap<>();
    monthlyRows.forEach(
        e ->
            monthly.merge(
                e.getStartTime().toLocalDate().withDayOfMonth(1).toString(),
                e.getRecognizedMinutes(),
                Integer::sum));
    result.put("monthlyMinutes", monthly);
    result.put(
        "limitExceeded",
        rows.stream()
            .anyMatch(
                e -> {
                  HrmPayrollWorkRuleDO r = ruleMapper.selectById(e.getRuleId());
                  return monthly.get(e.getStartTime().toLocalDate().withDayOfMonth(1).toString())
                      > r.getLimitMinutes();
                }));
    List<HrmPayrollWorkCycleDO> scopedCycles = cycles(employeeId, from, to);
    if (entityId != null) scopedCycles.removeIf(c -> !Objects.equals(c.getEntityId(), entityId));
    for (HrmPayrollWorkCycleDO cycle : scopedCycles) {
      try {
        validateCycle(cycle);
        cycle.setSourceFresh(true);
      } catch (cn.iocoder.yudao.framework.common.exception.ServiceException failure) {
        cycle.setSourceFresh(false);
      }
    }
    result.put("cycles", scopedCycles);
    return result;
  }
}
