package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.scheme;
import lombok.Data;
import java.util.*;
/** Configuration only. No group membership, employee amounts or payroll results. */
@Data
public class HrmPayrollSchemeSnapshotVO {
    private Integer schemaVersion=1;
    private Group group;
    private TaxRule taxRule;
    private List<Option> options=new ArrayList<>();
    private List<String> issues=new ArrayList<>();
    @Data public static class Group { private Long id;private String name;private String salaryStandard;private String changeRule;private Long taxRuleId; }
    @Data public static class TaxRule { private Long id;private String name;private Integer type;private Boolean taxEnabled;private String threshold;private Integer decimalScale;private Integer cycleType; }
    @Data public static class Option { private Long id;private Integer code;private Integer parentCode;private Long templateId;private String name;private String remark;private Boolean systemFlag;private Integer type;private Boolean visible;private Boolean enabled;private Boolean taxEnabled;private Boolean calculateEnabled; }
}
