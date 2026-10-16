package cn.iocoder.yudao.module.hrm.service.payroll;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollBankRowVO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.employee.employment.HrmEmployeeSalaryCardDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.salary.monthrecord.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.salary.monthrecord.HrmSalaryMonthRecordMapper;
import cn.iocoder.yudao.module.hrm.service.employee.employment.HrmEmployeeSalaryCardService;
import cn.iocoder.yudao.module.hrm.service.employee.info.HrmEmployeeService;
import java.util.*;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HrmPayrollPaymentService {
  @Resource private HrmPayrollPaymentMapper paymentMapper;
  @Resource private HrmPayrollScopeService scope;
  @Resource private HrmPayrollSourceClaimService sourceClaims;
  @Resource private HrmPayrollOffsetService offsets;
  @Resource private HrmPayrollPaymentReceiptMapper receiptMapper;
  @Resource private HrmSalaryMonthRecordMapper batchMapper;
  @Resource private HrmPayrollRunMapper runMapper;
  @Resource private HrmPayrollBatchEventMapper eventMapper;
  @Resource private HrmEmployeeSalaryCardService cardService;
  @Resource private HrmEmployeeService employeeService;
  @Resource private HrmPayrollBatchService batchService;

  public cn.iocoder.yudao.framework.common.pojo.PageResult<
          cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollPaymentBatchRespVO>
      batches(
          cn.iocoder.yudao.module.hrm.controller.admin.salary.vo.monthrecord
                  .HrmSalaryMonthRecordPageReqVO
              request) {
    cn.iocoder.yudao.framework.common.pojo.PageResult<HrmSalaryMonthRecordDO> page =
        scope.page(request, true);
    List<cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollPaymentBatchRespVO>
        rows = new ArrayList<>();
    for (HrmSalaryMonthRecordDO batch : page.getList()) {
      cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollPaymentBatchRespVO row =
          BeanUtils.toBean(
              batch,
              cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollPaymentBatchRespVO
                  .class);
      HrmPayrollRunDO run =
          runMapper.selectOne(
              new LambdaQueryWrapperX<HrmPayrollRunDO>()
                  .eq(HrmPayrollRunDO::getMonthRecordId, batch.getId())
                  .orderByDesc(HrmPayrollRunDO::getVersion)
                  .last("LIMIT 1"));
      row.setRunId(run == null ? null : run.getId());
      rows.add(row);
    }
    return new cn.iocoder.yudao.framework.common.pojo.PageResult<>(rows, page.getTotal());
  }

  @Transactional(rollbackFor = Exception.class)
  public void prepare(Long batchId, Long runId, Long actor) {
    scope.check(batchMapper.selectByIdForUpdate(batchId), "APPROVE", actor);
    HrmSalaryMonthRecordDO batch = batchMapper.selectByIdForUpdate(batchId);
    if (batch == null) throw exception(SALARY_MONTH_RECORD_NOT_EXISTS);
    HrmPayrollRunDO run = runMapper.selectById(runId);
    if (run == null || !Objects.equals(run.getMonthRecordId(), batchId))
      throw exception(PAYROLL_VERSION_CONFLICT);
    sourceClaims.assertReserved(batchId, runId);
    List<HrmPayrollPaymentDO> previous = rawList(batchId);
    if (!previous.isEmpty()) {
      if (!Objects.equals(previous.get(0).getRunId(), runId))
        throw exception(PAYROLL_VERSION_CONFLICT);
      return;
    }
    if (batch.getStatus() != 15) throw exception(SALARY_MONTH_RECORD_STATUS_INVALID);
    List<HrmSalaryMonthEmployeeRecordDO> results =
        JsonUtils.parseArray(run.getResultSnapshot(), HrmSalaryMonthEmployeeRecordDO.class);
    if (results.isEmpty()) throw exception(PAYROLL_PAYMENT_INVALID);
    for (HrmSalaryMonthEmployeeRecordDO result : results) {
      if (result.getRealPaySalary() == null || result.getRealPaySalary().signum() < 0)
        throw exception(PAYROLL_PAYMENT_INVALID);
      HrmEmployeeDO employee = employeeService.getEmployee(result.getEmployeeId());
      HrmEmployeeSalaryCardDO card = cardService.getSalaryCardByEmployeeId(result.getEmployeeId());
      if (employee == null
          || (result.getRealPaySalary().signum() > 0
              && (card == null
                  || card.getBankCardNumber() == null
                  || card.getBankCardNumber().trim().isEmpty())))
        throw exception(PAYROLL_PAYMENT_INVALID);
      HrmPayrollPaymentDO payment = new HrmPayrollPaymentDO();
      payment.setMonthRecordId(batchId);
      payment.setRunId(runId);
      payment.setEmployeeId(result.getEmployeeId());
      payment.setEmployeeName(employee.getName());
      payment.setBankAccount(card == null ? "" : card.getBankCardNumber());
      payment.setBankName(card == null ? "" : card.getBankName());
      payment.setAmount(result.getRealPaySalary());
      payment.setStatus(result.getRealPaySalary().signum() == 0 ? "ZERO_PENDING" : "PENDING");
      payment.setAttempt(1);
      paymentMapper.insert(payment);
    }
    batchService.transition(batchId, runId, "pay", "生成代发明细", actor);
    completeIfPaid(batchId, runId, actor);
  }

  @Transactional(
      rollbackFor = Exception.class,
      isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
  public void settleZero(
      Long id, java.time.LocalDate date, String reference, String evidence, Long actor) {
    HrmPayrollPaymentDO candidate = paymentMapper.selectById(id);
    if (candidate == null) throw exception(PAYROLL_PAYMENT_INVALID);
    HrmSalaryMonthRecordDO batch = batchMapper.selectByIdForUpdate(candidate.getMonthRecordId());
    scope.check(batch, "APPROVE", actor);
    HrmPayrollPaymentDO payment = paymentMapper.selectOneForUpdate(HrmPayrollPaymentDO::getId, id);
    if (payment == null) throw exception(PAYROLL_PAYMENT_INVALID);
    HrmPayrollRunDO run = runMapper.selectById(payment.getRunId());
    if (run == null
        || payment.getAmount() == null
        || payment.getAmount().signum() != 0
        || date == null
        || date.isAfter(java.time.LocalDate.now(java.time.ZoneId.of("Asia/Shanghai")))
        || reference == null
        || reference.trim().isEmpty()
        || reference.length() > 100
        || evidence == null
        || evidence.trim().isEmpty()
        || evidence.length() > 1000) throw exception(PAYROLL_PAYMENT_INVALID);
    if (Objects.equals(run.getComputedBy(), actor)) throw exception(PAYROLL_SELF_APPROVAL);
    if (run.getExpectedPaymentDate() != null
        && !java.time.YearMonth.from(run.getExpectedPaymentDate())
            .equals(java.time.YearMonth.from(date))) throw exception(PAYROLL_TAX_INVALID);
    if ("SUCCESS".equals(payment.getStatus()) && payment.getZeroSettlementReference() != null) {
      if (Objects.equals(date, payment.getValueDate())
          && Objects.equals(reference, payment.getZeroSettlementReference())
          && Objects.equals(evidence, payment.getZeroSettlementEvidence())) return;
      throw exception(PAYROLL_PAYMENT_INVALID);
    }
    if (!Arrays.asList(16, 17).contains(batch.getStatus())
        || !("ZERO_PENDING".equals(payment.getStatus())
            || ("SUCCESS".equals(payment.getStatus())
                && payment.getZeroSettlementReference() == null)))
      throw exception(SALARY_MONTH_RECORD_STATUS_INVALID);
    if ("SUCCESS".equals(payment.getStatus()) && !Objects.equals(date, payment.getValueDate()))
      throw exception(PAYROLL_PAYMENT_INVALID);
    sourceClaims.assertReserved(batch.getId(), run.getId());
    payment
        .setStatus("SUCCESS")
        .setValueDate(date)
        .setZeroSettlementReference(reference)
        .setZeroSettlementEvidence(evidence)
        .setZeroSettledBy(actor)
        .setZeroSettlementTime(java.time.LocalDateTime.now(java.time.ZoneId.of("Asia/Shanghai")));
    paymentMapper.updateById(payment);
    sourceClaims.paid(batch.getId(), run.getId(), payment.getEmployeeId());
    offsets.consume(payment.getId(),actor);
    Map<String, Object> audit = new LinkedHashMap<>();
    audit.put("paymentId", payment.getId());
    audit.put("date", payment.getValueDate());
    audit.put("reference", payment.getZeroSettlementReference());
    eventMapper.insert(
        new HrmPayrollBatchEventDO()
            .setMonthRecordId(batch.getId())
            .setRunId(run.getId())
            .setAction("zero_settled")
            .setFromStatus(batch.getStatus())
            .setToStatus(batch.getStatus())
            .setActorId(actor)
            .setReason(evidence)
            .setSnapshot(JsonUtils.toJsonString(audit)));
    completeIfPaid(batch.getId(), run.getId(), actor);
  }

  /** Only zero-only preparations can be rolled back without a bank cancellation workflow. */
  @Transactional(
      rollbackFor = Exception.class,
      isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
  public void cancelZeroPreparation(Long batchId, String reason, Long actor) {
    HrmSalaryMonthRecordDO batch = batchMapper.selectByIdForUpdate(batchId);
    scope.check(batch, "APPROVE", actor);
    List<HrmPayrollPaymentDO> payments = rawList(batchId);
    if (batch.getStatus() != 16
        || payments.isEmpty()
        || payments.stream().anyMatch(p -> !"ZERO_PENDING".equals(p.getStatus()))
        || reason == null
        || reason.trim().isEmpty()
        || reason.length() > 1000) throw exception(PAYROLL_PAYMENT_INVALID);
    Long runId = payments.get(0).getRunId();
    eventMapper.insert(
        new HrmPayrollBatchEventDO()
            .setMonthRecordId(batchId)
            .setRunId(runId)
            .setAction("cancel_zero_preparation")
            .setFromStatus(16)
            .setToStatus(15)
            .setActorId(actor)
            .setReason(reason)
            .setSnapshot(
                JsonUtils.toJsonString(
                    payments.stream()
                        .map(HrmPayrollPaymentDO::getId)
                        .collect(Collectors.toList()))));
    for (HrmPayrollPaymentDO payment : payments) paymentMapper.deleteById(payment.getId());
    batchMapper.updateById(new HrmSalaryMonthRecordDO().setId(batchId).setStatus(15));
  }

  private List<HrmPayrollPaymentDO> rawList(Long batchId) {
    return paymentMapper.selectList(
        new LambdaQueryWrapperX<HrmPayrollPaymentDO>()
            .eq(HrmPayrollPaymentDO::getMonthRecordId, batchId)
            .orderByAsc(HrmPayrollPaymentDO::getId));
  }

  public List<HrmPayrollPaymentDO> list(Long batchId) {
    scope.require(batchId, "READ");
    if (batchMapper.selectById(batchId) == null) throw exception(SALARY_MONTH_RECORD_NOT_EXISTS);
    List<HrmPayrollPaymentDO> rows = rawList(batchId);
    for (HrmPayrollPaymentDO row : rows) {
      String account = row.getBankAccount();
      row.setBankAccount(
          account == null || account.length() < 4
              ? "****"
              : "****" + account.substring(account.length() - 4));
    }
    return rows;
  }

  public List<HrmPayrollBankRowVO> export(Long batchId) {
    scope.require(batchId, "EXPORT");
    HrmSalaryMonthRecordDO batch = batchMapper.selectById(batchId);
    if (batch == null || batch.getStatus() != 16)
      throw exception(SALARY_MONTH_RECORD_STATUS_INVALID);
    List<HrmPayrollPaymentDO> all = rawList(batchId);
    if (!all.isEmpty()) sourceClaims.assertReserved(batchId, all.get(0).getRunId());
    return all.stream()
        .filter(row -> "PENDING".equals(row.getStatus()) && row.getAmount().signum() > 0)
        .map(row -> BeanUtils.toBean(row, HrmPayrollBankRowVO.class))
        .collect(Collectors.toList());
  }

  public List<cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollBankDifferenceVO>
      validateRows(Long batchId, List<HrmPayrollBankRowVO> rows) {
    scope.require(batchId, "APPROVE");
    if (batchMapper.selectById(batchId) == null) throw exception(SALARY_MONTH_RECORD_NOT_EXISTS);
    Map<Long, HrmPayrollPaymentDO> payments =
        rawList(batchId).stream().collect(Collectors.toMap(HrmPayrollPaymentDO::getId, r -> r));
    List<cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollBankDifferenceVO>
        differences = new ArrayList<>();
    Set<Long> seen = new HashSet<>();
    for (int index = 0; index < rows.size(); index++) {
      HrmPayrollBankRowVO row = rows.get(index);
      HrmPayrollPaymentDO payment = payments.get(row.getId());
      String reason = null;
      if (!seen.add(row.getId())) reason = "重复的代发明细编号";
      else if (payment == null) reason = "明细不属于此发放批次";
      else if (payment.getAmount().signum() == 0) reason = "零实发须使用结算凭据确认，不导入银行回盘";
      else if (!Objects.equals(payment.getBankAccount(), row.getBankAccount())) reason = "银行账户不匹配";
      else if (row.getAmount() == null || payment.getAmount().compareTo(row.getAmount()) != 0)
        reason = "金额不匹配";
      else if (!("SUCCESS".equals(row.getStatus()) || "FAILED".equals(row.getStatus())))
        reason = "回盘状态不合法";
      else if ("SUCCESS".equals(row.getStatus())
          && (row.getValueDate() == null
              || row.getValueDate()
                  .isAfter(java.time.LocalDate.now(java.time.ZoneId.of("Asia/Shanghai")))))
        reason = "成功回盘必须记录非未来的实际到账日期";
      else if ("SUCCESS".equals(payment.getStatus())
          && payment.getValueDate() != null
          && !Objects.equals(payment.getValueDate(), row.getValueDate())) reason = "已成功到账日期不可改写";
      else if ("SUCCESS".equals(payment.getStatus()) && !"SUCCESS".equals(row.getStatus()))
        reason = "成功明细不可撤销";
      else if (!"SUCCESS".equals(payment.getStatus())
          && !Objects.equals(payment.getAttempt(), row.getAttempt())) reason = "发放次数过期，请使用本次发放的回盘";
      else if ("FAILED".equals(row.getStatus())
          && (row.getFailureReason() == null || row.getFailureReason().trim().isEmpty()))
        reason = "失败明细缺少银行原因";
      if (reason != null)
        differences.add(
            new cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollBankDifferenceVO(
                index + 2, row.getId(), reason));
    }
    return differences;
  }

  @Transactional(rollbackFor = Exception.class)
  public Long reconcileFile(
      Long batchId, String fileName, String digest, List<HrmPayrollBankRowVO> rows, Long actor) {
    scope.check(batchMapper.selectByIdForUpdate(batchId), "APPROVE", actor);
    if (batchMapper.selectByIdForUpdate(batchId) == null)
      throw exception(SALARY_MONTH_RECORD_NOT_EXISTS);
    HrmPayrollPaymentReceiptDO previous =
        receiptMapper.selectOne(
            new LambdaQueryWrapperX<HrmPayrollPaymentReceiptDO>()
                .eq(HrmPayrollPaymentReceiptDO::getMonthRecordId, batchId)
                .eq(HrmPayrollPaymentReceiptDO::getDigest, digest));
    if (previous != null) return previous.getId();
    reconcile(batchId, rows, actor);
    HrmPayrollPaymentReceiptDO receipt = new HrmPayrollPaymentReceiptDO();
    receipt.setMonthRecordId(batchId);
    receipt.setFileName(fileName);
    receipt.setDigest(digest);
    receipt.setRowCount(rows.size());
    receipt.setActorId(actor);
    receipt.setDetails(JsonUtils.toJsonString(rows));
    receiptMapper.insert(receipt);
    return receipt.getId();
  }

  public List<HrmPayrollPaymentReceiptDO> receipts(Long batchId) {
    scope.require(batchId, "READ");
    if (batchMapper.selectById(batchId) == null) throw exception(SALARY_MONTH_RECORD_NOT_EXISTS);
    List<HrmPayrollPaymentReceiptDO> rows =
        receiptMapper.selectList(
            new LambdaQueryWrapperX<HrmPayrollPaymentReceiptDO>()
                .eq(HrmPayrollPaymentReceiptDO::getMonthRecordId, batchId)
                .orderByDesc(HrmPayrollPaymentReceiptDO::getId));
    for (HrmPayrollPaymentReceiptDO row : rows) row.setDetails(null);
    return rows;
  }

  @Transactional(rollbackFor = Exception.class)
  public void reconcile(Long batchId, List<HrmPayrollBankRowVO> rows, Long actor) {
    scope.check(batchMapper.selectByIdForUpdate(batchId), "APPROVE", actor);
    HrmSalaryMonthRecordDO batch = batchMapper.selectByIdForUpdate(batchId);
    if (batch == null || !(batch.getStatus() == 16 || batch.getStatus() == 17))
      throw exception(SALARY_MONTH_RECORD_STATUS_INVALID);
    if (rows.isEmpty() || !validateRows(batchId, rows).isEmpty())
      throw exception(PAYROLL_PAYMENT_INVALID);
    Map<Long, HrmPayrollPaymentDO> payments =
        rawList(batchId).stream().collect(Collectors.toMap(HrmPayrollPaymentDO::getId, r -> r));
    Set<Long> seen = new HashSet<>();
    for (HrmPayrollBankRowVO row : rows) {
      HrmPayrollPaymentDO payment = payments.get(row.getId());
      if (!seen.add(row.getId())
          || payment == null
          || row.getAmount() == null
          || !Objects.equals(payment.getBankAccount(), row.getBankAccount())
          || payment.getAmount().compareTo(row.getAmount()) != 0
          || !("SUCCESS".equals(row.getStatus()) || "FAILED".equals(row.getStatus())))
        throw exception(PAYROLL_PAYMENT_INVALID);
      if ("SUCCESS".equals(payment.getStatus())) {
        if (!"SUCCESS".equals(row.getStatus())) throw exception(PAYROLL_PAYMENT_INVALID);
        if (payment.getValueDate() == null)
          paymentMapper.updateById(
              new HrmPayrollPaymentDO().setId(payment.getId()).setValueDate(row.getValueDate()));
        continue;
      }
      if (!Objects.equals(payment.getAttempt(), row.getAttempt()))
        throw exception(PAYROLL_PAYMENT_INVALID);
      if ("FAILED".equals(row.getStatus())
          && (row.getFailureReason() == null || row.getFailureReason().trim().isEmpty()))
        throw exception(PAYROLL_PAYMENT_INVALID);
      if ("SUCCESS".equals(row.getStatus()))
        sourceClaims.paid(batchId, payment.getRunId(), payment.getEmployeeId());
      paymentMapper.update(
          null,
          new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<
                  HrmPayrollPaymentDO>()
              .eq(HrmPayrollPaymentDO::getId, payment.getId())
              .set(HrmPayrollPaymentDO::getStatus, row.getStatus())
              .set(
                  HrmPayrollPaymentDO::getFailureReason,
                  "SUCCESS".equals(row.getStatus()) ? null : row.getFailureReason())
              .set(
                  HrmPayrollPaymentDO::getValueDate,
                  "SUCCESS".equals(row.getStatus()) ? row.getValueDate() : null));
      if("SUCCESS".equals(row.getStatus()))offsets.consume(payment.getId(),actor);
    }
    Long runId = payments.values().iterator().next().getRunId();
    HrmPayrollBatchEventDO event = new HrmPayrollBatchEventDO();
    event.setMonthRecordId(batchId);
    event.setRunId(runId);
    event.setAction("reconcile");
    event.setActorId(actor);
    event.setReason("回盘匹配 " + rows.size() + " 条");
    event.setFromStatus(batch.getStatus());
    event.setToStatus(batch.getStatus());
    eventMapper.insert(event);
    completeIfPaid(batchId, runId, actor);
  }

  @Transactional(rollbackFor = Exception.class)
  public void retry(Long id, Long actor) {
    HrmPayrollPaymentDO candidate = paymentMapper.selectById(id);
    if (candidate == null) throw exception(PAYROLL_PAYMENT_INVALID);
    HrmSalaryMonthRecordDO batch = batchMapper.selectByIdForUpdate(candidate.getMonthRecordId());
    scope.check(batch, "APPROVE", actor);
    HrmPayrollPaymentDO row = paymentMapper.selectById(id);
    if (batch.getStatus() != 16 || !"FAILED".equals(row.getStatus()))
      throw exception(PAYROLL_PAYMENT_INVALID);
    row.setStatus("PENDING");
    row.setAttempt(row.getAttempt() + 1);
    paymentMapper.updateById(row);
    HrmPayrollBatchEventDO event = new HrmPayrollBatchEventDO();
    event.setMonthRecordId(batch.getId());
    event.setRunId(row.getRunId());
    event.setAction("retry");
    event.setFromStatus(16);
    event.setToStatus(16);
    event.setActorId(actor);
    event.setReason("重试失败明细 " + id);
    eventMapper.insert(event);
  }

  private void completeIfPaid(Long batchId, Long runId, Long actor) {
    List<HrmPayrollPaymentDO> payments = rawList(batchId);
    HrmSalaryMonthRecordDO batch = batchMapper.selectById(batchId);
    if (batch.getStatus() == 16
        && !payments.isEmpty()
        && payments.stream().allMatch(p -> "SUCCESS".equals(p.getStatus()))) {
      batchMapper.updateById(new HrmSalaryMonthRecordDO().setId(batchId).setStatus(17));
      HrmPayrollBatchEventDO event = new HrmPayrollBatchEventDO();
      event.setMonthRecordId(batchId);
      event.setRunId(runId);
      event.setAction("paid");
      event.setFromStatus(16);
      event.setToStatus(17);
      event.setActorId(actor);
      event.setReason("所有明细已成功对账");
      eventMapper.insert(event);
    }
  }
}
