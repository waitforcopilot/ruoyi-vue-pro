-- Immutable trial executions; no legacy wage tables or grants are changed.
CREATE TABLE IF NOT EXISTS hrm_payroll_trial_batch (
 id bigint NOT NULL AUTO_INCREMENT,
 code varchar(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,title varchar(160) NOT NULL,
 entity_code varchar(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,entity_name varchar(160) NOT NULL,
 period_type varchar(16) NOT NULL,period_start date NOT NULL,period_end date NOT NULL,
 definition_id bigint NOT NULL,revision int NOT NULL,status int NOT NULL DEFAULT 0,person_count int NOT NULL,
 owner_name varchar(120),reference text,configuration_json mediumtext NOT NULL,current_run_id bigint,latest_run_id bigint,
 creator varchar(64) DEFAULT '',create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updater varchar(64) DEFAULT '',update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 deleted bit NOT NULL DEFAULT 0,tenant_id bigint NOT NULL,
 PRIMARY KEY(id),UNIQUE KEY uk_trial_batch(tenant_id,code),KEY idx_trial_period(tenant_id,entity_code,period_start,period_end)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS hrm_payroll_trial_person (
 id bigint NOT NULL AUTO_INCREMENT,batch_id bigint NOT NULL,employee_id bigint NOT NULL,
 snapshot_dept_id bigint,snapshot_user_id bigint,employee_fingerprint char(64) NOT NULL,
 creator varchar(64) DEFAULT '',create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updater varchar(64) DEFAULT '',update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 deleted bit NOT NULL DEFAULT 0,tenant_id bigint NOT NULL,
 PRIMARY KEY(id),UNIQUE KEY uk_trial_footprint(tenant_id,batch_id,employee_id,employee_fingerprint),KEY idx_trial_person_scope(tenant_id,batch_id,employee_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS hrm_payroll_trial_run (
 id bigint NOT NULL AUTO_INCREMENT,batch_id bigint NOT NULL,run_version int NOT NULL,
 request_key varchar(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,expected_revision int NOT NULL,source_hash char(64) NOT NULL,
 result_json mediumtext NOT NULL,included_count int NOT NULL,excluded_count int NOT NULL,
 executed_by bigint NOT NULL,executed_by_name varchar(120),executed_at datetime NOT NULL,
 creator varchar(64) DEFAULT '',create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updater varchar(64) DEFAULT '',update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 deleted bit NOT NULL DEFAULT 0,tenant_id bigint NOT NULL,
 PRIMARY KEY(id),UNIQUE KEY uk_trial_run(tenant_id,batch_id,run_version),UNIQUE KEY uk_trial_request(tenant_id,batch_id,request_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
INSERT INTO system_menu(name,permission,type,sort,parent_id,path,icon,component,component_name)
SELECT '批次核验与试算','hrm:payroll:trial:query',2,110,m.id,'payroll-trial-batches','ep:coin','hrm/payroll/trial/index','HrmPayrollTrial'
FROM system_menu m WHERE m.path='/hrm' AND m.deleted=0
AND NOT EXISTS(SELECT 1 FROM system_menu WHERE permission='hrm:payroll:trial:query' AND deleted=0) LIMIT 1;
INSERT INTO system_menu(name,permission,type,sort,parent_id,path,icon,component,component_name)
SELECT '维护试算草稿','hrm:payroll:trial:maintain',3,1,m.id,'','','',''
FROM system_menu m WHERE m.component='hrm/payroll/trial/index' AND m.deleted=0
AND NOT EXISTS(SELECT 1 FROM system_menu WHERE permission='hrm:payroll:trial:maintain' AND deleted=0) LIMIT 1;
INSERT INTO system_menu(name,permission,type,sort,parent_id,path,icon,component,component_name)
SELECT '执行批次试算','hrm:payroll:trial:execute',3,2,m.id,'','','',''
FROM system_menu m WHERE m.component='hrm/payroll/trial/index' AND m.deleted=0
AND NOT EXISTS(SELECT 1 FROM system_menu WHERE permission='hrm:payroll:trial:execute' AND deleted=0) LIMIT 1;
