package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.trial;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import java.time.LocalDateTime;
import java.util.*;
@Data
public class HrmPayrollTrialStoredConfigVO {
 private HrmPayrollTrialConfigVO.Roles roles;
 private List<HrmPayrollTrialConfigVO.SourceBinding> sourceBindings;
 private List<Person> people=new ArrayList<>();
 @Data @EqualsAndHashCode(callSuper=true) public static class Person extends HrmPayrollTrialConfigVO.PersonInput {
  private String snapshotName; private String snapshotJobNumber; private Long snapshotDeptId; private Long snapshotUserId;
  @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using=com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer.class)
 @JsonFormat(shape=JsonFormat.Shape.STRING,pattern="yyyy-MM-dd'T'HH:mm:ss") private LocalDateTime snapshotCapturedAt;
 }
}
