package cn.iocoder.yudao.module.hrm.service.payroll;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollTaxDeductionReqVO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.employee.info.HrmEmployeeMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class HrmPayrollTaxDeductionService {
  public static final List<String> FIELDS =
      Collections.unmodifiableList(
          Arrays.asList("260101", "260102", "260103", "260104", "260105", "260106"));
  @Resource private HrmPayrollTaxDeductionMapper declarations;
  @Resource private HrmPayrollTaxDeductionEventMapper events;
  @Resource private HrmPayrollTaxProfileService profiles;
  @Resource private HrmPayrollEntityService entities;
  @Resource private HrmEmployeeMapper employees;
  @Resource private javax.validation.Validator validator;

  private void reason(String text) {
    if (text == null || text.trim().isEmpty() || text.length() > 1000)
      throw exception(PAYROLL_TAX_INVALID);
  }

  private HrmPayrollTaxDeductionDO active(Long entity, Long employee, Integer year, Integer month) {
    return declarations.selectOne(
        new LambdaQueryWrapperX<HrmPayrollTaxDeductionDO>()
            .eq(HrmPayrollTaxDeductionDO::getEntityId, entity)
            .eq(HrmPayrollTaxDeductionDO::getEmployeeId, employee)
            .eq(HrmPayrollTaxDeductionDO::getYear, year)
            .eq(HrmPayrollTaxDeductionDO::getMonth, month)
            .eq(HrmPayrollTaxDeductionDO::getStatus, "APPROVED"));
  }

  private void event(HrmPayrollTaxDeductionDO row, String action, String reason, Long actor) {
    events.insert(
        new HrmPayrollTaxDeductionEventDO()
            .setDeclarationId(row.getId())
            .setAction(action)
            .setReason(reason)
            .setActorId(actor)
            .setSnapshot(JsonUtils.toJsonString(row)));
  }

  public Map<String, BigDecimal> values(HrmPayrollTaxDeductionDO row) {
    Map<String, BigDecimal> result;
    try {
      result =
          JsonUtils.parseObject(
              row.getValuesSnapshot(),
              new com.fasterxml.jackson.core.type.TypeReference<TreeMap<String, BigDecimal>>() {});
    } catch (RuntimeException ex) {
      throw exception(PAYROLL_TAX_INVALID);
    }
    if (result == null || !result.keySet().equals(new HashSet<>(FIELDS)))
      throw exception(PAYROLL_TAX_INVALID);
    result.values().forEach(PayrollTaxPolicyEngine::amount);
    return result;
  }

  private HrmEmployeeDO lockEmployee(Long id) {
    HrmEmployeeDO employee = employees.selectByIdForUpdate(id);
    if (employee == null) throw exception(PAYROLL_TAX_INVALID);
    return employee;
  }

  private void validate(HrmPayrollTaxDeductionReqVO request) {
    if (!validator.validate(request).isEmpty()
        || request.getSourceDate().getYear() != request.getYear()
        || request.getSourceDate().getMonthValue() != request.getMonth()
        || request.getSourceDate().isAfter(LocalDate.now(ZoneId.of("Asia/Shanghai")))
        || !request.getValues().keySet().equals(new HashSet<>(FIELDS)))
      throw exception(PAYROLL_TAX_INVALID);
    for (BigDecimal amount : request.getValues().values()) {
      PayrollTaxPolicyEngine.amount(amount);
      if ("ZERO".equals(request.getMode()) && amount.signum() != 0)
        throw exception(PAYROLL_TAX_INVALID);
    }
    // Infant-care deduction began in 2022; medical expenses are excluded from payroll collection.
    if (request.getYear() < 2022 && request.getValues().get("260106").signum() != 0)
      throw exception(PAYROLL_TAX_INVALID);
    request.setValues(new TreeMap<>(request.getValues()));
  }

  @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
  public Long create(HrmPayrollTaxDeductionReqVO request, Long actor) {
    validate(request);
    entities.check(request.getEntityId(), "MANAGE", actor);
    lockEmployee(request.getEmployeeId());
    String snapshot = JsonUtils.toJsonString(request);
    HrmPayrollTaxDeductionDO replay =
        declarations.selectOne(HrmPayrollTaxDeductionDO::getRequestKey, request.getRequestKey());
    if (replay != null) {
      if (!Objects.equals(replay.getSubmittedBy(), actor)
          || !Objects.equals(replay.getRequestSnapshot(), snapshot))
        throw exception(PAYROLL_TAX_INVALID);
      return replay.getId();
    }
    HrmPayrollTaxProfileDO profile =
        profiles.requireApproved(request.getEntityId(), request.getEmployeeId(), request.getYear());
    HrmPayrollTaxDeductionDO current =
        active(
            request.getEntityId(), request.getEmployeeId(), request.getYear(), request.getMonth());
    if (!Objects.equals(request.getPriorDeclarationId(), current == null ? null : current.getId()))
      throw exception(PAYROLL_VERSION_CONFLICT);
    List<HrmPayrollTaxDeductionDO> previous =
        declarations.selectList(
            new LambdaQueryWrapperX<HrmPayrollTaxDeductionDO>()
                .eq(HrmPayrollTaxDeductionDO::getEntityId, request.getEntityId())
                .eq(HrmPayrollTaxDeductionDO::getEmployeeId, request.getEmployeeId())
                .eq(HrmPayrollTaxDeductionDO::getYear, request.getYear())
                .eq(HrmPayrollTaxDeductionDO::getMonth, request.getMonth()));
    HrmPayrollTaxDeductionDO row =
        BeanUtils.toBean(request, HrmPayrollTaxDeductionDO.class)
            .setVersion(
                previous.stream().mapToInt(HrmPayrollTaxDeductionDO::getVersion).max().orElse(0)
                    + 1)
            .setTaxpayerKey(profile.getTaxpayerKey())
            .setTaxRegistrationNumber(profile.getTaxRegistrationNumber())
            .setValuesSnapshot(JsonUtils.toJsonString(request.getValues()))
            .setRequestSnapshot(snapshot)
            .setSubmittedBy(actor)
            .setStatus("PENDING");
    declarations.insert(row);
    event(row, "SUBMIT", request.getEvidence(), actor);
    return row.getId();
  }

  @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
  public void review(Long id, boolean approved, String reason, Long actor) {
    reason(reason);
    HrmPayrollTaxDeductionDO candidate = declarations.selectById(id);
    if (candidate == null) throw exception(PAYROLL_TAX_INVALID);
    entities.check(candidate.getEntityId(), "REVIEW", actor);
    HrmEmployeeDO employee = lockEmployee(candidate.getEmployeeId());
    HrmPayrollTaxDeductionDO row =
        declarations.selectOneForUpdate(HrmPayrollTaxDeductionDO::getId, id);
    if (!"PENDING".equals(row.getStatus())) throw exception(PAYROLL_TAX_INVALID);
    if (Objects.equals(row.getSubmittedBy(), actor) || Objects.equals(employee.getUserId(), actor))
      throw exception(PAYROLL_SELF_APPROVAL);
    if (approved) {
      HrmPayrollTaxProfileDO profile =
          profiles.requireApproved(row.getEntityId(), row.getEmployeeId(), row.getYear());
      if (!Objects.equals(row.getTaxpayerKey(), profile.getTaxpayerKey())
          || !Objects.equals(row.getTaxRegistrationNumber(), profile.getTaxRegistrationNumber()))
        throw exception(PAYROLL_TAX_INVALID);
      HrmPayrollTaxDeductionDO current =
          active(row.getEntityId(), row.getEmployeeId(), row.getYear(), row.getMonth());
      if (!Objects.equals(row.getPriorDeclarationId(), current == null ? null : current.getId()))
        throw exception(PAYROLL_VERSION_CONFLICT);
      if (current != null) {
        current.setStatus("SUPERSEDED");
        declarations.updateById(current);
        event(current, "SUPERSEDE", "由版本" + row.getId() + "接续", actor);
      }
    }
    row.setStatus(approved ? "APPROVED" : "REJECTED")
        .setReviewedBy(actor)
        .setReviewedTime(LocalDateTime.now(ZoneId.of("Asia/Shanghai")))
        .setReviewReason(reason);
    declarations.updateById(row);
    event(row, approved ? "APPROVE" : "REJECT", reason, actor);
  }

  /** Internal source retrieval; callers must check their batch/entity scope first. */
  public HrmPayrollTaxDeductionDO source(Long entity, Long employee, Integer year, Integer month) {
    HrmPayrollTaxDeductionDO row = active(entity, employee, year, month);
    if (row == null) return null;
    HrmPayrollTaxProfileDO profile = profiles.requireApproved(entity, employee, year);
    if (!Objects.equals(row.getTaxpayerKey(), profile.getTaxpayerKey())
        || !Objects.equals(row.getTaxRegistrationNumber(), profile.getTaxRegistrationNumber()))
      throw exception(PAYROLL_TAX_INVALID);
    return row;
  }

  public List<HrmPayrollTaxDeductionDO> list(
      Long entity, Integer year, Integer month, Long employee, Long actor) {
    entities.check(entity, "READ", actor);
    return declarations.selectList(
        new LambdaQueryWrapperX<HrmPayrollTaxDeductionDO>()
            .eq(HrmPayrollTaxDeductionDO::getEntityId, entity)
            .eq(HrmPayrollTaxDeductionDO::getYear, year)
            .eqIfPresent(HrmPayrollTaxDeductionDO::getMonth, month)
            .eqIfPresent(HrmPayrollTaxDeductionDO::getEmployeeId, employee)
            .orderByDesc(HrmPayrollTaxDeductionDO::getId));
  }

  public List<HrmPayrollTaxDeductionEventDO> history(Long id, Long actor) {
    HrmPayrollTaxDeductionDO row = declarations.selectById(id);
    if (row == null) throw exception(PAYROLL_TAX_INVALID);
    entities.check(row.getEntityId(), "READ", actor);
    return events.selectList(
        new LambdaQueryWrapperX<HrmPayrollTaxDeductionEventDO>()
            .eq(HrmPayrollTaxDeductionEventDO::getDeclarationId, id)
            .orderByAsc(HrmPayrollTaxDeductionEventDO::getId));
  }
}
