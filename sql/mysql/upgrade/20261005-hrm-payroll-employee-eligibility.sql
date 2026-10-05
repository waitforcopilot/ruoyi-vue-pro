-- Explicit declared entity/person qualifications; no inferred eligibility or role grants.
CREATE TABLE IF NOT EXISTS hrm_payroll_employee_eligibility (
 id bigint NOT NULL AUTO_INCREMENT,
 entity_code varchar(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,entity_name varchar(160) NOT NULL,
 employee_id bigint NOT NULL,eligibility_version int NOT NULL,revision int NOT NULL,status int NOT NULL DEFAULT 0,
 qualification varchar(16) CHARACTER SET ascii COLLATE ascii_bin,owner_name varchar(120),reference text,reason text,
 effective_from date,effective_to date,
 snapshot_name varchar(120),snapshot_job_number varchar(64),snapshot_dept_id bigint,snapshot_user_id bigint,
 snapshot_entry_time datetime,snapshot_leave_time datetime,snapshot_employee_status int,
 snapshot_captured_at datetime NOT NULL,employee_fingerprint char(64) NOT NULL,
 reviewed_by bigint,reviewed_by_name varchar(120),reviewed_time datetime,evidence text,
 creator varchar(64) DEFAULT '',create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updater varchar(64) DEFAULT '',update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 deleted bit NOT NULL DEFAULT 0,tenant_id bigint NOT NULL,
 PRIMARY KEY(id),UNIQUE KEY uk_eligibility_version(tenant_id,entity_code,employee_id,eligibility_version),
 KEY idx_eligibility_period(tenant_id,entity_code,employee_id,status,effective_from,effective_to)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
INSERT INTO system_menu(name,permission,type,sort,parent_id,path,icon,component,component_name)
SELECT '计薪人员资格与期间','hrm:payroll:eligibility:query',2,100,m.id,'payroll-employee-eligibility','ep:user-filled','hrm/payroll/eligibility/index','HrmPayrollEligibility'
FROM system_menu m WHERE m.path='/hrm' AND m.deleted=0
AND NOT EXISTS(SELECT 1 FROM system_menu WHERE permission='hrm:payroll:eligibility:query' AND deleted=0) LIMIT 1;
INSERT INTO system_menu(name,permission,type,sort,parent_id,path,visible)
SELECT '维护计薪资格','hrm:payroll:eligibility:maintain',3,1,m.id,'',0
FROM system_menu m WHERE m.component='hrm/payroll/eligibility/index' AND m.deleted=0
AND NOT EXISTS(SELECT 1 FROM system_menu WHERE permission='hrm:payroll:eligibility:maintain' AND deleted=0) LIMIT 1;
INSERT INTO system_menu(name,permission,type,sort,parent_id,path,visible)
SELECT '评审计薪资格','hrm:payroll:eligibility:review',3,2,m.id,'',0
FROM system_menu m WHERE m.component='hrm/payroll/eligibility/index' AND m.deleted=0
AND NOT EXISTS(SELECT 1 FROM system_menu WHERE permission='hrm:payroll:eligibility:review' AND deleted=0) LIMIT 1;
