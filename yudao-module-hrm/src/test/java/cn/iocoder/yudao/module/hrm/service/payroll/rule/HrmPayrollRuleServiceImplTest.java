package cn.iocoder.yudao.module.hrm.service.payroll.rule;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.rule.*;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.rule.HrmPayrollRuleSaveReqVO.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.rule.HrmPayrollRuleMapper;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import javax.annotation.Resource;
import javax.validation.ConstraintViolationException;
import java.time.LocalDate;
import java.util.*;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@Import({HrmPayrollRuleServiceImpl.class, org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration.class})
class HrmPayrollRuleServiceImplTest extends BaseDbUnitTest {
    @Resource private HrmPayrollRuleService service;
    @Resource private HrmPayrollRuleMapper mapper;
    @MockBean private AdminUserApi adminUserApi;
    @BeforeEach void actor() { login(1L); }
    private void login(Long tenant) {
        TenantContextHolder.setTenantId(tenant);
        LoginUser user=new LoginUser().setId(10L).setTenantId(tenant).setUserType(2);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user,null,Collections.emptyList()));
        when(adminUserApi.getUser(10L)).thenReturn(new AdminUserRespDTO().setId(10L).setNickname("规则评审人"));
    }
    @AfterEach void clear() { TenantContextHolder.clear();SecurityContextHolder.clearContext(); }
    private HrmPayrollRuleSaveReqVO draft() {
        return new HrmPayrollRuleSaveReqVO().setCode("RULE-CUSTOM-TEST").setTitle("合成测试口径").setCategory("ROUNDING")
                .setQuestionCode("Q-11").setOwnerName("测试负责人").setScopeCode("QA-ONLY").setApplicableScope("仅合成测试")
                .setEffectiveFrom(LocalDate.of(2026,10,1)).setEffectiveTo(LocalDate.of(2026,10,31))
                .setDefinition("合成样例用于验证类型、版本及证据，不应用于工资计算")
                .setReference("隔离测试说明，无实际政策结论")
                .setParameters(new ArrayList<>(Collections.singletonList(new Parameter().setKey("zero").setLabel("零值参数")
                        .setType("DECIMAL").setValue("+00.00").setUnit("演示单位").setScale(2))))
                .setCases(new ArrayList<>(Collections.singletonList(new BusinessCase().setTitle("合成零值")
                        .setInputJson("{\"input\":0}").setExpectedResult("预期零值有效，无正式计算结果"))));
    }
    private HrmPayrollRuleSaveReqVO edit(Long id) { return BeanUtils.toBean(service.get(id),HrmPayrollRuleSaveReqVO.class); }
    private void review(Long id,String action) {
        service.review(new HrmPayrollRuleReviewReqVO().setId(id).setRevision(service.get(id).getRevision())
                .setAction(action).setEvidence("合成测试评审依据"));
    }
    private Long confirmed() { Long id=service.create(draft());review(id,"confirm");return id; }

    @Test void initializesFifteenUndecidedQuestionsIdempotentlyWithoutPolicyValues() {
        assertServiceException(()->service.create(draft().setCode("RULE-Q02")),PAYROLL_RULE_INVALID,"补充规则编号须以 RULE-CUSTOM- 开头并填写后缀");
        assertEquals(15,service.initialize());assertEquals(0,service.initialize());
        HrmPayrollRulePageReqVO query=new HrmPayrollRulePageReqVO();query.setPageSize(100);
        for(HrmPayrollRuleSaveReqVO item:service.page(query).getList()) {
            HrmPayrollRuleRespVO row=service.get(item.getId());
            assertEquals(0,row.getStatus());assertNull(row.getEffectiveFrom());assertNull(row.getOwnerName());
            assertNull(row.getReference());assertNull(row.getDefinition());assertTrue(row.getParameters().isEmpty());assertTrue(row.getCases().isEmpty());
        }
        Long id=mapper.selectRoot("RULE-Q02",1L,false).getId();service.update(edit(id).setOwnerName("业务补充负责人"));
        assertEquals(0,service.initialize());assertEquals("业务补充负责人",service.get(id).getOwnerName());
    }
    @Test void createIgnoresClientIdentityAndRevisionAndReservesCode() {
        Long id=service.create(draft().setId(999L).setRevision(99));
        assertNotEquals(999L,id);assertEquals(1,service.get(id).getRuleVersion());assertEquals(1,service.get(id).getRevision());
        assertServiceException(()->service.create(draft()),PAYROLL_RULE_CODE_DUPLICATE);
    }
    @Test void staleChangesAreRejectedAndNullFieldsCanBeCleared() {
        Long id=service.create(draft());HrmPayrollRuleSaveReqVO stale=edit(id);
        service.update(edit(id).setEffectiveTo(null).setReference(null));
        assertNull(service.get(id).getEffectiveTo());assertNull(service.get(id).getReference());
        assertServiceException(()->service.update(stale),PAYROLL_RULE_STALE);
        assertServiceException(()->service.newVersion(id,1),PAYROLL_RULE_STALE);assertEquals(2,service.history(id).size());
    }
    @Test void codeAndCategoryAreStableAcrossDraftEdits() {
        Long id=service.create(draft());
        assertServiceException(()->service.update(edit(id).setCode("RULE-OTHER")),PAYROLL_RULE_INVALID,"稳定规则编号不能修改");
        assertServiceException(()->service.update(edit(id).setCategory("TAX")),PAYROLL_RULE_INVALID,"同编号的规则分类不能修改");
    }
    @Test void confirmationNeedsOwnerScopeDatesDefinitionReferenceAndBusinessCase() {
        Long id=service.create(draft().setOwnerName(null));
        assertServiceException(()->review(id,"confirm"),PAYROLL_RULE_INVALID,"确认必须填写规则负责人");
        service.update(edit(id).setOwnerName("负责人").setScopeCode(null));
        assertServiceException(()->review(id,"confirm"),PAYROLL_RULE_INVALID,"确认必须填写稳定范围编号");
        service.update(edit(id).setScopeCode("QA").setCases(Collections.emptyList()));
        assertServiceException(()->review(id,"confirm"),PAYROLL_RULE_INVALID,"确认必须提供业务输入及预期结果样例");
    }
    @Test void zeroAndFalseAreExplicitAndNumericValuesNormalizeWithoutRounding() {
        HrmPayrollRuleSaveReqVO request=draft();request.getParameters().add(new Parameter().setKey("flag").setLabel("布尔")
                .setType("BOOLEAN").setValue("false"));
        Long id=service.create(request);review(id,"confirm");
        assertEquals("0",service.get(id).getParameters().get(0).getValue());assertEquals("false",service.get(id).getParameters().get(1).getValue());
        assertTrue(service.history(id).get(0).getAfterSnapshot().contains("\"value\":\"0\""));
    }
    @Test void confirmationRejectsMissingValueMissingUnitAndMissingPrecision() {
        HrmPayrollRuleSaveReqVO request=draft();request.getParameters().get(0).setValue(null);Long id=service.create(request);
        assertServiceException(()->review(id,"confirm"),PAYROLL_RULE_INVALID,"确认时所有参数必须明确值，零值应填 0");
        HrmPayrollRuleSaveReqVO update=edit(id);update.getParameters().get(0).setValue("0").setUnit(null);service.update(update);
        assertServiceException(()->review(id,"confirm"),PAYROLL_RULE_INVALID,"数值参数必须明确单位");
        update=edit(id);update.getParameters().get(0).setUnit("演示单位").setScale(null);service.update(update);
        assertServiceException(()->review(id,"confirm"),PAYROLL_RULE_INVALID,"小数参数必须明确小数位");
    }
    @Test void rejectsBadTypesPrecisionAndCalendarDateWithoutWritingConclusion() {
        for(String value:Arrays.asList("1.234","1e2","1234567890123456789","=1+1")) {
            HrmPayrollRuleSaveReqVO request=draft().setCode("RULE-CUSTOM-CASE-"+Math.abs(value.hashCode()));request.getParameters().get(0).setValue(value);
            Long id=service.create(request);assertServiceException(()->review(id,"confirm"),PAYROLL_RULE_INVALID,"参数值不符合类型或精度约束");
            assertEquals(1,service.history(id).size());
        }
        HrmPayrollRuleSaveReqVO request=draft();request.getParameters().get(0).setType("DATE").setValue("2026-02-30");Long id=service.create(request);
        assertServiceException(()->review(id,"confirm"),PAYROLL_RULE_INVALID,"参数值不符合类型或精度约束");
    }
    @Test void confirmedRuleIsImmutableAndCopiedVersionDoesNotInheritReviewMetadata() {
        Long id=confirmed();assertEquals("规则评审人",service.get(id).getReviewedByName());assertNotNull(service.get(id).getReviewedTime());
        assertEquals(10L,mapper.selectById(id).getReviewedBy());
        assertServiceException(()->service.update(edit(id)),PAYROLL_RULE_IMMUTABLE);
        Long next=service.newVersion(id,service.get(id).getRevision());HrmPayrollRuleRespVO row=service.get(next);
        assertEquals(2,row.getRuleVersion());assertEquals(0,row.getStatus());assertNull(row.getReviewedByName());assertNull(row.getReviewedTime());assertNull(row.getEvidence());
        assertEquals("0",row.getParameters().get(0).getValue());assertEquals(1,service.get(id).getStatus());
    }
    @Test void overlappingDatesBlockConfirmationWhileAdjacentDatesAndDifferentScopesAreAllowed() {
        Long id=confirmed(),next=service.newVersion(id,service.get(id).getRevision());
        assertServiceException(()->review(next,"confirm"),PAYROLL_RULE_PERIOD_OVERLAP);
        service.update(edit(next).setEffectiveFrom(LocalDate.of(2026,10,31)).setEffectiveTo(LocalDate.of(2026,11,30)));
        assertServiceException(()->review(next,"confirm"),PAYROLL_RULE_PERIOD_OVERLAP);
        service.update(edit(next).setEffectiveFrom(LocalDate.of(2026,11,1)));review(next,"confirm");
        Long other=service.newVersion(id,service.get(id).getRevision());service.update(edit(other).setScopeCode("QA-OTHER"));review(other,"confirm");
        assertEquals(1,service.get(other).getStatus());
    }
    @Test void openEndedRulesOverlapLaterDatesAndRetirementPreservesOldSnapshot() {
        Long id=service.create(draft().setEffectiveTo(null));review(id,"confirm");
        Long next=service.newVersion(id,service.get(id).getRevision());service.update(edit(next).setEffectiveFrom(LocalDate.of(2027,1,1)));
        assertServiceException(()->review(next,"confirm"),PAYROLL_RULE_PERIOD_OVERLAP);
        review(id,"retire");review(next,"confirm");assertEquals(2,service.get(id).getStatus());
        assertTrue(service.history(id).get(0).getBeforeSnapshot().contains("\"status\":1"));
        assertServiceException(()->service.update(edit(id)),PAYROLL_RULE_IMMUTABLE);
    }
    @Test void tenantChecksCoverListDetailHistoryEditReviewAndVersionAllocation() {
        Long id=confirmed();HrmPayrollRuleSaveReqVO payload=edit(id);login(999L);
        assertEquals(0L,service.page(new HrmPayrollRulePageReqVO()).getTotal());
        assertServiceException(()->service.get(id),PAYROLL_RULE_NOT_EXISTS);
        assertServiceException(()->service.history(id),PAYROLL_RULE_NOT_EXISTS);
        assertServiceException(()->service.update(payload),PAYROLL_RULE_NOT_EXISTS);
        assertServiceException(()->service.newVersion(id,payload.getRevision()),PAYROLL_RULE_NOT_EXISTS);
        assertServiceException(()->service.review(new HrmPayrollRuleReviewReqVO().setId(id).setRevision(payload.getRevision()).setAction("retire").setEvidence("跨租户")),PAYROLL_RULE_NOT_EXISTS);
        assertNotNull(service.create(draft()));
    }
    @Test void metadataListOmitsCasesAndParametersAndWireDatesUseIso() {
        Long id=confirmed();HrmPayrollRuleRespVO row=service.page(new HrmPayrollRulePageReqVO()).getList().get(0);
        assertNull(row.getCases());assertNull(row.getParameters());assertNull(row.getDefinition());assertEquals(1,row.getCaseCount());
        assertTrue(JsonUtils.toJsonString(service.get(id)).contains("\"effectiveFrom\":\"2026-10-01\""));
    }
    @Test void structuredExamplesRejectBrokenJsonNonObjectsDepthAndDuplicateParameters() {
        HrmPayrollRuleSaveReqVO request=draft();request.getCases().get(0).setInputJson("[1,2]");
        assertServiceException(()->service.create(request),PAYROLL_RULE_INVALID,"业务样例输入必须是合法 JSON 对象");
        request.getCases().get(0).setInputJson("broken");assertServiceException(()->service.create(request),PAYROLL_RULE_INVALID,"业务样例输入必须是合法 JSON 对象");
        request.getCases().get(0).setInputJson(String.join("",Collections.nCopies(18,"{\"a\":"))+"0"+String.join("",Collections.nCopies(18,"}")));
        assertServiceException(()->service.create(request),PAYROLL_RULE_INVALID,"业务样例 JSON 嵌套超过 16 层");
        request.getCases().get(0).setInputJson("{}");request.getParameters().add(request.getParameters().get(0));
        assertServiceException(()->service.create(request),PAYROLL_RULE_INVALID,"参数标识不能重复");
        request.getParameters().clear();request.getCases().get(0).setTitle("");assertThrows(ConstraintViolationException.class,()->service.create(request));
    }
}
