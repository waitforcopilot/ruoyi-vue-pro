package cn.iocoder.yudao.module.hrm.service.payroll.eligibility;
import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.eligibility.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.eligibility.HrmPayrollEligibilityMapper;
import cn.iocoder.yudao.module.hrm.service.payroll.identity.*;
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
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@Import({HrmPayrollEligibilityServiceImpl.class,HrmPayrollEmployeeMappingServiceImpl.class,HrmPayrollEmployeeAccess.class})
class HrmPayrollEligibilityServiceImplTest extends BaseDbUnitTest {
    @Resource private HrmPayrollEligibilityService service;
    @Resource private HrmPayrollEligibilityMapper mapper;
    @Resource private javax.sql.DataSource dataSource;
    @MockBean private PermissionApi permissionApi;
    @MockBean private AdminUserApi adminUserApi;
    private JdbcTemplate jdbc;
    @BeforeEach void fixture() {
        jdbc=new JdbcTemplate(dataSource);login(1L);when(permissionApi.hasAnyPermissions(10L,"hrm:employee:query")).thenReturn(true);
        scope(new DeptDataPermissionRespDTO().setAll(true));
        jdbc.update("INSERT INTO hrm_employee(id,name,job_number,dept_id,user_id,status,tenant_id) VALUES(101,'合成人员 A','001',11,10,20,1),(102,'合成人员 B','001',22,20,30,1),(999,'其他租户','001',11,10,20,999)");
    }
    private void context(Long tenant) {
        TenantContextHolder.setTenantId(tenant);SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new LoginUser().setId(10L).setTenantId(tenant).setUserType(2),null,Collections.emptyList()));
    }
    private void login(Long tenant) {
        context(tenant);
        when(adminUserApi.getUser(10L)).thenReturn(new AdminUserRespDTO().setId(10L).setNickname("合成评审人"));
    }
    @AfterEach void clear() { TenantContextHolder.clear();SecurityContextHolder.clearContext(); }
    private void scope(DeptDataPermissionRespDTO scope) { when(permissionApi.getDeptDataPermission(10L)).thenReturn(scope); }
    private HrmPayrollEligibilitySaveReqVO draft(String entity) {
        return new HrmPayrollEligibilitySaveReqVO().setEntityCode(entity).setEntityName("合成声明主体").setEmployeeId(101L).setQualification("INCLUDED")
                .setOwnerName("合成负责人").setReference("合成资格依据，非正式制度").setReason("合成纳入理由")
                .setEffectiveFrom(LocalDate.of(2026,10,1)).setEffectiveTo(LocalDate.of(2026,10,31));
    }
    private HrmPayrollEligibilitySaveReqVO edit(Long id) { return BeanUtils.toBean(service.get(id),HrmPayrollEligibilitySaveReqVO.class); }
    private void review(Long id,String action) { service.review(new HrmPayrollEligibilityReviewReqVO().setId(id).setRevision(service.get(id).getRevision()).setAction(action).setEvidence("合成核对依据")); }
    private Long confirmed(String entity) { Long id=service.create(draft(entity));review(id,"confirm");return id; }
    private HrmPayrollEligibilityPageReqVO page() { HrmPayrollEligibilityPageReqVO q=new HrmPayrollEligibilityPageReqVO();q.setPageSize(100);return q; }
    private HrmPayrollEligibilityLookupVO lookup(String entity,String start,String end) { return service.lookup(new HrmPayrollEligibilityLookupReqVO().setEntityCode(entity).setEmployeeId(101L).setStart(LocalDate.parse(start)).setEnd(LocalDate.parse(end))); }
    private void invalid(Runnable work) { assertEquals(PAYROLL_ELIGIBILITY_INVALID.getCode(),assertThrows(ServiceException.class,work::run).getCode()); }

    @Test void serverOwnsIdVersionAndSnapshotAndIdentityDoesNotUseDuplicateJobNumber() {
        Long id=service.create(draft("QA-A").setId(888L).setRevision(99));HrmPayrollEligibilityRespVO row=service.get(id);
        assertNotEquals(888L,id);assertEquals(1,row.getEligibilityVersion());assertEquals(1,row.getRevision());assertEquals("合成人员 A",row.getSnapshotName());assertEquals(11L,row.getSnapshotDeptId());
        assertNotNull(row.getEmployeeFingerprint());assertNotNull(row.getSnapshotCapturedAt());assertServiceException(()->service.create(draft("QA-A")),PAYROLL_ELIGIBILITY_DUPLICATE);
        service.create(draft("QA-B"));service.create(draft("QA-A").setEmployeeId(102L));assertEquals(3,service.page(page()).getTotal());
    }
    @Test void emptyDraftIsAllowedButDoesNotImplyQualification() {
        Long id=service.create(draft("EMPTY").setQualification(null).setOwnerName(null).setReference(null).setReason(null).setEffectiveFrom(null).setEffectiveTo(null));
        assertNull(service.get(id).getQualification());invalid(()->review(id,"confirm"));assertFalse(lookup("EMPTY","2026-10-01","2026-10-31").getMatched());
    }
    @Test void confirmationRequiresDecisionReasonOwnerReferenceAndBoundedPeriod() {
        Long id=service.create(draft("META").setQualification(null));invalid(()->review(id,"confirm"));
        service.update(edit(id).setQualification("INCLUDED").setReason(null));invalid(()->review(id,"confirm"));
        service.update(edit(id).setReason("理由").setOwnerName(null));invalid(()->review(id,"confirm"));
        service.update(edit(id).setOwnerName("负责人").setReference(null));invalid(()->review(id,"confirm"));
        service.update(edit(id).setReference("依据").setEffectiveTo(null));invalid(()->review(id,"confirm"));
        assertEquals(0,service.get(id).getStatus());
    }
    @Test void inclusiveOverlapIncludesExcludedVersionsAndAdjacentIsAllowed() {
        Long id=confirmed("PERIOD"),next=service.newVersion(id,service.get(id).getRevision());
        service.update(edit(next).setQualification("EXCLUDED").setEffectiveFrom(LocalDate.of(2026,10,31)).setEffectiveTo(LocalDate.of(2026,11,30)));
        assertServiceException(()->review(next,"confirm"),PAYROLL_ELIGIBILITY_OVERLAP);
        service.update(edit(next).setEffectiveFrom(LocalDate.of(2026,11,1)));review(next,"confirm");
        assertEquals(id,lookup("PERIOD","2026-10-31","2026-10-31").getEligibility().getId());
        assertEquals("EXCLUDED",lookup("PERIOD","2026-11-01","2026-11-30").getIssueCode());
    }
    @Test void wholePeriodRequiresOneConfirmedVersionAndDoesNotJoinOrProrate() {
        Long id=confirmed("WHOLE"),next=service.newVersion(id,service.get(id).getRevision());service.update(edit(next).setEffectiveFrom(LocalDate.of(2026,11,1)).setEffectiveTo(LocalDate.of(2026,11,30)));review(next,"confirm");
        assertFalse(lookup("WHOLE","2026-10-01","2026-11-30").getMatched());assertEquals("UNRESOLVED_QUALIFICATION",lookup("WHOLE","2026-10-01","2026-11-30").getIssueCode());
        assertFalse(lookup("WHOLE","2026-09-30","2026-10-01").getMatched());
    }
    @Test void exclusionIsExplicitAndDifferentFromMissingQualification() {
        Long id=service.create(draft("EXCLUDE").setEmployeeId(102L).setQualification("EXCLUDED"));review(id,"confirm");
        HrmPayrollEligibilityLookupVO result=service.lookup(new HrmPayrollEligibilityLookupReqVO().setEntityCode("EXCLUDE").setEmployeeId(102L).setStart(LocalDate.of(2026,10,1)).setEnd(LocalDate.of(2026,10,31)));
        assertTrue(result.getMatched());assertEquals("EXCLUDED",result.getIssueCode());assertEquals(30,result.getEligibility().getSnapshotEmployeeStatus());
        assertEquals("UNRESOLVED_QUALIFICATION",lookup("MISSING","2026-10-01","2026-10-31").getIssueCode());
    }
    @Test void currentStatusAndHireLeaveDatesDoNotInferOrRewriteDecision() {
        jdbc.update("UPDATE hrm_employee SET status=30,entry_time='2026-10-15',leave_time='2026-10-16' WHERE id=101");
        Long id=confirmed("DECLARED");assertTrue(lookup("DECLARED","2026-10-01","2026-10-31").getMatched());assertEquals("INCLUDED",service.get(id).getQualification());
    }
    @Test void staleRevisionAndConfirmedOrRetiredEditsAreBlocked() {
        Long id=service.create(draft("EDIT"));HrmPayrollEligibilitySaveReqVO old=edit(id);service.update(edit(id).setEntityName("修改名称"));
        assertServiceException(()->service.update(old),PAYROLL_ELIGIBILITY_STALE);review(id,"confirm");assertServiceException(()->service.update(edit(id)),PAYROLL_ELIGIBILITY_IMMUTABLE);
        review(id,"retire");assertServiceException(()->service.update(edit(id)),PAYROLL_ELIGIBILITY_IMMUTABLE);assertFalse(lookup("EDIT","2026-10-01","2026-10-31").getMatched());
    }
    @Test void editingCannotMoveEmployeeOrEntityIdentity() {
        Long id=service.create(draft("FIXED"));invalid(()->service.update(edit(id).setEmployeeId(102L)));invalid(()->service.update(edit(id).setEntityCode("OTHER")));
    }
    @Test void sourceDriftBlocksConfirmationAndLookupAndHistoricalSnapshotStaysFrozen() {
        Long id=service.create(draft("DRIFT"));jdbc.update("UPDATE hrm_employee SET name='新的合成姓名' WHERE id=101");
        assertServiceException(()->review(id,"confirm"),PAYROLL_ELIGIBILITY_PERSON_CHANGED);assertServiceException(()->service.update(edit(id)),PAYROLL_ELIGIBILITY_PERSON_CHANGED);
        service.update(edit(id).setEmployeeFingerprint(null));review(id,"confirm");
        jdbc.update("UPDATE hrm_employee SET status=30 WHERE id=101");HrmPayrollEligibilityLookupVO result=lookup("DRIFT","2026-10-01","2026-10-31");
        assertFalse(result.getMatched());assertTrue(result.getPersonChanged());assertEquals("PERSON_CHANGED",result.getIssueCode());assertEquals(20,result.getEligibility().getSnapshotEmployeeStatus());
    }
    @Test void pickerFingerprintPreventsRaceBeforeCreate() {
        String fingerprint=service.employee(101L).getEmployeeFingerprint();jdbc.update("UPDATE hrm_employee SET user_id=20 WHERE id=101");
        assertServiceException(()->service.create(draft("RACE").setEmployeeFingerprint(fingerprint)),PAYROLL_ELIGIBILITY_PERSON_CHANGED);assertEquals(0,mapper.selectCount());
    }
    @Test void nullableFieldsAndSnapshotValuesReallyClearWhenMasterChanges() {
        jdbc.update("UPDATE hrm_employee SET entry_time='2026-10-01',leave_time='2026-10-10' WHERE id=101");Long id=service.create(draft("NULLS"));
        jdbc.update("UPDATE hrm_employee SET job_number=NULL,dept_id=NULL,user_id=NULL,entry_time=NULL,leave_time=NULL,status=NULL WHERE id=101");
        service.update(edit(id).setEmployeeFingerprint(null).setQualification(null).setOwnerName(null).setReference(null).setReason(null).setEffectiveFrom(null).setEffectiveTo(null));
        HrmPayrollEligibilityRespVO row=service.get(id);assertNull(row.getSnapshotJobNumber());assertNull(row.getSnapshotDeptId());assertNull(row.getSnapshotUserId());assertNull(row.getSnapshotEntryTime());assertNull(row.getSnapshotLeaveTime());assertNull(row.getSnapshotEmployeeStatus());assertNull(row.getQualification());assertNull(row.getReason());assertNull(row.getReference());assertNull(row.getEffectiveTo());
    }
    @Test void newVersionRefreshesPersonnelWithoutInheritingReviewConclusion() {
        Long id=confirmed("NEW");jdbc.update("UPDATE hrm_employee SET name='新姓名' WHERE id=101");Long next=service.newVersion(id,service.get(id).getRevision());
        HrmPayrollEligibilityRespVO row=service.get(next);assertEquals(2,row.getEligibilityVersion());assertEquals(1,row.getRevision());assertEquals(0,row.getStatus());assertEquals("新姓名",row.getSnapshotName());assertNull(row.getEvidence());assertNull(row.getReviewedByName());assertNull(row.getReviewedTime());assertEquals("合成人员 A",service.get(id).getSnapshotName());
        assertServiceException(()->service.newVersion(id,1),PAYROLL_ELIGIBILITY_STALE);
    }
    @Test void tenantIsolationCoversPageGetHistoryEmployeeAndMutation() {
        Long id=confirmed("TENANT");assertServiceException(()->service.employee(999L),PAYROLL_ELIGIBILITY_NOT_EXISTS);login(999L);
        assertEquals(0,service.page(page()).getTotal());assertServiceException(()->service.get(id),PAYROLL_ELIGIBILITY_NOT_EXISTS);assertServiceException(()->service.history(id),PAYROLL_ELIGIBILITY_NOT_EXISTS);assertServiceException(()->service.newVersion(id,2),PAYROLL_ELIGIBILITY_NOT_EXISTS);
        service.create(draft("TENANT").setEmployeeId(999L));assertEquals(1,service.page(page()).getTotal());
    }
    @Test void departmentsRequireBothCapturedAndCurrentOrganization() {
        Long id=confirmed("DEPT");scope(new DeptDataPermissionRespDTO().setDeptIds(new HashSet<>(Collections.singletonList(11L))));assertEquals(1,service.page(page()).getTotal());
        jdbc.update("UPDATE hrm_employee SET dept_id=22 WHERE id=101");assertEquals(0,service.page(page()).getTotal());assertServiceException(()->service.get(id),PAYROLL_ELIGIBILITY_NOT_EXISTS);
        scope(new DeptDataPermissionRespDTO().setDeptIds(new HashSet<>(Collections.singletonList(22L))));assertEquals(0,service.page(page()).getTotal());assertServiceException(()->service.get(id),PAYROLL_ELIGIBILITY_NOT_EXISTS);
    }
    @Test void selfScopeRequiresBothSnapshotAndCurrentUserAndEmptyScopeIsDenied() {
        Long id=confirmed("SELF");scope(new DeptDataPermissionRespDTO().setSelf(true));assertEquals(1,service.page(page()).getTotal());
        jdbc.update("UPDATE hrm_employee SET user_id=20 WHERE id=101");assertEquals(0,service.page(page()).getTotal());assertServiceException(()->service.get(id),PAYROLL_ELIGIBILITY_NOT_EXISTS);
        scope(new DeptDataPermissionRespDTO());assertEquals(0,service.page(page()).getTotal());
    }
    @Test void scopedHistoryRedactsSnapshotsAndFreeformReason() {
        Long id=confirmed("HISTORY");assertTrue(service.history(id).get(0).getAfterSnapshot().contains("合成人员 A"));
        scope(new DeptDataPermissionRespDTO().setSelf(true));assertNull(service.history(id).get(0).getAfterSnapshot());assertNull(service.history(id).get(0).getBeforeSnapshot());assertNull(service.history(id).get(0).getReason());
    }
    @Test void allScopeHistoricalEvidenceSurvivesDeletedPersonButLookupIsUnavailable() {
        Long id=confirmed("DELETED");jdbc.update("UPDATE hrm_employee SET deleted=TRUE WHERE id=101");assertEquals("合成人员 A",service.get(id).getSnapshotName());assertEquals(2,service.history(id).size());
        assertEquals("PERSON_UNAVAILABLE",lookup("DELETED","2026-10-01","2026-10-31").getIssueCode());scope(new DeptDataPermissionRespDTO().setSelf(true));assertEquals(0,service.page(page()).getTotal());assertServiceException(()->service.get(id),PAYROLL_ELIGIBILITY_NOT_EXISTS);
    }
    @Test void missingHrmPermissionCannotReadSnapshotsOrUseLookup() {
        Long id=confirmed("NO-PERM");when(permissionApi.hasAnyPermissions(10L,"hrm:employee:query")).thenReturn(false);
        assertServiceException(()->service.get(id),PAYROLL_MAPPING_PERMISSION);assertServiceException(()->service.page(page()),PAYROLL_MAPPING_PERMISSION);assertServiceException(()->lookup("NO-PERM","2026-10-01","2026-10-31"),PAYROLL_MAPPING_PERMISSION);
    }
    @Test void invalidDecisionDateIdentityAndReviewDoNotWriteRows() {
        invalid(()->service.create(draft("bad lower")));invalid(()->service.create(draft("BAD").setQualification("AUTO")));invalid(()->service.create(draft("BAD").setEntityName(" ")));
        invalid(()->service.create(draft("BAD").setEffectiveFrom(LocalDate.of(999,1,1))));invalid(()->service.create(draft("BAD").setEffectiveTo(LocalDate.of(2026,9,1))));invalid(()->service.create(draft("BAD").setReason("nul\u0000")));assertEquals(0,mapper.selectCount());
        invalid(()->service.lookup(new HrmPayrollEligibilityLookupReqVO().setEntityCode("BAD").setEmployeeId(101L).setStart(LocalDate.of(2026,11,1)).setEnd(LocalDate.of(2026,10,1))));
    }
    @Test void auditActorAndRevisionsAreServerOwnedAndRetirementKeepsBeforeAfter() {
        Long id=confirmed("AUDIT");review(id,"retire");assertEquals(3,service.history(id).size());assertEquals(10L,service.history(id).get(0).getActorId());assertEquals("合成评审人",service.history(id).get(0).getActorName());assertEquals(2,service.history(id).get(0).getFromVersion());assertEquals(3,service.history(id).get(0).getToVersion());assertTrue(service.history(id).get(0).getBeforeSnapshot().contains("\"status\":1"));assertTrue(service.history(id).get(0).getAfterSnapshot().contains("\"status\":2"));
    }
    @Test void pageFiltersAndSourceWhitelistCannotExposeOtherPeople() {
        service.create(draft("QA"));service.create(draft("QB").setEmployeeId(102L).setQualification("EXCLUDED"));HrmPayrollEligibilityPageReqVO q=page();q.setEntityCode("QB");q.setSearch("人员 B");q.setQualification("EXCLUDED");assertEquals(1,service.page(q).getTotal());
        q.setEmployeeId(101L);assertEquals(0,service.page(q).getTotal());String json=JsonUtils.toJsonString(service.employee(101L));assertFalse(json.contains("mobile"));assertFalse(json.contains("idNumber"));assertFalse(json.contains("salary"));
    }
    @Test void httpDatesAreIsoStringsDespiteGlobalTimestampSerializer() throws Exception {
        Long id=confirmed("WIRE");org.springframework.http.converter.json.Jackson2ObjectMapperBuilder builder=new org.springframework.http.converter.json.Jackson2ObjectMapperBuilder();
        new cn.iocoder.yudao.framework.jackson.config.YudaoJacksonAutoConfiguration().ldtEpochMillisCustomizer().customize(builder);com.fasterxml.jackson.databind.ObjectMapper http=builder.build();
        com.fasterxml.jackson.databind.JsonNode wire=http.readTree(http.writeValueAsString(service.get(id)));assertEquals("2026-10-01",wire.get("effectiveFrom").asText());assertTrue(wire.get("snapshotCapturedAt").asText().matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}"));assertTrue(wire.get("reviewedTime").isTextual());
    }
    @Test void concurrentNewVersionsAreSerializedAndConcurrentReviewsCannotOverlap() throws Exception {
        Long id=confirmed("CONCURRENT");ExecutorService pool=Executors.newFixedThreadPool(2);
        try {
            Callable<Long> version=()->{context(1L);try{return service.newVersion(id,2);}finally{clear();}};
            List<Future<Long>> results=pool.invokeAll(Arrays.asList(version,version));Set<Integer> versions=new HashSet<>();for(Future<Long> result:results)versions.add(service.get(result.get(10,TimeUnit.SECONDS)).getEligibilityVersion());assertEquals(new HashSet<>(Arrays.asList(2,3)),versions);
            List<HrmPayrollEligibilityRespVO> rows=service.page(page()).getList();List<Long> drafts=new ArrayList<>();for(HrmPayrollEligibilityRespVO row:rows)if(row.getStatus()==0) { service.update(edit(row.getId()).setEffectiveFrom(LocalDate.of(2026,11,1)).setEffectiveTo(LocalDate.of(2026,11,30)));drafts.add(row.getId()); }
            List<Callable<Boolean>> reviews=new ArrayList<>();for(Long next:drafts)reviews.add(()->{context(1L);try{review(next,"confirm");return true;}catch(ServiceException e){assertEquals(PAYROLL_ELIGIBILITY_OVERLAP.getCode(),e.getCode());return false;}finally{clear();}});
            int success=0;for(Future<Boolean> result:pool.invokeAll(reviews))if(result.get(10,TimeUnit.SECONDS))success++;assertEquals(1,success);
        } finally {pool.shutdownNow();}
    }
}
