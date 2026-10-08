package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.trial;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.*;
@Data
public class HrmPayrollTrialCheckVO {
 private Long batchId; private Integer revision; private Boolean ready; private String sourceHash;
 private Integer includedCount; private Integer excludedCount; private Integer blockedCount;
 @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using=com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer.class)
 @JsonFormat(shape=JsonFormat.Shape.STRING,pattern="yyyy-MM-dd'T'HH:mm:ss") private LocalDateTime checkedAt;
 private List<Issue> issues=new ArrayList<>();
 private List<Person> people=new ArrayList<>();
 @Data public static class Issue { private String code; private String message; }
 @Data public static class Person {
  private Long employeeId; private String name; private String state;
  private Long eligibilityId; private Integer eligibilityVersion;
  private List<Issue> issues=new ArrayList<>();
 }
}
