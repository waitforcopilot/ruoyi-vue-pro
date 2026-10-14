package cn.iocoder.yudao.module.hrm.service.payroll;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollBonusReqVO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.salary.monthrecord.HrmSalaryMonthRecordDO;
import cn.iocoder.yudao.module.hrm.dal.mysql.employee.info.HrmEmployeeMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.salary.monthrecord.HrmSalaryMonthRecordMapper;
import java.time.*;
import java.util.*;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class HrmPayrollBonusService {
  @Resource private HrmPayrollBonusMapper bonuses;
  @Resource private HrmPayrollSourceClaimMapper claims;
  @Resource private HrmPayrollPaymentMapper payments;
  @Resource private HrmPayrollRunMapper runs;
  @Resource private HrmPayrollBonusEventMapper events;
  @Resource private HrmSalaryMonthRecordMapper batches;
  @Resource private HrmEmployeeMapper employees;
  @Resource private HrmPayrollScopeService scope;
  @Resource private HrmPayrollTaxProfileService profiles;
  @Resource private HrmPayrollTaxPolicyService policies;
  @Resource private javax.validation.Validator validator;

  private HrmSalaryMonthRecordDO batch(Long id, String action, Long actor, boolean draft) {
    HrmSalaryMonthRecordDO b = draft ? batches.selectByIdForUpdate(id) : batches.selectById(id);
    if (b == null) throw exception(SALARY_MONTH_RECORD_NOT_EXISTS);
    scope.check(b, action, actor);
    if (b.getEntityId() == null) throw exception(PAYROLL_ENTITY_INVALID);
    if (draft && !(Objects.equals(b.getStatus(), 5) || Objects.equals(b.getStatus(), 11)))
      throw exception(SALARY_MONTH_RECORD_STATUS_INVALID);
    if (draft && b.getExpectedPaymentDate() == null)
      throw exception(PAYROLL_TAX_PAYMENT_DATE_MISSING);
    return b;
  }

  private HrmPayrollBonusDO active(Long batch, Long employee) {
    return bonuses.selectOne(
        new LambdaQueryWrapperX<HrmPayrollBonusDO>()
            .eq(HrmPayrollBonusDO::getBatchId, batch)
            .eq(HrmPayrollBonusDO::getEmployeeId, employee)
            .eq(HrmPayrollBonusDO::getStatus, "APPROVED"));
  }

  private void event(HrmPayrollBonusDO row, String action, String reason, Long actor) {
    events.insert(
        new HrmPayrollBonusEventDO()
            .setBonusId(row.getId())
            .setAction(action)
            .setReason(reason)
            .setActorId(actor)
            .setSnapshot(JsonUtils.toJsonString(row)));
  }

  private HrmPayrollTaxProfileDO profile(HrmSalaryMonthRecordDO b, Long employee) {
    if (!scope.payrollDates(b).containsKey(employee)) throw exception(PAYROLL_ENTITY_INVALID);
    HrmPayrollTaxProfileDO p =
        profiles.requireApproved(b.getEntityId(), employee, b.getExpectedPaymentDate().getYear());
    if (!Boolean.TRUE.equals(p.getResidentThisYear()))
      throw exception(PAYROLL_TAX_RESIDENCY_UNCONFIRMED);
    return p;
  }

  private void eligible(HrmSalaryMonthRecordDO b, HrmPayrollTaxProfileDO p, String mode) {
    HrmPayrollTaxPolicyDO policy =
        policies.requireApproved(b.getEntityId(), b.getExpectedPaymentDate().getYear());
    if ("SEPARATE".equals(mode)
        && (Boolean.TRUE.equals(p.getSeparateBonusUsedElsewhere())
            || !Boolean.TRUE.equals(policy.getSeparateBonusAllowed())
            || policy.getSeparateBonusValidTo() == null
            || LocalDate.of(b.getExpectedPaymentDate().getYear(), 12, 31)
                .isAfter(policy.getSeparateBonusValidTo()))) throw exception(PAYROLL_TAX_INVALID);
  }

  @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
  public Long create(HrmPayrollBonusReqVO req, Long actor) {
    if (!validator.validate(req).isEmpty()
        || ("NONE".equals(req.getMode()) != (req.getAmount().signum() == 0)))
      throw exception(PAYROLL_TAX_INVALID);
    HrmSalaryMonthRecordDO b = batch(req.getBatchId(), "MANAGE", actor, true);
    HrmEmployeeDO employee = employees.selectByIdForUpdate(req.getEmployeeId());
    if (employee == null) throw exception(PAYROLL_ENTITY_INVALID);
    String snapshot = JsonUtils.toJsonString(req);
    HrmPayrollBonusDO replay =
        bonuses.selectOne(HrmPayrollBonusDO::getRequestKey, req.getRequestKey());
    if (replay != null) {
      if (!Objects.equals(replay.getSubmittedBy(), actor)
          || !Objects.equals(replay.getRequestSnapshot(), snapshot))
        throw exception(PAYROLL_TAX_INVALID);
      return replay.getId();
    }
    HrmPayrollTaxProfileDO p = profile(b, req.getEmployeeId());
    eligible(b, p, req.getMode());
    HrmPayrollBonusDO prior = active(b.getId(), req.getEmployeeId());
    if (!Objects.equals(req.getPriorBonusId(), prior == null ? null : prior.getId()))
      throw exception(PAYROLL_VERSION_CONFLICT);
    int version =
        bonuses
                .selectList(
                    new LambdaQueryWrapperX<HrmPayrollBonusDO>()
                        .eq(HrmPayrollBonusDO::getBatchId, b.getId())
                        .eq(HrmPayrollBonusDO::getEmployeeId, req.getEmployeeId()))
                .stream()
                .mapToInt(HrmPayrollBonusDO::getVersion)
                .max()
                .orElse(0)
            + 1;
    HrmPayrollBonusDO row =
        BeanUtils.toBean(req, HrmPayrollBonusDO.class)
            .setEntityId(b.getEntityId())
            .setExpectedPaymentDate(b.getExpectedPaymentDate())
            .setProfileId(p.getId())
            .setVersion(version)
            .setStatus("PENDING")
            .setSubmittedBy(actor)
            .setRequestSnapshot(snapshot);
    bonuses.insert(row);
    event(row, "SUBMIT", req.getEvidence(), actor);
    return row.getId();
  }

  @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
  public void review(Long id, boolean approve, String reason, Long actor) {
    if (reason == null || reason.trim().isEmpty() || reason.length() > 1000)
      throw exception(PAYROLL_TAX_INVALID);
    HrmPayrollBonusDO candidate = bonuses.selectById(id);
    if (candidate == null) throw exception(PAYROLL_TAX_INVALID);
    HrmSalaryMonthRecordDO b = batch(candidate.getBatchId(), "REVIEW", actor, true);
    HrmEmployeeDO employee = employees.selectByIdForUpdate(candidate.getEmployeeId());
    if (employee == null) throw exception(PAYROLL_ENTITY_INVALID);
    HrmPayrollBonusDO row = bonuses.selectOneForUpdate(HrmPayrollBonusDO::getId, id);
    if (!"PENDING".equals(row.getStatus())) throw exception(PAYROLL_TAX_INVALID);
    if (Objects.equals(actor, row.getSubmittedBy()) || Objects.equals(actor, employee.getUserId()))
      throw exception(PAYROLL_SELF_APPROVAL);
    if (approve) {
      HrmPayrollTaxProfileDO p = profile(b, row.getEmployeeId());
      eligible(b, p, row.getMode());
      if (!Objects.equals(p.getId(), row.getProfileId())
          || !Objects.equals(b.getExpectedPaymentDate(), row.getExpectedPaymentDate()))
        throw exception(PAYROLL_VERSION_CONFLICT);
      HrmPayrollBonusDO prior = active(b.getId(), row.getEmployeeId());
      if (!Objects.equals(row.getPriorBonusId(), prior == null ? null : prior.getId()))
        throw exception(PAYROLL_VERSION_CONFLICT);
      if (prior != null) {
        prior.setStatus("SUPERSEDED");
        bonuses.updateById(prior);
        event(prior, "SUPERSEDE", "接续版本" + id, actor);
      }
    }
    row.setStatus(approve ? "APPROVED" : "REJECTED")
        .setReviewedBy(actor)
        .setReviewedTime(LocalDateTime.now(ZoneId.of("Asia/Shanghai")))
        .setReviewReason(reason);
    bonuses.updateById(row);
    event(row, approve ? "APPROVE" : "REJECT", reason, actor);
  }

  /** Internal approved source; callers must authorize the batch first. */
  public HrmPayrollBonusDO source(Long batchId, Long employeeId) {
    HrmPayrollBonusDO row = active(batchId, employeeId);
    if (row == null) return null;
    HrmSalaryMonthRecordDO b = batches.selectById(batchId);
    if (b == null
        || b.getExpectedPaymentDate() == null
        || !Objects.equals(b.getEntityId(), row.getEntityId())
        || !Objects.equals(b.getExpectedPaymentDate(), row.getExpectedPaymentDate()))
      throw exception(PAYROLL_VERSION_CONFLICT);
    HrmPayrollTaxProfileDO p = profile(b, employeeId);
    eligible(b, p, row.getMode());
    if (!Objects.equals(p.getId(), row.getProfileId())) throw exception(PAYROLL_VERSION_CONFLICT);
    return row;
  }

  public static String quotaKey(String taxpayerKey, int year) {
    if (taxpayerKey == null || taxpayerKey.trim().isEmpty()) throw exception(PAYROLL_TAX_INVALID);
    return cn.hutool.crypto.digest.DigestUtil.sha256Hex(taxpayerKey + ":" + year);
  }

  /** Reserved by this batch is valid during its freeze/export revalidation; paid is always used. */
  public boolean separateUsed(Long batchId, String taxpayerKey, int year) {
    boolean reserved =
        claims
            .selectList(
                new LambdaQueryWrapperX<HrmPayrollSourceClaimDO>()
                    .eq(HrmPayrollSourceClaimDO::getKind, "BONUS_YEAR")
                    .eq(HrmPayrollSourceClaimDO::getSourceKey, quotaKey(taxpayerKey, year))
                    .in(HrmPayrollSourceClaimDO::getStatus, "RESERVED", "PAID"))
            .stream()
            .anyMatch(
                c -> "PAID".equals(c.getStatus()) || !Objects.equals(c.getBatchId(), batchId));
    if (reserved) return true;
    Set<String> paidBonusWorkers = new HashSet<>();
    for (HrmPayrollSourceClaimDO claim :
        claims.selectList(
            new LambdaQueryWrapperX<HrmPayrollSourceClaimDO>()
                .eq(HrmPayrollSourceClaimDO::getKind, "BONUS_YEAR")
                .eq(HrmPayrollSourceClaimDO::getStatus, "PAID")))
      paidBonusWorkers.add(claim.getRunId() + ":" + claim.getEmployeeId());
    if (paidBonusWorkers.isEmpty()) return false;
    Map<Long, HrmPayrollRunDO> runCache = new HashMap<>();
    // Actual bank dates can cross the planned tax year; retain that cash-year evidence as well.
    for (HrmPayrollPaymentDO payment :
        payments.selectList(
            new LambdaQueryWrapperX<HrmPayrollPaymentDO>()
                .eq(HrmPayrollPaymentDO::getStatus, "SUCCESS")
                .ge(HrmPayrollPaymentDO::getValueDate, LocalDate.of(year, 1, 1))
                .le(HrmPayrollPaymentDO::getValueDate, LocalDate.of(year, 12, 31)))) {
      if (!paidBonusWorkers.contains(payment.getRunId() + ":" + payment.getEmployeeId())) continue;
      HrmPayrollRunDO run = runCache.computeIfAbsent(payment.getRunId(), runs::selectById);
      if (run == null) throw exception(PAYROLL_TAX_INVALID);
      List<
              cn.iocoder.yudao.module.hrm.dal.dataobject.salary.monthrecord
                  .HrmSalaryMonthEmployeeRecordDO>
          rows =
              JsonUtils.parseArray(
                  run.getResultSnapshot(),
                  cn.iocoder.yudao.module.hrm.dal.dataobject.salary.monthrecord
                      .HrmSalaryMonthEmployeeRecordDO.class);
      if (rows == null) throw exception(PAYROLL_TAX_INVALID);
      int matches = 0;
      for (cn.iocoder.yudao.module.hrm.dal.dataobject.salary.monthrecord
              .HrmSalaryMonthEmployeeRecordDO
          row : rows) {
        if (!Objects.equals(row.getEmployeeId(), payment.getEmployeeId())) continue;
        matches++;
        HrmPayrollTaxWithholdingService.Source source =
            JsonUtils.parseObject(
                row.getTaxSourceSnapshot(), HrmPayrollTaxWithholdingService.Source.class);
        if (source == null || source.getProfile() == null || !"SEPARATE".equals(source.bonusMode()))
          throw exception(PAYROLL_TAX_INVALID);
        if (Objects.equals(source.getProfile().getTaxpayerKey(), taxpayerKey)) return true;
      }
      if (matches != 1) throw exception(PAYROLL_TAX_INVALID);
    }
    return false;
  }

  public List<Map<String, Object>> employees(Long batchId, Long actor) {
    HrmSalaryMonthRecordDO b = batch(batchId, "READ", actor, false);
    List<Map<String, Object>> result = new ArrayList<>();
    for (Long id : scope.payrollDates(b).keySet()) {
      HrmEmployeeDO employee = employees.selectById(id);
      if (employee == null) throw exception(PAYROLL_ENTITY_INVALID);
      Map<String, Object> item = new LinkedHashMap<>();
      item.put("id", id);
      item.put("name", employee.getName());
      item.put("jobNumber", employee.getJobNumber());
      result.add(item);
    }
    return result;
  }

  public List<HrmPayrollBonusDO> list(Long batchId, Long actor) {
    batch(batchId, "READ", actor, false);
    return bonuses.selectList(
        new LambdaQueryWrapperX<HrmPayrollBonusDO>()
            .eq(HrmPayrollBonusDO::getBatchId, batchId)
            .orderByDesc(HrmPayrollBonusDO::getId));
  }

  public List<HrmPayrollBonusEventDO> history(Long id, Long actor) {
    HrmPayrollBonusDO row = bonuses.selectById(id);
    if (row == null) throw exception(PAYROLL_TAX_INVALID);
    batch(row.getBatchId(), "READ", actor, false);
    return events.selectList(
        new LambdaQueryWrapperX<HrmPayrollBonusEventDO>()
            .eq(HrmPayrollBonusEventDO::getBonusId, id)
            .orderByAsc(HrmPayrollBonusEventDO::getId));
  }
}
