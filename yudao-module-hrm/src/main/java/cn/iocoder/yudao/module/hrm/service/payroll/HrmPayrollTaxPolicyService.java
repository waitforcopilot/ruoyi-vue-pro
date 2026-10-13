package cn.iocoder.yudao.module.hrm.service.payroll;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class HrmPayrollTaxPolicyService {
  @Resource private HrmPayrollTaxPolicyMapper policies;
  @Resource private HrmPayrollTaxPolicyEventMapper events;
  @Resource private HrmPayrollLegalEntityMapper legalEntities;
  @Resource private HrmPayrollEntityService entities;
  @Resource private javax.validation.Validator validator;

  private void reason(String value) {
    if (value == null || value.trim().isEmpty() || value.length() > 1000)
      throw exception(PAYROLL_TAX_INVALID);
  }

  private HrmPayrollLegalEntityDO legal(Long id, boolean lock) {
    HrmPayrollLegalEntityDO entity =
        lock
            ? legalEntities.selectOneForUpdate(HrmPayrollLegalEntityDO::getId, id)
            : legalEntities.selectById(id);
    if (entity == null
        || !"APPROVED".equals(entity.getStatus())
        || entity.getTaxRegistrationNumber() == null
        || entity.getTaxRegistrationNumber().trim().isEmpty()) throw exception(PAYROLL_TAX_INVALID);
    if (legalEntities.selectCount(
            new LambdaQueryWrapperX<HrmPayrollLegalEntityDO>()
                .eq(
                    HrmPayrollLegalEntityDO::getTaxRegistrationNumber,
                    entity.getTaxRegistrationNumber())
                .eq(HrmPayrollLegalEntityDO::getStatus, "APPROVED")
                .ne(HrmPayrollLegalEntityDO::getId, id))
        > 0) throw exception(PAYROLL_TAX_INVALID);
    return entity;
  }

  private HrmPayrollTaxPolicyDO active(Long entityId, Integer year) {
    return policies.selectOne(
        new LambdaQueryWrapperX<HrmPayrollTaxPolicyDO>()
            .eq(HrmPayrollTaxPolicyDO::getEntityId, entityId)
            .eq(HrmPayrollTaxPolicyDO::getYear, year)
            .eq(HrmPayrollTaxPolicyDO::getStatus, "APPROVED"));
  }

  private void event(HrmPayrollTaxPolicyDO row, String action, String reason, Long actor) {
    events.insert(
        new HrmPayrollTaxPolicyEventDO()
            .setPolicyId(row.getId())
            .setAction(action)
            .setActorId(actor)
            .setReason(reason)
            .setSnapshot(JsonUtils.toJsonString(row)));
  }

  public HrmPayrollTaxPolicyReqVO defaults(Long entityId, Integer year, Long actor) {
    entities.check(entityId, "READ", actor);
    legal(entityId, false);
    if (year == null || year < 2019 || year > 2099) throw exception(PAYROLL_TAX_INVALID);
    HrmPayrollTaxPolicyReqVO result =
        new HrmPayrollTaxPolicyReqVO()
            .setEntityId(entityId)
            .setYear(year)
            .setName(year + "年居民工资及年终奖税务政策")
            .setMonthlyBasicDeduction(new BigDecimal("5000"))
            .setSeparateBonusAllowed(year <= 2027)
            .setSeparateBonusValidTo(LocalDate.of(2027, 12, 31))
            .setOfficialReference("")
            .setEvidence("");
    result.setResidentBands(
        defaultBands(
            new String[] {"36000", "144000", "300000", "420000", "660000", "960000"},
            new String[] {"0", "2520", "16920", "31920", "52920", "85920", "181920"}));
    result.setBonusBands(
        defaultBands(
            new String[] {"3000", "12000", "25000", "35000", "55000", "80000"},
            new String[] {"0", "210", "1410", "2660", "4410", "7160", "15160"}));
    HrmPayrollTaxPolicyDO prior = active(entityId, year);
    result.setPriorPolicyId(prior == null ? null : prior.getId());
    return result;
  }

  private List<HrmPayrollTaxPolicyReqVO.Band> defaultBands(String[] bounds, String[] quick) {
    List<HrmPayrollTaxPolicyReqVO.Band> rows = new ArrayList<>();
    String[] rates = {"0.03", "0.10", "0.20", "0.25", "0.30", "0.35", "0.45"};
    for (int i = 0; i < rates.length; i++)
      rows.add(
          new HrmPayrollTaxPolicyReqVO.Band()
              .setUpper(i < bounds.length ? new BigDecimal(bounds[i]) : null)
              .setRate(new BigDecimal(rates[i]))
              .setQuickDeduction(new BigDecimal(quick[i])));
    return rows;
  }

  private void validate(HrmPayrollTaxPolicyReqVO request) {
    if (!validator.validate(request).isEmpty()) throw exception(PAYROLL_TAX_INVALID);
    PayrollTaxPolicyEngine.amount(request.getMonthlyBasicDeduction());
    PayrollTaxPolicyEngine.bands(request.getResidentBands());
    PayrollTaxPolicyEngine.bands(request.getBonusBands());
    if (Boolean.TRUE.equals(request.getSeparateBonusAllowed())
        && LocalDate.of(request.getYear(), 12, 31).isAfter(request.getSeparateBonusValidTo()))
      throw exception(PAYROLL_TAX_INVALID);
  }

  @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
  public Long create(HrmPayrollTaxPolicyReqVO request, Long actor) {
    validate(request);
    entities.check(request.getEntityId(), "MANAGE", actor);
    HrmPayrollLegalEntityDO entity = legal(request.getEntityId(), true);
    HrmPayrollTaxPolicyDO replay =
        policies.selectOne(HrmPayrollTaxPolicyDO::getRequestKey, request.getRequestKey());
    if (replay != null) {
      if (!Objects.equals(replay.getSubmittedBy(), actor)
          || !Objects.equals(replay.getRequestSnapshot(), JsonUtils.toJsonString(request))
          || !Objects.equals(replay.getTaxRegistrationNumber(), entity.getTaxRegistrationNumber()))
        throw exception(PAYROLL_TAX_INVALID);
      return replay.getId();
    }
    HrmPayrollTaxPolicyDO current = active(request.getEntityId(), request.getYear());
    if (!Objects.equals(request.getPriorPolicyId(), current == null ? null : current.getId()))
      throw exception(PAYROLL_VERSION_CONFLICT);
    List<HrmPayrollTaxPolicyDO> previous =
        policies.selectList(
            new LambdaQueryWrapperX<HrmPayrollTaxPolicyDO>()
                .eq(HrmPayrollTaxPolicyDO::getEntityId, request.getEntityId())
                .eq(HrmPayrollTaxPolicyDO::getYear, request.getYear()));
    HrmPayrollTaxPolicyDO row =
        new HrmPayrollTaxPolicyDO()
            .setEntityId(request.getEntityId())
            .setRequestKey(request.getRequestKey())
            .setRequestSnapshot(JsonUtils.toJsonString(request))
            .setYear(request.getYear())
            .setVersion(
                previous.stream().mapToInt(HrmPayrollTaxPolicyDO::getVersion).max().orElse(0) + 1)
            .setPriorPolicyId(request.getPriorPolicyId())
            .setTaxRegistrationNumber(entity.getTaxRegistrationNumber())
            .setName(request.getName())
            .setMonthlyBasicDeduction(request.getMonthlyBasicDeduction())
            .setResidentBands(JsonUtils.toJsonString(request.getResidentBands()))
            .setBonusBands(JsonUtils.toJsonString(request.getBonusBands()))
            .setSeparateBonusAllowed(request.getSeparateBonusAllowed())
            .setSeparateBonusValidTo(request.getSeparateBonusValidTo())
            .setOfficialReference(request.getOfficialReference())
            .setEvidence(request.getEvidence())
            .setStatus("PENDING")
            .setSubmittedBy(actor);
    policies.insert(row);
    event(row, "CREATE", request.getEvidence(), actor);
    return row.getId();
  }

  @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
  public void review(Long id, boolean approved, String reason, Long actor) {
    reason(reason);
    HrmPayrollTaxPolicyDO row = policies.selectById(id);
    if (row == null) throw exception(PAYROLL_TAX_INVALID);
    entities.check(row.getEntityId(), "APPROVE", actor);
    HrmPayrollLegalEntityDO entity = legal(row.getEntityId(), true);
    row = policies.selectOneForUpdate(HrmPayrollTaxPolicyDO::getId, id);
    if (!"PENDING".equals(row.getStatus())
        || !Objects.equals(entity.getTaxRegistrationNumber(), row.getTaxRegistrationNumber()))
      throw exception(PAYROLL_TAX_INVALID);
    if (Objects.equals(actor, row.getSubmittedBy())) throw exception(PAYROLL_SELF_APPROVAL);
    if (approved) {
      HrmPayrollTaxPolicyDO current = active(row.getEntityId(), row.getYear());
      if (!Objects.equals(row.getPriorPolicyId(), current == null ? null : current.getId()))
        throw exception(PAYROLL_VERSION_CONFLICT);
      if (current != null) {
        current.setStatus("SUPERSEDED");
        policies.updateById(current);
        event(current, "SUPERSEDE", "接续政策 " + id, actor);
      }
    }
    row.setStatus(approved ? "APPROVED" : "REJECTED");
    row.setApprovedBy(actor);
    row.setReviewOpinion(reason);
    row.setApprovedTime(LocalDateTime.now(ZoneId.of("Asia/Shanghai")));
    policies.updateById(row);
    event(row, approved ? "APPROVE" : "REJECT", reason, actor);
  }

  public List<HrmPayrollTaxPolicyDO> list(Long entityId, Integer year, Long actor) {
    entities.check(entityId, "READ", actor);
    return policies.selectList(
        new LambdaQueryWrapperX<HrmPayrollTaxPolicyDO>()
            .eq(HrmPayrollTaxPolicyDO::getEntityId, entityId)
            .eqIfPresent(HrmPayrollTaxPolicyDO::getYear, year)
            .orderByDesc(HrmPayrollTaxPolicyDO::getYear, HrmPayrollTaxPolicyDO::getVersion));
  }

  public HrmPayrollTaxPolicyDO requireApproved(Long entityId, Integer year) {
    HrmPayrollLegalEntityDO entity = legal(entityId, false);
    HrmPayrollTaxPolicyDO row = active(entityId, year);
    if (row == null
        || !Objects.equals(entity.getTaxRegistrationNumber(), row.getTaxRegistrationNumber()))
      throw exception(PAYROLL_TAX_INVALID);
    return row;
  }

  public PayrollTaxPolicyEngine.Quote trial(HrmPayrollTaxPolicyTrialReqVO request, Long actor) {
    if (!validator.validate(request).isEmpty()) throw exception(PAYROLL_TAX_INVALID);
    HrmPayrollTaxPolicyDO policy = policies.selectById(request.getPolicyId());
    if (policy == null) throw exception(PAYROLL_TAX_INVALID);
    entities.check(policy.getEntityId(), "READ", actor);
    HrmPayrollLegalEntityDO entity = legal(policy.getEntityId(), false);
    if (!Objects.equals(entity.getTaxRegistrationNumber(), policy.getTaxRegistrationNumber()))
      throw exception(PAYROLL_TAX_INVALID);
    return PayrollTaxPolicyEngine.quote(policy, request);
  }

  public List<HrmPayrollTaxPolicyEventDO> history(Long id, Long actor) {
    HrmPayrollTaxPolicyDO row = policies.selectById(id);
    if (row == null) throw exception(PAYROLL_TAX_INVALID);
    entities.check(row.getEntityId(), "READ", actor);
    return events.selectList(
        new LambdaQueryWrapperX<HrmPayrollTaxPolicyEventDO>()
            .eq(HrmPayrollTaxPolicyEventDO::getPolicyId, id)
            .orderByAsc(HrmPayrollTaxPolicyEventDO::getId));
  }
}
