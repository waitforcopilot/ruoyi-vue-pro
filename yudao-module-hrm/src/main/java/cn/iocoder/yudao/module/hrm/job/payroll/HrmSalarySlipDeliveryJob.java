package cn.iocoder.yudao.module.hrm.job.payroll;

import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJob;
import cn.iocoder.yudao.module.hrm.dal.dataobject.salary.slip.HrmSalarySlipDeliveryDO;
import cn.iocoder.yudao.module.hrm.service.salary.slip.HrmSalarySlipDeliveryService;
import javax.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
@lombok.extern.slf4j.Slf4j
public class HrmSalarySlipDeliveryJob implements JobHandler {
  @Resource private HrmSalarySlipDeliveryService service;

  @Override
  @TenantJob
  public String execute(String param) {
    int count = 0, failed = 0;
    for (Long id : service.pending())
      try {
        HrmSalarySlipDeliveryDO row = service.claim(id);
        if (row != null) {
          service.dispatch(row);
          count++;
        } else service.poll(id);
      } catch (RuntimeException error) {
        failed++;
        log.warn("工资条投递 {} 处理失败，类型 {}", id, error.getClass().getSimpleName());
      }
    return "已处理投递：" + count + "；处理失败：" + failed;
  }
}
