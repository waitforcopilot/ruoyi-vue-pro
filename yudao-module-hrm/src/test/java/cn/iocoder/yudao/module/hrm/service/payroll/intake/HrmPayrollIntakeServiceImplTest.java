package cn.iocoder.yudao.module.hrm.service.payroll.intake;

import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.intake.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.HrmPayrollSourceDO;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.HrmPayrollSourceMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.intake.*;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import javax.annotation.Resource;
import javax.validation.ConstraintViolationException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@Import({HrmPayrollIntakeServiceImpl.class, org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration.class})
class HrmPayrollIntakeServiceImplTest extends BaseDbUnitTest {
    @Resource private HrmPayrollIntakeService service;
    @Resource private HrmPayrollSourceMapper sources;
    @Resource private HrmPayrollContractMapper contracts;
    @Resource private HrmPayrollImportBatchMapper batches;
    @Resource private javax.sql.DataSource dataSource;
    @MockBean private AdminUserApi adminUserApi;
    @MockBean private PermissionApi permissionApi;
    @MockBean private cn.iocoder.yudao.module.hrm.service.payroll.identity.HrmPayrollEmployeeMappingService mappingService;

    @BeforeEach void actor() { login(1L, 10L); }
    private void login(Long tenant, Long id) {
        TenantContextHolder.setTenantId(tenant);
        LoginUser user = new LoginUser().setId(id).setTenantId(tenant).setUserType(2);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));
        when(adminUserApi.getUser(id)).thenReturn(new AdminUserRespDTO().setId(id).setNickname("测试评审人" + id));
    }
    @AfterEach void clear() { TenantContextHolder.clear(); SecurityContextHolder.clearContext(); }
    private HrmPayrollContractSaveReqVO draft() {
        HrmPayrollSourceDO source = new HrmPayrollSourceDO().setCode("DS-TEST").setName("测试来源").setReadiness(0).setVersion(1).setBuiltIn(false);
        source.setTenantId(1L); sources.insert(source);
        return new HrmPayrollContractSaveReqVO().setSourceId(source.getId()).setTitle("测试字段契约")
                .setActualSystem("隔离测试系统").setOwnerName("测试负责人").setApplicableScope("仅测试主体")
                .setSchema(HrmPayrollCsvValidatorTest.schema());
    }
    private void review(Long id, String action) {
        service.reviewContract(new HrmPayrollContractReviewReqVO().setId(id).setRevision(service.getContract(id).getRevision())
                .setAction(action).setEvidence("隔离测试评审依据"));
    }
    private Long confirmed(boolean employees) {
        HrmPayrollContractSaveReqVO request = draft();
        if (employees) request.getSchema().setEmployeeField("job");
        Long id = service.createContract(request); review(id, "confirm"); return id;
    }
    private byte[] csv(Long id, String rows) {
        byte[] template = service.template(id);
        return (new String(template, StandardCharsets.UTF_8) + rows).getBytes(StandardCharsets.UTF_8);
    }
    private Long preview(Long id, byte[] data) { return service.preview(HrmPayrollCsvValidatorTest.context(id), "测试.csv", data); }
    private HrmPayrollContractSaveReqVO edit(Long id) { return BeanUtils.toBean(service.getContract(id), HrmPayrollContractSaveReqVO.class); }
    private void employee(Long id, String job, Long tenant) {
        new JdbcTemplate(dataSource).update("INSERT INTO hrm_employee (id,name,job_number,tenant_id) VALUES (?,?,?,?)", id, "隔离员工", job, tenant);
    }

    @Test void allocatesVersionsAndIgnoresClientIdAndRevision() {
        HrmPayrollContractSaveReqVO request = draft().setId(999L).setRevision(99);
        Long one = service.createContract(request), two = service.createContract(request);
        assertNotEquals(999L, one); assertEquals(1, service.getContract(one).getContractVersion()); assertEquals(1, service.getContract(one).getRevision());
        assertEquals(2, service.getContract(two).getContractVersion()); assertEquals(0, service.getContract(two).getStatus());
        assertEquals(0, sources.selectById(request.getSourceId()).getReadiness());
    }
    @Test void editsDraftClearsNullableFieldsAndStaleWritesLeaveNoHistory() {
        Long id = service.createContract(draft()); HrmPayrollContractSaveReqVO stale = edit(id);
        service.updateContract(edit(id).setOwnerName(null).setActualSystem(null));
        assertNull(service.getContract(id).getOwnerName()); assertEquals(2, service.getContract(id).getRevision());
        assertServiceException(() -> service.updateContract(stale), PAYROLL_CONTRACT_STALE);
        assertEquals(2, service.history(id).size());
    }
    @Test void confirmationStampsServerActorAndImmutableContractCanOnlyGetNewVersion() {
        Long id = confirmed(false);
        HrmPayrollContractRespVO row = service.getContract(id);
        assertEquals("测试评审人10", row.getReviewedByName()); assertNotNull(row.getReviewedTime());
        assertServiceException(() -> service.updateContract(edit(id)), PAYROLL_CONTRACT_IMMUTABLE);
        Long next = service.createContract(edit(id)); assertEquals(2, service.getContract(next).getContractVersion()); assertEquals(0, service.getContract(next).getStatus());
        assertEquals("confirm", service.history(id).get(0).getAction());
    }
    @Test void confirmationRequiresMetadataAndExplicitPrecisionAndUnits() {
        HrmPayrollContractSaveReqVO request = draft().setActualSystem(null); Long id = service.createContract(request);
        assertServiceException(() -> review(id, "confirm"), PAYROLL_INTAKE_INVALID, "确认必须填写实际系统");
        HrmPayrollContractSaveReqVO update = edit(id).setActualSystem("测试系统"); update.getSchema().getFields().get(3).setScale(null);
        service.updateContract(update);
        assertServiceException(() -> review(id, "confirm"), PAYROLL_INTAKE_INVALID, "小数字段必须明确小数位，不能使用默认金额精度");
        update = edit(id); update.getSchema().getFields().get(3).setScale(2).setUnit(null); service.updateContract(update);
        assertServiceException(() -> review(id, "confirm"), PAYROLL_INTAKE_INVALID, "数值字段必须明确单位");
    }
    @Test void rejectsDuplicateFieldsInvalidReferencesAndOptionalKeys() {
        HrmPayrollContractSaveReqVO request = draft(); request.getSchema().getFields().get(1).setKey("job");
        assertServiceException(() -> service.createContract(request), PAYROLL_INTAKE_INVALID, "字段标识不能重复");
        request.getSchema().getFields().get(1).setKey("date"); request.getSchema().setEmployeeField("date");
        assertServiceException(() -> service.createContract(request), PAYROLL_INTAKE_INVALID, "工号校验字段必须引用必填文本字段");
        request.getSchema().setEmployeeField(null); request.getSchema().getFields().get(0).setRequired(false);
        assertServiceException(() -> service.createContract(request), PAYROLL_INTAKE_INVALID, "复合唯一键字段必须必填");
    }
    @Test void boundaryValidationRejectsBadTypeAndTooManyFields() {
        HrmPayrollContractSaveReqVO request = draft(); request.getSchema().getFields().get(0).setType("MONEY");
        assertThrows(ConstraintViolationException.class, () -> service.createContract(request));
        request.getSchema().getFields().get(0).setType("TEXT"); request.getSchema().setFields(Collections.nCopies(33, request.getSchema().getFields().get(0)));
        assertThrows(ConstraintViolationException.class, () -> service.createContract(request));
    }
    @Test void draftsAndRetiredContractsRejectTemplatesAndNewPrechecksButKeepBatches() {
        Long id = service.createContract(draft());
        assertServiceException(() -> service.template(id), PAYROLL_CONTRACT_NOT_CONFIRMED);
        review(id, "confirm"); byte[] file = csv(id, "001,2026-10-01,测试主体,0\n"); Long batch = preview(id, file);
        review(id, "retire"); assertEquals(2, service.getContract(id).getStatus());
        assertServiceException(() -> preview(id, file), PAYROLL_CONTRACT_NOT_CONFIRMED);
        assertEquals(1, service.batch(batch).getContractSnapshot().getStatus()); assertEquals(0, service.batch(batch).getStatus());
    }
    @Test void idempotencyIncludesOwnerContentContractAndDeclaredContext() {
        Long id = confirmed(false); byte[] file = csv(id, "001,2026-10-01,测试主体,0\n");
        Long first = preview(id, file);
        assertEquals(first, service.preview(HrmPayrollCsvValidatorTest.context(id), "renamed.csv", file));
        assertNotEquals(first, preview(id, csv(id, "001,2026-10-01,测试主体,1\n")));
        assertNotEquals(first, service.preview(HrmPayrollCsvValidatorTest.context(id).setDeclaredScope("其他主体"), "测试.csv", file));
        login(1L, 11L); assertNotEquals(first, preview(id, file)); assertEquals(4, batches.selectCount().intValue());
    }
    @Test void batchDataIsOwnerPrivateIncludingSameTenantAndContractIsTenantScoped() {
        Long id = confirmed(false), batch = preview(id, csv(id, "001,2026-10-01,测试主体,0\n"));
        login(1L, 11L);
        assertServiceException(() -> service.batch(batch), PAYROLL_IMPORT_BATCH_NOT_EXISTS);
        assertEquals(0L, service.batches(new HrmPayrollBatchPageReqVO()).getTotal());
        assertNotNull(service.getContract(id)); login(999L, 10L);
        assertServiceException(() -> service.getContract(id), PAYROLL_CONTRACT_NOT_EXISTS);
        assertServiceException(() -> service.history(id), PAYROLL_CONTRACT_NOT_EXISTS);
        assertServiceException(() -> service.template(id), PAYROLL_CONTRACT_NOT_EXISTS);
        assertServiceException(() -> service.batch(batch), PAYROLL_IMPORT_BATCH_NOT_EXISTS);
        assertEquals(0L, service.contracts(new HrmPayrollContractPageReqVO()).getTotal());
        assertServiceException(() -> service.createContract(editRequestWithSource(id)), PAYROLL_INTAKE_INVALID, "来源不存在或不属于当前租户");
    }
    private HrmPayrollContractSaveReqVO editRequestWithSource(Long id) {
        return new HrmPayrollContractSaveReqVO().setSourceId(contracts.selectById(id).getSourceId()).setTitle("跨租户请求").setSchema(HrmPayrollCsvValidatorTest.schema());
    }
    @Test void optionalEmployeeMatchNeedsPermissionAndRejectsMissingAmbiguousAndOtherTenant() {
        Long id = confirmed(true); byte[] file = csv(id, "known,2026-10-01,测试主体,0\nmissing,2026-10-01,测试主体,1\ndup,2026-10-01,测试主体,1\nother,2026-10-01,测试主体,1\n");
        assertServiceException(() -> preview(id, file), PAYROLL_INTAKE_EMPLOYEE_PERMISSION);
        when(permissionApi.hasAnyPermissions(10L, "hrm:employee:query")).thenReturn(true);
        employee(101L, "known", 1L); employee(102L, "dup", 1L); employee(103L, "dup", 1L); employee(104L, "other", 999L);
        Long batch = preview(id, file); HrmPayrollPreviewResultVO r = service.batch(batch).getResult();
        assertEquals(1, r.getValidCount()); assertEquals(101L, r.getRows().get(0).getEmployeeId());
        assertEquals("UNKNOWN_EMPLOYEE", r.getRows().get(1).getIssues().get(0).getCode());
        assertEquals("AMBIGUOUS_EMPLOYEE", r.getRows().get(2).getIssues().get(0).getCode());
        assertEquals("UNKNOWN_EMPLOYEE", r.getRows().get(3).getIssues().get(0).getCode());
        employee(105L, "missing", 1L); assertEquals(batch, preview(id, file));
        assertEquals("UNKNOWN_EMPLOYEE", service.batch(batch).getResult().getRows().get(1).getIssues().get(0).getCode());
    }
    @Test void malformedFileIsSavedAsFailedBatchAndListDoesNotExposeSnapshot() {
        Long id = confirmed(false), batch = preview(id, "wrong headers".getBytes(StandardCharsets.UTF_8));
        assertEquals(1, service.batch(batch).getStatus()); assertEquals("TEMPLATE_VERSION", service.batch(batch).getResult().getGlobalIssues().get(0).getCode());
        assertEquals(1L, service.batches(new HrmPayrollBatchPageReqVO()).getTotal());
        String wire = cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(service.batch(batch));
        assertTrue(wire.contains("\"periodStart\":\"2026-10-01\""));
        assertTrue(wire.contains("\"periodEnd\":\"2026-10-31\""));
        assertNull(service.contracts(new HrmPayrollContractPageReqVO()).getList().get(0).getSchema());
    }
    @Test void rejectsOversizedFilesAndReversedPeriodsWithoutSavingBatchAndSanitizesName() {
        Long id = confirmed(false);
        assertServiceException(() -> preview(id, new byte[1024 * 1024 + 1]), PAYROLL_INTAKE_INVALID, "文件超过 1 MiB 上限");
        HrmPayrollPreviewReqVO request = HrmPayrollCsvValidatorTest.context(id).setPeriodStart(java.time.LocalDate.of(2026, 11, 1));
        assertServiceException(() -> service.preview(request, "x.csv", new byte[0]), PAYROLL_INTAKE_INVALID, "期间开始不能晚于结束");
        assertEquals(0L, batches.selectCount());
        Long batch = service.preview(HrmPayrollCsvValidatorTest.context(id), "C:\\folder\\safe\r\n.csv", csv(id, "001,2026-10-01,测试主体,0\n"));
        assertEquals("safe.csv", service.batch(batch).getFileName());
    }
}
