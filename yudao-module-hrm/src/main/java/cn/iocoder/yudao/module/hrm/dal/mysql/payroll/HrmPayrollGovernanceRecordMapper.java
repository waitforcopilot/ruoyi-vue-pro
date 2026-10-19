package cn.iocoder.yudao.module.hrm.dal.mysql.payroll;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.HrmPayrollGovernanceRecordDO;
import org.apache.ibatis.annotations.*;
import java.util.*;

@Mapper
public interface HrmPayrollGovernanceRecordMapper extends BaseMapperX<HrmPayrollGovernanceRecordDO> {
    @Select("SELECT id,nickname,status FROM system_users WHERE deleted=0 ORDER BY id FOR UPDATE")
    List<Map<String,Object>> lockUserFacts();
}
