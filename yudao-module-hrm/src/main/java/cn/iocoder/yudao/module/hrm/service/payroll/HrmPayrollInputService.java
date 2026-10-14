package cn.iocoder.yudao.module.hrm.service.payroll;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.hrm.controller.admin.attendance.vo.statistics.HrmAttendanceMonthRecordRespVO;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollInputReqVO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.salary.monthrecord.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.employee.info.HrmEmployeeMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.HrmPayrollInputMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.salary.monthrecord.*;
import cn.iocoder.yudao.module.hrm.enums.employee.info.HrmEmployeeEntryStatusEnum;
import cn.iocoder.yudao.module.oa.api.payroll.OaPayrollSourceApi;
import cn.iocoder.yudao.module.oa.api.payroll.dto.OaPayrollOvertimeDTO;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import javax.validation.Validator;
import lombok.Data;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Inputs are confirmed against actual scope and sources. Missing is never assumed to be zero. */
@Service
public class HrmPayrollInputService {
  public static final List<String> KINDS =
      Collections.unmodifiableList(
          Arrays.asList("ATTENDANCE", "OVERTIME", "DEDUCTIONS", "CUMULATIVE_TAX"));
  public static final List<String> ATTENDANCE_FIELDS =
      Collections.unmodifiableList(
          Arrays.asList(
              "ACTUAL_DAYS",
              "NEED_DAYS",
              "190101",
              "190102",
              "190103",
              "190104",
              "190105",
              "190106"));
  public static final List<String> DEDUCTION_FIELDS =
      Collections.unmodifiableList(Arrays.asList("260101", "260102", "260103", "260104", "260105", "260106"));
  public static final List<String> CUMULATIVE_FIELDS =
      Collections.unmodifiableList(Arrays.asList("250101", "250102", "250103", "250105"));
  @Resource private HrmPayrollInputMapper inputs;
  @Resource private HrmPayrollTaxDeductionService deductions;
  @Resource private HrmPayrollTaxCashLedgerService cashLedger;
  @Resource private HrmSalaryMonthRecordMapper batches;
  @Resource private HrmSalaryMonthEmployeeRecordMapper employeeRecords;
  @Resource private HrmEmployeeMapper employees;
  @Resource private HrmPayrollScopeService scope;
  @Resource private HrmPayrollWindowService window;
  @Resource private HrmPayrollWorkService work;
  @Resource private HrmPayrollAttendanceService attendance;
  @Resource private OaPayrollSourceApi oa;
  @Resource private Validator validator;

  @Data
  public static class Prepared {
    private Map<String, BigDecimal> values;
    private Map<String, Object> sources;
  }

  @Data
  public static class Readiness {
    private Long employeeId;
    private String employeeName;
    private String jobNumber;
    private String kind;
    private String status;
    private String reason;
    private Long inputId;
  }

  public HrmSalaryMonthRecordDO batch(Long id, String action, Long actor, boolean edit) {
    HrmSalaryMonthRecordDO b = edit ? batches.selectByIdForUpdate(id) : batches.selectById(id);
    if (b == null) throw exception(SALARY_MONTH_RECORD_NOT_EXISTS);
    scope.check(b, action, actor);
    if (b.getEntityId() == null
        || edit && !(Objects.equals(b.getStatus(), 5) || Objects.equals(b.getStatus(), 11)))
      throw exception(PAYROLL_INPUT_INVALID);
    return b;
  }

  public List<HrmEmployeeDO> roster(HrmSalaryMonthRecordDO b) {
    Set<Long> ids = scope.payrollDates(b).keySet();
    if (ids.isEmpty()) return Collections.emptyList();
    List<HrmEmployeeDO> result = employees.selectByIds(ids);
    result.removeIf(
        e ->
            e.getEntryTime() != null
                    && e.getEntryTime().toLocalDate().isAfter(b.getEndTime().toLocalDate())
                || !(HrmEmployeeEntryStatusEnum.ACTIVE_STATUSES.contains(e.getEntryStatus())
                    || Objects.equals(
                            e.getEntryStatus(), HrmEmployeeEntryStatusEnum.LEFT.getStatus())
                        && e.getLeaveTime() != null
                        && !e.getLeaveTime()
                            .toLocalDate()
                            .isBefore(b.getStartTime().toLocalDate())));
    result.sort(Comparator.comparing(HrmEmployeeDO::getId));
    return result;
  }

  public List<String> fields(String kind) {
    return "ATTENDANCE".equals(kind)
        ? ATTENDANCE_FIELDS
        : "OVERTIME".equals(kind)
            ? Collections.singletonList("180101")
            : "DEDUCTIONS".equals(kind)
                ? DEDUCTION_FIELDS
                : "CUMULATIVE_TAX".equals(kind) ? CUMULATIVE_FIELDS : Collections.emptyList();
  }

  private void amounts(
      Map<String, BigDecimal> values, List<String> fields, HrmSalaryMonthRecordDO batch) {
    if (values == null || !values.keySet().equals(new HashSet<>(fields)))
      throw exception(PAYROLL_INPUT_INVALID);
    long days =
        java.time.temporal.ChronoUnit.DAYS.between(
                batch.getStartTime().toLocalDate(), batch.getEndTime().toLocalDate())
            + 1;
    for (Map.Entry<String, BigDecimal> entry : values.entrySet()) {
      BigDecimal v = entry.getValue();
      if (v == null || v.signum() < 0 || v.scale() > 2 || (long) v.precision() - v.scale() > 12)
        throw exception(PAYROLL_INPUT_INVALID);
      if (entry.getKey().endsWith("_DAYS") && v.compareTo(BigDecimal.valueOf(days)) > 0)
        throw exception(PAYROLL_INPUT_INVALID);
    }
    if (values.containsKey("ACTUAL_DAYS")
        && values.get("ACTUAL_DAYS").compareTo(values.get("NEED_DAYS")) > 0)
      throw exception(PAYROLL_INPUT_INVALID);
  }

  private HrmSalaryMonthEmployeeRecordDO priorPaid(HrmSalaryMonthRecordDO b, Long employeeId) {
    List<HrmSalaryMonthRecordDO> paid =
        batches.selectList(
            new LambdaQueryWrapperX<HrmSalaryMonthRecordDO>()
                .eq(HrmSalaryMonthRecordDO::getEntityId, b.getEntityId())
                .eq(HrmSalaryMonthRecordDO::getYear, b.getYear())
                .lt(HrmSalaryMonthRecordDO::getMonth, b.getMonth())
                .in(HrmSalaryMonthRecordDO::getStatus, 17, 10)
                .orderByDesc(HrmSalaryMonthRecordDO::getMonth)
                .orderByDesc(HrmSalaryMonthRecordDO::getId));
    for (HrmSalaryMonthRecordDO batch : paid) {
      HrmSalaryMonthEmployeeRecordDO row =
          employeeRecords.selectOne(
              new LambdaQueryWrapperX<HrmSalaryMonthEmployeeRecordDO>()
                  .eq(HrmSalaryMonthEmployeeRecordDO::getMonthRecordId, batch.getId())
                  .eq(HrmSalaryMonthEmployeeRecordDO::getEmployeeId, employeeId));
      if (row != null) return row;
    }
    return null;
  }

  private Map<String, BigDecimal> cumulative(HrmSalaryMonthEmployeeRecordDO prior) {
    Map<Integer, BigDecimal> values = new HashMap<>();
    if (prior.getOptionValues() != null)
      prior.getOptionValues().forEach(v -> values.put(v.getCode(), v.getValue()));
    Map<String, BigDecimal> result = new LinkedHashMap<>();
    result.put("250101", values.get(270101));
    result.put("250102", values.get(270102));
    result.put("250103", values.get(270103));
    BigDecimal before = values.get(250105);
    result.put(
        "250105",
        before == null || prior.getPersonalTax() == null
            ? null
            : before.add(prior.getPersonalTax()));
    return result;
  }

  private BigDecimal n(BigDecimal v) {
    if (v == null) throw exception(PAYROLL_INPUT_INVALID);
    return v;
  }

  private Prepared capture(HrmSalaryMonthRecordDO b, HrmEmployeeDO e, HrmPayrollInputReqVO r) {
    if (!validator.validate(r).isEmpty()
        || !Objects.equals(b.getId(), r.getBatchId())
        || !Objects.equals(e.getId(), r.getEmployeeId())) throw exception(PAYROLL_INPUT_INVALID);
    Set<LocalDate> dates = scope.payrollDates(b).get(e.getId());
    if (dates == null || dates.isEmpty()) throw exception(PAYROLL_INPUT_INVALID);
    Map<String, Object> sources = new LinkedHashMap<>();
    sources.put("batchId", b.getId());
    sources.put("entityId", b.getEntityId());
    sources.put("employeeId", e.getId());
    sources.put("userId", e.getUserId());
    sources.put("entryTime", e.getEntryTime());
    sources.put("leaveTime", e.getLeaveTime());
    sources.put("entryStatus", e.getEntryStatus());
    sources.put("dates", new TreeSet<>(dates));
    sources.put("from", b.getStartTime().toLocalDate());
    sources.put("to", b.getEndTime().toLocalDate());
    Map<String, BigDecimal> values = new LinkedHashMap<>();
    if (Arrays.asList("ATTENDANCE", "OVERTIME").contains(r.getKind())) {
      List<HrmPayrollWorkVarianceDO> variances =
          work.requireVarianceReady(b.getEntityId(), e.getId(), dates);
      if (variances != null && !variances.isEmpty()) sources.put("workVariances", variances);
    }
    if ("ATTENDANCE".equals(r.getKind())) {
      List<HrmPayrollWorkRuleDO> policies =
          work.rules(e.getId()).stream()
              .filter(rule -> Objects.equals(rule.getEntityId(), b.getEntityId()))
              .collect(Collectors.toList());
      Set<LocalDate> standardDates = new TreeSet<>();
      Map<Long, HrmPayrollWorkRuleDO> appliedPolicies = new TreeMap<>();
      for (LocalDate date : dates) {
        List<HrmPayrollWorkRuleDO> applicable =
            policies.stream()
                .filter(rule -> HrmPayrollWorkService.applies(rule, date))
                .collect(Collectors.toList());
        if (applicable.size() != 1) throw exception(PAYROLL_WORK_INVALID);
        HrmPayrollWorkRuleDO policy = applicable.get(0);
        if (!Arrays.asList("STANDARD", "COMPREHENSIVE", "IRREGULAR")
            .contains(policy.getSystemType())) throw exception(PAYROLL_WORK_INVALID);
        appliedPolicies.put(policy.getId(), policy);
        if ("STANDARD".equals(policy.getSystemType())) standardDates.add(date);
      }
      sources.put("attendanceWorkRules", new ArrayList<>(appliedPolicies.values()));
      sources.put("standardPenaltyDates", standardDates);
      if ("SYNC".equals(r.getMode())) {
        HrmAttendanceMonthRecordRespVO a = window.attendance(e.getId(), dates);
        if (a == null || a.getAttendDays() == null) throw exception(PAYROLL_INPUT_INVALID);
        values.put("ACTUAL_DAYS", n(a.getActualDays()));
        values.put("NEED_DAYS", BigDecimal.valueOf(a.getAttendDays()));
        HrmAttendanceMonthRecordRespVO penalties =
            standardDates.isEmpty()
                ? null
                : standardDates.equals(dates) ? a : window.attendance(e.getId(), standardDates);
        if (!standardDates.isEmpty() && penalties == null) throw exception(PAYROLL_INPUT_INVALID);
        values.put(
            "190101", penalties == null ? BigDecimal.ZERO : n(penalties.getLateDeductAmount()));
        values.put(
            "190102", penalties == null ? BigDecimal.ZERO : n(penalties.getEarlyDeductAmount()));
        values.put(
            "190103",
            penalties == null ? BigDecimal.ZERO : n(penalties.getAbsenteeismDeductAmount()));
        values.put("190104", BigDecimal.ZERO);
        values.put(
            "190105", penalties == null ? BigDecimal.ZERO : n(penalties.getMisscardDeductAmount()));
        values.put(
            "190106",
            n(a.getAttendanceDeductAmount())
                .subtract(a.getLateDeductAmount())
                .subtract(a.getEarlyDeductAmount())
                .subtract(a.getAbsenteeismDeductAmount())
                .subtract(a.getMisscardDeductAmount()));
        List<Object> raw = new ArrayList<>();
        for (LocalDate[] range : window.intervals(dates))
          raw.add(attendance.snapshot(range[0], range[1], Collections.singletonList(e.getId())));
        sources.put("attendance", raw);
        sources.put("statistics", a);
        sources.put("standardPenaltyStatistics", penalties);
      } else if ("IMPORT".equals(r.getMode()))
        values.putAll(r.getValues() == null ? Collections.emptyMap() : r.getValues());
      else throw exception(PAYROLL_INPUT_INVALID);
    } else if ("OVERTIME".equals(r.getKind())) {
      if (!Arrays.asList("SYNC", "ZERO").contains(r.getMode()))
        throw exception(PAYROLL_INPUT_INVALID);
      List<HrmPayrollWorkRuleDO> rules =
          work.rules(e.getId()).stream()
              .filter(
                  rule ->
                      "APPROVED".equals(rule.getStatus())
                          && Objects.equals(rule.getEntityId(), b.getEntityId()))
              .collect(Collectors.toList());
      Set<Long> appliedRules = new TreeSet<>();
      for (LocalDate date : dates) {
        List<HrmPayrollWorkRuleDO> applicable =
            rules.stream()
                .filter(
                    rule ->
                        !date.isBefore(rule.getEffectiveFrom())
                            && !date.isAfter(rule.getEffectiveTo())
                            && (rule.getSupersededFrom() == null
                                || date.isBefore(rule.getSupersededFrom())))
                .collect(Collectors.toList());
        if (applicable.size() != 1) throw exception(PAYROLL_WORK_INVALID);
        appliedRules.add(applicable.get(0).getId());
      }
      sources.put(
          "rules",
          rules.stream()
              .filter(rule -> appliedRules.contains(rule.getId()))
              .sorted(Comparator.comparing(HrmPayrollWorkRuleDO::getId))
              .collect(Collectors.toList()));
      List<HrmPayrollTimeEntryDO> entries = new ArrayList<>();
      List<HrmPayrollWorkCycleDO> cycles = new ArrayList<>();
      Map<Long, OaPayrollOvertimeDTO> approved = new TreeMap<>();
      for (LocalDate[] range : window.intervals(dates)) {
        entries.addAll(
            work.entries(e.getId(), range[0], range[1]).stream()
                .filter(row -> Objects.equals(row.getEntityId(), b.getEntityId()))
                .collect(Collectors.toList()));
        cycles.addAll(
            work.cycles(e.getId(), range[0], range[1]).stream()
                .filter(row -> Objects.equals(row.getEntityId(), b.getEntityId()))
                .collect(Collectors.toList()));
        for (OaPayrollOvertimeDTO source :
            oa.getApprovedOvertimeList(
                e.getUserId(), range[0].atStartOfDay(), range[1].plusDays(1).atStartOfDay()))
          approved.put(source.getId(), source);
      }
      for (OaPayrollOvertimeDTO approvedSource : approved.values()) {
        for (LocalDate date : dates)
          if (approvedSource.getStartTime().isBefore(date.plusDays(1).atStartOfDay())
              && approvedSource.getEndTime().isAfter(date.atStartOfDay())
              && entries.stream()
                  .noneMatch(
                      entry ->
                          Objects.equals(entry.getSourceApplyId(), approvedSource.getId())
                              && entry.getStartTime().toLocalDate().equals(date)))
            throw exception(PAYROLL_WORK_INVALID);
      }
      BigDecimal earned = window.earningsForEntity(e.getId(), b.getEntityId(), dates);
      if (earned == null) throw exception(PAYROLL_WORK_INVALID);
      boolean recorded = !entries.isEmpty() || !cycles.isEmpty();
      if ("ZERO".equals(r.getMode()) && (recorded || !approved.isEmpty() || earned.signum() != 0)
          || "SYNC".equals(r.getMode()) && !recorded) throw exception(PAYROLL_INPUT_INVALID);
      values.put("180101", earned);
      sources.put("entries", entries);
      sources.put("cycles", cycles);
      sources.put("approvedOvertime", new ArrayList<>(approved.values()));
    } else if ("DEDUCTIONS".equals(r.getKind())) {
      LocalDate paymentDate=b.getExpectedPaymentDate();
      if(paymentDate!=null) sources.put("expectedPaymentDate",paymentDate);
      HrmPayrollTaxDeductionDO declaration = deductions.source(b.getEntityId(), e.getId(), paymentDate==null?b.getYear():paymentDate.getYear(), paymentDate==null?b.getMonth():paymentDate.getMonthValue());
      if(declaration != null) sources.put("taxDeductionDeclaration", declaration);
      if ("ZERO".equals(r.getMode()))
        DEDUCTION_FIELDS.forEach(code -> values.put(code, BigDecimal.ZERO));
      else if ("SYNC".equals(r.getMode()) && declaration != null) values.putAll(deductions.values(declaration));
      else if ("IMPORT".equals(r.getMode()) && r.getValues() != null) values.putAll(r.getValues());
      else throw exception(PAYROLL_INPUT_INVALID);
      if(declaration != null) {
        Map<String,BigDecimal> approved=deductions.values(declaration);
        for(String code:DEDUCTION_FIELDS)
          if(values.get(code)==null || approved.get(code)==null || values.get(code).compareTo(approved.get(code))!=0) throw exception(PAYROLL_INPUT_INVALID);
      }
    } else if ("CUMULATIVE_TAX".equals(r.getKind()) && "CASH".equals(r.getMode())) {
      LocalDate paymentDate=b.getExpectedPaymentDate();
      if(paymentDate==null) throw exception(PAYROLL_INPUT_INVALID);
      LocalDate today=LocalDate.now(ZoneId.of("Asia/Shanghai")), asOf=paymentDate.isAfter(today)?today:paymentDate;
      if(asOf.getYear()!=paymentDate.getYear()) throw exception(PAYROLL_INPUT_INVALID);
      cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollTaxCashLedgerVO ledger=cashLedger.forApprovedPayroll(b.getEntityId(),e.getId(),paymentDate.getYear(),asOf);
      if(ledger==null || !ledger.isReady()) throw exception(PAYROLL_INPUT_INVALID);
      values.put("250101",ledger.getIncome());values.put("250102",ledger.getBasicDeduction());
      values.put("250103",ledger.getSocialDeduction());values.put("250105",ledger.getWithheld());
      ledger.setAsOfDate(null);
      sources.put("expectedPaymentDate",paymentDate);sources.put("cashLedger",ledger);
    } else if ("CUMULATIVE_TAX".equals(r.getKind())) {
      HrmSalaryMonthEmployeeRecordDO prior = priorPaid(b, e.getId());
      sources.put("priorPaidRecord", prior);
      if ("ZERO".equals(r.getMode())) {
        if (prior != null) throw exception(PAYROLL_INPUT_INVALID);
        CUMULATIVE_FIELDS.forEach(code -> values.put(code, BigDecimal.ZERO));
      } else if ("SYNC".equals(r.getMode())) {
        if (prior == null) throw exception(PAYROLL_INPUT_INVALID);
        values.putAll(cumulative(prior));
      } else if ("IMPORT".equals(r.getMode()) && r.getValues() != null) {
        values.putAll(r.getValues());
        if (prior != null) {
          Map<String, BigDecimal> known = cumulative(prior);
          for (String code : CUMULATIVE_FIELDS)
            if (known.get(code) == null
                || values.get(code) == null
                || known.get(code).compareTo(values.get(code)) != 0)
              throw exception(PAYROLL_INPUT_INVALID);
        }
      } else throw exception(PAYROLL_INPUT_INVALID);
    } else throw exception(PAYROLL_INPUT_INVALID);
    amounts(values, fields(r.getKind()), b);
    if (!"IMPORT".equals(r.getMode()) && r.getValues() != null && !r.getValues().isEmpty())
      throw exception(PAYROLL_INPUT_INVALID);
    Prepared result = new Prepared();
    result.setValues(values);
    result.setSources(sources);
    return result;
  }

  public Prepared inspect(HrmPayrollInputReqVO request, Long actor) {
    HrmSalaryMonthRecordDO b = batch(request.getBatchId(), "MANAGE", actor, false);
    HrmEmployeeDO e =
        roster(b).stream()
            .filter(row -> Objects.equals(row.getId(), request.getEmployeeId()))
            .findFirst()
            .orElseThrow(() -> exception(PAYROLL_INPUT_INVALID));
    return capture(b, e, request);
  }

  @Transactional(rollbackFor = Exception.class)
  public Long submit(HrmPayrollInputReqVO request, Long actor) {
    HrmSalaryMonthRecordDO b = batch(request.getBatchId(), "MANAGE", actor, true);
    Prepared p = inspect(request, actor);
    HrmPayrollInputDO last = latest(b.getId(), request.getEmployeeId(), request.getKind());
    if (last != null && "PENDING".equals(last.getStatus())) throw exception(PAYROLL_INPUT_INVALID);
    HrmPayrollInputDO row = new HrmPayrollInputDO();
    row.setBatchId(b.getId());
    row.setEntityId(b.getEntityId());
    row.setEmployeeId(request.getEmployeeId());
    row.setKind(request.getKind());
    row.setMode(request.getMode());
    row.setEvidence(request.getEvidence());
    row.setPayload(JsonUtils.toJsonString(p.getValues()));
    row.setSourceSnapshot(JsonUtils.toJsonString(p.getSources()));
    row.setCreator(String.valueOf(actor));
    row.setVersion(last == null ? 1 : last.getVersion() + 1);
    row.setStatus("PENDING");
    inputs.insert(row);
    return row.getId();
  }

  private HrmPayrollInputDO latest(Long batch, Long employee, String kind) {
    return inputs.selectOne(
        new LambdaQueryWrapperX<HrmPayrollInputDO>()
            .eq(HrmPayrollInputDO::getBatchId, batch)
            .eq(HrmPayrollInputDO::getEmployeeId, employee)
            .eq(HrmPayrollInputDO::getKind, kind)
            .orderByDesc(HrmPayrollInputDO::getVersion)
            .last("LIMIT 1"));
  }

  private HrmPayrollInputReqVO request(HrmPayrollInputDO row) {
    HrmPayrollInputReqVO r =
        new HrmPayrollInputReqVO()
            .setBatchId(row.getBatchId())
            .setEmployeeId(row.getEmployeeId())
            .setKind(row.getKind())
            .setMode(row.getMode())
            .setEvidence(row.getEvidence());
    if ("IMPORT".equals(row.getMode()))
      r.setValues(
          JsonUtils.parseObject(
              row.getPayload(),
              new com.fasterxml.jackson.core.type.TypeReference<Map<String, BigDecimal>>() {}));
    return r;
  }

  private Prepared validate(HrmSalaryMonthRecordDO b, HrmEmployeeDO e, HrmPayrollInputDO row) {
    Prepared now = capture(b, e, request(row));
    if (!JsonUtils.parseTree(row.getSourceSnapshot())
            .equals(JsonUtils.parseTree(JsonUtils.toJsonString(now.getSources())))
        || !JsonUtils.parseTree(row.getPayload())
            .equals(JsonUtils.parseTree(JsonUtils.toJsonString(now.getValues()))))
      throw exception(PAYROLL_INPUT_INVALID);
    return now;
  }

  @Transactional(
      rollbackFor = Exception.class,
      isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
  public void review(Long id, boolean approve, String reason, Long actor) {
    if (reason == null || reason.trim().isEmpty() || reason.length() > 1000)
      throw exception(PAYROLL_INPUT_INVALID);
    HrmPayrollInputDO row = inputs.selectById(id);
    if (row == null) throw exception(PAYROLL_INPUT_INVALID);
    HrmSalaryMonthRecordDO b = batch(row.getBatchId(), "REVIEW", actor, true);
    HrmEmployeeDO subject = employees.selectByIdForUpdate(row.getEmployeeId());
    if (subject == null) throw exception(PAYROLL_INPUT_INVALID);
    if (Objects.equals(subject.getUserId(), actor)) throw exception(PAYROLL_SELF_APPROVAL);
    row = inputs.selectOneForUpdate(HrmPayrollInputDO::getId, id);
    if (!"PENDING".equals(row.getStatus())) throw exception(PAYROLL_INPUT_INVALID);
    if (Objects.equals(row.getCreator(), String.valueOf(actor)))
      throw exception(PAYROLL_SELF_APPROVAL);
    if (approve) {
      Long employeeId = row.getEmployeeId();
      HrmEmployeeDO e =
          roster(b).stream()
              .filter(item -> Objects.equals(item.getId(), employeeId))
              .findFirst()
              .orElseThrow(() -> exception(PAYROLL_INPUT_INVALID));
      validate(b, e, row);
    }
    row.setReviewedBy(actor);
    row.setStatus(approve ? "APPROVED" : "REJECTED");
    inputs.updateById(row);
    cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.HrmPayrollBatchEventDO event =
        new HrmPayrollBatchEventDO();
    event.setMonthRecordId(b.getId());
    event.setAction(approve ? "approve_input" : "reject_input");
    event.setActorId(actor);
    event.setReason(reason);
    event.setFromStatus(b.getStatus());
    event.setToStatus(b.getStatus());
    event.setSnapshot(JsonUtils.toJsonString(row));
    batchEvents.insert(event);
  }

  @Resource
  private cn.iocoder.yudao.module.hrm.dal.mysql.payroll.HrmPayrollBatchEventMapper batchEvents;

  public List<HrmPayrollInputDO> list(Long batchId, Long actor) {
    batch(batchId, "READ", actor, false);
    return inputs.selectList(
        new LambdaQueryWrapperX<HrmPayrollInputDO>()
            .eq(HrmPayrollInputDO::getBatchId, batchId)
            .orderByDesc(HrmPayrollInputDO::getId));
  }

  public List<Readiness> readiness(Long batchId, Long actor) {
    HrmSalaryMonthRecordDO b = batch(batchId, "READ", actor, false);
    List<Readiness> result = new ArrayList<>();
    for (HrmEmployeeDO e : roster(b))
      for (String kind : KINDS) {
        Readiness r = new Readiness();
        r.setEmployeeId(e.getId());
        r.setEmployeeName(e.getName());
        r.setJobNumber(e.getJobNumber());
        r.setKind(kind);
        HrmPayrollInputDO row = latest(batchId, e.getId(), kind);
        r.setInputId(row == null ? null : row.getId());
        r.setStatus(row == null ? "MISSING" : row.getStatus());
        r.setReason(row == null ? "尚未明确来源及零值/导入/同步状态" : row.getEvidence());
        if (row != null && "APPROVED".equals(row.getStatus()))
          try {
            validate(b, e, row);
          } catch (cn.iocoder.yudao.framework.common.exception.ServiceException ex) {
            r.setStatus("STALE");
            r.setReason(ex.getMessage());
          }
        result.add(r);
      }
    return result;
  }

  public Map<Long, Map<String, HrmPayrollInputDO>> forPayroll(
      HrmSalaryMonthRecordDO b, List<HrmEmployeeDO> roster, Long actor) {
    scope.check(b, "CALCULATE", actor);
    Map<Long, Map<String, HrmPayrollInputDO>> result = new LinkedHashMap<>();
    for (HrmEmployeeDO e : roster) {
      Map<String, HrmPayrollInputDO> employee = new LinkedHashMap<>();
      for (String kind : KINDS) {
        HrmPayrollInputDO row = latest(b.getId(), e.getId(), kind);
        if (row == null || !"APPROVED".equals(row.getStatus()))
          throw exception(PAYROLL_INPUT_INVALID);
        validate(b, e, row);
        employee.put(kind, row);
      }
      result.put(e.getId(), employee);
    }
    return result;
  }

  public Map<String, BigDecimal> values(HrmPayrollInputDO input) {
    return JsonUtils.parseObject(
        input.getPayload(),
        new com.fasterxml.jackson.core.type.TypeReference<Map<String, BigDecimal>>() {});
  }

  public void validateInputSnapshot(String json) {
    Map<String, Object> snapshot = JsonUtils.parseObject(json, Map.class);
    Object ids = snapshot.get("approvedInputs");
    if (!(ids instanceof Map)) {
      if (snapshot.containsKey("entityScope") && !snapshot.containsKey("sourceRunId"))
        throw exception(PAYROLL_INPUT_INVALID);
      return;
    }
    for (Object employeeValue : ((Map<?, ?>) ids).values())
      for (Object recordValue : ((Map<?, ?>) employeeValue).values()) {
        HrmPayrollInputDO old =
            JsonUtils.parseObject(JsonUtils.toJsonString(recordValue), HrmPayrollInputDO.class);
        HrmPayrollInputDO current = latest(old.getBatchId(), old.getEmployeeId(), old.getKind());
        if (current == null
            || !Objects.equals(old.getId(), current.getId())
            || !"APPROVED".equals(current.getStatus())) throw exception(PAYROLL_INPUT_INVALID);
        HrmSalaryMonthRecordDO b = batches.selectById(old.getBatchId());
        if (b == null) throw exception(PAYROLL_INPUT_INVALID);
        HrmEmployeeDO e = employees.selectById(old.getEmployeeId());
        if (e == null) throw exception(PAYROLL_INPUT_INVALID);
        validate(b, e, current);
      }
  }
}
