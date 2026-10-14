package cn.iocoder.yudao.module.hrm.service.payroll;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollTaxProfileReqVO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.employee.info.HrmEmployeeMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.*;
import java.time.*;
import java.util.*;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class HrmPayrollTaxProfileService {
  @Resource private HrmPayrollTaxProfileMapper profiles;
  @Resource private HrmPayrollTaxProfileEventMapper events;
  @Resource private HrmEmployeeMapper employees;
  @Resource private HrmPayrollEntityService entities;
  @Resource private HrmPayrollTaxIdentityCodec identity;
  @Resource private HrmPayrollTaxPolicyService policies;
  @Resource private javax.validation.Validator validator;

  private HrmEmployeeDO employee(Long id) {
    HrmEmployeeDO row = employees.selectByIdForUpdate(id);
    if (row == null) throw exception(EMPLOYEE_NOT_EXISTS);
    return row;
  }

  private String identityKey(String type, String country, String value) {
    return identity.fingerprint(
        TenantContextHolder.getRequiredTenantId()
            + ":"
            + type
            + ":"
            + country
            + ":"
            + value.toUpperCase(Locale.ROOT));
  }

  private String employeeSnapshot(Long entityId, HrmEmployeeDO employee, Integer year) {
    List<HrmPayrollEntityMembershipDO> memberships =
        new ArrayList<>(
            entities.effectiveMemberships(
                entityId, LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31)));
    memberships.removeIf(row -> !Objects.equals(row.getEmployeeId(), employee.getId()));
    if (memberships.isEmpty()) throw exception(PAYROLL_TAX_INVALID);
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("employeeId", employee.getId());
    result.put("userId", employee.getUserId());
    result.put("name", employee.getName());
    result.put("entryTime", employee.getEntryTime());
    result.put("leaveTime", employee.getLeaveTime());
    result.put("entryStatus", employee.getEntryStatus());
    result.put("memberships", memberships);
    return JsonUtils.toJsonString(result);
  }

  private HrmPayrollTaxProfileDO active(Long entityId, Long employeeId, Integer year) {
    return profiles.selectOne(
        new LambdaQueryWrapperX<HrmPayrollTaxProfileDO>()
            .eq(HrmPayrollTaxProfileDO::getEntityId, entityId)
            .eq(HrmPayrollTaxProfileDO::getEmployeeId, employeeId)
            .eq(HrmPayrollTaxProfileDO::getYear, year)
            .eq(HrmPayrollTaxProfileDO::getStatus, "APPROVED"));
  }

  private void event(HrmPayrollTaxProfileDO row, String action, String reason, Long actor) {
    events.insert(
        new HrmPayrollTaxProfileEventDO()
            .setProfileId(row.getId())
            .setAction(action)
            .setActorId(actor)
            .setReason(reason)
            .setSnapshot(JsonUtils.toJsonString(row)));
  }

  @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
  public Long create(HrmPayrollTaxProfileReqVO request, Long actor) {
    if (!validator.validate(request).isEmpty()
        || request.getOpeningAsOfDate().getYear() != request.getYear()
        || request.getOpeningAsOfDate().isAfter(LocalDate.now(ZoneId.of("Asia/Shanghai"))))
      throw exception(PAYROLL_TAX_INVALID);
    for (java.math.BigDecimal value :
        Arrays.asList(
            request.getOpeningIncome(),
            request.getOpeningSocialDeduction(),
            request.getOpeningBasicDeduction(),
            request.getOpeningWithheld())) PayrollTaxPolicyEngine.amount(value);
    if ("ZERO".equals(request.getOpeningMode())
        && (request.getOpeningIncome().signum() != 0
            || request.getOpeningSocialDeduction().signum() != 0
            || request.getOpeningBasicDeduction().signum() != 0
            || request.getOpeningWithheld().signum() != 0)) throw exception(PAYROLL_TAX_INVALID);
    if (Boolean.TRUE.equals(request.getFirstSalaryThisYear())
        && (!Boolean.TRUE.equals(request.getResidentThisYear())
            || !Boolean.TRUE.equals(request.getNoPriorSalaryThisYear())
            || !Boolean.TRUE.equals(request.getNoPriorContinuousRemunerationBeforeEntry())))
      throw exception(PAYROLL_TAX_INVALID);
    if (request.getIdentitySourceProfileId() == null
        && (request.getIdentifier() == null || request.getIdentifier().trim().isEmpty()))
      throw exception(PAYROLL_TAX_INVALID);
    if (request.getIdentitySourceProfileId() != null && request.getIdentifier() != null)
      throw exception(PAYROLL_TAX_INVALID);
    if (request.getIdentitySourceProfileId() == null
        && "ID_CARD".equals(request.getIdentifierType())
        && (!"CN".equals(request.getIdentifierCountry())
            || !cn.hutool.core.util.IdcardUtil.isValidCard(request.getIdentifier())))
      throw exception(PAYROLL_TAX_INVALID);
    entities.check(request.getEntityId(), "MANAGE", actor);
    HrmEmployeeDO employee = employee(request.getEmployeeId());
    if (!Objects.equals(employee.getName(), request.getTaxpayerName()))
      throw exception(PAYROLL_TAX_INVALID);
    HrmPayrollTaxPolicyDO policy =
        policies.requireApproved(request.getEntityId(), request.getYear());
    HrmPayrollTaxProfileDO reused = null;
    String normalized = null, taxpayer;
    if (request.getIdentitySourceProfileId() != null) {
      reused = profiles.selectById(request.getIdentitySourceProfileId());
      if (reused == null
          || !Objects.equals(reused.getEntityId(), request.getEntityId())
          || !Objects.equals(reused.getEmployeeId(), request.getEmployeeId())
          || reused.getYear() > request.getYear()
          || !Arrays.asList("APPROVED", "SUPERSEDED").contains(reused.getStatus())
          || !Objects.equals(reused.getIdentifierType(), request.getIdentifierType())
          || !Objects.equals(reused.getIdentifierCountry(), request.getIdentifierCountry())
          || reused.getIdentifierCipher() == null
          || reused.getTaxpayerKey() == null) throw exception(PAYROLL_TAX_INVALID);
      taxpayer = reused.getTaxpayerKey();
    } else {
      normalized = request.getIdentifier().toUpperCase(Locale.ROOT);
      taxpayer =
          identityKey(request.getIdentifierType(), request.getIdentifierCountry(), normalized);
    }
    String fingerprint = identity.fingerprint("request:" + JsonUtils.toJsonString(request));
    HrmPayrollTaxProfileDO replay =
        profiles.selectOne(HrmPayrollTaxProfileDO::getRequestKey, request.getRequestKey());
    if (replay != null) {
      if (!Objects.equals(replay.getSubmittedBy(), actor)
          || !Objects.equals(replay.getRequestFingerprint(), fingerprint)
          || !Objects.equals(replay.getTaxpayerKey(), taxpayer))
        throw exception(PAYROLL_TAX_INVALID);
      return replay.getId();
    }
    if (reused != null) {
      HrmPayrollTaxProfileDO latestIdentity =
          profiles.selectOne(
              new LambdaQueryWrapperX<HrmPayrollTaxProfileDO>()
                  .eq(HrmPayrollTaxProfileDO::getEntityId, request.getEntityId())
                  .eq(HrmPayrollTaxProfileDO::getEmployeeId, request.getEmployeeId())
                  .le(HrmPayrollTaxProfileDO::getYear, request.getYear())
                  .in(HrmPayrollTaxProfileDO::getStatus, "APPROVED", "SUPERSEDED")
                  .orderByDesc(HrmPayrollTaxProfileDO::getYear)
                  .orderByDesc(HrmPayrollTaxProfileDO::getVersion)
                  .last("LIMIT 1"));
      if (latestIdentity == null || !Objects.equals(latestIdentity.getId(), reused.getId()))
        throw exception(PAYROLL_VERSION_CONFLICT);
    }
    HrmPayrollTaxProfileDO current =
        active(request.getEntityId(), request.getEmployeeId(), request.getYear());
    if (!Objects.equals(request.getPriorProfileId(), current == null ? null : current.getId()))
      throw exception(PAYROLL_VERSION_CONFLICT);
    if (current != null && !Objects.equals(current.getTaxpayerKey(), taxpayer))
      throw exception(PAYROLL_TAX_INVALID);
    List<HrmPayrollTaxProfileDO> old =
        profiles.selectList(
            new LambdaQueryWrapperX<HrmPayrollTaxProfileDO>()
                .eq(HrmPayrollTaxProfileDO::getEntityId, request.getEntityId())
                .eq(HrmPayrollTaxProfileDO::getEmployeeId, request.getEmployeeId())
                .eq(HrmPayrollTaxProfileDO::getYear, request.getYear()));
    HrmPayrollTaxProfileDO row =
        new HrmPayrollTaxProfileDO()
            .setEntityId(request.getEntityId())
            .setEmployeeId(request.getEmployeeId())
            .setYear(request.getYear())
            .setVersion(
                old.stream().mapToInt(HrmPayrollTaxProfileDO::getVersion).max().orElse(0) + 1)
            .setPriorProfileId(request.getPriorProfileId())
            .setIdentitySourceProfileId(request.getIdentitySourceProfileId())
            .setTaxRegistrationNumber(policy.getTaxRegistrationNumber())
            .setTaxpayerName(request.getTaxpayerName())
            .setIdentifierType(request.getIdentifierType())
            .setIdentifierCountry(request.getIdentifierCountry())
            .setIdentifierCipher(
                reused == null ? identity.encrypt(normalized) : reused.getIdentifierCipher())
            .setTaxpayerKey(taxpayer)
            .setMaskedIdentifier(
                reused != null
                    ? reused.getMaskedIdentifier()
                    : normalized.length() <= 4
                        ? "****"
                        : "****" + normalized.substring(normalized.length() - 4))
            .setResidentThisYear(request.getResidentThisYear())
            .setNoPriorContinuousRemunerationBeforeEntry(
                request.getNoPriorContinuousRemunerationBeforeEntry())
            .setFirstSalaryThisYear(request.getFirstSalaryThisYear())
            .setNoPriorSalaryThisYear(request.getNoPriorSalaryThisYear())
            .setSeparateBonusUsedElsewhere(request.getSeparateBonusUsedElsewhere())
            .setOpeningMode(request.getOpeningMode())
            .setOpeningAsOfDate(request.getOpeningAsOfDate())
            .setOpeningIncome(request.getOpeningIncome())
            .setOpeningSocialDeduction(request.getOpeningSocialDeduction())
            .setOpeningBasicDeduction(request.getOpeningBasicDeduction())
            .setOpeningWithheld(request.getOpeningWithheld())
            .setOfficialReference(request.getOfficialReference())
            .setEvidence(request.getEvidence())
            .setEmployeeSnapshot(
                employeeSnapshot(request.getEntityId(), employee, request.getYear()))
            .setRequestKey(request.getRequestKey())
            .setRequestFingerprint(fingerprint)
            .setStatus("PENDING")
            .setSubmittedBy(actor);
    profiles.insert(row);
    event(row, "CREATE", request.getEvidence(), actor);
    return row.getId();
  }

  @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
  public void review(Long id, boolean approved, String reason, Long actor) {
    if (reason == null || reason.trim().isEmpty() || reason.length() > 1000)
      throw exception(PAYROLL_TAX_INVALID);
    HrmPayrollTaxProfileDO row = profiles.selectById(id);
    if (row == null) throw exception(PAYROLL_TAX_INVALID);
    entities.check(row.getEntityId(), "REVIEW", actor);
    HrmEmployeeDO employee = employee(row.getEmployeeId());
    row = profiles.selectOneForUpdate(HrmPayrollTaxProfileDO::getId, id);
    if (!"PENDING".equals(row.getStatus())) throw exception(PAYROLL_TAX_INVALID);
    if (Objects.equals(actor, row.getSubmittedBy()) || Objects.equals(actor, employee.getUserId()))
      throw exception(PAYROLL_SELF_APPROVAL);
    if (approved) {
      if (row.getResidentThisYear() == null
          || row.getNoPriorContinuousRemunerationBeforeEntry() == null
          || Boolean.TRUE.equals(row.getFirstSalaryThisYear())
              && (!Boolean.TRUE.equals(row.getResidentThisYear())
                  || !Boolean.TRUE.equals(row.getNoPriorSalaryThisYear())
                  || !Boolean.TRUE.equals(row.getNoPriorContinuousRemunerationBeforeEntry())))
        throw exception(PAYROLL_TAX_INVALID);
      HrmPayrollTaxProfileDO current =
          active(row.getEntityId(), row.getEmployeeId(), row.getYear());
      if (!Objects.equals(row.getPriorProfileId(), current == null ? null : current.getId()))
        throw exception(PAYROLL_VERSION_CONFLICT);
      if (!Objects.equals(
              row.getEmployeeSnapshot(),
              employeeSnapshot(row.getEntityId(), employee, row.getYear()))
          || !Objects.equals(
              row.getTaxRegistrationNumber(),
              policies
                  .requireApproved(row.getEntityId(), row.getYear())
                  .getTaxRegistrationNumber())) throw exception(PAYROLL_TAX_INVALID);
      if (current != null) {
        current.setStatus("SUPERSEDED");
        profiles.updateById(current);
        event(current, "SUPERSEDE", "后续档案 " + id, actor);
      }
    }
    row.setStatus(approved ? "APPROVED" : "REJECTED");
    row.setReviewedBy(actor);
    row.setReviewOpinion(reason);
    row.setReviewedTime(LocalDateTime.now(ZoneId.of("Asia/Shanghai")));
    profiles.updateById(row);
    event(row, approved ? "APPROVE" : "REJECT", reason, actor);
  }

  public List<HrmPayrollTaxProfileDO> list(
      Long entityId, Integer year, Long employeeId, Long actor) {
    entities.check(entityId, "READ", actor);
    return profiles.selectList(
        new LambdaQueryWrapperX<HrmPayrollTaxProfileDO>()
            .eq(HrmPayrollTaxProfileDO::getEntityId, entityId)
            .eq(HrmPayrollTaxProfileDO::getYear, year)
            .eqIfPresent(HrmPayrollTaxProfileDO::getEmployeeId, employeeId)
            .orderByDesc(HrmPayrollTaxProfileDO::getId));
  }

  public HrmPayrollTaxProfileDO requireApproved(Long entityId, Long employeeId, Integer year) {
    HrmPayrollTaxProfileDO row = active(entityId, employeeId, year);
    HrmEmployeeDO employee = employees.selectById(employeeId);
    if (row == null
        || employee == null
        || !Objects.equals(row.getEmployeeSnapshot(), employeeSnapshot(entityId, employee, year))
        || !Objects.equals(
            row.getTaxRegistrationNumber(),
            policies.requireApproved(entityId, year).getTaxRegistrationNumber()))
      throw exception(PAYROLL_TAX_INVALID);
    return row;
  }

  public Map<String, Object> identitySource(
      Long entityId, Long employeeId, Integer year, Long actor) {
    entities.check(entityId, "MANAGE", actor);
    if (year == null || year < 2019 || year > 2099) throw exception(PAYROLL_TAX_INVALID);
    HrmPayrollTaxProfileDO source =
        profiles.selectOne(
            new LambdaQueryWrapperX<HrmPayrollTaxProfileDO>()
                .eq(HrmPayrollTaxProfileDO::getEntityId, entityId)
                .eq(HrmPayrollTaxProfileDO::getEmployeeId, employeeId)
                .le(HrmPayrollTaxProfileDO::getYear, year)
                .in(HrmPayrollTaxProfileDO::getStatus, "APPROVED", "SUPERSEDED")
                .orderByDesc(HrmPayrollTaxProfileDO::getYear)
                .orderByDesc(HrmPayrollTaxProfileDO::getVersion)
                .last("LIMIT 1"));
    if (source == null) return Collections.emptyMap();
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("id", source.getId());
    out.put("year", source.getYear());
    out.put("identifierType", source.getIdentifierType());
    out.put("identifierCountry", source.getIdentifierCountry());
    out.put("maskedIdentifier", source.getMaskedIdentifier());
    return out;
  }

  @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
  public String privateIdentifier(Long id, Long actor) {
    HrmPayrollTaxProfileDO row = profiles.selectById(id);
    if (row == null) throw exception(PAYROLL_TAX_INVALID);
    entities.check(row.getEntityId(), "READ", actor);
    if (!entities.allowed(row.getEntityId(), "MANAGE", actor)
        && !entities.allowed(row.getEntityId(), "REVIEW", actor))
      throw exception(PAYROLL_ENTITY_INVALID);
    String clear = identity.decrypt(row.getIdentifierCipher());
    if (!Objects.equals(
        row.getTaxpayerKey(),
        identityKey(row.getIdentifierType(), row.getIdentifierCountry(), clear)))
      throw exception(PAYROLL_TAX_INVALID);
    event(row, "READ_PRIVATE_IDENTIFIER", "按授权核实完整税务标识", actor);
    return clear;
  }

  public List<HrmPayrollTaxProfileEventDO> history(Long id, Long actor) {
    HrmPayrollTaxProfileDO row = profiles.selectById(id);
    if (row == null) throw exception(PAYROLL_TAX_INVALID);
    entities.check(row.getEntityId(), "READ", actor);
    return events.selectList(
        new LambdaQueryWrapperX<HrmPayrollTaxProfileEventDO>()
            .eq(HrmPayrollTaxProfileEventDO::getProfileId, id)
            .orderByAsc(HrmPayrollTaxProfileEventDO::getId));
  }
}
