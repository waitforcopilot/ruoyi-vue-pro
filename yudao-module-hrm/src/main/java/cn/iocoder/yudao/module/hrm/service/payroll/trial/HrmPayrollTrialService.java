package cn.iocoder.yudao.module.hrm.service.payroll.trial;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.trial.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.HrmPayrollReviewDO;
import java.util.List;
public interface HrmPayrollTrialService {
    cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.trial.HrmPayrollTrialBatchDO lockBatch(Long id);
    String validateCurrentRun(Long batchId, Long runId);
 Long create(HrmPayrollTrialSaveReqVO req);
 void update(HrmPayrollTrialSaveReqVO req);
 HrmPayrollTrialRespVO get(Long id);
 PageResult<HrmPayrollTrialRespVO> page(HrmPayrollTrialPageReqVO req);
 HrmPayrollTrialCheckVO check(Long id);
 HrmPayrollTrialInspectionVO inspect(Long id);
 HrmPayrollTrialRunRespVO execute(HrmPayrollTrialExecuteReqVO req);
 List<HrmPayrollTrialRunRespVO> runs(Long id);
 HrmPayrollTrialRunRespVO run(Long id);
 HrmPayrollTrialCompareVO compare(Long leftId,Long rightId);
 List<HrmPayrollReviewDO> history(Long id);
}
