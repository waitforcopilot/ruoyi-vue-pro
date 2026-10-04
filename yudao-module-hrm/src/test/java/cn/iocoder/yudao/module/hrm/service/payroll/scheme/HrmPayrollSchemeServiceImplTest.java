package cn.iocoder.yudao.module.hrm.service.payroll.scheme;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.scheme.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.scheme.HrmPayrollSchemeMapper;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.*;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import javax.annotation.Resource;
import java.time.*;
import java.util.*;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
@Import({HrmPayrollSchemeServiceImpl.class,ValidationAutoConfiguration.class})
class HrmPayrollSchemeServiceImplTest extends BaseDbUnitTest {
    @Resource private HrmPayrollSchemeService service;
    @Resource private HrmPayrollSchemeMapper mapper;
    @Resource private javax.sql.DataSource dataSource;
    @MockBean private PermissionApi permissionApi;
    @MockBean private AdminUserApi adminUserApi;
    private JdbcTemplate jdbc;
    @BeforeEach void fixture() {
        jdbc=new JdbcTemplate(dataSource);login(1L);
        for(String p:Arrays.asList("hrm:salary:group:query","hrm:salary:option:query","hrm:salary:tax-rule:query"))when(permissionApi.hasAnyPermissions(10L,p)).thenReturn(true);
        when(adminUserApi.getUser(10L)).thenReturn(new AdminUserRespDTO().setId(10L).setNickname("合成评审人"));
        jdbc.update("INSERT INTO hrm_salary_tax_rule(id,name,type,tax_enabled,tenant_id) VALUES(11,'不计税合成规则',3,FALSE,1),(22,'另一租户税务配置',3,FALSE,999)");
        jdbc.update("INSERT INTO hrm_salary_group(id,name,salary_standard,change_rule,tax_rule_id,employee_ids,dept_ids,tenant_id) VALUES(1,'方案合成组',10,'合成变更规则',11,'[987654321]','[987654322]',1),(2,'另一租户组',20,'另一租户规则',22,'[]','[]',999)");
        jdbc.update("INSERT INTO hrm_salary_option(id,code,parent_code,name,type,enabled,visible,tax_enabled,calculate_enabled,tenant_id) VALUES(101,10,0,'合成加项目录',2,TRUE,TRUE,FALSE,FALSE,1),(102,1000000001,10,'合成输入项',1,TRUE,TRUE,FALSE,TRUE,1),(201,10,0,'另一租户目录',2,TRUE,TRUE,FALSE,FALSE,999)");
    }
    private void login(Long tenant) { TenantContextHolder.setTenantId(tenant);SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new LoginUser().setId(10L).setTenantId(tenant).setUserType(2),null,Collections.emptyList())); }
    @AfterEach void clear() { TenantContextHolder.clear();SecurityContextHolder.clearContext(); }
    private HrmPayrollSchemeSaveReqVO draft() { return new HrmPayrollSchemeSaveReqVO().setGroupId(1L).setTitle("合成方案配置").setOwnerName("合成负责人").setReference("隔离配置依据").setEffectiveFrom(LocalDate.of(2026,10,1)).setEffectiveTo(LocalDate.of(2026,10,31)); }
    private HrmPayrollSchemeSaveReqVO edit(Long id) {
        HrmPayrollSchemeRespVO row=service.get(id);return new HrmPayrollSchemeSaveReqVO().setId(id).setRevision(row.getRevision()).setGroupId(row.getGroupId()).setTitle(row.getTitle())
                .setOwnerName(row.getOwnerName()).setReference(row.getReference()).setEffectiveFrom(row.getEffectiveFrom()).setEffectiveTo(row.getEffectiveTo());
    }
    private void review(Long id,String action) { service.review(new HrmPayrollSchemeReviewReqVO().setId(id).setRevision(service.get(id).getRevision()).setAction(action).setEvidence("合成技术回归评审")); }
    private Long confirmed() { Long id=service.create(draft());review(id,"confirm");return id; }
    @Test void captureIsReadOnlyMinimalAndStableWithExplicitMissingAndFalse() {
        HrmPayrollSchemeRespVO a=service.capture(1L),b=service.capture(1L);assertEquals(a.getSourceHash(),b.getSourceHash());assertEquals(64,a.getSourceHash().length());assertEquals(2,a.getOptionCount());
        assertEquals("10.00",a.getSnapshot().getGroup().getSalaryStandard());assertEquals(Boolean.FALSE,a.getSnapshot().getTaxRule().getTaxEnabled());assertNull(a.getSnapshot().getTaxRule().getThreshold());
        assertTrue(a.getSnapshot().getIssues().isEmpty());assertEquals(0,mapper.selectCount());String json=JsonUtils.toJsonString(a);
        assertFalse(json.contains("employeeIds"));assertFalse(json.contains("deptIds"));assertFalse(json.contains("987654321"));assertFalse(json.contains("probationSalary"));
    }
    @Test void captureDoesNotResolveForeignTenantSourceOrForeignTaxReference() {
        assertServiceException(()->service.capture(2L),PAYROLL_SCHEME_INVALID,"源薪资组不存在或不可访问");jdbc.update("UPDATE hrm_salary_group SET tax_rule_id=22 WHERE id=1");
        HrmPayrollSchemeRespVO row=service.capture(1L);assertNull(row.getSnapshot().getTaxRule());assertFalse(JsonUtils.toJsonString(row).contains("另一租户"));assertFalse(row.getSnapshot().getIssues().isEmpty());
    }
    @Test void sourceFingerprintIsIndependentOfLockingReadOrder() {
        jdbc.update("INSERT INTO hrm_salary_option(id,code,parent_code,name,type,tenant_id) VALUES(99,1000000002,10,'另一个合成项',1,1)");
        HrmPayrollSchemeRespVO candidate=service.capture(1L);Long id=service.create(draft().setExpectedSourceHash(candidate.getSourceHash()));
        assertEquals(candidate.getSourceHash(),service.get(id).getSourceHash());
        assertEquals(Arrays.asList(10,1000000001,1000000002),service.get(id).getSnapshot().getOptions().stream().map(HrmPayrollSchemeSnapshotVO.Option::getCode).collect(java.util.stream.Collectors.toList()));
    }
    @Test void eachLegacyConfigurationPermissionIsRequiredEvenForSavedVersions() {
        Long id=service.create(draft());when(permissionApi.hasAnyPermissions(10L,"hrm:salary:option:query")).thenReturn(false);
        assertServiceException(()->service.get(id),PAYROLL_SCHEME_PERMISSION);assertServiceException(()->service.history(id),PAYROLL_SCHEME_PERMISSION);
        assertServiceException(()->service.compare(id,id),PAYROLL_SCHEME_PERMISSION);assertServiceException(()->service.create(draft()),PAYROLL_SCHEME_PERMISSION);
    }
    @Test void serverAllocatesInitialVersionAndChecksSourcePickerHash() {
        Long id=service.create(draft().setId(123L).setRevision(98).setExpectedSourceHash(service.capture(1L).getSourceHash()));assertNotEquals(123L,id);
        HrmPayrollSchemeRespVO row=service.get(id);assertEquals(1,row.getSchemeVersion());assertEquals(1,row.getRevision());assertEquals(0,row.getStatus());assertEquals(1L,row.getGroupId());
        assertServiceException(()->service.create(draft()),PAYROLL_SCHEME_DUPLICATE);
        jdbc.update("INSERT INTO hrm_salary_group(id,name,salary_standard,change_rule,tax_rule_id,tenant_id) VALUES(3,'新组',10,'合成规则',11,1)");
        assertServiceException(()->service.create(draft().setGroupId(3L).setExpectedSourceHash("old")),PAYROLL_SCHEME_SOURCE_CHANGED);assertEquals(1,mapper.selectCount());
    }
    @Test void draftRefreshClearsNullableMetadataAndRejectsStaleIdentityChanges() {
        Long id=service.create(draft());HrmPayrollSchemeSaveReqVO old=edit(id);service.update(edit(id).setOwnerName(null).setReference(null).setEffectiveFrom(null).setEffectiveTo(null));
        HrmPayrollSchemeRespVO row=service.get(id);assertNull(row.getOwnerName());assertNull(row.getReference());assertNull(row.getEffectiveFrom());assertNull(row.getEffectiveTo());
        assertServiceException(()->service.update(old),PAYROLL_SCHEME_STALE);assertServiceException(()->service.update(edit(id).setGroupId(2L)),PAYROLL_SCHEME_INVALID,"源薪资组固定，不能修改");
    }
    private void drift(String sql) {
        Long id=service.create(draft());HrmPayrollSchemeRespVO before=service.get(id);jdbc.update(sql);
        assertServiceException(()->review(id,"confirm"),PAYROLL_SCHEME_SOURCE_CHANGED);assertEquals(before.getSourceHash(),service.get(id).getSourceHash());
        assertServiceException(()->service.update(edit(id).setExpectedSourceHash(before.getSourceHash())),PAYROLL_SCHEME_SOURCE_CHANGED);
        service.update(edit(id));assertNotEquals(before.getSourceHash(),service.get(id).getSourceHash());review(id,"confirm");
    }
    @Test void groupSourceDriftRequiresExplicitRefresh() { drift("UPDATE hrm_salary_group SET salary_standard=12 WHERE id=1"); }
    @Test void taxSourceDriftRequiresExplicitRefresh() { drift("UPDATE hrm_salary_tax_rule SET name='已核对合成规则' WHERE id=11"); }
    @Test void catalogueSourceDriftRequiresExplicitRefresh() { drift("UPDATE hrm_salary_option SET tax_enabled=TRUE WHERE id=102"); }
    @Test void confirmationRequiresMetadataEvidenceAndValidExistingConfiguration() {
        Long id=service.create(draft().setOwnerName(null));assertServiceException(()->review(id,"confirm"),PAYROLL_SCHEME_INVALID,"确认必须填写负责人");
        service.update(edit(id).setOwnerName("负责人").setReference(null));assertServiceException(()->review(id,"confirm"),PAYROLL_SCHEME_INVALID,"确认必须填写配置依据");
        service.update(edit(id).setReference("依据").setEffectiveFrom(null).setEffectiveTo(null));assertServiceException(()->review(id,"confirm"),PAYROLL_SCHEME_INVALID,"确认必须填写有效期开始日期");
        assertServiceException(()->service.review(new HrmPayrollSchemeReviewReqVO().setId(id).setRevision(service.get(id).getRevision()).setAction("confirm").setEvidence(" ")),PAYROLL_SCHEME_INVALID,"必须填写评审依据");
        service.update(edit(id).setEffectiveFrom(LocalDate.of(2026,10,1)));jdbc.update("UPDATE hrm_salary_group SET salary_standard=0 WHERE id=1");service.update(edit(id));
        assertFalse(service.get(id).getSnapshot().getIssues().isEmpty());assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->review(id,"confirm"));
    }
    @Test void confirmedAndRetiredSnapshotsStayImmutableWhileNewVersionCapturesCurrentConfiguration() {
        Long id=confirmed();String snapshot=JsonUtils.toJsonString(service.get(id).getSnapshot());jdbc.update("UPDATE hrm_salary_option SET name='新目录名称' WHERE id=102");
        assertEquals(snapshot,JsonUtils.toJsonString(service.get(id).getSnapshot()));assertServiceException(()->service.update(edit(id)),PAYROLL_SCHEME_IMMUTABLE);
        Long next=service.newVersion(id,service.get(id).getRevision());assertEquals(2,service.get(next).getSchemeVersion());assertEquals(0,service.get(next).getStatus());assertNull(service.get(next).getReviewedTime());assertNotEquals(snapshot,JsonUtils.toJsonString(service.get(next).getSnapshot()));
        review(id,"retire");assertEquals(2,service.get(id).getStatus());assertEquals(snapshot,JsonUtils.toJsonString(service.get(id).getSnapshot()));
    }
    @Test void effectivePeriodsAreInclusiveAndDoNotStitchVersions() {
        Long id=confirmed(),next=service.newVersion(id,service.get(id).getRevision());assertServiceException(()->review(next,"confirm"),PAYROLL_SCHEME_OVERLAP);
        service.update(edit(next).setEffectiveFrom(LocalDate.of(2026,11,1)).setEffectiveTo(null));review(next,"confirm");
        assertEquals(id,service.resolve(1L,LocalDate.of(2026,10,1),LocalDate.of(2026,10,31)).getId());assertEquals(next,service.resolve(1L,LocalDate.of(2027,1,1),LocalDate.of(2027,1,31)).getId());
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->service.resolve(1L,LocalDate.of(2026,10,31),LocalDate.of(2026,11,1)));
    }
    @Test void auditContainsServerActorAndRetainedConfiguration() {
        Long id=confirmed();assertEquals("合成评审人",service.get(id).getReviewedByName());assertTrue(service.history(id).stream().anyMatch(h->"confirm".equals(h.getAction())&&h.getActorId()==10&&h.getBeforeSnapshot()!=null&&h.getAfterSnapshot()!=null));
        jdbc.update("UPDATE hrm_salary_group SET name='当前更名组' WHERE id=1");assertEquals("方案合成组",service.get(id).getGroupName());assertTrue(service.history(id).get(0).getAfterSnapshot().contains("方案合成组"));
    }
    @Test void deletedSourceGroupKeepsHistoryButCannotCreateAnotherCapture() {
        Long id=confirmed();jdbc.update("UPDATE hrm_salary_group SET deleted=TRUE WHERE id=1");assertEquals(id,service.get(id).getId());assertFalse(service.history(id).isEmpty());
        assertServiceException(()->service.newVersion(id,service.get(id).getRevision()),PAYROLL_SCHEME_INVALID,"源薪资组不存在或不可访问");review(id,"retire");assertEquals(2,service.get(id).getStatus());
    }
    @Test void tenantBoundaryCoversVersionIdsCountsHistoryCompareAndPeriodResolution() {
        Long id=confirmed();login(999L);assertEquals(0,service.page(new HrmPayrollSchemePageReqVO()).getTotal());assertServiceException(()->service.get(id),PAYROLL_SCHEME_NOT_EXISTS);
        assertServiceException(()->service.history(id),PAYROLL_SCHEME_NOT_EXISTS);assertServiceException(()->service.newVersion(id,2),PAYROLL_SCHEME_NOT_EXISTS);assertServiceException(()->service.compare(id,id),PAYROLL_SCHEME_NOT_EXISTS);
        HrmPayrollSchemeRespVO own=service.capture(2L);assertEquals("另一租户目录",own.getSnapshot().getOptions().get(0).getName());assertEquals(1,service.groups(null).size());
    }
    @Test void comparisonShowsZeroFalseAndAddedRemovedOptionsWithoutChangingVersions() {
        Long left=confirmed();jdbc.update("UPDATE hrm_salary_group SET salary_standard=0 WHERE id=1");jdbc.update("UPDATE hrm_salary_option SET enabled=FALSE WHERE id=101");jdbc.update("DELETE FROM hrm_salary_option WHERE id=102");
        jdbc.update("INSERT INTO hrm_salary_option(id,code,parent_code,name,type,tenant_id) VALUES(103,1000000002,10,'新增合成项',1,1)");Long right=service.newVersion(left,service.get(left).getRevision());
        HrmPayrollSchemeCompareVO comparison=service.compare(left,right);assertTrue(comparison.getChanges().stream().anyMatch(c->"group.salaryStandard".equals(c.getPath())&&"0.00".equals(c.getRight())));
        assertTrue(comparison.getChanges().stream().anyMatch(c->"options.10.enabled".equals(c.getPath())&&"false".equals(c.getRight())));assertTrue(comparison.getChanges().stream().anyMatch(c->"REMOVED".equals(c.getKind())));assertTrue(comparison.getChanges().stream().anyMatch(c->"ADDED".equals(c.getKind())));
        assertTrue(service.compare(left,left).getChanges().isEmpty());assertEquals(1,service.get(left).getStatus());assertEquals(0,service.get(right).getStatus());
    }
    @Test void existingTaxValidationAndCatalogueHierarchyAreReusedBeforeConfirmation() {
        jdbc.update("UPDATE hrm_salary_tax_rule SET type=1,tax_enabled=TRUE,threshold=NULL,decimal_scale=NULL,cycle_type=NULL WHERE id=11");
        assertTrue(service.capture(1L).getSnapshot().getIssues().stream().anyMatch(s->s.startsWith("计税规则：")));
        jdbc.update("UPDATE hrm_salary_tax_rule SET type=3,tax_enabled=FALSE WHERE id=11");jdbc.update("UPDATE hrm_salary_option SET parent_code=1000000001 WHERE id=101");
        assertTrue(service.capture(1L).getSnapshot().getIssues().stream().anyMatch(s->s.contains("循环")));
        jdbc.update("UPDATE hrm_salary_option SET parent_code=999 WHERE id=101");assertTrue(service.capture(1L).getSnapshot().getIssues().stream().anyMatch(s->s.contains("父级缺失")));
    }
    @Test void snapshotHttpDatesAndListsRemainMinimal() throws Exception {
        Long id=confirmed();org.springframework.http.converter.json.Jackson2ObjectMapperBuilder b=new org.springframework.http.converter.json.Jackson2ObjectMapperBuilder();
        new cn.iocoder.yudao.framework.jackson.config.YudaoJacksonAutoConfiguration().ldtEpochMillisCustomizer().customize(b);com.fasterxml.jackson.databind.ObjectMapper m=b.build();
        com.fasterxml.jackson.databind.JsonNode json=m.readTree(m.writeValueAsString(service.get(id)));assertEquals("2026-10-01",json.get("effectiveFrom").asText());assertTrue(json.get("capturedAt").asText().matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}"));
        assertFalse(json.toString().contains("employeeIds"));HrmPayrollSchemeRespVO summary=service.page(new HrmPayrollSchemePageReqVO()).getList().get(0);assertNull(summary.getSnapshot());assertNull(summary.getReference());assertNull(summary.getEvidence());
    }
    @Test void dateLimitsAndReversedRangesFailWithoutPersistingAudit() {
        assertServiceException(()->service.create(draft().setEffectiveFrom(LocalDate.of(999,1,1))),PAYROLL_SCHEME_INVALID,"日期超过数据库支持范围");
        assertServiceException(()->service.create(draft().setEffectiveFrom(LocalDate.of(2026,11,1))),PAYROLL_SCHEME_INVALID,"有效期结束不能早于开始，且须先填写开始日期");assertEquals(0,mapper.selectCount());
    }
}
