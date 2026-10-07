package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.trial;
import lombok.Data;
import java.util.*;
@Data
public class HrmPayrollTrialCompareVO {
 private HrmPayrollTrialRunRespVO left; private HrmPayrollTrialRunRespVO right; private Boolean ruleChanged;
 private Map<String,String> totalDifferences=new LinkedHashMap<>();
 private List<Person> people=new ArrayList<>();
 @Data public static class Person {
  private Long employeeId; private String name; private String change;
  private Map<String,String> leftAmounts; private Map<String,String> rightAmounts; private Map<String,String> differences;
  private Map<String,String> leftInputs; private Map<String,String> rightInputs;
 }
}
