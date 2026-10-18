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
}
