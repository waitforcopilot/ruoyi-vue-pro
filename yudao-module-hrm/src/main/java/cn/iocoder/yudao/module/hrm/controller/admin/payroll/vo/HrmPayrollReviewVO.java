package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo;
import lombok.Data;
import javax.validation.constraints.*;
import java.util.*;
public class HrmPayrollReviewVO {
 @Data public static class Business {
  private String page,anchor,targetType,contextModule;
  private String role,scenario,goal,expected,normalCase,exceptionCase,calculation,unit,precision,validity,ruleException;
  private Long disputeOwnerId; private String nextStep;
 }
 @Data public static class Context {
  @NotNull @Positive private Long projectId;
  @NotNull @Min(1) private Integer projectScopeVersion;
  @NotNull @Positive private Long id;
  @NotNull @Min(1) private Integer version;
  @NotBlank @Size(max=2000) private String reason;
 }
 @Data @lombok.EqualsAndHashCode(callSuper=true) public static class Decision extends Context {
  @NotBlank @Pattern(regexp="CANDIDATE|INCLUDED|DEFERRED|REMOVED") private String decision;
  private Long responsibleId,replacementId;
  @Size(max=2000) private String reevaluate;
  @Pattern(regexp="DUPLICATE|MISTAKE|OUT_OF_SCOPE") private String removalKind;
 }
 @Data @lombok.EqualsAndHashCode(callSuper=true) public static class Implementation extends Context {
  @NotBlank @Pattern(regexp="UNSCHEDULED|DEVELOPING|ACCEPTANCE_PENDING|ACCEPTED") private String state;
  @Size(max=2000) private String page,api,task,evidence;
 }
 @Data @lombok.EqualsAndHashCode(callSuper=true) public static class Collaboration extends Context {
  @NotBlank @Pattern(regexp="MEETING|REMINDER") private String kind;
  @NotBlank @Size(max=2000) private String reference;
  private Long recipientId;
 }
 @Data public static class BatchAssignment {
  @NotNull @Positive private Long projectId;
  @NotNull @Min(1) private Integer projectScopeVersion;
  @NotNull @Positive private Long reviewerId;
  @NotEmpty @Size(max=50) @javax.validation.Valid private List<Context> items;
 }
}
