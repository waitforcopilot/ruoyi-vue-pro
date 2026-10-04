package cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.intake;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hrm_payroll_import_batch")
public class HrmPayrollImportBatchDO extends TenantBaseDO {
    @TableId private Long id;
    private Long contractId;
    private String sourceCode;
    private Integer contractVersion;
    private String fileName;
    private String fileHash;
    private String idempotencyKey;
    private String declaredScope;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private Integer rowCount;
    private Integer validCount;
    private Integer errorCount;
    private Integer status;
    private Long createdBy;
    private String createdByName;
    private String snapshot;
}
