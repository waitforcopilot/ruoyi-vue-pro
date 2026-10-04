package cn.iocoder.yudao.module.hrm.service.payroll.intake;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.intake.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.HrmPayrollReviewDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.HrmPayrollSourceDO;
import javax.validation.Valid;
import java.util.List;

public interface HrmPayrollIntakeService {
    List<HrmPayrollSourceDO> sourceOptions();
    Long createContract(@Valid HrmPayrollContractSaveReqVO request);
    void updateContract(@Valid HrmPayrollContractSaveReqVO request);
    void reviewContract(@Valid HrmPayrollContractReviewReqVO request);
    HrmPayrollContractRespVO getContract(Long id);
    PageResult<HrmPayrollContractRespVO> contracts(@Valid HrmPayrollContractPageReqVO query);
    List<HrmPayrollReviewDO> history(Long id);
    byte[] template(Long id);
    Long preview(@Valid HrmPayrollPreviewReqVO request, String fileName, byte[] bytes);
    PageResult<HrmPayrollBatchRespVO> batches(@Valid HrmPayrollBatchPageReqVO query);
    HrmPayrollBatchDetailRespVO batch(Long id);
}
