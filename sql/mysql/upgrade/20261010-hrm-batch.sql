-- HRM payroll batch; additive, safe to reapply.
CREATE TABLE IF NOT EXISTS `hrm_payroll_run` (
 `id` bigint NOT NULL AUTO_INCREMENT, `month_record_id` bigint NOT NULL,
 `version` int NOT NULL, `input_snapshot` LONGTEXT NOT NULL, `rule_snapshot` LONGTEXT NOT NULL,
 `result_snapshot` LONGTEXT NOT NULL, `computed_by` bigint,
 `creator` varchar(64) DEFAULT '', `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `updater` varchar(64) DEFAULT '', `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `deleted` bit NOT NULL DEFAULT FALSE, `tenant_id` bigint NOT NULL DEFAULT 0, PRIMARY KEY (`id`), UNIQUE (`tenant_id`, `month_record_id`, `version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS `hrm_payroll_batch_event` (
 `id` bigint NOT NULL AUTO_INCREMENT, `month_record_id` bigint NOT NULL,
 `run_id` bigint NOT NULL, `action` varchar(32) NOT NULL, `from_status` int NOT NULL,
 `to_status` int NOT NULL, `actor_id` bigint NOT NULL, `reason` varchar(1000) NOT NULL,
 `creator` varchar(64) DEFAULT '', `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `updater` varchar(64) DEFAULT '', `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `deleted` bit NOT NULL DEFAULT FALSE, `tenant_id` bigint NOT NULL DEFAULT 0, PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


SET @hrm_payroll_menu = (SELECT id FROM system_menu WHERE path='/hrm-payroll' AND deleted=0 LIMIT 1);
INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name)
SELECT '核算审批', 'hrm:payroll:batch:query', 2, 20, @hrm_payroll_menu, 'batch', 'ep:checked', 'hrm/payroll/batch/index', 'HrmPayrollBatch'
WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE permission='hrm:payroll:batch:query' AND deleted=0);
SET @hrm_batch_menu = (SELECT id FROM system_menu WHERE permission='hrm:payroll:batch:query' AND deleted=0 LIMIT 1);
INSERT INTO system_menu (name, permission, type, parent_id) SELECT '提交复核', 'hrm:payroll:batch:submit', 3, @hrm_batch_menu WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE permission='hrm:payroll:batch:submit' AND deleted=0);
INSERT INTO system_menu (name, permission, type, parent_id) SELECT 'HR 复核', 'hrm:payroll:batch:review', 3, @hrm_batch_menu WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE permission='hrm:payroll:batch:review' AND deleted=0);
INSERT INTO system_menu (name, permission, type, parent_id) SELECT '财务审批', 'hrm:payroll:batch:approve', 3, @hrm_batch_menu WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE permission='hrm:payroll:batch:approve' AND deleted=0);
INSERT INTO system_menu (name, permission, type, parent_id) SELECT '退回核对', 'hrm:payroll:batch:reject', 3, @hrm_batch_menu WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE permission='hrm:payroll:batch:reject' AND deleted=0);
INSERT INTO system_menu (name, permission, type, parent_id) SELECT '冻结', 'hrm:payroll:batch:freeze', 3, @hrm_batch_menu WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE permission='hrm:payroll:batch:freeze' AND deleted=0);
INSERT INTO system_menu (name, permission, type, parent_id) SELECT '解冻', 'hrm:payroll:batch:unfreeze', 3, @hrm_batch_menu WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE permission='hrm:payroll:batch:unfreeze' AND deleted=0);
INSERT INTO system_menu (name, permission, type, parent_id) SELECT '归档', 'hrm:payroll:batch:archive', 3, @hrm_batch_menu WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE permission='hrm:payroll:batch:archive' AND deleted=0);
