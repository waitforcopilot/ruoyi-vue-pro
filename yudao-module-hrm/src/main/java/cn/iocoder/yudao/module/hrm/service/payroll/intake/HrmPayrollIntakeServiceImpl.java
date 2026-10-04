package cn.iocoder.yudao.module.hrm.service.payroll.intake;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.intake.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.intake.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.employee.info.HrmEmployeeMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.intake.*;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;

/** Precheck snapshots only. No attendance, payroll, or tax calculation tables are written. */
@Service
@Validated
public class HrmPayrollIntakeServiceImpl implements HrmPayrollIntakeService {
    @Resource private HrmPayrollContractMapper contractMapper;
    @Resource private HrmPayrollImportBatchMapper batchMapper;
    @Resource private HrmPayrollSourceMapper sourceMapper;
    @Resource private HrmPayrollReviewMapper reviewMapper;
    @Resource private HrmEmployeeMapper employeeMapper;
    @Resource private PermissionApi permissionApi;
    @Resource private AdminUserApi adminUserApi;

    private Long tenant() { return TenantContextHolder.getRequiredTenantId(); }

    @Override public List<HrmPayrollSourceDO> sourceOptions() {
        return sourceMapper.selectList(new LambdaQueryWrapperX<HrmPayrollSourceDO>().eq(HrmPayrollSourceDO::getTenantId, tenant())
                .select(HrmPayrollSourceDO::getId, HrmPayrollSourceDO::getCode, HrmPayrollSourceDO::getName,
                        HrmPayrollSourceDO::getReadiness, HrmPayrollSourceDO::getActualSystem, HrmPayrollSourceDO::getOwnerName)
                .orderByAsc(HrmPayrollSourceDO::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public Long createContract(HrmPayrollContractSaveReqVO request) {
        HrmPayrollSourceDO source = sourceMapper.selectTenantById(request.getSourceId(), tenant(), true);
        check(source != null, "来源不存在或不属于当前租户");
        validateSchema(request.getSchema(), false);
        HrmPayrollContractDO latest = contractMapper.selectLatest(source.getId(), tenant());
        HrmPayrollContractDO row = editable(request);
        row.setId(null).setSourceCode(source.getCode()).setSourceName(source.getName())
                .setContractVersion(latest == null ? 1 : latest.getContractVersion() + 1).setRevision(1).setStatus(0);
        contractMapper.insert(row);
        audit("create", null, row, "登记字段草稿，尚未确认");
        return row.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateContract(HrmPayrollContractSaveReqVO request) {
        HrmPayrollContractDO before = require(request.getId(), true);
        revision(before, request.getRevision());
        if (before.getStatus() != 0) throw exception(PAYROLL_CONTRACT_IMMUTABLE);
        check(Objects.equals(before.getSourceId(), request.getSourceId()), "已登记契约的来源不能修改");
        validateSchema(request.getSchema(), false);
        HrmPayrollContractDO after = editable(request).setRevision(before.getRevision() + 1);
        after.setSourceCode(before.getSourceCode()).setSourceName(before.getSourceName())
                .setContractVersion(before.getContractVersion()).setStatus(0);
        contractMapper.updateById(after);
        audit("update", before, after, "修改草稿，需确认后才能预检");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewContract(HrmPayrollContractReviewReqVO request) {
        HrmPayrollContractDO before = require(request.getId(), true);
        revision(before, request.getRevision());
        if ("confirm".equals(request.getAction())) {
            check(before.getStatus() == 0, "只有草稿可确认");
            check(StrUtil.isNotBlank(before.getActualSystem()), "确认必须填写实际系统");
            check(StrUtil.isNotBlank(before.getOwnerName()), "确认必须填写来源负责人");
            check(StrUtil.isNotBlank(before.getApplicableScope()), "确认必须填写适用范围");
            validateSchema(schema(before), true);
        } else check(before.getStatus() == 1, "只有已确认契约可停用");
        HrmPayrollContractDO after = BeanUtils.toBean(before, HrmPayrollContractDO.class)
                .setStatus("confirm".equals(request.getAction()) ? 1 : 2).setRevision(before.getRevision() + 1)
                .setEvidence(request.getEvidence()).setReviewedBy(getLoginUserId())
                .setReviewedByName(actorName()).setReviewedTime(LocalDateTime.now());
        contractMapper.updateById(after);
        audit(request.getAction(), before, after, request.getEvidence());
    }

    private HrmPayrollContractDO editable(HrmPayrollContractSaveReqVO request) {
        HrmPayrollContractDO row = BeanUtils.toBean(request, HrmPayrollContractDO.class);
        row.setTenantId(tenant());
        return row.setSchemaJson(JsonUtils.toJsonString(request.getSchema())).setFieldCount(request.getSchema().getFields().size());
    }

    private void validateSchema(HrmPayrollContractSchemaVO schema, boolean confirming) {
        schema.setPeriodField(StrUtil.trimToNull(schema.getPeriodField()));
        schema.setSubjectField(StrUtil.trimToNull(schema.getSubjectField()));
        schema.setEmployeeField(StrUtil.trimToNull(schema.getEmployeeField()));
        Map<String, HrmPayrollContractSchemaVO.Field> fields = new LinkedHashMap<>();
        for (HrmPayrollContractSchemaVO.Field field : schema.getFields()) {
            check(field != null, "字段不能为 null");
            check(!fields.containsKey(field.getKey()), "字段标识不能重复");
            fields.put(field.getKey(), field);
            if (confirming) {
                if ("DECIMAL".equals(field.getType())) {
                    check(field.getScale() != null, "小数字段必须明确小数位，不能使用默认金额精度");
                    check(StrUtil.isNotBlank(field.getUnit()), "数值字段必须明确单位");
                }
                if ("INTEGER".equals(field.getType())) check(StrUtil.isNotBlank(field.getUnit()), "数值字段必须明确单位");
                if ("TEXT".equals(field.getType())) check(field.getMaxLength() != null, "文本字段必须明确最大长度");
            }
        }
        check(new HashSet<>(schema.getKeyFields()).size() == schema.getKeyFields().size(), "复合唯一键字段不能重复");
        for (String key : schema.getKeyFields()) {
            check(key != null && fields.containsKey(key), "复合唯一键引用了不存在的字段");
            check(Boolean.TRUE.equals(fields.get(key).getRequired()), "复合唯一键字段必须必填");
        }
        reference(fields, schema.getPeriodField(), "DATE", "期间校验字段必须引用必填日期字段");
        reference(fields, schema.getSubjectField(), "TEXT", "主体校验字段必须引用必填文本字段");
        reference(fields, schema.getEmployeeField(), "TEXT", "工号校验字段必须引用必填文本字段");
    }

    private void reference(Map<String, HrmPayrollContractSchemaVO.Field> fields, String key, String type, String message) {
        if (StrUtil.isBlank(key)) return;
        HrmPayrollContractSchemaVO.Field field = fields.get(key);
        check(field != null && type.equals(field.getType()) && Boolean.TRUE.equals(field.getRequired()), message);
    }

    private HrmPayrollContractDO require(Long id, boolean lock) {
        HrmPayrollContractDO row = id == null ? null : contractMapper.selectTenantById(id, tenant(), lock);
        if (row == null) throw exception(PAYROLL_CONTRACT_NOT_EXISTS);
        return row;
    }

    private HrmPayrollContractDO confirmed(Long id, boolean lock) {
        HrmPayrollContractDO row = require(id, lock);
        if (row.getStatus() != 1) throw exception(PAYROLL_CONTRACT_NOT_CONFIRMED);
        return row;
    }

    private HrmPayrollContractSchemaVO schema(HrmPayrollContractDO row) {
        return JsonUtils.parseObject(row.getSchemaJson(), HrmPayrollContractSchemaVO.class);
    }

    private HrmPayrollContractRespVO response(HrmPayrollContractDO row) {
        HrmPayrollContractRespVO value = BeanUtils.toBean(row, HrmPayrollContractRespVO.class);
        if (row.getSchemaJson() != null) value.setSchema(schema(row));
        return value;
    }

    @Override public HrmPayrollContractRespVO getContract(Long id) { return response(require(id, false)); }

    @Override public PageResult<HrmPayrollContractRespVO> contracts(HrmPayrollContractPageReqVO query) {
        LambdaQueryWrapperX<HrmPayrollContractDO> wrapper = new LambdaQueryWrapperX<HrmPayrollContractDO>()
                .eq(HrmPayrollContractDO::getTenantId, tenant())
                .eqIfPresent(HrmPayrollContractDO::getSourceId, query.getSourceId())
                .eqIfPresent(HrmPayrollContractDO::getStatus, query.getStatus());
        if (StrUtil.isNotBlank(query.getSearch())) wrapper.and(w -> w.like(HrmPayrollContractDO::getTitle, query.getSearch())
                .or().like(HrmPayrollContractDO::getSourceCode, query.getSearch()));
        wrapper.select(HrmPayrollContractDO.class, field -> !"schemaJson".equals(field.getProperty()));
        PageResult<HrmPayrollContractDO> page = contractMapper.selectPage(query, wrapper.orderByDesc(HrmPayrollContractDO::getId));
        return new PageResult<>(page.getList().stream().map(this::response).collect(Collectors.toList()), page.getTotal());
    }

    @Override public List<HrmPayrollReviewDO> history(Long id) {
        require(id, false);
        return reviewMapper.selectList(new LambdaQueryWrapperX<HrmPayrollReviewDO>().eq(HrmPayrollReviewDO::getTenantId, tenant())
                .eq(HrmPayrollReviewDO::getObjectType, "contract").eq(HrmPayrollReviewDO::getObjectId, id)
                .orderByDesc(HrmPayrollReviewDO::getId));
    }

    @Override public byte[] template(Long id) {
        HrmPayrollContractDO row = confirmed(id, false);
        String headers = schema(row).getFields().stream().map(HrmPayrollContractSchemaVO.Field::getKey).collect(Collectors.joining(","));
        return ("\uFEFF#hrm-payroll-contract," + row.getId() + "," + row.getContractVersion() + "\r\n" + headers + "\r\n")
                .getBytes(StandardCharsets.UTF_8);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public Long preview(HrmPayrollPreviewReqVO request, String fileName, byte[] bytes) {
        check(bytes != null && bytes.length <= HrmPayrollCsvValidator.MAX_BYTES, "文件超过 1 MiB 上限");
        check(!request.getPeriodStart().isAfter(request.getPeriodEnd()), "期间开始不能晚于结束");
        request.setDeclaredScope(request.getDeclaredScope().trim());
        HrmPayrollContractDO contract = confirmed(request.getContractId(), true);
        HrmPayrollContractSchemaVO schema = schema(contract);
        boolean matchEmployees = StrUtil.isNotBlank(schema.getEmployeeField());
        if (matchEmployees && !permissionApi.hasAnyPermissions(getLoginUserId(), "hrm:employee:query"))
            throw exception(PAYROLL_INTAKE_EMPLOYEE_PERMISSION);
        String fileHash = sha256(bytes);
        String key = sha256(JsonUtils.toJsonString(Arrays.asList(getLoginUserId(), contract.getId(),
                request.getDeclaredScope(), request.getPeriodStart(), request.getPeriodEnd(), fileHash)).getBytes(StandardCharsets.UTF_8));
        HrmPayrollImportBatchDO existing = batchMapper.selectIdempotent(key, tenant(), getLoginUserId());
        if (existing != null) return existing.getId();
        HrmPayrollPreviewResultVO result = HrmPayrollCsvValidator.validate(bytes, schema, contract.getId(), contract.getContractVersion(), request);
        if (matchEmployees) matchEmployees(result, schema.getEmployeeField());
        HrmPayrollCsvValidator.finish(result);
        HrmPayrollBatchDetailRespVO snapshot = new HrmPayrollBatchDetailRespVO().setContractSnapshot(response(contract)).setResult(result);
        String json = JsonUtils.toJsonString(snapshot);
        check(json.getBytes(StandardCharsets.UTF_8).length <= 4 * 1024 * 1024, "预检结果超过 4 MiB 保存上限，请拆分文件");
        HrmPayrollImportBatchDO batch = new HrmPayrollImportBatchDO().setContractId(contract.getId()).setSourceCode(contract.getSourceCode())
                .setContractVersion(contract.getContractVersion()).setFileName(safeFileName(fileName)).setFileHash(fileHash).setIdempotencyKey(key)
                .setDeclaredScope(request.getDeclaredScope()).setPeriodStart(request.getPeriodStart()).setPeriodEnd(request.getPeriodEnd())
                .setRowCount(result.getRowCount()).setValidCount(result.getValidCount()).setErrorCount(result.getErrorCount())
                .setStatus(result.getErrorCount() == 0 ? 0 : 1).setCreatedBy(getLoginUserId()).setCreatedByName(actorName()).setSnapshot(json);
        batch.setTenantId(tenant());
        batchMapper.insert(batch);
        return batch.getId();
    }

    private void matchEmployees(HrmPayrollPreviewResultVO result, String field) {
        Set<String> numbers = result.getRows().stream().map(r -> r.getValues().get(field)).filter(Objects::nonNull).collect(Collectors.toSet());
        if (numbers.isEmpty()) return;
        // This older HRM DO lacks a Java tenantId property; constrain the physical column explicitly as well as the interceptor.
        List<HrmEmployeeDO> employees = employeeMapper.selectList(new QueryWrapper<HrmEmployeeDO>()
                .select("id", "job_number").eq("tenant_id", tenant()).in("job_number", numbers));
        Map<String, List<HrmEmployeeDO>> byNumber = employees.stream().collect(Collectors.groupingBy(HrmEmployeeDO::getJobNumber));
        for (HrmPayrollPreviewResultVO.Row row : result.getRows()) {
            String number = row.getValues().get(field);
            if (number == null) continue;
            List<HrmEmployeeDO> matches = byNumber.getOrDefault(number, Collections.emptyList());
            if (matches.size() != 1) row.getIssues().add(HrmPayrollCsvValidator.issue(row.getLine(), field,
                    matches.isEmpty() ? "UNKNOWN_EMPLOYEE" : "AMBIGUOUS_EMPLOYEE",
                    matches.isEmpty() ? "当前租户未找到对应 HRM 工号" : "当前租户存在重复 HRM 工号，需先核实人员主档"));
            else row.setEmployeeId(matches.get(0).getId());
        }
    }

    @Override public PageResult<HrmPayrollBatchRespVO> batches(HrmPayrollBatchPageReqVO query) {
        PageResult<HrmPayrollImportBatchDO> page = batchMapper.selectPage(query,
                new LambdaQueryWrapperX<HrmPayrollImportBatchDO>().eq(HrmPayrollImportBatchDO::getTenantId, tenant())
                        .eq(HrmPayrollImportBatchDO::getCreatedBy, getLoginUserId())
                        .eqIfPresent(HrmPayrollImportBatchDO::getContractId, query.getContractId())
                        .eqIfPresent(HrmPayrollImportBatchDO::getStatus, query.getStatus())
                        .select(HrmPayrollImportBatchDO.class, field -> !"snapshot".equals(field.getProperty()) && !"idempotencyKey".equals(field.getProperty()))
                        .orderByDesc(HrmPayrollImportBatchDO::getId));
        return BeanUtils.toBean(page, HrmPayrollBatchRespVO.class);
    }

    @Override public HrmPayrollBatchDetailRespVO batch(Long id) {
        HrmPayrollImportBatchDO row = batchMapper.selectOwned(id, tenant(), getLoginUserId());
        if (row == null) throw exception(PAYROLL_IMPORT_BATCH_NOT_EXISTS);
        HrmPayrollBatchDetailRespVO snapshot = JsonUtils.parseObject(row.getSnapshot(), HrmPayrollBatchDetailRespVO.class);
        return BeanUtils.toBean(row, HrmPayrollBatchDetailRespVO.class)
                .setContractSnapshot(snapshot.getContractSnapshot()).setResult(snapshot.getResult());
    }

    private String safeFileName(String name) {
        String value = name == null ? "upload.csv" : name.replace('\\', '/');
        value = value.substring(value.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "");
        return value.isEmpty() ? "upload.csv" : value.substring(0, Math.min(value.length(), 200));
    }

    private String sha256(byte[] bytes) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder result = new StringBuilder(64);
            for (byte value : digest) result.append(String.format("%02x", value & 0xff));
            return result.toString();
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    private void revision(HrmPayrollContractDO row, Integer expected) {
        if (!Objects.equals(row.getRevision(), expected)) throw exception(PAYROLL_CONTRACT_STALE);
    }
    private void check(boolean valid, String message) { if (!valid) throw exception(PAYROLL_INTAKE_INVALID, message); }
    private String actorName() {
        AdminUserRespDTO actor = adminUserApi.getUser(getLoginUserId());
        return actor == null ? String.valueOf(getLoginUserId()) : actor.getNickname();
    }
    private void audit(String action, HrmPayrollContractDO before, HrmPayrollContractDO after, String reason) {
        HrmPayrollReviewDO row = new HrmPayrollReviewDO().setObjectType("contract").setObjectId(after.getId()).setAction(action)
                .setFromVersion(before == null ? null : before.getRevision()).setToVersion(after.getRevision())
                .setActorId(getLoginUserId()).setActorName(actorName()).setReason(reason)
                .setBeforeSnapshot(before == null ? null : JsonUtils.toJsonString(response(before))).setAfterSnapshot(JsonUtils.toJsonString(response(after)));
        row.setTenantId(tenant());
        reviewMapper.insert(row);
    }
}
