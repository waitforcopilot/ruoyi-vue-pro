package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.trial;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.calculation.*;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.eligibility.HrmPayrollEligibilityRespVO;
import lombok.Data;
import java.util.*;
@Data
public class HrmPayrollTrialResultVO {
 private Integer schemaVersion=1;
 private HrmPayrollTrialRespVO batch;
 private HrmPayrollCalculationRespVO definition;
 private cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.scheme.HrmPayrollSchemeRespVO scheme;
 private String programHash;
 private HrmPayrollTrialCheckVO check;
 private Map<String,String> totals=new LinkedHashMap<>();
 private List<Person> people=new ArrayList<>();
 @Data public static class Person {
  private HrmPayrollTrialStoredConfigVO.Person input;
  private HrmPayrollEligibilityRespVO eligibility;
  private String state;
  private HrmPayrollCalculationResultVO calculation;
  private Map<String,String> amounts=new LinkedHashMap<>();
 }
}
