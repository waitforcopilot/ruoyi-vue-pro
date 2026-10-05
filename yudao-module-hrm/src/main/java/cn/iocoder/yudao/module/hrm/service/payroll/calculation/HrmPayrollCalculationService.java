package cn.iocoder.yudao.module.hrm.service.payroll.calculation;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.calculation.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.HrmPayrollReviewDO;
import java.util.List;
public interface HrmPayrollCalculationService {
    Long create(HrmPayrollCalculationSaveReqVO req);
    void update(HrmPayrollCalculationSaveReqVO req);
    Long newVersion(Long id, Integer revision);
    void review(HrmPayrollCalculationReviewReqVO req);
    HrmPayrollCalculationRespVO get(Long id);
    PageResult<HrmPayrollCalculationRespVO> page(HrmPayrollCalculationPageReqVO req);
    List<HrmPayrollReviewDO> history(Long id);
    HrmPayrollCalculationCasesVO cases(Long id);
    HrmPayrollCalculationPreviewVO preview(HrmPayrollCalculationPreviewReqVO req);
    HrmPayrollCalculationCompareVO compare(Long leftId, Long rightId);
}
