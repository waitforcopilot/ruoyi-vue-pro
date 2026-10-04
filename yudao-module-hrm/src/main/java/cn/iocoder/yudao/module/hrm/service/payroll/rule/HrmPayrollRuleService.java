package cn.iocoder.yudao.module.hrm.service.payroll.rule;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.rule.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.HrmPayrollReviewDO;
import javax.validation.Valid;
import java.util.List;
public interface HrmPayrollRuleService {
    Integer initialize();
    Long create(@Valid HrmPayrollRuleSaveReqVO request);
    Long newVersion(Long id, Integer revision);
    void update(@Valid HrmPayrollRuleSaveReqVO request);
    void review(@Valid HrmPayrollRuleReviewReqVO request);
    HrmPayrollRuleRespVO get(Long id);
    PageResult<HrmPayrollRuleRespVO> page(@Valid HrmPayrollRulePageReqVO query);
    List<HrmPayrollReviewDO> history(Long id);
}
