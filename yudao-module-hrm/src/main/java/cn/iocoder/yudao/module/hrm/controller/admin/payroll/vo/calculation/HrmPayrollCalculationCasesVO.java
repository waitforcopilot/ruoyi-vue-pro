package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.calculation;
import lombok.Data;
import java.util.*;
@Data
public class HrmPayrollCalculationCasesVO {
    private String programHash;
    private Integer total;
    private Integer passed;
    private Boolean allPassed;
    private List<CaseResult> cases;
    @Data
    public static class CaseResult {
        private String title;
        private Boolean passed;
        private String error;
        private Map<String, String> expected;
        private Map<String, String> actual;
    }
}
