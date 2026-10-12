package cn.iocoder.yudao.module.hrm.service.payroll;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollInsuranceReqVO;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollInsuranceReqVO.Project;
import cn.iocoder.yudao.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.insurance.monthrecord.HrmInsuranceMonthEmployeeRecordDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.employee.info.HrmEmployeeMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.*;
import cn.iocoder.yudao.module.hrm.enums.insurance.config.HrmInsuranceProjectTypeEnum;
import java.math.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import javax.annotation.Resource;
import javax.validation.Validator;
import lombok.Data;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Approved monthly policies and employee declaration history; legacy mutable schemes are not
 * approvals.
 */
@Service
public class HrmPayrollInsurancePolicyService {
  @Resource private HrmPayrollInsurancePolicyMapper policies;
  @Resource private HrmPayrollInsuranceBaseMapper bases;
  @Resource private HrmPayrollInsuranceEventMapper events;
  @Resource private HrmEmployeeMapper employees;
  @Resource private HrmPayrollEntityService entities;
  @Resource private Validator validator;

  private BigDecimal n(BigDecimal v) {
    return v == null ? BigDecimal.ZERO : v;
  }

  private void reason(String text) {
    if (text == null || text.trim().isEmpty() || text.length() > 1000)
      throw exception(PAYROLL_INSURANCE_INVALID);
  }

  private void period(LocalDate from, LocalDate to) {
    if (from == null
        || to == null
        || from.getYear() < 2000
        || to.getYear() > 2100
        || to.isBefore(from)
        || from.getDayOfMonth() != 1
        || !to.equals(YearMonth.from(to).atEndOfMonth())
        || ChronoUnit.MONTHS.between(YearMonth.from(from), YearMonth.from(to)) > 11)
      throw exception(PAYROLL_INSURANCE_INVALID);
  }

  private void event(
      Long entityId,
      Long policyId,
      Long baseId,
      String action,
      String reason,
      Object snapshot,
      Long actor) {
    HrmPayrollInsuranceEventDO e = new HrmPayrollInsuranceEventDO();
    e.setEntityId(entityId);
    e.setPolicyId(policyId);
    e.setBaseId(baseId);
    e.setAction(action);
    e.setActorId(actor);
    e.setReason(reason);
    e.setSnapshot(JsonUtils.toJsonString(snapshot));
    events.insert(e);
  }

  private List<Project> projects(HrmPayrollInsurancePolicyDO p) {
    return JsonUtils.parseArray(p.getProjects(), Project.class);
  }

  public HrmPayrollInsurancePolicyDO policy(Long id, Long actor) {
    HrmPayrollInsurancePolicyDO policy = policies.selectById(id);
    if (policy == null) throw exception(PAYROLL_INSURANCE_INVALID);
    entities.check(policy.getEntityId(), "READ", actor);
    return policy;
  }

  public List<HrmPayrollInsurancePolicyDO> policies(Long entityId, Long actor) {
    entities.check(entityId, "READ", actor);
    return policies.selectList(
        new LambdaQueryWrapperX<HrmPayrollInsurancePolicyDO>()
            .eq(HrmPayrollInsurancePolicyDO::getEntityId, entityId)
            .orderByAsc(HrmPayrollInsurancePolicyDO::getCode)
            .orderByDesc(HrmPayrollInsurancePolicyDO::getVersion));
  }

  public List<HrmPayrollInsuranceBaseDO> bases(Long entityId, Long actor) {
    entities.check(entityId, "READ", actor);
    List<HrmPayrollInsuranceBaseDO> result =
        bases.selectList(
            new LambdaQueryWrapperX<HrmPayrollInsuranceBaseDO>()
                .eq(HrmPayrollInsuranceBaseDO::getEntityId, entityId)
                .orderByDesc(HrmPayrollInsuranceBaseDO::getId));
    for (HrmPayrollInsuranceBaseDO b : result) {
      HrmEmployeeDO employee = employees.selectById(b.getEmployeeId());
      if (employee != null) {
        b.setEmployeeName(employee.getName());
        b.setJobNumber(employee.getJobNumber());
      }
    }
    return result;
  }

  public List<HrmPayrollInsuranceEventDO> history(Long entityId, Long actor) {
    entities.check(entityId, "READ", actor);
    return events.selectList(
        new LambdaQueryWrapperX<HrmPayrollInsuranceEventDO>()
            .eq(HrmPayrollInsuranceEventDO::getEntityId, entityId)
            .orderByDesc(HrmPayrollInsuranceEventDO::getId));
  }

  @Transactional(rollbackFor = Exception.class)
  public Long createPolicy(HrmPayrollInsuranceReqVO r, Long actor) {
    if (!validator.validate(r).isEmpty()) throw exception(PAYROLL_INSURANCE_INVALID);
    period(r.getEffectiveFrom(), r.getEffectiveTo());
    entities.check(r.getEntityId(), "MANAGE", actor);
    entities.lockApproved(r.getEntityId());
    Set<String> codes = new HashSet<>();
    for (Project p : r.getProjects())
      if (!codes.add(p.getCode())
          || p.getMaximumBase().compareTo(p.getMinimumBase()) < 0
          || HrmInsuranceProjectTypeEnum.valueOf(p.getType()) == null)
        throw exception(PAYROLL_INSURANCE_INVALID);
    r.setCode(r.getCode().toUpperCase(Locale.ROOT));
    HrmPayrollInsurancePolicyDO latest =
        policies.selectOne(
            new LambdaQueryWrapperX<HrmPayrollInsurancePolicyDO>()
                .eq(HrmPayrollInsurancePolicyDO::getEntityId, r.getEntityId())
                .eq(HrmPayrollInsurancePolicyDO::getCode, r.getCode())
                .orderByDesc(HrmPayrollInsurancePolicyDO::getVersion)
                .last("LIMIT 1"));
    HrmPayrollInsurancePolicyDO p = BeanUtils.toBean(r, HrmPayrollInsurancePolicyDO.class);
    p.setProjects(JsonUtils.toJsonString(r.getProjects()));
    p.setVersion(latest == null ? 1 : latest.getVersion() + 1);
    p.setStatus("PENDING");
    p.setCreator(String.valueOf(actor));
    policies.insert(p);
    event(p.getEntityId(), p.getId(), null, "CREATE_POLICY", r.getEvidence(), p, actor);
    return p.getId();
  }

  @Transactional(rollbackFor = Exception.class)
  public void approvePolicy(Long id, String evidence, Long actor) {
    reason(evidence);
    HrmPayrollInsurancePolicyDO p = policies.selectById(id);
    if (p == null) throw exception(PAYROLL_INSURANCE_INVALID);
    entities.check(p.getEntityId(), "APPROVE", actor);
    entities.lockApproved(p.getEntityId());
    p = policies.selectOneForUpdate(HrmPayrollInsurancePolicyDO::getId, id);
    if (!"PENDING".equals(p.getStatus())) throw exception(PAYROLL_INSURANCE_INVALID);
    if (Objects.equals(p.getCreator(), String.valueOf(actor)))
      throw exception(PAYROLL_SELF_APPROVAL);
    HrmPayrollInsurancePolicyDO replaced = null;
    if (p.getReplacesId() != null) {
      replaced = policies.selectOneForUpdate(HrmPayrollInsurancePolicyDO::getId, p.getReplacesId());
      if (replaced == null
          || !"APPROVED".equals(replaced.getStatus())
          || !Objects.equals(replaced.getEntityId(), p.getEntityId())
          || !Objects.equals(replaced.getCode(), p.getCode())
          || !Objects.equals(replaced.getEffectiveFrom(), p.getEffectiveFrom())
          || p.getEffectiveTo().isAfter(replaced.getEffectiveTo()))
        throw exception(PAYROLL_INSURANCE_INVALID);
    }
    com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<HrmPayrollInsurancePolicyDO>
        overlap =
            new LambdaQueryWrapperX<HrmPayrollInsurancePolicyDO>()
                .eq(HrmPayrollInsurancePolicyDO::getEntityId, p.getEntityId())
                .eq(HrmPayrollInsurancePolicyDO::getCode, p.getCode())
                .eq(HrmPayrollInsurancePolicyDO::getStatus, "APPROVED")
                .le(HrmPayrollInsurancePolicyDO::getEffectiveFrom, p.getEffectiveTo())
                .ge(HrmPayrollInsurancePolicyDO::getEffectiveTo, p.getEffectiveFrom());
    if (replaced != null) overlap.ne(HrmPayrollInsurancePolicyDO::getId, replaced.getId());
    if (policies.selectCount(overlap) > 0) throw exception(PAYROLL_INSURANCE_INVALID);
    if (replaced != null) {
      event(
          replaced.getEntityId(),
          replaced.getId(),
          null,
          "SUPERSEDE_POLICY",
          evidence,
          replaced,
          actor);
      replaced.setStatus("SUPERSEDED");
      policies.updateById(replaced);
    }
    p.setStatus("APPROVED");
    p.setApprovedBy(actor);
    policies.updateById(p);
    event(p.getEntityId(), id, null, "APPROVE_POLICY", evidence, p, actor);
  }

  @Transactional(rollbackFor = Exception.class)
  public void rejectPolicy(Long id, String evidence, Long actor) {
    reason(evidence);
    HrmPayrollInsurancePolicyDO p = policies.selectById(id);
    if (p == null) throw exception(PAYROLL_INSURANCE_INVALID);
    entities.check(p.getEntityId(), "APPROVE", actor);
    entities.lockApproved(p.getEntityId());
    p = policies.selectOneForUpdate(HrmPayrollInsurancePolicyDO::getId, id);
    if (!"PENDING".equals(p.getStatus())) throw exception(PAYROLL_INSURANCE_INVALID);
    p.setStatus("REJECTED");
    policies.updateById(p);
    event(p.getEntityId(), id, null, "REJECT_POLICY", evidence, p, actor);
  }

  private HrmPayrollInsurancePolicyDO policyAt(Long reference, LocalDate day) {
    HrmPayrollInsurancePolicyDO source = policies.selectById(reference);
    if (source == null || !Arrays.asList("APPROVED", "SUPERSEDED").contains(source.getStatus()))
      throw exception(PAYROLL_INSURANCE_INVALID);
    HrmPayrollInsurancePolicyDO p =
        policies.selectOne(
            new LambdaQueryWrapperX<HrmPayrollInsurancePolicyDO>()
                .eq(HrmPayrollInsurancePolicyDO::getEntityId, source.getEntityId())
                .eq(HrmPayrollInsurancePolicyDO::getCode, source.getCode())
                .eq(HrmPayrollInsurancePolicyDO::getStatus, "APPROVED")
                .le(HrmPayrollInsurancePolicyDO::getEffectiveFrom, day)
                .ge(HrmPayrollInsurancePolicyDO::getEffectiveTo, day));
    if (p == null) throw exception(PAYROLL_INSURANCE_INVALID);
    return p;
  }

  private void validateBase(HrmPayrollInsuranceReqVO.Base r) {
    if (!validator.validate(r).isEmpty()) throw exception(PAYROLL_INSURANCE_INVALID);
    period(r.getEffectiveFrom(), r.getEffectiveTo());
    if ("EXEMPT".equals(r.getCoverage())) {
      if (r.getPolicyId() != null
          || (r.getDeclaredBases() != null && !r.getDeclaredBases().isEmpty()))
        throw exception(PAYROLL_INSURANCE_INVALID);
      return;
    }
    if (r.getIncomeYear() == null
        || r.getIncomeYear() > r.getEffectiveFrom().getYear()
        || r.getPolicyId() == null
        || r.getDeclaredBases() == null) throw exception(PAYROLL_INSURANCE_INVALID);
    HrmPayrollInsurancePolicyDO p = policyAt(r.getPolicyId(), r.getEffectiveFrom());
    if (!Objects.equals(p.getEntityId(), r.getEntityId()))
      throw exception(PAYROLL_INSURANCE_INVALID);
    Set<String> codes = new HashSet<>();
    for (Project project : projects(p)) {
      codes.add(project.getCode());
      BigDecimal amount = r.getDeclaredBases().get(project.getCode());
      if (amount == null
          || amount.signum() < 0
          || amount.scale() > 2
          || amount.precision() - amount.scale() > 10) throw exception(PAYROLL_INSURANCE_INVALID);
    }
    if (!codes.equals(r.getDeclaredBases().keySet())) throw exception(PAYROLL_INSURANCE_INVALID);
  }

  public Long resolvePolicy(Long entityId, String code, LocalDate day) {
    HrmPayrollInsurancePolicyDO policy =
        policies.selectOne(
            new LambdaQueryWrapperX<HrmPayrollInsurancePolicyDO>()
                .eq(HrmPayrollInsurancePolicyDO::getEntityId, entityId)
                .eq(HrmPayrollInsurancePolicyDO::getCode, code.toUpperCase(Locale.ROOT))
                .eq(HrmPayrollInsurancePolicyDO::getStatus, "APPROVED")
                .le(HrmPayrollInsurancePolicyDO::getEffectiveFrom, day)
                .ge(HrmPayrollInsurancePolicyDO::getEffectiveTo, day));
    if (policy == null) throw exception(PAYROLL_INSURANCE_INVALID);
    return policy.getId();
  }

  public void inspectBase(HrmPayrollInsuranceReqVO.Base request, Long actor) {
    validateBase(request);
    entities.check(request.getEntityId(), "MANAGE", actor);
    entities.approved(request.getEntityId());
    if (employees.selectById(request.getEmployeeId()) == null) throw exception(EMPLOYEE_NOT_EXISTS);
    if (bases.selectCount(
            new LambdaQueryWrapperX<HrmPayrollInsuranceBaseDO>()
                .eq(HrmPayrollInsuranceBaseDO::getEmployeeId, request.getEmployeeId())
                .in(HrmPayrollInsuranceBaseDO::getStatus, "HR_REVIEW", "FINANCE_REVIEW"))
        > 0) throw exception(PAYROLL_INSURANCE_INVALID);
    HrmPayrollInsuranceBaseDO replaced =
        request.getReplacesId() == null ? null : bases.selectById(request.getReplacesId());
    if (request.getReplacesId() != null
        && (replaced == null
            || !"APPROVED".equals(replaced.getStatus())
            || !Objects.equals(replaced.getEmployeeId(), request.getEmployeeId())
            || !Objects.equals(replaced.getEntityId(), request.getEntityId())
            || !Objects.equals(replaced.getEffectiveFrom(), request.getEffectiveFrom())
            || request.getEffectiveTo().isAfter(replaced.getEffectiveTo())))
      throw exception(PAYROLL_INSURANCE_INVALID);
    com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<HrmPayrollInsuranceBaseDO>
        overlaps =
            new LambdaQueryWrapperX<HrmPayrollInsuranceBaseDO>()
                .eq(HrmPayrollInsuranceBaseDO::getEmployeeId, request.getEmployeeId())
                .eq(HrmPayrollInsuranceBaseDO::getStatus, "APPROVED")
                .le(HrmPayrollInsuranceBaseDO::getEffectiveFrom, request.getEffectiveTo())
                .ge(HrmPayrollInsuranceBaseDO::getEffectiveTo, request.getEffectiveFrom());
    if (replaced != null) overlaps.ne(HrmPayrollInsuranceBaseDO::getId, replaced.getId());
    if (bases.selectCount(overlaps) > 0) throw exception(PAYROLL_INSURANCE_INVALID);
  }

  @Transactional(rollbackFor = Exception.class)
  public Long proposeBase(HrmPayrollInsuranceReqVO.Base r, Long actor) {
    validateBase(r);
    entities.check(r.getEntityId(), "MANAGE", actor);
    entities.approved(r.getEntityId());
    if (employees.selectOneForUpdate(HrmEmployeeDO::getId, r.getEmployeeId()) == null)
      throw exception(EMPLOYEE_NOT_EXISTS);
    if (bases.selectCount(
            new LambdaQueryWrapperX<HrmPayrollInsuranceBaseDO>()
                .eq(HrmPayrollInsuranceBaseDO::getEmployeeId, r.getEmployeeId())
                .in(HrmPayrollInsuranceBaseDO::getStatus, "HR_REVIEW", "FINANCE_REVIEW"))
        > 0) throw exception(PAYROLL_INSURANCE_INVALID);
    HrmPayrollInsuranceBaseDO b = BeanUtils.toBean(r, HrmPayrollInsuranceBaseDO.class);
    b.setDeclaredBases(
        JsonUtils.toJsonString(
            r.getDeclaredBases() == null ? Collections.emptyMap() : r.getDeclaredBases()));
    b.setStatus("HR_REVIEW");
    b.setCreator(String.valueOf(actor));
    bases.insert(b);
    event(b.getEntityId(), null, b.getId(), "SUBMIT_BASE", r.getEvidence(), b, actor);
    return b.getId();
  }

  @Transactional(rollbackFor = Exception.class)
  public void reviewBase(Long id, String evidence, Long actor) {
    reason(evidence);
    HrmPayrollInsuranceBaseDO b = bases.selectById(id);
    if (b == null) throw exception(PAYROLL_INSURANCE_INVALID);
    entities.check(b.getEntityId(), "REVIEW", actor);
    if (employees.selectOneForUpdate(HrmEmployeeDO::getId, b.getEmployeeId()) == null)
      throw exception(EMPLOYEE_NOT_EXISTS);
    b = bases.selectOneForUpdate(HrmPayrollInsuranceBaseDO::getId, id);
    if (!"HR_REVIEW".equals(b.getStatus())) throw exception(PAYROLL_INSURANCE_INVALID);
    if (Objects.equals(b.getCreator(), String.valueOf(actor)))
      throw exception(PAYROLL_SELF_APPROVAL);
    b.setStatus("FINANCE_REVIEW");
    b.setReviewedBy(actor);
    bases.updateById(b);
    event(b.getEntityId(), null, id, "REVIEW_BASE", evidence, b, actor);
  }

  @Transactional(rollbackFor = Exception.class)
  public void approveBase(Long id, String evidence, Long actor) {
    reason(evidence);
    HrmPayrollInsuranceBaseDO b = bases.selectById(id);
    if (b == null) throw exception(PAYROLL_INSURANCE_INVALID);
    entities.check(b.getEntityId(), "APPROVE", actor);
    entities.approved(b.getEntityId());
    if (employees.selectOneForUpdate(HrmEmployeeDO::getId, b.getEmployeeId()) == null)
      throw exception(EMPLOYEE_NOT_EXISTS);
    b = bases.selectOneForUpdate(HrmPayrollInsuranceBaseDO::getId, id);
    if (!"FINANCE_REVIEW".equals(b.getStatus())) throw exception(PAYROLL_INSURANCE_INVALID);
    if (Objects.equals(b.getCreator(), String.valueOf(actor))
        || Objects.equals(b.getReviewedBy(), actor)) throw exception(PAYROLL_SELF_APPROVAL);
    HrmPayrollInsuranceReqVO.Base request =
        new HrmPayrollInsuranceReqVO.Base()
            .setEntityId(b.getEntityId())
            .setEmployeeId(b.getEmployeeId())
            .setPolicyId(b.getPolicyId())
            .setCoverage(b.getCoverage())
            .setIncomeYear(b.getIncomeYear())
            .setEffectiveFrom(b.getEffectiveFrom())
            .setEffectiveTo(b.getEffectiveTo())
            .setReplacesId(b.getReplacesId())
            .setEvidence(b.getEvidence());
    request.setDeclaredBases(
        JsonUtils.parseObject(
            b.getDeclaredBases(),
            new com.fasterxml.jackson.core.type.TypeReference<Map<String, BigDecimal>>() {}));
    validateBase(request);
    HrmPayrollInsuranceBaseDO replaced = null;
    if (b.getReplacesId() != null) {
      replaced = bases.selectOneForUpdate(HrmPayrollInsuranceBaseDO::getId, b.getReplacesId());
      if (replaced == null
          || !"APPROVED".equals(replaced.getStatus())
          || !Objects.equals(replaced.getEmployeeId(), b.getEmployeeId())
          || !Objects.equals(replaced.getEntityId(), b.getEntityId())
          || !Objects.equals(replaced.getEffectiveFrom(), b.getEffectiveFrom())
          || b.getEffectiveTo().isAfter(replaced.getEffectiveTo()))
        throw exception(PAYROLL_INSURANCE_INVALID);
    }
    com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<HrmPayrollInsuranceBaseDO>
        overlap =
            new LambdaQueryWrapperX<HrmPayrollInsuranceBaseDO>()
                .eq(HrmPayrollInsuranceBaseDO::getEmployeeId, b.getEmployeeId())
                .eq(HrmPayrollInsuranceBaseDO::getStatus, "APPROVED")
                .le(HrmPayrollInsuranceBaseDO::getEffectiveFrom, b.getEffectiveTo())
                .ge(HrmPayrollInsuranceBaseDO::getEffectiveTo, b.getEffectiveFrom());
    if (replaced != null) overlap.ne(HrmPayrollInsuranceBaseDO::getId, replaced.getId());
    if (bases.selectCount(overlap) > 0) throw exception(PAYROLL_INSURANCE_INVALID);
    if (replaced != null) {
      event(
          replaced.getEntityId(),
          null,
          replaced.getId(),
          "SUPERSEDE_BASE",
          evidence,
          replaced,
          actor);
      replaced.setStatus("SUPERSEDED");
      bases.updateById(replaced);
    }
    b.setStatus("APPROVED");
    b.setApprovedBy(actor);
    bases.updateById(b);
    event(b.getEntityId(), null, id, "APPROVE_BASE", evidence, b, actor);
  }

  @Transactional(rollbackFor = Exception.class)
  public void rejectBase(Long id, String evidence, Long actor) {
    reason(evidence);
    HrmPayrollInsuranceBaseDO b = bases.selectById(id);
    if (b == null) throw exception(PAYROLL_INSURANCE_INVALID);
    entities.check(
        b.getEntityId(), "HR_REVIEW".equals(b.getStatus()) ? "REVIEW" : "APPROVE", actor);
    if (employees.selectOneForUpdate(HrmEmployeeDO::getId, b.getEmployeeId()) == null)
      throw exception(EMPLOYEE_NOT_EXISTS);
    b = bases.selectOneForUpdate(HrmPayrollInsuranceBaseDO::getId, id);
    if (!Arrays.asList("HR_REVIEW", "FINANCE_REVIEW").contains(b.getStatus()))
      throw exception(PAYROLL_INSURANCE_INVALID);
    b.setStatus("REJECTED");
    bases.updateById(b);
    event(b.getEntityId(), null, id, "REJECT_BASE", evidence, b, actor);
  }

  @Data
  public static class ProjectAmount {
    private String code;
    private String name;
    private Integer type;
    private BigDecimal declaredBase;
    private BigDecimal appliedBase;
    private BigDecimal personalRate;
    private BigDecimal corporateRate;
    private BigDecimal minimumBase;
    private BigDecimal maximumBase;
    private BigDecimal personalFixed;
    private BigDecimal corporateFixed;
    private BigDecimal personalAmount;
    private BigDecimal corporateAmount;
  }

  @Data
  public static class Quote {
    private Long entityId;
    private Long employeeId;
    private Long payerEntityId;
    private Long baseId;
    private Long policyId;
    private Integer policyVersion;
    private Integer year;
    private Integer month;
    private String coverage;
    private String baseEvidence;
    private String policyEvidence;
    private Integer incomeYear;
    private BigDecimal personalInsurance = BigDecimal.ZERO;
    private BigDecimal personalFund = BigDecimal.ZERO;
    private BigDecimal corporateInsurance = BigDecimal.ZERO;
    private BigDecimal corporateFund = BigDecimal.ZERO;
    private List<ProjectAmount> projects = new ArrayList<>();
  }

  private Quote quote(Long entityId, Long employeeId, YearMonth month) {
    HrmPayrollInsuranceBaseDO base =
        bases.selectOne(
            new LambdaQueryWrapperX<HrmPayrollInsuranceBaseDO>()
                .eq(HrmPayrollInsuranceBaseDO::getEmployeeId, employeeId)
                .eq(HrmPayrollInsuranceBaseDO::getStatus, "APPROVED")
                .le(HrmPayrollInsuranceBaseDO::getEffectiveFrom, month.atDay(1))
                .ge(HrmPayrollInsuranceBaseDO::getEffectiveTo, month.atEndOfMonth()));
    if (base == null) throw exception(PAYROLL_INSURANCE_INVALID);
    Quote q = new Quote();
    q.setEntityId(entityId);
    q.setEmployeeId(employeeId);
    q.setPayerEntityId(base.getEntityId());
    q.setBaseId(base.getId());
    q.setYear(month.getYear());
    q.setMonth(month.getMonthValue());
    q.setCoverage(base.getCoverage());
    if (!Objects.equals(base.getEntityId(), entityId)) {
      q.setCoverage("EXTERNAL");
      return q;
    }
    q.setBaseEvidence(base.getEvidence());
    q.setIncomeYear(base.getIncomeYear());
    if ("EXEMPT".equals(base.getCoverage())) return q;
    HrmPayrollInsurancePolicyDO p = policyAt(base.getPolicyId(), month.atDay(1));
    q.setPolicyId(p.getId());
    q.setPolicyVersion(p.getVersion());
    q.setPolicyEvidence(p.getEvidence());
    Map<String, BigDecimal> declared =
        JsonUtils.parseObject(
            base.getDeclaredBases(),
            new com.fasterxml.jackson.core.type.TypeReference<Map<String, BigDecimal>>() {});
    for (Project project : projects(p)) {
      BigDecimal input = declared.get(project.getCode());
      if (input == null) throw exception(PAYROLL_INSURANCE_INVALID);
      BigDecimal applied = input.max(project.getMinimumBase()).min(project.getMaximumBase());
      ProjectAmount a = new ProjectAmount();
      a.setCode(project.getCode());
      a.setName(project.getName());
      a.setType(project.getType());
      a.setDeclaredBase(input);
      a.setAppliedBase(applied);
      a.setMinimumBase(project.getMinimumBase());
      a.setMaximumBase(project.getMaximumBase());
      a.setPersonalFixed(n(project.getPersonalFixed()));
      a.setCorporateFixed(n(project.getCorporateFixed()));
      a.setPersonalRate(project.getPersonalRate());
      a.setCorporateRate(project.getCorporateRate());
      a.setPersonalAmount(
          applied
              .multiply(project.getPersonalRate())
              .divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP)
              .add(n(project.getPersonalFixed()))
              .setScale(2, RoundingMode.HALF_UP));
      a.setCorporateAmount(
          applied
              .multiply(project.getCorporateRate())
              .divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP)
              .add(n(project.getCorporateFixed()))
              .setScale(2, RoundingMode.HALF_UP));
      q.getProjects().add(a);
      if (HrmInsuranceProjectTypeEnum.isProvidentFund(project.getType())) {
        q.setPersonalFund(q.getPersonalFund().add(a.getPersonalAmount()));
        q.setCorporateFund(q.getCorporateFund().add(a.getCorporateAmount()));
      } else {
        q.setPersonalInsurance(q.getPersonalInsurance().add(a.getPersonalAmount()));
        q.setCorporateInsurance(q.getCorporateInsurance().add(a.getCorporateAmount()));
      }
    }
    return q;
  }

  public Map<Long, Quote> preview(
      Long entityId, Collection<Long> employeeIds, YearMonth month, Long actor) {
    entities.check(entityId, "READ", actor);
    entities.approved(entityId);
    Map<Long, Quote> result = new LinkedHashMap<>();
    for (Long id : new TreeSet<>(employeeIds)) result.put(id, quote(entityId, id, month));
    return result;
  }

  public Map<Long, Quote> forPayroll(
      Long entityId, Collection<Long> employeeIds, YearMonth month, Long actor) {
    entities.check(entityId, "CALCULATE", actor);
    entities.approved(entityId);
    Map<Long, Quote> result = new LinkedHashMap<>();
    for (Long id : new TreeSet<>(employeeIds)) result.put(id, quote(entityId, id, month));
    return result;
  }

  public Map<Long, Quote> previewForEntity(Long entityId, YearMonth month, Long actor) {
    entities.check(entityId, "READ", actor);
    List<Long> employeeIds =
        bases
            .selectList(
                new LambdaQueryWrapperX<HrmPayrollInsuranceBaseDO>()
                    .eq(HrmPayrollInsuranceBaseDO::getEntityId, entityId)
                    .eq(HrmPayrollInsuranceBaseDO::getStatus, "APPROVED")
                    .le(HrmPayrollInsuranceBaseDO::getEffectiveFrom, month.atDay(1))
                    .ge(HrmPayrollInsuranceBaseDO::getEffectiveTo, month.atEndOfMonth()))
            .stream()
            .map(HrmPayrollInsuranceBaseDO::getEmployeeId)
            .collect(java.util.stream.Collectors.toList());
    return preview(entityId, employeeIds, month, actor);
  }

  public Map<Long, HrmInsuranceMonthEmployeeRecordDO> payrollRecords(Map<Long, Quote> quotes) {
    Map<Long, HrmInsuranceMonthEmployeeRecordDO> records = new LinkedHashMap<>();
    for (Quote q : quotes.values())
      records.put(
          q.getEmployeeId(),
          new HrmInsuranceMonthEmployeeRecordDO()
              .setEmployeeId(q.getEmployeeId())
              .setYear(q.getYear())
              .setMonth(q.getMonth())
              .setStatus(1)
              .setPersonalInsuranceAmount(q.getPersonalInsurance())
              .setPersonalProvidentFundAmount(q.getPersonalFund())
              .setCorporateInsuranceAmount(q.getCorporateInsurance())
              .setCorporateProvidentFundAmount(q.getCorporateFund()));
    return records;
  }

  public void validateInputSnapshot(String json) {
    com.fasterxml.jackson.databind.JsonNode input = JsonUtils.parseTree(json),
        source = input.get("insurancePolicySources");
    if (source == null) return;
    Map<Long, Quote> before =
        JsonUtils.parseObject(
            source.toString(),
            new com.fasterxml.jackson.core.type.TypeReference<Map<Long, Quote>>() {});
    Map<Long, Quote> now = new LinkedHashMap<>();
    for (Quote q : before.values())
      now.put(
          q.getEmployeeId(),
          quote(q.getEntityId(), q.getEmployeeId(), YearMonth.of(q.getYear(), q.getMonth())));
    if (!source.equals(JsonUtils.parseTree(JsonUtils.toJsonString(now))))
      throw exception(PAYROLL_INSURANCE_INVALID);
  }
}
