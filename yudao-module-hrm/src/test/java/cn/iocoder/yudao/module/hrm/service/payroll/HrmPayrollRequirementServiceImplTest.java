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
        r.setReviewerId(2L);r.setEvidence("业务评审记录");r.setAcceptance("按规则版本核算");r.setSourceOwnerId(3L);r.setSourceSystem("HRM");
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
        HrmPayrollRequirementSaveReqVO r=request();r.setStatus("CONFIRMED");r.setReviewerId(2L);r.setEvidence("评审记录");r.setAcceptance("验收场景");r.setSourceOwnerId(3L);r.setSourceSystem("HRM");
        r.setId(service.save(r,2L));r.setVersion(1);r.setStatus("REVIEWING");service.save(r,3L);
        assertNull(service.list("PLAN").get(0).getConfirmedBy());assertEquals(2,service.history(r.getId()).size());
    }
}
