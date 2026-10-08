package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.trial;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.*;
@Data
public class HrmPayrollTrialRespVO {
 private Long id; private String code; private String title; private String entityCode; private String entityName;
 private String periodType;
 @JsonFormat(shape=JsonFormat.Shape.STRING,pattern="yyyy-MM-dd") private LocalDate periodStart;
 @JsonFormat(shape=JsonFormat.Shape.STRING,pattern="yyyy-MM-dd") private LocalDate periodEnd;
 private Long definitionId; private Integer revision; private Integer status; private Integer personCount;
 private String ownerName; private String reference; private Long currentRunId; private Long latestRunId;
 private Long activeReviewId; private Long frozenRunId;
 private HrmPayrollTrialStoredConfigVO configuration;
}
