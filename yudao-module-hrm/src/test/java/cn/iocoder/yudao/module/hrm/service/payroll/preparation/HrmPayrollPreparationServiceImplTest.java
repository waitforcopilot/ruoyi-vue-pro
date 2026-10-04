package cn.iocoder.yudao.module.hrm.service.payroll.preparation;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.preparation.HrmPayrollPreparationRespVO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.intake.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.rule.HrmPayrollRuleDO;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.intake.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.rule.HrmPayrollRuleMapper;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@Import(HrmPayrollPreparationServiceImpl.class)
class HrmPayrollPreparationServiceImplTest extends BaseDbUnitTest {
    @Resource private HrmPayrollPreparationService service;
    @Resource private HrmPayrollRequirementMapper requirements;
    @Resource private HrmPayrollSourceMapper sources;
    @Resource private HrmPayrollContractMapper contracts;
    @Resource private HrmPayrollRuleMapper rules;
    @Resource private HrmPayrollImportBatchMapper batches;
    @MockBean private PermissionApi permissionApi;
    @BeforeEach void actor() {
        login(1L,10L);
        for(String permission:Arrays.asList("hrm:payroll:requirements:query","hrm:payroll:intake:query","hrm:payroll:rule:query"))
            when(permissionApi.hasAnyPermissions(10L,permission)).thenReturn(true);
    }
    private void login(Long tenant,Long user) {
        TenantContextHolder.setTenantId(tenant);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new LoginUser().setId(user).setTenantId(tenant).setUserType(2),null,Collections.emptyList()));
    }
    @AfterEach void clear() { TenantContextHolder.clear();SecurityContextHolder.clearContext(); }
    private void requirement(long id,long tenant,int status,int scope,String module) {
        HrmPayrollRequirementDO row=new HrmPayrollRequirementDO().setId(id).setCode("CUSTOM-"+id).setModuleCode(module)
                .setTitle("fixture").setStatus(status).setScopeDecision(scope).setVersion(1).setDescription("SENSITIVE_NOT_RETURNED");
        row.setTenantId(tenant);requirements.insert(row);
    }
    private void source(long id,long tenant,int state) {
        HrmPayrollSourceDO row=new HrmPayrollSourceDO().setId(id).setCode("DS-"+id).setName("fixture").setReadiness(state)
                .setVersion(1).setEvidence("SENSITIVE_NOT_RETURNED");row.setTenantId(tenant);sources.insert(row);
    }
    private void contract(long id,long tenant,int state) {
        HrmPayrollContractDO row=new HrmPayrollContractDO().setId(id).setSourceId(id).setSourceCode("DS-01").setSourceName("fixture")
                .setContractVersion(1).setRevision(1).setStatus(state).setTitle("fixture").setSchemaJson("{}").setFieldCount(1);
        row.setTenantId(tenant);contracts.insert(row);
    }
    private void rule(long id,long tenant,int state) {
        HrmPayrollRuleDO row=new HrmPayrollRuleDO().setId(id).setCode("RULE-CUSTOM-"+id).setTitle("fixture").setCategory("ROUNDING")
                .setRuleVersion(1).setRevision(1).setStatus(state).setParametersJson("[]").setCasesJson("[]").setParameterCount(0).setCaseCount(0)
                .setDefinition("SENSITIVE_NOT_RETURNED");row.setTenantId(tenant);rules.insert(row);
    }
    private void batch(long id,long tenant,long actor,int state) {
        HrmPayrollImportBatchDO row=new HrmPayrollImportBatchDO().setId(id).setContractId(1L).setSourceCode("DS-01").setContractVersion(2)
                .setFileName("SENSITIVE_NOT_RETURNED.csv").setFileHash(String.format("%064d",id)).setIdempotencyKey(String.format("%064d",id))
                .setDeclaredScope("SENSITIVE_NOT_RETURNED").setPeriodStart(LocalDate.of(2026,10,1)).setPeriodEnd(LocalDate.of(2026,10,31))
                .setRowCount(6).setValidCount(state==0?6:1).setErrorCount(state==0?0:5).setStatus(state).setCreatedBy(actor)
                .setSnapshot("SENSITIVE_NOT_RETURNED");row.setTenantId(tenant);batches.insert(row);
    }
    @Test void emptyTenantDoesNotInitializeOrClaimReadiness() {
        HrmPayrollPreparationRespVO result=service.summary();
        assertEquals(0L,result.getRequirements().getCounts().get("total"));assertEquals(10,result.getRequirements().getModules().size());
        assertEquals(0L,result.getSources().getCounts().get("ready"));assertEquals(0L,result.getRules().getCounts().get("confirmed"));
        assertTrue(result.getBatches().getRecentBatches().isEmpty());assertNotNull(result.getGeneratedAt());
        assertEquals(0L,requirements.selectCount());assertEquals(0L,sources.selectCount());
    }
    @Test void scopeAndReviewRemainIndependent() {
        requirement(1,1,0,1,"calc");requirement(2,1,2,1,"calc");requirement(3,1,1,2,"plan");requirement(4,1,1,0,"plan");
        HrmPayrollPreparationRespVO result=service.summary();Map<String,Long> counts=result.getRequirements().getCounts();
        assertEquals(4L,counts.get("total"));assertEquals(2L,counts.get("inScopeUnconfirmed"));assertEquals(2L,counts.get("confirmed"));
        assertEquals(1L,counts.get("undecided"));assertEquals(1L,counts.get("deferred"));
        HrmPayrollPreparationRespVO.ModuleCount calc=result.getRequirements().getModules().stream().filter(m->m.getCode().equals("calc")).findFirst().get();
        assertEquals(2L,calc.getTotal());assertEquals(1L,calc.getPending());assertEquals(1L,calc.getDisputed());assertEquals(2L,calc.getInScopeUnconfirmed());
    }
    @Test void countsActualSourcesVersionsAndAllStatusesWithoutUsingContractAsSourceReadiness() {
        source(1,1,0);source(2,1,1);source(3,1,2);for(int state=0;state<3;state++){contract(state+1,1,state);rule(state+1,1,state);}
        HrmPayrollPreparationRespVO result=service.summary();assertEquals(1L,result.getSources().getCounts().get("ready"));
        assertEquals(1L,result.getSources().getCounts().get("gap"));assertEquals(3,result.getSources().getSources().size());
        for(HrmPayrollPreparationRespVO.Section section:Arrays.asList(result.getContracts(),result.getRules())) {
            assertEquals(3L,section.getCounts().get("total"));assertEquals(1L,section.getCounts().get("draft"));
            assertEquals(1L,section.getCounts().get("confirmed"));assertEquals(1L,section.getCounts().get("retired"));
        }
    }
    @Test void batchStatisticsAndRecentRowsBelongToCurrentActorIncludingAdministrator() {
        for(int i=1;i<=7;i++)batch(i,1,10,i%2);batch(8,1,20,1);batch(9,999,10,0);
        HrmPayrollPreparationRespVO.Section result=service.summary().getBatches();
        assertEquals(7L,result.getCounts().get("total"));assertEquals(3L,result.getCounts().get("passed"));assertEquals(4L,result.getCounts().get("failed"));
        assertEquals(Arrays.asList(7L,6L,5L,4L,3L),result.getRecentBatches().stream().map(HrmPayrollPreparationRespVO.Batch::getId).collect(java.util.stream.Collectors.toList()));
        login(1L,20L);when(permissionApi.hasAnyPermissions(20L,"hrm:payroll:intake:query")).thenReturn(true);
        assertEquals(1L,service.summary().getBatches().getCounts().get("total"));
    }
    @Test void allSectionsRespectTenantAndLogicalDeletion() {
        for(long tenant:Arrays.asList(1L,999L)) {
            requirement(tenant,tenant,0,0,"req");source(tenant,tenant,0);contract(tenant,tenant,0);rule(tenant,tenant,0);batch(tenant,tenant,10,0);
        }
        HrmPayrollPreparationRespVO result=service.summary();
        for(HrmPayrollPreparationRespVO.Section section:Arrays.asList(result.getRequirements(),result.getSources(),result.getContracts(),result.getRules(),result.getBatches()))
            assertEquals(1L,section.getCounts().get("total"));
        requirements.deleteById(1L);sources.deleteById(1L);contracts.deleteById(1L);rules.deleteById(1L);batches.deleteById(1L);
        result=service.summary();
        for(HrmPayrollPreparationRespVO.Section section:Arrays.asList(result.getRequirements(),result.getSources(),result.getContracts(),result.getRules(),result.getBatches()))
            assertEquals(0L,section.getCounts().get("total"));
        login(999L,10L);assertEquals(1L,service.summary().getRequirements().getCounts().get("total"));
    }
    @Test void overviewPermissionAloneDoesNotExposeUnderlyingCountsOrRows() {
        requirement(1,1,1,1,"calc");source(1,1,1);contract(1,1,1);rule(1,1,1);batch(1,1,10,0);
        for(String permission:Arrays.asList("hrm:payroll:requirements:query","hrm:payroll:intake:query","hrm:payroll:rule:query"))
            when(permissionApi.hasAnyPermissions(10L,permission)).thenReturn(false);
        HrmPayrollPreparationRespVO result=service.summary();
        for(HrmPayrollPreparationRespVO.Section section:Arrays.asList(result.getRequirements(),result.getSources(),result.getContracts(),result.getRules(),result.getBatches())) {
            assertFalse(section.getAuthorized());assertNull(section.getCounts());assertNull(section.getModules());assertNull(section.getSources());assertNull(section.getRecentBatches());
        }
    }
    @Test void permissionRevocationRemovesOnlyTheAffectedSectionsOnNextRefresh() {
        when(permissionApi.hasAnyPermissions(10L,"hrm:payroll:intake:query")).thenReturn(false);
        HrmPayrollPreparationRespVO result=service.summary();assertFalse(result.getContracts().getAuthorized());assertFalse(result.getBatches().getAuthorized());
        assertTrue(result.getRequirements().getAuthorized());assertTrue(result.getRules().getAuthorized());assertNull(result.getBatches().getCounts());
    }
    @Test void responseExcludesSensitivePayloadAndSerializesDatesAsIso() {
        requirement(1,1,1,1,"calc");source(1,1,1);rule(1,1,1);batch(1,1,10,0);
        String json=JsonUtils.toJsonString(service.summary());assertFalse(json.contains("SENSITIVE_NOT_RETURNED"));
        assertFalse(json.contains("snapshot"));assertFalse(json.contains("fileName"));assertFalse(json.contains("parameters"));
        assertTrue(json.contains("\"periodStart\":\"2026-10-01\""));assertTrue(json.contains("\"periodEnd\":\"2026-10-31\""));
    }
    @Test void unknownModuleMetadataStillContributesToTotalInsteadOfBeingLost() {
        requirement(1,1,0,0,"legacy-module");
        HrmPayrollPreparationRespVO result=service.summary();assertEquals(1L,result.getRequirements().getCounts().get("total"));
        assertEquals(11,result.getRequirements().getModules().size());assertEquals(1L,result.getRequirements().getModules().stream().mapToLong(HrmPayrollPreparationRespVO.ModuleCount::getTotal).sum());
    }
}
