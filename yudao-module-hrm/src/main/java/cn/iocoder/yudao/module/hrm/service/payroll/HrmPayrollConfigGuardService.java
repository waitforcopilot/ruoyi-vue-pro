package cn.iocoder.yudao.module.hrm.service.payroll;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.HrmPayrollConfigGuardMapper;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
/** Same tenant lock precedes configuration, batch and employee locks; released at transaction end. */
@Service public class HrmPayrollConfigGuardService {
 @Resource private HrmPayrollConfigGuardMapper mapper;
 @Transactional(propagation=Propagation.MANDATORY)
 public void lock(){Long tenant=TenantContextHolder.getTenantId();mapper.lock(tenant==null?0L:tenant);}
}
