package cn.iocoder.yudao.module.hrm.service.payroll;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollTaxProfileReqVO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.employee.info.HrmEmployeeMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import javax.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

@Import({
  HrmPayrollTaxProfileService.class,
  HrmPayrollTaxIdentityCodec.class,
  org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration.class
})
@TestPropertySource(
    properties = {
      "yudao.hrm.tax.identity-encryption-key=MTExMTExMTExMTExMTExMTExMTExMTExMTExMTExMTE=",
      "yudao.hrm.tax.identity-fingerprint-key=MjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjI="
    })
class HrmPayrollTaxProfileServiceTest extends BaseDbUnitTest {
  @Resource private HrmPayrollTaxProfileService service;
  @Resource private HrmPayrollTaxProfileMapper profiles;
  @Resource private HrmEmployeeMapper employees;
  @Resource private HrmPayrollTaxIdentityCodec codec;
  @MockBean private HrmPayrollEntityService entities;
  @MockBean private HrmPayrollTaxPolicyService policies;
  private Long employeeId;

  @BeforeEach
  void setup() {
    TenantContextHolder.setTenantId(0L);
    HrmEmployeeDO employee =
        new HrmEmployeeDO()
            .setName("税务员工")
            .setJobNumber("TAX-PROFILE-1")
            .setUserId(99L)
            .setEntryTime(LocalDate.of(2026, 1, 1).atStartOfDay());
    employees.insert(employee);
    employeeId = employee.getId();
    when(policies.requireApproved(10L, 2026))
        .thenReturn(
            new HrmPayrollTaxPolicyDO()
                .setEntityId(10L)
                .setYear(2026)
                .setTaxRegistrationNumber("914400000000000001"));
    when(entities.effectiveMemberships(eq(10L), any(), any()))
        .thenReturn(
            Collections.singletonList(
                new HrmPayrollEntityMembershipDO()
                    .setEmployeeId(employeeId)
                    .setEntityId(10L)
                    .setEffectiveFrom(LocalDate.of(2026, 1, 1))
                    .setEffectiveTo(LocalDate.of(2026, 12, 31))));
    when(entities.allowed(eq(10L), anyString(), anyLong())).thenReturn(true);
  }

  @AfterEach
  void cleanupTenant() {
    TenantContextHolder.clear();
  }

  private HrmPayrollTaxProfileReqVO request() {
    return new HrmPayrollTaxProfileReqVO()
        .setEntityId(10L)
        .setEmployeeId(employeeId)
        .setYear(2026)
        .setTaxpayerName("税务员工")
        .setIdentifierType("OTHER")
        .setIdentifierCountry("CN")
        .setIdentifier("TESTTAX12345678")
        .setRequestKey(UUID.randomUUID().toString())
        .setResidentThisYear(true)
        .setNoPriorContinuousRemunerationBeforeEntry(false)
        .setFirstSalaryThisYear(false)
        .setNoPriorSalaryThisYear(false)
        .setSeparateBonusUsedElsewhere(false)
        .setOpeningMode("ZERO")
        .setOpeningAsOfDate(LocalDate.of(2026, 1, 1))
        .setOpeningIncome(BigDecimal.ZERO)
        .setOpeningSocialDeduction(BigDecimal.ZERO)
        .setOpeningBasicDeduction(BigDecimal.ZERO)
        .setOpeningWithheld(BigDecimal.ZERO)
        .setOfficialReference("测试纳税资料及期初申报表")
        .setEvidence("核对身份与本扣缴单位累计范围");
  }

  @Test
  void encryptedIdentityNeverAppearsInOrdinaryJsonOrAuditHistory() {
    Long id = service.create(request(), 1L);
    HrmPayrollTaxProfileDO row = profiles.selectById(id);
    assertFalse(row.getIdentifierCipher().contains("TESTTAX12345678"));
    assertEquals("****5678", row.getMaskedIdentifier());
    assertFalse(JsonUtils.toJsonString(row).contains("TESTTAX12345678"));
    assertFalse(JsonUtils.toJsonString(row).contains("identifierCipher"));
    assertEquals("TESTTAX12345678", service.privateIdentifier(id, 2L));
    assertFalse(service.history(id, 2L).get(0).getSnapshot().contains("TESTTAX12345678"));
  }

  @Test
  void requestSerializationDoesNotExposeRawIdentifierAndPrivateReadsAreAudited() {
    HrmPayrollTaxProfileReqVO request = request();
    assertFalse(JsonUtils.toJsonString(request).contains("TESTTAX12345678"));
    Long id = service.create(request, 1L);
    service.privateIdentifier(id, 2L);
    assertEquals("READ_PRIVATE_IDENTIFIER", service.history(id, 2L).get(1).getAction());
    assertFalse(service.history(id, 2L).get(1).getSnapshot().contains("TESTTAX12345678"));
  }

  @Test
  void independentApprovalAndEmployeeSelfReviewAreRequired() {
    Long id = service.create(request(), 1L);
    assertServiceException(
        () -> service.requireApproved(10L, employeeId, 2026), PAYROLL_TAX_INVALID);
    assertServiceException(() -> service.review(id, true, "自行批准", 1L), PAYROLL_SELF_APPROVAL);
    assertServiceException(() -> service.review(id, true, "员工本人批准", 99L), PAYROLL_SELF_APPROVAL);
    service.review(id, true, "独立核对资料", 2L);
    assertEquals(id, service.requireApproved(10L, employeeId, 2026).getId());
  }

  @Test
  void sourceOrAccountMappingChangesInvalidatePendingAndApprovedProfiles() {
    Long id = service.create(request(), 1L);
    HrmEmployeeDO employee = employees.selectById(employeeId);
    employee.setUserId(100L);
    employees.updateById(employee);
    assertServiceException(() -> service.review(id, true, "批准旧映射", 2L), PAYROLL_TAX_INVALID);
    employee.setUserId(99L);
    employees.updateById(employee);
    service.review(id, true, "复核", 2L);
    employee.setLeaveTime(LocalDate.of(2026, 10, 1).atStartOfDay());
    employees.updateById(employee);
    assertServiceException(
        () -> service.requireApproved(10L, employeeId, 2026), PAYROLL_TAX_INVALID);
  }

  @Test
  void firstSalaryEligibilityCannotHidePreviouslyDeclaredIncome() {
    assertServiceException(
        () -> service.create(request().setFirstSalaryThisYear(true), 1L), PAYROLL_TAX_INVALID);
    assertServiceException(
        () ->
            service.create(
                request()
                    .setFirstSalaryThisYear(true)
                    .setNoPriorSalaryThisYear(true)
                    .setOpeningIncome(BigDecimal.ONE),
                1L),
        PAYROLL_TAX_INVALID);
    Long id =
        service.create(
            request()
                .setFirstSalaryThisYear(true)
                .setNoPriorSalaryThisYear(true)
                .setNoPriorContinuousRemunerationBeforeEntry(true),
            1L);
    service.review(id, true, "核对年度首次取得工资资格", 2L);
    assertTrue(profiles.selectById(id).getFirstSalaryThisYear());
  }

  @Test
  void firstIncomeEligibilityIsAssessedBeforeEntryAndDoesNotEraseThisEmployerHistory() {
    HrmPayrollTaxProfileReqVO missing =
        request().setFirstSalaryThisYear(true).setNoPriorSalaryThisYear(true);
    assertServiceException(() -> service.create(missing, 1L), PAYROLL_TAX_INVALID);
    HrmPayrollTaxProfileReqVO valid =
        request()
            .setFirstSalaryThisYear(true)
            .setNoPriorSalaryThisYear(true)
            .setNoPriorContinuousRemunerationBeforeEntry(true)
            .setOpeningMode("DECLARE")
            .setOpeningIncome(new java.math.BigDecimal("10000"))
            .setOpeningBasicDeduction(new java.math.BigDecimal("5000"))
            .setOpeningWithheld(new java.math.BigDecimal("150"));
    Long id = service.create(valid, 1L);
    service.review(id, true, "核实新入职前无前述所得，累计为本单位入职后发放", 2L);
    assertEquals(
        0, new java.math.BigDecimal("10000").compareTo(profiles.selectById(id).getOpeningIncome()));
  }

  @Test
  void requestReplayDoesNotCreateAnotherVersionOrPermitPayloadMutation() {
    HrmPayrollTaxProfileReqVO request = request();
    Long id = service.create(request, 1L);
    assertEquals(id, service.create(request, 1L));
    assertServiceException(
        () -> service.create(request.setIdentifier("CHANGED123456"), 1L), PAYROLL_TAX_INVALID);
    assertEquals(1, profiles.selectCount());
  }

  @Test
  void verifiedIdentityCanBeReusedWithoutPlaintextOrPrivateReadPermission() {
    Long first = service.create(request(), 1L);
    service.review(first, true, "核实身份", 2L);
    HrmPayrollTaxProfileReqVO reuse =
        request().setPriorProfileId(first).setIdentitySourceProfileId(first).setIdentifier(null);
    Long next = service.create(reuse, 1L);
    assertEquals(first, profiles.selectById(next).getIdentitySourceProfileId());
    assertEquals(
        profiles.selectById(first).getIdentifierCipher(),
        profiles.selectById(next).getIdentifierCipher());
    assertEquals(
        profiles.selectById(first).getTaxpayerKey(), profiles.selectById(next).getTaxpayerKey());
    assertFalse(JsonUtils.toJsonString(reuse).contains("TESTTAX12345678"));
    service.review(next, true, "仅核实年度资格更新，身份沿用批准来源", 2L);
    assertEquals(next, service.create(reuse, 1L));
    assertEquals("TESTTAX12345678", service.privateIdentifier(next, 2L));
    assertFalse(service.history(next, 2L).get(0).getSnapshot().contains("TESTTAX12345678"));
    assertServiceException(
        () ->
            service.create(
                request()
                    .setPriorProfileId(next)
                    .setIdentitySourceProfileId(first)
                    .setIdentifier(null),
                1L),
        PAYROLL_VERSION_CONFLICT);
  }

  @Test
  void reuseCannotBorrowAnotherEntityEmployeePendingIdentityOrMixPlaintext() {
    HrmPayrollTaxProfileDO foreign =
        new HrmPayrollTaxProfileDO()
            .setEntityId(20L)
            .setEmployeeId(employeeId)
            .setYear(2026)
            .setVersion(1)
            .setStatus("APPROVED")
            .setIdentifierType("OTHER")
            .setIdentifierCountry("CN")
            .setTaxpayerKey("foreign-test-only")
            .setIdentifierCipher("v1:test-only");
    profiles.insert(foreign);
    assertServiceException(
        () ->
            service.create(
                request().setIdentitySourceProfileId(foreign.getId()).setIdentifier(null), 1L),
        PAYROLL_TAX_INVALID);
    Long pending = service.create(request(), 1L);
    assertServiceException(
        () -> service.create(request().setIdentitySourceProfileId(pending).setIdentifier(null), 1L),
        PAYROLL_TAX_INVALID);
    service.review(pending, true, "核实", 2L);
    assertServiceException(
        () ->
            service.create(
                request().setIdentitySourceProfileId(pending).setPriorProfileId(pending), 1L),
        PAYROLL_TAX_INVALID);
    assertServiceException(
        () ->
            service.create(
                request()
                    .setIdentitySourceProfileId(pending)
                    .setPriorProfileId(pending)
                    .setIdentifier(null)
                    .setIdentifierCountry("US"),
                1L),
        PAYROLL_TAX_INVALID);
    HrmPayrollTaxProfileDO another =
        new HrmPayrollTaxProfileDO()
            .setEntityId(10L)
            .setEmployeeId(employeeId + 100)
            .setYear(2026)
            .setVersion(1)
            .setStatus("APPROVED")
            .setIdentifierType("OTHER")
            .setIdentifierCountry("CN")
            .setTaxpayerKey("other-test-only")
            .setIdentifierCipher("v1:test-only");
    profiles.insert(another);
    assertServiceException(
        () ->
            service.create(
                request().setIdentitySourceProfileId(another.getId()).setIdentifier(null), 1L),
        PAYROLL_TAX_INVALID);
  }

  @Test
  void identitySourceProjectionIsMaskedAndRequiresManageScope() {
    Long first = service.create(request(), 1L);
    service.review(first, true, "核实", 2L);
    java.util.Map<String, Object> info = service.identitySource(10L, employeeId, 2026, 1L);
    assertEquals(first, info.get("id"));
    assertEquals("****5678", info.get("maskedIdentifier"));
    assertEquals(5, info.size());
    assertFalse(JsonUtils.toJsonString(info).contains("identifierCipher"));
    assertFalse(JsonUtils.toJsonString(info).contains("TESTTAX12345678"));
    assertTrue(service.identitySource(20L, employeeId, 2026, 1L).isEmpty());
    org.mockito.Mockito.doThrow(new IllegalStateException("denied"))
        .when(entities)
        .check(10L, "MANAGE", 8L);
    assertThrows(
        IllegalStateException.class, () -> service.identitySource(10L, employeeId, 2026, 8L));
  }

  @Test
  void profileSuccessorPreservesHistoryAndCannotResetTaxpayerIdentity() {
    Long first = service.create(request(), 1L);
    service.review(first, true, "核对", 2L);
    HrmPayrollTaxProfileReqVO next =
        request().setPriorProfileId(first).setSeparateBonusUsedElsewhere(true);
    Long id = service.create(next, 1L);
    service.review(id, true, "核对外单位奖金单独计税资料", 2L);
    assertEquals("SUPERSEDED", profiles.selectById(first).getStatus());
    assertFalse(profiles.selectById(first).getSeparateBonusUsedElsewhere());
    assertServiceException(
        () -> service.create(request().setPriorProfileId(id).setIdentifier("OTHERPERSON123"), 1L),
        PAYROLL_TAX_INVALID);
  }

  @Test
  void privateIdentifiersRequireAdditionalEntityPrivileges() {
    Long id = service.create(request(), 1L);
    when(entities.allowed(10L, "MANAGE", 4L)).thenReturn(false);
    when(entities.allowed(10L, "REVIEW", 4L)).thenReturn(false);
    assertServiceException(() -> service.privateIdentifier(id, 4L), PAYROLL_ENTITY_INVALID);
  }

  @Test
  void cipherNoncesAreUniqueAndTamperingIsRejected() {
    String first = codec.encrypt("TESTTAX12345678"), second = codec.encrypt("TESTTAX12345678");
    assertNotEquals(first, second);
    assertEquals("TESTTAX12345678", codec.decrypt(first));
    String[] parts = first.split(":");
    byte[] ciphertext = Base64.getDecoder().decode(parts[2]);
    ciphertext[0] ^= 1;
    assertServiceException(
        () ->
            codec.decrypt(
                parts[0] + ":" + parts[1] + ":" + Base64.getEncoder().encodeToString(ciphertext)),
        PAYROLL_TAX_INVALID);
  }
}
