package cn.iocoder.yudao.module.hrm.service.payroll.identity;
import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.identity.HrmPayrollEmployeeMappingDO;
import cn.iocoder.yudao.module.hrm.dal.mysql.employee.info.HrmEmployeeMapper;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;
import java.util.*;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;

/** Both current and captured organization must be visible to scoped readers. */
@Component
public class HrmPayrollEmployeeAccess {
    @Resource private HrmEmployeeMapper employeeMapper;
    @Resource private PermissionApi permissionApi;
    public DeptDataPermissionRespDTO scope() {
        if(!permissionApi.hasAnyPermissions(getLoginUserId(),"hrm:employee:query")) throw exception(PAYROLL_MAPPING_PERMISSION);
        DeptDataPermissionRespDTO scope=permissionApi.getDeptDataPermission(getLoginUserId());
        return scope==null?new DeptDataPermissionRespDTO():scope;
    }
    public boolean visible(DeptDataPermissionRespDTO scope,Long dept,Long user) {
        return Boolean.TRUE.equals(scope.getAll()) || (scope.getDeptIds()!=null && dept!=null && scope.getDeptIds().contains(dept))
                || (Boolean.TRUE.equals(scope.getSelf()) && user!=null && user.equals(getLoginUserId()));
    }
    public HrmEmployeeDO employee(Long id,boolean lock) {
        QueryWrapper<HrmEmployeeDO> q=new QueryWrapper<HrmEmployeeDO>().eq("tenant_id",TenantContextHolder.getRequiredTenantId()).eq("id",id)
                .select("id","name","job_number","user_id","dept_id","entry_time","leave_time","status");
        if(lock) q.last("FOR UPDATE");
        HrmEmployeeDO person=id==null?null:employeeMapper.selectOne(q);
        if(person==null || !visible(scope(),person.getDeptId(),person.getUserId())) throw exception(PAYROLL_MAPPING_NOT_EXISTS);
        return person;
    }
    public void require(HrmPayrollEmployeeMappingDO row) {
        require(row.getEmployeeId(),row.getSnapshotDeptId(),row.getSnapshotUserId());
    }
    public void require(Long employeeId,Long capturedDept,Long capturedUser) {
        DeptDataPermissionRespDTO scope=scope();
        if(Boolean.TRUE.equals(scope.getAll())) return; // All-scope history remains readable after main-record deletion.
        if(!visible(scope,capturedDept,capturedUser)) throw exception(PAYROLL_MAPPING_NOT_EXISTS);
        employee(employeeId,false);
    }
    public LambdaQueryWrapperX<HrmPayrollEmployeeMappingDO> filter(LambdaQueryWrapperX<HrmPayrollEmployeeMappingDO> q) {
        return filter(q,"hrm_payroll_employee_mapping");
    }
    public <T> LambdaQueryWrapperX<T> filter(LambdaQueryWrapperX<T> q,String table) {
        if(!Arrays.asList("hrm_payroll_employee_mapping","hrm_payroll_employee_eligibility").contains(table))
            throw new IllegalArgumentException("Unsupported personnel snapshot table");
        DeptDataPermissionRespDTO scope=scope();if(Boolean.TRUE.equals(scope.getAll())) return q;
        List<Object> parameters=new ArrayList<>();
        String captured=predicate(scope,"snapshot_dept_id","snapshot_user_id",parameters);
        int index=parameters.size();parameters.add(TenantContextHolder.getRequiredTenantId());
        String current=predicate(scope,"e.dept_id","e.user_id",parameters);
        q.apply("("+captured+") AND EXISTS (SELECT 1 FROM hrm_employee e WHERE e.id="+table+".employee_id"
                +" AND e.tenant_id={"+index+"} AND e.deleted=0 AND ("+current+"))",parameters.toArray());
        return q;
    }
    private String predicate(DeptDataPermissionRespDTO scope,String dept,String user,List<Object> values) {
        List<String> clauses=new ArrayList<>();
        if(scope.getDeptIds()!=null && !scope.getDeptIds().isEmpty()) {
            List<String> slots=new ArrayList<>();for(Long id:new TreeSet<>(scope.getDeptIds())) { slots.add("{"+values.size()+"}");values.add(id); }
            clauses.add(dept+" IN ("+String.join(",",slots)+")");
        }
        if(Boolean.TRUE.equals(scope.getSelf())) { clauses.add(user+"={"+values.size()+"}");values.add(getLoginUserId()); }
        return clauses.isEmpty()?"1=0":String.join(" OR ",clauses);
    }
}
