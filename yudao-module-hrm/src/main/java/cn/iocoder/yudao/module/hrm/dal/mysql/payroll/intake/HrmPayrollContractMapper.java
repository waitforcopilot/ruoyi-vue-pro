package cn.iocoder.yudao.module.hrm.dal.mysql.payroll.intake;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.intake.HrmPayrollContractDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HrmPayrollContractMapper extends BaseMapperX<HrmPayrollContractDO> {
    default HrmPayrollContractDO selectTenantById(Long id, Long tenant, boolean lock) {
        LambdaQueryWrapperX<HrmPayrollContractDO> q = new LambdaQueryWrapperX<HrmPayrollContractDO>()
                .eq(HrmPayrollContractDO::getId, id).eq(HrmPayrollContractDO::getTenantId, tenant);
        if (lock) q.last("FOR UPDATE");
        return selectOne(q);
    }
    default HrmPayrollContractDO selectLatest(Long sourceId, Long tenant) {
        return selectOne(new LambdaQueryWrapperX<HrmPayrollContractDO>()
                .eq(HrmPayrollContractDO::getSourceId, sourceId).eq(HrmPayrollContractDO::getTenantId, tenant)
                .orderByDesc(HrmPayrollContractDO::getContractVersion).last("LIMIT 1"));
    }
}
