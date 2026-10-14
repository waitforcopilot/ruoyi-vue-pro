package cn.iocoder.yudao.module.hrm.service.payroll;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
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
public class HrmPayrollTaxFilingService {
  @Resource private HrmPayrollTaxFilingMapper filings;
  @Resource private HrmPayrollTaxReceiptMapper receipts;
  @Resource private HrmPayrollTaxFilingEventMapper events;
  @Resource private HrmPayrollTaxFilingSourceService sources;
  @Resource private HrmPayrollEntityService entities;
  @Resource private HrmPayrollTaxProfileService profiles;
  @Resource private javax.validation.Validator validator;

  private LocalDate today() {
    return LocalDate.now(ZoneId.of("Asia/Shanghai"));
  }

  private void text(String s, int max) {
    if (s == null || s.trim().isEmpty() || s.length() > max) throw exception(PAYROLL_TAX_INVALID);
  }

  private HrmPayrollTaxFilingDO row(Long id, String action, Long actor, boolean lock) {
    HrmPayrollTaxFilingDO row = filings.selectById(id);
    if (row == null) throw exception(PAYROLL_TAX_INVALID);
    entities.check(row.getEntityId(), action, actor);
    if (lock) {
      entities.lockApproved(row.getEntityId());
      row = filings.selectOneForUpdate(HrmPayrollTaxFilingDO::getId, id);
    }
    return row;
  }

  private HrmPayrollTaxFilingDO active(Long entity, Integer year, Integer month) {
    return filings.selectOne(
        new LambdaQueryWrapperX<HrmPayrollTaxFilingDO>()
            .eq(HrmPayrollTaxFilingDO::getEntityId, entity)
            .eq(HrmPayrollTaxFilingDO::getYear, year)
            .eq(HrmPayrollTaxFilingDO::getMonth, month)
            .in(HrmPayrollTaxFilingDO::getStatus, "APPROVED", "FILED", "RECONCILED"));
  }

  private HrmPayrollTaxFilingSourceVO saved(HrmPayrollTaxFilingDO row) {
    return JsonUtils.parseObject(row.getSourceSnapshot(), HrmPayrollTaxFilingSourceVO.class);
  }

  private boolean fresh(HrmPayrollTaxFilingDO row) {
    return Objects.equals(
        row.getSourceSnapshot(),
        JsonUtils.toJsonString(sources.source(row.getEntityId(), row.getYear(), row.getMonth())));
  }

  private void requireFresh(HrmPayrollTaxFilingDO row) {
    if (!fresh(row)) throw exception(PAYROLL_VERSION_CONFLICT);
  }

  private void avoidSelf(HrmPayrollTaxFilingDO row, Long creator, Long actor) {
    if (Objects.equals(creator, actor)) throw exception(PAYROLL_SELF_APPROVAL);
  }

  private void event(
      HrmPayrollTaxFilingDO row,
      Long receiptId,
      String action,
      String reason,
      Long actor,
      Object snapshot) {
    events.insert(
        new HrmPayrollTaxFilingEventDO()
            .setFilingId(row.getId())
            .setReceiptId(receiptId)
            .setAction(action)
            .setReason(reason)
            .setActorId(actor)
            .setSnapshot(JsonUtils.toJsonString(snapshot)));
  }

  @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
  public Long create(HrmPayrollTaxFilingReqVO req, Long actor) {
    if (!validator.validate(req).isEmpty()) throw exception(PAYROLL_TAX_INVALID);
    HrmPayrollTaxFilingSourceService.period(req.getYear(), req.getMonth());
    entities.check(req.getEntityId(), "MANAGE", actor);
    entities.lockApproved(req.getEntityId());
    String body = JsonUtils.toJsonString(req);
    HrmPayrollTaxFilingDO replay =
        filings.selectOne(HrmPayrollTaxFilingDO::getRequestKey, req.getRequestKey());
    if (replay != null) {
      if (!Objects.equals(replay.getSubmittedBy(), actor)
          || !Objects.equals(body, replay.getRequestSnapshot()))
        throw exception(PAYROLL_TAX_INVALID);
      return replay.getId();
    }
    HrmPayrollTaxFilingDO prior = active(req.getEntityId(), req.getYear(), req.getMonth());
    if (!Objects.equals(req.getPriorFilingId(), prior == null ? null : prior.getId()))
      throw exception(PAYROLL_VERSION_CONFLICT);
    HrmPayrollTaxFilingSourceVO source =
        sources.source(req.getEntityId(), req.getYear(), req.getMonth());
    if (Boolean.TRUE.equals(req.getZeroDeclaration()) != source.getRows().isEmpty())
      throw exception(PAYROLL_TAX_INVALID);
    int version =
        filings
                .selectList(
                    new LambdaQueryWrapperX<HrmPayrollTaxFilingDO>()
                        .eq(HrmPayrollTaxFilingDO::getEntityId, req.getEntityId())
                        .eq(HrmPayrollTaxFilingDO::getYear, req.getYear())
                        .eq(HrmPayrollTaxFilingDO::getMonth, req.getMonth()))
                .stream()
                .mapToInt(HrmPayrollTaxFilingDO::getVersion)
                .max()
                .orElse(0)
            + 1;
    HrmPayrollTaxFilingDO row =
        BeanUtils.toBean(req, HrmPayrollTaxFilingDO.class)
            .setVersion(version)
            .setSubmittedBy(actor)
            .setRequestSnapshot(body)
            .setSourceSnapshot(JsonUtils.toJsonString(source))
            .setExpectedTax(source.getTotalTax())
            .setStatus("PENDING");
    filings.insert(row);
    event(row, null, "SUBMIT", req.getEvidence(), actor, row);
    return row.getId();
  }

  @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
  public void review(Long id, boolean approve, String reason, Long actor) {
    text(reason, 1000);
    HrmPayrollTaxFilingDO row = row(id, "REVIEW", actor, true);
    if (!"PENDING".equals(row.getStatus())) throw exception(PAYROLL_TAX_INVALID);
    avoidSelf(row, row.getSubmittedBy(), actor);
    if (approve) {
      requireFresh(row);
      HrmPayrollTaxFilingDO prior = active(row.getEntityId(), row.getYear(), row.getMonth());
      if (!Objects.equals(row.getPriorFilingId(), prior == null ? null : prior.getId()))
        throw exception(PAYROLL_VERSION_CONFLICT);
      if (prior != null) {
        prior.setStatus("SUPERSEDED");
        filings.updateById(prior);
        event(prior, null, "SUPERSEDE", "接续版本" + id, actor, prior);
      }
    }
    row.setStatus(approve ? "APPROVED" : "REJECTED")
        .setReviewedBy(actor)
        .setReviewedTime(LocalDateTime.now(ZoneId.of("Asia/Shanghai")))
        .setReviewReason(reason);
    filings.updateById(row);
    event(row, null, approve ? "APPROVE" : "REJECT", reason, actor, row);
  }

  public List<HrmPayrollTaxFilingDO> list(Long entity, Integer year, Integer month, Long actor) {
    entities.check(entity, "READ", actor);
    return filings.selectList(
        new LambdaQueryWrapperX<HrmPayrollTaxFilingDO>()
            .eq(HrmPayrollTaxFilingDO::getEntityId, entity)
            .eq(HrmPayrollTaxFilingDO::getYear, year)
            .eqIfPresent(HrmPayrollTaxFilingDO::getMonth, month)
            .orderByDesc(HrmPayrollTaxFilingDO::getId));
  }

  @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
  public List<HrmPayrollTaxFilingExportRowVO> export(Long id, Long actor) {
    HrmPayrollTaxFilingDO row = row(id, "READ", actor, true);
    if (!Arrays.asList("APPROVED", "FILED", "RECONCILED").contains(row.getStatus()))
      throw exception(PAYROLL_TAX_INVALID);
    requireFresh(row);
    List<HrmPayrollTaxFilingExportRowVO> output = new ArrayList<>();
    Map<Long, String> identifiers = new HashMap<>();
    for (HrmPayrollTaxFilingSourceVO.Row worker : saved(row).getRows()) {
      String identifier =
          identifiers.computeIfAbsent(
              worker.getProfileId(), key -> profiles.privateIdentifier(key, actor));
      output.add(
          BeanUtils.toBean(worker, HrmPayrollTaxFilingExportRowVO.class)
              .setIdentifier(identifier)
              .setTaxRegistrationNumber(saved(row).getTaxRegistrationNumber())
              .setPeriod(YearMonth.of(row.getYear(), row.getMonth()).toString()));
    }
    event(
        row,
        null,
        "EXPORT",
        "导出批准申报准备文件，按完整税务标识权限读取",
        actor,
        Collections.singletonMap("rows", output.size()));
    return output;
  }

  @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
  public void filed(Long id, String reference, LocalDate date, String evidence, Long actor) {
    text(reference, 100);
    text(evidence, 1000);
    HrmPayrollTaxFilingDO row = row(id, "MANAGE", actor, true);
    if (date == null
        || date.isAfter(today())
        || date.isBefore(YearMonth.of(row.getYear(), row.getMonth()).atEndOfMonth()))
      throw exception(PAYROLL_TAX_INVALID);
    if (Arrays.asList("FILED", "RECONCILED").contains(row.getStatus())
        && Objects.equals(reference, row.getFilingReference())
        && Objects.equals(date, row.getFiledDate())
        && Objects.equals(evidence, row.getFilingEvidence())) return;
    if (!"APPROVED".equals(row.getStatus())) throw exception(PAYROLL_TAX_INVALID);
    requireFresh(row);
    row.setStatus("FILED")
        .setFilingReference(reference)
        .setFiledDate(date)
        .setFilingEvidence(evidence)
        .setFiledBy(actor);
    filings.updateById(row);
    event(row, null, "FILED", evidence, actor, row);
  }

  @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
  public Long receipt(HrmPayrollTaxReceiptReqVO req, Long actor) {
    if (!validator.validate(req).isEmpty() || req.getPaymentDate().isAfter(today()))
      throw exception(PAYROLL_TAX_INVALID);
    HrmPayrollTaxFilingDO filing = row(req.getFilingId(), "MANAGE", actor, true);
    if (filing.getFiledDate() == null) throw exception(PAYROLL_TAX_INVALID);
    String body = JsonUtils.toJsonString(req);
    HrmPayrollTaxReceiptDO replay =
        receipts.selectOne(HrmPayrollTaxReceiptDO::getRequestKey, req.getRequestKey());
    if (replay != null) {
      if (!Objects.equals(actor, replay.getSubmittedBy())
          || !Objects.equals(body, replay.getRequestSnapshot()))
        throw exception(PAYROLL_TAX_INVALID);
      return replay.getId();
    }
    if (receipts.selectOne(
            new LambdaQueryWrapperX<HrmPayrollTaxReceiptDO>()
                .eq(HrmPayrollTaxReceiptDO::getEntityId, filing.getEntityId())
                .eq(HrmPayrollTaxReceiptDO::getReference, req.getReference())
                .in(HrmPayrollTaxReceiptDO::getStatus, "PENDING", "APPROVED"))
        != null) throw exception(PAYROLL_TAX_INVALID);
    if ("PAYMENT".equals(req.getKind()) && req.getOriginalReceiptId() != null)
      throw exception(PAYROLL_TAX_INVALID);
    if ("REFUND".equals(req.getKind())) {
      validateRefund(filing, req.getOriginalReceiptId(), req.getAmount());
      if (req.getPaymentDate()
          .isBefore(receipts.selectById(req.getOriginalReceiptId()).getPaymentDate()))
        throw exception(PAYROLL_TAX_INVALID);
    }
    HrmPayrollTaxReceiptDO receipt =
        BeanUtils.toBean(req, HrmPayrollTaxReceiptDO.class)
            .setEntityId(filing.getEntityId())
            .setYear(filing.getYear())
            .setMonth(filing.getMonth())
            .setRequestSnapshot(body)
            .setSubmittedBy(actor)
            .setStatus("PENDING");
    receipts.insert(receipt);
    event(filing, receipt.getId(), "RECEIPT_SUBMIT", req.getEvidence(), actor, receipt);
    return receipt.getId();
  }

  @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
  public List<Long> importReceipts(
      Long filingId, List<HrmPayrollTaxReceiptImportRowVO> rows, String digest, Long actor) {
    row(filingId, "MANAGE", actor, true);
    if (rows == null || rows.isEmpty() || rows.size() > 5000 || digest == null)
      throw exception(PAYROLL_TAX_INVALID);
    List<Long> ids = new ArrayList<>();
    int index = 0;
    for (HrmPayrollTaxReceiptImportRowVO value : rows) {
      String kind =
          "缴税".equals(value.getKind())
              ? "PAYMENT"
              : "退税".equals(value.getKind()) ? "REFUND" : value.getKind();
      LocalDate date;
      try {
        date = LocalDate.parse(value.getPaymentDate());
      } catch (RuntimeException ex) {
        throw exception(PAYROLL_TAX_INVALID);
      }
      HrmPayrollTaxReceiptReqVO req =
          new HrmPayrollTaxReceiptReqVO()
              .setFilingId(filingId)
              .setKind(kind)
              .setReference(value.getReference())
              .setAmount(value.getAmount())
              .setPaymentDate(date)
              .setOriginalReceiptId(value.getOriginalReceiptId())
              .setEvidence(value.getEvidence())
              .setRequestKey(
                  "tax-receipt-import:"
                      + cn.hutool.crypto.digest.DigestUtil.sha256Hex(
                          filingId + ":" + digest + ":" + (index++)));
      ids.add(receipt(req, actor));
    }
    return ids;
  }

  private void validateRefund(HrmPayrollTaxFilingDO filing, Long originalId, BigDecimal amount) {
    if (originalId == null) throw exception(PAYROLL_TAX_INVALID);
    HrmPayrollTaxReceiptDO original = receipts.selectById(originalId);
    if (original == null
        || !"APPROVED".equals(original.getStatus())
        || !"PAYMENT".equals(original.getKind())
        || !Objects.equals(original.getEntityId(), filing.getEntityId())
        || !Objects.equals(original.getYear(), filing.getYear())
        || !Objects.equals(original.getMonth(), filing.getMonth()))
      throw exception(PAYROLL_TAX_INVALID);
    BigDecimal already =
        receipts
            .selectList(
                new LambdaQueryWrapperX<HrmPayrollTaxReceiptDO>()
                    .eq(HrmPayrollTaxReceiptDO::getOriginalReceiptId, originalId)
                    .eq(HrmPayrollTaxReceiptDO::getStatus, "APPROVED"))
            .stream()
            .map(HrmPayrollTaxReceiptDO::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    if (already.add(amount).compareTo(original.getAmount()) > 0)
      throw exception(PAYROLL_TAX_INVALID);
  }

  @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
  public void reviewReceipt(Long id, boolean approve, String reason, Long actor) {
    text(reason, 1000);
    HrmPayrollTaxReceiptDO candidate = receipts.selectById(id);
    if (candidate == null) throw exception(PAYROLL_TAX_INVALID);
    HrmPayrollTaxFilingDO filing = row(candidate.getFilingId(), "REVIEW", actor, true);
    HrmPayrollTaxReceiptDO receipt = receipts.selectOneForUpdate(HrmPayrollTaxReceiptDO::getId, id);
    if (!"PENDING".equals(receipt.getStatus())) throw exception(PAYROLL_TAX_INVALID);
    avoidSelf(filing, receipt.getSubmittedBy(), actor);
    if (approve && "REFUND".equals(receipt.getKind()))
      validateRefund(filing, receipt.getOriginalReceiptId(), receipt.getAmount());
    receipt
        .setStatus(approve ? "APPROVED" : "REJECTED")
        .setReviewedBy(actor)
        .setReviewedTime(LocalDateTime.now(ZoneId.of("Asia/Shanghai")))
        .setReviewReason(reason);
    receipts.updateById(receipt);
    // A new official fact requires explicit reconciliation again; earlier reconciliation stays in
    // events.
    HrmPayrollTaxFilingDO current =
        active(filing.getEntityId(), filing.getYear(), filing.getMonth());
    if (approve && current != null && "RECONCILED".equals(current.getStatus())) {
      current.setStatus("FILED");
      filings.updateById(current);
    }
    event(filing, id, approve ? "RECEIPT_APPROVE" : "RECEIPT_REJECT", reason, actor, receipt);
  }

  private List<HrmPayrollTaxReceiptDO> periodReceipts(HrmPayrollTaxFilingDO row) {
    return receipts.selectList(
        new LambdaQueryWrapperX<HrmPayrollTaxReceiptDO>()
            .eq(HrmPayrollTaxReceiptDO::getEntityId, row.getEntityId())
            .eq(HrmPayrollTaxReceiptDO::getYear, row.getYear())
            .eq(HrmPayrollTaxReceiptDO::getMonth, row.getMonth())
            .orderByAsc(HrmPayrollTaxReceiptDO::getId));
  }

  private BigDecimal paid(HrmPayrollTaxFilingDO row) {
    return periodReceipts(row).stream()
        .filter(r -> "APPROVED".equals(r.getStatus()))
        .map(r -> "REFUND".equals(r.getKind()) ? r.getAmount().negate() : r.getAmount())
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
  public void reconcile(Long id, String reason, Long actor) {
    text(reason, 1000);
    HrmPayrollTaxFilingDO row = row(id, "REVIEW", actor, true);
    if (!Arrays.asList("FILED", "RECONCILED").contains(row.getStatus()))
      throw exception(PAYROLL_TAX_INVALID);
    avoidSelf(row, row.getSubmittedBy(), actor);
    requireFresh(row);
    if (!Objects.equals(active(row.getEntityId(), row.getYear(), row.getMonth()).getId(), id)
        || paid(row).compareTo(row.getExpectedTax()) != 0) throw exception(PAYROLL_TAX_INVALID);
    if ("RECONCILED".equals(row.getStatus())) return;
    row.setStatus("RECONCILED");
    filings.updateById(row);
    event(row, null, "RECONCILE", reason, actor, row);
  }

  public Map<String, Object> detail(Long id, Long actor) {
    HrmPayrollTaxFilingDO row = row(id, "READ", actor, false);
    Map<String, Object> output = new LinkedHashMap<>();
    output.put("filing", row);
    output.put("source", saved(row));
    output.put("receipts", periodReceipts(row));
    output.put("officialPaid", paid(row));
    output.put("difference", row.getExpectedTax().subtract(paid(row)));
    boolean ready;
    try {
      ready = fresh(row);
    } catch (RuntimeException ex) {
      ready = false;
    }
    output.put("sourceFresh", ready);
    output.put(
        "events",
        events.selectList(
            new LambdaQueryWrapperX<HrmPayrollTaxFilingEventDO>()
                .eq(HrmPayrollTaxFilingEventDO::getFilingId, id)
                .orderByAsc(HrmPayrollTaxFilingEventDO::getId)));
    return output;
  }

  public List<HrmPayrollTaxReceiptDO> exportReceipts(Long id, Long actor) {
    HrmPayrollTaxFilingDO row = row(id, "READ", actor, false);
    event(
        row,
        null,
        "RECEIPT_EXPORT",
        "导出完税对账资料",
        actor,
        Collections.singletonMap("year", row.getYear()));
    return periodReceipts(row);
  }
}
