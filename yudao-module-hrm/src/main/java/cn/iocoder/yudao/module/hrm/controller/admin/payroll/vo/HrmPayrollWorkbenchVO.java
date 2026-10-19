package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo;

import lombok.Data;
import javax.validation.Valid;
import javax.validation.constraints.*;
import java.util.*;

/** Explicit commands: client cannot supply evidence identity, author, time or effective state. */
public final class HrmPayrollWorkbenchVO {
 private HrmPayrollWorkbenchVO() {}
 @Data public static class Context {
  @NotNull @Positive private Long projectId;
  @NotNull @Min(1) private Integer projectScopeVersion;
  @Positive private Long id;
  @Min(1) private Integer version;
  @NotBlank @Size(max=2000) private String reason;
 }
 @Data public static class Field {
  @NotBlank @Size(max=128) private String name;
  @NotBlank @Size(max=500) private String meaning;
  @NotBlank @Size(max=64) private String type;
  @NotBlank @Size(max=64) private String unit;
  @NotNull private Boolean required;
 }
 @Data public static class Source {
  @NotBlank @Size(max=200) private String name;
  @NotBlank @Pattern(regexp="OV|PLAN|CALC|TAX|ATT|OT|HOUR|INS|RPT|COL") private String module;
  @NotBlank @Size(max=12000) private String scopeJson;
  @NotNull @Positive private Long ownerId;
  @NotBlank @Size(max=200) private String technicalContact;
  @NotBlank @Size(max=1000) private String acquisition;
  @NotBlank @Size(max=1000) private String specification;
  @NotBlank @Size(max=1000) private String uniqueKey;
  @NotBlank @Size(max=1000) private String employeeMatch;
  @NotBlank @Size(max=500) private String frequency;
  @NotBlank @Size(max=500) private String cutoff;
  @NotBlank @Size(max=500) private String volume;
  @NotEmpty @Size(max=100) @Valid private List<Field> fields;
  @NotBlank @Size(max=1000) private String qualityRule;
  private Integer semanticVersion;
  private Integer operationalVersion;
  private Integer deliveryVersion;
  private String supply;
  private String deliveryDigest;
  private String deliveryReference;
  private Long copiedFromProjectId;
  private Long copiedFromSourceId;
 }
 @Data public static class SourceSave extends Context { @NotNull @Valid private Source source; }
 @Data public static class Copy extends Context {
  @NotNull @Positive private Long sourceProjectId;
  @NotNull @Positive private Long sourceId;
  @NotNull @Min(1) private Integer sourceVersion;
 }
 @Data public static class Relation {
  @NotNull @Positive private Long sourceId;
  @NotNull @Positive private Long requirementId;
  @NotEmpty @Size(max=100) @Valid private List<Mapping> fields;
  @NotBlank @Size(max=1000) private String consumptionRule;
  private Integer mappingVersion;
  private Boolean active;
 }
 @Data public static class Mapping {
  @NotBlank @Size(max=128) private String pageField;
  @NotBlank @Size(max=128) private String sourceField;
  @NotBlank @Size(max=500) private String meaning;
  @NotBlank @Size(max=64) private String type;
  @NotBlank @Size(max=64) private String unit;
  @NotNull private Boolean required;
  @NotBlank @Size(max=1000) private String transform;
  @NotBlank @Size(max=1000) private String nullRule;
  @NotBlank @Size(max=1000) private String duplicateRule;
  @NotBlank @Size(max=1000) private String periodRule;
  @NotBlank @Size(max=1000) private String exceptionRule;
 }
 @Data public static class RelationSave extends Context { @NotNull @Valid private Relation relation; }
 @Data public static class Supply extends Context {
  @NotBlank @Pattern(regexp="UNINVENTORIED|PENDING|PROVIDED|PROBLEM") private String supply;
  @Size(max=500) private String deliveryDigest;
  @Size(max=1000) private String deliveryReference;
  @NotNull private Boolean replaceDelivery;
 }
 @Data public static class Verify extends Context {
  @NotBlank @Pattern(regexp="SOURCE_SEMANTIC|RELATION_SEMANTIC|QS|QC") private String layer;
  @NotBlank @Pattern(regexp="PASS|FAIL") private String result;
  @NotBlank @Size(max=2000) private String reference;
  @NotBlank @Size(max=500) private String sampleDigest;
  @NotBlank @Size(max=64) private String expectedBinding;
 }
 @Data public static class Issue {
  @NotBlank @Size(max=2000) private String description;
  @NotNull @Size(max=100) private List<Long> requirementIds;
  @NotNull @Size(max=100) private List<Long> sourceIds;
  @NotNull private Boolean wholeProject;
  @NotBlank @Pattern(regexp="BASELINE_BLOCK|DATA_READINESS|ADVISORY") private String category;
  @NotNull @Positive private Long handlerId;
  @NotNull @Positive private Long reviewerId;
  @NotBlank @Pattern(regexp="[0-9]{4}-[0-9]{2}-[0-9]{2}") private String dueDate;
  @Size(max=100) private List<Long> relationIds = new ArrayList<>();
  private String state;
  private Integer cycle;
  private Boolean wasOverdue;
  private String closedDate;
  private String processing;
  private String review;
 }
 @Data public static class IssueSave extends Context { @NotNull @Valid private Issue issue; }
 @Data public static class IssueAction extends Context {
  @NotBlank @Pattern(regexp="START|SUBMIT|REJECT|CLOSE|REOPEN|RECLASSIFY|REASSIGN") private String action;
  @NotBlank @Size(max=2000) private String evidence;
  @Size(max=64) private String expectedBinding;
  @Pattern(regexp="[0-9]{4}-[0-9]{2}-[0-9]{2}") private String dueDate;
  @Pattern(regexp="BASELINE_BLOCK|DATA_READINESS|ADVISORY") private String category;
  private Boolean wholeProject;
  private List<Long> requirementIds;
  private List<Long> sourceIds;
  private Long handlerId;
  private Long reviewerId;
 }
 @Data public static class Proposal extends Context {
  @NotBlank @Size(max=4000) private String suggestion;
 }
}
