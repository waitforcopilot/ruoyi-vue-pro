package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.trial;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.calculation.PlainDecimalStringDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;
import javax.validation.Valid;
import javax.validation.constraints.*;
import java.util.*;
@Data
public class HrmPayrollTrialConfigVO {
 @Valid @NotNull private Roles roles=new Roles();
 @Valid @NotNull @Size(min=1,max=100) private List<@NotNull PersonInput> people=new ArrayList<>();
 @Data public static class Roles {
  @NotBlank @Pattern(regexp="[A-Za-z][A-Za-z0-9_]{0,63}") private String gross="gross";
  @NotBlank @Pattern(regexp="[A-Za-z][A-Za-z0-9_]{0,63}") private String deductions="deductions";
  @NotBlank @Pattern(regexp="[A-Za-z][A-Za-z0-9_]{0,63}") private String tax="tax";
  @NotBlank @Pattern(regexp="[A-Za-z][A-Za-z0-9_]{0,63}") private String net="net";
 }
 @Data public static class PersonInput {
  @NotNull @Min(1) private Long employeeId;
  @Pattern(regexp="[a-f0-9]{64}") private String employeeFingerprint;
  @NotNull @Size(max=32) @JsonDeserialize(contentUsing=PlainDecimalStringDeserializer.class)
  private Map<@Pattern(regexp="[A-Za-z][A-Za-z0-9_]{0,63}") String,@NotNull @Size(max=40) String> inputs=new LinkedHashMap<>();
  @Size(max=2000) private String inputReference;
 }
}
