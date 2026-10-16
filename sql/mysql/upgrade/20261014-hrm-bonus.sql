SET NAMES utf8mb4;
CREATE TABLE IF NOT EXISTS `hrm_payroll_bonus` (
 `id` bigint NOT NULL AUTO_INCREMENT, `batch_id` bigint, `entity_id` bigint, `employee_id` bigint, `profile_id` bigint, `prior_bonus_id` bigint,
 `version` int, `expected_payment_date` date, `amount` decimal(20,2), `mode` varchar(32), `evidence` varchar(1000),
 `request_key` varchar(100), `request_snapshot` LONGTEXT, `status` varchar(32), `submitted_by` bigint, `reviewed_by` bigint,
 `review_reason` varchar(1000), `reviewed_time` datetime, `creator` varchar(64) DEFAULT '', `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `updater` varchar(64) DEFAULT '', `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `deleted` bit NOT NULL DEFAULT FALSE, `tenant_id` bigint NOT NULL DEFAULT 0,
 `active_bonus_key` varchar(128) GENERATED ALWAYS AS (CASE WHEN `deleted`=b'0' AND `status`='APPROVED' THEN CONCAT(`batch_id`,':',`employee_id`) ELSE NULL END) STORED,
 PRIMARY KEY (`id`), UNIQUE KEY `uk_bonus_request` (`tenant_id`,`request_key`),
 UNIQUE KEY `uk_bonus_version` (`tenant_id`,`batch_id`,`employee_id`,`version`),
 UNIQUE KEY `uk_bonus_active` (`tenant_id`,`active_bonus_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS `hrm_payroll_bonus_event` (
 `id` bigint NOT NULL AUTO_INCREMENT, `bonus_id` bigint, `actor_id` bigint, `action` varchar(32), `reason` varchar(1000), `snapshot` LONGTEXT,
 `creator` varchar(64) DEFAULT '', `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `updater` varchar(64) DEFAULT '', `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `deleted` bit NOT NULL DEFAULT FALSE, `tenant_id` bigint NOT NULL DEFAULT 0, PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
