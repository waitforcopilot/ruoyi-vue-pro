SET NAMES utf8mb4;
CREATE TABLE IF NOT EXISTS `hrm_payroll_tax_filing` (
 `id` bigint NOT NULL AUTO_INCREMENT, `entity_id` bigint, `prior_filing_id` bigint, `year` int, `month` int, `version` int, `zero_declaration` bit,
 `request_key` varchar(100), `request_snapshot` LONGTEXT, `source_snapshot` LONGTEXT, `status` varchar(32), `evidence` varchar(1000), `review_reason` varchar(1000),
 `submitted_by` bigint, `reviewed_by` bigint, `filed_by` bigint, `reviewed_time` datetime,
 `filing_reference` varchar(100), `filing_evidence` varchar(1000), `filed_date` date, `expected_tax` decimal(20,2),
 `creator` varchar(64) DEFAULT '', `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `updater` varchar(64) DEFAULT '', `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `deleted` bit NOT NULL DEFAULT FALSE, `tenant_id` bigint NOT NULL DEFAULT 0,
 `active_filing_key` varchar(128) GENERATED ALWAYS AS (CASE WHEN `deleted`=b'0' AND `status` IN ('APPROVED','FILED','RECONCILED') THEN CONCAT(`entity_id`,':',`year`,':',`month`) ELSE NULL END) STORED,
 PRIMARY KEY (`id`), UNIQUE KEY `uk_tax_filing_request` (`tenant_id`,`request_key`),
 UNIQUE KEY `uk_tax_filing_version` (`tenant_id`,`entity_id`,`year`,`month`,`version`),
 UNIQUE KEY `uk_tax_filing_active` (`tenant_id`,`active_filing_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS `hrm_payroll_tax_receipt` (
 `id` bigint NOT NULL AUTO_INCREMENT, `filing_id` bigint, `entity_id` bigint, `original_receipt_id` bigint, `year` int, `month` int,
 `kind` varchar(32), `reference` varchar(100), `evidence` varchar(1000), `status` varchar(32), `request_key` varchar(100), `request_snapshot` LONGTEXT, `review_reason` varchar(1000),
 `payment_date` date, `amount` decimal(20,2), `submitted_by` bigint, `reviewed_by` bigint, `reviewed_time` datetime,
 `creator` varchar(64) DEFAULT '', `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `updater` varchar(64) DEFAULT '', `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `deleted` bit NOT NULL DEFAULT FALSE, `tenant_id` bigint NOT NULL DEFAULT 0, PRIMARY KEY (`id`), UNIQUE KEY `uk_tax_receipt_request` (`tenant_id`,`request_key`), `active_reference` varchar(100) GENERATED ALWAYS AS (CASE WHEN `deleted`=b'0' AND `status` IN ('PENDING','APPROVED') THEN `reference` ELSE NULL END) STORED, UNIQUE KEY `uk_tax_receipt_reference` (`tenant_id`,`entity_id`,`active_reference`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS `hrm_payroll_tax_filing_event` (
 `id` bigint NOT NULL AUTO_INCREMENT, `filing_id` bigint, `receipt_id` bigint, `actor_id` bigint, `action` varchar(32), `reason` varchar(1000), `snapshot` LONGTEXT,
 `creator` varchar(64) DEFAULT '', `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `updater` varchar(64) DEFAULT '', `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `deleted` bit NOT NULL DEFAULT FALSE, `tenant_id` bigint NOT NULL DEFAULT 0, PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
