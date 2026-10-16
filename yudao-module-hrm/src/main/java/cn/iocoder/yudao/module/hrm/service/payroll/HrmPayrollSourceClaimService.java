package cn.iocoder.yudao.module.hrm.service.payroll;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.salary.monthrecord.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.employee.info.HrmEmployeeMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.salary.monthrecord.HrmSalaryMonthRecordMapper;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cross-batch claims prevent duplicate wage dates, overtime, cycle settlement and monthly
 * contributions.
 */
@Service
public class HrmPayrollSourceClaimService {
  @Resource private HrmPayrollSourceClaimMapper claims;
  @Resource private HrmPayrollOffsetService offsets;
  @Resource private HrmPayrollRetroService retroService;
  @Resource private HrmPayrollLegalEntityMapper legalEntities;
  @Resource private HrmPayrollTaxWithholdingService taxWithholding;
  @Resource private HrmEmployeeMapper employees;
  @Resource private HrmPayrollScopeService scope;
  @Resource private HrmPayrollRunMapper runs;
  @Resource private HrmSalaryMonthRecordMapper batches;

  public void lockEmployees(HrmPayrollRunDO run) {
    Set<Long> ids = new TreeSet<>();
    for (HrmSalaryMonthEmployeeRecordDO e :
        JsonUtils.parseArray(run.getResultSnapshot(), HrmSalaryMonthEmployeeRecordDO.class))
      ids.add(e.getEmployeeId());
    for (Long id : ids)
      if (employees.selectOneForUpdate(HrmEmployeeDO::getId, id) == null)
        throw exception(PAYROLL_SOURCE_CLAIM_INVALID);
  }

  private HrmPayrollRunDO root(HrmPayrollRunDO latest) {
    Set<Long> visited = new HashSet<>();
    HrmPayrollRunDO row = latest;
    while (true) {
      if (row == null || !visited.add(row.getId())) throw exception(PAYROLL_SOURCE_CLAIM_INVALID);
      Object parent = JsonUtils.parseObject(row.getInputSnapshot(), Map.class).get("sourceRunId");
      if (parent == null) return row;
      if (!(parent instanceof Number)) throw exception(PAYROLL_SOURCE_CLAIM_INVALID);
      HrmPayrollRunDO prior = runs.selectById(((Number) parent).longValue());
      if (prior == null
          || !Objects.equals(prior.getMonthRecordId(), latest.getMonthRecordId())
          || prior.getVersion() >= row.getVersion()) throw exception(PAYROLL_SOURCE_CLAIM_INVALID);
      row = prior;
    }
  }

  private void add(
      Map<String, HrmPayrollSourceClaimDO> out,
      HrmSalaryMonthRecordDO batch,
      HrmPayrollRunDO latest,
      Long employee,
      String kind,
      String key,
      BigDecimal amount) {
    HrmPayrollSourceClaimDO c = new HrmPayrollSourceClaimDO();
    c.setEntityId(batch.getEntityId());
    c.setEmployeeId(employee);
    c.setBatchId(batch.getId());
    c.setRunId(latest.getId());
    c.setKind(kind);
    c.setSourceKey(key);
    c.setAmount(amount);
    c.setStatus("RESERVED");
    if (out.putIfAbsent(kind + ":" + key, c) != null) throw exception(PAYROLL_SOURCE_CLAIM_INVALID);
  }

  private Map<Long, String> taxContexts(HrmPayrollRunDO latest) {
    Set<Long> included = new HashSet<>();
    JsonUtils.parseArray(latest.getResultSnapshot(), HrmSalaryMonthEmployeeRecordDO.class)
        .forEach(row -> included.add(row.getEmployeeId()));
    Map<Long, String> out = new LinkedHashMap<>();
    Set<Long> visited = new HashSet<>();
    HrmPayrollRunDO row = latest;
    while (row != null) {
      if (!visited.add(row.getId())) throw exception(PAYROLL_SOURCE_CLAIM_INVALID);
      Map<String, Object> input = JsonUtils.parseObject(row.getInputSnapshot(), Map.class);
      Object contexts = input.get("withholdingContexts");
      if (contexts != null) {
        if (!(contexts instanceof List)) throw exception(PAYROLL_SOURCE_CLAIM_INVALID);
        Set<Long> local = new HashSet<>();
        for (Object value : (List<?>) contexts) {
          if (!(value instanceof String)) throw exception(PAYROLL_SOURCE_CLAIM_INVALID);
          HrmPayrollTaxWithholdingService.Source source =
              JsonUtils.parseObject((String) value, HrmPayrollTaxWithholdingService.Source.class);
          if (source == null
              || source.getEmployeeId() == null
              || !local.add(source.getEmployeeId())) throw exception(PAYROLL_SOURCE_CLAIM_INVALID);
          if (included.contains(source.getEmployeeId()))
            out.putIfAbsent(source.getEmployeeId(), (String) value);
        }
      }
      Object parent = input.get("sourceRunId");
      if (parent == null) break;
      if (!(parent instanceof Number)) throw exception(PAYROLL_SOURCE_CLAIM_INVALID);
      HrmPayrollRunDO previous = runs.selectById(((Number) parent).longValue());
      if (previous == null
          || !Objects.equals(previous.getMonthRecordId(), latest.getMonthRecordId())
          || previous.getVersion() >= row.getVersion())
        throw exception(PAYROLL_SOURCE_CLAIM_INVALID);
      row = previous;
    }
    return out;
  }

  private void lockTaxEntity(Long id) {
    if (legalEntities.selectOneForUpdate(HrmPayrollLegalEntityDO::getId, id) == null)
      throw exception(PAYROLL_SOURCE_CLAIM_INVALID);
  }

  private List<HrmPayrollSourceClaimDO> required(
      HrmSalaryMonthRecordDO batch, HrmPayrollRunDO latest) {
    if (batch.getEntityId() == null || !("REGULAR".equals(batch.getBatchKey())||HrmPayrollSupplementService.isSupplement(batch)))
      throw exception(PAYROLL_SOURCE_CLAIM_INVALID);
    HrmPayrollRunDO base = root(latest);
    com.fasterxml.jackson.databind.JsonNode input = JsonUtils.parseTree(base.getInputSnapshot());
    Map<String, HrmPayrollSourceClaimDO> out = new TreeMap<>();
    if("REGULAR".equals(batch.getBatchKey())) {
    if (input.get("entityPayrollDates") == null
        || input.get("approvedInputs") == null
        || input.get("insurancePolicySources") == null)
      throw exception(PAYROLL_SOURCE_CLAIM_INVALID);
    Map<Long, Set<LocalDate>> dates =
        JsonUtils.parseObject(
            input.get("entityPayrollDates").toString(),
            new com.fasterxml.jackson.core.type.TypeReference<Map<Long, Set<LocalDate>>>() {});
    Map<Long, Map<String, HrmPayrollInputDO>> confirmed =
        JsonUtils.parseObject(
            input.get("approvedInputs").toString(),
            new com.fasterxml.jackson.core.type.TypeReference<
                Map<Long, Map<String, HrmPayrollInputDO>>>() {});
    Map<Long, HrmPayrollInsurancePolicyService.Quote> fees =
        JsonUtils.parseObject(
            input.get("insurancePolicySources").toString(),
            new com.fasterxml.jackson.core.type.TypeReference<
                Map<Long, HrmPayrollInsurancePolicyService.Quote>>() {});

    for (HrmSalaryMonthEmployeeRecordDO employee :
        JsonUtils.parseArray(latest.getResultSnapshot(), HrmSalaryMonthEmployeeRecordDO.class)) {
      Long id = employee.getEmployeeId();
      if (dates.get(id) == null
          || dates.get(id).isEmpty()
          || confirmed.get(id) == null
          || confirmed.get(id).get("OVERTIME") == null
          || fees.get(id) == null) throw exception(PAYROLL_SOURCE_CLAIM_INVALID);
      for (LocalDate date : new TreeSet<>(dates.get(id)))
        add(out, batch, latest, id, "WAGE_DAY", id + ":" + date, null);
      Map<String, Object> sources =
          JsonUtils.parseObject(confirmed.get(id).get("OVERTIME").getSourceSnapshot(), Map.class);
      if (!(sources.get("entries") instanceof List) || !(sources.get("cycles") instanceof List))
        throw exception(PAYROLL_SOURCE_CLAIM_INVALID);
      for (HrmPayrollTimeEntryDO e :
          JsonUtils.parseArray(
              JsonUtils.toJsonString(sources.get("entries")), HrmPayrollTimeEntryDO.class)) {
        if (!Objects.equals(e.getEmployeeId(), id)
            || !dates.get(id).contains(e.getStartTime().toLocalDate())
            || e.getSourceApplyId() == null) throw exception(PAYROLL_SOURCE_CLAIM_INVALID);
        add(
            out,
            batch,
            latest,
            id,
            "OVERTIME",
            id + ":" + e.getSourceApplyId() + ":" + e.getStartTime().toLocalDate(),
            e.getAmount());
      }
      for (HrmPayrollWorkCycleDO cycle :
          JsonUtils.parseArray(
              JsonUtils.toJsonString(sources.get("cycles")), HrmPayrollWorkCycleDO.class)) {
        if (!Objects.equals(cycle.getEmployeeId(), id)
            || !dates.get(id).contains(cycle.getCycleTo()))
          throw exception(PAYROLL_SOURCE_CLAIM_INVALID);
        add(
            out,
            batch,
            latest,
            id,
            "WORK_CYCLE",
            id + ":" + cycle.getCycleFrom() + ":" + cycle.getCycleTo(),
            cycle.getAmount());
      }
      HrmPayrollInsurancePolicyService.Quote q = fees.get(id);
      if ("INSURED".equals(q.getCoverage())
          && q.getPersonalInsurance()
                  .add(q.getPersonalFund())
                  .add(q.getCorporateInsurance())
                  .add(q.getCorporateFund())
                  .signum()
              > 0) {
        if (!Objects.equals(q.getPayerEntityId(), batch.getEntityId()))
          throw exception(PAYROLL_SOURCE_CLAIM_INVALID);
        add(
            out,
            batch,
            latest,
            id,
            "INSURANCE_MONTH",
            id + ":" + YearMonth.of(q.getYear(), q.getMonth()),
            q.getPersonalInsurance()
                .add(q.getPersonalFund())
                .add(q.getCorporateInsurance())
                .add(q.getCorporateFund()));
      }
    }
    } else {
      List<HrmPayrollRetroDO> retro=retroService.sources(batch.getId());
      if(retro.isEmpty()) throw exception(PAYROLL_SOURCE_CLAIM_INVALID);
      Map<Long,BigDecimal> amounts=new TreeMap<>();for(HrmPayrollRetroDO r:retro) {
        if(r.getDelta().signum()<=0) throw exception(PAYROLL_SOURCE_CLAIM_INVALID);
        amounts.merge(r.getEmployeeId(),r.getDelta(),BigDecimal::add);
      }
      Map<Long,String> contexts=taxContexts(latest);
      if(!contexts.keySet().equals(amounts.keySet())) throw exception(PAYROLL_SOURCE_CLAIM_INVALID);
      Set<Long> checked=new HashSet<>();
      for(HrmSalaryMonthEmployeeRecordDO row:JsonUtils.parseArray(latest.getResultSnapshot(),HrmSalaryMonthEmployeeRecordDO.class)) {
        if(!checked.add(row.getEmployeeId()) || !amounts.containsKey(row.getEmployeeId()) || row.getExpectedPaySalary()==null
          || row.getExpectedPaySalary().compareTo(amounts.get(row.getEmployeeId()))!=0) throw exception(PAYROLL_SOURCE_CLAIM_INVALID);
        HrmPayrollTaxWithholdingService.Source tax=JsonUtils.parseObject(row.getTaxSourceSnapshot(),HrmPayrollTaxWithholdingService.Source.class);
        if(!Objects.equals(contexts.get(row.getEmployeeId()),row.getTaxSourceSnapshot()) || tax==null || tax.getCurrentIncome().compareTo(amounts.get(row.getEmployeeId()))!=0 || tax.getCurrentSocialDeduction().signum()!=0
          || tax.bonusAmount().signum()!=0 || tax.getQuote().getCurrentWithholding().compareTo(row.getPersonalTax())!=0
          || row.getRealPaySalary().compareTo(row.getExpectedPaySalary().subtract(row.getPersonalTax()))!=0) throw exception(PAYROLL_SOURCE_CLAIM_INVALID);
      }
      if(!checked.equals(amounts.keySet())) throw exception(PAYROLL_SOURCE_CLAIM_INVALID);
    }
    retroService.validateInputSnapshot(base.getInputSnapshot(),batch.getId());
    for(HrmPayrollRetroDO r:retroService.sources(batch.getId()))
      add(out,batch,latest,r.getEmployeeId(),"RETRO_WAGE",r.getId().toString(),r.getDelta());
    for (Map.Entry<Long, String> entry : taxContexts(latest).entrySet()) {
      HrmPayrollTaxWithholdingService.Source source =
          JsonUtils.parseObject(entry.getValue(), HrmPayrollTaxWithholdingService.Source.class);
      if (!Objects.equals(source.getEntityId(), batch.getEntityId())
          || source.getProfile() == null
          || source.getExpectedPaymentDate() == null
          || source.getQuote() == null
          || source.getProfile().getTaxpayerKey() == null
          || source.getProfile().getTaxRegistrationNumber() == null)
        throw exception(PAYROLL_SOURCE_CLAIM_INVALID);
      String prefix =
          cn.hutool.crypto.digest.DigestUtil.sha256Hex(
              batch.getEntityId()
                  + ":"
                  + source.getProfile().getTaxpayerKey()
                  + ":"
                  + source.getExpectedPaymentDate().getYear());
      add(
          out,
          batch,
          latest,
          entry.getKey(),
          "TAX_YEAR",
          prefix + ":" + latest.getId(),
          source.getQuote().getCurrentSalaryWithholding());
      if ("SEPARATE".equals(source.bonusMode())) {
        if (source.bonusAmount().signum() <= 0
            || !Objects.equals(source.getBatchId(), batch.getId()))
          throw exception(PAYROLL_SOURCE_CLAIM_INVALID);
        add(
            out,
            batch,
            latest,
            entry.getKey(),
            "BONUS_YEAR",
            HrmPayrollBonusService.quotaKey(
                source.getProfile().getTaxpayerKey(), source.getExpectedPaymentDate().getYear()),
            source.bonusAmount());
      }
    }
    offsets.validateInputSnapshot(base.getInputSnapshot(),batch.getId(),cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId());
    Set<Long> salaryEmployees=new HashSet<>();for(HrmSalaryMonthEmployeeRecordDO row:JsonUtils.parseArray(latest.getResultSnapshot(),HrmSalaryMonthEmployeeRecordDO.class))salaryEmployees.add(row.getEmployeeId());
    List<HrmPayrollOffsetService.Source> settlementSources=input.get("settlementOffsets")==null?Collections.emptyList():JsonUtils.parseArray(input.get("settlementOffsets").toString(),HrmPayrollOffsetService.Source.class);
    for(HrmPayrollOffsetService.Source source:settlementSources){if(!salaryEmployees.contains(source.getEmployeeId())||!Objects.equals(source.getBatchId(),batch.getId())||!Objects.equals(source.getEntityId(),batch.getEntityId()))throw exception(PAYROLL_OFFSET_INVALID);
      add(out,batch,latest,source.getEmployeeId(),"SETTLEMENT_OFFSET",source.getId().toString(),source.getNetAmount());
      HrmPayrollTaxWithholdingService.Source context=JsonUtils.parseObject(taxContexts(latest).get(source.getEmployeeId()),HrmPayrollTaxWithholdingService.Source.class);if(context==null||context.getProfile()==null||!Objects.equals(source.getTaxpayerKeyHash(),cn.hutool.crypto.digest.DigestUtil.sha256Hex(JsonUtils.toJsonString(context.getProfile().getTaxpayerKey()))))throw exception(PAYROLL_OFFSET_INVALID);
      String prefix=cn.hutool.crypto.digest.DigestUtil.sha256Hex(batch.getEntityId()+":"+context.getProfile().getTaxpayerKey()+":"+source.getOriginYear());String key=prefix+":"+latest.getId();if(!out.containsKey("TAX_YEAR:"+key))add(out,batch,latest,source.getEmployeeId(),"TAX_YEAR",key,BigDecimal.ZERO);
    }
    return new ArrayList<>(out.values());
  }

  @Transactional(
      rollbackFor = Exception.class,
      isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
  public void reserve(HrmSalaryMonthRecordDO batch, HrmPayrollRunDO run, Long actor) {
    scope.check(batch, "APPROVE", actor);
    lockEmployees(run);
    List<HrmPayrollSourceClaimDO> required = required(batch, run);
    if (required.stream().anyMatch(c -> "TAX_YEAR".equals(c.getKind()))) {
      lockTaxEntity(batch.getEntityId());
      for (String snapshot : taxContexts(run).values()) taxWithholding.validate(snapshot);
    }
    for (HrmPayrollSourceClaimDO c : required) {
      if ("TAX_YEAR".equals(c.getKind())) {
        String prefix = c.getSourceKey().substring(0, c.getSourceKey().lastIndexOf(':') + 1);
        HrmPayrollSourceClaimDO pending =
            claims.selectOne(
                new LambdaQueryWrapperX<HrmPayrollSourceClaimDO>()
                    .eq(HrmPayrollSourceClaimDO::getKind, "TAX_YEAR")
                    .eq(HrmPayrollSourceClaimDO::getStatus, "RESERVED")
                    .likeRight(HrmPayrollSourceClaimDO::getSourceKey, prefix));
        if (pending != null
            && (!Objects.equals(pending.getBatchId(), batch.getId())
                || !Objects.equals(pending.getRunId(), run.getId())))
          throw exception(PAYROLL_SOURCE_CLAIM_INVALID);
      }
      HrmPayrollSourceClaimDO existing =
          claims.selectOne(
              new LambdaQueryWrapperX<HrmPayrollSourceClaimDO>()
                  .eq(HrmPayrollSourceClaimDO::getKind, c.getKind())
                  .eq(HrmPayrollSourceClaimDO::getSourceKey, c.getSourceKey())
                  .in(HrmPayrollSourceClaimDO::getStatus, "RESERVED", "PAID"));
      if (existing != null) {
        if (Objects.equals(existing.getBatchId(), batch.getId())
            && Objects.equals(existing.getRunId(), run.getId())) continue;
        throw exception(PAYROLL_SOURCE_CLAIM_INVALID);
      }
      try {
        claims.insert(c);
      } catch (org.springframework.dao.DuplicateKeyException ex) {
        throw exception(PAYROLL_SOURCE_CLAIM_INVALID);
      }
    }
  }

  @Transactional(rollbackFor = Exception.class)
  public void release(Long batchId, Long runId, String reason, Long actor) {
    HrmSalaryMonthRecordDO batch = batches.selectByIdForUpdate(batchId);
    scope.check(batch, "MANAGE", actor);
    if (!Objects.equals(batch.getStatus(), 15)
        || reason == null
        || reason.trim().isEmpty()
        || reason.length() > 1000) throw exception(PAYROLL_SOURCE_CLAIM_INVALID);
    List<HrmPayrollSourceClaimDO> rows =
        claims.selectList(
            new LambdaQueryWrapperX<HrmPayrollSourceClaimDO>()
                .eq(HrmPayrollSourceClaimDO::getBatchId, batchId)
                .eq(HrmPayrollSourceClaimDO::getRunId, runId));
    if (rows.stream().anyMatch(c -> !"RESERVED".equals(c.getStatus())))
      throw exception(PAYROLL_SOURCE_CLAIM_INVALID);
    for (HrmPayrollSourceClaimDO c : rows) {
      c.setStatus("RELEASED");
      c.setReleasedBy(actor);
      c.setReleaseReason(reason);
      claims.updateById(c);
    }
  }

  public void assertReserved(Long batchId, Long runId) {
    HrmSalaryMonthRecordDO batch = batches.selectById(batchId);
    HrmPayrollRunDO run = runs.selectById(runId);
    if (run == null || !Objects.equals(run.getMonthRecordId(), batchId))
      throw exception(PAYROLL_SOURCE_CLAIM_INVALID);
    List<HrmPayrollSourceClaimDO> needed = required(batch, run);
    List<HrmPayrollSourceClaimDO> actual =
        claims.selectList(
            new LambdaQueryWrapperX<HrmPayrollSourceClaimDO>()
                .eq(HrmPayrollSourceClaimDO::getBatchId, batchId)
                .eq(HrmPayrollSourceClaimDO::getRunId, runId)
                .in(HrmPayrollSourceClaimDO::getStatus, "RESERVED", "PAID"));
    Set<String> expected = new TreeSet<>(), found = new TreeSet<>();
    needed.forEach(c -> expected.add(c.getKind() + ":" + c.getSourceKey()));
    actual.forEach(c -> found.add(c.getKind() + ":" + c.getSourceKey()));
    if (!expected.equals(found)) throw exception(PAYROLL_SOURCE_CLAIM_INVALID);
    Map<Long, String> contexts = taxContexts(run);
    for (HrmPayrollSourceClaimDO c : actual)
      if ("TAX_YEAR".equals(c.getKind()) && "RESERVED".equals(c.getStatus())) {
        String snapshot = contexts.get(c.getEmployeeId());
        if (snapshot == null) throw exception(PAYROLL_SOURCE_CLAIM_INVALID);
        taxWithholding.validate(snapshot);
      }
  }

  @Transactional(rollbackFor = Exception.class)
  public void paid(Long batchId, Long runId, Long employeeId) {
    boolean tax =
        claims.selectCount(
                new LambdaQueryWrapperX<HrmPayrollSourceClaimDO>()
                    .eq(HrmPayrollSourceClaimDO::getBatchId, batchId)
                    .eq(HrmPayrollSourceClaimDO::getRunId, runId)
                    .eq(HrmPayrollSourceClaimDO::getEmployeeId, employeeId)
                    .eq(HrmPayrollSourceClaimDO::getKind, "TAX_YEAR"))
            > 0;
    if (tax) {
      if (employees.selectOneForUpdate(HrmEmployeeDO::getId, employeeId) == null)
        throw exception(PAYROLL_SOURCE_CLAIM_INVALID);
      HrmSalaryMonthRecordDO batch = batches.selectById(batchId);
      if (batch == null) throw exception(PAYROLL_SOURCE_CLAIM_INVALID);
      lockTaxEntity(batch.getEntityId());
    }
    List<HrmPayrollSourceClaimDO> rows =
        claims.selectList(
            new LambdaQueryWrapperX<HrmPayrollSourceClaimDO>()
                .eq(HrmPayrollSourceClaimDO::getBatchId, batchId)
                .eq(HrmPayrollSourceClaimDO::getRunId, runId)
                .eq(HrmPayrollSourceClaimDO::getEmployeeId, employeeId)
                .eq(HrmPayrollSourceClaimDO::getStatus, "RESERVED"));
    for (HrmPayrollSourceClaimDO c : rows) {
      c.setStatus("PAID");
      claims.updateById(c);
    }
  }

  public List<HrmPayrollSourceClaimDO> list(Long batchId, Long actor) {
    scope.check(batches.selectById(batchId), "READ", actor);
    return claims.selectList(
        new LambdaQueryWrapperX<HrmPayrollSourceClaimDO>()
            .eq(HrmPayrollSourceClaimDO::getBatchId, batchId)
            .orderByDesc(HrmPayrollSourceClaimDO::getId));
  }
}
