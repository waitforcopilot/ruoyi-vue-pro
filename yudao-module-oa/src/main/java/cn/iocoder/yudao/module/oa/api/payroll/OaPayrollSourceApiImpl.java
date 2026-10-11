package cn.iocoder.yudao.module.oa.api.payroll;

import cn.iocoder.yudao.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import cn.iocoder.yudao.module.oa.api.payroll.dto.OaPayrollOvertimeDTO;
import cn.iocoder.yudao.module.oa.dal.dataobject.overtime.OaOvertimeApplyDO;
import cn.iocoder.yudao.module.oa.dal.mysql.overtime.OaOvertimeApplyMapper;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class OaPayrollSourceApiImpl implements OaPayrollSourceApi {
  @Resource private OaOvertimeApplyMapper mapper;

  @Override
  public OaPayrollOvertimeDTO getApprovedOvertime(Long id) {
    OaOvertimeApplyDO source = mapper.selectOneForUpdate(OaOvertimeApplyDO::getId, id);
    if (source == null
        || !BpmProcessInstanceStatusEnum.APPROVE.getStatus().equals(source.getStatus())
        || source.getProcessInstanceId() == null
        || source.getProcessInstanceId().trim().isEmpty()) return null;
    Long userId;
    try {
      userId = Long.valueOf(source.getCreator());
    } catch (NumberFormatException ex) {
      return null;
    }
    OaPayrollOvertimeDTO result = new OaPayrollOvertimeDTO();
    result.setId(source.getId());
    result.setApplicantUserId(userId);
    result.setStartTime(source.getStartTime());
    result.setEndTime(source.getEndTime());
    result.setProcessInstanceId(source.getProcessInstanceId());
    return result;
  }
}
