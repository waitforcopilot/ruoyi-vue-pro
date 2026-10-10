package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo;
import lombok.Data;
import lombok.AllArgsConstructor;
@Data @AllArgsConstructor
public class HrmPayrollBankDifferenceVO {
    private int row;
    private Long paymentId;
    private String reason;
}
