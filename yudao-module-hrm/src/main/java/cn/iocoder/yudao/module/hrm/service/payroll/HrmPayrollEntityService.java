package cn.iocoder.yudao.module.hrm.service.payroll;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollEntityReqVO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.employee.info.HrmEmployeeMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.*;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import java.time.*;
import java.util.*;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HrmPayrollEntityService {
  public static final Set<String> ACTIONS =
      Collections.unmodifiableSet(
          new HashSet<>(
              Arrays.asList("READ", "CALCULATE", "REVIEW", "APPROVE", "EXPORT", "MANAGE")));
  @Resource private HrmPayrollLegalEntityMapper entities;
  @Resource private HrmPayrollEntityGrantMapper grants;
  @Resource private HrmPayrollEntityMembershipMapper memberships;
  @Resource private HrmPayrollEntityEventMapper events;
  @Resource private HrmEmployeeMapper employees;
  @Resource private AdminUserApi users;
  @Resource private PermissionApi permissions;
  @Resource private DeptApi departments;
  @Resource private javax.validation.Validator validator;

  private LocalDate today() {
    return LocalDate.now(ZoneId.of("Asia/Shanghai"));
  }

  public boolean superAdmin(Long actor) {
    return actor != null && permissions.hasAnyRoles(actor, "super_admin");
  }

  public boolean allowed(Long entityId, String action, Long actor) {
    if (actor == null
        || entityId == null
        || !ACTIONS.contains(action)
        || entities.selectById(entityId) == null) return false;
    if (superAdmin(actor)) return true;
    return grants
        .selectList(
            new LambdaQueryWrapperX<HrmPayrollEntityGrantDO>()
                .eq(HrmPayrollEntityGrantDO::getEntityId, entityId)
                .eq(HrmPayrollEntityGrantDO::getUserId, actor)
                .le(HrmPayrollEntityGrantDO::getEffectiveFrom, today())
                .ge(HrmPayrollEntityGrantDO::getEffectiveTo, today()))
        .stream()
        .anyMatch(g -> JsonUtils.parseArray(g.getActions(), String.class).contains(action));
  }

  public void check(Long entityId, String action, Long actor) {
    if (!allowed(entityId, action, actor)) throw exception(PAYROLL_ENTITY_INVALID);
  }

  public HrmPayrollLegalEntityDO approved(Long entityId) {
    HrmPayrollLegalEntityDO entity = entities.selectById(entityId);
    if (entity == null || !"APPROVED".equals(entity.getStatus()))
      throw exception(PAYROLL_ENTITY_INVALID);
    return entity;
  }

  public String name(Long id) { HrmPayrollLegalEntityDO entity=entities.selectById(id); return entity==null?null:entity.getName(); }

  public HrmPayrollLegalEntityDO lockApproved(Long id) {
    HrmPayrollLegalEntityDO entity = entities.selectOneForUpdate(HrmPayrollLegalEntityDO::getId, id);
    if (entity == null || !"APPROVED".equals(entity.getStatus())) throw exception(PAYROLL_ENTITY_INVALID);
    return entity;
  }

  public List<HrmPayrollLegalEntityDO> list(Long actor) {
    return entities.selectList().stream()
        .filter(e -> allowed(e.getId(), "READ", actor) || allowed(e.getId(), "MANAGE", actor))
        .peek(
            e ->
                e.setAllowedActions(
                    ACTIONS.stream()
                        .filter(a -> allowed(e.getId(), a, actor))
                        .collect(java.util.stream.Collectors.toSet())))
        .collect(java.util.stream.Collectors.toList());
  }

  public List<Long> accessible(String action, Long actor) {
    return entities.selectList().stream()
        .filter(e -> allowed(e.getId(), action, actor))
        .map(HrmPayrollLegalEntityDO::getId)
        .collect(java.util.stream.Collectors.toList());
  }

  private void event(Long entityId, String action, String reason, Object snapshot, Long actor) {
    HrmPayrollEntityEventDO event = new HrmPayrollEntityEventDO();
    event.setEntityId(entityId);
    event.setAction(action);
    event.setReason(reason);
    event.setSnapshot(JsonUtils.toJsonString(snapshot));
    event.setActorId(actor);
    events.insert(event);
  }

  public Map<String, Object> options(Long entityId, Long actor) {
    check(entityId, "MANAGE", actor);
    Map<String, Object> result = new LinkedHashMap<>();
    result.put(
        "employees",
        employees.selectList().stream()
            .map(
                e -> {
                  Map<String, Object> item = new LinkedHashMap<>();
                  item.put("id", e.getId());
                  item.put("name", e.getName());
                  item.put("jobNumber", e.getJobNumber());
                  return item;
                })
            .collect(java.util.stream.Collectors.toList()));
    result.put(
        "users",
        users.getUserListByStatus(0).stream()
            .map(
                u -> {
                  Map<String, Object> item = new LinkedHashMap<>();
                  item.put("id", u.getId());
                  item.put("name", u.getNickname());
                  return item;
                })
            .collect(java.util.stream.Collectors.toList()));
    return result;
  }

  @Transactional(rollbackFor = Exception.class)
  public Long create(HrmPayrollEntityReqVO request, Long actor) {
    if (actor == null || !validator.validate(request).isEmpty())
      throw exception(PAYROLL_ENTITY_INVALID);
    users.validateUser(actor);
    request.setCode(request.getCode().toUpperCase(Locale.ROOT));
    if (entities.selectCount(
            new LambdaQueryWrapperX<HrmPayrollLegalEntityDO>()
                .eq(HrmPayrollLegalEntityDO::getCode, request.getCode()))
        > 0) throw exception(PAYROLL_ENTITY_INVALID);
    HrmPayrollLegalEntityDO entity = BeanUtils.toBean(request, HrmPayrollLegalEntityDO.class);
    entity.setStatus("PENDING");
    entity.setCreator(String.valueOf(actor));
    entity.setSubmittedBy(actor);
    entities.insert(entity);
    HrmPayrollEntityGrantDO grant = new HrmPayrollEntityGrantDO();
    grant.setEntityId(entity.getId());
    grant.setUserId(actor);
    grant.setActions(JsonUtils.toJsonString(new TreeSet<>(ACTIONS)));
    grant.setEffectiveFrom(today());
    grant.setEffectiveTo(today().plusYears(50));
    grant.setReason("主体创建人初始管理授权");
    grants.insert(grant);
    event(entity.getId(), "CREATE", request.getEvidence(), entity, actor);
    return entity.getId();
  }

  @Transactional(rollbackFor = Exception.class)
  public void approve(Long id, String reason, Long actor) {
    check(id, "APPROVE", actor);
    HrmPayrollLegalEntityDO entity =
        entities.selectOneForUpdate(HrmPayrollLegalEntityDO::getId, id);
    if (!"PENDING".equals(entity.getStatus())
        || reason == null
        || (reason.trim().isEmpty() || reason.length() > 1000))
      throw exception(PAYROLL_ENTITY_INVALID);
    if (Objects.equals(entity.getSubmittedBy(), actor)) throw exception(PAYROLL_SELF_APPROVAL);
    entity.setStatus("APPROVED");
    entity.setApprovedBy(actor);
    entities.updateById(entity);
    event(id, "APPROVE", reason, entity, actor);
  }

  @Transactional(rollbackFor = Exception.class)
  public Long grant(HrmPayrollEntityReqVO.Grant request, Long actor) {
    if (!validator.validate(request).isEmpty()
        || request.getEffectiveTo().isBefore(request.getEffectiveFrom())
        || !ACTIONS.containsAll(request.getActions())) throw exception(PAYROLL_ENTITY_INVALID);
    check(request.getEntityId(), "MANAGE", actor);
    entities.selectOneForUpdate(HrmPayrollLegalEntityDO::getId, request.getEntityId());
    users.validateUser(request.getUserId());
    HrmPayrollEntityGrantDO grant = BeanUtils.toBean(request, HrmPayrollEntityGrantDO.class);
    grant.setActions(JsonUtils.toJsonString(new TreeSet<>(request.getActions())));
    grants.insert(grant);
    event(request.getEntityId(), "GRANT", request.getReason(), grant, actor);
    return grant.getId();
  }

  @Transactional(rollbackFor = Exception.class)
  public void revoke(Long id, String reason, Long actor) {
    HrmPayrollEntityGrantDO grant = grants.selectById(id);
    if (grant == null || reason == null || (reason.trim().isEmpty() || reason.length() > 1000))
      throw exception(PAYROLL_ENTITY_INVALID);
    check(grant.getEntityId(), "MANAGE", actor);
    entities.selectOneForUpdate(HrmPayrollLegalEntityDO::getId, grant.getEntityId());
    grants.deleteById(id);
    event(grant.getEntityId(), "REVOKE", reason, grant, actor);
  }

  public List<HrmPayrollEntityGrantDO> grants(Long id, Long actor) {
    check(id, "MANAGE", actor);
    return grants.selectList(HrmPayrollEntityGrantDO::getEntityId, id);
  }

  public List<HrmPayrollEntityEventDO> history(Long id, Long actor) {
    check(id, "READ", actor);
    return events.selectList(
        new LambdaQueryWrapperX<HrmPayrollEntityEventDO>()
            .eq(HrmPayrollEntityEventDO::getEntityId, id)
            .orderByAsc(HrmPayrollEntityEventDO::getId));
  }

  public List<HrmPayrollEntityMembershipDO> memberships(Long id, Long actor) {
    check(id, "READ", actor);
    return memberships.selectList(
        new LambdaQueryWrapperX<HrmPayrollEntityMembershipDO>()
            .eq(HrmPayrollEntityMembershipDO::getEntityId, id)
            .orderByAsc(HrmPayrollEntityMembershipDO::getEmployeeId)
            .orderByAsc(HrmPayrollEntityMembershipDO::getEffectiveFrom));
  }

  @Transactional(rollbackFor = Exception.class)
  public Long assign(HrmPayrollEntityReqVO.Membership request, Long actor) {
    if (!validator.validate(request).isEmpty()
        || request.getEffectiveTo().isBefore(request.getEffectiveFrom()))
      throw exception(PAYROLL_ENTITY_INVALID);
    check(request.getEntityId(), "MANAGE", actor);
    approved(request.getEntityId());
    if (employees.selectOneForUpdate(HrmEmployeeDO::getId, request.getEmployeeId()) == null)
      throw exception(EMPLOYEE_NOT_EXISTS);
    departments.validateDeptList(Collections.singleton(request.getDeptId()));
    HrmPayrollEntityMembershipDO membership =
        BeanUtils.toBean(request, HrmPayrollEntityMembershipDO.class);
    membership.setStatus("PENDING");
    membership.setCreator(String.valueOf(actor));
    memberships.insert(membership);
    event(request.getEntityId(), "ASSIGN", request.getEvidence(), membership, actor);
    return membership.getId();
  }

  @Transactional(rollbackFor = Exception.class)
  public void approveMembership(Long id, String reason, Long actor) {
    HrmPayrollEntityMembershipDO membership = memberships.selectById(id);
    if (membership == null || reason == null || (reason.trim().isEmpty() || reason.length() > 1000))
      throw exception(PAYROLL_ENTITY_INVALID);
    check(membership.getEntityId(), "APPROVE", actor);
    approved(membership.getEntityId());
    if (employees.selectOneForUpdate(HrmEmployeeDO::getId, membership.getEmployeeId()) == null)
      throw exception(EMPLOYEE_NOT_EXISTS);
    membership = memberships.selectOneForUpdate(HrmPayrollEntityMembershipDO::getId, id);
    if (!"PENDING".equals(membership.getStatus())) throw exception(PAYROLL_ENTITY_INVALID);
    if (Objects.equals(membership.getCreator(), String.valueOf(actor)))
      throw exception(PAYROLL_SELF_APPROVAL);
    HrmPayrollEntityMembershipDO replaced = null;
    if (membership.getReplacesId() != null) {
      replaced =
          memberships.selectOneForUpdate(
              HrmPayrollEntityMembershipDO::getId, membership.getReplacesId());
      if (replaced == null
          || !"APPROVED".equals(replaced.getStatus())
          || !Objects.equals(replaced.getEmployeeId(), membership.getEmployeeId())
          || !Objects.equals(replaced.getEntityId(), membership.getEntityId())
          || !Objects.equals(replaced.getEffectiveFrom(), membership.getEffectiveFrom())
          || membership.getEffectiveTo().isAfter(replaced.getEffectiveTo()))
        throw exception(PAYROLL_ENTITY_INVALID);
    }
    com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<HrmPayrollEntityMembershipDO>
        overlap =
            new LambdaQueryWrapperX<HrmPayrollEntityMembershipDO>()
                .eq(HrmPayrollEntityMembershipDO::getEmployeeId, membership.getEmployeeId())
                .eq(HrmPayrollEntityMembershipDO::getStatus, "APPROVED")
                .le(HrmPayrollEntityMembershipDO::getEffectiveFrom, membership.getEffectiveTo())
                .ge(HrmPayrollEntityMembershipDO::getEffectiveTo, membership.getEffectiveFrom());
    if (replaced != null) overlap.ne(HrmPayrollEntityMembershipDO::getId, replaced.getId());
    if (memberships.selectCount(overlap) > 0) throw exception(PAYROLL_ENTITY_INVALID);
    if (replaced != null) {
      event(replaced.getEntityId(), "SUPERSEDE_ASSIGNMENT", reason, replaced, actor);
      replaced.setStatus("SUPERSEDED");
      memberships.updateById(replaced);
    }
    membership.setStatus("APPROVED");
    membership.setApprovedBy(actor);
    memberships.updateById(membership);
    event(membership.getEntityId(), "APPROVE_ASSIGNMENT", reason, membership, actor);
  }

  public boolean hasOtherMembership(Long employeeId,Long entityId,LocalDate from,LocalDate to) {
    return memberships.selectCount(new LambdaQueryWrapperX<HrmPayrollEntityMembershipDO>()
        .eq(HrmPayrollEntityMembershipDO::getEmployeeId,employeeId).eq(HrmPayrollEntityMembershipDO::getStatus,"APPROVED")
        .ne(HrmPayrollEntityMembershipDO::getEntityId,entityId).le(HrmPayrollEntityMembershipDO::getEffectiveFrom,to)
        .ge(HrmPayrollEntityMembershipDO::getEffectiveTo,from))>0;
  }

  public List<HrmPayrollEntityMembershipDO> effectiveMemberships(
      Long entityId, LocalDate from, LocalDate to) {
    approved(entityId);
    return memberships.selectList(
        new LambdaQueryWrapperX<HrmPayrollEntityMembershipDO>()
            .eq(HrmPayrollEntityMembershipDO::getEntityId, entityId)
            .eq(HrmPayrollEntityMembershipDO::getStatus, "APPROVED")
            .le(HrmPayrollEntityMembershipDO::getEffectiveFrom, to)
            .ge(HrmPayrollEntityMembershipDO::getEffectiveTo, from)
            .orderByAsc(HrmPayrollEntityMembershipDO::getEmployeeId)
            .orderByAsc(HrmPayrollEntityMembershipDO::getEffectiveFrom));
  }

  @Transactional(rollbackFor = Exception.class)
  public void revise(Long id, HrmPayrollEntityReqVO request, Long actor) {
    check(id, "MANAGE", actor);
    if (request.getCode() != null) request.setCode(request.getCode().toUpperCase(Locale.ROOT));
    if (!validator.validate(request).isEmpty()) throw exception(PAYROLL_ENTITY_INVALID);
    HrmPayrollLegalEntityDO original =
        entities.selectOneForUpdate(HrmPayrollLegalEntityDO::getId, id);
    if (!"PENDING".equals(original.getStatus())
        || !Objects.equals(original.getCode(), request.getCode()))
      throw exception(PAYROLL_ENTITY_INVALID);
    event(id, "BEFORE_REVISE", request.getEvidence(), original, actor);
    HrmPayrollLegalEntityDO revised = BeanUtils.toBean(request, HrmPayrollLegalEntityDO.class);
    revised.setId(id);
    revised.setSubmittedBy(actor);
    entities.updateById(revised);
    event(id, "REVISE", request.getEvidence(), entities.selectById(id), actor);
  }

  @Transactional(rollbackFor = Exception.class)
  public void rejectMembership(Long id, String reason, Long actor) {
    HrmPayrollEntityMembershipDO member = memberships.selectById(id);
    if (member == null || reason == null || (reason.trim().isEmpty() || reason.length() > 1000))
      throw exception(PAYROLL_ENTITY_INVALID);
    check(member.getEntityId(), "APPROVE", actor);
    employees.selectOneForUpdate(HrmEmployeeDO::getId, member.getEmployeeId());
    member = memberships.selectOneForUpdate(HrmPayrollEntityMembershipDO::getId, id);
    if (!"PENDING".equals(member.getStatus())) throw exception(PAYROLL_ENTITY_INVALID);
    member.setStatus("REJECTED");
    memberships.updateById(member);
    event(member.getEntityId(), "REJECT_ASSIGNMENT", reason, member, actor);
  }
}
