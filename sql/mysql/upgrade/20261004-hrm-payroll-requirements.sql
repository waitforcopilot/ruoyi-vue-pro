-- Apply after the HRM/BPM baseline. Business catalogs are initialized per tenant
-- through the authorized API; original prototype states are never imported.

CREATE TABLE IF NOT EXISTS `hrm_payroll_requirement` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `code` varchar(64) NOT NULL,
    `module_code` varchar(32) NOT NULL,
    `title` varchar(500) NOT NULL,
    `description` text,
    `owner_name` varchar(120),
    `priority` int,
    `scope_decision` int NOT NULL DEFAULT 0,
    `scope_reason` text,
    `applicable_scope` varchar(500),
    `source_codes` text,
    `field_mapping` text,
    `acceptance_criteria` text,
    `remark` text,
    `built_in` bit NOT NULL DEFAULT 0,
    `origin` varchar(500),
    `status` int NOT NULL DEFAULT 0,
    `reviewed_by` bigint,
    `reviewed_by_name` varchar(120),
    `reviewed_time` datetime,
    `evidence` text,
    `version` int NOT NULL DEFAULT 1,
    `creator` varchar(64) DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted` bit NOT NULL DEFAULT 0,
    `tenant_id` bigint NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_tenant_deleted` (`tenant_id`, `deleted`),
    UNIQUE KEY `uk_tenant_code` (`tenant_id`, `code`),
    KEY `idx_module_status` (`tenant_id`, `module_code`, `status`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `hrm_payroll_source` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `code` varchar(64) NOT NULL,
    `name` varchar(200) NOT NULL,
    `description` text,
    `required_fields` text,
    `actual_system` varchar(200),
    `field_mapping` text,
    `owner_name` varchar(120),
    `readiness` int NOT NULL DEFAULT 0,
    `evidence` text,
    `built_in` bit NOT NULL DEFAULT 0,
    `confirmed_by` bigint,
    `confirmed_by_name` varchar(120),
    `confirmed_time` datetime,
    `version` int NOT NULL DEFAULT 1,
    `creator` varchar(64) DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted` bit NOT NULL DEFAULT 0,
    `tenant_id` bigint NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_tenant_deleted` (`tenant_id`, `deleted`),
    UNIQUE KEY `uk_tenant_code` (`tenant_id`, `code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `hrm_payroll_review` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `object_type` varchar(32) NOT NULL,
    `object_id` bigint NOT NULL,
    `action` varchar(32) NOT NULL,
    `from_version` int,
    `to_version` int,
    `actor_id` bigint,
    `actor_name` varchar(120),
    `reason` text,
    `before_snapshot` longtext,
    `after_snapshot` longtext,
    `creator` varchar(64) DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted` bit NOT NULL DEFAULT 0,
    `tenant_id` bigint NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_tenant_deleted` (`tenant_id`, `deleted`),
    KEY `idx_object` (`tenant_id`, `object_type`, `object_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `hrm_payroll_baseline` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `requirement_count` int NOT NULL,
    `source_count` int NOT NULL,
    `exported_by` bigint,
    `exported_by_name` varchar(120),
    `snapshot` longtext NOT NULL,
    `creator` varchar(64) DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted` bit NOT NULL DEFAULT 0,
    `tenant_id` bigint NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_tenant_deleted` (`tenant_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Menu seeds use allocated IDs and preserve existing role grants.
INSERT INTO system_menu (name, type, sort, parent_id, path, icon)
SELECT 'HRM 人力资源', 1, 350, 0, '/hrm', 'ep:office-building'
WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE path = '/hrm' AND parent_id = 0 AND deleted = 0);

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name)
SELECT '薪酬需求征集', 'hrm:payroll:requirements:query', 2, 90, root.id, 'payroll-requirements', 'ep:document-checked', 'hrm/payroll/requirements/index', 'HrmPayrollRequirements'
FROM system_menu root WHERE root.path = '/hrm' AND root.parent_id = 0 AND root.deleted = 0
AND NOT EXISTS (SELECT 1 FROM system_menu WHERE component = 'hrm/payroll/requirements/index' AND deleted = 0) LIMIT 1;

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, visible)
SELECT '登记与补充需求', 'hrm:payroll:requirements:create', 3, 1, page.id, '', 0 FROM system_menu page
WHERE page.component = 'hrm/payroll/requirements/index' AND page.deleted = 0
AND NOT EXISTS (SELECT 1 FROM system_menu WHERE permission = 'hrm:payroll:requirements:create' AND deleted = 0) LIMIT 1;

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, visible)
SELECT '维护需求', 'hrm:payroll:requirements:update', 3, 2, page.id, '', 0 FROM system_menu page
WHERE page.component = 'hrm/payroll/requirements/index' AND page.deleted = 0
AND NOT EXISTS (SELECT 1 FROM system_menu WHERE permission = 'hrm:payroll:requirements:update' AND deleted = 0) LIMIT 1;

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, visible)
SELECT '删除补充需求', 'hrm:payroll:requirements:delete', 3, 3, page.id, '', 0 FROM system_menu page
WHERE page.component = 'hrm/payroll/requirements/index' AND page.deleted = 0
AND NOT EXISTS (SELECT 1 FROM system_menu WHERE permission = 'hrm:payroll:requirements:delete' AND deleted = 0) LIMIT 1;

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, visible)
SELECT '评审需求', 'hrm:payroll:requirements:review', 3, 4, page.id, '', 0 FROM system_menu page
WHERE page.component = 'hrm/payroll/requirements/index' AND page.deleted = 0
AND NOT EXISTS (SELECT 1 FROM system_menu WHERE permission = 'hrm:payroll:requirements:review' AND deleted = 0) LIMIT 1;

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, visible)
SELECT '生成及导出基线', 'hrm:payroll:requirements:export', 3, 5, page.id, '', 0 FROM system_menu page
WHERE page.component = 'hrm/payroll/requirements/index' AND page.deleted = 0
AND NOT EXISTS (SELECT 1 FROM system_menu WHERE permission = 'hrm:payroll:requirements:export' AND deleted = 0) LIMIT 1;

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, visible)
SELECT '维护数据来源', 'hrm:payroll:source:update', 3, 6, page.id, '', 0 FROM system_menu page
WHERE page.component = 'hrm/payroll/requirements/index' AND page.deleted = 0
AND NOT EXISTS (SELECT 1 FROM system_menu WHERE permission = 'hrm:payroll:source:update' AND deleted = 0) LIMIT 1;
