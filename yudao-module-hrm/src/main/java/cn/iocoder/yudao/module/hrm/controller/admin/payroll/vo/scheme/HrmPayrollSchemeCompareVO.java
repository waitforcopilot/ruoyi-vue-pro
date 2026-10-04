package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.scheme;
import lombok.Data;
import java.util.*;
@Data
public class HrmPayrollSchemeCompareVO {
    private HrmPayrollSchemeRespVO left;private HrmPayrollSchemeRespVO right;
    private List<Change> changes=new ArrayList<>();
    @Data public static class Change { private String path;private String label;private String left;private String right;private String kind; }
}
