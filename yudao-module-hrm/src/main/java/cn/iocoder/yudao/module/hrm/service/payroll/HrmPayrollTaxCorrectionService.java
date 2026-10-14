package cn.iocoder.yudao.module.hrm.service.payroll;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.salary.monthrecord.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.employee.info.HrmEmployeeMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.salary.monthrecord.HrmSalaryMonthRecordMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import javax.annotation.Resource;
import javax.validation.Validator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** Tax-source amendments do not edit wages, transfer amounts or bank value dates. */
@Service
public class HrmPayrollTaxCorrectionService {
  @Resource private HrmPayrollTaxCorrectionMapper corrections;
  @Resource private HrmPayrollPaymentMapper payments;
  @Resource private HrmPayrollRunMapper runs;
  @Resource private HrmPayrollSourceClaimMapper claims;
  @Resource private HrmSalaryMonthRecordMapper batches;
  @Resource private HrmEmployeeMapper employees;
  @Resource private HrmPayrollBatchEventMapper events;
  @Resource private HrmPayrollEntityService entities;
  @Resource private HrmPayrollTaxProfileService profiles;
  @Resource private HrmPayrollTaxPolicyService policies;
  @Resource private HrmPayrollTaxEmploymentService employment;
  @Resource private Validator validator;

  public static String digest(HrmPayrollPaymentDO payment, HrmPayrollRunDO run) {
    Map<String, Object> facts = new LinkedHashMap<>();
    facts.put("paymentId", payment.getId());
    facts.put("runId", payment.getRunId());
    facts.put("employeeId", payment.getEmployeeId());
    facts.put("valueDate", payment.getValueDate());
    facts.put("amount", payment.getAmount());
    facts.put("status", payment.getStatus());
    facts.put("zeroSettlementReference", payment.getZeroSettlementReference());
    facts.put("resultSnapshot", run.getResultSnapshot());
    facts.put("inputSnapshot", run.getInputSnapshot());
    return DigestUtil.sha256Hex(JsonUtils.toJsonString(facts));
  }

  private HrmPayrollTaxCorrectionDO active(Long paymentId) {
    return corrections.selectOne(
        new LambdaQueryWrapper<HrmPayrollTaxCorrectionDO>()
            .eq(HrmPayrollTaxCorrectionDO::getPaymentId, paymentId)
            .eq(HrmPayrollTaxCorrectionDO::getStatus, "APPROVED"));
  }

  private HrmSalaryMonthRecordDO batch(
      HrmPayrollPaymentDO p, String action, Long actor, boolean lock) {
    if (p == null) throw exception(PAYROLL_TAX_INVALID);
    HrmSalaryMonthRecordDO b = batches.selectById(p.getMonthRecordId());
    if (b == null || b.getEntityId() == null) throw exception(PAYROLL_ENTITY_INVALID);
    entities.check(b.getEntityId(), action, actor);
    if (lock) {
      entities.lockApproved(b.getEntityId());
      b = batches.selectByIdForUpdate(b.getId());
    }
    return b;
  }

  public HrmPayrollTaxCorrectionDO preview(HrmPayrollTaxCorrectionReqVO request, Long actor) {
    if (!validator.validate(request).isEmpty()) throw exception(PAYROLL_TAX_INVALID);
    HrmPayrollPaymentDO payment = payments.selectById(request.getPaymentId());
    HrmSalaryMonthRecordDO b = batch(payment, "MANAGE", actor, false);
    return prepare(request, payment, b);
  }

  private HrmPayrollTaxCorrectionDO prepare(
      HrmPayrollTaxCorrectionReqVO request, HrmPayrollPaymentDO payment, HrmSalaryMonthRecordDO b) {
    if (!"SUCCESS".equals(payment.getStatus())
        || payment.getValueDate() == null
        || payment.getValueDate().isAfter(LocalDate.now(ZoneId.of("Asia/Shanghai"))))
      throw exception(PAYROLL_TAX_INVALID);
    HrmPayrollRunDO run = runs.selectById(payment.getRunId());
    HrmPayrollTaxCashLedgerVO.Row original = new HrmPayrollTaxCashLedgerVO.Row();
    try {
      HrmPayrollTaxCashLedgerService.populate(original, payment, run, false);
    } catch (RuntimeException ex) {
      throw exception(PAYROLL_TAX_INVALID);
    }
    HrmSalaryMonthEmployeeRecordDO result =
        JsonUtils.parseArray(run.getResultSnapshot(), HrmSalaryMonthEmployeeRecordDO.class).stream()
            .filter(r -> Objects.equals(r.getEmployeeId(), payment.getEmployeeId()))
            .findFirst()
            .orElseThrow(() -> exception(PAYROLL_TAX_INVALID));
    HrmPayrollTaxWithholdingService.Source source =
        JsonUtils.parseObject(
            result.getTaxSourceSnapshot(), HrmPayrollTaxWithholdingService.Source.class);
    HrmPayrollTaxProfileDO profile =
        profiles.requireApproved(
            b.getEntityId(), payment.getEmployeeId(), payment.getValueDate().getYear());
    HrmPayrollTaxPolicyDO policy =
        policies.requireApproved(b.getEntityId(), payment.getValueDate().getYear());
    PayrollTaxEmploymentEngine.Result entitlement =
        employment.forApprovedPayroll(
            b.getEntityId(), payment.getEmployeeId(), payment.getValueDate());
    if (source == null
        || source.getProfile() == null
        || !Boolean.TRUE.equals(profile.getResidentThisYear())
        || !Objects.equals(profile.getTaxpayerKey(), source.getProfile().getTaxpayerKey())
        || !Objects.equals(profile.getTaxRegistrationNumber(), policy.getTaxRegistrationNumber())
        || request
                .getBasicDeduction()
                .compareTo(policy.getMonthlyBasicDeduction().multiply(BigDecimal.valueOf(12)))
            > 0) throw exception(PAYROLL_TAX_INVALID);
    if (entitlement == null
        || entitlement.getBasicDeduction() == null
        || request.getBasicDeduction().compareTo(entitlement.getBasicDeduction()) > 0)
      throw exception(PAYROLL_TAX_INVALID);
    HrmPayrollTaxCorrectionDO prior = active(payment.getId());
    HrmPayrollTaxCorrectionDO row = new HrmPayrollTaxCorrectionDO();
    row.setPaymentId(payment.getId());
    row.setEntityId(b.getEntityId());
    row.setEmployeeId(payment.getEmployeeId());
    row.setRunId(run.getId());
    row.setPriorCorrectionId(prior == null ? null : prior.getId());
    row.setVersion(
        corrections
                .selectList(
                    new LambdaQueryWrapper<HrmPayrollTaxCorrectionDO>()
                        .eq(HrmPayrollTaxCorrectionDO::getPaymentId, payment.getId()))
                .stream()
                .map(HrmPayrollTaxCorrectionDO::getVersion)
                .max(Integer::compareTo)
                .orElse(0)
            + 1);
    row.setValueDate(payment.getValueDate());
    row.setSourceDigest(digest(payment, run));
    row.setPolicyId(policy.getId());
    row.setPolicyVersion(policy.getVersion());
    row.setProfileId(profile.getId());
    row.setProfileVersion(profile.getVersion());
    row.setTaxRegistrationNumber(profile.getTaxRegistrationNumber());
    row.setTaxpayerKey(profile.getTaxpayerKey());
    row.setIncome(original.getIncome());
    row.setSocialDeduction(original.getSocialDeduction());
    row.setBasicDeduction(request.getBasicDeduction());
    row.setActualSalaryWithheld(original.getWithheld());
    row.setSeparateBonusIncome(original.getSeparateBonusIncome());
    row.setActualBonusWithheld(original.getSeparateBonusTax());
    row.setOriginalSnapshot(JsonUtils.toJsonString(original));
    row.setOfficialReference(request.getOfficialReference());
    row.setEvidence(request.getEvidence());
    row.setReason(request.getReason());
    row.setRequestKey(request.getRequestKey());
    row.setRequestSnapshot(JsonUtils.toJsonString(request));
    return row;
  }

  @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
  public Long submit(HrmPayrollTaxCorrectionReqVO request, Long actor) {
    if (!validator.validate(request).isEmpty()) throw exception(PAYROLL_TAX_INVALID);
    HrmPayrollPaymentDO candidate = payments.selectById(request.getPaymentId());
    HrmSalaryMonthRecordDO b = batch(candidate, "MANAGE", actor, true);
    HrmPayrollPaymentDO payment =
        payments.selectOneForUpdate(HrmPayrollPaymentDO::getId, candidate.getId());
    HrmPayrollTaxCorrectionDO existing =
        corrections.selectOne(HrmPayrollTaxCorrectionDO::getRequestKey, request.getRequestKey());
    if (existing != null) {
      if (!Objects.equals(existing.getEntityId(), b.getEntityId())
          || !Objects.equals(existing.getRequestSnapshot(), JsonUtils.toJsonString(request)))
        throw exception(PAYROLL_VERSION_CONFLICT);
      return existing.getId();
    }
    HrmPayrollTaxCorrectionDO row = prepare(request, payment, b);
    row.setStatus("PENDING");
    row.setSubmittedBy(actor);
    corrections.insert(row);
    event(row, b, "SUBMIT", request.getReason(), actor);
    return row.getId();
  }

  @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
  public void review(Long id, boolean approve, String reason, Long actor) {
    if (reason == null || reason.trim().isEmpty() || reason.length() > 1000)
      throw exception(PAYROLL_TAX_INVALID);
    HrmPayrollTaxCorrectionDO candidate = corrections.selectById(id);
    if (candidate == null) throw exception(PAYROLL_TAX_INVALID);
    HrmSalaryMonthRecordDO b =
        batch(payments.selectById(candidate.getPaymentId()), "REVIEW", actor, true);
    HrmPayrollTaxCorrectionDO row =
        corrections.selectOneForUpdate(HrmPayrollTaxCorrectionDO::getId, id);
    if (!"PENDING".equals(row.getStatus())) throw exception(PAYROLL_TAX_INVALID);
    cn.iocoder.yudao.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO employee =
        employees.selectByIdForUpdate(row.getEmployeeId());
    if (employee == null
        || Objects.equals(actor, row.getSubmittedBy())
        || Objects.equals(actor, employee.getUserId())) throw exception(PAYROLL_SELF_APPROVAL);
    if (approve) {
      for (Integer year :
          new HashSet<>(
              Arrays.asList(
                  row.getValueDate().getYear(),
                  batches.selectById(b.getId()).getExpectedPaymentDate() == null
                      ? row.getValueDate().getYear()
                      : batches.selectById(b.getId()).getExpectedPaymentDate().getYear()))) {
        String prefix =
            DigestUtil.sha256Hex(row.getEntityId() + ":" + row.getTaxpayerKey() + ":" + year) + ":";
        if (!claims
            .selectList(
                new LambdaQueryWrapper<HrmPayrollSourceClaimDO>()
                    .eq(HrmPayrollSourceClaimDO::getEntityId, row.getEntityId())
                    .eq(HrmPayrollSourceClaimDO::getKind, "TAX_YEAR")
                    .eq(HrmPayrollSourceClaimDO::getStatus, "RESERVED")
                    .likeRight(HrmPayrollSourceClaimDO::getSourceKey, prefix))
            .isEmpty()) throw exception(PAYROLL_SOURCE_CLAIM_INVALID);
      }
      HrmPayrollPaymentDO payment =
          payments.selectOneForUpdate(HrmPayrollPaymentDO::getId, row.getPaymentId());
      HrmPayrollTaxCorrectionDO fresh =
          prepare(
              JsonUtils.parseObject(row.getRequestSnapshot(), HrmPayrollTaxCorrectionReqVO.class),
              payment,
              b);
      if (!Objects.equals(row.getSourceDigest(), fresh.getSourceDigest())
          || !Objects.equals(row.getPolicyId(), fresh.getPolicyId())
          || !Objects.equals(row.getProfileId(), fresh.getProfileId())
          || !Objects.equals(row.getPriorCorrectionId(), fresh.getPriorCorrectionId()))
        throw exception(PAYROLL_VERSION_CONFLICT);
      HrmPayrollTaxCorrectionDO prior = active(row.getPaymentId());
      if (prior != null) {
        prior.setStatus("SUPERSEDED");
        corrections.updateById(prior);
        event(prior, b, "SUPERSEDE", "接续更正" + id, actor);
      }
    }
    row.setStatus(approve ? "APPROVED" : "REJECTED");
    row.setApprovedBy(actor);
    row.setApprovedTime(LocalDateTime.now());
    row.setReviewOpinion(reason);
    corrections.updateById(row);
    event(row, b, approve ? "APPROVE" : "REJECT", reason, actor);
  }

  public List<HrmPayrollTaxCorrectionDO> list(Long paymentId, Long actor) {
    batch(payments.selectById(paymentId), "READ", actor, false);
    return corrections.selectList(
        new LambdaQueryWrapper<HrmPayrollTaxCorrectionDO>()
            .eq(HrmPayrollTaxCorrectionDO::getPaymentId, paymentId)
            .orderByDesc(HrmPayrollTaxCorrectionDO::getVersion));
  }

  private void event(
      HrmPayrollTaxCorrectionDO row,
      HrmSalaryMonthRecordDO b,
      String action,
      String reason,
      Long actor) {
    events.insert(
        new HrmPayrollBatchEventDO()
            .setMonthRecordId(b.getId())
            .setRunId(row.getRunId())
            .setAction("tax_source_correction_" + action)
            .setActorId(actor)
            .setFromStatus(b.getStatus())
            .setToStatus(b.getStatus())
            .setReason(reason)
            .setSnapshot(JsonUtils.toJsonString(row)));
  }
}
