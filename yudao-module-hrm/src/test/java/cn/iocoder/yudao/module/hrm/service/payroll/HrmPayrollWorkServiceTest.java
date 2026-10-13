package cn.iocoder.yudao.module.hrm.service.payroll;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.attendance.config.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.attendance.record.HrmAttendanceClockDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.attendance.config.HrmAttendanceHolidayMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.attendance.record.HrmAttendanceClockMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.employee.info.HrmEmployeeMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.*;
import cn.iocoder.yudao.module.hrm.service.attendance.config.HrmAttendanceGroupService;
import cn.iocoder.yudao.module.oa.api.payroll.OaPayrollSourceApi;
import cn.iocoder.yudao.module.oa.api.payroll.dto.OaPayrollOvertimeDTO;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import javax.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

@Import({
  HrmPayrollWorkService.class,
  org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration.class
})
class HrmPayrollWorkServiceTest extends BaseDbUnitTest {
  @Resource private HrmPayrollWorkService service;
  @Resource private HrmEmployeeMapper employees;
  @Resource private HrmPayrollWorkRuleMapper rules;
  @Resource private HrmPayrollTimeEntryMapper entries;
  @Resource private HrmPayrollTimeOffMapper off;
  @Resource private HrmPayrollNoOvertimeMapper zeroCases;
  @Resource private HrmAttendanceClockMapper clocks;
  @Resource private HrmAttendanceHolidayMapper holidays;
  @MockBean private OaPayrollSourceApi oa;
  @MockBean private HrmPayrollEntityService entities;
  @MockBean private HrmAttendanceGroupService groups;
  private Long employeeId;
  private final LocalDate day = LocalDate.of(2026, 9, 10);

  @BeforeEach
  void fixtures() {
    HrmEmployeeDO employee =
        new HrmEmployeeDO().setName("工时员工").setJobNumber("WORK-1").setUserId(99L);
    employees.insert(employee);
    employeeId = employee.getId();
    when(entities.effectiveMemberships(eq(10L), any(), any()))
        .thenReturn(
            Collections.singletonList(
                new HrmPayrollEntityMembershipDO()
                    .setEmployeeId(employeeId)
                    .setEffectiveFrom(LocalDate.of(2026, 1, 1))
                    .setEffectiveTo(LocalDate.of(2026, 12, 31))));
    when(groups.getAttendanceGroupMap(anyCollection()))
        .thenReturn(Collections.singletonMap(employeeId, new HrmAttendanceGroupDO()));
    when(groups.getEmployeeAttendanceShift(eq(employeeId), any()))
        .thenReturn(
            HrmAttendanceGroupDO.Shift.builder()
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(17, 0))
                .build());
  }

  private HrmPayrollWorkRuleReqVO policy(String type) {
    HrmPayrollWorkRuleReqVO r = new HrmPayrollWorkRuleReqVO();
    r.setEmployeeId(employeeId);
    r.setEntityId(10L);
    r.setSystemType(type);
    r.setEffectiveFrom(day.withDayOfMonth(1));
    r.setEffectiveTo(day.withDayOfMonth(1).plusMonths(3).minusDays(1));
    r.setLegalBasis("工时制度批准文件编号");
    r.setCycleMinutes(100);
    r.setCycleMonths(1);
    r.setTimeOffExpiryDays(90);
    r.setLimitMinutes(2160);
    r.setHourlyBase(new BigDecimal("20"));
    r.setWorkdayRate(new BigDecimal("1.5"));
    r.setRestdayRate(new BigDecimal("2"));
    r.setHolidayRate(new BigDecimal("3"));
    return r;
  }

  private Long approved(String type) {
    Long id = service.createRule(policy(type), 1L);
    HrmPayrollWorkRuleDO r = rules.selectById(id);
    r.setCreator("1");
    rules.updateById(r);
    service.approveRule(id, 2L);
    return id;
  }

  private HrmPayrollTimeEntryReqVO request(LocalDateTime start, LocalDateTime end) {
    OaPayrollOvertimeDTO source = new OaPayrollOvertimeDTO();
    source.setId(7L);
    source.setApplicantUserId(99L);
    source.setStartTime(start);
    source.setEndTime(end);
    source.setProcessInstanceId("approved-flow");
    when(oa.getApprovedOvertime(7L)).thenReturn(source);
    HrmAttendanceClockDO in =
        HrmAttendanceClockDO.builder()
            .employeeId(employeeId)
            .type(1)
            .status(0)
            .clockTime(start)
            .attendanceTime(start)
            .build();
    clocks.insert(in);
    HrmAttendanceClockDO out =
        HrmAttendanceClockDO.builder()
            .employeeId(employeeId)
            .type(2)
            .status(0)
            .clockTime(end)
            .attendanceTime(end)
            .build();
    clocks.insert(out);
    HrmPayrollTimeEntryReqVO r = new HrmPayrollTimeEntryReqVO();
    r.setEmployeeId(employeeId);
    r.setEntityId(10L);
    r.setSourceSystem("OA");
    r.setSourceRecordId("7");
    r.setClockInId(in.getId());
    r.setClockOutId(out.getId());
    r.setEvidence("核对已批准加班");
    r.setStartTime(start);
    r.setEndTime(end);
    r.setBreakMinutes(0);
    r.setDayType("HOLIDAY");
    r.setCompensation("PAY");
    return r;
  }

  @Test
  void policyApprovalAndStatutoryMinimum() {
    HrmPayrollWorkRuleReqVO p = policy("STANDARD");
    p.setHolidayRate(BigDecimal.ONE);
    assertServiceException(() -> service.createRule(p, 1L), PAYROLL_WORK_INVALID);
    Long id = service.createRule(policy("STANDARD"), 1L);
    HrmPayrollWorkRuleDO r = rules.selectById(id);
    r.setCreator("1");
    rules.updateById(r);
    assertServiceException(() -> service.approveRule(id, 1L), PAYROLL_SELF_APPROVAL);
    service.approveRule(id, 2L);
    assertEquals("APPROVED", rules.selectById(id).getStatus());
  }

  @Test
  void authoritativeClockAndCalendarOverrideClient() {
    approved("STANDARD");
    HrmPayrollTimeEntryReqVO r = request(day.atTime(18, 0), day.atTime(20, 0));
    r.setClockEnd(day.atTime(23, 0));
    Long id = service.confirm(r, 2L).get(0);
    HrmPayrollTimeEntryDO e = entries.selectById(id);
    assertEquals(120, e.getRecognizedMinutes());
    assertEquals("WORKDAY", e.getDayType());
    assertEquals(0, new BigDecimal("60").compareTo(e.getAmount()));
    assertTrue(e.getSourceSnapshot().contains("clockInId"));
    assertServiceException(() -> service.confirm(r, 2L), PAYROLL_WORK_INVALID);
  }

  @Test
  void wrongApplicantAndPendingApprovalCannotPay() {
    approved("STANDARD");
    HrmPayrollTimeEntryReqVO r = request(day.atTime(18, 0), day.atTime(20, 0));
    OaPayrollOvertimeDTO source = oa.getApprovedOvertime(7L);
    source.setApplicantUserId(100L);
    assertServiceException(() -> service.confirm(r, 2L), PAYROLL_WORK_INVALID);
    when(oa.getApprovedOvertime(7L)).thenReturn(null);
    assertServiceException(() -> service.confirm(r, 2L), PAYROLL_WORK_INVALID);
    assertEquals(0L, entries.selectCount());
  }

  @Test
  void crossMidnightSplitsAndUsesEachDaysClassification() {
    approved("STANDARD");
    HrmPayrollTimeEntryReqVO r = request(day.atTime(23, 0), day.plusDays(1).atTime(1, 0));
    HrmAttendanceHolidayDO holiday = new HrmAttendanceHolidayDO();
    holiday.setDate(day.plusDays(1).atStartOfDay());
    holiday.setType(3);
    holidays.insert(holiday);
    List<Long> ids = service.confirm(r, 2L);
    assertEquals(2, ids.size());
    assertEquals(0, new BigDecimal("30").compareTo(entries.selectById(ids.get(0)).getAmount()));
    assertEquals(0, new BigDecimal("60").compareTo(entries.selectById(ids.get(1)).getAmount()));
  }

  @Test
  void sourceChangedAfterConfirmationBlocksCalculation() {
    approved("STANDARD");
    HrmPayrollTimeEntryReqVO r = request(day.atTime(18, 0), day.atTime(20, 0));
    service.confirm(r, 2L);
    HrmAttendanceClockDO out = clocks.selectById(r.getClockOutId());
    out.setClockTime(day.atTime(21, 0));
    clocks.updateById(out);
    assertServiceException(() -> service.earnings(employeeId, day, day), PAYROLL_WORK_INVALID);
  }

  @Test
  void onlyRestDaysCanConvertToTimeOff() {
    approved("STANDARD");
    HrmPayrollTimeEntryReqVO r = request(day.atTime(18, 0), day.atTime(20, 0));
    r.setCompensation("TIME_OFF");
    assertServiceException(() -> service.confirm(r, 2L), PAYROLL_WORK_INVALID);
    when(groups.getEmployeeAttendanceShift(eq(employeeId), any())).thenReturn(null);
    Long id = service.confirm(r, 2L).get(0);
    assertEquals(0, entries.selectById(id).getAmount().signum());
    assertEquals(120, service.timeOffBalance(employeeId));
    Long debit = service.useTimeOff(employeeId, 90, "LEAVE-1", "调休申请占用", 2L);
    assertEquals(debit, service.useTimeOff(employeeId, 90, "LEAVE-1", "重试", 2L));
    assertEquals(30, service.timeOffBalance(employeeId));
    service.consumeTimeOff(debit, 2L);
    Long refund = service.refundTimeOff(debit, "ignored", "撤销调休", 2L);
    assertEquals(refund, service.refundTimeOff(debit, "ignored2", "重试", 2L));
    assertEquals(120, service.timeOffBalance(employeeId));
    assertServiceException(() -> service.consumeTimeOff(debit, 2L), PAYROLL_WORK_INVALID);
  }

  @Test
  void expiredRefundCannotExtendOriginalEntitlement() {
    HrmPayrollTimeOffDO credit = new HrmPayrollTimeOffDO();
    credit.setEmployeeId(employeeId);
    credit.setMinutes(120);
    credit.setStatus("CREDIT");
    credit.setExpiresOn(LocalDate.now(ZoneId.of("Asia/Shanghai")).plusDays(1));
    credit.setRequestKey("lot");
    off.insert(credit);
    Long debit = service.useTimeOff(employeeId, 120, "leave", "占用", 2L);
    credit.setExpiresOn(LocalDate.now(ZoneId.of("Asia/Shanghai")).minusDays(1));
    off.updateById(credit);
    assertServiceException(() -> service.consumeTimeOff(debit, 2L), PAYROLL_WORK_INVALID);
    service.refundTimeOff(debit, "refund", "退回", 2L);
    assertEquals(0, service.timeOffBalance(employeeId));
  }

  @Test
  void comprehensiveCycleDefersDailyPayAndCapsByApprovedExcess() {
    Long rule = approved("COMPREHENSIVE");
    HrmPayrollTimeEntryReqVO r = request(day.atTime(18, 0), day.atTime(20, 0));
    Long id = service.confirm(r, 2L).get(0);
    assertEquals(0, entries.selectById(id).getAmount().signum());
    Long cycle = service.settleCycle(rule, day.withDayOfMonth(1), 2L);
    assertEquals(cycle, service.settleCycle(rule, day.withDayOfMonth(1), 2L));
    assertEquals(
        0,
        new BigDecimal("10")
            .compareTo(
                service.earnings(employeeId, day.withDayOfMonth(1), day.withDayOfMonth(30))));
    assertEquals(20, HrmPayrollWorkService.cyclePayableMinutes(120, 100, 120));
  }

  @Test
  void irregularNonHolidayDoesNotUseStandardOvertimeMultiplier() {
    approved("IRREGULAR");
    HrmPayrollTimeEntryReqVO r = request(day.atTime(18, 0), day.atTime(20, 0));
    assertEquals(0, entries.selectById(service.confirm(r, 2L).get(0)).getAmount().signum());
  }

  @Test
  void endedCycleMustBeSettledBeforePayroll() {
    approved("COMPREHENSIVE");
    assertServiceException(
        () -> service.earnings(employeeId, day.withDayOfMonth(1), day.withDayOfMonth(30)),
        PAYROLL_WORK_INVALID);
  }

  @Test
  void approvedRegularShiftCannotAlsoBePaidAsOvertime() {
    approved("STANDARD");
    HrmPayrollTimeEntryReqVO r = request(day.atTime(9, 0), day.atTime(11, 0));
    assertServiceException(() -> service.confirm(r, 2L), PAYROLL_WORK_INVALID);
    assertEquals(0L, entries.selectCount());
  }

  @Test
  void membershipMustCoverEveryRuleDateAndApprovalRechecksIt() {
    when(entities.effectiveMemberships(eq(10L), any(), any()))
        .thenReturn(
            Collections.singletonList(
                new HrmPayrollEntityMembershipDO()
                    .setEmployeeId(employeeId)
                    .setEffectiveFrom(day)
                    .setEffectiveTo(day.plusDays(1))));
    assertServiceException(() -> service.createRule(policy("STANDARD"), 1L), PAYROLL_WORK_INVALID);
    assertTrue(rules.selectList().isEmpty());
  }

  @Test
  void foreignEmployerRuleCannotAuthorizeRecognition() {
    approved("STANDARD");
    when(entities.effectiveMemberships(eq(20L), any(), any()))
        .thenReturn(
            Collections.singletonList(
                new HrmPayrollEntityMembershipDO()
                    .setEmployeeId(employeeId)
                    .setEffectiveFrom(LocalDate.of(2026, 1, 1))
                    .setEffectiveTo(LocalDate.of(2026, 12, 31))));
    HrmPayrollTimeEntryReqVO r = request(day.atTime(18, 0), day.atTime(20, 0));
    r.setEntityId(20L);
    assertServiceException(() -> service.confirm(r, 2L), PAYROLL_WORK_INVALID);
    assertTrue(entries.selectList().isEmpty());
  }

  @Test
  void rejectedRuleRetainsReasonAndAllowsFreshApplication() {
    Long id = service.createRule(policy("STANDARD"), 1L);
    HrmPayrollWorkRuleDO r = rules.selectById(id);
    r.setCreator("1");
    rules.updateById(r);
    assertServiceException(() -> service.rejectRule(id, "不适用", 1L), PAYROLL_SELF_APPROVAL);
    service.rejectRule(id, "批准依据错误", 2L);
    assertEquals("批准依据错误", rules.selectById(id).getRejectionReason());
    assertNotEquals(id, service.createRule(policy("STANDARD"), 1L));
    assertEquals(2, rules.selectList().size());
  }

  @Test
  void employerScopedEarningsIgnoreForeignAndLegacyUnboundAmounts() {
    HrmPayrollTimeEntryDO foreign = new HrmPayrollTimeEntryDO();
    foreign.setEmployeeId(employeeId);
    foreign.setEntityId(20L);
    foreign.setSourceRecordId("FOREIGN");
    foreign.setStartTime(day.atTime(18, 0));
    foreign.setAmount(new BigDecimal("999"));
    entries.insert(foreign);
    HrmPayrollTimeEntryDO legacy = new HrmPayrollTimeEntryDO();
    legacy.setEmployeeId(employeeId);
    legacy.setSourceRecordId("LEGACY");
    legacy.setStartTime(day.atTime(18, 0));
    legacy.setAmount(new BigDecimal("888"));
    entries.insert(legacy);
    assertEquals(0, service.earningsForEntity(employeeId, 10L, day, day).signum());
    assertTrue(service.scopedRules(10L, employeeId, 2L).isEmpty());
  }

  @Test
  void employerPermissionIsCheckedBeforeCreatingRules() {
    doThrow(new IllegalStateException("forbidden")).when(entities).check(10L, "MANAGE", 1L);
    assertThrows(IllegalStateException.class, () -> service.createRule(policy("STANDARD"), 1L));
    assertTrue(rules.selectList().isEmpty());
  }

  private HrmPayrollNoOvertimeReqVO zeroRequest(String key) {
    OaPayrollOvertimeDTO source = new OaPayrollOvertimeDTO();
    source.setId(77L);
    source.setApplicantUserId(99L);
    source.setStartTime(day.atTime(18, 0));
    source.setEndTime(day.atTime(20, 0));
    source.setProcessInstanceId("approved-zero-flow");
    when(oa.getApprovedOvertime(77L)).thenReturn(source);
    HrmPayrollNoOvertimeReqVO r = new HrmPayrollNoOvertimeReqVO();
    r.setEntityId(10L);
    r.setEmployeeId(employeeId);
    r.setSourceApplyId(77L);
    r.setWorkDate(day);
    r.setRequestKey(key);
    r.setReason("生产任务取消，未实际加班");
    r.setEvidence("主管确认单编号ZERO-1");
    return r;
  }

  @Test
  void approvedZeroDeclarationCreatesSourceOnceAndKeepsProof() {
    approved("STANDARD");
    HrmPayrollNoOvertimeReqVO r = zeroRequest("zero-1");
    Long id = service.submitNoOvertime(r, 1L);
    assertEquals(id, service.submitNoOvertime(r, 1L));
    assertTrue(entries.selectList().isEmpty());
    assertServiceException(
        () -> service.reviewNoOvertime(id, true, "同意", 1L), PAYROLL_SELF_APPROVAL);
    assertServiceException(
        () -> service.reviewNoOvertime(id, true, "本人同意", 99L), PAYROLL_SELF_APPROVAL);
    service.reviewNoOvertime(id, true, "核对任务取消证明，确认未发生", 2L);
    service.reviewNoOvertime(id, true, "重复回调", 2L);
    HrmPayrollTimeEntryDO entry = entries.selectById(zeroCases.selectById(id).getTimeEntryId());
    assertEquals("VERIFIED_ZERO", entry.getCompensation());
    assertEquals(0, entry.getRecognizedMinutes());
    assertEquals(0, entry.getAmount().signum());
    assertEquals(77L, entry.getSourceApplyId());
    assertTrue(entry.getSourceSnapshot().contains("ZERO-1"));
    assertEquals(1, entries.selectList().size());
    assertEquals(0, service.earningsForEntity(employeeId, 10L, day, day).signum());
  }

  @Test
  void changedApprovalBlocksZeroReviewAndSourceFreeze() {
    approved("STANDARD");
    Long id = service.submitNoOvertime(zeroRequest("zero-1"), 1L);
    OaPayrollOvertimeDTO changed = new OaPayrollOvertimeDTO();
    changed.setId(77L);
    changed.setApplicantUserId(99L);
    changed.setStartTime(day.atTime(18, 0));
    changed.setEndTime(day.atTime(21, 0));
    changed.setProcessInstanceId("approved-zero-flow");
    when(oa.getApprovedOvertime(77L)).thenReturn(changed);
    assertServiceException(
        () -> service.reviewNoOvertime(id, true, "同意", 2L), PAYROLL_WORK_INVALID);
    assertEquals("PENDING", zeroCases.selectById(id).getStatus());
    assertTrue(entries.selectList().isEmpty());
  }

  @Test
  void laterPunchesInvalidateAnApprovedZeroDeclaration() {
    approved("STANDARD");
    Long id = service.submitNoOvertime(zeroRequest("zero-1"), 1L);
    service.reviewNoOvertime(id, true, "事实确认", 2L);
    clocks.insert(
        HrmAttendanceClockDO.builder()
            .employeeId(employeeId)
            .type(1)
            .status(0)
            .clockTime(day.atTime(18, 0))
            .attendanceTime(day.atStartOfDay())
            .build());
    clocks.insert(
        HrmAttendanceClockDO.builder()
            .employeeId(employeeId)
            .type(2)
            .status(0)
            .clockTime(day.atTime(20, 0))
            .attendanceTime(day.atStartOfDay())
            .build());
    assertServiceException(
        () -> service.validateSources(entries.selectList()), PAYROLL_WORK_INVALID);
    assertFalse(service.noOvertimeCases(10L, employeeId, 2L).get(0).getSourceFresh());
  }

  @Test
  void knownActualOvertimeCannotBeDeclaredZero() {
    approved("STANDARD");
    HrmPayrollNoOvertimeReqVO r = zeroRequest("zero-1");
    clocks.insert(
        HrmAttendanceClockDO.builder()
            .employeeId(employeeId)
            .type(1)
            .status(0)
            .clockTime(day.atTime(18, 0))
            .attendanceTime(day.atStartOfDay())
            .build());
    clocks.insert(
        HrmAttendanceClockDO.builder()
            .employeeId(employeeId)
            .type(2)
            .status(0)
            .clockTime(day.atTime(20, 0))
            .attendanceTime(day.atStartOfDay())
            .build());
    assertServiceException(() -> service.submitNoOvertime(r, 1L), PAYROLL_WORK_INVALID);
    assertTrue(zeroCases.selectList().isEmpty());
  }

  @Test
  void rejectingZeroCasePreservesRecordAndAllowsNewPreview() {
    approved("STANDARD");
    Long id = service.submitNoOvertime(zeroRequest("zero-1"), 1L);
    service.reviewNoOvertime(id, false, "证明不足", 2L);
    assertTrue(entries.selectList().isEmpty());
    assertNotEquals(id, service.submitNoOvertime(zeroRequest("zero-2"), 1L));
    assertEquals("证明不足", zeroCases.selectById(id).getReviewOpinion());
    assertEquals(2, zeroCases.selectList().size());
  }

  @Test
  void duplicateSourcesAndWrongEmployerDeclarationsAreRejected() {
    approved("STANDARD");
    service.submitNoOvertime(zeroRequest("zero-1"), 1L);
    assertServiceException(
        () -> service.submitNoOvertime(zeroRequest("zero-2"), 1L), PAYROLL_WORK_INVALID);
    HrmPayrollNoOvertimeReqVO foreign = zeroRequest("foreign");
    foreign.setEntityId(20L);
    assertServiceException(() -> service.submitNoOvertime(foreign, 1L), PAYROLL_WORK_INVALID);
  }

  @Test
  void unrelatedNextDayPunchesDoNotInvalidateZeroFacts() {
    approved("STANDARD");
    Long id = service.submitNoOvertime(zeroRequest("zero-next"), 1L);
    service.reviewNoOvertime(id, true, "核实确认", 2L);
    clocks.insert(
        HrmAttendanceClockDO.builder()
            .employeeId(employeeId)
            .type(1)
            .status(0)
            .clockTime(day.plusDays(1).atTime(9, 0))
            .attendanceTime(day.plusDays(1).atStartOfDay())
            .build());
    clocks.insert(
        HrmAttendanceClockDO.builder()
            .employeeId(employeeId)
            .type(2)
            .status(0)
            .clockTime(day.plusDays(1).atTime(17, 0))
            .attendanceTime(day.plusDays(1).atStartOfDay())
            .build());
    service.validateSources(entries.selectList());
    assertTrue(service.noOvertimeCases(10L, employeeId, 2L).get(0).getSourceFresh());
  }

  @Test
  void overlappingPlannedShiftsCannotConcealActualOvertime() {
    approved("STANDARD");
    HrmPayrollNoOvertimeReqVO r = zeroRequest("overlap-shift");
    when(groups.getEmployeeAttendanceShift(eq(employeeId), any()))
        .thenReturn(
            HrmAttendanceGroupDO.Shift.builder()
                .startTime(LocalTime.of(22, 0))
                .endTime(LocalTime.of(14, 0))
                .build(),
            HrmAttendanceGroupDO.Shift.builder()
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(17, 0))
                .build());
    assertServiceException(() -> service.submitNoOvertime(r, 1L), PAYROLL_WORK_INVALID);
    assertTrue(zeroCases.selectList().isEmpty());
  }
}
