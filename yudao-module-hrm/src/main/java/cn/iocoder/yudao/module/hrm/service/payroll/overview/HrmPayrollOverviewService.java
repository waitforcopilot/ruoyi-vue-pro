package cn.iocoder.yudao.module.hrm.service.payroll.overview;

import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.overview.*;

public interface HrmPayrollOverviewService {
    HrmPayrollOverviewRespVO page(HrmPayrollOverviewPageReqVO req);

    HrmPayrollOverviewRespVO.Detail detail(Long batchId);
}
