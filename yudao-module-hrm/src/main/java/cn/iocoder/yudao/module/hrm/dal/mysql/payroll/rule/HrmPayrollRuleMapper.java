package cn.iocoder.yudao.module.hrm.dal.mysql.payroll.rule;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.rule.HrmPayrollRuleDO;
import org.apache.ibatis.annotations.Mapper;
@Mapper
public interface HrmPayrollRuleMapper extends BaseMapperX<HrmPayrollRuleDO> {
    default HrmPayrollRuleDO selectTenantById(Long id, Long tenant, boolean lock) {
        LambdaQueryWrapperX<HrmPayrollRuleDO> query=new LambdaQueryWrapperX<HrmPayrollRuleDO>()
                .eq(HrmPayrollRuleDO::getId,id).eq(HrmPayrollRuleDO::getTenantId,tenant);
        if(lock) query.last("FOR UPDATE");
        return selectOne(query);
    }
    default HrmPayrollRuleDO selectRoot(String code, Long tenant, boolean lock) {
        LambdaQueryWrapperX<HrmPayrollRuleDO> query=new LambdaQueryWrapperX<HrmPayrollRuleDO>()
                .eq(HrmPayrollRuleDO::getTenantId,tenant).eq(HrmPayrollRuleDO::getCode,code).eq(HrmPayrollRuleDO::getRuleVersion,1);
        // Version 1 is retained as the unique lock row. Avoid ORDER/LIMIT with FOR UPDATE:
        // this project's tenant SQL parser reorders that combination on MySQL.
        if(lock) query.last("FOR UPDATE");
        return selectOne(query);
    }
}
