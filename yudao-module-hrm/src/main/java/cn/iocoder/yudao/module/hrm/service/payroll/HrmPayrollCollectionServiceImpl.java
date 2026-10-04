package cn.iocoder.yudao.module.hrm.service.payroll;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.*;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import com.fasterxml.jackson.databind.JsonNode;
import javax.annotation.Resource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.util.StreamUtils;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;

/** Tenant-owned collection only: confirmation never enables a payroll rule. */
@Service
@Validated
public class HrmPayrollCollectionServiceImpl implements HrmPayrollCollectionService {
    private static final int BUILT_IN_REQUIREMENTS = 46;
    private static final int BUILT_IN_SOURCES = 12;
    @Resource private HrmPayrollRequirementMapper requirementMapper;
    @Resource private HrmPayrollSourceMapper sourceMapper;
    @Resource private HrmPayrollReviewMapper reviewMapper;
    @Resource private HrmPayrollBaselineMapper baselineMapper;
    @Resource private AdminUserApi adminUserApi;

    private Long tenant() { return TenantContextHolder.getRequiredTenantId(); }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Integer> initialize() {
        JsonNode catalog;
        try (java.io.InputStream input = new ClassPathResource("payroll/requirement-catalog.json").getInputStream()) {
            catalog = JsonUtils.getObjectMapper().readTree(StreamUtils.copyToString(input, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load the reviewed collection catalog", e);
        }
        int createdSources = 0, createdRequirements = 0;
        for (JsonNode node : catalog.get("sources")) {
            String code = node.get("code").asText();
            if (sourceMapper.selectTenantByCode(code, tenant()) != null) continue;
            HrmPayrollSourceDO source = JsonUtils.parseObject(node.toString(), HrmPayrollSourceDO.class);
            source.setTenantId(tenant());
            source.setVersion(1).setReadiness(0).setBuiltIn(true);
            try {
                sourceMapper.insert(source);
                audit("source", source.getId(), "initialize", null, 1, null, source, "登记 PRD 候选来源，待核实实际系统");
                createdSources++;
            } catch (DuplicateKeyException ignored) { /* Another initialization won; never overwrite edits. */ }
        }
        for (JsonNode node : catalog.get("requirements")) {
            String code = node.get("code").asText();
            if (requirementMapper.selectTenantByCode(code, tenant()) != null) continue;
            HrmPayrollRequirementDO requirement = JsonUtils.parseObject(node.toString(), HrmPayrollRequirementDO.class);
            requirement.setTenantId(tenant());
            requirement.setVersion(1).setStatus(0).setScopeDecision(0).setBuiltIn(true);
            try {
                requirementMapper.insert(requirement);
                audit("requirement", requirement.getId(), "initialize", null, 1, null, requirement, "登记候选功能，不导入原型确认状态");
                createdRequirements++;
            } catch (DuplicateKeyException ignored) { /* Idempotent under concurrent calls. */ }
        }
        Map<String, Integer> result = new LinkedHashMap<>();
        result.put("createdRequirements", createdRequirements);
        result.put("createdSources", createdSources);
        return result;
    }

    @Override
    public PageResult<HrmPayrollRequirementDO> page(HrmPayrollRequirementPageReqVO query) {
        LambdaQueryWrapperX<HrmPayrollRequirementDO> wrapper = requirementQuery()
                .eqIfPresent(HrmPayrollRequirementDO::getModuleCode, query.getModuleCode())
                .eqIfPresent(HrmPayrollRequirementDO::getStatus, query.getStatus())
                .eqIfPresent(HrmPayrollRequirementDO::getPriority, query.getPriority())
                .eqIfPresent(HrmPayrollRequirementDO::getScopeDecision, query.getScopeDecision());
        if (StrUtil.isNotBlank(query.getSearch())) {
            wrapper.and(w -> w.like(HrmPayrollRequirementDO::getTitle, query.getSearch())
                    .or().like(HrmPayrollRequirementDO::getCode, query.getSearch()));
        }
        if (StrUtil.isNotBlank(query.getSourceCode())) {
            // The list field uses JacksonTypeHandler; force a plain string LIKE parameter.
            // Otherwise MyBatis serializes the pattern as JSON and adds unwanted quotes.
            wrapper.apply("source_codes LIKE {0,jdbcType=VARCHAR,typeHandler=org.apache.ibatis.type.StringTypeHandler}",
                    "%\"" + query.getSourceCode() + "\"%");
        }
        return requirementMapper.selectPage(query, wrapper.orderByAsc(HrmPayrollRequirementDO::getId));
    }

    private LambdaQueryWrapperX<HrmPayrollRequirementDO> requirementQuery() {
        return new LambdaQueryWrapperX<HrmPayrollRequirementDO>().eq(HrmPayrollRequirementDO::getTenantId, tenant());
    }

    @Override
    public HrmPayrollRequirementDO get(Long id) { return require(id, false); }

    private HrmPayrollRequirementDO require(Long id, boolean lock) {
        if (id == null) throw exception(PAYROLL_COLLECTION_NOT_EXISTS);
        HrmPayrollRequirementDO row = requirementMapper.selectTenantById(id, tenant(), lock);
        if (row == null) throw exception(PAYROLL_COLLECTION_NOT_EXISTS);
        return row;
    }

    private HrmPayrollSourceDO requireSource(Long id, boolean lock) {
        if (id == null) throw exception(PAYROLL_COLLECTION_NOT_EXISTS);
        HrmPayrollSourceDO row = sourceMapper.selectTenantById(id, tenant(), lock);
        if (row == null) throw exception(PAYROLL_COLLECTION_NOT_EXISTS);
        return row;
    }

    @Override
    public Map<String, Long> summary() {
        List<HrmPayrollRequirementDO> rows = requirementMapper.selectList(requirementQuery());
        List<HrmPayrollSourceDO> sources = sources();
        Map<String, Long> result = new LinkedHashMap<>();
        result.put("total", (long) rows.size());
        result.put("confirmed", rows.stream().filter(r -> Objects.equals(r.getStatus(), 1)).count());
        result.put("disputed", rows.stream().filter(r -> Objects.equals(r.getStatus(), 2)).count());
        result.put("pending", rows.stream().filter(r -> Objects.equals(r.getStatus(), 0)).count());
        result.put("mvp", rows.stream().filter(r -> Objects.equals(r.getScopeDecision(), 1)).count());
        result.put("sourceTotal", (long) sources.size());
        result.put("sourceReady", sources.stream().filter(s -> Objects.equals(s.getReadiness(), 1)).count());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(HrmPayrollRequirementSaveReqVO request) {
        check(request.getCode().startsWith("CUSTOM-"), "补充需求编号须以 CUSTOM- 开头");
        long count = requirementMapper.selectCount(requirementQuery());
        long builtInCount = requirementMapper.selectCount(requirementQuery().eq(HrmPayrollRequirementDO::getBuiltIn, true));
        check(count + BUILT_IN_REQUIREMENTS - builtInCount < 500, "需求数量已达上限，需为完整原型清单保留容量");
        validateRequirement(request);
        HrmPayrollRequirementDO row = BeanUtils.toBean(request, HrmPayrollRequirementDO.class);
        row.setId(null).setVersion(1).setStatus(0).setBuiltIn(false).setOrigin("业务补充");
        row.setScopeDecision(request.getScopeDecision() == null ? 0 : request.getScopeDecision());
        row.setTenantId(tenant());
        try { requirementMapper.insert(row); }
        catch (DuplicateKeyException e) { throw exception(PAYROLL_COLLECTION_CODE_DUPLICATE); }
        audit("requirement", row.getId(), "create", null, 1, null, row, "补充候选需求");
        return row.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(HrmPayrollRequirementSaveReqVO request) {
        HrmPayrollRequirementDO before = require(request.getId(), true);
        version(before.getVersion(), request.getVersion());
        check(Objects.equals(before.getCode(), request.getCode()), "稳定编号不能修改");
        validateRequirement(request);
        HrmPayrollRequirementDO after = BeanUtils.toBean(request, HrmPayrollRequirementDO.class);
        after.setTenantId(tenant());
        after.setVersion(before.getVersion() + 1).setBuiltIn(before.getBuiltIn()).setOrigin(before.getOrigin());
        after.setScopeDecision(request.getScopeDecision() == null ? 0 : request.getScopeDecision());
        // Every edit invalidates the previous conclusion; evidence remains in immutable history.
        after.setStatus(0).setReviewedBy(null).setReviewedByName(null).setReviewedTime(null).setEvidence(null);
        requirementMapper.updateById(after);
        audit("requirement", after.getId(), "update", before.getVersion(), after.getVersion(), before, after,
                before.getStatus() == 1 ? "确认后修改，需重新评审" : "修改需求，待重新评审");
    }

    private void validateRequirement(HrmPayrollRequirementSaveReqVO request) {
        if (Objects.equals(request.getScopeDecision(), 2)) check(StrUtil.isNotBlank(request.getScopeReason()), "暂缓必须填写原因");
        if (request.getSourceCodes() == null) request.setSourceCodes(Collections.emptyList());
        check(new HashSet<>(request.getSourceCodes()).size() == request.getSourceCodes().size(), "数据来源不能重复");
        for (String code : request.getSourceCodes()) {
            check(code != null && sourceMapper.selectTenantByCode(code, tenant()) != null, "数据来源不存在或不属于当前租户");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, Integer expectedVersion) {
        HrmPayrollRequirementDO before = require(id, true);
        version(before.getVersion(), expectedVersion);
        if (Boolean.TRUE.equals(before.getBuiltIn())) throw exception(PAYROLL_COLLECTION_BUILTIN_DELETE);
        requirementMapper.deleteById(id);
        audit("requirement", id, "delete", before.getVersion(), before.getVersion() + 1, before, null, "删除业务补充需求，编号保留不可复用");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void review(HrmPayrollReviewReqVO request) {
        HrmPayrollRequirementDO before = require(request.getId(), true);
        version(before.getVersion(), request.getVersion());
        if (request.getStatus() == 1) {
            check(StrUtil.isNotBlank(before.getOwnerName()), "确认必须填写需求负责人");
            check(StrUtil.isNotBlank(before.getAcceptanceCriteria()), "确认必须填写验收条件");
            check(before.getSourceCodes() != null && !before.getSourceCodes().isEmpty(), "确认必须关联数据来源");
        }
        if (request.getStatus() == 2) check(StrUtil.isNotBlank(before.getOwnerName()), "异议必须指定处理负责人");
        HrmPayrollRequirementDO after = BeanUtils.toBean(before, HrmPayrollRequirementDO.class);
        after.setVersion(before.getVersion() + 1).setStatus(request.getStatus()).setEvidence(request.getEvidence());
        after.setReviewedBy(getLoginUserId()).setReviewedByName(actorName()).setReviewedTime(LocalDateTime.now());
        requirementMapper.updateById(after);
        audit("requirement", after.getId(), "review", before.getVersion(), after.getVersion(), before, after, request.getEvidence());
    }

    @Override
    public List<HrmPayrollSourceDO> sources() {
        return sourceMapper.selectList(new LambdaQueryWrapperX<HrmPayrollSourceDO>()
                .eq(HrmPayrollSourceDO::getTenantId, tenant()).orderByAsc(HrmPayrollSourceDO::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSource(HrmPayrollSourceSaveReqVO request) {
        check(request.getCode().startsWith("DS-CUSTOM-"), "补充来源编号须以 DS-CUSTOM- 开头");
        List<HrmPayrollSourceDO> sources = sources();
        long builtInCount = sources.stream().filter(s -> Boolean.TRUE.equals(s.getBuiltIn())).count();
        check(sources.size() + BUILT_IN_SOURCES - builtInCount < 100, "来源数量已达上限，需为 PRD 来源保留容量");
        HrmPayrollSourceDO row = BeanUtils.toBean(request, HrmPayrollSourceDO.class);
        row.setId(null).setVersion(1).setBuiltIn(false);
        row.setTenantId(tenant());
        validateSource(row);
        try { sourceMapper.insert(row); }
        catch (DuplicateKeyException e) { throw exception(PAYROLL_COLLECTION_CODE_DUPLICATE); }
        audit("source", row.getId(), "create", null, 1, null, row, "补充数据来源");
        return row.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSource(HrmPayrollSourceSaveReqVO request) {
        HrmPayrollSourceDO before = requireSource(request.getId(), true);
        version(before.getVersion(), request.getVersion());
        check(Objects.equals(before.getCode(), request.getCode()), "稳定编号不能修改");
        HrmPayrollSourceDO after = BeanUtils.toBean(request, HrmPayrollSourceDO.class);
        after.setTenantId(tenant());
        after.setVersion(before.getVersion() + 1).setBuiltIn(before.getBuiltIn());
        validateSource(after);
        sourceMapper.updateById(after);
        audit("source", after.getId(), "update", before.getVersion(), after.getVersion(), before, after,
                StrUtil.blankToDefault(request.getEvidence(), "维护数据契约，尚未确认就绪"));
    }

    private void validateSource(HrmPayrollSourceDO row) {
        if (row.getReadiness() == 1) {
            check(StrUtil.isNotBlank(row.getActualSystem()), "数据就绪必须填写实际系统");
            check(StrUtil.isNotBlank(row.getOwnerName()), "数据就绪必须填写来源负责人");
            check(StrUtil.isNotBlank(row.getFieldMapping()), "数据就绪必须填写字段映射与单位");
            check(StrUtil.isNotBlank(row.getEvidence()), "数据就绪必须填写核验依据");
            row.setConfirmedBy(getLoginUserId()).setConfirmedByName(actorName()).setConfirmedTime(LocalDateTime.now());
        } else {
            if (row.getReadiness() == 2) check(StrUtil.isNotBlank(row.getEvidence()), "待补齐来源必须说明缺口");
            row.setConfirmedBy(null).setConfirmedByName(null).setConfirmedTime(null);
        }
    }

    @Override
    public List<HrmPayrollReviewDO> history(String objectType, Long objectId) {
        check("requirement".equals(objectType) || "source".equals(objectType), "对象类型不合法");
        List<HrmPayrollReviewDO> history = reviewMapper.selectList(new LambdaQueryWrapperX<HrmPayrollReviewDO>()
                .eq(HrmPayrollReviewDO::getTenantId, tenant()).eq(HrmPayrollReviewDO::getObjectType, objectType)
                .eq(HrmPayrollReviewDO::getObjectId, objectId).orderByDesc(HrmPayrollReviewDO::getId));
        // Deleted custom requirements keep their tenant-owned history addressable.
        if (history.isEmpty()) {
            if ("requirement".equals(objectType)) require(objectId, false); else requireSource(objectId, false);
        }
        return history;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.REPEATABLE_READ)
    public Long createBaseline() {
        List<HrmPayrollRequirementDO> requirements = requirementMapper.selectList(requirementQuery().orderByAsc(HrmPayrollRequirementDO::getId));
        List<HrmPayrollSourceDO> sources = sources();
        if (requirements.size() > 500 || sources.size() > 100) throw exception(PAYROLL_BASELINE_TOO_LARGE);
        check(!requirements.isEmpty(), "请先登记候选需求");
        check(requirements.stream().filter(r -> Boolean.TRUE.equals(r.getBuiltIn())).count() == BUILT_IN_REQUIREMENTS,
                "请先登记完整的 46 个原型与征集候选需求");
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("requirements", BeanUtils.toBean(requirements, HrmPayrollRequirementRespVO.class));
        snapshot.put("sources", BeanUtils.toBean(sources, HrmPayrollSourceRespVO.class));
        String json = JsonUtils.toJsonString(snapshot);
        if (json.getBytes(StandardCharsets.UTF_8).length > 8 * 1024 * 1024) throw exception(PAYROLL_BASELINE_TOO_LARGE);
        HrmPayrollBaselineDO baseline = new HrmPayrollBaselineDO().setRequirementCount(requirements.size())
                .setSourceCount(sources.size()).setExportedBy(getLoginUserId()).setExportedByName(actorName()).setSnapshot(json);
        baseline.setTenantId(tenant());
        baselineMapper.insert(baseline);
        return baseline.getId();
    }

    @Override
    public List<HrmPayrollBaselineDO> baselines() {
        // A list never loads potentially large snapshots.
        return baselineMapper.selectList(new LambdaQueryWrapperX<HrmPayrollBaselineDO>()
                .select(HrmPayrollBaselineDO::getId, HrmPayrollBaselineDO::getRequirementCount, HrmPayrollBaselineDO::getSourceCount,
                        HrmPayrollBaselineDO::getExportedBy, HrmPayrollBaselineDO::getExportedByName, HrmPayrollBaselineDO::getCreateTime)
                .eq(HrmPayrollBaselineDO::getTenantId, tenant()).orderByDesc(HrmPayrollBaselineDO::getId).last("LIMIT 100"));
    }

    @Override
    public HrmPayrollBaselineDO baseline(Long id) {
        HrmPayrollBaselineDO baseline = baselineMapper.selectTenantById(id, tenant(), false);
        if (baseline == null) throw exception(PAYROLL_BASELINE_NOT_EXISTS);
        return baseline;
    }

    private void version(Integer actual, Integer expected) {
        if (!Objects.equals(actual, expected)) throw exception(PAYROLL_COLLECTION_VERSION_STALE);
    }

    private void check(boolean valid, String message) {
        if (!valid) throw exception(PAYROLL_COLLECTION_INVALID, message);
    }

    private String actorName() {
        Long id = getLoginUserId();
        AdminUserRespDTO user = id == null ? null : adminUserApi.getUser(id);
        return user == null ? String.valueOf(id) : user.getNickname();
    }

    private void audit(String type, Long id, String action, Integer from, Integer to, Object before, Object after, String reason) {
        HrmPayrollReviewDO review = new HrmPayrollReviewDO().setObjectType(type).setObjectId(id).setAction(action)
                .setFromVersion(from).setToVersion(to).setActorId(getLoginUserId()).setActorName(actorName()).setReason(reason)
                .setBeforeSnapshot(before == null ? null : JsonUtils.toJsonString(before))
                .setAfterSnapshot(after == null ? null : JsonUtils.toJsonString(after));
        review.setTenantId(tenant());
        reviewMapper.insert(review);
    }
}
