package cn.iocoder.yudao.module.hrm.job.payroll;
import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJob;
import cn.iocoder.yudao.module.hrm.service.payroll.HrmPayrollInputIssueService;
import javax.annotation.Resource;
import org.springframework.stereotype.Component;
@Component @lombok.extern.slf4j.Slf4j
public class HrmPayrollInputIssueReminderJob implements JobHandler {
 @Resource private HrmPayrollInputIssueService service;
 @Override @TenantJob public String execute(String param) {int count=0,failed=0;for(Long id:service.overdueIds())try{count+=service.remind(id);}catch(RuntimeException error){failed++;log.warn("薪酬缺口提醒事务失败，工单 {}，类型 {}",id,error.getClass().getSimpleName());}return "站内提醒已保存："+count+"，失败工单："+failed;}
}
