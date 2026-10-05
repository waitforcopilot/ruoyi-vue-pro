package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.calculation;
import lombok.Data;
import java.util.*;
@Data
public class HrmPayrollCalculationResultVO {
    private Map<String, String> inputs;
    private List<Result> items;
    @Data
    public static class Result {
        private String key;
        private String label;
        private String unit;
        private String expression;
        private List<String> dependencies;
        private String rawResult;
        private String amount;
        private Integer amountScale;
        private String roundingMode;
        private List<String> steps;
    }
}
