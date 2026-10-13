package cn.iocoder.yudao.module.hrm.service.payroll;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.HrmPayrollEntityMembershipDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.salary.monthrecord.HrmSalaryMonthRecordDO;
import cn.iocoder.yudao.module.hrm.dal.mysql.salary.monthrecord.HrmSalaryMonthRecordMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.LocalDate;
import java.util.*;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;

/** Server-side scope checks shared by payroll, approvals, payment and export. */
@Service
public class HrmPayrollScopeService {
  @Resource private HrmPayrollEntityService entities;
  @Resource private HrmSalaryMonthRecordMapper batches;
  @Resource private cn.iocoder.yudao.module.hrm.dal.mysql.employee.info.HrmEmployeeMapper employees;
  @Resource private cn.iocoder.yudao.module.hrm.dal.mysql.payroll.HrmPayrollBatchEventMapper events;

  @Resource
  private cn.iocoder.yudao.module.hrm.dal.mysql.salary.monthrecord
          .HrmSalaryMonthEmployeeRecordMapper
      employeeRecords;

  @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
  public void bindDraft(Long batchId, Long entityId, String reason) {
    Long actor = getLoginUserId();
    if (!entities.superAdmin(actor)
        || reason == null
        || reason.trim().isEmpty()
        || reason.length() > 1000) throw exception(PAYROLL_ENTITY_INVALID);
    entities.check(entityId, "MANAGE", actor);
    entities.lockApproved(entityId);
    HrmSalaryMonthRecordDO batch = batches.selectByIdForUpdate(batchId);
    if (batch == null
        || batch.getEntityId() != null
        || !(Objects.equals(batch.getStatus(), 5) || Objects.equals(batch.getStatus(), 11)))
      throw exception(PAYROLL_ENTITY_INVALID);
    if (batches.selectByEntityYearMonth(entityId, batch.getYear(), batch.getMonth()) != null)
      throw exception(SALARY_MONTH_RECORD_EXISTS);
    cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.HrmPayrollBatchEventDO event =
        new cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.HrmPayrollBatchEventDO();
    event.setMonthRecordId(batchId);
    event.setAction("bind_subject");
    event.setActorId(actor);
    event.setFromStatus(batch.getStatus());
    event.setToStatus(5);
    event.setReason(reason);
    Map<String, Object> before = new LinkedHashMap<>();
    before.put("entityId", entityId);
    before.put("previousBatch", batch);
    List<
            cn.iocoder.yudao.module.hrm.dal.dataobject.salary.monthrecord
                .HrmSalaryMonthEmployeeRecordDO>
        original = employeeRecords.selectListByMonthRecordId(batchId);
    before.put("previousEmployeeRecords", original);
    event.setSnapshot(JsonUtils.toJsonString(before));
    events.insert(event);
    employeeRecords.delete(
        new LambdaQueryWrapper<
                cn.iocoder.yudao.module.hrm.dal.dataobject.salary.monthrecord
                    .HrmSalaryMonthEmployeeRecordDO>()
            .eq(
                cn.iocoder.yudao.module.hrm.dal.dataobject.salary.monthrecord
                        .HrmSalaryMonthEmployeeRecordDO
                    ::getMonthRecordId,
                batchId));
    batches.updateById(
        new HrmSalaryMonthRecordDO()
            .setId(batchId)
            .setEntityId(entityId)
            .setBatchKey("REGULAR")
            .setStatus(5));
  }

  public void create(Long entityId) {
    entities.check(entityId, "CALCULATE", getLoginUserId());
    entities.lockApproved(entityId);
  }

  public List<Long> calculableEntities() {
    return entities.accessible("CALCULATE", getLoginUserId());
  }

  public void check(HrmSalaryMonthRecordDO batch, String action) {
    check(batch, action, getLoginUserId());
  }

  public void check(HrmSalaryMonthRecordDO batch, String action, Long actor) {
    if (batch == null) throw exception(SALARY_MONTH_RECORD_NOT_EXISTS);
    // Historical unbound batches remain visible only to administrators for explicit attribution.
    if (batch.getEntityId() == null) {
      if (("READ".equals(action) || "MANAGE".equals(action)) && entities.superAdmin(actor)) return;
      throw exception(PAYROLL_ENTITY_INVALID);
    }
    entities.check(batch.getEntityId(), action, actor);
    batch.setEntityName(entities.name(batch.getEntityId()));
    if (!"READ".equals(action)) entities.approved(batch.getEntityId());
  }

  public HrmSalaryMonthRecordDO require(Long batchId, String action) {
    HrmSalaryMonthRecordDO batch = batches.selectById(batchId);
    check(batch, action);
    return batch;
  }

  public void restrict(LambdaQueryWrapper<HrmSalaryMonthRecordDO> query, String action) {
    Long actor = getLoginUserId();
    List<Long> ids = entities.accessible(action, actor);
    boolean legacy = "READ".equals(action) && entities.superAdmin(actor);
    if (ids.isEmpty() && !legacy) {
      query.apply("1=0");
      return;
    }
    query.and(
        w -> {
          if (!ids.isEmpty()) w.in(HrmSalaryMonthRecordDO::getEntityId, ids);
          if (legacy) {
            if (!ids.isEmpty()) w.or();
            w.isNull(HrmSalaryMonthRecordDO::getEntityId);
          }
        });
  }

  public cn.iocoder.yudao.framework.common.pojo.PageResult<HrmSalaryMonthRecordDO> page(
      cn.iocoder.yudao.module.hrm.controller.admin.salary.vo.monthrecord
              .HrmSalaryMonthRecordPageReqVO
          request,
      boolean paymentOnly) {
    cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX<HrmSalaryMonthRecordDO>
        query = new cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX<>();
    query
        .eqIfPresent(HrmSalaryMonthRecordDO::getEntityId, request.getEntityId())
        .eqIfPresent(HrmSalaryMonthRecordDO::getYear, request.getYear())
        .eqIfPresent(HrmSalaryMonthRecordDO::getMonth, request.getMonth())
        .eqIfPresent(HrmSalaryMonthRecordDO::getStatus, request.getStatus());
    if (paymentOnly) query.in(HrmSalaryMonthRecordDO::getStatus, 10, 15, 16, 17);
    query.orderByDesc(
        HrmSalaryMonthRecordDO::getYear,
        HrmSalaryMonthRecordDO::getMonth,
        HrmSalaryMonthRecordDO::getId);
    restrict(query, "READ");
    cn.iocoder.yudao.framework.common.pojo.PageResult<HrmSalaryMonthRecordDO> result =
        batches.selectPage(request, query);
    result.getList().forEach(row -> row.setEntityName(entities.name(row.getEntityId())));
    return result;
  }

  public List<HrmSalaryMonthRecordDO> list(Integer status) {
    cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX<HrmSalaryMonthRecordDO>
        query = new cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX<>();
    query.eqIfPresent(HrmSalaryMonthRecordDO::getStatus, status);
    query.orderByDesc(
        HrmSalaryMonthRecordDO::getYear,
        HrmSalaryMonthRecordDO::getMonth,
        HrmSalaryMonthRecordDO::getId);
    restrict(query, "READ");
    List<HrmSalaryMonthRecordDO> result = batches.selectList(query);
    result.forEach(row -> row.setEntityName(entities.name(row.getEntityId())));
    return result;
  }

  public <T> void restrictRelated(
      LambdaQueryWrapper<T> query,
      com.baomidou.mybatisplus.core.toolkit.support.SFunction<T, Long> batchId,
      String action) {
    LambdaQueryWrapper<HrmSalaryMonthRecordDO> parent = new LambdaQueryWrapper<>();
    restrict(parent, action);
    List<Long> ids =
        batches.selectList(parent).stream()
            .map(HrmSalaryMonthRecordDO::getId)
            .collect(java.util.stream.Collectors.toList());
    if (ids.isEmpty()) query.apply("1=0");
    else query.in(batchId, ids);
  }

  public List<HrmPayrollEntityMembershipDO> memberships(HrmSalaryMonthRecordDO batch) {
    if (batch.getEntityId() == null) throw exception(PAYROLL_ENTITY_INVALID);
    return entities.effectiveMemberships(
        batch.getEntityId(), batch.getStartTime().toLocalDate(), batch.getEndTime().toLocalDate());
  }

  public boolean requiresInsuranceAttribution(HrmSalaryMonthRecordDO batch, Long employeeId) {
    return batch.getEntityId() != null
        && entities.hasOtherMembership(
            employeeId,
            batch.getEntityId(),
            batch.getStartTime().toLocalDate(),
            batch.getEndTime().toLocalDate());
  }

  public Map<Long, Set<LocalDate>> payrollDates(HrmSalaryMonthRecordDO batch) {
    Map<Long, Set<LocalDate>> result = new LinkedHashMap<>();
    LocalDate start = batch.getStartTime().toLocalDate(), end = batch.getEndTime().toLocalDate();
    for (HrmPayrollEntityMembershipDO membership : memberships(batch)) {
      LocalDate from =
          membership.getEffectiveFrom().isBefore(start) ? start : membership.getEffectiveFrom();
      LocalDate to = membership.getEffectiveTo().isAfter(end) ? end : membership.getEffectiveTo();
      cn.iocoder.yudao.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO employee =
          employees.selectById(membership.getEmployeeId());
      if (employee == null) throw exception(PAYROLL_ENTITY_INVALID);
      if (employee.getEntryTime() != null && employee.getEntryTime().toLocalDate().isAfter(from))
        from = employee.getEntryTime().toLocalDate();
      if (employee.getLeaveTime() != null && employee.getLeaveTime().toLocalDate().isBefore(to))
        to = employee.getLeaveTime().toLocalDate();
      if (from.isAfter(to)) continue;

      Set<LocalDate> dates =
          result.computeIfAbsent(membership.getEmployeeId(), k -> new TreeSet<>());
      for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) dates.add(day);
    }
    return result;
  }

  public Map<String, Object> snapshot(HrmSalaryMonthRecordDO batch) {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("entity", entities.approved(batch.getEntityId()));
    result.put("memberships", memberships(batch));
    result.put("batchId", batch.getId());
    return result;
  }

  public void validateInputSnapshot(String json) {
    com.fasterxml.jackson.databind.JsonNode input = JsonUtils.parseTree(json);
    com.fasterxml.jackson.databind.JsonNode original = input.get("entityScope");
    if (original == null) {
      if (input.get("sourceRunId") != null) return;
      throw exception(PAYROLL_ENTITY_INVALID);
    }
    HrmSalaryMonthRecordDO batch = batches.selectById(original.get("batchId").longValue());
    if (batch == null
        || !original.equals(JsonUtils.parseTree(JsonUtils.toJsonString(snapshot(batch)))))
      throw exception(PAYROLL_ENTITY_INVALID);
  }
}
