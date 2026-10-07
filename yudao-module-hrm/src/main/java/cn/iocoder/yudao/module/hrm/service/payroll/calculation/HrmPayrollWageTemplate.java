package cn.iocoder.yudao.module.hrm.service.payroll.calculation;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.calculation.HrmPayrollCalculationSpecVO;
import java.util.*;
/** Editable industry defaults with synthetic test cases. Never persisted or confirmed implicitly. */
public final class HrmPayrollWageTemplate {
 private HrmPayrollWageTemplate() { }
 public static HrmPayrollCalculationSpecVO program() {
  HrmPayrollCalculationSpecVO p=new HrmPayrollCalculationSpecVO().setDivisionScale(8).setDivisionRoundingMode("HALF_UP");
  String[] keys={"baseSalary","allowance","performance","overtime","absenceDeduction","employeeInsurance","employeeFund","otherDeduction","withheldTax"};
  String[] labels={"基本工资","津贴","绩效","加班工资","缺勤扣款","个人社保","个人公积金","其他扣款","已核定个税"};
  for(int i=0;i<keys.length;i++) p.getInputs().add(new HrmPayrollCalculationSpecVO.Input().setKey(keys[i]).setLabel(labels[i]).setType("DECIMAL").setUnit("CNY").setScale(2));
  String[] outputs={"gross","deductions","tax","net"}, names={"应发","扣款合计","个税","实发"};
  String[] expressions={"baseSalary + allowance + performance + overtime","absenceDeduction + employeeInsurance + employeeFund + otherDeduction","withheldTax","gross - deductions - tax"};
  for(int i=0;i<outputs.length;i++) p.getItems().add(new HrmPayrollCalculationSpecVO.Item().setKey(outputs[i]).setLabel(names[i]).setUnit("CNY").setAmountScale(2).setRoundingMode("HALF_UP").setExpression(expressions[i]));
  p.getCases().add(sample("合成常规工资对账",keys,new String[]{"6000.00","500.00","1000.00","300.00","100.00","500.00","600.00","50.00","350.00"},outputs,new String[]{"7800.00","1250.00","350.00","6200.00"}));
  p.getCases().add(sample("合成显式零扣款",keys,new String[]{"1000.00","0.00","0.00","0.00","0.00","0.00","0.00","0.00","0.00"},outputs,new String[]{"1000.00","0.00","0.00","1000.00"}));
  return p;
 }
 private static HrmPayrollCalculationSpecVO.BusinessCase sample(String title,String[] keys,String[] values,String[] outputs,String[] expected) {
  Map<String,String> inputs=new LinkedHashMap<>(), amounts=new LinkedHashMap<>();
  for(int i=0;i<keys.length;i++)inputs.put(keys[i],values[i]);for(int i=0;i<outputs.length;i++)amounts.put(outputs[i],expected[i]);
  return new HrmPayrollCalculationSpecVO.BusinessCase().setTitle(title).setInputs(inputs).setExpected(amounts);
 }
}
