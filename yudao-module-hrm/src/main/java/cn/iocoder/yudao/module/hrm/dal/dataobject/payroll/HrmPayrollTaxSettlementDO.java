package cn.iocoder.yudao.module.hrm.dal.dataobject.payroll;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;
import java.time.*;
import lombok.*;
@TableName("hrm_payroll_tax_settlement") @KeySequence("hrm_payroll_tax_settlement_seq")
@Data @EqualsAndHashCode(callSuper=true)
public class HrmPayrollTaxSettlementDO extends BaseDO {
 @TableId private Long id;
 private Long filingId;
 private Long entityId;
 private Long employeeId;
 private Long profileId;
 private Integer year;
 private Integer month;
 private String mode;
 private String kind;
 private BigDecimal amount;
 @JsonIgnore private String taxpayerKey;
 private String taxRegistrationNumber;
 private String employeeAcknowledgement;
 private String evidence;
 private String reason;
 private String status;
 private Long submittedBy;
 private Long reviewedBy;
 private String reviewReason;
 private LocalDateTime reviewedTime;
 private LocalDate valueDate;
 private String bankReference;
 private String bankEvidence;
 private Long confirmedBy;
 private LocalDateTime confirmedTime;
 private String requestKey;
 @JsonIgnore private String requestSnapshot;
 @JsonIgnore private String authoritySnapshot;
 private Long accountingFilingId,accountedBy;
 private LocalDateTime accountedTime;
 private String accountingEvidence;
 @JsonIgnore private String accountingSnapshot,accountingRequest;
}
