package cn.iocoder.yudao.module.hrm.service.payroll.review;

import cn.iocoder.yudao.module.bpm.api.event.*;
import javax.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class HrmPayrollReviewStatusListener extends BpmProcessInstanceStatusEventListener {
    @Resource private HrmPayrollReviewService service;

    @Override
    protected String getProcessDefinitionKey() {
        return PayrollBpmContext.KEY;
    }

    @Override
    protected void onEvent(BpmProcessInstanceStatusEvent event) {
        service.processEvent(event);
    }
}
