package cn.iocoder.yudao.module.hrm.job.payroll;

import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJob;
import cn.iocoder.yudao.module.hrm.service.payroll.HrmPayrollWorkVarianceService;
import javax.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
@lombok.extern.slf4j.Slf4j
public class HrmPayrollWorkVarianceReminderJob implements JobHandler {
  @Resource private HrmPayrollWorkVarianceService service;

  @Override
  @TenantJob
  public String execute(String param) {
    int count = 0, failed = 0;
    for (Long id : service.overdueIds()) {
      try {
        count += service.remind(id);
      } catch (RuntimeException failure) {
        failed++;
        log.warn("工时差异提醒事务失败，工单 {}，类型 {}", id, failure.getClass().getSimpleName());
      }
    }
    return "工时差异逾期站内提醒已送达：" + count + "，失败工单：" + failed;
  }
}
