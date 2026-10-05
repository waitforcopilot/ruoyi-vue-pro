-- Local policy records only; no default policy values, legacy configuration writes or role grants.
CREATE TABLE IF NOT EXISTS hrm_payroll_insurance_policy (
 id bigint NOT NULL AUTO_INCREMENT,
 identity_key char(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 city_area_id int NOT NULL,city_name varchar(64) NOT NULL,
 scope_code varchar(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,scope_name varchar(120),
 project_type int NOT NULL,project_code varchar(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,project_name varchar(64) NOT NULL,
 title varchar(160) NOT NULL,policy_version int NOT NULL,revision int NOT NULL,status int NOT NULL DEFAULT 0,
 owner_name varchar(120),reference text,source_url varchar(2048),effective_from date,effective_to date,
 config_schema_version int NOT NULL,config_json text NOT NULL,
 reviewed_by bigint,reviewed_by_name varchar(120),reviewed_time datetime,evidence text,
 creator varchar(64) DEFAULT '',create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updater varchar(64) DEFAULT '',update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 deleted bit NOT NULL DEFAULT 0,tenant_id bigint NOT NULL,
 PRIMARY KEY(id),UNIQUE KEY uk_insurance_policy_version(tenant_id,identity_key,policy_version),
 KEY idx_insurance_policy_scope(tenant_id,city_area_id,scope_code,project_type,status),
 KEY idx_insurance_policy_period(tenant_id,identity_key,status,effective_from,effective_to)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
INSERT INTO system_menu(name,permission,type,sort,parent_id,path,icon,component,component_name)
SELECT '社保公积金政策版本','hrm:payroll:insurance-policy:query',2,98,m.id,'payroll-insurance-policies','ep:document-checked','hrm/payroll/insurance/index','HrmPayrollInsurancePolicy'
FROM system_menu m WHERE m.path='/hrm' AND m.deleted=0
AND NOT EXISTS(SELECT 1 FROM system_menu WHERE permission='hrm:payroll:insurance-policy:query' AND deleted=0) LIMIT 1;
INSERT INTO system_menu(name,permission,type,sort,parent_id,path,visible)
SELECT '维护本地缴费政策','hrm:payroll:insurance-policy:maintain',3,1,m.id,'',0
FROM system_menu m WHERE m.component='hrm/payroll/insurance/index' AND m.deleted=0
AND NOT EXISTS(SELECT 1 FROM system_menu WHERE permission='hrm:payroll:insurance-policy:maintain' AND deleted=0) LIMIT 1;
INSERT INTO system_menu(name,permission,type,sort,parent_id,path,visible)
SELECT '评审本地缴费政策','hrm:payroll:insurance-policy:review',3,2,m.id,'',0
FROM system_menu m WHERE m.component='hrm/payroll/insurance/index' AND m.deleted=0
AND NOT EXISTS(SELECT 1 FROM system_menu WHERE permission='hrm:payroll:insurance-policy:review' AND deleted=0) LIMIT 1;
