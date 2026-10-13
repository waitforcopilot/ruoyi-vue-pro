-- Versioned annual resident withholding policy; templates require formal source and approval.
CREATE TABLE IF NOT EXISTS `hrm_payroll_tax_policy` (
 `id` bigint NOT NULL AUTO_INCREMENT, `entity_id` bigint, `year` int, `version` int, `prior_policy_id` bigint,
 `request_key` varchar(100), `request_snapshot` LONGTEXT,
 `tax_registration_number` varchar(100), `name` varchar(100), `monthly_basic_deduction` decimal(18,2),
 `resident_bands` LONGTEXT, `bonus_bands` LONGTEXT, `separate_bonus_allowed` bit,
 `separate_bonus_valid_to` date, `official_reference` varchar(1000), `evidence` varchar(1000),
 `status` varchar(32), `submitted_by` bigint, `approved_by` bigint, `review_opinion` varchar(1000), `approved_time` datetime,
 `creator` varchar(64) DEFAULT '', `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `updater` varchar(64) DEFAULT '', `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `deleted` bit NOT NULL DEFAULT FALSE, `tenant_id` bigint NOT NULL DEFAULT 0,
 `active_year_key` varchar(128) GENERATED ALWAYS AS (CASE WHEN `deleted`=b'0' AND `status`='APPROVED' THEN CONCAT(`entity_id`,':',`year`) ELSE NULL END) STORED,
 PRIMARY KEY (`id`), UNIQUE KEY `uk_tax_policy_request` (`tenant_id`,`request_key`), UNIQUE KEY `uk_tax_policy_version` (`tenant_id`,`entity_id`,`year`,`version`),
 UNIQUE KEY `uk_tax_policy_active` (`tenant_id`,`active_year_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS `hrm_payroll_tax_policy_event` (
 `id` bigint NOT NULL AUTO_INCREMENT, `policy_id` bigint, `action` varchar(32), `actor_id` bigint, `reason` varchar(1000), `snapshot` LONGTEXT,
 `creator` varchar(64) DEFAULT '', `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `updater` varchar(64) DEFAULT '', `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `deleted` bit NOT NULL DEFAULT FALSE, `tenant_id` bigint NOT NULL DEFAULT 0,
 PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
SET @hrm_alter=IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='hrm_payroll_tax_policy' AND COLUMN_NAME='request_key')=0, 'ALTER TABLE `hrm_payroll_tax_policy` ADD COLUMN `request_key` varchar(100) NULL', 'SELECT 1');
PREPARE hrm_stmt FROM @hrm_alter; EXECUTE hrm_stmt; DEALLOCATE PREPARE hrm_stmt;
SET @hrm_alter=IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='hrm_payroll_tax_policy' AND COLUMN_NAME='request_snapshot')=0, 'ALTER TABLE `hrm_payroll_tax_policy` ADD COLUMN `request_snapshot` LONGTEXT NULL', 'SELECT 1');
PREPARE hrm_stmt FROM @hrm_alter; EXECUTE hrm_stmt; DEALLOCATE PREPARE hrm_stmt;
SET @hrm_alter=IF((SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='hrm_payroll_tax_policy' AND INDEX_NAME='uk_tax_policy_request')=0, 'ALTER TABLE `hrm_payroll_tax_policy` ADD UNIQUE KEY `uk_tax_policy_request` (`tenant_id`,`request_key`)', 'SELECT 1');
PREPARE hrm_stmt FROM @hrm_alter; EXECUTE hrm_stmt; DEALLOCATE PREPARE hrm_stmt;
SET @hrm_salary_parent=(SELECT parent_id FROM system_menu WHERE permission='hrm:payroll:requirement:query' AND deleted=0 LIMIT 1);
INSERT INTO system_menu(name,permission,type,parent_id,path,icon,component,component_name)
SELECT '年度税务政策','hrm:payroll:tax:query',2,@hrm_salary_parent,'tax-policy','ep:coin','hrm/payroll/tax-policy/index','HrmPayrollTaxPolicy'
WHERE NOT EXISTS(SELECT 1 FROM system_menu WHERE permission='hrm:payroll:tax:query' AND deleted=0);
SET @hrm_tax_menu=(SELECT id FROM system_menu WHERE permission='hrm:payroll:tax:query' AND deleted=0 LIMIT 1);
INSERT INTO system_menu(name,permission,type,parent_id) SELECT '维护税务政策','hrm:payroll:tax:update',3,@hrm_tax_menu WHERE NOT EXISTS(SELECT 1 FROM system_menu WHERE permission='hrm:payroll:tax:update' AND deleted=0);
INSERT INTO system_menu(name,permission,type,parent_id) SELECT '审批税务政策','hrm:payroll:tax:approve',3,@hrm_tax_menu WHERE NOT EXISTS(SELECT 1 FROM system_menu WHERE permission='hrm:payroll:tax:approve' AND deleted=0);
