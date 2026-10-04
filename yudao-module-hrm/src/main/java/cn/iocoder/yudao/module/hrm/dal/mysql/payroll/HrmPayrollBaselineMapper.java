package cn.iocoder.yudao.module.hrm.dal.mysql.payroll;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.HrmPayrollBaselineDO;
import org.apache.ibatis.annotations.Mapper;
@Mapper
public interface HrmPayrollBaselineMapper extends BaseMapperX<HrmPayrollBaselineDO> {
    default HrmPayrollBaselineDO selectTenantById(Long id, Long tenantId, boolean lock) {
        LambdaQueryWrapperX<HrmPayrollBaselineDO> query = new LambdaQueryWrapperX<HrmPayrollBaselineDO>()
                .eq(HrmPayrollBaselineDO::getId, id).eq(HrmPayrollBaselineDO::getTenantId, tenantId);
        if (lock) query.last("FOR UPDATE");
        return selectOne(query);
    }
}
