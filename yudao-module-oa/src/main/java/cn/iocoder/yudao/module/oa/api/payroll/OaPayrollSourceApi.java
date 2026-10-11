package cn.iocoder.yudao.module.oa.api.payroll;

import cn.iocoder.yudao.module.oa.api.payroll.dto.OaPayrollOvertimeDTO;

public interface OaPayrollSourceApi {
  /** Returns an approved, tenant-visible source under the caller's transaction lock. */
  OaPayrollOvertimeDTO getApprovedOvertime(Long id);
}
