package cn.iocoder.yudao.module.hrm.service.payroll.calculation;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.calculation.*;
import org.springframework.stereotype.Component;
import java.math.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import java.util.stream.Collectors;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.PAYROLL_CALCULATION_INVALID;

/** A bounded decimal-only grammar. No SpEL, reflection, scripts, or implicit variables. */
@Component
public class HrmPayrollCalculationEngine {
    private static final Set<String> FUNCTIONS = new HashSet<>(Arrays.asList("abs", "min", "max"));
    private static final Set<String> ROUNDING = new HashSet<>(Arrays.asList("HALF_UP", "HALF_EVEN", "DOWN", "UP"));
    private static void check(boolean valid, String message) {
        if (!valid) throw exception(PAYROLL_CALCULATION_INVALID, message);
    }
    private static void text(String value, int limit, String field) {
        check(value != null && !value.trim().isEmpty() && value.length() <= limit, field + "缺失或超过长度限制");
    }
    private static void scale(Integer value, String field) {
        check(value != null && value >= 0 && value <= 8, field + "须明确填写 0～8");
    }
    private static BigDecimal bounded(BigDecimal value) {
        check(value.precision() <= 38 && value.scale() <= 32, "中间结果超过 38 位精度或 32 位小数的计算限制");
        return value;
    }
    private static BigDecimal number(String value, int allowedScale, boolean integer, String key) {
        check(value != null && value.length() <= 40 && value.matches("[+-]?(0|[1-9][0-9]{0,23})(\\.[0-9]{1,8})?"),
                key + "须明确填写普通十进制字符串，不填缺值、不接受指数或分隔符");
        BigDecimal parsed = new BigDecimal(value);
        check(!integer || value.indexOf('.') < 0, key + "声明为整数，不能填写小数");
        check(parsed.scale() <= allowedScale, key + "超过声明的小数位数，不自动舍入输入");
        return bounded(parsed);
    }
    private static String hash(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte b : bytes) result.append(String.format("%02x", b & 255));
            return result.toString();
        } catch (NoSuchAlgorithmException error) { throw new IllegalStateException(error); }
    }

    public Compiled compile(HrmPayrollCalculationSpecVO value) {
        check(value != null, "计算定义缺失");
        HrmPayrollCalculationSpecVO program = JsonUtils.parseObject(JsonUtils.toJsonString(value), HrmPayrollCalculationSpecVO.class);
        check(program.getInputs() != null && program.getInputs().size() <= 32, "输入项最多 32 个");
        check(program.getItems() != null && !program.getItems().isEmpty() && program.getItems().size() <= 32, "计算项须为 1～32 个");
        check(program.getCases() != null && program.getCases().size() <= 20, "业务样例最多 20 个");
        check((program.getDivisionScale() == null) == (program.getDivisionRoundingMode() == null), "除法精度和舍入方式须同时声明");
        if (program.getDivisionScale() != null) {
            scale(program.getDivisionScale(), "除法精度");
            check(ROUNDING.contains(program.getDivisionRoundingMode()), "除法舍入方式不合法");
        }
        Compiled compiled = new Compiled(program);
        for (HrmPayrollCalculationSpecVO.Input input : program.getInputs()) {
            check(input != null, "输入项不能为 null");
            key(input.getKey()); text(input.getLabel(), 120, "输入名称"); text(input.getUnit(), 40, "输入单位");
            check("INTEGER".equals(input.getType()) || "DECIMAL".equals(input.getType()), "输入类型须为 INTEGER 或 DECIMAL");
            scale(input.getScale(), "输入精度");
            check(!"INTEGER".equals(input.getType()) || input.getScale() == 0, "整数输入须声明 0 位小数");
            check(compiled.inputs.put(input.getKey(), input) == null, "重复输入编号：" + input.getKey());
            input.setLabel(input.getLabel().trim()).setUnit(input.getUnit().trim());
        }
        for (HrmPayrollCalculationSpecVO.Item item : program.getItems()) {
            check(item != null, "计算项不能为 null");
            key(item.getKey()); text(item.getLabel(), 120, "计算项名称"); text(item.getUnit(), 40, "结果单位");
            text(item.getExpression(), 256, "表达式"); scale(item.getAmountScale(), "结果精度");
            check(ROUNDING.contains(item.getRoundingMode()), "结果舍入方式须明确声明");
            check(!compiled.inputs.containsKey(item.getKey()) && compiled.items.put(item.getKey(), item) == null,
                    "输入和计算项编号不能重复：" + item.getKey());
            item.setLabel(item.getLabel().trim()).setUnit(item.getUnit().trim()).setExpression(item.getExpression().trim());
            Parser parser = new Parser(item.getExpression(), item.getKey());
            compiled.trees.put(item.getKey(), parser.parse());
            check(!parser.division || program.getDivisionScale() != null, "包含除法，须明确除法精度和舍入方式");
        }
        for (String item : compiled.items.keySet()) {
            Set<String> refs = compiled.trees.get(item).references();
            for (String ref : refs) check(compiled.inputs.containsKey(ref) || compiled.items.containsKey(ref), item + "引用未知编号：" + ref);
        }
        for (String item : compiled.items.keySet()) compiled.visit(item, new LinkedHashSet<>());
        compiled.programJson = JsonUtils.toJsonString(program);
        compiled.programHash = hash(compiled.programJson);
        return compiled;
    }
    private static void key(String key) {
        check(key != null && key.matches("[A-Za-z][A-Za-z0-9_]{0,63}") && !FUNCTIONS.contains(key), "编号不合法或占用函数名：" + key);
    }

    public static final class Compiled {
        private final HrmPayrollCalculationSpecVO program;
        private final Map<String, HrmPayrollCalculationSpecVO.Input> inputs = new LinkedHashMap<>();
        private final Map<String, HrmPayrollCalculationSpecVO.Item> items = new LinkedHashMap<>();
        private final Map<String, Node> trees = new LinkedHashMap<>();
        private final List<String> order = new ArrayList<>();
        private String programJson, programHash;
        private Compiled(HrmPayrollCalculationSpecVO program) { this.program = program; }
        public String getProgramJson() { return programJson; }
        public String getProgramHash() { return programHash; }
        public int getInputCount() { return inputs.size(); }
        public int getItemCount() { return items.size(); }
        public int getCaseCount() { return program.getCases().size(); }
        private void visit(String key, Set<String> path) {
            if (order.contains(key)) return;
            check(path.add(key), "循环依赖：" + String.join(" → ", path) + " → " + key);
            for (String dependency : trees.get(key).references()) if (items.containsKey(dependency)) visit(dependency, path);
            path.remove(key); order.add(key);
        }
        public HrmPayrollCalculationResultVO execute(Map<String, String> supplied) {
            check(supplied != null && supplied.keySet().equals(inputs.keySet()), "输入编号须与定义完全一致，不能缺失或额外填写");
            Map<String, BigDecimal> values = new LinkedHashMap<>();
            Map<String, String> visible = new LinkedHashMap<>();
            for (HrmPayrollCalculationSpecVO.Input input : inputs.values()) {
                BigDecimal number = number(supplied.get(input.getKey()), input.getScale(), "INTEGER".equals(input.getType()), input.getKey());
                values.put(input.getKey(), number); visible.put(input.getKey(), number.toPlainString());
            }
            List<HrmPayrollCalculationResultVO.Result> results = new ArrayList<>();
            for (String key : order) {
                HrmPayrollCalculationSpecVO.Item item = items.get(key);
                List<String> steps = new ArrayList<>();
                BigDecimal raw;
                try { raw = trees.get(key).evaluate(values, program, steps); }
                catch (ServiceException error) { throw exception(PAYROLL_CALCULATION_INVALID, key + "：" + error.getMessage()); }
                BigDecimal amount = bounded(raw.setScale(item.getAmountScale(), RoundingMode.valueOf(item.getRoundingMode())));
                steps.add("结果舍入 " + item.getAmountScale() + " 位 / " + item.getRoundingMode() + "：" + raw.toPlainString()
                        + " → " + amount.toPlainString() + "；后续项目引用此舍入结果");
                values.put(key, amount);
                results.add(new HrmPayrollCalculationResultVO.Result().setKey(key).setLabel(item.getLabel()).setUnit(item.getUnit())
                        .setExpression(item.getExpression()).setDependencies(new ArrayList<>(trees.get(key).references()))
                        .setRawResult(raw.toPlainString()).setAmount(amount.toPlainString()).setAmountScale(item.getAmountScale())
                        .setRoundingMode(item.getRoundingMode()).setSteps(steps));
            }
            return new HrmPayrollCalculationResultVO().setInputs(visible).setItems(results);
        }
        public HrmPayrollCalculationCasesVO verifyCases() {
            List<HrmPayrollCalculationCasesVO.CaseResult> results = new ArrayList<>();
            int passed = 0;
            for (HrmPayrollCalculationSpecVO.BusinessCase sample : program.getCases()) {
                Map<String, String> actual = new LinkedHashMap<>();
                boolean matches = false; String error = null;
                try {
                    check(sample != null, "样例缺失"); text(sample.getTitle(), 120, "样例名称");
                    check(sample.getExpected() != null && sample.getExpected().keySet().equals(items.keySet()), "样例预期须覆盖全部计算项，且不能额外填写");
                    for (HrmPayrollCalculationResultVO.Result result : execute(sample.getInputs()).getItems()) actual.put(result.getKey(), result.getAmount());
                    matches = true;
                    for (HrmPayrollCalculationSpecVO.Item item : items.values()) {
                        BigDecimal expected = number(sample.getExpected().get(item.getKey()), item.getAmountScale(), false, item.getKey() + "预期");
                        if (expected.compareTo(new BigDecimal(actual.get(item.getKey()))) != 0) matches = false;
                    }
                } catch (ServiceException failure) { matches = false; error = failure.getMessage(); }
                if (matches) passed++;
                results.add(new HrmPayrollCalculationCasesVO.CaseResult().setTitle(sample == null ? "缺失样例" : sample.getTitle())
                        .setExpected(sample == null || sample.getExpected() == null ? null : new LinkedHashMap<>(sample.getExpected()))
                        .setActual(actual).setError(error).setPassed(matches));
            }
            return new HrmPayrollCalculationCasesVO().setProgramHash(programHash).setTotal(results.size()).setPassed(passed)
                    .setAllPassed(!results.isEmpty() && passed == results.size()).setCases(results);
        }
    }

    private static final class Node {
        private final String operation, symbol;
        private final BigDecimal literal;
        private final List<Node> children;
        private Node(String operation, String symbol, BigDecimal literal, List<Node> children) {
            this.operation = operation; this.symbol = symbol; this.literal = literal; this.children = children;
        }
        private Set<String> references() {
            Set<String> result = new LinkedHashSet<>();
            if ("variable".equals(operation)) result.add(symbol);
            for (Node child : children) result.addAll(child.references());
            return result;
        }
        private BigDecimal evaluate(Map<String, BigDecimal> values, HrmPayrollCalculationSpecVO program, List<String> steps) {
            if ("number".equals(operation)) return literal;
            if ("variable".equals(operation)) {
                BigDecimal value = values.get(symbol); check(value != null, "缺少变量：" + symbol);
                steps.add(symbol + " = " + value.toPlainString()); return value;
            }
            List<BigDecimal> args = children.stream().map(c -> c.evaluate(values, program, steps)).collect(Collectors.toList());
            BigDecimal left = args.get(0), result;
            switch (operation) {
                case "negate": result = left.negate(); break;
                case "+": result = left.add(args.get(1)); break;
                case "-": result = left.subtract(args.get(1)); break;
                case "*": result = left.multiply(args.get(1)); break;
                case "/":
                    check(args.get(1).compareTo(BigDecimal.ZERO) != 0, "除零，须修正输入或规则");
                    result = left.divide(args.get(1), program.getDivisionScale(), RoundingMode.valueOf(program.getDivisionRoundingMode())); break;
                case "abs": result = left.abs(); break;
                case "min": result = args.stream().min(BigDecimal::compareTo).get(); break;
                case "max": result = args.stream().max(BigDecimal::compareTo).get(); break;
                default: throw new IllegalStateException("Uncompiled operation");
            }
            bounded(result);
            steps.add(operation + "(" + args.stream().map(BigDecimal::toPlainString).collect(Collectors.joining(", ")) + ") = "
                    + result.toPlainString() + ("/".equals(operation) ? "；除法 " + program.getDivisionScale() + " 位 / " + program.getDivisionRoundingMode() : ""));
            return result;
        }
    }

    private static final class Parser {
        private final String source, item;
        private int position, depth, nodes;
        private boolean division;
        private Parser(String source, String item) { this.source = source; this.item = item; }
        private void fail(String reason) { throw exception(PAYROLL_CALCULATION_INVALID, item + " 第 " + (position + 1) + " 位：" + reason); }
        private void whitespace() { while (position < source.length() && Character.isWhitespace(source.charAt(position))) position++; }
        private boolean take(char value) { whitespace(); if (position < source.length() && source.charAt(position) == value) { position++; return true; } return false; }
        private Node node(String operation, String symbol, BigDecimal literal, List<Node> children) {
            if (++nodes > 128) fail("表达式节点超过 128 个");
            return new Node(operation, symbol, literal, children);
        }
        private Node parse() {
            Node result = expression(); whitespace(); if (position != source.length()) fail("不支持的字符或相邻表达式"); return result;
        }
        private Node expression() {
            Node result = term();
            while (true) {
                if (take('+')) result = node("+", null, null, Arrays.asList(result, term()));
                else if (take('-')) result = node("-", null, null, Arrays.asList(result, term()));
                else return result;
            }
        }
        private Node term() {
            Node result = atom();
            while (true) {
                if (take('*')) result = node("*", null, null, Arrays.asList(result, atom()));
                else if (take('/')) { division = true; result = node("/", null, null, Arrays.asList(result, atom())); }
                else return result;
            }
        }
        private Node atom() {
            if (++depth > 32) fail("嵌套超过 32 层");
            try {
                whitespace();
                if (take('+')) return atom();
                if (take('-')) return node("negate", null, null, Collections.singletonList(atom()));
                if (take('(')) { Node value = expression(); if (!take(')')) fail("缺少右括号"); return value; }
                if (position >= source.length()) { fail("表达式不完整"); }
                char first = source.charAt(position);
                if (first >= '0' && first <= '9') {
                    int start = position;
                    while (position < source.length() && source.charAt(position) >= '0' && source.charAt(position) <= '9') position++;
                    if (position < source.length() && source.charAt(position) == '.') {
                        position++; int fractional = position;
                        while (position < source.length() && source.charAt(position) >= '0' && source.charAt(position) <= '9') position++;
                        if (position == fractional) fail("小数点后缺少数字");
                    }
                    return node("number", null, number(source.substring(start, position), 8, false, "常量"), Collections.emptyList());
                }
                if (first >= 'A' && first <= 'Z' || first >= 'a' && first <= 'z') {
                    int start = position++;
                    while (position < source.length()) {
                        char next = source.charAt(position);
                        if (!(next >= 'A' && next <= 'Z' || next >= 'a' && next <= 'z' || next >= '0' && next <= '9' || next == '_')) break;
                        position++;
                    }
                    String name = source.substring(start, position);
                    if (!take('(')) return node("variable", name, null, Collections.emptyList());
                    if (!FUNCTIONS.contains(name)) fail("只支持 abs、min、max 函数");
                    List<Node> args = new ArrayList<>();
                    if (!take(')')) {
                        do { args.add(expression()); if (args.size() > 16) fail("函数参数超过 16 个"); } while (take(','));
                        if (!take(')')) fail("函数缺少右括号");
                    }
                    if ("abs".equals(name) ? args.size() != 1 : args.size() < 2) fail("函数参数数量不合法");
                    return node(name, null, null, args);
                }
                fail("只支持十进制、声明编号、括号及 + - * / 运算"); return null;
            } finally { depth--; }
        }
    }
}
