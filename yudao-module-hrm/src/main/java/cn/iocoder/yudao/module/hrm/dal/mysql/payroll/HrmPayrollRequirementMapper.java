package cn.iocoder.yudao.module.hrm.dal.mysql.payroll;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.HrmPayrollRequirementDO;
import org.apache.ibatis.annotations.Mapper;
@Mapper
public interface HrmPayrollRequirementMapper extends BaseMapperX<HrmPayrollRequirementDO> {
    default HrmPayrollRequirementDO selectTenantById(Long id, Long tenantId, boolean lock) {
        LambdaQueryWrapperX<HrmPayrollRequirementDO> query = new LambdaQueryWrapperX<HrmPayrollRequirementDO>()
                .eq(HrmPayrollRequirementDO::getId, id).eq(HrmPayrollRequirementDO::getTenantId, tenantId);
        if (lock) query.last("FOR UPDATE");
        return selectOne(query);
    }
    default HrmPayrollRequirementDO selectTenantByCode(String code, Long tenantId) {
        return selectOne(new LambdaQueryWrapperX<HrmPayrollRequirementDO>()
                .eq(HrmPayrollRequirementDO::getCode, code).eq(HrmPayrollRequirementDO::getTenantId, tenantId));
    }
}
