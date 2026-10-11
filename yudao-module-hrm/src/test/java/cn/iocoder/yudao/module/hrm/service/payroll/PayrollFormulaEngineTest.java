package cn.iocoder.yudao.module.hrm.service.payroll;
import cn.iocoder.yudao.module.hrm.service.payroll.rule.PayrollFormulaEngine;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class PayrollFormulaEngineTest {
    @Test void evaluatesDependenciesWithDecimalArithmetic() {
        Map<String,String> rules=new LinkedHashMap<>();rules.put("net","gross - tax");rules.put("gross","base + hours * rate");
        Map<String,BigDecimal> inputs=new HashMap<>();inputs.put("base",new BigDecimal("1000.10"));inputs.put("hours",new BigDecimal("1.5"));inputs.put("rate",new BigDecimal("20"));inputs.put("tax",new BigDecimal("10"));
        assertEquals(0,new BigDecimal("1020.10").compareTo(PayrollFormulaEngine.evaluate(rules,inputs).get("net")));
    }
    @Test void rejectsCyclesMissingInputsAndDivisionByZero() {
        Map<String,String> cycle=new HashMap<>();cycle.put("a","b+1");cycle.put("b","a+1");
        assertThrows(IllegalArgumentException.class,()->PayrollFormulaEngine.evaluate(cycle,Collections.emptyMap()));
        assertThrows(IllegalArgumentException.class,()->PayrollFormulaEngine.evaluate(Collections.singletonMap("net","missing+1"),Collections.emptyMap()));
        assertThrows(IllegalArgumentException.class,()->PayrollFormulaEngine.evaluate(Collections.singletonMap("net","1/0"),Collections.emptyMap()));
    }
    @Test void rejectsExecutableExpressions() {
        for(String expression:Arrays.asList("Runtime.getRuntime()","new ProcessBuilder()","${base}","1; 2","1e10"))
            assertThrows(IllegalArgumentException.class,()->PayrollFormulaEngine.evaluate(Collections.singletonMap("net",expression),Collections.emptyMap()));
    }
    @Test void graphValidationChecksSyntaxWithoutEvaluatingValues() {
        assertEquals(new HashSet<>(Arrays.asList("base","days")),PayrollFormulaEngine.variables("base/(days-1)"));
        PayrollFormulaEngine.validateGraph(Collections.singletonMap("result","base/(days-1)"),new HashSet<>(Arrays.asList("base","days")));
        assertThrows(IllegalArgumentException.class,()->PayrollFormulaEngine.validateGraph(Collections.singletonMap("a","a+1"),Collections.singleton("a")));
        assertThrows(IllegalArgumentException.class,()->PayrollFormulaEngine.variables("base + )"));
    }
    @Test void parenthesesAndUnaryMinusAreDeterministic() {
        assertEquals(0,new BigDecimal("-2").compareTo(PayrollFormulaEngine.evaluate(Collections.singletonMap("net","-(2+4)/3"),Collections.emptyMap()).get("net")));
    }
}
