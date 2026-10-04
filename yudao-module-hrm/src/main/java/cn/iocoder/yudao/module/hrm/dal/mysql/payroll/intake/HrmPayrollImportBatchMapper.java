package cn.iocoder.yudao.module.hrm.dal.mysql.payroll.intake;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.intake.HrmPayrollImportBatchDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HrmPayrollImportBatchMapper extends BaseMapperX<HrmPayrollImportBatchDO> {
    default HrmPayrollImportBatchDO selectOwned(Long id, Long tenant, Long user) {
        return selectOne(new LambdaQueryWrapperX<HrmPayrollImportBatchDO>()
                .eq(HrmPayrollImportBatchDO::getId, id).eq(HrmPayrollImportBatchDO::getTenantId, tenant)
                .eq(HrmPayrollImportBatchDO::getCreatedBy, user));
    }
    default HrmPayrollImportBatchDO selectIdempotent(String key, Long tenant, Long user) {
        return selectOne(new LambdaQueryWrapperX<HrmPayrollImportBatchDO>()
                .eq(HrmPayrollImportBatchDO::getIdempotencyKey, key).eq(HrmPayrollImportBatchDO::getTenantId, tenant)
                .eq(HrmPayrollImportBatchDO::getCreatedBy, user));
    }
}
