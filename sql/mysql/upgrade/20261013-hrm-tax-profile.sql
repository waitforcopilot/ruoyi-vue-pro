-- Encrypted yearly taxpayer identity and explicitly evidenced opening balances.
CREATE TABLE IF NOT EXISTS `hrm_payroll_tax_profile` (
 `id` bigint NOT NULL AUTO_INCREMENT, `entity_id` bigint, `employee_id` bigint, `year` int, `version` int, `prior_profile_id` bigint,
 `tax_registration_number` varchar(100), `taxpayer_name` varchar(100), `identifier_type` varchar(32), `identifier_country` varchar(2),
 `identifier_cipher` varchar(512), `request_fingerprint` varchar(128), `taxpayer_key` varchar(128), `masked_identifier` varchar(100),
 `first_salary_this_year` bit, `no_prior_salary_this_year` bit, `separate_bonus_used_elsewhere` bit,
 `opening_mode` varchar(32), `opening_as_of_date` date, `opening_income` decimal(18,2), `opening_social_deduction` decimal(18,2),
 `opening_basic_deduction` decimal(18,2), `opening_withheld` decimal(18,2),
 `official_reference` varchar(1000), `evidence` varchar(1000), `employee_snapshot` LONGTEXT, `request_key` varchar(100),
 `status` varchar(32), `submitted_by` bigint, `reviewed_by` bigint, `review_opinion` varchar(1000), `reviewed_time` datetime,
 `creator` varchar(64) DEFAULT '', `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `updater` varchar(64) DEFAULT '', `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `deleted` bit NOT NULL DEFAULT FALSE, `tenant_id` bigint NOT NULL DEFAULT 0,
 `active_profile_key` varchar(128) GENERATED ALWAYS AS (CASE WHEN `deleted`=b'0' AND `status`='APPROVED' THEN CONCAT(`entity_id`,':',`employee_id`,':',`year`) ELSE NULL END) STORED,
 PRIMARY KEY (`id`), UNIQUE KEY `uk_tax_profile_request` (`tenant_id`,`request_key`),
 UNIQUE KEY `uk_tax_profile_version` (`tenant_id`,`entity_id`,`employee_id`,`year`,`version`),
 UNIQUE KEY `uk_tax_profile_active` (`tenant_id`,`active_profile_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS `hrm_payroll_tax_profile_event` (
 `id` bigint NOT NULL AUTO_INCREMENT, `profile_id` bigint, `action` varchar(32), `actor_id` bigint, `reason` varchar(1000), `snapshot` LONGTEXT,
 `creator` varchar(64) DEFAULT '', `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `updater` varchar(64) DEFAULT '', `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `deleted` bit NOT NULL DEFAULT FALSE, `tenant_id` bigint NOT NULL DEFAULT 0,
 PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
SET @hrm_tax_menu=(SELECT id FROM system_menu WHERE permission='hrm:payroll:tax:query' AND deleted=0 LIMIT 1);
INSERT INTO system_menu(name,permission,type,parent_id) SELECT '查看税务完整标识','hrm:payroll:tax:private',3,@hrm_tax_menu WHERE NOT EXISTS(SELECT 1 FROM system_menu WHERE permission='hrm:payroll:tax:private' AND deleted=0);
