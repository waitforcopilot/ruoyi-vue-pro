-- Requires the HRM baseline and payroll requirements migration. No policy values or role grants are seeded.
CREATE TABLE IF NOT EXISTS `hrm_payroll_rule` (
 `id` bigint NOT NULL AUTO_INCREMENT,
 `code` varchar(64) NOT NULL,
 `title` varchar(200) NOT NULL,
 `category` varchar(32) NOT NULL,
 `question_code` varchar(8),
 `rule_version` int NOT NULL,
 `revision` int NOT NULL,
 `status` int NOT NULL DEFAULT 0,
 `built_in` bit NOT NULL DEFAULT 0,
 `owner_name` varchar(120),
 `scope_code` varchar(64),
 `applicable_scope` varchar(500),
 `effective_from` date,
 `effective_to` date,
 `definition` text,
 `reference` text,
 `parameters_json` mediumtext NOT NULL,
 `cases_json` mediumtext NOT NULL,
 `parameter_count` int NOT NULL,
 `case_count` int NOT NULL,
 `reviewed_by` bigint,
 `reviewed_by_name` varchar(120),
 `reviewed_time` datetime,
 `evidence` text,
 `creator` varchar(64) DEFAULT '',
 `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `updater` varchar(64) DEFAULT '',
 `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `deleted` bit NOT NULL DEFAULT 0,
 `tenant_id` bigint NOT NULL,
 PRIMARY KEY (`id`),
 UNIQUE KEY `uk_rule_version` (`tenant_id`, `code`, `rule_version`),
 KEY `idx_rule_scope_status` (`tenant_id`, `code`, `scope_code`, `status`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO system_menu(name,permission,type,sort,parent_id,path,icon,component,component_name)
SELECT '薪酬规则台账','hrm:payroll:rule:query',2,92,root.id,'payroll-rules','ep:notebook','hrm/payroll/rules/index','HrmPayrollRules'
FROM system_menu root WHERE root.path='/hrm' AND root.parent_id=0 AND root.deleted=0
AND NOT EXISTS(SELECT 1 FROM system_menu WHERE component='hrm/payroll/rules/index' AND deleted=0) LIMIT 1;

INSERT INTO system_menu(name,permission,type,sort,parent_id,path,visible)
SELECT '登记与维护规则','hrm:payroll:rule:maintain',3,1,page.id,'',0 FROM system_menu page
WHERE page.component='hrm/payroll/rules/index' AND page.deleted=0
AND NOT EXISTS(SELECT 1 FROM system_menu WHERE permission='hrm:payroll:rule:maintain' AND deleted=0) LIMIT 1;

INSERT INTO system_menu(name,permission,type,sort,parent_id,path,visible)
SELECT '确认与停用规则','hrm:payroll:rule:review',3,2,page.id,'',0 FROM system_menu page
WHERE page.component='hrm/payroll/rules/index' AND page.deleted=0
AND NOT EXISTS(SELECT 1 FROM system_menu WHERE permission='hrm:payroll:rule:review' AND deleted=0) LIMIT 1;
