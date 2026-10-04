package cn.iocoder.yudao.module.hrm.service.payroll.intake;

import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.intake.*;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class HrmPayrollCsvValidatorTest {
    static HrmPayrollContractSchemaVO schema() {
        return new HrmPayrollContractSchemaVO().setFields(Arrays.asList(
                new HrmPayrollContractSchemaVO.Field().setKey("job").setLabel("工号").setType("TEXT").setRequired(true).setMaxLength(32),
                new HrmPayrollContractSchemaVO.Field().setKey("date").setLabel("日期").setType("DATE").setRequired(true),
                new HrmPayrollContractSchemaVO.Field().setKey("scope").setLabel("主体").setType("TEXT").setRequired(true).setMaxLength(100),
                new HrmPayrollContractSchemaVO.Field().setKey("amount").setLabel("金额").setType("DECIMAL").setRequired(true).setScale(2).setUnit("元")))
                .setKeyFields(Arrays.asList("job", "date")).setPeriodField("date").setSubjectField("scope");
    }
    static HrmPayrollPreviewReqVO context(Long id) {
        return new HrmPayrollPreviewReqVO().setContractId(id).setDeclaredScope("测试主体")
                .setPeriodStart(LocalDate.of(2026, 10, 1)).setPeriodEnd(LocalDate.of(2026, 10, 31));
    }
    private HrmPayrollPreviewResultVO validate(String data) {
        return HrmPayrollCsvValidator.validate(("\uFEFF#hrm-payroll-contract,7,1\r\njob,date,scope,amount\r\n" + data)
                .getBytes(StandardCharsets.UTF_8), schema(), 7L, 1, context(7L));
    }
    private boolean code(HrmPayrollPreviewResultVO r, String code) {
        return r.getGlobalIssues().stream().anyMatch(i -> code.equals(i.getCode()))
                || r.getRows().stream().flatMap(row -> row.getIssues().stream()).anyMatch(i -> code.equals(i.getCode()));
    }
    @Test void zeroIsPresentWhileMissingFailsRequiredAndTextKeepsLeadingZero() {
        HrmPayrollPreviewResultVO result = validate("001,2026-10-01,测试主体,0\r\n002,2026-10-01,测试主体,\r\n");
        assertEquals(1, result.getValidCount()); assertEquals("001", result.getRows().get(0).getValues().get("job"));
        assertEquals("0", result.getRows().get(0).getValues().get("amount"));
        assertNull(result.getRows().get(1).getValues().get("amount")); assertTrue(code(result, "REQUIRED"));
    }
    @Test void precisionNeverSilentlyRoundsAndExponentAndFormulaAreRejected() {
        HrmPayrollPreviewResultVO result = validate("001,2026-10-01,测试主体,1.234\n002,2026-10-01,测试主体,1e2\n003,2026-10-01,测试主体,=SUM(1)\n004,2026-10-01,测试主体,1234567890123456789\n");
        assertEquals(4, result.getErrorCount()); assertEquals(0, result.getValidCount());
    }
    @Test void bothSidesOfDuplicateCompositeKeyAreInvalidButOtherPeriodsDiffer() {
        HrmPayrollPreviewResultVO result = validate("001,2026-10-01,测试主体,1\n001,2026-10-01,测试主体,2\n001,2026-10-02,测试主体,3\n");
        assertEquals(1, result.getValidCount()); assertEquals(2, result.getErrorCount());
        assertEquals("DUPLICATE", result.getRows().get(0).getIssues().get(0).getCode());
        assertEquals("DUPLICATE", result.getRows().get(1).getIssues().get(0).getCode());
    }
    @Test void validatesActualCalendarPeriodAndScopeWithInclusiveEndpoints() {
        HrmPayrollPreviewResultVO result = validate("a,2026-10-01,测试主体,1\nb,2026-10-31,测试主体,1\nc,2026-09-30,测试主体,1\nd,2026-10-01,其他主体,1\ne,2026-02-30,测试主体,1\n");
        assertEquals(2, result.getValidCount()); assertTrue(code(result, "PERIOD")); assertTrue(code(result, "SCOPE")); assertTrue(code(result, "TYPE"));
    }
    @Test void supportsQuotedCommaMultilineAndEscapedQuoteWithoutLosingPhysicalLineNumbers() {
        HrmPayrollPreviewResultVO r = validate("\"a,b\",2026-10-01,测试主体,1\r\n\"c\"\"d\r\ne\",2026-10-01,测试主体,2\r\nf,2026-10-01,测试主体,3");
        assertEquals(3, r.getValidCount()); assertEquals("c\"d\ne", r.getRows().get(1).getValues().get("job"));
        assertEquals(6, r.getRows().get(2).getLine());
    }
    @Test void acceptsReorderedExactHeaders() {
        HrmPayrollPreviewResultVO r = HrmPayrollCsvValidator.validate("#hrm-payroll-contract,7,1\namount,scope,date,job\n0,测试主体,2026-10-01,0001\n".getBytes(StandardCharsets.UTF_8), schema(), 7L, 1, context(7L));
        assertEquals(1, r.getValidCount()); assertEquals("0001", r.getRows().get(0).getValues().get("job"));
    }
    @Test void rejectsMismatchedContractOrVersionAndMissingMarker() {
        for (String header : Arrays.asList("#hrm-payroll-contract,8,1", "#hrm-payroll-contract,7,2", "job,date,scope,amount")) {
            HrmPayrollPreviewResultVO r = HrmPayrollCsvValidator.validate((header + "\njob,date,scope,amount\n").getBytes(StandardCharsets.UTF_8), schema(), 7L, 1, context(7L));
            assertTrue(code(r, "TEMPLATE_VERSION"));
        }
    }
    @Test void rejectsMissingUnknownAndDuplicateHeaders() {
        for (String header : Arrays.asList("job,date,scope", "job,date,scope,other", "job,date,scope,job")) {
            HrmPayrollPreviewResultVO r = HrmPayrollCsvValidator.validate(("#hrm-payroll-contract,7,1\n" + header + "\n").getBytes(StandardCharsets.UTF_8), schema(), 7L, 1, context(7L));
            assertTrue(code(r, "HEADERS"));
        }
    }
    @Test void malformedQuotesCannotPartiallyPassAndColumnCountIsRowIssue() {
        assertTrue(code(validate("\"unclosed,2026-10-01,测试主体,1\n"), "CSV_FORMAT"));
        assertTrue(code(validate("a\"b,2026-10-01,测试主体,1\n"), "CSV_FORMAT"));
        assertTrue(code(validate("\"a\"junk,2026-10-01,测试主体,1\n"), "CSV_FORMAT"));
        assertTrue(code(validate("a,2026-10-01,测试主体\n"), "COLUMN_COUNT"));
    }
    @Test void rejectsInvalidUtf8AndEmptyData() {
        HrmPayrollPreviewResultVO r = HrmPayrollCsvValidator.validate(new byte[]{(byte) 0xc3, (byte) 0x28}, schema(), 7L, 1, context(7L));
        assertTrue(code(r, "ENCODING")); assertTrue(code(validate("\r\n"), "EMPTY"));
    }
    @Test void boundsRowsColumnsAndCells() {
        StringBuilder rows = new StringBuilder();
        for (int i = 0; i < 501; i++) rows.append(i).append(",2026-10-01,测试主体,1\n");
        assertTrue(code(validate(rows.toString()), "CSV_LIMIT"));
        assertTrue(code(validate(String.join(",", Collections.nCopies(33, "1")) + "\n"), "CSV_LIMIT"));
        assertTrue(code(validate(String.join("", Collections.nCopies(4097, "a")) + ",2026-10-01,测试主体,1\n"), "CSV_LIMIT"));
    }
    @Test void integerNormalizationAndOptionalMissingAreDifferentFromZero() {
        HrmPayrollContractSchemaVO s = schema();
        s.getFields().get(3).setType("INTEGER").setRequired(false);
        HrmPayrollPreviewResultVO r = HrmPayrollCsvValidator.validate("#hrm-payroll-contract,7,1\njob,date,scope,amount\na,2026-10-01,测试主体,+000\nb,2026-10-01,测试主体,\nc,2026-10-01,测试主体,1.0\n".getBytes(StandardCharsets.UTF_8), s, 7L, 1, context(7L));
        assertEquals(2, r.getValidCount()); assertEquals("0", r.getRows().get(0).getValues().get("amount")); assertNull(r.getRows().get(1).getValues().get("amount"));
    }
}
