SET NAMES utf8mb4;
-- Required payroll input confirmations and immutable import previews.
CREATE TABLE IF NOT EXISTS `hrm_payroll_input` (
 `id` bigint NOT NULL AUTO_INCREMENT,
 `batch_id` bigint,
 `entity_id` bigint,
 `employee_id` bigint,
 `reviewed_by` bigint,
 `version` int,
 `kind` varchar(64),
 `mode` varchar(64),
 `evidence` varchar(1000),
 `payload` LONGTEXT,
 `source_snapshot` LONGTEXT,
 `status` varchar(64),
 `creator` varchar(64) DEFAULT '', `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `updater` varchar(64) DEFAULT '', `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `deleted` bit NOT NULL DEFAULT FALSE, `tenant_id` bigint NOT NULL DEFAULT 0,
 PRIMARY KEY (`id`),
 UNIQUE KEY `uk_payroll_input_version` (`tenant_id`,`batch_id`,`employee_id`,`kind`,`version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS `hrm_payroll_input_import` (
 `id` bigint NOT NULL AUTO_INCREMENT,
 `batch_id` bigint,
 `entity_id` bigint,
 `version` int,
 `file_name` varchar(255),
 `digest` varchar(64),
 `rows_snapshot` LONGTEXT,
 `errors` LONGTEXT,
 `requests` LONGTEXT,
 `input_ids` LONGTEXT,
 `status` varchar(64),
 `creator` varchar(64) DEFAULT '', `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `updater` varchar(64) DEFAULT '', `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `deleted` bit NOT NULL DEFAULT FALSE, `tenant_id` bigint NOT NULL DEFAULT 0,
 PRIMARY KEY (`id`),
 UNIQUE KEY `uk_payroll_input_import_version` (`tenant_id`,`batch_id`,`digest`,`version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
SET @hrm_payroll_menu=(SELECT id FROM system_menu WHERE path='/hrm-payroll' AND deleted=0 LIMIT 1);
INSERT INTO system_menu(name,permission,type,parent_id,path,component,component_name) SELECT '核算输入与就绪','hrm:payroll:input:query',2,@hrm_payroll_menu,'input','hrm/payroll/input/index','HrmPayrollInput' WHERE NOT EXISTS(SELECT 1 FROM system_menu WHERE permission='hrm:payroll:input:query' AND deleted=0);
SET @hrm_input_menu=(SELECT id FROM system_menu WHERE permission='hrm:payroll:input:query' AND deleted=0 LIMIT 1);
INSERT INTO system_menu(name,permission,type,parent_id) SELECT '准备及提交核算输入','hrm:payroll:input:update',3,@hrm_input_menu WHERE NOT EXISTS(SELECT 1 FROM system_menu WHERE permission='hrm:payroll:input:update' AND deleted=0);
INSERT INTO system_menu(name,permission,type,parent_id) SELECT '复核核算输入','hrm:payroll:input:review',3,@hrm_input_menu WHERE NOT EXISTS(SELECT 1 FROM system_menu WHERE permission='hrm:payroll:input:review' AND deleted=0);
INSERT INTO system_menu(name,permission,type,parent_id) SELECT '导出核算输入','hrm:payroll:input:export',3,@hrm_input_menu WHERE NOT EXISTS(SELECT 1 FROM system_menu WHERE permission='hrm:payroll:input:export' AND deleted=0);
