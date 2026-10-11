-- Versioned legal entities, time-bound authorization and employee affiliation.
CREATE TABLE IF NOT EXISTS `hrm_payroll_legal_entity` (
 `id` bigint NOT NULL AUTO_INCREMENT,
 `code` varchar(64),
 `name` varchar(1000),
 `tax_registration_number` varchar(32),
 `evidence` varchar(1000),
 `status` varchar(32),
 `approved_by` bigint,
 `submitted_by` bigint,
 `creator` varchar(64) DEFAULT '',
 `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `updater` varchar(64) DEFAULT '',
 `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `deleted` bit NOT NULL DEFAULT FALSE,
 `tenant_id` bigint NOT NULL DEFAULT 0,
 PRIMARY KEY (`id`),
 UNIQUE (`tenant_id`,`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `hrm_payroll_entity_grant` (
 `id` bigint NOT NULL AUTO_INCREMENT,
 `entity_id` bigint,
 `user_id` bigint,
 `actions` LONGTEXT,
 `effective_from` date,
 `effective_to` date,
 `reason` varchar(1000),
 `creator` varchar(64) DEFAULT '',
 `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `updater` varchar(64) DEFAULT '',
 `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `deleted` bit NOT NULL DEFAULT FALSE,
 `tenant_id` bigint NOT NULL DEFAULT 0,
 PRIMARY KEY (`id`),
 KEY `idx_entity_grant_user` (`tenant_id`,`entity_id`,`user_id`,`effective_from`,`effective_to`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `hrm_payroll_entity_membership` (
 `id` bigint NOT NULL AUTO_INCREMENT,
 `entity_id` bigint,
 `replaces_id` bigint,
 `employee_id` bigint,
 `dept_id` bigint,
 `employee_category` varchar(1000),
 `work_city` varchar(1000),
 `effective_from` date,
 `effective_to` date,
 `evidence` varchar(1000),
 `status` varchar(32),
 `approved_by` bigint,
 `creator` varchar(64) DEFAULT '',
 `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `updater` varchar(64) DEFAULT '',
 `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `deleted` bit NOT NULL DEFAULT FALSE,
 `tenant_id` bigint NOT NULL DEFAULT 0,
 PRIMARY KEY (`id`),
 KEY `idx_membership_employee_period` (`tenant_id`,`employee_id`,`effective_from`,`effective_to`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `hrm_payroll_entity_event` (
 `id` bigint NOT NULL AUTO_INCREMENT,
 `entity_id` bigint,
 `action` varchar(1000),
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
INSERT INTO system_menu(name,permission,type,parent_id,path,component,component_name) SELECT '法人主体与归属','hrm:payroll:entity:query',2,@hrm_payroll_menu,'entity','hrm/payroll/entity/index','HrmPayrollEntity' WHERE NOT EXISTS(SELECT 1 FROM system_menu WHERE permission='hrm:payroll:entity:query' AND deleted=0);
SET @hrm_entity_menu=(SELECT id FROM system_menu WHERE permission='hrm:payroll:entity:query' AND deleted=0 LIMIT 1);
INSERT INTO system_menu(name,permission,type,parent_id) SELECT '维护主体与人员归属','hrm:payroll:entity:update',3,@hrm_entity_menu WHERE NOT EXISTS(SELECT 1 FROM system_menu WHERE permission='hrm:payroll:entity:update' AND deleted=0);
INSERT INTO system_menu(name,permission,type,parent_id) SELECT '审批主体与人员归属','hrm:payroll:entity:approve',3,@hrm_entity_menu WHERE NOT EXISTS(SELECT 1 FROM system_menu WHERE permission='hrm:payroll:entity:approve' AND deleted=0);
INSERT INTO system_menu(name,permission,type,parent_id) SELECT '管理主体授权','hrm:payroll:entity:grant',3,@hrm_entity_menu WHERE NOT EXISTS(SELECT 1 FROM system_menu WHERE permission='hrm:payroll:entity:grant' AND deleted=0);
