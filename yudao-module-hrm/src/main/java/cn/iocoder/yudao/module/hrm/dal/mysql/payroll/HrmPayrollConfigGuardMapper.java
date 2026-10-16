package cn.iocoder.yudao.module.hrm.dal.mysql.payroll;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.HrmPayrollConfigGuardDO;
import org.apache.ibatis.annotations.*;
@Mapper public interface HrmPayrollConfigGuardMapper extends BaseMapperX<HrmPayrollConfigGuardDO> {
 @Insert("INSERT INTO hrm_payroll_config_guard (id,tenant_id) VALUES (1,#{tenantId}) ON DUPLICATE KEY UPDATE id=id")
 void lock(@Param("tenantId") Long tenantId);
}
