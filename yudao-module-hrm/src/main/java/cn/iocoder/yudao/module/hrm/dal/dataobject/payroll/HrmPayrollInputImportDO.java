package cn.iocoder.yudao.module.hrm.dal.dataobject.payroll;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
@Data
@EqualsAndHashCode(callSuper=true)
@TableName("hrm_payroll_input_import")
@KeySequence("hrm_payroll_input_import_seq")
public class HrmPayrollInputImportDO extends BaseDO {
  @TableId private Long id;
  private Long batchId;
  private Long entityId;
  private Integer version;
  private String fileName;
  private String digest;
  private String rowsSnapshot;
  private String errors;
  private String requests;
  private String inputIds;
  private String status;
}
