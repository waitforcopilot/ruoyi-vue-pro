package cn.iocoder.yudao.module.hrm.dal.mysql.salary.slip;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.hrm.dal.dataobject.salary.slip.HrmSalarySlipDeliveryDO;
import org.apache.ibatis.annotations.*;

@Mapper
public interface HrmSalarySlipDeliveryMapper extends BaseMapperX<HrmSalarySlipDeliveryDO> {
  @Select("SELECT * FROM hrm_salary_slip_delivery WHERE id=#{id} AND deleted=0 FOR UPDATE")
  HrmSalarySlipDeliveryDO lock(Long id);
}
