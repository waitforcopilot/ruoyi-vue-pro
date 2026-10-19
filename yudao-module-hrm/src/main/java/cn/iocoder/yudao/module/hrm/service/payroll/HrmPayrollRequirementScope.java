package cn.iocoder.yudao.module.hrm.service.payroll;
import lombok.Data;
import java.time.LocalDate;
import java.util.*;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;
@Data public class HrmPayrollRequirementScope {
 private Set<Long> entities; private Set<Long> organizations;
 private Set<String> workerTypes; private Set<String> scenarios;
 @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
 private LocalDate from;
 @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
 private LocalDate to; private String explanation;
 public static HrmPayrollRequirementScope parse(String json) {
  try {
   HrmPayrollRequirementScope s=JsonUtils.parseObject(json,HrmPayrollRequirementScope.class);
   if(s==null || s.entities==null || s.entities.isEmpty() || s.organizations==null || s.organizations.isEmpty() || s.workerTypes==null || s.workerTypes.isEmpty() || s.scenarios==null || s.scenarios.isEmpty() || s.from==null || s.to==null || s.from.isAfter(s.to) || s.explanation==null || s.explanation.trim().isEmpty() || s.explanation.length()>2000 || s.entities.stream().anyMatch(x->x==null||x<=0) || s.organizations.stream().anyMatch(x->x==null||x<=0) || s.workerTypes.stream().anyMatch(x->x==null||x.trim().isEmpty()) || s.scenarios.stream().anyMatch(x->x==null||x.trim().isEmpty())) throw new IllegalArgumentException();
   return s;
  } catch(Exception ex) {throw exception(PAYROLL_PROJECT_SCOPE_INVALID);}
 }
 public boolean contains(HrmPayrollRequirementScope other) {
  return entities.containsAll(other.entities) && organizations.containsAll(other.organizations) && workerTypes.containsAll(other.workerTypes) && scenarios.containsAll(other.scenarios) && !from.isAfter(other.from) && !to.isBefore(other.to);
 }
}
