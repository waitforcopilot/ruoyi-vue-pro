package cn.iocoder.yudao.module.hrm.service.payroll.calculation;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.calculation.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.HrmPayrollReviewMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.calculation.HrmPayrollCalculationMapper;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.*;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
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
import static cn.iocoder.yudao.module.hrm.service.payroll.calculation.HrmPayrollCalculationEngineTest.*;

@Import({HrmPayrollCalculationServiceImpl.class, HrmPayrollCalculationEngine.class, ValidationAutoConfiguration.class})
class HrmPayrollCalculationServiceImplTest extends BaseDbUnitTest {
    @Resource private HrmPayrollCalculationService service;
    @Resource private HrmPayrollCalculationMapper mapper;
    @Resource private HrmPayrollReviewMapper reviews;
    @MockBean private AdminUserApi adminUserApi;
    private final LocalDate start = LocalDate.of(2026, 10, 1), end = LocalDate.of(2026, 10, 31);
    @BeforeEach void before() {
        login(1L); when(adminUserApi.getUser(10L)).thenReturn(new AdminUserRespDTO().setId(10L).setNickname("合成规则评审人"));
    }
    private void login(Long tenant) {
        TenantContextHolder.setTenantId(tenant);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new LoginUser().setId(10L).setUserType(2).setTenantId(tenant), null, Collections.emptyList()));
    }
    @AfterEach void after() { TenantContextHolder.clear(); SecurityContextHolder.clearContext(); }
    private HrmPayrollCalculationSaveReqVO draft() {
        return new HrmPayrollCalculationSaveReqVO().setCode("SYNTHETIC-CALC").setTitle("合成规则执行样例").setScopeCode("SYNTHETIC")
                .setApplicableScope("合成技术测试范围").setOwnerName("QA 合成负责人").setReference("合成数学样例，不代表真实工资制度")
                .setEffectiveFrom(start).setEffectiveTo(end).setProgram(sample());
    }
    private HrmPayrollCalculationSaveReqVO edit(Long id) { return BeanUtils.toBean(service.get(id), HrmPayrollCalculationSaveReqVO.class); }
    private void review(Long id, String action) {
        service.review(new HrmPayrollCalculationReviewReqVO().setId(id).setRevision(service.get(id).getRevision()).setAction(action).setEvidence("隔离合成回归评审"));
    }
    private Long confirmed(HrmPayrollCalculationSaveReqVO req) { Long id = service.create(req); review(id, "confirm"); return id; }
    private HrmPayrollCalculationPreviewReqVO preview(Long id) {
        return new HrmPayrollCalculationPreviewReqVO().setDefinitionId(id).setStart(start).setEnd(end).setInputs(values("base", "1000.00", "days", "1", "cycleDays", "3"));
    }
    @Test void createOwnsIdentityRevisionStatusAndStoredVerification() {
        Long id = service.create(draft().setId(999L).setRevision(99)); HrmPayrollCalculationRespVO row = service.get(id);
        assertNotEquals(999L, id); assertEquals(1, row.getDefinitionVersion()); assertEquals(1, row.getRevision()); assertEquals(0, row.getStatus());
        assertEquals(3, row.getInputCount()); assertEquals(3, row.getItemCount()); assertEquals(1, row.getCaseCount()); assertTrue(row.getVerifiedCases().getAllPassed());
        assertNull(row.getReviewedBy()); assertEquals(1, reviews.selectCount());
    }
    @Test void invalidGraphsAndMissingPrecisionDoNotWriteRowsOrAudit() {
        HrmPayrollCalculationSaveReqVO req = draft(); req.getProgram().getItems().get(0).setExpression("unknown + 1");
        assertThrows(ServiceException.class, () -> service.create(req));
        req.getProgram().getItems().get(0).setExpression("prorated - deduction").setRoundingMode(null);
        assertThrows(ServiceException.class, () -> service.create(req)); assertEquals(0, mapper.selectCount()); assertEquals(0, reviews.selectCount());
        assertThrows(ServiceException.class, () -> service.create(draft().setProgram(null)));
    }
    @Test void nullListElementsCannotCreateDefinitionsOrAudit() {
        HrmPayrollCalculationSaveReqVO nullInput = draft(); nullInput.getProgram().setInputs(Collections.singletonList(null));
        HrmPayrollCalculationSaveReqVO nullItem = draft(); nullItem.getProgram().setItems(Collections.singletonList(null));
        HrmPayrollCalculationSaveReqVO nullCase = draft(); nullCase.getProgram().setCases(Collections.singletonList(null));
        for (HrmPayrollCalculationSaveReqVO req : Arrays.asList(nullInput, nullItem, nullCase)) {
            assertEquals(PAYROLL_CALCULATION_INVALID.getCode(), assertThrows(ServiceException.class, () -> service.create(req)).getCode());
        }
        assertEquals(0, mapper.selectCount()); assertEquals(0, reviews.selectCount());
    }
    @Test void missingWrongAndInvalidExpectationsBlockConfirmationUntilFixed() {
        HrmPayrollCalculationSaveReqVO req = draft(); req.getProgram().setCases(Collections.emptyList()); Long id = service.create(req);
        assertServiceException(() -> review(id, "confirm"), PAYROLL_CALCULATION_CASES_FAILED);
        req = edit(id).setProgram(sample()); req.getProgram().getCases().get(0).getExpected().put("net", "wrong"); service.update(req);
        assertEquals(0, service.cases(id).getPassed()); assertFalse(service.get(id).getVerifiedCases().getAllPassed());
        assertServiceException(() -> review(id, "confirm"), PAYROLL_CALCULATION_CASES_FAILED);
        req = edit(id).setProgram(sample()); service.update(req); review(id, "confirm"); assertEquals(1, service.get(id).getStatus());
    }
    @Test void confirmationRequiresOwnerScopeReferenceAndDate() {
        Long id = service.create(draft().setOwnerName(null)); assertThrows(ServiceException.class, () -> review(id, "confirm"));
        service.update(edit(id).setOwnerName("合成负责人").setApplicableScope(null)); assertThrows(ServiceException.class, () -> review(id, "confirm"));
        service.update(edit(id).setApplicableScope("合成范围").setReference(null)); assertThrows(ServiceException.class, () -> review(id, "confirm"));
        service.update(edit(id).setReference("合成依据").setEffectiveFrom(null).setEffectiveTo(null)); assertThrows(ServiceException.class, () -> review(id, "confirm"));
    }
    @Test void updatesClearNullableMetadataAndRejectStaleRevisionOrIdentityChanges() {
        Long id = service.create(draft()); HrmPayrollCalculationSaveReqVO stale = edit(id);
        service.update(edit(id).setOwnerName(null).setReference(null).setApplicableScope(null).setDescription(null).setEffectiveFrom(null).setEffectiveTo(null));
        HrmPayrollCalculationRespVO row = service.get(id); assertNull(row.getOwnerName()); assertNull(row.getReference()); assertNull(row.getEffectiveTo());
        assertServiceException(() -> service.update(stale), PAYROLL_CALCULATION_STALE);
        assertThrows(ServiceException.class, () -> service.update(edit(id).setCode("OTHER")));
        assertThrows(ServiceException.class, () -> service.update(edit(id).setScopeCode("OTHER")));
    }
    @Test void confirmedAndRetiredRowsAreImmutableAndCopiesResetApproval() {
        Long id = confirmed(draft()); String original = JsonUtils.toJsonString(service.get(id).getProgram());
        assertServiceException(() -> service.update(edit(id)), PAYROLL_CALCULATION_IMMUTABLE);
        Long next = service.newVersion(id, service.get(id).getRevision()); HrmPayrollCalculationRespVO copy = service.get(next);
        assertEquals(2, copy.getDefinitionVersion()); assertEquals(0, copy.getStatus()); assertEquals(1, copy.getRevision());
        assertNull(copy.getReviewedBy()); assertNull(copy.getReviewedTime()); assertNull(copy.getEvidence());
        assertEquals(original, JsonUtils.toJsonString(copy.getProgram()));
        HrmPayrollCalculationSaveReqVO req = edit(next); req.getProgram().getItems().get(0).setExpression("0"); service.update(req);
        assertEquals(original, JsonUtils.toJsonString(service.get(id).getProgram()));
        review(id, "retire"); assertServiceException(() -> service.update(edit(id)), PAYROLL_CALCULATION_IMMUTABLE);
    }
    @Test void previewRequiresOneConfirmedVersionCoveringTheFullPeriod() {
        Long id = service.create(draft()); assertThrows(ServiceException.class, () -> service.preview(preview(id))); review(id, "confirm");
        HrmPayrollCalculationPreviewVO result = service.preview(preview(id)); assertEquals("333.33", result.getResult().getItems().get(2).getAmount());
        assertEquals(service.cases(id).getProgramHash(), result.getProgramHash());
        assertThrows(ServiceException.class, () -> service.preview(preview(id).setStart(start.minusDays(1))));
        assertThrows(ServiceException.class, () -> service.preview(preview(id).setEnd(end.plusDays(1))));
        assertThrows(ServiceException.class, () -> service.preview(preview(id).setEnd(start.minusDays(1))));
        review(id, "retire"); assertThrows(ServiceException.class, () -> service.preview(preview(id)));
    }
    @Test void missingInputsAndZeroDenominatorsFailWithoutZeroSubstitution() {
        Long id = confirmed(draft());
        assertThrows(ServiceException.class, () -> service.preview(preview(id).setInputs(values("base", "1000", "days", "1"))));
        assertTrue(assertThrows(ServiceException.class, () -> service.preview(preview(id).setInputs(values("base", "1000", "days", "1", "cycleDays", "0")))).getMessage().contains("除零"));
        assertEquals("0.00", service.preview(preview(id).setInputs(values("base", "0", "days", "0", "cycleDays", "3"))).getResult().getItems().get(2).getAmount());
    }
    @Test void inclusiveIntervalsRejectOverlapAllowAdjacencyAndNeverStitch() {
        Long first = confirmed(draft().setEffectiveTo(start.plusDays(14))), second = service.newVersion(first, service.get(first).getRevision());
        service.update(edit(second).setEffectiveFrom(start.plusDays(15)).setEffectiveTo(end)); review(second, "confirm");
        Long overlapping = service.newVersion(second, service.get(second).getRevision()); service.update(edit(overlapping).setEffectiveFrom(start.plusDays(14)));
        assertServiceException(() -> review(overlapping, "confirm"), PAYROLL_CALCULATION_OVERLAP);
        assertThrows(ServiceException.class, () -> service.preview(preview(first))); assertThrows(ServiceException.class, () -> service.preview(preview(second)));
    }
    @Test void duplicateSeriesAndCrossSeriesComparisonAreRejected() {
        Long id = service.create(draft()); assertServiceException(() -> service.create(draft()), PAYROLL_CALCULATION_DUPLICATE);
        Long other = service.create(draft().setCode("OTHER")); assertThrows(ServiceException.class, () -> service.compare(id, other));
    }
    @Test void tenantIsolationCoversGetPageHistoryExecutionAndMutations() {
        Long id = confirmed(draft()); login(2L);
        assertEquals(0L, service.page(new HrmPayrollCalculationPageReqVO()).getTotal());
        assertServiceException(() -> service.get(id), PAYROLL_CALCULATION_NOT_EXISTS);
        assertServiceException(() -> service.cases(id), PAYROLL_CALCULATION_NOT_EXISTS);
        assertServiceException(() -> service.history(id), PAYROLL_CALCULATION_NOT_EXISTS);
        assertServiceException(() -> service.preview(preview(id)), PAYROLL_CALCULATION_NOT_EXISTS);
        assertServiceException(() -> service.newVersion(id, 2), PAYROLL_CALCULATION_NOT_EXISTS);
        Long sameCode = service.create(draft()); assertNotEquals(id, sameCode);
    }
    @Test void pageOmitsProgramAndEvidenceButDetailsAndAuditRetainThem() {
        Long id = confirmed(draft()); HrmPayrollCalculationRespVO summary = service.page(new HrmPayrollCalculationPageReqVO()).getList().get(0);
        assertNull(summary.getProgram()); assertNull(summary.getVerifiedCases()); assertNull(summary.getReference()); assertNull(summary.getEvidence());
        assertEquals(2, service.history(id).size()); assertEquals("confirm", service.history(id).get(0).getAction());
        assertTrue(service.history(id).get(0).getAfterSnapshot().contains("programJson")); assertNotNull(service.get(id).getProgram());
    }
    @Test void comparisonKeepsZeroPrecisionAndMissingMetadataDistinct() {
        Long first = service.create(draft()), next = service.newVersion(first, 1); HrmPayrollCalculationSaveReqVO req = edit(next);
        req.setOwnerName(null); req.getProgram().getItems().get(0).setAmountScale(0); service.update(req);
        HrmPayrollCalculationCompareVO result = service.compare(first, next);
        assertTrue(result.getChanges().stream().anyMatch(c -> "ownerName".equals(c.getPath()) && c.getRight() == null));
        assertTrue(result.getChanges().stream().anyMatch(c -> "program".equals(c.getPath()) && c.getRight().contains("\"amountScale\":0")));
    }
    @Test void unsupportedSchemaCannotExecuteAndOldRevisionCannotReview() {
        Long id = service.create(draft()); service.update(edit(id).setDescription("更新说明"));
        assertServiceException(() -> service.review(new HrmPayrollCalculationReviewReqVO().setId(id).setRevision(1).setAction("confirm").setEvidence("合成")), PAYROLL_CALCULATION_STALE);
        review(id, "confirm"); mapper.updateById(mapper.selectById(id).setSchemaVersion(999));
        assertTrue(assertThrows(ServiceException.class, () -> service.preview(preview(id))).getMessage().contains("结构版本"));
        assertTrue(assertThrows(ServiceException.class, () -> service.cases(id)).getMessage().contains("结构版本"));
    }
    @Test void concurrentVersionAllocationAndConfirmationSerializeOnTheFirstVersion() throws Exception {
        Long id = service.create(draft()); ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Callable<Long> copy = () -> { login(1L); try { return service.newVersion(id, 1); } finally { after(); } };
            Future<Long> one = pool.submit(copy), two = pool.submit(copy); Long a = one.get(15, TimeUnit.SECONDS), b = two.get(15, TimeUnit.SECONDS);
            assertNotEquals(a, b); assertEquals(new HashSet<>(Arrays.asList(2, 3)), new HashSet<>(Arrays.asList(service.get(a).getDefinitionVersion(), service.get(b).getDefinitionVersion())));
            Callable<Boolean> approveA = () -> concurrentConfirm(a), approveB = () -> concurrentConfirm(b);
            Future<Boolean> first = pool.submit(approveA), second = pool.submit(approveB);
            assertNotEquals(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS));
        } finally { pool.shutdownNow(); }
    }
    private boolean concurrentConfirm(Long id) {
        login(1L);
        try { review(id, "confirm"); return true; }
        catch (ServiceException error) { assertEquals(PAYROLL_CALCULATION_OVERLAP.getCode(), error.getCode()); return false; }
        finally { after(); }
    }
}
