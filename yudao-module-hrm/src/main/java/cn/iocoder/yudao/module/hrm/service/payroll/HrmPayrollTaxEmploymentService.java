package cn.iocoder.yudao.module.hrm.service.payroll;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.PAYROLL_TAX_INVALID;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.HrmPayrollTaxProfileMapper;
import java.time.LocalDate;
import java.util.*;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class HrmPayrollTaxEmploymentService {
  @Resource private HrmPayrollEntityService entities;
  @Resource private HrmPayrollTaxProfileService profiles;
  @Resource private HrmPayrollTaxProfileMapper profileMapper;
  @Resource private HrmPayrollTaxPolicyService policies;

  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public PayrollTaxEmploymentEngine.Result deduction(
      Long entityId, Long employeeId, LocalDate payDate, Long actor) {
    entities.check(entityId, "READ", actor);
    if (payDate == null || payDate.getYear() < 2019 || payDate.getYear() > 2099)
      throw exception(PAYROLL_TAX_INVALID);
    HrmPayrollTaxProfileDO profile =
        profiles.requireApproved(entityId, employeeId, payDate.getYear());
    HrmPayrollTaxPolicyDO policy = policies.requireApproved(entityId, payDate.getYear());
    Set<Long> employeeIds = new HashSet<>();
    employeeIds.add(employeeId);
    profileMapper
        .selectList(
            new LambdaQueryWrapperX<HrmPayrollTaxProfileDO>()
                .eq(HrmPayrollTaxProfileDO::getEntityId, entityId)
                .eq(HrmPayrollTaxProfileDO::getTaxpayerKey, profile.getTaxpayerKey())
                .eq(
                    HrmPayrollTaxProfileDO::getTaxRegistrationNumber,
                    profile.getTaxRegistrationNumber())
                .in(HrmPayrollTaxProfileDO::getStatus, "APPROVED", "SUPERSEDED"))
        .forEach(p -> employeeIds.add(p.getEmployeeId()));
    return PayrollTaxEmploymentEngine.calculate(
        entityId,
        employeeIds,
        payDate,
        Boolean.TRUE.equals(profile.getFirstSalaryThisYear()),
        Boolean.TRUE.equals(profile.getNoPriorSalaryThisYear()),
        policy.getMonthlyBasicDeduction(),
        entities.effectiveMemberships(entityId, LocalDate.of(payDate.getYear(), 1, 1), payDate));
  }
}
