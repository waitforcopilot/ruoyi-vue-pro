package cn.iocoder.yudao.module.hrm.dal.mysql.payroll.scheme;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.scheme.HrmPayrollSchemeDO;
import org.apache.ibatis.annotations.*;
@Mapper
public interface HrmPayrollSchemeMapper extends BaseMapperX<HrmPayrollSchemeDO> {
    // Keep the source-group lock even for a soft-deleted group's retained history.
    @Select("SELECT id FROM hrm_salary_group WHERE id=#{id} AND tenant_id=#{tenant} FOR UPDATE")
    Long lockGroup(@Param("id") Long id,@Param("tenant") Long tenant);
}
