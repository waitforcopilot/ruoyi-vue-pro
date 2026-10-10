package cn.iocoder.yudao.module.hrm.dal.dataobject.payroll;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.*;
import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hrm_payroll_requirement")
@KeySequence("hrm_payroll_requirement_seq")
public class HrmPayrollRequirementDO extends BaseDO {
    @TableId private Long id;
    @ExcelProperty("需求编号") private String code;
    @ExcelProperty("模块") private String module;
    @ExcelProperty("功能") private String description;
    @ExcelProperty("字段映射") private String fieldMapping;
    @ExcelProperty("数据来源") private String sourceSystem;
    @ExcelProperty("来源负责人") private Long sourceOwnerId;
    @ExcelProperty("评审负责人") private Long reviewerId;
    @ExcelProperty("优先级") private Integer priority;
    @ExcelProperty("评审状态") private String status;
    @ExcelProperty("数据就绪") private String readiness;
    @ExcelProperty("验收条件") private String acceptance;
    @ExcelProperty("确认依据及异议") private String evidence;
    @ExcelProperty("版本") private Integer version;
    @ExcelProperty("确认人") private Long confirmedBy;
    @ExcelProperty("确认时间") private java.time.LocalDateTime confirmedAt;
}
