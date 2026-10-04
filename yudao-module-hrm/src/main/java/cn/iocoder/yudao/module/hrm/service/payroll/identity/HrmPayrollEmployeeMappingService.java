package cn.iocoder.yudao.module.hrm.service.payroll.identity;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.identity.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.HrmPayrollReviewDO;
import java.util.*;
public interface HrmPayrollEmployeeMappingService {
    Long create(HrmPayrollMappingSaveReqVO request);
    void update(HrmPayrollMappingSaveReqVO request);
    Long newVersion(Long id,Integer revision,Long employeeId);
    void review(HrmPayrollMappingReviewReqVO request);
    HrmPayrollMappingRespVO get(Long id);
    PageResult<HrmPayrollMappingRespVO> page(HrmPayrollMappingPageReqVO request);
    List<HrmPayrollReviewDO> history(Long id);
    List<HrmPayrollMappingLookupVO.Match> resolve(Long sourceId,String namespace,List<HrmPayrollMappingLookupVO> lookups);
    HrmPayrollMappingRespVO employee(Long id);
}
