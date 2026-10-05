package cn.iocoder.yudao.module.hrm.service.payroll.calculation;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.calculation.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class HrmPayrollCalculationEngineTest {
    private final HrmPayrollCalculationEngine engine = new HrmPayrollCalculationEngine();
    static Map<String, String> values(String... pairs) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int index = 0; index < pairs.length; index += 2) map.put(pairs[index], pairs[index + 1]); return map;
    }
    static HrmPayrollCalculationSpecVO.Input input(String key, String type, int scale) {
        return new HrmPayrollCalculationSpecVO.Input().setKey(key).setLabel(key).setType(type).setScale(scale).setUnit("合成声明单位");
    }
    static HrmPayrollCalculationSpecVO.Item item(String key, String expression) {
        return new HrmPayrollCalculationSpecVO.Item().setKey(key).setLabel(key).setExpression(expression).setUnit("合成元")
                .setAmountScale(2).setRoundingMode("HALF_UP");
    }
    static HrmPayrollCalculationSpecVO sample() {
        return new HrmPayrollCalculationSpecVO().setInputs(Arrays.asList(input("base", "DECIMAL", 2), input("days", "DECIMAL", 2), input("cycleDays", "DECIMAL", 2)))
                .setItems(Arrays.asList(item("net", "prorated - deduction"), item("prorated", "base * days / cycleDays"), item("deduction", "0")))
                .setDivisionScale(8).setDivisionRoundingMode("HALF_UP")
                .setCases(Collections.singletonList(new HrmPayrollCalculationSpecVO.BusinessCase().setTitle("合成三分之一样例")
                        .setInputs(values("base", "1000.00", "days", "1", "cycleDays", "3"))
                        .setExpected(values("net", "333.33", "prorated", "333.33", "deduction", "0.00"))));
    }
    private HrmPayrollCalculationSpecVO simple(String expression) {
        return new HrmPayrollCalculationSpecVO().setInputs(Collections.singletonList(input("value", "DECIMAL", 8)))
                .setItems(Collections.singletonList(item("result", expression))).setDivisionScale(8).setDivisionRoundingMode("HALF_UP");
    }
    private String amount(HrmPayrollCalculationSpecVO spec, String input) {
        return engine.compile(spec).execute(values("value", input)).getItems().get(0).getAmount();
    }
    @Test void topologicalExecutionIgnoresDisplayOrderAndExplainsEveryRoundedDependency() {
        HrmPayrollCalculationResultVO result = engine.compile(sample()).execute(values("base", "1000.00", "days", "1", "cycleDays", "3"));
        assertEquals("prorated", result.getItems().get(0).getKey()); assertEquals("333.33333333", result.getItems().get(0).getRawResult());
        assertEquals("net", result.getItems().get(2).getKey()); assertEquals("333.33", result.getItems().get(2).getAmount());
        assertTrue(result.getItems().get(0).getSteps().stream().anyMatch(s -> s.contains("除法 8 位 / HALF_UP")));
        assertTrue(result.getItems().get(2).getSteps().contains("prorated = 333.33"));
        assertTrue(engine.compile(sample()).verifyCases().getAllPassed());
    }
    @Test void valuesBeyondJavascriptSafeIntegersRetainEveryDecimal() {
        assertEquals("9007199254740993.01", amount(simple("value + 0.005"), "9007199254740993.00"));
    }
    @ParameterizedTest @CsvSource({"HALF_UP,2.345,2.35", "HALF_EVEN,2.345,2.34", "DOWN,2.345,2.34", "UP,2.345,2.35",
            "HALF_UP,-2.345,-2.35", "HALF_EVEN,-2.345,-2.34", "DOWN,-2.345,-2.34", "UP,-2.345,-2.35"})
    void positiveAndNegativeRoundingFollowExplicitModes(String mode, String input, String expected) {
        HrmPayrollCalculationSpecVO spec = simple("value"); spec.getItems().get(0).setRoundingMode(mode); assertEquals(expected, amount(spec, input));
    }
    @Test void expressionsRespectPrecedenceAssociativityAndFunctionArity() {
        assertEquals("12.00", amount(simple("abs(-3) + min(value, 4) + max(1, value)"), "5"));
        assertEquals("5.00", amount(simple("10 - 3 - 2"), "0")); assertEquals("9.00", amount(simple("10 - (3 - 2)"), "0"));
        assertEquals("2.00", amount(simple("12 / 3 / 2"), "0"));
    }
    @Test void divisionRoundsAtEachExplicitDivisionBeforeItemRounding() {
        HrmPayrollCalculationSpecVO spec = simple("1 / 3 * 3").setDivisionScale(2).setDivisionRoundingMode("DOWN");
        assertEquals("0.99", amount(spec, "0"));
    }
    @Test void downstreamItemsUseRoundedAmountsRatherThanHiddenRawValues() {
        HrmPayrollCalculationSpecVO spec = simple("value"); spec.setItems(Arrays.asList(item("total", "rounded + rounded"), item("rounded", "value")));
        assertEquals("4.70", engine.compile(spec).execute(values("value", "2.345")).getItems().get(1).getAmount());
    }
    @Test void explicitZeroIsAcceptedWhileMissingBlankNullAndUnexpectedInputsAreRejected() {
        HrmPayrollCalculationEngine.Compiled spec = engine.compile(simple("value"));
        assertEquals("0.00", spec.execute(values("value", "0")).getItems().get(0).getAmount());
        for (Map<String, String> values : Arrays.asList(values(), values("value", ""), values("value", null), values("value", "0", "extra", "0")))
            assertThrows(ServiceException.class, () -> spec.execute(values));
    }
    @ParameterizedTest @ValueSource(strings = {"1e3", "NaN", "Infinity", "1,000", " 1", "1 ", "01", "1.123456789"})
    void inputNumbersMustBeBoundedPlainDecimalStrings(String value) { assertThrows(ServiceException.class, () -> amount(simple("value"), value)); }
    @Test void integersAndDeclaredInputPrecisionAreEnforcedWithoutRounding() {
        HrmPayrollCalculationSpecVO spec = simple("value"); spec.getInputs().get(0).setType("INTEGER").setScale(0);
        assertEquals("2.00", amount(spec, "2")); assertThrows(ServiceException.class, () -> amount(spec, "2.0"));
        spec.getInputs().get(0).setScale(2); assertThrows(ServiceException.class, () -> engine.compile(spec));
        spec.getInputs().get(0).setType("DECIMAL"); assertThrows(ServiceException.class, () -> amount(spec, "2.001"));
    }
    @ParameterizedTest @ValueSource(strings = {"T(java.lang.Runtime).getRuntime()", "value[0]", "value=1", "value++", "pow(value,2)", "abs()", "abs(1,2)", "min(1)", "1e3", "1.", "(1+2", "value**2", "'1'"})
    void unsupportedSyntaxAndExecutableExpressionsAreRejected(String expression) { assertThrows(ServiceException.class, () -> engine.compile(simple(expression))); }
    @Test void unknownNamesDuplicateIdentifiersAndReservedFunctionKeysAreRejected() {
        assertThrows(ServiceException.class, () -> engine.compile(simple("unknown")));
        HrmPayrollCalculationSpecVO duplicateInputs = simple("value").setInputs(Arrays.asList(input("value", "DECIMAL", 2), input("value", "DECIMAL", 2)));
        assertThrows(ServiceException.class, () -> engine.compile(duplicateInputs));
        HrmPayrollCalculationSpecVO duplicate = simple("value").setItems(Collections.singletonList(item("value", "1")));
        assertThrows(ServiceException.class, () -> engine.compile(duplicate));
        HrmPayrollCalculationSpecVO reserved = simple("value"); reserved.getInputs().get(0).setKey("min"); assertThrows(ServiceException.class, () -> engine.compile(reserved));
    }
    @Test void cyclicDependenciesAndMissingDivisionPolicyAreRejected() {
        HrmPayrollCalculationSpecVO cycle = simple("value").setItems(Arrays.asList(item("a", "b+1"), item("b", "a+1")));
        assertTrue(assertThrows(ServiceException.class, () -> engine.compile(cycle)).getMessage().contains("循环依赖"));
        assertThrows(ServiceException.class, () -> engine.compile(simple("1 / value").setDivisionScale(null).setDivisionRoundingMode(null)));
        assertThrows(ServiceException.class, () -> engine.compile(simple("value").setDivisionScale(null)));
    }
    @Test void divisionByZeroIsAnErrorNotAZeroResult() {
        assertTrue(assertThrows(ServiceException.class, () -> amount(simple("1 / value"), "0")).getMessage().contains("除零"));
    }
    @Test void expressionDepthNodesLengthAndIntermediateMagnitudeAreBounded() {
        assertThrows(ServiceException.class, () -> engine.compile(simple(String.join("", Collections.nCopies(34, "(")) + "value" + String.join("", Collections.nCopies(34, ")")))));
        assertThrows(ServiceException.class, () -> engine.compile(simple(String.join("+", Collections.nCopies(66, "1")))));
        assertThrows(ServiceException.class, () -> amount(simple("value * value"), "999999999999999999999999"));
        assertThrows(ServiceException.class, () -> engine.compile(simple(String.join("", Collections.nCopies(257, "1")))));
    }
    @Test void missingMismatchedInvalidAndPartialExpectationsCannotPassVerification() {
        HrmPayrollCalculationSpecVO spec = sample(); spec.getCases().get(0).getExpected().put("net", "333.34"); assertFalse(engine.compile(spec).verifyCases().getAllPassed());
        spec.getCases().get(0).getExpected().put("net", "invalid"); assertEquals(0, engine.compile(spec).verifyCases().getPassed());
        spec.getCases().get(0).getExpected().remove("net"); assertFalse(engine.compile(spec).verifyCases().getAllPassed());
        spec.setCases(Collections.emptyList()); assertFalse(engine.compile(spec).verifyCases().getAllPassed());
    }
    @Test void compiledDefinitionAndVerificationExpectationsAreDefensivelyCopied() {
        HrmPayrollCalculationSpecVO original = sample(); HrmPayrollCalculationEngine.Compiled compiled = engine.compile(original);
        String hash = compiled.getProgramHash(); original.getItems().get(0).setExpression("0"); original.getCases().get(0).getExpected().put("net", "0");
        assertTrue(compiled.verifyCases().getAllPassed()); compiled.verifyCases().getCases().get(0).getExpected().put("net", "0");
        assertTrue(compiled.verifyCases().getAllPassed()); assertEquals(hash, compiled.getProgramHash()); assertEquals(64, hash.length());
    }
    @Test void jsonNumbersAreNotCoercedIntoFinancialStrings() {
        assertThrows(RuntimeException.class, () -> JsonUtils.parseObject("{\"inputs\":{\"value\":9007199254740993}}", HrmPayrollCalculationPreviewReqVO.class));
        HrmPayrollCalculationPreviewReqVO req = JsonUtils.parseObject("{\"inputs\":{\"value\":\"9007199254740993\"}}", HrmPayrollCalculationPreviewReqVO.class);
        assertEquals("9007199254740993", req.getInputs().get("value"));
    }
    @Test void precisionSettingsCannotSilentlyTruncateFractionsOrCoerceStrings() {
        for (String value : Arrays.asList("2.5", "\"2\"", "true"))
            assertThrows(RuntimeException.class, () -> JsonUtils.parseObject("{\"divisionScale\":" + value + "}", HrmPayrollCalculationSpecVO.class));
        assertEquals(0, JsonUtils.parseObject("{\"divisionScale\":0}", HrmPayrollCalculationSpecVO.class).getDivisionScale());
    }
}
