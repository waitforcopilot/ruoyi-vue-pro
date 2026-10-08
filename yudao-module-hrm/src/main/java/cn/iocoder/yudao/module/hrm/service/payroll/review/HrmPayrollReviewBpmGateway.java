package cn.iocoder.yudao.module.hrm.service.payroll.review;

import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.review.HrmPayrollReviewRespVO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.review.HrmPayrollReviewCycleDO;
import java.util.List;

public interface HrmPayrollReviewBpmGateway {
    String definition();

    String start(HrmPayrollReviewCycleDO cycle);

    List<HrmPayrollReviewRespVO.Task> tasks(String instanceId);

    void decide(String instanceId, String taskId, Long actorId, String action);

    void cancel(String instanceId, Long actorId, boolean administrator);

    Integer actualStatus(String instanceId, String businessKey);
}
