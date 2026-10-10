package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo;
import lombok.Data;
import java.math.BigDecimal;
@Data
public class HrmPayrollPaymentBatchRespVO {
    private Long id;
    private String title;
    private Integer status;
    private Integer employeeCount;
    private BigDecimal realPaySalary;
    private Long runId;
}
