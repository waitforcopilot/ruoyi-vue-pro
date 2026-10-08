package cn.iocoder.yudao.module.hrm.service.payroll.review;

import cn.iocoder.yudao.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.review.*;

public interface HrmPayrollReviewService {
    HrmPayrollReviewRespVO get(Long batchId);

    HrmPayrollReviewRespVO sync(Long batchId);

    HrmPayrollReviewRespVO action(HrmPayrollReviewActionReqVO request);

    void processEvent(BpmProcessInstanceStatusEvent event);
}
