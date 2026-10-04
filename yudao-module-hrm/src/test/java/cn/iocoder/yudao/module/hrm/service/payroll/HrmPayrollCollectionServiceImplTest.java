package cn.iocoder.yudao.module.hrm.service.payroll;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.*;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import javax.annotation.Resource;
import javax.validation.ConstraintViolationException;
import java.io.ByteArrayInputStream;
import java.util.*;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@Import({HrmPayrollCollectionServiceImpl.class, org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration.class})
class HrmPayrollCollectionServiceImplTest extends BaseDbUnitTest {
    @Resource private HrmPayrollCollectionService service;
    @Resource private HrmPayrollRequirementMapper requirementMapper;
    @Resource private HrmPayrollSourceMapper sourceMapper;
    @Resource private HrmPayrollReviewMapper reviewMapper;
    @MockBean private AdminUserApi adminUserApi;

    @BeforeEach
    void actor() {
        TenantContextHolder.setTenantId(1L);
        LoginUser user = new LoginUser().setId(10L).setTenantId(1L).setUserType(2);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));
        when(adminUserApi.getUser(10L)).thenReturn(new AdminUserRespDTO().setId(10L).setNickname("评审人甲"));
    }

    @AfterEach
    void clear() { TenantContextHolder.clear(); SecurityContextHolder.clearContext(); }

    private HrmPayrollRequirementDO candidate() {
        service.initialize();
        return requirementMapper.selectTenantByCode("OV-01", 1L);
    }

    private HrmPayrollRequirementSaveReqVO editable(HrmPayrollRequirementDO row) {
        return BeanUtils.toBean(row, HrmPayrollRequirementSaveReqVO.class)
                .setOwnerName("薪酬负责人").setAcceptanceCriteria("按已确认口径核对员工及合计，保留来源编号");
    }

    private HrmPayrollRequirementSaveReqVO custom(String code) {
        return new HrmPayrollRequirementSaveReqVO().setCode(code).setModuleCode("req").setTitle("补充需求")
                .setSourceCodes(Collections.emptyList()).setScopeDecision(0);
    }

    private HrmPayrollReviewReqVO conclusion(HrmPayrollRequirementDO row, int status) {
        return new HrmPayrollReviewReqVO().setId(row.getId()).setVersion(row.getVersion()).setStatus(status).setEvidence("依据评审会议纪要确认");
    }

    @Test void initializesAllCandidatesWithoutPrototypeConclusionsAndPreservesChanges() {
        assertEquals(46, service.initialize().get("createdRequirements"));
        assertEquals(12, service.sources().size());
        assertEquals(58, reviewMapper.selectCount().intValue());
        assertEquals(46L, service.summary().get("pending"));
        for (HrmPayrollRequirementDO row : requirementMapper.selectList()) {
            assertNull(row.getPriority()); assertEquals(0, row.getStatus()); assertEquals(0, row.getScopeDecision());
            assertNotNull(row.getOrigin());
        }
        HrmPayrollRequirementDO row = requirementMapper.selectTenantByCode("OV-01", 1L);
        service.update(editable(row).setTitle("真实补充内容"));
        assertEquals(0, service.initialize().get("createdRequirements"));
        assertEquals("真实补充内容", service.get(row.getId()).getTitle());
        assertEquals(59, reviewMapper.selectCount().intValue());
    }

    @Test void savesCustomIgnoresClientIdAndVersionAndRejectsDuplicate() {
        HrmPayrollRequirementSaveReqVO request = custom("CUSTOM-NEW").setId(999L).setVersion(99);
        Long id = service.create(request);
        assertNotEquals(999L, id); assertEquals(1, service.get(id).getVersion());
        assertServiceException(() -> service.create(custom("CUSTOM-NEW")), PAYROLL_COLLECTION_CODE_DUPLICATE);
        assertEquals(1, reviewMapper.selectCount().intValue());
    }

    @Test void rejectsBuiltInCodeAndDeletingPrototypeCandidate() {
        assertServiceException(() -> service.create(custom("OV-NEW")), PAYROLL_COLLECTION_INVALID, "补充需求编号须以 CUSTOM- 开头");
        HrmPayrollRequirementDO row = candidate();
        assertServiceException(() -> service.delete(row.getId(), row.getVersion()), PAYROLL_COLLECTION_BUILTIN_DELETE);
    }

    @Test void deletesCustomWithAuditAndNeverReusesItsCode() {
        Long id = service.create(custom("CUSTOM-DELETE"));
        service.delete(id, 1);
        assertServiceException(() -> service.get(id), PAYROLL_COLLECTION_NOT_EXISTS);
        assertServiceException(() -> service.create(custom("CUSTOM-DELETE")), PAYROLL_COLLECTION_CODE_DUPLICATE);
        assertEquals(2, reviewMapper.selectCount().intValue());
        assertEquals("delete", service.history("requirement", id).get(0).getAction());
    }

    @Test void deferredNeedsReasonAndUnknownOrRepeatedSourceRejected() {
        HrmPayrollRequirementDO row = candidate();
        assertServiceException(() -> service.update(editable(row).setScopeDecision(2)), PAYROLL_COLLECTION_INVALID, "暂缓必须填写原因");
        assertServiceException(() -> service.update(editable(row).setSourceCodes(Arrays.asList("DS-01", "DS-01"))), PAYROLL_COLLECTION_INVALID, "数据来源不能重复");
        assertServiceException(() -> service.update(editable(row).setSourceCodes(Collections.singletonList("DS-MISSING"))), PAYROLL_COLLECTION_INVALID, "数据来源不存在或不属于当前租户");
        service.update(editable(row).setScopeDecision(2).setScopeReason("首期缺少来源，暂缓"));
        assertEquals(2, service.get(row.getId()).getScopeDecision());
    }

    @Test void confirmationNeedsOwnerCriteriaSourcesAndDisputeNeedsOwner() {
        HrmPayrollRequirementDO row = candidate();
        assertServiceException(() -> service.review(conclusion(row, 1)), PAYROLL_COLLECTION_INVALID, "确认必须填写需求负责人");
        assertServiceException(() -> service.review(conclusion(row, 2)), PAYROLL_COLLECTION_INVALID, "异议必须指定处理负责人");
        service.update(editable(row).setAcceptanceCriteria(null));
        HrmPayrollRequirementDO updated = service.get(row.getId());
        assertServiceException(() -> service.review(conclusion(updated, 1)), PAYROLL_COLLECTION_INVALID, "确认必须填写验收条件");
        service.update(editable(updated).setSourceCodes(Collections.emptyList()));
        assertServiceException(() -> service.review(conclusion(service.get(row.getId()), 1)), PAYROLL_COLLECTION_INVALID, "确认必须关联数据来源");
    }

    @Test void confirmationDoesNotMakeSourcesReadyAndEditResetsConclusionWithHistory() {
        HrmPayrollRequirementDO row = candidate();
        service.update(editable(row));
        service.review(conclusion(service.get(row.getId()), 1));
        HrmPayrollRequirementDO confirmed = service.get(row.getId());
        assertEquals(1, confirmed.getStatus()); assertEquals(10L, confirmed.getReviewedBy());
        assertEquals("评审人甲", confirmed.getReviewedByName()); assertNotNull(confirmed.getReviewedTime());
        assertEquals(0L, service.summary().get("sourceReady"));
        service.update(editable(confirmed).setPriority(2));
        HrmPayrollRequirementDO changed = service.get(row.getId());
        assertEquals(0, changed.getStatus()); assertNull(changed.getReviewedBy()); assertNull(changed.getReviewedTime()); assertNull(changed.getEvidence());
        List<HrmPayrollReviewDO> history = service.history("requirement", row.getId());
        assertEquals(4, history.size());
        assertTrue(history.get(0).getBeforeSnapshot().contains("依据评审会议纪要确认"));
        assertEquals("确认后修改，需重新评审", history.get(0).getReason());
    }

    @Test void staleUpdateReviewAndDeleteDoNotWriteAnyAudit() {
        HrmPayrollRequirementDO row = candidate();
        service.update(editable(row));
        long count = reviewMapper.selectCount();
        assertServiceException(() -> service.update(editable(row)), PAYROLL_COLLECTION_VERSION_STALE);
        assertServiceException(() -> service.review(conclusion(row, 1)), PAYROLL_COLLECTION_VERSION_STALE);
        assertServiceException(() -> service.delete(row.getId(), row.getVersion()), PAYROLL_COLLECTION_VERSION_STALE);
        assertEquals(count, reviewMapper.selectCount());
    }

    @Test void nullableFieldsCanBeClearedAndCodeCannotChange() {
        HrmPayrollRequirementDO row = candidate();
        service.update(editable(row).setPriority(1).setRemark("后续删除"));
        HrmPayrollRequirementSaveReqVO change = editable(service.get(row.getId())).setPriority(null).setRemark(null);
        service.update(change);
        assertNull(service.get(row.getId()).getPriority()); assertNull(service.get(row.getId()).getRemark());
        assertServiceException(() -> service.update(editable(service.get(row.getId())).setCode("OV-OTHER")), PAYROLL_COLLECTION_INVALID, "稳定编号不能修改");
    }

    @Test void sourceReadyNeedsActualSystemOwnerMappingEvidenceAndCanReturnToPending() {
        candidate();
        HrmPayrollSourceDO source = service.sources().get(0);
        HrmPayrollSourceSaveReqVO request = BeanUtils.toBean(source, HrmPayrollSourceSaveReqVO.class).setReadiness(1);
        assertServiceException(() -> service.updateSource(request), PAYROLL_COLLECTION_INVALID, "数据就绪必须填写实际系统");
        request.setActualSystem("测试 HR 主档").setOwnerName("来源负责人").setFieldMapping("employeeCode -> employee_id；金额为元，月度截止时间由负责人核实").setEvidence("脱敏样本核对通过");
        service.updateSource(request);
        HrmPayrollSourceDO ready = sourceMapper.selectTenantById(source.getId(), 1L, false);
        assertEquals(1, ready.getReadiness()); assertNotNull(ready.getConfirmedTime());
        assertEquals(0L, service.summary().get("confirmed"));
        assertServiceException(() -> service.updateSource(request), PAYROLL_COLLECTION_VERSION_STALE);
        service.updateSource(BeanUtils.toBean(ready, HrmPayrollSourceSaveReqVO.class).setReadiness(0).setFieldMapping(null).setEvidence(null));
        HrmPayrollSourceDO pending = sourceMapper.selectTenantById(source.getId(), 1L, false);
        assertNull(pending.getConfirmedBy()); assertNull(pending.getConfirmedTime()); assertNull(pending.getFieldMapping());
    }

    @Test void customSourceGapRequiresEvidence() {
        HrmPayrollSourceSaveReqVO request = new HrmPayrollSourceSaveReqVO().setCode("DS-CUSTOM-ONE").setName("补充来源").setReadiness(2);
        assertServiceException(() -> service.createSource(request), PAYROLL_COLLECTION_INVALID, "待补齐来源必须说明缺口");
        Long id = service.createSource(request.setEvidence("尚未接入"));
        assertEquals(1, sourceMapper.selectTenantById(id, 1L, false).getVersion());
    }

    @Test void pageFiltersAndSummaryUsePersistedValues() {
        HrmPayrollRequirementDO row = candidate();
        service.update(editable(row).setPriority(3).setScopeDecision(1));
        HrmPayrollRequirementPageReqVO query = new HrmPayrollRequirementPageReqVO().setModuleCode("overview").setSearch("OV-01").setPriority(3).setScopeDecision(1);
        assertEquals(1L, service.page(query).getTotal());
        assertEquals(1L, service.summary().get("mvp"));
    }

    @Test void baselineIsImmutableAndExcelIncludesAllCandidatesAndBothDirections() throws Exception {
        HrmPayrollRequirementDO row = candidate();
        String originalTitle = row.getTitle();
        Long id = service.createBaseline();
        service.update(editable(row).setTitle("基线之后的修改"));
        assertEquals(originalTitle, JsonUtils.getObjectMapper().readTree(service.baseline(id).getSnapshot()).get("requirements").get(0).get("title").asText());
        assertNull(service.baselines().get(0).getSnapshot());
        MockHttpServletResponse response = new MockHttpServletResponse();
        HrmPayrollBaselineExporter.write(service.baseline(id), response);
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(response.getContentAsByteArray()))) {
            assertEquals(3, workbook.getNumberOfSheets());
            assertEquals(46, workbook.getSheet("需求评审").getLastRowNum());
            assertEquals(12, workbook.getSheet("数据来源").getLastRowNum());
            assertEquals(originalTitle, workbook.getSheet("需求评审").getRow(1).getCell(2).getStringCellValue());
            assertEquals("待确认", workbook.getSheet("需求评审").getRow(1).getCell(5).getStringCellValue());
            assertTrue(workbook.getSheet("数据来源").getRow(1).getCell(12).getStringCellValue().contains("OV-01"));
        }
    }

    @Test void emptyCollectionCannotProduceBaseline() {
        assertServiceException(() -> service.createBaseline(), PAYROLL_COLLECTION_INVALID, "请先登记候选需求");
    }

    @Test void customOnlyCollectionCannotExportAnIncompletePrototypeCatalog() {
        service.create(custom("CUSTOM-FIRST"));
        assertServiceException(() -> service.createBaseline(), PAYROLL_COLLECTION_INVALID, "请先登记完整的 46 个原型与征集候选需求");
        service.initialize();
        assertEquals(47, service.baseline(service.createBaseline()).getRequirementCount());
    }

    @Test void sourceFilterMatchesTheExactJsonElementWithoutJsonEncodingTheLikePattern() {
        candidate();
        HrmPayrollRequirementPageReqVO query = new HrmPayrollRequirementPageReqVO().setSourceCode("DS-01");
        assertTrue(service.page(query).getTotal() > 0);
        assertEquals(0L, service.page(query.setSourceCode("DS-010")).getTotal());
        assertThrows(ConstraintViolationException.class, () -> service.page(query.setSourceCode("DS-%")));
    }

    @Test void tenantCannotReadMutateHistoryOrExportAnotherTenant() {
        HrmPayrollRequirementDO row = candidate();
        HrmPayrollSourceDO source = service.sources().get(0);
        Long baseline = service.createBaseline();
        TenantContextHolder.setTenantId(2L);
        assertEquals(0L, service.summary().get("total")); assertTrue(service.sources().isEmpty()); assertTrue(service.baselines().isEmpty());
        assertServiceException(() -> service.get(row.getId()), PAYROLL_COLLECTION_NOT_EXISTS);
        assertServiceException(() -> service.update(editable(row)), PAYROLL_COLLECTION_NOT_EXISTS);
        assertServiceException(() -> service.review(conclusion(row, 1)), PAYROLL_COLLECTION_NOT_EXISTS);
        assertServiceException(() -> service.delete(row.getId(), 1), PAYROLL_COLLECTION_NOT_EXISTS);
        assertServiceException(() -> service.history("requirement", row.getId()), PAYROLL_COLLECTION_NOT_EXISTS);
        assertServiceException(() -> service.updateSource(BeanUtils.toBean(source, HrmPayrollSourceSaveReqVO.class)), PAYROLL_COLLECTION_NOT_EXISTS);
        assertServiceException(() -> service.baseline(baseline), PAYROLL_BASELINE_NOT_EXISTS);
        assertEquals(46, service.initialize().get("createdRequirements"));
        assertEquals(12, service.sources().size());
    }

    @Test void malformedInputAndMissingVersionAreRejected() {
        assertThrows(ConstraintViolationException.class, () -> service.create(custom("CUSTOM-VALID").setTitle(" ")));
        HrmPayrollRequirementDO row = candidate();
        assertServiceException(() -> service.update(editable(row).setVersion(null)), PAYROLL_COLLECTION_VERSION_STALE);
        assertThrows(ConstraintViolationException.class, () -> service.review(conclusion(row, 0).setEvidence("")));
        assertServiceException(() -> service.history("bad", row.getId()), PAYROLL_COLLECTION_INVALID, "对象类型不合法");
    }
}
