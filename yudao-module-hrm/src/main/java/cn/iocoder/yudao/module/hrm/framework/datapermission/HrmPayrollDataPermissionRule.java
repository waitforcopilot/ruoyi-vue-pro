package cn.iocoder.yudao.module.hrm.framework.datapermission;

import cn.iocoder.yudao.framework.common.biz.system.permission.PermissionCommonApi;
import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.datapermission.core.rule.DataPermissionRule;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/** Department/self scope intersects the existing subject/action checks.
 * Mixed-scope batch snapshots are hidden in full; they must never expose aggregate amounts.
 */
@Component
public class HrmPayrollDataPermissionRule implements DataPermissionRule {
    @Resource private PermissionCommonApi permissionApi;
    private static final String CONTEXT_KEY = HrmPayrollDataPermissionRule.class.getName();
    private static final Set<String> SHARED = new HashSet<>(Arrays.asList(
            "hrm_payroll_config_guard", "hrm_payroll_requirement", "hrm_payroll_requirement_history",
            "hrm_payroll_formula", "hrm_payroll_grade_policy", "hrm_payroll_bank_template",
            "hrm_payroll_legal_entity", "hrm_payroll_entity_grant", "hrm_payroll_insurance_policy",
            "hrm_payroll_tax_policy", "hrm_payroll_tax_policy_event", "hrm_payroll_financial_plan"));

    @Override
    public Set<String> getTableNames() {
        return TableInfoHelper.getTableInfos().stream().filter(info -> {
            String name = info.getTableName();
            return name.equals("hrm_employee") || name.startsWith("hrm_payroll_") || name.startsWith("hrm_salary_")
                    || (name.startsWith("hrm_") && info.getFieldList().stream().anyMatch(field -> field.getProperty().equals("employeeId")));
        }).map(TableInfo::getTableName).collect(Collectors.toSet());
    }

    @Override
    public Expression getExpression(String table, Alias alias) {
        LoginUser user = SecurityFrameworkUtils.getLoginUser();
        if (user == null || !Objects.equals(user.getUserType(), 2)) return null;
        DeptDataPermissionRespDTO scope = user.getContext(CONTEXT_KEY, DeptDataPermissionRespDTO.class);
        if (scope == null) {
            scope = permissionApi.getDeptDataPermission(user.getId());
            if (scope == null) throw new IllegalStateException("HRM department scope is unavailable");
            user.setContext(CONTEXT_KEY, scope);
        }
        if (Boolean.TRUE.equals(scope.getAll())) return null;
        TableInfo info = TableInfoHelper.getTableInfo(table);
        if (info == null) throw new IllegalStateException("HRM table metadata is unavailable: " + table);
        Map<String, String> columns = new HashMap<>();
        info.getFieldList().forEach(field -> columns.put(field.getProperty(), field.getColumn()));
        String prefix = alias == null ? table : alias.getName();
        String employees = employeeSelect(scope, user.getId());
        String predicate;
        if ("hrm_employee".equals(table)) {
            predicate = employeeCondition(prefix, scope, user.getId());
        } else if (columns.containsKey("employeeId")) {
            predicate = prefix + "." + columns.get("employeeId") + " IN (" + employees + ")";
        } else if ("hrm_salary_month_record".equals(table)) {
            predicate = batchCondition(prefix, employees);
        } else if (columns.containsKey("monthRecordId") || columns.containsKey("batchId")) {
            String column = columns.getOrDefault("monthRecordId", columns.get("batchId"));
            predicate = prefix + "." + column + " IN (" + batchSelect(employees) + ")";
        } else if (columns.containsKey("runId")) {
            predicate = prefix + "." + columns.get("runId") + " IN (SELECT scope_run.id FROM hrm_payroll_run scope_run WHERE "
                    + tenant("scope_run") + " AND scope_run.month_record_id IN (" + batchSelect(employees) + "))";
        } else if ("hrm_payroll_insurance_event".equals(table)) {
            predicate = "(" + prefix + ".base_id IS NULL OR " + prefix + ".base_id IN (SELECT scope_base.id FROM hrm_payroll_insurance_base scope_base WHERE "
                    + tenant("scope_base") + " AND scope_base.employee_id IN (" + employees + ")))";
        } else if (linkedParent(table) != null) {
            String[] link = linkedParent(table);
            Expression parent = getExpression(link[1], new Alias("scope_parent"));
            predicate = prefix + "." + link[0] + " IN (SELECT scope_parent.id FROM " + link[1]
                    + " scope_parent WHERE " + tenant("scope_parent") + (parent == null ? "" : " AND (" + parent + ")") + ")";
        } else if (SHARED.contains(table) || (table.startsWith("hrm_salary_")
                && !table.contains("slip_delivery_attempt"))) {
            // Structural salary configuration contains no employee amounts.
            return null;
        } else {
            // Entity-wide snapshots, import files and opaque history cannot be partially redacted safely.
            predicate = "1 = 0";
        }
        try {
            return CCJSqlParserUtil.parseCondExpression(predicate);
        } catch (net.sf.jsqlparser.JSQLParserException e) {
            throw new IllegalStateException("Cannot build HRM department scope", e);
        }
    }

    private String[] linkedParent(String table) {
        switch (table) {
            case "hrm_payroll_attendance_issue_event": return new String[]{"issue_id", "hrm_payroll_attendance_issue"};
            case "hrm_payroll_input_issue_event": return new String[]{"issue_id", "hrm_payroll_input_issue"};
            case "hrm_payroll_work_variance_event": return new String[]{"variance_id", "hrm_payroll_work_variance"};
            case "hrm_payroll_tax_profile_event": return new String[]{"profile_id", "hrm_payroll_tax_profile"};
            case "hrm_payroll_tax_settlement_event": return new String[]{"settlement_id", "hrm_payroll_tax_settlement"};
            case "hrm_payroll_bonus_event": return new String[]{"bonus_id", "hrm_payroll_bonus"};
            case "hrm_payroll_tax_deduction_event": return new String[]{"declaration_id", "hrm_payroll_tax_deduction"};
            case "hrm_payroll_retro_event": return new String[]{"retro_id", "hrm_payroll_retro"};
            case "hrm_payroll_offset_event": return new String[]{"offset_id", "hrm_payroll_offset"};
            case "hrm_payroll_wage_recovery_event": return new String[]{"recovery_id", "hrm_payroll_wage_recovery"};
            case "hrm_payroll_insurance_adjustment_event": return new String[]{"adjustment_id", "hrm_payroll_insurance_adjustment"};
            case "hrm_salary_slip_delivery": return new String[]{"slip_id", "hrm_salary_slip"};
            case "hrm_salary_slip_delivery_attempt": return new String[]{"delivery_id", "hrm_salary_slip_delivery"};
            default: return null;
        }
    }

    private String employeeSelect(DeptDataPermissionRespDTO scope, Long actor) {
        return "SELECT scope_employee.id FROM hrm_employee scope_employee WHERE " + tenant("scope_employee")
                + " AND (" + employeeCondition("scope_employee", scope, actor) + ")";
    }

    private String employeeCondition(String alias, DeptDataPermissionRespDTO scope, Long actor) {
        List<String> conditions = new ArrayList<>();
        if (scope.getDeptIds() != null && !scope.getDeptIds().isEmpty()) {
            String ids = scope.getDeptIds().stream().sorted().map(String::valueOf).collect(Collectors.joining(","));
            conditions.add(alias + ".dept_id IN (" + ids + ")");
        }
        if (Boolean.TRUE.equals(scope.getSelf())) conditions.add(alias + ".user_id = " + actor);
        return conditions.isEmpty() ? "1 = 0" : "(" + String.join(" OR ", conditions) + ")";
    }

    private String batchSelect(String employees) {
        return "SELECT scope_batch.id FROM hrm_salary_month_record scope_batch WHERE " + tenant("scope_batch")
                + " AND " + batchCondition("scope_batch", employees);
    }

    private String batchCondition(String batch, String employees) {
        // Require a provable participant scope; empty/orphan snapshots must not become visible.
        String records = "SELECT 1 FROM hrm_salary_month_employee_record scope_record WHERE "
                + tenant("scope_record") + " AND scope_record.month_record_id = " + batch + ".id";
        String members = "SELECT 1 FROM hrm_payroll_entity_membership scope_member WHERE "
                + tenant("scope_member") + " AND scope_member.entity_id = " + batch + ".entity_id"
                + " AND scope_member.status = 'APPROVED' AND scope_member.effective_from <= " + batch + ".end_time"
                + " AND scope_member.effective_to >= " + batch + ".start_time";
        return "((EXISTS (" + records + " AND scope_record.employee_id IN (" + employees + "))"
                + " OR EXISTS (" + members + " AND scope_member.employee_id IN (" + employees + ")))"
                + " AND NOT EXISTS (" + records + " AND scope_record.employee_id NOT IN (" + employees + "))"
                + " AND NOT EXISTS (" + members + " AND scope_member.employee_id NOT IN (" + employees + ")))";
    }

    private String tenant(String alias) {
        return alias + ".tenant_id = " + TenantContextHolder.getRequiredTenantId() + " AND " + alias + ".deleted = 0";
    }
}
