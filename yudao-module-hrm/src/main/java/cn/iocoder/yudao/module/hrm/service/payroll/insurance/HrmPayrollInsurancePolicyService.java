package cn.iocoder.yudao.module.hrm.service.payroll.insurance;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.insurance.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.HrmPayrollReviewDO;
import java.util.List;
import java.time.LocalDate;

public interface HrmPayrollInsurancePolicyService {
    Long create(HrmPayrollInsuranceSaveReqVO req);
    void update(HrmPayrollInsuranceSaveReqVO req);
    Long newVersion(Long id, Integer revision);
    void review(HrmPayrollInsuranceReviewReqVO req);
    HrmPayrollInsuranceRespVO get(Long id);
    PageResult<HrmPayrollInsuranceRespVO> page(HrmPayrollInsurancePageReqVO req);
    List<HrmPayrollReviewDO> history(Long id);
    HrmPayrollInsuranceCompareVO compare(Long leftId, Long rightId);
    HrmPayrollInsuranceRespVO resolve(Long id, LocalDate start, LocalDate end);
    HrmPayrollInsurancePreviewVO preview(HrmPayrollInsurancePreviewReqVO req);
}
