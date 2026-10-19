package cn.iocoder.yudao.module.hrm.dal.dataobject.payroll;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
/** Project-owned typed workbench objects. Evidence/history are append-only in the service. */
@Data @EqualsAndHashCode(callSuper=true) @TableName("hrm_payroll_workbench_record")
@KeySequence("hrm_payroll_workbench_record_seq")
public class HrmPayrollWorkbenchRecordDO extends BaseDO {
 @TableId private Long id;
 private Long projectId;
 private String kind;
 private Integer version;
 private String dataJson;
}
