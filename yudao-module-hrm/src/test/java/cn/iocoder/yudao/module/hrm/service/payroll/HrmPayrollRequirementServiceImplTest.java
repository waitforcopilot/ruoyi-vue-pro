package cn.iocoder.yudao.module.hrm.service.payroll;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollRequirementSaveReqVO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.HrmPayrollRequirementDO;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import javax.annotation.Resource;
import static org.junit.jupiter.api.Assertions.*;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;
@Import(HrmPayrollRequirementServiceImpl.class)
class HrmPayrollRequirementServiceImplTest extends BaseDbUnitTest {
    @Resource private HrmPayrollRequirementService service;
    @org.springframework.boot.test.mock.mockito.MockBean private cn.iocoder.yudao.module.system.api.user.AdminUserApi users;
    @org.junit.jupiter.api.BeforeEach void users() {
        org.mockito.Mockito.when(users.getUser(2L)).thenReturn(new cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO().setId(2L).setStatus(0));
        org.mockito.Mockito.when(users.getUser(3L)).thenReturn(new cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO().setId(3L).setStatus(0));
    }
    private HrmPayrollRequirementSaveReqVO request() {
        HrmPayrollRequirementSaveReqVO r=new HrmPayrollRequirementSaveReqVO();
        r.setCode("PLAN-06");r.setModule("PLAN");r.setDescription("测试薪资方案规则");
        r.setStatus("PENDING");r.setReadiness("MISSING");r.setPriority(2);return r;
    }
    @Test void initializeIsIdempotentAndDoesNotConfirmRequirements() {
        assertEquals(43,service.initialize(1L));assertEquals(0,service.initialize(1L));
        assertEquals(43,service.list(null).size());
        for(HrmPayrollRequirementDO r:service.list(null)){assertEquals("PENDING",r.getStatus());assertEquals("MISSING",r.getReadiness());assertEquals(1,service.history(r.getId()).size());}
    }
    @Test void confirmationRequiresEvidenceAndTracksHistoryWithoutChangingReadiness() {
        HrmPayrollRequirementSaveReqVO r=request();Long id=service.save(r,1L);
        r.setId(id);r.setVersion(1);r.setStatus("CONFIRMED");
        assertServiceException(()->service.save(r,2L),PAYROLL_REVIEW_INCOMPLETE);
        r.setReviewerId(2L);r.setEvidence("业务评审记录");r.setAcceptance("按规则版本核算");r.setSourceOwnerId(3L);r.setSourceSystem("HRM");r.setFieldMapping("员工编号 → employeeId");
        service.save(r,2L);
        HrmPayrollRequirementDO result=service.list("PLAN").get(0);
        assertEquals(2,result.getVersion());assertEquals(2L,result.getConfirmedBy());assertEquals("MISSING",result.getReadiness());assertEquals(2,service.history(id).size());
    }
    @Test void staleUpdateIsRejected() {
        HrmPayrollRequirementSaveReqVO r=request();r.setId(service.save(r,1L));r.setVersion(1);
        service.save(r,1L);assertServiceException(()->service.save(r,2L),PAYROLL_VERSION_CONFLICT);
    }
    @Test void duplicateCodeIsRejected() {
        service.save(request(),1L);assertServiceException(()->service.save(request(),2L),PAYROLL_REQUIREMENT_DUPLICATE);
    }
    @Test void reopeningRemovesConfirmationButRetainsHistory() {
        HrmPayrollRequirementSaveReqVO r=request();r.setStatus("CONFIRMED");r.setReviewerId(2L);r.setEvidence("评审记录");r.setAcceptance("验收场景");r.setSourceOwnerId(3L);r.setSourceSystem("HRM");r.setFieldMapping("员工编号 → employeeId");
        r.setId(service.save(r,2L));r.setVersion(1);r.setStatus("REVIEWING");service.save(r,3L);
        assertNull(service.list("PLAN").get(0).getConfirmedBy());assertEquals(2,service.history(r.getId()).size());
    }
    private HrmPayrollRequirementSaveReqVO confirmed() {
        HrmPayrollRequirementSaveReqVO r=request();r.setStatus("CONFIRMED");r.setReviewerId(2L);
        r.setEvidence("评审记录");r.setAcceptance("可验证场景");r.setSourceOwnerId(3L);r.setSourceSystem("HRM");
        r.setFieldMapping("来源字段 → 本项目字段");return r;
    }
    @Test void confirmationRejectsEmptyMappingAndBareExemption() {
        HrmPayrollRequirementSaveReqVO r=confirmed();
        for(String mapping:new String[]{null," ","N/A","N/A\n ","不适用："}) {
            r.setFieldMapping(mapping);
            assertServiceException(()->service.save(r,2L),PAYROLL_REQUIREMENT_CONFIRMATION_INVALID);
        }
        assertTrue(service.list(null).isEmpty());
    }
    @Test void explicitExemptionWithReasonIsRetained() {
        HrmPayrollRequirementSaveReqVO r=confirmed();r.setFieldMapping("不适用：本需求为评审操作规范，不涉及外部业务数据字段");
        service.save(r,2L);assertEquals(r.getFieldMapping(),service.list(null).get(0).getFieldMapping());
    }
    @Test void nonexistentOrDisabledOwnersCannotConfirm() {
        HrmPayrollRequirementSaveReqVO r=confirmed();r.setSourceOwnerId(99999991L);
        assertServiceException(()->service.save(r,2L),PAYROLL_REQUIREMENT_PERSON_INVALID);
        r.setSourceOwnerId(3L);org.mockito.Mockito.when(users.getUser(3L)).thenReturn(new cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO().setId(3L).setStatus(1));
        assertServiceException(()->service.save(r,2L),PAYROLL_REQUIREMENT_PERSON_INVALID);
    }
    @Test void nonexistentReviewerAndForgedConfirmationAreRejected() {
        HrmPayrollRequirementSaveReqVO r=confirmed();r.setReviewerId(99999992L);
        assertServiceException(()->service.save(r,2L),PAYROLL_REQUIREMENT_PERSON_INVALID);
        r.setReviewerId(2L);
        assertServiceException(()->service.save(r,3L),PAYROLL_REQUIREMENT_CONFIRMATION_INVALID);
    }
    @Test void legacyProjectIsServerOwnedAndStableAcrossEdits() {
        assertNull(service.legacyProject()); // Reads do not provision projects.
        HrmPayrollRequirementSaveReqVO r=request(); Long id=service.save(r,1L);
        Long project=service.legacyProject().getId();
        assertNotNull(project);assertNull(service.legacyProject().getProjectScopeVersion());
        assertEquals(1,service.legacyProject().getMetadataVersion());
        assertEquals(project,service.list(null).get(0).getProjectId());
        r.setId(id);r.setVersion(1);r.setDescription("只修改说明，保留归属");service.save(r,1L);
        assertEquals(project,service.list(null).get(0).getProjectId());
        assertEquals(project,service.legacyProject().getId());assertEquals(2,service.history(id).size());
        assertTrue(service.history(id).get(0).getSnapshot().contains("\"projectId\":"+project));
    }
    @Test void multipleRequirementsReuseLegacyProjectAndInvalidWritesDoNotCreateIt() {
        HrmPayrollRequirementSaveReqVO bad=confirmed();bad.setFieldMapping("");
        assertServiceException(()->service.save(bad,2L),PAYROLL_REQUIREMENT_CONFIRMATION_INVALID);
        assertNull(service.legacyProject());
        service.save(request(),1L);Long project=service.legacyProject().getId();
        HrmPayrollRequirementSaveReqVO second=request();second.setCode("PLAN-07");service.save(second,1L);
        for(HrmPayrollRequirementDO row:service.list(null)) assertEquals(project,row.getProjectId());
    }
    @Test void confirmedContentChangeRequiresSeparateReconfirmationAndKeepsOldSnapshot() {
        HrmPayrollRequirementSaveReqVO r=confirmed();r.setId(service.save(r,2L));r.setVersion(1);
        r.setDescription("已确认后修改业务行为");service.save(r,2L);
        HrmPayrollRequirementDO changed=service.list(null).get(0);
        assertEquals("REVIEWING",changed.getStatus());assertNull(changed.getConfirmedBy());assertNull(changed.getConfirmedAt());
        assertEquals("CONFIRMED",r.getStatus()); // Do not mutate the caller's request.
        assertTrue(service.history(r.getId()).get(1).getSnapshot().contains("\"confirmedBy\":2"));
        r.setVersion(2);service.save(r,2L);
        assertEquals("CONFIRMED",service.list(null).get(0).getStatus());
        assertEquals(3,service.history(r.getId()).size());
    }
    @Test void unchangedConfirmedSaveDoesNotRenewSignatureTime() {
        HrmPayrollRequirementSaveReqVO r=confirmed();r.setId(service.save(r,2L));r.setVersion(1);
        java.time.LocalDateTime signed=service.list(null).get(0).getConfirmedAt();
        service.save(r,2L);
        assertEquals(signed,service.list(null).get(0).getConfirmedAt());
        assertEquals(2L,service.list(null).get(0).getConfirmedBy());
    }
    @Test void eachExistingReviewContractFieldInvalidatesConfirmation() {
        java.util.List<java.util.function.Consumer<HrmPayrollRequirementSaveReqVO>> edits=java.util.Arrays.asList(
            x->x.setCode("PLAN-07"),x->x.setModule("CALC"),x->x.setDescription("新行为"),
            x->x.setFieldMapping("新字段 → 新映射"),x->x.setSourceSystem("新来源"),
            x->x.setSourceOwnerId(2L),x->x.setReviewerId(3L),x->x.setAcceptance("新验收"),
            x->x.setEvidence("新依据"),x->x.setPriority(1));
        int index=100;
        for(java.util.function.Consumer<HrmPayrollRequirementSaveReqVO> edit:edits) {
            HrmPayrollRequirementSaveReqVO r=confirmed();r.setCode("PLAN-"+(index++));
            r.setId(service.save(r,2L));r.setVersion(1);edit.accept(r);service.save(r,1L);
            HrmPayrollRequirementDO saved=service.list(null).stream().filter(x->x.getId().equals(r.getId())).findFirst().get();
            assertEquals("REVIEWING",saved.getStatus());assertNull(saved.getConfirmedBy());
        }
    }
    @Test void staleRevisionCannotClearCurrentSignature() {
        HrmPayrollRequirementSaveReqVO r=confirmed();r.setId(service.save(r,2L));r.setVersion(0);r.setDescription("过期修改");
        assertServiceException(()->service.save(r,2L),PAYROLL_VERSION_CONFLICT);
        assertEquals("CONFIRMED",service.list(null).get(0).getStatus());assertEquals(1,service.history(r.getId()).size());
    }
}
