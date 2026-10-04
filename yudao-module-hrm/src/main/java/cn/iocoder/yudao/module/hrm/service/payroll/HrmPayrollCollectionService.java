package cn.iocoder.yudao.module.hrm.service.payroll;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;

public interface HrmPayrollCollectionService {
    Map<String, Integer> initialize();
    PageResult<HrmPayrollRequirementDO> page(@Valid HrmPayrollRequirementPageReqVO query);
    HrmPayrollRequirementDO get(Long id);
    Map<String, Long> summary();
    Long create(@Valid HrmPayrollRequirementSaveReqVO request);
    void update(@Valid HrmPayrollRequirementSaveReqVO request);
    void delete(Long id, Integer version);
    void review(@Valid HrmPayrollReviewReqVO request);
    List<HrmPayrollSourceDO> sources();
    Long createSource(@Valid HrmPayrollSourceSaveReqVO request);
    void updateSource(@Valid HrmPayrollSourceSaveReqVO request);
    List<HrmPayrollReviewDO> history(String objectType, Long objectId);
    Long createBaseline();
    List<HrmPayrollBaselineDO> baselines();
    HrmPayrollBaselineDO baseline(Long id);
}
