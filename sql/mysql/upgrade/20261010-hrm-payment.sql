-- HRM payroll payment; additive, safe to reapply.
CREATE TABLE IF NOT EXISTS `hrm_payroll_payment` (
 `id` bigint NOT NULL AUTO_INCREMENT, `month_record_id` bigint NOT NULL,
 `run_id` bigint NOT NULL, `employee_id` bigint NOT NULL, `employee_name` varchar(255) NOT NULL,
 `bank_account` varchar(128) NOT NULL, `bank_name` varchar(255), `amount` decimal(18,2) NOT NULL,
 `status` varchar(32) NOT NULL, `failure_reason` varchar(1000), `attempt` int NOT NULL,
 `creator` varchar(64) DEFAULT '', `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `updater` varchar(64) DEFAULT '', `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `deleted` bit NOT NULL DEFAULT FALSE, `tenant_id` bigint NOT NULL DEFAULT 0,
 PRIMARY KEY (`id`), UNIQUE (`tenant_id`, `month_record_id`, `employee_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


CREATE TABLE IF NOT EXISTS `hrm_payroll_bank_template` (
 `id` bigint NOT NULL AUTO_INCREMENT,
 `name` varchar(100) NOT NULL, `columns` varchar(4000) NOT NULL,
 `return_columns` varchar(4000) NOT NULL, `success_value` varchar(30) NOT NULL, `failed_value` varchar(30) NOT NULL,
 `creator` varchar(64) DEFAULT '', `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `updater` varchar(64) DEFAULT '', `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `deleted` bit NOT NULL DEFAULT FALSE, `tenant_id` bigint NOT NULL DEFAULT 0, PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @hrm_payroll_menu = (SELECT id FROM system_menu WHERE path='/hrm-payroll' AND deleted=0 LIMIT 1);
INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name)
SELECT '银行代发', 'hrm:payroll:payment:query', 2, 30, @hrm_payroll_menu, 'payment', 'ep:credit-card', 'hrm/payroll/payment/index', 'HrmPayrollPayment'
WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE permission='hrm:payroll:payment:query' AND deleted=0);
SET @hrm_payment_menu = (SELECT id FROM system_menu WHERE permission='hrm:payroll:payment:query' AND deleted=0 LIMIT 1);
INSERT INTO system_menu (name, permission, type, parent_id) SELECT '生成代发及失败重试', 'hrm:payroll:batch:pay', 3, @hrm_payment_menu WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE permission='hrm:payroll:batch:pay' AND deleted=0);
INSERT INTO system_menu (name, permission, type, parent_id) SELECT '导出银行文件', 'hrm:payroll:payment:export', 3, @hrm_payment_menu WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE permission='hrm:payroll:payment:export' AND deleted=0);
INSERT INTO system_menu (name, permission, type, parent_id) SELECT '导入回盘', 'hrm:payroll:payment:reconcile', 3, @hrm_payment_menu WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE permission='hrm:payroll:payment:reconcile' AND deleted=0);
INSERT INTO system_menu (name, permission, type, parent_id) SELECT '配置银行模板', 'hrm:payroll:payment:config', 3, @hrm_payment_menu WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE permission='hrm:payroll:payment:config' AND deleted=0);

CREATE TABLE IF NOT EXISTS `hrm_payroll_payment_receipt` (
 `id` bigint NOT NULL AUTO_INCREMENT,
 `month_record_id` bigint NOT NULL, `file_name` varchar(255), `digest` varchar(64) NOT NULL,
 `row_count` int NOT NULL, `actor_id` bigint NOT NULL, `details` LONGTEXT NOT NULL,
 `creator` varchar(64) DEFAULT '', `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `updater` varchar(64) DEFAULT '', `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `deleted` bit NOT NULL DEFAULT FALSE, `tenant_id` bigint NOT NULL DEFAULT 0, PRIMARY KEY (`id`), UNIQUE (`tenant_id`,`month_record_id`,`digest`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
