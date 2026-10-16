SET NAMES utf8mb4;
-- Cross-batch payroll source claims; released records remain auditable.
CREATE TABLE IF NOT EXISTS `hrm_payroll_source_claim` (
 `id` bigint NOT NULL AUTO_INCREMENT,
 `entity_id` bigint,
 `employee_id` bigint,
 `batch_id` bigint,
 `run_id` bigint,
 `kind` varchar(32),
 `source_key` varchar(128),
 `amount` decimal(18,2),
 `status` varchar(32),
 `released_by` bigint,
 `release_reason` varchar(1000),
 `creator` varchar(64) DEFAULT '', `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `updater` varchar(64) DEFAULT '', `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `deleted` bit NOT NULL DEFAULT FALSE, `tenant_id` bigint NOT NULL DEFAULT 0,
 `active_source_key` varchar(128) GENERATED ALWAYS AS (CASE WHEN `deleted`=b'0' AND `status` IN ('RESERVED','PAID') THEN `source_key` ELSE NULL END) STORED,
 PRIMARY KEY (`id`),
 UNIQUE KEY `uk_payroll_claim_active_source` (`tenant_id`,`kind`,`active_source_key`),
 KEY `idx_payroll_claim_batch_run` (`tenant_id`,`batch_id`,`run_id`,`employee_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
