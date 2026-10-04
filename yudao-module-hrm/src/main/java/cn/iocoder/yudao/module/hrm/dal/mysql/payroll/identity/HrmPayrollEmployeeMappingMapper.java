package cn.iocoder.yudao.module.hrm.dal.mysql.payroll.identity;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.identity.HrmPayrollEmployeeMappingDO;
import org.apache.ibatis.annotations.Mapper;
@Mapper
public interface HrmPayrollEmployeeMappingMapper extends BaseMapperX<HrmPayrollEmployeeMappingDO> {
    default HrmPayrollEmployeeMappingDO root(String key,Long tenant,boolean lock) {
        LambdaQueryWrapperX<HrmPayrollEmployeeMappingDO> q=new LambdaQueryWrapperX<HrmPayrollEmployeeMappingDO>()
                .eq(HrmPayrollEmployeeMappingDO::getTenantId,tenant).eq(HrmPayrollEmployeeMappingDO::getIdentityKey,key)
                .eq(HrmPayrollEmployeeMappingDO::getMappingVersion,1);
        if(lock) q.last("FOR UPDATE");
        return selectOne(q);
    }
}
