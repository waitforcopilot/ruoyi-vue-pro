package cn.iocoder.yudao.module.hrm.dal.mysql.payroll;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.HrmPayrollReviewDO;
import org.apache.ibatis.annotations.Mapper;
@Mapper
public interface HrmPayrollReviewMapper extends BaseMapperX<HrmPayrollReviewDO> {
    default HrmPayrollReviewDO selectTenantById(Long id, Long tenantId, boolean lock) {
        LambdaQueryWrapperX<HrmPayrollReviewDO> query = new LambdaQueryWrapperX<HrmPayrollReviewDO>()
                .eq(HrmPayrollReviewDO::getId, id).eq(HrmPayrollReviewDO::getTenantId, tenantId);
        if (lock) query.last("FOR UPDATE");
        return selectOne(query);
    }
}
