-- Requires the HRM baseline and payroll requirements/intake migrations. No people or role grants are seeded.
CREATE TABLE IF NOT EXISTS hrm_payroll_employee_mapping (
 id bigint NOT NULL AUTO_INCREMENT,
 identity_key char(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 source_id bigint NOT NULL, source_code varchar(64) NOT NULL,
 namespace varchar(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 external_code varchar(128) COLLATE utf8mb4_bin NOT NULL,
 mapping_version int NOT NULL, revision int NOT NULL, status int NOT NULL DEFAULT 0,
 employee_id bigint NOT NULL, owner_name varchar(120), reference text,
 effective_from date, effective_to date,
 snapshot_name varchar(120), snapshot_job_number varchar(64), snapshot_dept_id bigint, snapshot_user_id bigint,
 snapshot_entry_time datetime, snapshot_leave_time datetime, snapshot_employee_status int,
 snapshot_captured_at datetime NOT NULL, employee_fingerprint char(64) NOT NULL,
 reviewed_by bigint, reviewed_by_name varchar(120), reviewed_time datetime, evidence text,
 creator varchar(64) DEFAULT '', create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updater varchar(64) DEFAULT '', update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 deleted bit NOT NULL DEFAULT 0, tenant_id bigint NOT NULL,
 PRIMARY KEY(id), UNIQUE KEY uk_identity_version(tenant_id,identity_key,mapping_version),
 KEY idx_identity_lookup(tenant_id,source_id,namespace,external_code,status,deleted),
 KEY idx_identity_employee(tenant_id,employee_id,deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
INSERT INTO system_menu(name,permission,type,sort,parent_id,path,icon,component,component_name)
SELECT '薪酬人员编号映射','hrm:payroll:identity:query',2,93,root.id,'payroll-identity','ep:connection','hrm/payroll/identity/index','HrmPayrollIdentity'
FROM system_menu root WHERE root.path='/hrm' AND root.parent_id=0 AND root.deleted=0
AND NOT EXISTS(SELECT 1 FROM system_menu WHERE component='hrm/payroll/identity/index' AND deleted=0) LIMIT 1;
INSERT INTO system_menu(name,permission,type,sort,parent_id,path,visible)
SELECT '维护人员编号映射','hrm:payroll:identity:maintain',3,1,p.id,'',0 FROM system_menu p
WHERE p.component='hrm/payroll/identity/index' AND p.deleted=0
AND NOT EXISTS(SELECT 1 FROM system_menu WHERE permission='hrm:payroll:identity:maintain' AND deleted=0) LIMIT 1;
INSERT INTO system_menu(name,permission,type,sort,parent_id,path,visible)
SELECT '评审人员编号映射','hrm:payroll:identity:review',3,2,p.id,'',0 FROM system_menu p
WHERE p.component='hrm/payroll/identity/index' AND p.deleted=0
AND NOT EXISTS(SELECT 1 FROM system_menu WHERE permission='hrm:payroll:identity:review' AND deleted=0) LIMIT 1;
