package cn.iocoder.yudao.module.hrm.service.payroll.scheme;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.scheme.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.HrmPayrollReviewDO;
import java.time.LocalDate;
import java.util.List;
public interface HrmPayrollSchemeService {
    List<HrmPayrollSchemeSnapshotVO.Group> groups(String search);
    HrmPayrollSchemeRespVO capture(Long groupId);
    Long create(HrmPayrollSchemeSaveReqVO request);
    void update(HrmPayrollSchemeSaveReqVO request);
    Long newVersion(Long id,Integer revision);
    void review(HrmPayrollSchemeReviewReqVO request);
    HrmPayrollSchemeRespVO get(Long id);
    PageResult<HrmPayrollSchemeRespVO> page(HrmPayrollSchemePageReqVO request);
    List<HrmPayrollReviewDO> history(Long id);
    HrmPayrollSchemeCompareVO compare(Long leftId,Long rightId);
    HrmPayrollSchemeRespVO resolve(Long groupId,LocalDate start,LocalDate end);
}
