package cn.iocoder.yudao.module.hrm.service.payroll;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.employee.info.HrmEmployeeMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import javax.annotation.Resource;
import javax.validation.Validator;
import lombok.Data;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class HrmPayrollTaxSettlementService {
  @Resource private HrmPayrollTaxSettlementMapper settlements;
  @Resource private HrmPayrollPaymentMapper payments;
  @Resource private cn.iocoder.yudao.module.hrm.dal.mysql.salary.monthrecord.HrmSalaryMonthRecordMapper batches;
  @Resource private HrmPayrollTaxCashLedgerService cash;
  @Resource private HrmPayrollOffsetMapper offsets;
  @Resource private HrmPayrollTaxFilingMapper filings;
  @Resource private HrmPayrollTaxProfileMapper profiles;
  @Resource private HrmPayrollTaxFilingService filingService;
  @Resource private HrmPayrollTaxReceiptMapper receipts;
  @Resource private HrmEmployeeMapper employees;
  @Resource private HrmPayrollEntityService entities;
  @Resource private Validator validator;
  @Resource private HrmPayrollTaxSettlementEventMapper events;

  @Data
  public static class Balance {
    private Long profileId;
    private List<Long> employeeIds;
    private String mode;
    private String maskedIdentifier;
    private String taxpayerName;
    private BigDecimal originalWithheld;
    private BigDecimal expectedWithholding;
    private BigDecimal settledNet;
    private BigDecimal remaining;
    private BigDecimal refundReserved;
    private BigDecimal recoveryReserved;
  }

  private HrmPayrollTaxFilingDO filing(Long id, String action, Long actor, boolean lock) {
    HrmPayrollTaxFilingDO f = filings.selectById(id);
    if (f == null) throw exception(PAYROLL_TAX_INVALID);
    entities.check(f.getEntityId(), action, actor);
    if (lock) {
      entities.lockApproved(f.getEntityId());
      f = filings.selectOneForUpdate(HrmPayrollTaxFilingDO::getId, id);
    }
    return f;
  }

  private HrmPayrollTaxFilingSourceVO source(HrmPayrollTaxFilingDO f) {
    return JsonUtils.parseObject(f.getSourceSnapshot(), HrmPayrollTaxFilingSourceVO.class);
  }

  private List<HrmPayrollTaxSettlementDO> period(HrmPayrollTaxFilingDO f, String key, String mode) {
    return settlements.selectList(
        new LambdaQueryWrapper<HrmPayrollTaxSettlementDO>()
            .eq(HrmPayrollTaxSettlementDO::getEntityId, f.getEntityId())
            .eq(HrmPayrollTaxSettlementDO::getYear, f.getYear())
            .eq(HrmPayrollTaxSettlementDO::getMonth, f.getMonth())
            .eq(HrmPayrollTaxSettlementDO::getTaxpayerKey, key)
            .eq(HrmPayrollTaxSettlementDO::getMode, mode));
  }

  public static BigDecimal signed(HrmPayrollTaxSettlementDO s) {
    return "REFUND".equals(s.getKind()) ? s.getAmount().negate() : s.getAmount();
  }

  private Balance balance(
      HrmPayrollTaxFilingDO f, HrmPayrollTaxFilingSourceVO.Row row, Long exclude) {
    HrmPayrollTaxProfileDO p = profiles.selectById(row.getProfileId());
    if (p == null || !Objects.equals(p.getEntityId(), f.getEntityId()))
      throw exception(PAYROLL_TAX_INVALID);
    BigDecimal expected =
        "RESIDENT_WAGE".equals(row.getMode())
            ? row.getExpectedSalaryWithholding()
            : row.getExpectedBonusWithholding();
    if (expected == null) expected = row.getWithheld();
    BigDecimal paid = BigDecimal.ZERO, refund = BigDecimal.ZERO, recovery = BigDecimal.ZERO;
    for (HrmPayrollTaxSettlementDO s : period(f, p.getTaxpayerKey(), row.getMode())) {
      if (Objects.equals(s.getId(), exclude)) continue;
      if (Arrays.asList("SETTLED","ABSORBED").contains(s.getStatus())) paid = paid.add(signed(s));
      else if (Arrays.asList("PENDING", "APPROVED").contains(s.getStatus())) {
        if ("REFUND".equals(s.getKind())) refund = refund.add(s.getAmount());
        else recovery = recovery.add(s.getAmount());
      }
    }
    return new Balance()
        .setProfileId(row.getProfileId())
        .setEmployeeIds(row.getEmployeeIds())
        .setMode(row.getMode())
        .setMaskedIdentifier(row.getMaskedIdentifier())
        .setTaxpayerName(row.getTaxpayerName())
        .setOriginalWithheld(row.getWithheld())
        .setExpectedWithholding(expected)
        .setSettledNet(paid)
        .setRemaining(expected.subtract(row.getWithheld()).subtract(paid))
        .setRefundReserved(refund)
        .setRecoveryReserved(recovery);
  }

  public List<Balance> balances(Long filingId, Long actor) {
    HrmPayrollTaxFilingDO f = filing(filingId, "READ", actor, false);
    List<Balance> out = new ArrayList<>();
    for (HrmPayrollTaxFilingSourceVO.Row row : source(f).getRows()) out.add(balance(f, row, null));
    return out;
  }

  public List<HrmPayrollTaxSettlementDO> list(Long filingId, Long actor) {
    HrmPayrollTaxFilingDO f = filing(filingId, "READ", actor, false);
    return settlements.selectList(
        new LambdaQueryWrapper<HrmPayrollTaxSettlementDO>()
            .eq(HrmPayrollTaxSettlementDO::getEntityId, f.getEntityId())
            .eq(HrmPayrollTaxSettlementDO::getYear, f.getYear())
            .eq(HrmPayrollTaxSettlementDO::getMonth, f.getMonth())
            .orderByDesc(HrmPayrollTaxSettlementDO::getId));
  }

  private HrmPayrollTaxFilingSourceVO.Row worker(
      HrmPayrollTaxFilingDO f, Long employeeId, String mode) {
    List<HrmPayrollTaxFilingSourceVO.Row> matching = new ArrayList<>();
    for (HrmPayrollTaxFilingSourceVO.Row row : source(f).getRows())
      if (Objects.equals(row.getMode(), mode) && row.getEmployeeIds().contains(employeeId))
        matching.add(row);
    if (matching.size() != 1) throw exception(PAYROLL_TAX_INVALID);
    return matching.get(0);
  }

  private void ready(HrmPayrollTaxFilingDO f, Long actor) {
    if (!"RECONCILED".equals(f.getStatus())
        || !Boolean.TRUE.equals(filingService.detail(f.getId(), actor).get("sourceFresh")))
      throw exception(PAYROLL_TAX_INVALID);
  }

  private void available(HrmPayrollTaxFilingDO f, HrmPayrollTaxSettlementReqVO req, Long exclude) {
    Balance b = balance(f, worker(f, req.getEmployeeId(), req.getMode()), exclude);
    BigDecimal available =
        "REFUND".equals(req.getKind())
            ? b.getRemaining().negate().subtract(b.getRefundReserved())
            : b.getRemaining().subtract(b.getRecoveryReserved());
    if (available.signum() <= 0 || req.getAmount().compareTo(available) > 0)
      throw exception(PAYROLL_TAX_INVALID);
  }

  @Data private static class LaterProof {private Long filingId;private String filingSnapshot;private HrmPayrollTaxCashLedgerVO cash;private BigDecimal balance;private List<Long> paymentIds;}
  private List<HrmPayrollPaymentDO> laterPayments(HrmPayrollTaxSettlementDO s){
    if(!"RESIDENT_WAGE".equals(s.getMode()))return Collections.emptyList();Set<Long> ids=new HashSet<>();for(HrmPayrollTaxProfileDO p:profiles.selectList(new LambdaQueryWrapper<HrmPayrollTaxProfileDO>().eq(HrmPayrollTaxProfileDO::getEntityId,s.getEntityId()).eq(HrmPayrollTaxProfileDO::getYear,s.getYear()).eq(HrmPayrollTaxProfileDO::getTaxpayerKey,s.getTaxpayerKey())))ids.add(p.getEmployeeId());if(ids.isEmpty())throw exception(PAYROLL_TAX_INVALID);
    List<HrmPayrollPaymentDO> out=new ArrayList<>();for(HrmPayrollPaymentDO p:payments.selectList(new LambdaQueryWrapper<HrmPayrollPaymentDO>().in(HrmPayrollPaymentDO::getEmployeeId,ids).eq(HrmPayrollPaymentDO::getStatus,"SUCCESS").gt(HrmPayrollPaymentDO::getValueDate,YearMonth.of(s.getYear(),s.getMonth()).atEndOfMonth()).le(HrmPayrollPaymentDO::getValueDate,LocalDate.of(s.getYear(),12,31)))){cn.iocoder.yudao.module.hrm.dal.dataobject.salary.monthrecord.HrmSalaryMonthRecordDO b=batches.selectById(p.getMonthRecordId());if(b!=null&&Objects.equals(b.getEntityId(),s.getEntityId()))out.add(p);}return out;
  }
  private LaterProof later(HrmPayrollTaxSettlementDO s,Long actor,boolean required){
    List<HrmPayrollPaymentDO> paid=laterPayments(s);if(paid.isEmpty())return null;YearMonth last=paid.stream().map(p->YearMonth.from(p.getValueDate())).max(Comparator.naturalOrder()).get();
    HrmPayrollTaxFilingDO latest=filings.selectOne(new LambdaQueryWrapper<HrmPayrollTaxFilingDO>().eq(HrmPayrollTaxFilingDO::getEntityId,s.getEntityId()).eq(HrmPayrollTaxFilingDO::getYear,last.getYear()).eq(HrmPayrollTaxFilingDO::getMonth,last.getMonthValue()).eq(HrmPayrollTaxFilingDO::getStatus,"RECONCILED").orderByDesc(HrmPayrollTaxFilingDO::getVersion).last("LIMIT 1"));
    if(latest==null){if(required)throw exception(PAYROLL_TAX_LATER_PAYROLL_RECONCILIATION_REQUIRED);return null;}ready(latest,actor);
    List<HrmPayrollTaxFilingSourceVO.Row> matching=new ArrayList<>();for(HrmPayrollTaxFilingSourceVO.Row r:source(latest).getRows()){HrmPayrollTaxProfileDO p=profiles.selectById(r.getProfileId());if(p!=null&&Objects.equals(p.getTaxpayerKey(),s.getTaxpayerKey())&&Objects.equals(p.getEntityId(),s.getEntityId())&&"RESIDENT_WAGE".equals(r.getMode()))matching.add(r);}if(matching.size()!=1)throw exception(PAYROLL_TAX_INVALID);HrmPayrollTaxFilingSourceVO.Row row=matching.get(0);List<Long> ids=new ArrayList<>();for(HrmPayrollPaymentDO p:paid)if(YearMonth.from(p.getValueDate()).equals(last)){if(!row.getPaymentIds().contains(p.getId()))throw exception(PAYROLL_TAX_INVALID);ids.add(p.getId());}Collections.sort(ids);
    HrmPayrollTaxCashLedgerVO ledger=cash.forTaxFiling(s.getEntityId(),s.getEmployeeId(),s.getYear(),last.atEndOfMonth().isAfter(LocalDate.now(ZoneId.of("Asia/Shanghai")))?LocalDate.now(ZoneId.of("Asia/Shanghai")):last.atEndOfMonth());if(ledger==null||!ledger.isReady()||row.getCumulativeTax()==null||ledger.getWithheld()==null)throw exception(PAYROLL_TAX_INVALID);BigDecimal remaining=row.getCumulativeTax().subtract(ledger.getWithheld());if(row.getCumulativeTaxBalance()!=null&&row.getCumulativeTaxBalance().compareTo(remaining)!=0)throw exception(PAYROLL_TAX_INVALID);
    return new LaterProof().setFilingId(latest.getId()).setFilingSnapshot(latest.getSourceSnapshot()).setCash(ledger).setBalance(remaining).setPaymentIds(ids);
  }
  private void outstanding(HrmPayrollTaxSettlementDO s,Long actor,boolean required){LaterProof proof=later(s,actor,required);if(proof!=null&&("RECOVERY".equals(s.getKind())?proof.getBalance():proof.getBalance().negate()).compareTo(s.getAmount())<0)throw exception(PAYROLL_TAX_LATER_PAYROLL_RECONCILIATION_REQUIRED);}

  private void authority(HrmPayrollTaxSettlementDO settlement,HrmPayrollTaxFilingDO filing){
    if(!Objects.equals(JsonUtils.parseTree(settlement.getAuthoritySnapshot()),JsonUtils.parseTree(filing.getSourceSnapshot())))throw exception(PAYROLL_VERSION_CONFLICT);
    HrmPayrollTaxFilingSourceVO.Row worker=worker(filing,settlement.getEmployeeId(),settlement.getMode());HrmPayrollTaxProfileDO profile=profiles.selectById(worker.getProfileId());
    if(profile==null||!Objects.equals(profile.getTaxpayerKey(),settlement.getTaxpayerKey())||!Objects.equals(settlement.getTaxRegistrationNumber(),source(filing).getTaxRegistrationNumber()))throw exception(PAYROLL_VERSION_CONFLICT);
  }

  @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
  public Long submit(HrmPayrollTaxSettlementReqVO req, Long actor) {
    if (!validator.validate(req).isEmpty()) throw exception(PAYROLL_TAX_INVALID);
    HrmPayrollTaxFilingDO f = filing(req.getFilingId(), "MANAGE", actor, true);
    HrmPayrollTaxSettlementDO replay =
        settlements.selectOne(HrmPayrollTaxSettlementDO::getRequestKey, req.getRequestKey());
    if (replay != null) {
      if (!Objects.equals(replay.getEntityId(), f.getEntityId())
          || !Objects.equals(replay.getRequestSnapshot(), JsonUtils.toJsonString(req)))
        throw exception(PAYROLL_VERSION_CONFLICT);
      return replay.getId();
    }
    ready(f, actor);
    available(f, req, null);
    HrmPayrollTaxFilingSourceVO.Row row = worker(f, req.getEmployeeId(), req.getMode());
    HrmPayrollTaxProfileDO profile = profiles.selectById(row.getProfileId());
    HrmPayrollTaxSettlementDO s =
        cn.iocoder.yudao.framework.common.util.object.BeanUtils.toBean(
                req, HrmPayrollTaxSettlementDO.class)
            .setEntityId(f.getEntityId())
            .setYear(f.getYear())
            .setMonth(f.getMonth())
            .setProfileId(profile.getId())
            .setTaxpayerKey(profile.getTaxpayerKey())
            .setTaxRegistrationNumber(source(f).getTaxRegistrationNumber())
            .setSubmittedBy(actor)
            .setStatus("PENDING")
            .setRequestSnapshot(JsonUtils.toJsonString(req))
            .setAuthoritySnapshot(f.getSourceSnapshot());
    settlements.insert(s);
    event(s, "SUBMIT", req.getReason(), actor);
    return s.getId();
  }

  @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
  public void review(Long id, boolean approve, String reason, Long actor) {
    text(reason);
    HrmPayrollTaxSettlementDO candidate = settlements.selectById(id);
    if (candidate == null) throw exception(PAYROLL_TAX_INVALID);
    HrmPayrollTaxFilingDO f = filing(candidate.getFilingId(), "REVIEW", actor, true);
    HrmPayrollTaxSettlementDO s =
        settlements.selectOneForUpdate(HrmPayrollTaxSettlementDO::getId, id);
    if (!"PENDING".equals(s.getStatus())) throw exception(PAYROLL_TAX_INVALID);
    avoidSelf(s, actor);
    if (approve) {
      authority(s,f);
      ready(f, actor);
      available(
          f, JsonUtils.parseObject(s.getRequestSnapshot(), HrmPayrollTaxSettlementReqVO.class), id);
    }
    s.setStatus(approve ? "APPROVED" : "REJECTED")
        .setReviewedBy(actor)
        .setReviewReason(reason)
        .setReviewedTime(LocalDateTime.now(ZoneId.of("Asia/Shanghai")));
    settlements.updateById(s);
    event(s, approve ? "APPROVE" : "REJECT", reason, actor);
  }

  /** Internal read for designated payroll; the caller already checks target batch scope. */
  public HrmPayrollTaxSettlementDO approvedForPayroll(Long id,Long actor){
    HrmPayrollTaxSettlementDO s=settlements.selectById(id);if(s==null||!"APPROVED".equals(s.getStatus()))throw exception(PAYROLL_OFFSET_INVALID);
    HrmPayrollTaxFilingDO f=filing(s.getFilingId(),"READ",actor,false);authority(s,f);ready(f,actor);
    available(f,JsonUtils.parseObject(s.getRequestSnapshot(),HrmPayrollTaxSettlementReqVO.class),s.getId());outstanding(s,actor,false);return s;
  }
  private void assertNotScheduled(Long id){if(offsets.selectCount(new cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX<HrmPayrollOffsetDO>().eq(HrmPayrollOffsetDO::getSourceType,"TAX").eq(HrmPayrollOffsetDO::getSourceId,id).in(HrmPayrollOffsetDO::getStatus,"PENDING","APPROVED","CONSUMED"))>0)throw exception(PAYROLL_OFFSET_INVALID);}
  @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
  public void confirm(HrmPayrollTaxSettlementConfirmReqVO req, Long actor) {
    if (!validator.validate(req).isEmpty()) throw exception(PAYROLL_TAX_INVALID);
    HrmPayrollTaxSettlementDO candidate = settlements.selectById(req.getId());
    if (candidate == null) throw exception(PAYROLL_TAX_INVALID);
    HrmPayrollTaxFilingDO f=filing(candidate.getFilingId(), "APPROVE", actor, true);
    assertNotScheduled(candidate.getId());
    HrmPayrollTaxSettlementDO s =
        settlements.selectOneForUpdate(HrmPayrollTaxSettlementDO::getId, req.getId());
    avoidSelf(s, actor);
    if ("SETTLED".equals(s.getStatus())) {
      if (Objects.equals(s.getValueDate(), req.getValueDate())
          && Objects.equals(s.getBankReference(), req.getBankReference())
          && Objects.equals(s.getBankEvidence(), req.getEvidence())
          && s.getAmount().compareTo(req.getAmount()) == 0) return;
      throw exception(PAYROLL_VERSION_CONFLICT);
    }
    if (!"APPROVED".equals(s.getStatus())
        || req.getAmount().compareTo(s.getAmount()) != 0
        || req.getValueDate().isAfter(LocalDate.now(ZoneId.of("Asia/Shanghai")))
        || req.getValueDate().isBefore(s.getReviewedTime().toLocalDate()))
      throw exception(PAYROLL_TAX_INVALID);
    authority(s,f);ready(f,actor);available(f,JsonUtils.parseObject(s.getRequestSnapshot(),HrmPayrollTaxSettlementReqVO.class),s.getId());
    outstanding(s,actor,true);
    s.setStatus("SETTLED")
        .setValueDate(req.getValueDate())
        .setBankReference(req.getBankReference())
        .setBankEvidence(req.getEvidence())
        .setConfirmedBy(actor)
        .setConfirmedTime(LocalDateTime.now(ZoneId.of("Asia/Shanghai")));
    if (settlements.selectOne(HrmPayrollTaxSettlementDO::getBankReference, req.getBankReference())
        != null) throw exception(PAYROLL_TAX_INVALID);
    try {
      settlements.updateById(s);
    } catch (org.springframework.dao.DuplicateKeyException duplicate) {
      throw exception(PAYROLL_TAX_INVALID);
    }
    event(s, "SETTLE", req.getEvidence(), actor);
  }

  @Transactional(rollbackFor=Exception.class,isolation=Isolation.READ_COMMITTED)
  public void absorb(HrmPayrollTaxAbsorptionReqVO req,Long actor){if(!validator.validate(req).isEmpty())throw exception(PAYROLL_TAX_INVALID);HrmPayrollTaxSettlementDO candidate=settlements.selectById(req.getId());if(candidate==null)throw exception(PAYROLL_TAX_INVALID);HrmPayrollTaxFilingDO f=filing(candidate.getFilingId(),"APPROVE",actor,true);assertNotScheduled(candidate.getId());HrmPayrollTaxSettlementDO s=settlements.selectOneForUpdate(HrmPayrollTaxSettlementDO::getId,req.getId());avoidSelf(s,actor);
    if("ABSORBED".equals(s.getStatus())){if(Objects.equals(s.getAccountingRequest(),JsonUtils.toJsonString(req)))return;throw exception(PAYROLL_VERSION_CONFLICT);}if(!"APPROVED".equals(s.getStatus())||!"RESIDENT_WAGE".equals(s.getMode()))throw exception(PAYROLL_TAX_INVALID);authority(s,f);ready(f,actor);available(f,JsonUtils.parseObject(s.getRequestSnapshot(),HrmPayrollTaxSettlementReqVO.class),s.getId());LaterProof proof=later(s,actor,true);if(proof==null||!Objects.equals(proof.getFilingId(),req.getAccountingFilingId())||proof.getBalance().signum()!=0)throw exception(PAYROLL_TAX_LATER_PAYROLL_RECONCILIATION_REQUIRED);
    s.setStatus("ABSORBED").setAccountingFilingId(proof.getFilingId()).setAccountingEvidence(req.getEvidence()).setAccountingSnapshot(JsonUtils.toJsonString(proof)).setAccountingRequest(JsonUtils.toJsonString(req)).setAccountedBy(actor).setAccountedTime(LocalDateTime.now(ZoneId.of("Asia/Shanghai")));settlements.updateById(s);event(s,"ABSORB_PAYROLL",req.getEvidence(),actor);
  }
  public List<Map<String,Object>> absorptionOptions(Long id,Long actor){HrmPayrollTaxSettlementDO s=settlements.selectById(id);if(s==null)throw exception(PAYROLL_TAX_INVALID);filing(s.getFilingId(),"READ",actor,false);LaterProof proof=later(s,actor,true);if(proof==null||proof.getBalance().signum()!=0)return Collections.emptyList();Map<String,Object> option=new LinkedHashMap<>();option.put("id",proof.getFilingId());option.put("paymentIds",proof.getPaymentIds());option.put("balance",proof.getBalance());return Collections.singletonList(option);}

  @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
  public void cancel(Long id, String reason, String bankVoidEvidence, Long actor) {
    text(reason);
    HrmPayrollTaxSettlementDO candidate = settlements.selectById(id);
    if (candidate == null) throw exception(PAYROLL_TAX_INVALID);
    filing(candidate.getFilingId(), "APPROVE", actor, true);
    assertNotScheduled(candidate.getId());
    HrmPayrollTaxSettlementDO s =
        settlements.selectOneForUpdate(HrmPayrollTaxSettlementDO::getId, id);
    avoidSelf(s, actor);
    if (!Arrays.asList("PENDING", "APPROVED").contains(s.getStatus()))
      throw exception(PAYROLL_TAX_INVALID);
    if ("APPROVED".equals(s.getStatus())) text(bankVoidEvidence);
    s.setStatus("CANCELLED").setReviewReason(reason);
    settlements.updateById(s);
    Map<String, Object> proof = new LinkedHashMap<>();
    proof.put("settlement", s);
    proof.put("bankVoidEvidence", bankVoidEvidence);
    events.insert(
        new HrmPayrollTaxSettlementEventDO()
            .setSettlementId(s.getId())
            .setAction("CANCEL")
            .setReason(reason)
            .setActorId(actor)
            .setSnapshot(JsonUtils.toJsonString(proof)));
  }

  private void avoidSelf(HrmPayrollTaxSettlementDO s, Long actor) {
    cn.iocoder.yudao.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO e =
        employees.selectById(s.getEmployeeId());
    if (e == null
        || Objects.equals(actor, s.getSubmittedBy())
        || Objects.equals(actor, e.getUserId())) throw exception(PAYROLL_SELF_APPROVAL);
  }

  private void text(String value) {
    if (value == null || value.trim().isEmpty() || value.length() > 1000)
      throw exception(PAYROLL_TAX_INVALID);
  }

  private void event(HrmPayrollTaxSettlementDO s, String action, String reason, Long actor) {
    events.insert(
        new HrmPayrollTaxSettlementEventDO()
            .setSettlementId(s.getId())
            .setAction(action)
            .setReason(reason)
            .setActorId(actor)
            .setSnapshot(JsonUtils.toJsonString(s)));
  }
}
