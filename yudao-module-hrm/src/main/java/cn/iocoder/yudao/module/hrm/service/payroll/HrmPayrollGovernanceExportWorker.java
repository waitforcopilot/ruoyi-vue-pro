package cn.iocoder.yudao.module.hrm.service.payroll;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import org.springframework.stereotype.Service;
import javax.annotation.*;
import java.util.concurrent.*;

/** Small local worker; no external queue, message delivery or public file URL. */
@Service
public class HrmPayrollGovernanceExportWorker {
    @Resource private HrmPayrollGovernanceExportService exports;
    private final ExecutorService executor=Executors.newFixedThreadPool(2,r->{Thread t=new Thread(r,"hrm-governance-export");t.setDaemon(true);return t;});
    public void submit(Long tenant,Long project,Long id,Long actor){executor.submit(()->{TenantContextHolder.setTenantId(tenant);try{exports.generate(project,id,actor);}catch(Exception failure){exports.failed(project,id,actor);}finally{TenantContextHolder.clear();}});}
    @PreDestroy public void stop(){executor.shutdown();}
}
