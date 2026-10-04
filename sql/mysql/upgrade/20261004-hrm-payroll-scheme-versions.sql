-- Existing HRM salary configuration is read only. No business defaults or role grants.
CREATE TABLE IF NOT EXISTS hrm_payroll_scheme_version (
 id bigint NOT NULL AUTO_INCREMENT,group_id bigint NOT NULL,group_name varchar(64) NOT NULL,title varchar(160) NOT NULL,
 scheme_version int NOT NULL,revision int NOT NULL,status int NOT NULL DEFAULT 0,
 owner_name varchar(120),reference text,effective_from date,effective_to date,
 option_count int NOT NULL,source_hash char(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 snapshot_json mediumtext NOT NULL,captured_at datetime NOT NULL,
 reviewed_by bigint,reviewed_by_name varchar(120),reviewed_time datetime,evidence text,
 creator varchar(64) DEFAULT '',create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updater varchar(64) DEFAULT '',update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 deleted bit NOT NULL DEFAULT 0,tenant_id bigint NOT NULL,
 PRIMARY KEY(id),UNIQUE KEY uk_scheme_version(tenant_id,group_id,scheme_version),
 KEY idx_scheme_period(tenant_id,group_id,status,deleted,effective_from)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
INSERT INTO system_menu(name,permission,type,sort,parent_id,path,icon,component,component_name)
SELECT '薪酬方案配置版本','hrm:payroll:scheme:query',2,94,root.id,'payroll-schemes','ep:files','hrm/payroll/scheme/index','HrmPayrollScheme'
FROM system_menu root WHERE root.path='/hrm' AND root.parent_id=0 AND root.deleted=0
AND NOT EXISTS(SELECT 1 FROM system_menu WHERE component='hrm/payroll/scheme/index' AND deleted=0) LIMIT 1;
INSERT INTO system_menu(name,permission,type,sort,parent_id,path,visible)
SELECT '维护方案配置版本','hrm:payroll:scheme:maintain',3,1,p.id,'',0 FROM system_menu p
WHERE p.component='hrm/payroll/scheme/index' AND p.deleted=0
AND NOT EXISTS(SELECT 1 FROM system_menu WHERE permission='hrm:payroll:scheme:maintain' AND deleted=0) LIMIT 1;
INSERT INTO system_menu(name,permission,type,sort,parent_id,path,visible)
SELECT '评审方案配置版本','hrm:payroll:scheme:review',3,2,p.id,'',0 FROM system_menu p
WHERE p.component='hrm/payroll/scheme/index' AND p.deleted=0
AND NOT EXISTS(SELECT 1 FROM system_menu WHERE permission='hrm:payroll:scheme:review' AND deleted=0) LIMIT 1;
