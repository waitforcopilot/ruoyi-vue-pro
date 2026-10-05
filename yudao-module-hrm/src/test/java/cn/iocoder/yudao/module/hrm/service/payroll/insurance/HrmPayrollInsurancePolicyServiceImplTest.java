package cn.iocoder.yudao.module.hrm.service.payroll.insurance;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.insurance.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.HrmPayrollReviewMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.insurance.HrmPayrollInsurancePolicyMapper;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.*;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@Import({HrmPayrollInsurancePolicyServiceImpl.class, ValidationAutoConfiguration.class})
class HrmPayrollInsurancePolicyServiceImplTest extends BaseDbUnitTest {
    @Resource private HrmPayrollInsurancePolicyService service;
    @Resource private HrmPayrollInsurancePolicyMapper mapper;
    @Resource private HrmPayrollReviewMapper reviewMapper;
    @MockBean private AdminUserApi adminUserApi;
    private final LocalDate start = LocalDate.of(2026, 10, 1), end = LocalDate.of(2026, 10, 31);

    @BeforeEach void before() {
        login(1L);
        when(adminUserApi.getUser(10L)).thenReturn(new AdminUserRespDTO().setId(10L).setNickname("合成政策评审人"));
    }
    private void login(Long tenant) {
        TenantContextHolder.setTenantId(tenant);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new LoginUser().setId(10L).setTenantId(tenant).setUserType(2), null, Collections.emptyList()));
    }
    @AfterEach void after() { TenantContextHolder.clear(); SecurityContextHolder.clearContext(); }
    private BigDecimal decimal(String value) { return new BigDecimal(value); }
    private HrmPayrollInsuranceConfigVO values() {
        return new HrmPayrollInsuranceConfigVO().setLowerBase(decimal("0")).setUpperBase(decimal("10000"))
                .setBaseUnit("YUAN_MONTH").setCorporateMode("RATE").setPersonalMode("RATE")
                .setCorporateRatePercent(decimal("16")).setPersonalRatePercent(decimal("8"))
                .setAmountScale(2).setRoundingMode("HALF_UP").setRoundingStage("TOTAL");
    }
    private HrmPayrollInsuranceSaveReqVO draft() {
        return new HrmPayrollInsuranceSaveReqVO().setCityAreaId(330100).setScopeCode("SYNTHETIC")
                .setScopeName("合成技术样例，不代表真实参保人群").setProjectType(1).setTitle("合成缴费政策")
                .setOwnerName("QA 合成负责人").setReference("合成测试出处，不代表官方政策")
                .setSourceUrl("https://example.test/policy").setEffectiveFrom(start).setEffectiveTo(end).setConfig(values());
    }
    private HrmPayrollInsuranceSaveReqVO edit(Long id) {
        return BeanUtils.toBean(service.get(id), HrmPayrollInsuranceSaveReqVO.class);
    }
    private void review(Long id, String action) {
        service.review(new HrmPayrollInsuranceReviewReqVO().setId(id).setRevision(service.get(id).getRevision())
                .setAction(action).setEvidence("隔离合成回归评审"));
    }
    private Long confirmed(HrmPayrollInsuranceSaveReqVO req) { Long id = service.create(req); review(id, "confirm"); return id; }
    private HrmPayrollInsurancePreviewVO preview(Long id, String base) {
        return service.preview(new HrmPayrollInsurancePreviewReqVO().setPolicyId(id).setStart(start).setEnd(end).setBaseAmount(decimal(base)));
    }
    @Test void emptyDraftDoesNotInventPolicyValuesOrApproval() {
        Long id = service.create(draft().setId(998L).setRevision(99).setConfig(null));
        assertNotEquals(998L, id); HrmPayrollInsuranceRespVO row = service.get(id);
        assertEquals(1, row.getPolicyVersion()); assertEquals(1, row.getRevision()); assertEquals(0, row.getStatus());
        assertNull(row.getConfig().getLowerBase()); assertNull(row.getConfig().getPersonalRatePercent());
        assertNull(row.getConfig().getRoundingMode()); assertNull(row.getReviewedBy()); assertEquals(1, row.getConfigSchemaVersion());
        assertThrows(ServiceException.class, () -> review(id, "confirm"));
    }
    @Test void districtAndCityUseTheSamePolicyIdentity() {
        Long id = service.create(draft().setCityAreaId(330102)); assertEquals(330100, service.get(id).getCityAreaId());
        assertEquals("杭州市", service.get(id).getCityName());
        assertServiceException(() -> service.create(draft()), PAYROLL_INSURANCE_POLICY_DUPLICATE);
        assertThrows(ServiceException.class, () -> service.create(draft().setCityAreaId(1)));
    }
    @Test void customProjectsNeedDistinctStableCodesAndStandardProjectsRejectThem() {
        assertThrows(ServiceException.class, () -> service.create(draft().setProjectType(9)));
        Long first = service.create(draft().setProjectType(9).setCustomProjectCode("CUSTOM-A"));
        Long second = service.create(draft().setProjectType(9).setCustomProjectCode("CUSTOM-B")); assertNotEquals(first, second);
        assertEquals("CUSTOM-A", service.get(first).getCustomProjectCode());
        assertThrows(ServiceException.class, () -> service.create(draft().setCustomProjectCode("UNUSED")));
    }
    @Test void malformedNumbersAndContradictoryModesAreRejectedBeforePersistence() {
        assertThrows(ServiceException.class, () -> service.create(draft().setConfig(values().setLowerBase(decimal("1.001")))));
        assertThrows(ServiceException.class, () -> service.create(draft().setConfig(values().setPersonalRatePercent(decimal("100.0001")))));
        assertThrows(ServiceException.class, () -> service.create(draft().setConfig(values().setCorporateRatePercent(decimal("1.00001")))));
        assertThrows(ServiceException.class, () -> service.create(draft().setConfig(values().setUpperBase(decimal("-1")))));
        assertThrows(ServiceException.class, () -> service.create(draft().setConfig(values().setCorporateFixedAmount(decimal("0")))));
        assertEquals(0, mapper.selectCount()); assertEquals(0, reviewMapper.selectCount());
    }
    @Test void missingRateCannotBeConfirmedButExplicitZeroCan() {
        Long id = service.create(draft().setConfig(values().setPersonalRatePercent(null)));
        assertServiceException(() -> review(id, "confirm"), PAYROLL_INSURANCE_POLICY_INVALID, "个人缴费比例缺失，零值须明确填写");
        HrmPayrollInsuranceSaveReqVO req = edit(id); req.getConfig().setPersonalRatePercent(BigDecimal.ZERO); service.update(req); review(id, "confirm");
        assertEquals("0.00", preview(id, "5000").getPersonalAmount());
    }
    @Test void precisionUnitAndRoundingAreRequiredWithoutDefaults() {
        Long id = service.create(draft().setConfig(values().setBaseUnit(null).setAmountScale(null).setRoundingMode(null).setRoundingStage(null)));
        assertThrows(ServiceException.class, () -> review(id, "confirm"));
        HrmPayrollInsuranceSaveReqVO req = edit(id); req.getConfig().setBaseUnit("YUAN_MONTH"); service.update(req);
        assertServiceException(() -> review(id, "confirm"), PAYROLL_INSURANCE_POLICY_INVALID, "金额精度、舍入方式及舍入步骤须明确填写");
    }
    @Test void confirmationRequiresScopeResponsiblePersonOfficialReferenceAndDate() {
        Long id = service.create(draft().setScopeName(null)); assertThrows(ServiceException.class, () -> review(id, "confirm"));
        service.update(edit(id).setScopeName("合成范围").setOwnerName(null)); assertThrows(ServiceException.class, () -> review(id, "confirm"));
        service.update(edit(id).setOwnerName("合成负责人").setReference(null)); assertThrows(ServiceException.class, () -> review(id, "confirm"));
        service.update(edit(id).setReference("合成依据").setEffectiveFrom(null).setEffectiveTo(null)); assertThrows(ServiceException.class, () -> review(id, "confirm"));
    }
    @Test void draftUpdatesClearNullableMetadataAndRejectOldRevisionOrIdentityChange() {
        Long id = service.create(draft()); HrmPayrollInsuranceSaveReqVO stale = edit(id);
        service.update(edit(id).setOwnerName(null).setReference(null).setSourceUrl(null).setScopeName(null).setEffectiveFrom(null).setEffectiveTo(null));
        HrmPayrollInsuranceRespVO row = service.get(id); assertNull(row.getOwnerName()); assertNull(row.getSourceUrl()); assertNull(row.getEffectiveTo());
        assertServiceException(() -> service.update(stale), PAYROLL_INSURANCE_POLICY_STALE);
        assertServiceException(() -> service.update(edit(id).setScopeCode("OTHER")), PAYROLL_INSURANCE_POLICY_INVALID, "城市、范围编号及项目固定，不能修改");
    }
    @Test void sourceLinksAreReferencesOnlyAndCannotEmbedCredentialsOrExecutableSchemes() {
        for (String url : new String[]{"javascript:alert(1)", "file:///tmp/policy", "https://name:secret@example.test/policy", "https://"})
            assertThrows(ServiceException.class, () -> service.create(draft().setSourceUrl(url)));
        Long id = service.create(draft()); assertEquals("https://example.test/policy", service.get(id).getSourceUrl());
    }
    @Test void confirmedVersionsAreImmutableAndCopiesKeepValuesButClearApproval() {
        Long id = confirmed(draft()); String original = JsonUtils.toJsonString(service.get(id).getConfig());
        assertServiceException(() -> service.update(edit(id)), PAYROLL_INSURANCE_POLICY_IMMUTABLE);
        Long next = service.newVersion(id, service.get(id).getRevision()); HrmPayrollInsuranceRespVO copy = service.get(next);
        assertEquals(2, copy.getPolicyVersion()); assertEquals(original, JsonUtils.toJsonString(copy.getConfig()));
        assertEquals(0, copy.getStatus()); assertNull(copy.getReviewedBy()); assertNull(copy.getReviewedTime());
        HrmPayrollInsuranceSaveReqVO req = edit(next); req.getConfig().setPersonalRatePercent(BigDecimal.ZERO); service.update(req);
        assertEquals(original, JsonUtils.toJsonString(service.get(id).getConfig()));
    }
    @Test void periodsAreInclusiveAdjacentAllowedAndNotStitched() {
        Long id = confirmed(draft()), next = service.newVersion(id, service.get(id).getRevision());
        assertServiceException(() -> review(next, "confirm"), PAYROLL_INSURANCE_POLICY_OVERLAP);
        service.update(edit(next).setEffectiveFrom(LocalDate.of(2026, 11, 1)).setEffectiveTo(null)); review(next, "confirm");
        assertEquals(id, service.resolve(next, start, end).getId());
        assertEquals(next, service.resolve(id, LocalDate.of(2027, 1, 1), LocalDate.of(2027, 1, 31)).getId());
        assertThrows(ServiceException.class, () -> service.resolve(id, end, LocalDate.of(2026, 11, 1)));
    }
    @Test void baseBoundsAreCheckedWithoutClampingAndEndpointsAreAccepted() {
        Long id = confirmed(draft().setConfig(values().setLowerBase(decimal("3000")).setUpperBase(decimal("6000"))));
        assertServiceException(() -> preview(id, "2999.99"), PAYROLL_INSURANCE_POLICY_BASE_OUTSIDE);
        assertServiceException(() -> preview(id, "6000.01"), PAYROLL_INSURANCE_POLICY_BASE_OUTSIDE);
        assertEquals("240.00", preview(id, "3000").getPersonalAmount()); assertEquals("480.00", preview(id, "6000").getPersonalAmount());
    }
    @Test void percentagesMeanPercentAndOutputsRemainExactDecimalStrings() {
        Long id = confirmed(draft()); HrmPayrollInsurancePreviewVO row = preview(id, "5000");
        assertEquals("800.00", row.getCorporateAmount()); assertEquals("400.00", row.getPersonalAmount());
        assertTrue(row.getCorporateExpression().contains("16.0000%")); assertTrue(row.getExplanation().contains("不生成月账"));
        assertEquals(0, decimal(row.getCorporateRawAmount()).compareTo(decimal("800")));
    }
    @Test void fixedAmountsRequireExplicitAmountsAndPermitExplicitZero() {
        HrmPayrollInsuranceConfigVO config = values().setCorporateMode("FIXED").setCorporateRatePercent(null)
                .setCorporateFixedAmount(decimal("12.50")).setPersonalMode("FIXED").setPersonalRatePercent(null).setPersonalFixedAmount(BigDecimal.ZERO);
        Long id = confirmed(draft().setConfig(config)); assertEquals("12.50", preview(id, "0").getCorporateAmount()); assertEquals("0.00", preview(id, "0").getPersonalAmount());
        Long missing = service.create(draft().setScopeCode("MISSING-FIXED").setConfig(config.setPersonalFixedAmount(null)));
        assertThrows(ServiceException.class, () -> review(missing, "confirm"));
    }
    @Test void totalAndComponentRoundingCanProduceDifferentDeclaredResults() {
        HrmPayrollInsuranceConfigVO c = values().setCorporateMode("RATE_PLUS_FIXED").setCorporateRatePercent(decimal("1.5"))
                .setCorporateFixedAmount(decimal("0.50")).setAmountScale(0);
        Long total = confirmed(draft().setConfig(c)); assertEquals("2", preview(total, "100").getCorporateAmount());
        assertEquals("比例部分 1.5 + 固定部分 0.5 = 2；合计舍入 → 2", preview(total, "100").getCorporateSteps());
        Long component = confirmed(draft().setScopeCode("COMPONENT").setConfig(c.setRoundingStage("COMPONENT")));
        assertEquals("3", preview(component, "100").getCorporateAmount());
        assertEquals("比例部分 1.5 → 2；固定部分 0.5 → 1；合计 3", preview(component, "100").getCorporateSteps());
        assertEquals(0, decimal(preview(total, "100").getCorporateRawAmount()).compareTo(decimal(preview(component, "100").getCorporateRawAmount())));
    }
    @Test void allDeclaredRoundingModesAreHonored() {
        String[] modes = {"HALF_UP", "HALF_EVEN", "DOWN", "UP"}; String[] expected = {"1", "0", "0", "1"};
        for (int i = 0; i < modes.length; i++) {
            Long id = confirmed(draft().setScopeCode(modes[i]).setConfig(values().setAmountScale(0).setRoundingMode(modes[i]).setCorporateRatePercent(decimal("50"))));
            assertEquals(expected[i], preview(id, "1").getCorporateAmount());
        }
    }
    @Test void fourPlaceRatesAndLargeBasesAvoidBinaryFloatingPoint() {
        Long id = confirmed(draft().setConfig(values().setUpperBase(decimal("9999999999.99")).setCorporateRatePercent(decimal("1.2345")).setAmountScale(4)));
        HrmPayrollInsurancePreviewVO row = preview(id, "9999999999.99");
        assertEquals("123449999.9999", row.getCorporateAmount());
        assertEquals(0, decimal(row.getCorporateRawAmount()).compareTo(decimal("123449999.99987655")));
    }
    @Test void draftRetiredAndWrongPeriodPoliciesCannotPreviewOrCreatePayrollAmounts() {
        Long id = service.create(draft()); assertThrows(ServiceException.class, () -> preview(id, "5000")); review(id, "confirm");
        assertThrows(ServiceException.class, () -> service.preview(new HrmPayrollInsurancePreviewReqVO().setPolicyId(id).setStart(start.minusDays(1)).setEnd(end).setBaseAmount(decimal("5000"))));
        String config = JsonUtils.toJsonString(service.get(id).getConfig()); review(id, "retire");
        assertThrows(ServiceException.class, () -> preview(id, "5000")); assertEquals(config, JsonUtils.toJsonString(service.get(id).getConfig())); assertFalse(service.history(id).isEmpty());
    }
    @Test void savedAuditUsesServerActorAndParameterSnapshot() {
        Long id = confirmed(draft()); HrmPayrollInsuranceRespVO row = service.get(id); assertEquals(10L, row.getReviewedBy()); assertEquals("合成政策评审人", row.getReviewedByName());
        assertTrue(service.history(id).stream().anyMatch(h -> "confirm".equals(h.getAction()) && h.getActorId() == 10L && h.getBeforeSnapshot() != null && h.getAfterSnapshot().contains("configJson")));
    }
    @Test void comparisonSeparatesZeroMissingAndParametersWithoutArtificialMetadataDifferences() {
        Long left = service.create(draft().setConfig(values().setPersonalRatePercent(null))), right = service.newVersion(left, 1);
        HrmPayrollInsuranceSaveReqVO req = edit(right); req.getConfig().setPersonalRatePercent(BigDecimal.ZERO); service.update(req);
        HrmPayrollInsuranceCompareVO comparison = service.compare(left, right);
        assertTrue(comparison.getChanges().stream().anyMatch(c -> "personalRatePercent".equals(c.getPath()) && c.getLeft() == null && "0.0000".equals(c.getRight())), JsonUtils.toJsonString(comparison.getChanges()));
        assertTrue(service.compare(left, left).getChanges().isEmpty());
        Long other = service.create(draft().setScopeCode("OTHER")); assertThrows(ServiceException.class, () -> service.compare(left, other));
    }
    @Test void tenantBoundaryCoversIdsCountsHistoryCopiesResolutionAndPreview() {
        Long id = confirmed(draft()); login(999L); assertEquals(0, service.page(new HrmPayrollInsurancePageReqVO()).getTotal());
        assertServiceException(() -> service.get(id), PAYROLL_INSURANCE_POLICY_NOT_EXISTS);
        assertServiceException(() -> service.history(id), PAYROLL_INSURANCE_POLICY_NOT_EXISTS);
        assertServiceException(() -> service.newVersion(id, 2), PAYROLL_INSURANCE_POLICY_NOT_EXISTS);
        assertServiceException(() -> service.resolve(id, start, end), PAYROLL_INSURANCE_POLICY_NOT_EXISTS);
        assertServiceException(() -> preview(id, "5000"), PAYROLL_INSURANCE_POLICY_NOT_EXISTS);
        Long own = confirmed(draft()); assertNotEquals(id, own);
    }
    @Test void actualHttpSerializerPreservesDatesNumbersAndMinimalList() throws Exception {
        Long id = confirmed(draft()); org.springframework.http.converter.json.Jackson2ObjectMapperBuilder b = new org.springframework.http.converter.json.Jackson2ObjectMapperBuilder();
        new cn.iocoder.yudao.framework.jackson.config.YudaoJacksonAutoConfiguration().ldtEpochMillisCustomizer().customize(b);
        com.fasterxml.jackson.databind.ObjectMapper m = b.build(); com.fasterxml.jackson.databind.JsonNode json = m.readTree(m.writeValueAsString(service.get(id)));
        assertEquals("2026-10-01", json.get("effectiveFrom").asText()); assertTrue(json.get("reviewedTime").isIntegralNumber());
        assertEquals("16.0000", json.get("config").get("corporateRatePercent").asText()); assertTrue(json.get("config").get("lowerBase").isTextual());
        assertNull(service.page(new HrmPayrollInsurancePageReqVO()).getList().get(0).getConfig());
        assertFalse(json.toString().contains("employeeIds"));
    }
    @Test void invalidDatesAndUpperBelowLowerDoNotPersistVersionsOrAudits() {
        assertThrows(ServiceException.class, () -> service.create(draft().setEffectiveFrom(LocalDate.of(999, 1, 1))));
        assertThrows(ServiceException.class, () -> service.create(draft().setEffectiveFrom(end.plusDays(1))));
        assertThrows(ServiceException.class, () -> service.create(draft().setConfig(values().setLowerBase(decimal("2")).setUpperBase(decimal("1")))));
        assertEquals(0, mapper.selectCount()); assertEquals(0, reviewMapper.selectCount());
    }
}
