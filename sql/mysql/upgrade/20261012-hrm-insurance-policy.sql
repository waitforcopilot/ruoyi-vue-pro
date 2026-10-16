SET NAMES utf8mb4;
-- Approved insurance policies and annual declaration history. Repeatable upgrade.
CREATE TABLE IF NOT EXISTS `hrm_payroll_insurance_policy` (
 `id` bigint NOT NULL AUTO_INCREMENT,
 `entity_id` bigint,
 `code` varchar(64),
 `city` varchar(128),
 `version` int,
 `effective_from` date,
 `effective_to` date,
 `replaces_id` bigint,
 `evidence` varchar(1000),
 `projects` LONGTEXT,
 `status` varchar(32),
 `approved_by` bigint,
 `creator` varchar(64) DEFAULT '',
 `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `updater` varchar(64) DEFAULT '',
 `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `deleted` bit NOT NULL DEFAULT FALSE,
 `tenant_id` bigint NOT NULL DEFAULT 0,
 PRIMARY KEY (`id`),
 UNIQUE KEY `uk_insurance_policy_version` (`tenant_id`,`entity_id`,`code`,`version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `hrm_payroll_insurance_base` (
 `id` bigint NOT NULL AUTO_INCREMENT,
 `entity_id` bigint,
 `employee_id` bigint,
 `policy_id` bigint,
 `coverage` varchar(32),
 `income_year` int,
 `effective_from` date,
 `effective_to` date,
 `replaces_id` bigint,
 `declared_bases` LONGTEXT,
 `evidence` varchar(1000),
 `status` varchar(32),
 `reviewed_by` bigint,
 `approved_by` bigint,
 `creator` varchar(64) DEFAULT '',
 `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `updater` varchar(64) DEFAULT '',
 `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `deleted` bit NOT NULL DEFAULT FALSE,
 `tenant_id` bigint NOT NULL DEFAULT 0,
 PRIMARY KEY (`id`),
 KEY `idx_insurance_base_employee_month` (`tenant_id`,`employee_id`,`effective_from`,`effective_to`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `hrm_payroll_insurance_event` (
 `id` bigint NOT NULL AUTO_INCREMENT,
 `entity_id` bigint,
 `policy_id` bigint,
 `base_id` bigint,
 `action` varchar(32),
 `actor_id` bigint,
 `reason` varchar(1000),
 `snapshot` LONGTEXT,
 `creator` varchar(64) DEFAULT '',
 `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `updater` varchar(64) DEFAULT '',
 `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `deleted` bit NOT NULL DEFAULT FALSE,
 `tenant_id` bigint NOT NULL DEFAULT 0,
 PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
SET @hrm_payroll_menu=(SELECT id FROM system_menu WHERE path='/hrm-payroll' AND deleted=0 LIMIT 1);
INSERT INTO system_menu(name,permission,type,parent_id,path,component,component_name) SELECT '缴费政策与年度调基','hrm:payroll:insurance:query',2,@hrm_payroll_menu,'insurance-policy','hrm/payroll/insurance-policy/index','HrmPayrollInsurancePolicy' WHERE NOT EXISTS(SELECT 1 FROM system_menu WHERE permission='hrm:payroll:insurance:query' AND deleted=0);
SET @hrm_insurance_menu=(SELECT id FROM system_menu WHERE permission='hrm:payroll:insurance:query' AND deleted=0 LIMIT 1);
INSERT INTO system_menu(name,permission,type,parent_id) SELECT '维护政策与调基','hrm:payroll:insurance:update',3,@hrm_insurance_menu WHERE NOT EXISTS(SELECT 1 FROM system_menu WHERE permission='hrm:payroll:insurance:update' AND deleted=0);
INSERT INTO system_menu(name,permission,type,parent_id) SELECT '复核年度调基','hrm:payroll:insurance:review',3,@hrm_insurance_menu WHERE NOT EXISTS(SELECT 1 FROM system_menu WHERE permission='hrm:payroll:insurance:review' AND deleted=0);
INSERT INTO system_menu(name,permission,type,parent_id) SELECT '审批缴费政策与调基','hrm:payroll:insurance:approve',3,@hrm_insurance_menu WHERE NOT EXISTS(SELECT 1 FROM system_menu WHERE permission='hrm:payroll:insurance:approve' AND deleted=0);
INSERT INTO system_menu(name,permission,type,parent_id) SELECT '导出政策与调基','hrm:payroll:insurance:export',3,@hrm_insurance_menu WHERE NOT EXISTS(SELECT 1 FROM system_menu WHERE permission='hrm:payroll:insurance:export' AND deleted=0);
CREATE TABLE IF NOT EXISTS `hrm_payroll_insurance_import` (
 `id` bigint NOT NULL AUTO_INCREMENT,
 `entity_id` bigint,
 `file_name` varchar(255),
 `digest` varchar(64),
 `rows_snapshot` LONGTEXT,
 `errors` LONGTEXT,
 `requests` LONGTEXT,
 `proposal_ids` LONGTEXT,
 `status` varchar(32),
 `creator` varchar(64) DEFAULT '',
 `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `updater` varchar(64) DEFAULT '',
 `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `deleted` bit NOT NULL DEFAULT FALSE,
 `tenant_id` bigint NOT NULL DEFAULT 0,
 PRIMARY KEY (`id`),
 UNIQUE KEY `uk_insurance_import_content` (`tenant_id`,`entity_id`,`digest`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
