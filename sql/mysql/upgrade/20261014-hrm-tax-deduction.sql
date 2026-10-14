-- Explicit authorized monthly cumulative deduction declarations; no inferred family eligibility.
CREATE TABLE IF NOT EXISTS `hrm_payroll_tax_deduction` (
 `id` bigint NOT NULL AUTO_INCREMENT, `entity_id` bigint, `employee_id` bigint, `prior_declaration_id` bigint,
 `year` int, `month` int, `version` int, `tax_registration_number` varchar(100), `taxpayer_key` varchar(128),
 `source_date` date, `mode` varchar(32), `values_snapshot` LONGTEXT, `official_reference` varchar(1000), `evidence` varchar(1000),
 `request_key` varchar(100), `request_snapshot` LONGTEXT, `status` varchar(32), `submitted_by` bigint, `reviewed_by` bigint,
 `review_reason` varchar(1000), `reviewed_time` datetime,
 `creator` varchar(64) DEFAULT '', `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `updater` varchar(64) DEFAULT '', `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `deleted` bit NOT NULL DEFAULT FALSE, `tenant_id` bigint NOT NULL DEFAULT 0,
 `active_declaration_key` varchar(128) GENERATED ALWAYS AS (CASE WHEN `deleted`=b'0' AND `status`='APPROVED' THEN CONCAT(`entity_id`,':',`employee_id`,':',`year`,':',`month`) ELSE NULL END) STORED,
 PRIMARY KEY (`id`), UNIQUE KEY `uk_tax_deduction_request` (`tenant_id`,`request_key`),
 UNIQUE KEY `uk_tax_deduction_version` (`tenant_id`,`entity_id`,`employee_id`,`year`,`month`,`version`),
 UNIQUE KEY `uk_tax_deduction_active` (`tenant_id`,`active_declaration_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS `hrm_payroll_tax_deduction_event` (
 `id` bigint NOT NULL AUTO_INCREMENT, `declaration_id` bigint, `actor_id` bigint, `action` varchar(32), `reason` varchar(1000), `snapshot` LONGTEXT,
 `creator` varchar(64) DEFAULT '', `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `updater` varchar(64) DEFAULT '', `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `deleted` bit NOT NULL DEFAULT FALSE, `tenant_id` bigint NOT NULL DEFAULT 0, PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
INSERT INTO hrm_salary_option_template(code,parent_code,name,remark,system_flag,type,tax_enabled,visible,calculate_enabled)
SELECT 260106,260,'累计婴幼儿照护','仅依据经授权且复核的累计扣除来源，不接受通用公式或人工调整',1,1,1,1,1
WHERE NOT EXISTS (SELECT 1 FROM hrm_salary_option_template WHERE code=260106);
-- Run the existing salary-option catalog synchronization for each tenant after installing this template.
