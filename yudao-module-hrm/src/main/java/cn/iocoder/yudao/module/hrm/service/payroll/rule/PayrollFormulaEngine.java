package cn.iocoder.yudao.module.hrm.service.payroll.rule;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/** Payroll arithmetic only: decimal literals, variables, parentheses and + - * /. */
public final class PayrollFormulaEngine {
    private static final int MAX_LENGTH = 1000;
    private static final int MAX_VARIABLES = 200;
    private static final int SCALE = 10;
    private PayrollFormulaEngine() {}

    /** Validate syntax without inventing sample amounts or evaluating denominators. */
    public static Set<String> variables(String expression) {
        if (expression == null || expression.length() > MAX_LENGTH) throw new IllegalArgumentException("公式为空或过长");
        Set<String> variables = new LinkedHashSet<>();
        Parser parser = new Parser(expression, name -> {variables.add(name);return BigDecimal.ONE;}, true);
        parser.expression();parser.space();
        if (parser.position != expression.length()) throw new IllegalArgumentException("公式包含无效字符");
        if (variables.size() > MAX_VARIABLES) throw new IllegalArgumentException("公式变量数量超限");
        return Collections.unmodifiableSet(variables);
    }

    public static void validateGraph(Map<String,String> formulas, Set<String> availableInputs) {
        if(formulas.size()>MAX_VARIABLES)throw new IllegalArgumentException("公式数量超限");
        Map<String,Set<String>> references=new LinkedHashMap<>();
        for(Map.Entry<String,String> formula:formulas.entrySet()) {
            checkName(formula.getKey());Set<String> names=variables(formula.getValue());
            for(String name:names)if(!formulas.containsKey(name)&&!availableInputs.contains(name))throw new IllegalArgumentException("缺少变量："+name);
            references.put(formula.getKey(),names);
        }
        Set<String> done=new HashSet<>(),visiting=new HashSet<>();
        for(String name:formulas.keySet())visit(name,references,done,visiting);
    }
    private static void visit(String name,Map<String,Set<String>> references,Set<String> done,Set<String> visiting) {
        if(done.contains(name))return;
        if(!visiting.add(name))throw new IllegalArgumentException("公式循环依赖："+name);
        for(String dependency:references.get(name))if(references.containsKey(dependency))visit(dependency,references,done,visiting);
        visiting.remove(name);done.add(name);
    }

    public static Map<String, BigDecimal> evaluate(Map<String, String> formulas, Map<String, BigDecimal> inputs) {
        if (formulas.size() + inputs.size() > MAX_VARIABLES) throw new IllegalArgumentException("公式变量数量超限");
        Map<String, BigDecimal> results = new LinkedHashMap<>();
        for (Map.Entry<String, BigDecimal> input : inputs.entrySet()) {
            checkName(input.getKey());
            if (input.getValue() == null) throw new IllegalArgumentException("缺少变量：" + input.getKey());
            results.put(input.getKey(), input.getValue());
        }
        for (String name : formulas.keySet()) {
            checkName(name);
            if (inputs.containsKey(name)) throw new IllegalArgumentException("公式与输入变量重名：" + name);
        }
        Set<String> visiting = new HashSet<>();
        for (String name : formulas.keySet()) resolve(name, formulas, results, visiting);
        return Collections.unmodifiableMap(results);
    }

    private static BigDecimal resolve(String name, Map<String, String> formulas, Map<String, BigDecimal> values, Set<String> visiting) {
        if (values.containsKey(name)) return values.get(name);
        if (!formulas.containsKey(name)) throw new IllegalArgumentException("缺少变量：" + name);
        if (!visiting.add(name)) throw new IllegalArgumentException("公式循环依赖：" + name);
        String expression = formulas.get(name);
        if (expression == null || expression.length() > MAX_LENGTH) throw new IllegalArgumentException("公式为空或过长");
        Parser parser = new Parser(expression, variable -> resolve(variable, formulas, values, visiting));
        BigDecimal result = parser.expression();
        parser.space();
        if (parser.position != expression.length()) throw new IllegalArgumentException("公式包含无效字符，位置 " + parser.position);
        if (result.precision() > 30) throw new IllegalArgumentException("计算结果超出金额精度");
        visiting.remove(name);
        values.put(name, result);
        return result;
    }

    private static void checkName(String name) {
        if (name == null || !name.matches("[A-Za-z_][A-Za-z0-9_]{0,63}")) throw new IllegalArgumentException("变量名称不合法");
    }

    private static final class Parser {
        private final String text;
        private final java.util.function.Function<String, BigDecimal> variables;
        private final boolean validateOnly;
        private int position;
        private int depth;
        Parser(String text, java.util.function.Function<String, BigDecimal> variables) { this(text,variables,false); }
        Parser(String text, java.util.function.Function<String, BigDecimal> variables,boolean validateOnly) { this.text=text;this.variables=variables;this.validateOnly=validateOnly; }
        void space() { while(position<text.length() && Character.isWhitespace(text.charAt(position))) position++; }
        boolean take(char c) { space(); if(position<text.length() && text.charAt(position)==c){position++;return true;}return false; }
        BigDecimal expression() {
            BigDecimal value=term();
            while(true) {
                if(take('+')){BigDecimal right=term();value=validateOnly?BigDecimal.ONE:value.add(right);}
                else if(take('-')){BigDecimal right=term();value=validateOnly?BigDecimal.ONE:value.subtract(right);}
                else return value;
            }
        }
        BigDecimal term() {
            BigDecimal value=factor();
            while(true) {
                if(take('*')){BigDecimal right=factor();value=validateOnly?BigDecimal.ONE:value.multiply(right);}
                else if(take('/')) {BigDecimal divisor=factor();if(!validateOnly&&divisor.signum()==0)throw new IllegalArgumentException("公式除数不能为零");value=validateOnly?BigDecimal.ONE:value.divide(divisor,SCALE,RoundingMode.HALF_UP);}
                else return value;
                if(value.precision()>40)throw new IllegalArgumentException("中间计算结果超限");
            }
        }
        BigDecimal factor() {
            if(++depth>32)throw new IllegalArgumentException("公式嵌套过深");
            try {
                if(take('+'))return factor();
                if(take('-'))return factor().negate();
                if(take('(')){BigDecimal value=expression();if(!take(')'))throw new IllegalArgumentException("公式缺少右括号");return value;}
                space();int begin=position;
                if(position<text.length() && (Character.isLetter(text.charAt(position)) || text.charAt(position)=='_')) {
                    while(position<text.length() && (Character.isLetterOrDigit(text.charAt(position)) || text.charAt(position)=='_'))position++;
                    String name=text.substring(begin,position);checkName(name);return variables.apply(name);
                }
                while(position<text.length() && (Character.isDigit(text.charAt(position)) || text.charAt(position)=='.'))position++;
                if(begin==position)throw new IllegalArgumentException("公式需要金额或变量，位置 "+position);
                String literal=text.substring(begin,position);
                if(literal.length()>30)throw new IllegalArgumentException("金额常量过长");
                try{return new BigDecimal(literal);}catch(NumberFormatException ex){throw new IllegalArgumentException("金额常量不合法",ex);}
            } finally {depth--;}
        }
    }
}
