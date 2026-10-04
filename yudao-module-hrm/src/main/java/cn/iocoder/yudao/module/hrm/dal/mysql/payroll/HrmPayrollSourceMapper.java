package cn.iocoder.yudao.module.hrm.dal.mysql.payroll;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.HrmPayrollSourceDO;
import org.apache.ibatis.annotations.Mapper;
@Mapper
public interface HrmPayrollSourceMapper extends BaseMapperX<HrmPayrollSourceDO> {
    default HrmPayrollSourceDO selectTenantById(Long id, Long tenantId, boolean lock) {
        LambdaQueryWrapperX<HrmPayrollSourceDO> query = new LambdaQueryWrapperX<HrmPayrollSourceDO>()
                .eq(HrmPayrollSourceDO::getId, id).eq(HrmPayrollSourceDO::getTenantId, tenantId);
        if (lock) query.last("FOR UPDATE");
        return selectOne(query);
    }
    default HrmPayrollSourceDO selectTenantByCode(String code, Long tenantId) {
        return selectOne(new LambdaQueryWrapperX<HrmPayrollSourceDO>()
                .eq(HrmPayrollSourceDO::getCode, code).eq(HrmPayrollSourceDO::getTenantId, tenantId));
    }
}
