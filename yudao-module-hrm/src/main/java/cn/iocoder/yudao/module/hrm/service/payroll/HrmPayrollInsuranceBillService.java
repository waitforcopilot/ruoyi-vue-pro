package cn.iocoder.yudao.module.hrm.service.payroll;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.employee.info.HrmEmployeeMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.*;
import java.math.*;
import java.time.*;
import java.util.*;
import javax.annotation.Resource;
import javax.validation.Validator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Approved statutory statements and evidenced payments/refunds. Payroll deductions are not
 * repeated.
 */
@Service
public class HrmPayrollInsuranceBillService {
  @Resource private HrmPayrollInsuranceBillMapper bills;
  @Resource private HrmPayrollInsuranceBillLineMapper lines;
  @Resource private HrmPayrollInsuranceSettlementMapper settlements;
  @Resource private HrmPayrollInsurancePolicyService policies;
  @Resource private HrmPayrollEntityService entities;
  @Resource private HrmEmployeeMapper employees;
  @Resource private Validator validator;
  @Resource private HrmPayrollInsuranceEventMapper events;

  private String text(String v) {
    return v == null ? "" : v.trim();
  }

  private void reason(String v) {
    if (text(v).isEmpty() || v.length() > 1000) throw exception(PAYROLL_INSURANCE_INVALID);
  }

  private void event(Long entity, String action, String reason, Object snapshot, Long actor) {
    HrmPayrollInsuranceEventDO e = new HrmPayrollInsuranceEventDO();
    e.setEntityId(entity);
    e.setAction(action);
    e.setReason(reason);
    e.setActorId(actor);
    e.setSnapshot(JsonUtils.toJsonString(snapshot));
    events.insert(e);
  }

  private BigDecimal amount(String v) {
    if (text(v).isEmpty()) throw new IllegalArgumentException("实缴金额不能为空，真实零值请明确填 0");
    BigDecimal n;
    try {
      n = new BigDecimal(text(v));
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("实缴金额须为元，最多两位小数");
    }
    if (n.signum() < 0 || n.scale() > 2 || (long) n.precision() - n.scale() > 10)
      throw new IllegalArgumentException("实缴金额须非负且最多两位小数");
    return n.setScale(2, RoundingMode.UNNECESSARY);
  }

  private String key(Long employee, String project) {
    return employee + ":" + project;
  }

  private List<HrmPayrollInsuranceBillLineDO> comparison(
      Map<Long, HrmPayrollInsurancePolicyService.Quote> quotes) {
    List<HrmPayrollInsuranceBillLineDO> result = new ArrayList<>();
    for (HrmPayrollInsurancePolicyService.Quote quote : quotes.values()) {
      HrmEmployeeDO employee = employees.selectById(quote.getEmployeeId());
      if (employee == null) throw exception(EMPLOYEE_NOT_EXISTS);
      for (HrmPayrollInsurancePolicyService.ProjectAmount p : quote.getProjects()) {
        HrmPayrollInsuranceBillLineDO line = new HrmPayrollInsuranceBillLineDO();
        line.setEmployeeId(employee.getId());
        line.setJobNumber(employee.getJobNumber());
        line.setEmployeeName(employee.getName());
        line.setProjectCode(p.getCode());
        line.setProjectName(p.getName());
        line.setExpectedPersonal(p.getPersonalAmount());
        line.setExpectedCorporate(p.getCorporateAmount());
        line.setRevision(0L);
        line.setStatus("MISSING");
        result.add(line);
      }
    }
    return result;
  }

  @Transactional(rollbackFor = Exception.class)
  public HrmPayrollInsuranceBillDO preview(
      Long entityId,
      YearMonth month,
      String reference,
      String evidence,
      String fileName,
      String digest,
      List<HrmPayrollInsuranceBillRowVO> rows,
      Long actor) {
    return previewVersion(
        entityId, month, reference, evidence, fileName, digest, rows, actor, false);
  }

  private HrmPayrollInsuranceBillDO previewVersion(
      Long entityId,
      YearMonth month,
      String reference,
      String evidence,
      String fileName,
      String digest,
      List<HrmPayrollInsuranceBillRowVO> rows,
      Long actor,
      boolean renew) {
    entities.check(entityId, "MANAGE", actor);
    entities.lockApproved(entityId);
    reason(evidence);
    if (month == null
        || month.getYear() < 2000
        || month.getYear() > 2100
        || text(reference).isEmpty()
        || reference.length() > 128
        || fileName == null
        || fileName.length() > 255
        || digest == null
        || !digest.matches("[a-f0-9]{64}")
        || rows == null
        || rows.isEmpty()
        || rows.size() > 50000) throw exception(PAYROLL_INSURANCE_INVALID);
    HrmPayrollInsuranceBillDO prior =
        bills.selectOne(
            new LambdaQueryWrapperX<HrmPayrollInsuranceBillDO>()
                .eq(HrmPayrollInsuranceBillDO::getEntityId, entityId)
                .eq(HrmPayrollInsuranceBillDO::getYear, month.getYear())
                .eq(HrmPayrollInsuranceBillDO::getMonth, month.getMonthValue())
                .eq(HrmPayrollInsuranceBillDO::getDigest, digest)
                .orderByDesc(HrmPayrollInsuranceBillDO::getVersion)
                .last("LIMIT 1"));
    if (prior != null && !renew) return prior;
    if (renew
        && (prior == null
            || !Objects.equals(prior.getCreator(), String.valueOf(actor))
            || !Arrays.asList("VALIDATED", "FAILED", "REJECTED").contains(prior.getStatus())))
      throw exception(PAYROLL_INSURANCE_INVALID);
    Map<Long, HrmPayrollInsurancePolicyService.Quote> sources =
        policies.reconciliationSources(entityId, month, actor);
    List<HrmPayrollInsuranceBillLineDO> compared = comparison(sources);
    Map<String, HrmPayrollInsuranceBillLineDO> index = new LinkedHashMap<>();
    compared.forEach(l -> index.put(key(l.getEmployeeId(), l.getProjectCode()), l));
    Set<String> seen = new HashSet<>();
    List<HrmPayrollInsuranceImportService.RowError> errors = new ArrayList<>();
    for (int i = 0; i < rows.size(); i++) {
      HrmPayrollInsuranceBillRowVO row = rows.get(i);
      try {
        if (!month.toString().equals(text(row.getMonth())))
          throw new IllegalArgumentException("缴费月份须与账单期间一致");
        if (text(row.getJobNumber()).isEmpty()) throw new IllegalArgumentException("工号不能为空");
        List<HrmEmployeeDO> matches =
            employees.selectList(
                new LambdaQueryWrapperX<HrmEmployeeDO>()
                    .eq(HrmEmployeeDO::getJobNumber, text(row.getJobNumber())));
        if (matches.size() != 1) throw new IllegalArgumentException("工号未唯一匹配到员工");
        String k = key(matches.get(0).getId(), text(row.getProjectCode()));
        if (!seen.add(k)) throw new IllegalArgumentException("同员工同项目重复");
        HrmPayrollInsuranceBillLineDO line = index.get(k);
        if (line == null) throw new IllegalArgumentException("未匹配批准的本主体申报或政策项目，请先核实缴费主体及来源");
        BigDecimal personal = amount(row.getPersonal()), corporate = amount(row.getCorporate());
        line.setOriginalPersonal(personal);
        line.setOriginalCorporate(corporate);
        line.setActualPersonal(personal);
        line.setActualCorporate(corporate);
        refresh(line);
      } catch (IllegalArgumentException ex) {
        errors.add(
            new HrmPayrollInsuranceImportService.RowError(
                i + 2, text(row.getJobNumber()), ex.getMessage()));
      }
    }
    HrmPayrollInsuranceBillDO bill = new HrmPayrollInsuranceBillDO();
    bill.setEntityId(entityId);
    bill.setVersion(prior == null ? 1 : prior.getVersion() + 1);
    bill.setReplacesId(renew ? prior.getId() : null);
    bill.setYear(month.getYear());
    bill.setMonth(month.getMonthValue());
    bill.setFileName(fileName);
    bill.setDigest(digest);
    bill.setReference(reference);
    bill.setEvidence(evidence);
    bill.setSources(JsonUtils.toJsonString(sources));
    bill.setRowsSnapshot(JsonUtils.toJsonString(rows));
    bill.setErrors(JsonUtils.toJsonString(errors));
    bill.setComparison(JsonUtils.toJsonString(compared));
    bill.setCreator(String.valueOf(actor));
    bill.setStatus(errors.isEmpty() ? "VALIDATED" : "FAILED");
    bills.insert(bill);
    if (renew) {
      event(entityId, "SUPERSEDE_BILL", evidence, prior, actor);
      prior.setStatus("SUPERSEDED");
      bills.updateById(prior);
    }
    event(entityId, "PREVIEW_BILL", evidence, bill, actor);
    return bill;
  }

  @Transactional(rollbackFor = Exception.class)
  public HrmPayrollInsuranceBillDO revalidate(Long id, Long actor) {
    HrmPayrollInsuranceBillDO old = bill(id, "MANAGE", actor, true);
    if (!Objects.equals(old.getCreator(), String.valueOf(actor))
        || !Arrays.asList("VALIDATED", "FAILED", "REJECTED").contains(old.getStatus()))
      throw exception(PAYROLL_INSURANCE_INVALID);
    return previewVersion(
        old.getEntityId(),
        YearMonth.of(old.getYear(), old.getMonth()),
        old.getReference(),
        old.getEvidence(),
        old.getFileName(),
        old.getDigest(),
        JsonUtils.parseArray(old.getRowsSnapshot(), HrmPayrollInsuranceBillRowVO.class),
        actor,
        true);
  }

  private HrmPayrollInsuranceBillDO bill(Long id, String action, Long actor, boolean lock) {
    HrmPayrollInsuranceBillDO bill = bills.selectById(id);
    if (bill == null) throw exception(PAYROLL_INSURANCE_INVALID);
    entities.check(bill.getEntityId(), action, actor);
    if (lock) {
      entities.lockApproved(bill.getEntityId());
      bill = bills.selectOneForUpdate(HrmPayrollInsuranceBillDO::getId, id);
    }
    return bill;
  }

  public HrmPayrollInsuranceBillDO get(Long id, Long actor) {
    return bill(id, "READ", actor, false);
  }

  public List<HrmPayrollInsuranceBillDO> list(Long entityId, Long actor) {
    entities.check(entityId, "READ", actor);
    List<HrmPayrollInsuranceBillDO> result =
        bills.selectList(
            new LambdaQueryWrapperX<HrmPayrollInsuranceBillDO>()
                .eq(HrmPayrollInsuranceBillDO::getEntityId, entityId)
                .orderByDesc(HrmPayrollInsuranceBillDO::getId));
    for (HrmPayrollInsuranceBillDO b : result) {
      b.setSources(null);
      b.setRowsSnapshot(null);
      b.setComparison(null);
    }
    return result;
  }

  private void independent(String creator, Long reviewer, Long actor) {
    if (Objects.equals(creator, String.valueOf(actor)) || Objects.equals(reviewer, actor))
      throw exception(PAYROLL_SELF_APPROVAL);
  }

  private void currentSources(HrmPayrollInsuranceBillDO b, Long actor) {
    Map<Long, HrmPayrollInsurancePolicyService.Quote> now =
        policies.reconciliationSourcesForApproval(
            b.getEntityId(), YearMonth.of(b.getYear(), b.getMonth()));
    if (!JsonUtils.parseTree(b.getSources())
        .equals(JsonUtils.parseTree(JsonUtils.toJsonString(now))))
      throw exception(PAYROLL_INSURANCE_INVALID);
  }

  @Transactional(rollbackFor = Exception.class)
  public void submit(Long id, Long actor) {
    HrmPayrollInsuranceBillDO b = bill(id, "MANAGE", actor, true);
    if (!Objects.equals(b.getCreator(), String.valueOf(actor)))
      throw exception(PAYROLL_INSURANCE_INVALID);
    if (Arrays.asList("HR_REVIEW", "FINANCE_REVIEW", "APPROVED", "RECONCILED")
        .contains(b.getStatus())) return;
    if (!"VALIDATED".equals(b.getStatus())) throw exception(PAYROLL_INSURANCE_INVALID);
    if (bills.selectCount(
            new LambdaQueryWrapperX<HrmPayrollInsuranceBillDO>()
                .eq(HrmPayrollInsuranceBillDO::getEntityId, b.getEntityId())
                .eq(HrmPayrollInsuranceBillDO::getYear, b.getYear())
                .eq(HrmPayrollInsuranceBillDO::getMonth, b.getMonth())
                .in(
                    HrmPayrollInsuranceBillDO::getStatus,
                    "HR_REVIEW",
                    "FINANCE_REVIEW",
                    "APPROVED",
                    "RECONCILED"))
        > 0) throw exception(PAYROLL_INSURANCE_INVALID);
    currentSources(b, actor);
    b.setStatus("HR_REVIEW");
    bills.updateById(b);
    event(b.getEntityId(), "SUBMIT_BILL", b.getEvidence(), b, actor);
  }

  @Transactional(rollbackFor = Exception.class)
  public void review(Long id, String reason, Long actor) {
    reason(reason);
    HrmPayrollInsuranceBillDO b = bill(id, "REVIEW", actor, true);
    if (!"HR_REVIEW".equals(b.getStatus())) throw exception(PAYROLL_INSURANCE_INVALID);
    independent(b.getCreator(), null, actor);
    currentSources(b, actor);
    b.setReviewedBy(actor);
    b.setStatus("FINANCE_REVIEW");
    bills.updateById(b);
    event(b.getEntityId(), "REVIEW_BILL", reason, b, actor);
  }

  @Transactional(rollbackFor = Exception.class)
  public void approve(Long id, String reason, Long actor) {
    reason(reason);
    HrmPayrollInsuranceBillDO b = bill(id, "APPROVE", actor, true);
    if (!"FINANCE_REVIEW".equals(b.getStatus())) throw exception(PAYROLL_INSURANCE_INVALID);
    independent(b.getCreator(), b.getReviewedBy(), actor);
    currentSources(b, actor);
    List<HrmPayrollInsuranceBillLineDO> compared =
        JsonUtils.parseArray(b.getComparison(), HrmPayrollInsuranceBillLineDO.class);
    for (HrmPayrollInsuranceBillLineDO line : compared) {
      line.setBillId(id);
      line.setEntityId(b.getEntityId());
      lines.insert(line);
    }
    b.setApprovedBy(actor);
    b.setStatus(
        compared.stream().allMatch(l -> "MATCHED".equals(l.getStatus()))
            ? "RECONCILED"
            : "APPROVED");
    bills.updateById(b);
    event(b.getEntityId(), "APPROVE_BILL", reason, b, actor);
  }

  @Transactional(rollbackFor = Exception.class)
  public void reject(Long id, String reason, Long actor) {
    reason(reason);
    HrmPayrollInsuranceBillDO before = bills.selectById(id);
    if (before == null) throw exception(PAYROLL_INSURANCE_INVALID);
    HrmPayrollInsuranceBillDO b =
        bill(id, "HR_REVIEW".equals(before.getStatus()) ? "REVIEW" : "APPROVE", actor, true);
    if (!Arrays.asList("HR_REVIEW", "FINANCE_REVIEW").contains(b.getStatus()))
      throw exception(PAYROLL_INSURANCE_INVALID);
    entities.check(
        b.getEntityId(), "HR_REVIEW".equals(b.getStatus()) ? "REVIEW" : "APPROVE", actor);
    b.setStatus("REJECTED");
    bills.updateById(b);
    event(b.getEntityId(), "REJECT_BILL", reason, b, actor);
  }

  public List<HrmPayrollInsuranceBillLineDO> lines(Long billId, Long actor) {
    bill(billId, "READ", actor, false);
    return currentLines(billId);
  }

  private List<HrmPayrollInsuranceBillLineDO> currentLines(Long billId) {
    List<HrmPayrollInsuranceBillLineDO> result =
        lines.selectList(
            new LambdaQueryWrapperX<HrmPayrollInsuranceBillLineDO>()
                .eq(HrmPayrollInsuranceBillLineDO::getBillId, billId)
                .orderByAsc(HrmPayrollInsuranceBillLineDO::getId));
    result.forEach(this::refresh);
    return result;
  }

  public HrmPayrollInsuranceBillDO getForExport(Long id, Long actor) {
    return bill(id, "EXPORT", actor, false);
  }

  public List<HrmPayrollInsuranceBillLineDO> exportLines(Long id, Long actor) {
    HrmPayrollInsuranceBillDO b = getForExport(id, actor);
    if (Arrays.asList("APPROVED", "RECONCILED").contains(b.getStatus())) return currentLines(id);
    List<HrmPayrollInsuranceBillLineDO> compared =
        JsonUtils.parseArray(b.getComparison(), HrmPayrollInsuranceBillLineDO.class);
    compared.forEach(this::refresh);
    return compared;
  }

  public List<HrmPayrollInsuranceSettlementDO> settlements(Long billId, Long actor) {
    bill(billId, "READ", actor, false);
    List<HrmPayrollInsuranceSettlementDO> result =
        settlements.selectList(
            new LambdaQueryWrapperX<HrmPayrollInsuranceSettlementDO>()
                .eq(HrmPayrollInsuranceSettlementDO::getBillId, billId)
                .orderByDesc(HrmPayrollInsuranceSettlementDO::getId));
    result.forEach(item -> item.setProofContent(null));
    return result;
  }

  private void refresh(HrmPayrollInsuranceBillLineDO line) {
    if (line.getActualPersonal() == null || line.getActualCorporate() == null) {
      line.setStatus("MISSING");
      line.setPersonalDifference(null);
      line.setCorporateDifference(null);
      return;
    }
    line.setPersonalDifference(line.getExpectedPersonal().subtract(line.getActualPersonal()));
    line.setCorporateDifference(line.getExpectedCorporate().subtract(line.getActualCorporate()));
    int p = line.getActualPersonal().compareTo(line.getExpectedPersonal()),
        c = line.getActualCorporate().compareTo(line.getExpectedCorporate());
    line.setStatus(
        p == 0 && c == 0
            ? "MATCHED"
            : p <= 0 && c <= 0 ? "UNDERPAID" : p >= 0 && c >= 0 ? "OVERPAID" : "MIXED");
  }

  private void payment(
      HrmPayrollInsuranceBillLineDO line, String mode, BigDecimal personal, BigDecimal corporate) {
    if ("RECORD".equals(mode)) {
      if (!"MISSING".equals(line.getStatus())) throw exception(PAYROLL_INSURANCE_INVALID);
      return;
    }
    if ("MISSING".equals(line.getStatus())
        || "MATCHED".equals(line.getStatus())
        || personal.add(corporate).signum() <= 0) throw exception(PAYROLL_INSURANCE_INVALID);
    BigDecimal p = line.getExpectedPersonal().subtract(line.getActualPersonal()),
        c = line.getExpectedCorporate().subtract(line.getActualCorporate());
    if ("REFUND".equals(mode)) {
      p = p.negate();
      c = c.negate();
    } else if (!"PAY".equals(mode)) throw exception(PAYROLL_INSURANCE_INVALID);
    if (personal.compareTo(p.max(BigDecimal.ZERO)) > 0
        || corporate.compareTo(c.max(BigDecimal.ZERO)) > 0)
      throw exception(PAYROLL_INSURANCE_INVALID);
  }

  @Transactional(rollbackFor = Exception.class)
  public Long propose(
      HrmPayrollInsuranceSettlementReqVO r, byte[] proof, String proofName, Long actor) {
    if (proof == null
        || proof.length == 0
        || proof.length > 2 * 1024 * 1024
        || proofName == null
        || proofName.length() > 255
        || !proofName.toLowerCase(Locale.ROOT).matches(".*\\.(pdf|png|jpeg|jpg|xlsx|xls)$"))
      throw exception(PAYROLL_INSURANCE_INVALID);
    r.setDigest(cn.hutool.crypto.digest.DigestUtil.sha256Hex(proof));
    if (!validator.validate(r).isEmpty()) throw exception(PAYROLL_INSURANCE_INVALID);
    HrmPayrollInsuranceBillLineDO line = lines.selectById(r.getLineId());
    if (line == null) throw exception(PAYROLL_INSURANCE_INVALID);
    HrmPayrollInsuranceBillDO b = bill(line.getBillId(), "MANAGE", actor, true);
    line = lines.selectOneForUpdate(HrmPayrollInsuranceBillLineDO::getId, line.getId());
    HrmPayrollInsuranceSettlementDO prior =
        settlements.selectOne(
            new LambdaQueryWrapperX<HrmPayrollInsuranceSettlementDO>()
                .eq(HrmPayrollInsuranceSettlementDO::getLineId, line.getId())
                .eq(HrmPayrollInsuranceSettlementDO::getDigest, r.getDigest()));
    if (prior != null) {
      if (!Objects.equals(prior.getCreator(), String.valueOf(actor))
          || !Objects.equals(prior.getMode(), r.getMode())
          || prior.getPersonal().compareTo(r.getPersonal()) != 0
          || prior.getCorporate().compareTo(r.getCorporate()) != 0
          || !Objects.equals(prior.getReference(), r.getReference())
          || !Objects.equals(prior.getEvidence(), r.getEvidence()))
        throw exception(PAYROLL_INSURANCE_INVALID);
      return prior.getId();
    }
    if (!"APPROVED".equals(b.getStatus())) throw exception(PAYROLL_INSURANCE_INVALID);
    if (settlements.selectCount(
            new LambdaQueryWrapperX<HrmPayrollInsuranceSettlementDO>()
                .eq(HrmPayrollInsuranceSettlementDO::getLineId, line.getId())
                .in(HrmPayrollInsuranceSettlementDO::getStatus, "HR_REVIEW", "FINANCE_REVIEW"))
        > 0) throw exception(PAYROLL_INSURANCE_INVALID);
    payment(line, r.getMode(), r.getPersonal(), r.getCorporate());
    HrmPayrollInsuranceSettlementDO s = BeanUtils.toBean(r, HrmPayrollInsuranceSettlementDO.class);
    s.setBillId(b.getId());
    s.setEntityId(b.getEntityId());
    s.setLineRevision(line.getRevision());
    s.setStatus("HR_REVIEW");
    s.setCreator(String.valueOf(actor));
    s.setProofName(proofName);
    s.setProofContent(Base64.getEncoder().encodeToString(proof));
    settlements.insert(s);
    event(b.getEntityId(), "SUBMIT_SETTLEMENT", r.getEvidence(), r, actor);
    return s.getId();
  }

  @Transactional(rollbackFor = Exception.class)
  public void transitionSettlement(Long id, String action, String reason, Long actor) {
    reason(reason);
    HrmPayrollInsuranceSettlementDO before = settlements.selectById(id);
    if (before == null) throw exception(PAYROLL_INSURANCE_INVALID);
    String permission =
        "review".equals(action)
            ? "REVIEW"
            : "reject".equals(action) && "HR_REVIEW".equals(before.getStatus())
                ? "REVIEW"
                : "APPROVE";
    HrmPayrollInsuranceBillDO b = bill(before.getBillId(), permission, actor, true);
    HrmPayrollInsuranceBillLineDO line =
        lines.selectOneForUpdate(HrmPayrollInsuranceBillLineDO::getId, before.getLineId());
    HrmPayrollInsuranceSettlementDO s =
        settlements.selectOneForUpdate(HrmPayrollInsuranceSettlementDO::getId, id);
    if (!"APPROVED".equals(b.getStatus())
        || !Objects.equals(line.getRevision(), s.getLineRevision()))
      throw exception(PAYROLL_INSURANCE_INVALID);
    if ("reject".equals(action)) {
      entities.check(
          s.getEntityId(), "HR_REVIEW".equals(s.getStatus()) ? "REVIEW" : "APPROVE", actor);
      if (!Arrays.asList("HR_REVIEW", "FINANCE_REVIEW").contains(s.getStatus()))
        throw exception(PAYROLL_INSURANCE_INVALID);
      s.setStatus("REJECTED");
    } else if ("review".equals(action)) {
      if (!"HR_REVIEW".equals(s.getStatus())) throw exception(PAYROLL_INSURANCE_INVALID);
      independent(s.getCreator(), null, actor);
      payment(line, s.getMode(), s.getPersonal(), s.getCorporate());
      s.setReviewedBy(actor);
      s.setStatus("FINANCE_REVIEW");
    } else if ("approve".equals(action)) {
      if (!"FINANCE_REVIEW".equals(s.getStatus())) throw exception(PAYROLL_INSURANCE_INVALID);
      independent(s.getCreator(), s.getReviewedBy(), actor);
      payment(line, s.getMode(), s.getPersonal(), s.getCorporate());
      if ("RECORD".equals(s.getMode())) {
        line.setActualPersonal(s.getPersonal());
        line.setActualCorporate(s.getCorporate());
      } else {
        int sign = "PAY".equals(s.getMode()) ? 1 : -1;
        line.setActualPersonal(
            line.getActualPersonal().add(s.getPersonal().multiply(BigDecimal.valueOf(sign))));
        line.setActualCorporate(
            line.getActualCorporate().add(s.getCorporate().multiply(BigDecimal.valueOf(sign))));
      }
      line.setRevision(line.getRevision() + 1);
      refresh(line);
      lines.updateById(line);
      s.setApprovedBy(actor);
      s.setStatus("APPROVED");
      if (lines.selectCount(
              new LambdaQueryWrapperX<HrmPayrollInsuranceBillLineDO>()
                  .eq(HrmPayrollInsuranceBillLineDO::getBillId, b.getId())
                  .ne(HrmPayrollInsuranceBillLineDO::getStatus, "MATCHED"))
          == 0) {
        b.setStatus("RECONCILED");
        bills.updateById(b);
      }
    } else throw exception(PAYROLL_INSURANCE_INVALID);
    settlements.updateById(s);
    s.setProofContent(null);
    event(s.getEntityId(), "SETTLEMENT_" + action.toUpperCase(Locale.ROOT), reason, s, actor);
  }

  public HrmPayrollInsuranceSettlementDO proof(Long id, Long actor) {
    HrmPayrollInsuranceSettlementDO s = settlements.selectById(id);
    if (s == null) throw exception(PAYROLL_INSURANCE_INVALID);
    entities.check(s.getEntityId(), "EXPORT", actor);
    return s;
  }
}
