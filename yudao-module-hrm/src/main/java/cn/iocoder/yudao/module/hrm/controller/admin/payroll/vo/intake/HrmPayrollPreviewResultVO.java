package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.intake;

import lombok.Data;
import java.util.*;

@Data
public class HrmPayrollPreviewResultVO {
    private List<Issue> globalIssues = new ArrayList<>();
    private List<Row> rows = new ArrayList<>();
    private Integer rowCount;
    private Integer validCount;
    private Integer errorCount;
    private Boolean employeeMatchEnabled;
    private String employeeMatchMode;

    @Data
    public static class Row {
        private Integer line;
        private Map<String, String> values = new LinkedHashMap<>();
        private Long employeeId;
        private cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.identity.HrmPayrollMappingLookupVO.Match employeeMapping;
        private List<Issue> issues = new ArrayList<>();
    }
    @Data
    public static class Issue {
        private Integer line;
        private String field;
        private String code;
        private String message;
    }
}
