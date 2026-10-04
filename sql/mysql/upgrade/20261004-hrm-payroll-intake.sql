-- Requires 20261004-hrm-payroll-requirements.sql. No business schemas or role grants are seeded.
CREATE TABLE IF NOT EXISTS `hrm_payroll_source_contract` (
 `id` bigint NOT NULL AUTO_INCREMENT,
 `source_id` bigint NOT NULL,
 `source_code` varchar(64) NOT NULL,
 `source_name` varchar(200) NOT NULL,
 `contract_version` int NOT NULL,
 `revision` int NOT NULL DEFAULT 1,
 `status` int NOT NULL DEFAULT 0,
 `title` varchar(200) NOT NULL,
 `actual_system` varchar(200),
 `owner_name` varchar(120),
 `applicable_scope` varchar(500),
 `evidence` text,
 `schema_json` mediumtext NOT NULL,
 `field_count` int NOT NULL,
 `reviewed_by` bigint,
 `reviewed_by_name` varchar(120),
 `reviewed_time` datetime,
 `creator` varchar(64) DEFAULT '',
 `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `updater` varchar(64) DEFAULT '',
 `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `deleted` bit NOT NULL DEFAULT 0,
 `tenant_id` bigint NOT NULL,
 PRIMARY KEY (`id`),
 UNIQUE KEY `uk_source_contract_version` (`tenant_id`, `source_id`, `contract_version`),
 KEY `idx_contract_tenant_status` (`tenant_id`, `status`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `hrm_payroll_import_batch` (
 `id` bigint NOT NULL AUTO_INCREMENT,
 `contract_id` bigint NOT NULL,
 `source_code` varchar(64) NOT NULL,
 `contract_version` int NOT NULL,
 `file_name` varchar(200) NOT NULL,
 `file_hash` char(64) NOT NULL,
 `idempotency_key` char(64) NOT NULL,
 `declared_scope` varchar(120) NOT NULL,
 `period_start` date NOT NULL,
 `period_end` date NOT NULL,
 `row_count` int NOT NULL,
 `valid_count` int NOT NULL,
 `error_count` int NOT NULL,
 `status` int NOT NULL,
 `created_by` bigint NOT NULL,
 `created_by_name` varchar(120),
 `snapshot` longtext NOT NULL,
 `creator` varchar(64) DEFAULT '',
 `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `updater` varchar(64) DEFAULT '',
 `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `deleted` bit NOT NULL DEFAULT 0,
 `tenant_id` bigint NOT NULL,
 PRIMARY KEY (`id`),
 UNIQUE KEY `uk_batch_idempotency` (`tenant_id`, `created_by`, `idempotency_key`),
 KEY `idx_batch_owner` (`tenant_id`, `created_by`, `deleted`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name)
SELECT '薪酬数据接入', 'hrm:payroll:intake:query', 2, 91, root.id, 'payroll-intake', 'ep:upload-filled', 'hrm/payroll/intake/index', 'HrmPayrollIntake'
FROM system_menu root WHERE root.path='/hrm' AND root.parent_id=0 AND root.deleted=0
AND NOT EXISTS (SELECT 1 FROM system_menu WHERE component='hrm/payroll/intake/index' AND deleted=0) LIMIT 1;

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, visible)
SELECT '维护字段契约', 'hrm:payroll:intake:contract', 3, 1, page.id, '', 0 FROM system_menu page
WHERE page.component='hrm/payroll/intake/index' AND page.deleted=0
AND NOT EXISTS (SELECT 1 FROM system_menu WHERE permission='hrm:payroll:intake:contract' AND deleted=0) LIMIT 1;

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, visible)
SELECT '确认与停用契约', 'hrm:payroll:intake:confirm', 3, 2, page.id, '', 0 FROM system_menu page
WHERE page.component='hrm/payroll/intake/index' AND page.deleted=0
AND NOT EXISTS (SELECT 1 FROM system_menu WHERE permission='hrm:payroll:intake:confirm' AND deleted=0) LIMIT 1;

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, visible)
SELECT '上传并预检', 'hrm:payroll:intake:preview', 3, 3, page.id, '', 0 FROM system_menu page
WHERE page.component='hrm/payroll/intake/index' AND page.deleted=0
AND NOT EXISTS (SELECT 1 FROM system_menu WHERE permission='hrm:payroll:intake:preview' AND deleted=0) LIMIT 1;
