package cn.iocoder.yudao.module.hrm.service.payroll.eligibility;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.eligibility.*;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.identity.HrmPayrollMappingRespVO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.HrmPayrollReviewDO;
import java.util.List;
public interface HrmPayrollEligibilityService {
    Long create(HrmPayrollEligibilitySaveReqVO request);
    void update(HrmPayrollEligibilitySaveReqVO request);
    Long newVersion(Long id,Integer revision);
    void review(HrmPayrollEligibilityReviewReqVO request);
    HrmPayrollEligibilityRespVO get(Long id);
    PageResult<HrmPayrollEligibilityRespVO> page(HrmPayrollEligibilityPageReqVO request);
    List<HrmPayrollReviewDO> history(Long id);
    HrmPayrollMappingRespVO employee(Long id);
    HrmPayrollEligibilityLookupVO lookup(HrmPayrollEligibilityLookupReqVO request);
}
