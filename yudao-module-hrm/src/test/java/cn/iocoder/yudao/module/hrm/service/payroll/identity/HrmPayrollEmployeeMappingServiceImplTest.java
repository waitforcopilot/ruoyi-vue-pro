package cn.iocoder.yudao.module.hrm.service.payroll.identity;
import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.identity.*;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.intake.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.HrmPayrollSourceDO;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.HrmPayrollSourceMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.identity.HrmPayrollEmployeeMappingMapper;
import cn.iocoder.yudao.module.hrm.service.payroll.intake.*;
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
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@Import({HrmPayrollEmployeeMappingServiceImpl.class,HrmPayrollEmployeeAccess.class,HrmPayrollIntakeServiceImpl.class})
class HrmPayrollEmployeeMappingServiceImplTest extends BaseDbUnitTest {
    @Resource private HrmPayrollEmployeeMappingService service;
    @Resource private HrmPayrollIntakeService intake;
    @Resource private HrmPayrollSourceMapper sources;
    @Resource private HrmPayrollEmployeeMappingMapper mapper;
    @Resource private javax.sql.DataSource dataSource;
    @MockBean private PermissionApi permissionApi;
    @MockBean private AdminUserApi adminUserApi;
    private JdbcTemplate jdbc;
    @BeforeEach void fixture() {
        jdbc=new JdbcTemplate(dataSource);login(1L,10L);
        when(permissionApi.hasAnyPermissions(10L,"hrm:employee:query")).thenReturn(true);
        when(permissionApi.hasAnyPermissions(10L,"hrm:payroll:identity:query")).thenReturn(true);
        scope(new DeptDataPermissionRespDTO().setAll(true));
        source(1L,1L);source(2L,999L);
        person(101L,1L,"合成人员 A","001",11L,10L);person(102L,1L,"合成人员 B","001",22L,20L);person(999L,999L,"合成租户 B","001",11L,10L);
    }
    private void login(Long tenant,Long actor) {
        TenantContextHolder.setTenantId(tenant);SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new LoginUser().setId(actor).setTenantId(tenant).setUserType(2),null,Collections.emptyList()));
        when(adminUserApi.getUser(actor)).thenReturn(new AdminUserRespDTO().setId(actor).setNickname("合成评审人"));
    }
    @AfterEach void clear() { TenantContextHolder.clear();SecurityContextHolder.clearContext(); }
    private void scope(DeptDataPermissionRespDTO scope) { when(permissionApi.getDeptDataPermission(10L)).thenReturn(scope); }
    private void source(Long id,Long tenant) {
        HrmPayrollSourceDO row=new HrmPayrollSourceDO().setId(id).setCode("DS-MAPPING").setName("合成来源").setReadiness(0).setVersion(1);row.setTenantId(tenant);sources.insert(row);
    }
    private void person(Long id,Long tenant,String name,String job,Long dept,Long user) {
        jdbc.update("INSERT INTO hrm_employee(id,name,job_number,dept_id,user_id,status,tenant_id) VALUES(?,?,?,?,?,20,?)",id,name,job,dept,user,tenant);
    }
    private HrmPayrollMappingSaveReqVO draft(String code) {
        return new HrmPayrollMappingSaveReqVO().setSourceId(1L).setNamespace("QA-SOURCE").setExternalCode(code).setEmployeeId(101L)
                .setOwnerName("合成负责人").setReference("合成样例，非正式人员/计薪资格结论")
                .setEffectiveFrom(LocalDate.of(2026,10,1)).setEffectiveTo(LocalDate.of(2026,10,31));
    }
    private HrmPayrollMappingSaveReqVO edit(Long id) { return BeanUtils.toBean(service.get(id),HrmPayrollMappingSaveReqVO.class); }
    private void review(Long id,String action) { service.review(new HrmPayrollMappingReviewReqVO().setId(id).setRevision(service.get(id).getRevision()).setAction(action).setEvidence("合成身份核对依据")); }
    private Long confirmed(String code) { Long id=service.create(draft(code));review(id,"confirm");return id; }
    private HrmPayrollMappingLookupVO lookup(String code,String start,String end) { return new HrmPayrollMappingLookupVO().setExternalCode(code).setStart(LocalDate.parse(start)).setEnd(LocalDate.parse(end)); }
    private HrmPayrollMappingLookupVO.Match resolve(String code,String start,String end) { return service.resolve(1L,"QA-SOURCE",Collections.singletonList(lookup(code,start,end))).get(0); }
    private HrmPayrollMappingPageReqVO page() { HrmPayrollMappingPageReqVO q=new HrmPayrollMappingPageReqVO();q.setPageSize(100);return q; }

    @Test void sourceNamespaceCodeIsUniqueAndServerAllocatesVersionIdentityAndSnapshots() {
        Long id=service.create(draft("0001").setId(987L).setRevision(99));HrmPayrollMappingRespVO row=service.get(id);
        assertNotEquals(987L,id);assertEquals(1,row.getMappingVersion());assertEquals(1,row.getRevision());assertEquals("合成人员 A",row.getSnapshotName());assertEquals("001",row.getSnapshotJobNumber());
        assertNotNull(row.getSnapshotCapturedAt());assertServiceException(()->service.create(draft("0001")),PAYROLL_MAPPING_DUPLICATE);
        service.create(draft("0001").setNamespace("QA-OTHER"));service.create(draft("1"));assertEquals(3,service.page(page()).getTotal());
    }
    @Test void exactCodeCaseAndLeadingZerosDoNotCollapse() {
        Long upper=confirmed("Aa"),lower=confirmed("aa"),zero=confirmed("001");
        assertEquals(upper,resolve("Aa","2026-10-01","2026-10-31").getMappingId());assertEquals(lower,resolve("aa","2026-10-01","2026-10-31").getMappingId());
        assertEquals(zero,resolve("001","2026-10-01","2026-10-31").getMappingId());assertNull(resolve("1","2026-10-01","2026-10-31").getEmployeeId());
    }
    @Test void editsAreVersionedImmutableAfterConfirmationAndNullsCanBeCleared() {
        Long id=service.create(draft("EDIT"));HrmPayrollMappingSaveReqVO stale=edit(id);service.update(edit(id).setEffectiveTo(null).setOwnerName(null));
        assertNull(service.get(id).getOwnerName());assertNull(service.get(id).getEffectiveTo());assertServiceException(()->service.update(stale),PAYROLL_MAPPING_STALE);
        service.update(edit(id).setOwnerName("合成负责人"));review(id,"confirm");assertServiceException(()->service.update(edit(id)),PAYROLL_MAPPING_IMMUTABLE);
        Long next=service.newVersion(id,service.get(id).getRevision(),102L);assertEquals(2,service.get(next).getMappingVersion());assertEquals(102L,service.get(next).getEmployeeId());
        assertNull(service.get(next).getReviewedByName());assertNull(service.get(next).getEvidence());assertEquals(0,service.get(next).getStatus());
    }
    @Test void stableIdentityCannotBeChangedByEditing() {
        Long id=service.create(draft("STABLE"));assertServiceException(()->service.update(edit(id).setExternalCode("OTHER")),PAYROLL_MAPPING_INVALID,"来源、命名空间和外部编号固定，不能修改");
    }
    @Test void confirmationRequiresRealOwnerEvidenceAndDate() {
        Long id=service.create(draft("META").setOwnerName(null));assertServiceException(()->review(id,"confirm"),PAYROLL_MAPPING_INVALID,"确认必须填写映射负责人");
        service.update(edit(id).setOwnerName("负责人").setReference(null));assertServiceException(()->review(id,"confirm"),PAYROLL_MAPPING_INVALID,"确认必须填写来源依据");
        service.update(edit(id).setReference("依据").setEffectiveFrom(null).setEffectiveTo(null));assertServiceException(()->review(id,"confirm"),PAYROLL_MAPPING_INVALID,"确认必须填写有效期开始日期");
    }
    @Test void masterDriftBlocksConfirmationUntilDraftIsExplicitlyRefreshed() {
        Long id=service.create(draft("DRIFT"));jdbc.update("UPDATE hrm_employee SET name='合成人员 A 新姓名' WHERE id=101");
        assertServiceException(()->review(id,"confirm"),PAYROLL_MAPPING_PERSON_CHANGED);assertEquals("合成人员 A",service.get(id).getSnapshotName());
        service.update(edit(id).setEmployeeFingerprint(null));assertEquals("合成人员 A 新姓名",service.get(id).getSnapshotName());review(id,"confirm");
        jdbc.update("UPDATE hrm_employee SET name='确认后再次变化',dept_id=22 WHERE id=101");assertEquals("合成人员 A 新姓名",service.get(id).getSnapshotName());assertEquals(11L,service.get(id).getSnapshotDeptId());
    }
    @Test void pickerFingerprintPreventsSilentRaceDuringSave() {
        String fingerprint=service.employee(101L).getEmployeeFingerprint();jdbc.update("UPDATE hrm_employee SET user_id=20 WHERE id=101");
        assertServiceException(()->service.create(draft("RACE").setEmployeeFingerprint(fingerprint)),PAYROLL_MAPPING_PERSON_CHANGED);
    }
    @Test void inclusiveOverlapOpenEndAndAdjacentVersionsAreChecked() {
        Long id=confirmed("PERIOD"),next=service.newVersion(id,service.get(id).getRevision(),null);
        assertServiceException(()->review(next,"confirm"),PAYROLL_MAPPING_OVERLAP);
        service.update(edit(next).setEffectiveFrom(LocalDate.of(2026,10,31)).setEffectiveTo(null));assertServiceException(()->review(next,"confirm"),PAYROLL_MAPPING_OVERLAP);
        service.update(edit(next).setEffectiveFrom(LocalDate.of(2026,11,1)));review(next,"confirm");
        assertEquals(next,resolve("PERIOD","2026-11-01","2027-01-01").getMappingId());assertNull(resolve("PERIOD","2026-10-01","2026-11-01").getMappingId());
    }
    @Test void dateResolutionAndRetirementPreserveOldSnapshotEvidence() {
        Long id=confirmed("ARCHIVE");assertEquals(id,resolve("ARCHIVE","2026-10-31","2026-10-31").getMappingId());
        assertNull(resolve("ARCHIVE","2026-09-30","2026-09-30").getMappingId());review(id,"retire");assertNull(resolve("ARCHIVE","2026-10-01","2026-10-01").getMappingId());
        assertTrue(service.history(id).get(0).getBeforeSnapshot().contains("合成人员 A"));assertEquals("retire",service.history(id).get(0).getAction());
    }
    @Test void tenantIsolationProtectsPersonnelAllIdPathsAndResolution() {
        Long id=confirmed("TENANT");assertServiceException(()->service.create(draft("BADPERSON").setEmployeeId(999L)),PAYROLL_MAPPING_NOT_EXISTS);
        login(999L,10L);assertServiceException(()->service.get(id),PAYROLL_MAPPING_NOT_EXISTS);assertServiceException(()->service.history(id),PAYROLL_MAPPING_NOT_EXISTS);
        assertServiceException(()->service.employee(101L),PAYROLL_MAPPING_NOT_EXISTS);assertEquals(0,service.page(page()).getTotal());assertNull(resolve("TENANT","2026-10-01","2026-10-01").getEmployeeId());
    }
    @Test void currentAndHistoricalDepartmentAreBothRequiredForScopedAccess() {
        Long a=confirmed("A"),b=service.create(draft("B").setEmployeeId(102L));review(b,"confirm");
        scope(new DeptDataPermissionRespDTO().setDeptIds(new HashSet<>(Collections.singletonList(11L))));
        assertEquals(1,service.page(page()).getTotal());assertEquals(a,resolve("A","2026-10-01","2026-10-01").getMappingId());assertNull(resolve("B","2026-10-01","2026-10-01").getMappingId());
        assertServiceException(()->service.get(b),PAYROLL_MAPPING_NOT_EXISTS);assertServiceException(()->service.employee(102L),PAYROLL_MAPPING_NOT_EXISTS);
        assertNull(service.history(a).get(0).getBeforeSnapshot());assertNull(service.history(a).get(0).getAfterSnapshot());assertNull(service.history(a).get(0).getReason());
        jdbc.update("UPDATE hrm_employee SET dept_id=22 WHERE id=101");assertEquals(0,service.page(page()).getTotal());assertServiceException(()->service.get(a),PAYROLL_MAPPING_NOT_EXISTS);
        scope(new DeptDataPermissionRespDTO().setDeptIds(new HashSet<>(Collections.singletonList(22L))));assertServiceException(()->service.get(a),PAYROLL_MAPPING_NOT_EXISTS);
    }
    @Test void selfScopeRequiresBothCurrentAndCapturedUserBinding() {
        confirmed("SELF");Long other=service.create(draft("OTHER").setEmployeeId(102L));review(other,"confirm");scope(new DeptDataPermissionRespDTO().setSelf(true));
        assertEquals(1,service.page(page()).getTotal());assertServiceException(()->service.get(other),PAYROLL_MAPPING_NOT_EXISTS);jdbc.update("UPDATE hrm_employee SET user_id=20 WHERE id=101");assertEquals(0,service.page(page()).getTotal());
    }
    @Test void missingScopeAndEmployeeQueryPermissionFailClosed() {
        Long id=confirmed("NOACCESS");when(permissionApi.getDeptDataPermission(10L)).thenReturn(null);assertEquals(0,service.page(page()).getTotal());assertServiceException(()->service.get(id),PAYROLL_MAPPING_NOT_EXISTS);
        when(permissionApi.hasAnyPermissions(10L,"hrm:employee:query")).thenReturn(false);assertServiceException(()->service.page(page()),PAYROLL_MAPPING_PERMISSION);
    }
    @Test void deletedMasterBlocksNewResolutionButFullScopeCanReadFrozenHistory() {
        Long id=confirmed("DELETED");jdbc.update("UPDATE hrm_employee SET deleted=TRUE WHERE id=101");assertNull(resolve("DELETED","2026-10-01","2026-10-01").getEmployeeId());
        assertEquals("合成人员 A",service.get(id).getSnapshotName());review(id,"retire");assertEquals(2,service.get(id).getStatus());
    }
    @Test void dateAndNamespaceValidationRejectsMalformedInputWithoutAuditWrites() {
        assertServiceException(()->service.create(draft("BAD\nID")),PAYROLL_MAPPING_INVALID,"外部编号不能为空、超过 128 字符或包含控制字符");
        assertServiceException(()->service.create(draft("BAD").setNamespace("qa lower")),PAYROLL_MAPPING_INVALID,"命名空间须为稳定英文编号");
        assertServiceException(()->service.create(draft("BAD").setEffectiveFrom(LocalDate.of(999,1,1))),PAYROLL_MAPPING_INVALID,"日期超过数据库支持范围");assertEquals(0,mapper.selectCount());
    }
    @Test void wireFormatHasIsoDatesAndNoUnrelatedEmployeePersonalFields() throws Exception {
        Long id=confirmed("WIRE");String json=JsonUtils.toJsonString(service.get(id));assertTrue(json.contains("\"effectiveFrom\":\"2026-10-01\""));
        assertFalse(json.contains("mobile"));assertFalse(json.contains("idNumber"));assertFalse(json.contains("identityKey"));assertFalse(json.contains("salary"));
        // The HTTP mapper uses Yudao's timestamp serializer, unlike JsonUtils in this isolated test.
        org.springframework.http.converter.json.Jackson2ObjectMapperBuilder builder=new org.springframework.http.converter.json.Jackson2ObjectMapperBuilder();
        new cn.iocoder.yudao.framework.jackson.config.YudaoJacksonAutoConfiguration().ldtEpochMillisCustomizer().customize(builder);
        com.fasterxml.jackson.databind.ObjectMapper httpMapper=builder.build();
        com.fasterxml.jackson.databind.JsonNode wire=httpMapper.readTree(httpMapper.writeValueAsString(service.get(id)));
        assertEquals("2026-10-01",wire.get("effectiveFrom").asText());
        assertTrue(wire.get("snapshotCapturedAt").asText().matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}"));
        assertTrue(wire.get("reviewedTime").asText().matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}"));
        HrmPayrollMappingLookupVO.Match match=resolve("WIRE","2026-10-01","2026-10-01");
        assertEquals(wire.get("snapshotCapturedAt").asText(),httpMapper.readTree(httpMapper.writeValueAsString(match)).get("snapshotCapturedAt").asText());
    }
    private Long contract(boolean perRowDate) {
        List<HrmPayrollContractSchemaVO.Field> fields=new ArrayList<>(Arrays.asList(
                new HrmPayrollContractSchemaVO.Field().setKey("external").setLabel("外部编号").setType("TEXT").setRequired(true).setMaxLength(128),
                new HrmPayrollContractSchemaVO.Field().setKey("date").setLabel("数据日期").setType("DATE").setRequired(true)));
        HrmPayrollContractSchemaVO schema=new HrmPayrollContractSchemaVO().setFields(fields).setKeyFields(Arrays.asList("external","date"))
                .setExternalEmployeeField("external").setEmployeeNamespace("QA-SOURCE").setPeriodField(perRowDate?"date":null);
        Long id=intake.createContract(new HrmPayrollContractSaveReqVO().setSourceId(1L).setTitle("合成外部编号契约").setActualSystem("合成来源")
                .setOwnerName("合成负责人").setApplicableScope("仅合成测试").setSchema(schema));
        intake.reviewContract(new HrmPayrollContractReviewReqVO().setId(id).setRevision(intake.getContract(id).getRevision()).setAction("confirm").setEvidence("合成契约确认"));return id;
    }
    private Long preview(Long contract,String data,String end) {
        byte[] csv=(new String(intake.template(contract),StandardCharsets.UTF_8)+data).getBytes(StandardCharsets.UTF_8);
        return intake.preview(new HrmPayrollPreviewReqVO().setContractId(contract).setDeclaredScope("QA").setPeriodStart(LocalDate.of(2026,10,1)).setPeriodEnd(LocalDate.parse(end)),"合成.csv",csv);
    }
    @Test void csvUsesConfirmedMappingInsteadOfDuplicateHrmJobNumbersAndRetainsVersionReference() {
        Long id=confirmed("EXT"),contract=contract(true),batch=preview(contract,"EXT,2026-10-20\nUNKNOWN,2026-10-21\n","2026-10-31");
        HrmPayrollBatchDetailRespVO saved=intake.batch(batch);assertEquals(1,saved.getValidCount());assertEquals(1,saved.getErrorCount());
        assertEquals(id,saved.getResult().getRows().get(0).getEmployeeMapping().getMappingId());assertEquals(101L,saved.getResult().getRows().get(0).getEmployeeId());
        assertEquals("EXTERNAL_MAPPING",saved.getResult().getEmployeeMatchMode());assertNull(saved.getResult().getRows().get(1).getEmployeeId());
        assertEquals(batch,preview(contract,"EXT,2026-10-20\nUNKNOWN,2026-10-21\n","2026-10-31"));
    }
    @Test void changedMappingCreatesNewCsvBatchWhileOldOutcomeRemainsFrozen() {
        Long id=confirmed("EXT"),contract=contract(true),before=preview(contract,"EXT,2026-10-20\n","2026-10-31");review(id,"retire");
        Long after=preview(contract,"EXT,2026-10-20\n","2026-10-31");assertNotEquals(before,after);assertEquals(1,intake.batch(before).getValidCount());assertEquals(1,intake.batch(after).getErrorCount());
        Long next=service.newVersion(id,service.get(id).getRevision(),102L);review(next,"confirm");Long rebound=preview(contract,"EXT,2026-10-20\n","2026-10-31");
        assertNotEquals(after,rebound);assertEquals(102L,intake.batch(rebound).getResult().getRows().get(0).getEmployeeId());assertEquals(101L,intake.batch(before).getResult().getRows().get(0).getEmployeeId());
    }
    @Test void csvWithoutDateFieldRequiresOneMappingForWholeDeclaredPeriod() {
        Long id=confirmed("EXT"),contract=contract(false);Long next=service.newVersion(id,service.get(id).getRevision(),null);
        service.update(edit(next).setEffectiveFrom(LocalDate.of(2026,11,1)).setEffectiveTo(LocalDate.of(2026,11,30)));review(next,"confirm");
        assertEquals(1,intake.batch(preview(contract,"EXT,2026-10-20\n","2026-11-30")).getErrorCount());
    }
    @Test void csvScopedPersonnelAndQueryPermissionAreEnforced() {
        Long id=service.create(draft("OTHER").setEmployeeId(102L));review(id,"confirm");Long contract=contract(true);
        scope(new DeptDataPermissionRespDTO().setDeptIds(new HashSet<>(Collections.singletonList(11L))));assertEquals(1,intake.batch(preview(contract,"OTHER,2026-10-20\n","2026-10-31")).getErrorCount());
        when(permissionApi.hasAnyPermissions(10L,"hrm:payroll:identity:query")).thenReturn(false);assertServiceException(()->preview(contract,"OTHER,2026-10-20\n","2026-10-31"),PAYROLL_MAPPING_PERMISSION);
    }
}
