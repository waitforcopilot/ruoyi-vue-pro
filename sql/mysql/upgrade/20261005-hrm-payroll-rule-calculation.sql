-- Explicit expression definitions only. No inferred wage rules, role grants or legacy data writes.
CREATE TABLE IF NOT EXISTS hrm_payroll_calculation_definition (
 id bigint NOT NULL AUTO_INCREMENT,
 code varchar(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,title varchar(160) NOT NULL,
 definition_version int NOT NULL,schema_version int NOT NULL,revision int NOT NULL,status int NOT NULL DEFAULT 0,
 owner_name varchar(120),scope_code varchar(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,applicable_scope varchar(500),
 description text,reference text,effective_from date,effective_to date,
 program_json mediumtext NOT NULL,input_count int NOT NULL,item_count int NOT NULL,case_count int NOT NULL,
 verification_json mediumtext,reviewed_by bigint,reviewed_by_name varchar(120),reviewed_time datetime,evidence text,
 creator varchar(64) DEFAULT '',create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updater varchar(64) DEFAULT '',update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 deleted bit NOT NULL DEFAULT 0,tenant_id bigint NOT NULL,
 PRIMARY KEY(id),UNIQUE KEY uk_calculation_version(tenant_id,code,definition_version),
 KEY idx_calculation_period(tenant_id,code,scope_code,status,effective_from,effective_to)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
INSERT INTO system_menu(name,permission,type,sort,parent_id,path,icon,component,component_name)
SELECT '工资规则表达式与试算','hrm:payroll:calculation:query',2,99,m.id,'payroll-calculation','ep:cpu','hrm/payroll/calculation/index','HrmPayrollCalculation'
FROM system_menu m WHERE m.path='/hrm' AND m.deleted=0
AND NOT EXISTS(SELECT 1 FROM system_menu WHERE permission='hrm:payroll:calculation:query' AND deleted=0) LIMIT 1;
INSERT INTO system_menu(name,permission,type,sort,parent_id,path,visible)
SELECT '维护计算规则','hrm:payroll:calculation:maintain',3,1,m.id,'',0
FROM system_menu m WHERE m.component='hrm/payroll/calculation/index' AND m.deleted=0
AND NOT EXISTS(SELECT 1 FROM system_menu WHERE permission='hrm:payroll:calculation:maintain' AND deleted=0) LIMIT 1;
INSERT INTO system_menu(name,permission,type,sort,parent_id,path,visible)
SELECT '评审计算规则','hrm:payroll:calculation:review',3,2,m.id,'',0
FROM system_menu m WHERE m.component='hrm/payroll/calculation/index' AND m.deleted=0
AND NOT EXISTS(SELECT 1 FROM system_menu WHERE permission='hrm:payroll:calculation:review' AND deleted=0) LIMIT 1;
