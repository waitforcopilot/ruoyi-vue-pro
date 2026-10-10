package cn.iocoder.yudao.module.hrm.service.payroll;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollRequirementSaveReqVO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import java.util.List;
public interface HrmPayrollRequirementService {
    List<HrmPayrollRequirementDO> list(String module);
    Long save(HrmPayrollRequirementSaveReqVO request, Long actorId);
    List<HrmPayrollRequirementHistoryDO> history(Long id);
    int initialize(Long actorId);
}
