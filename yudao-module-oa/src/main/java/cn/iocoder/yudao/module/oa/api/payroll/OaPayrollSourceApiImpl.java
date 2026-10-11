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
  @Resource private cn.iocoder.yudao.module.oa.dal.mysql.travel.OaTravelApplyMapper travelMapper;

  @Override
  public java.util.List<cn.iocoder.yudao.module.oa.api.payroll.dto.OaPayrollAttendanceDTO>
      getApprovedAttendance(
          java.util.Collection<Long> userIds,
          java.time.LocalDateTime from,
          java.time.LocalDateTime to) {
    if (userIds == null || userIds.isEmpty()) return java.util.Collections.emptyList();
    java.util.List<String> creators =
        userIds.stream().map(String::valueOf).collect(java.util.stream.Collectors.toList());
    java.util.List<cn.iocoder.yudao.module.oa.dal.dataobject.travel.OaTravelApplyDO> rows =
        travelMapper.selectList(
            new cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX<
                    cn.iocoder.yudao.module.oa.dal.dataobject.travel.OaTravelApplyDO>()
                .in(
                    cn.iocoder.yudao.module.oa.dal.dataobject.travel.OaTravelApplyDO::getCreator,
                    creators)
                .eq(
                    cn.iocoder.yudao.module.oa.dal.dataobject.travel.OaTravelApplyDO::getStatus,
                    BpmProcessInstanceStatusEnum.APPROVE.getStatus())
                .lt(
                    cn.iocoder.yudao.module.oa.dal.dataobject.travel.OaTravelApplyDO::getStartTime,
                    to)
                .gt(
                    cn.iocoder.yudao.module.oa.dal.dataobject.travel.OaTravelApplyDO::getEndTime,
                    from)
                .orderByAsc(
                    cn.iocoder.yudao.module.oa.dal.dataobject.travel.OaTravelApplyDO::getId));
    java.util.List<cn.iocoder.yudao.module.oa.api.payroll.dto.OaPayrollAttendanceDTO> result =
        new java.util.ArrayList<>();
    for (cn.iocoder.yudao.module.oa.dal.dataobject.travel.OaTravelApplyDO row : rows) {
      if (row.getProcessInstanceId() == null
          || row.getProcessInstanceId().trim().isEmpty()
          || !row.getEndTime().isAfter(row.getStartTime())) continue;
      cn.iocoder.yudao.module.oa.api.payroll.dto.OaPayrollAttendanceDTO item =
          new cn.iocoder.yudao.module.oa.api.payroll.dto.OaPayrollAttendanceDTO();
      item.setId(row.getId());
      item.setApplicantUserId(Long.valueOf(row.getCreator()));
      item.setType("BUSINESS_TRIP");
      item.setStartTime(row.getStartTime());
      item.setEndTime(row.getEndTime());
      item.setProcessInstanceId(row.getProcessInstanceId());
      result.add(item);
    }
    return result;
  }

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
