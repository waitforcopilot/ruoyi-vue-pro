-- HRM payroll requirements; additive, safe to reapply.
CREATE TABLE IF NOT EXISTS `hrm_payroll_requirement` (
 `id` bigint NOT NULL AUTO_INCREMENT,
 `code` varchar(64) NOT NULL, `module` varchar(32) NOT NULL, `description` varchar(1000) NOT NULL,
 `field_mapping` TEXT, `source_system` varchar(1000), `source_owner_id` bigint, `reviewer_id` bigint,
 `priority` int NOT NULL, `status` varchar(32) NOT NULL, `readiness` varchar(32) NOT NULL,
 `acceptance` TEXT, `evidence` TEXT, `version` int NOT NULL,
 `confirmed_by` bigint, `confirmed_at` datetime,
 `creator` varchar(64) DEFAULT '', `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `updater` varchar(64) DEFAULT '', `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `deleted` bit NOT NULL DEFAULT FALSE, `tenant_id` bigint NOT NULL DEFAULT 0,
 PRIMARY KEY (`id`), UNIQUE (`tenant_id`, `code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS `hrm_payroll_requirement_history` (
 `id` bigint NOT NULL AUTO_INCREMENT, `requirement_id` bigint NOT NULL,
 `version` int NOT NULL, `snapshot` TEXT NOT NULL, `actor_id` bigint NOT NULL,
 `creator` varchar(64) DEFAULT '', `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `updater` varchar(64) DEFAULT '', `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `deleted` bit NOT NULL DEFAULT FALSE, `tenant_id` bigint NOT NULL DEFAULT 0,
 PRIMARY KEY (`id`), UNIQUE (`tenant_id`, `requirement_id`, `version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- Uses generated IDs; reruns do not create duplicate routes or permissions.
INSERT INTO system_menu (name, type, sort, parent_id, path, icon, component, component_name)
SELECT '薪酬管理', 1, 55, 0, '/hrm-payroll', 'ep:money', NULL, NULL
WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE path='/hrm-payroll' AND deleted=0);
SET @hrm_payroll_menu = (SELECT id FROM system_menu WHERE path='/hrm-payroll' AND deleted=0 LIMIT 1);
INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name)
SELECT '需求评审', 'hrm:payroll:requirement:query', 2, 10, @hrm_payroll_menu, 'requirement', 'ep:list', 'hrm/payroll/requirement/index', 'HrmPayrollRequirement'
WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE permission='hrm:payroll:requirement:query' AND deleted=0);
SET @hrm_requirement_menu = (SELECT id FROM system_menu WHERE permission='hrm:payroll:requirement:query' AND deleted=0 LIMIT 1);
INSERT INTO system_menu (name, permission, type, parent_id) SELECT '维护需求', 'hrm:payroll:requirement:update', 3, @hrm_requirement_menu WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE permission='hrm:payroll:requirement:update' AND deleted=0);
INSERT INTO system_menu (name, permission, type, parent_id) SELECT '导出评审', 'hrm:payroll:requirement:export', 3, @hrm_requirement_menu WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE permission='hrm:payroll:requirement:export' AND deleted=0);
