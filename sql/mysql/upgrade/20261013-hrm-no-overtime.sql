-- Independently reviewed declarations; approval alone does not prove overtime actually occurred.
CREATE TABLE IF NOT EXISTS `hrm_payroll_no_overtime` (
 `id` bigint NOT NULL AUTO_INCREMENT,
 `entity_id` bigint, `employee_id` bigint, `source_apply_id` bigint,
 `work_date` date, `request_key` varchar(100), `reason` varchar(1000), `evidence` varchar(1000),
 `source_snapshot` LONGTEXT, `status` varchar(32), `submitted_by` bigint, `reviewed_by` bigint,
 `review_opinion` varchar(1000), `reviewed_time` datetime, `time_entry_id` bigint,
 `creator` varchar(64) DEFAULT '', `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `updater` varchar(64) DEFAULT '', `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `deleted` bit NOT NULL DEFAULT FALSE, `tenant_id` bigint NOT NULL DEFAULT 0,
 `active_source_key` varchar(128) GENERATED ALWAYS AS (CASE WHEN `deleted`=b'0' AND `status` IN ('PENDING','APPROVED') THEN CONCAT(`source_apply_id`,':',`work_date`) ELSE NULL END) STORED,
 PRIMARY KEY (`id`), UNIQUE KEY `uk_no_overtime_request` (`tenant_id`,`request_key`),
 UNIQUE KEY `uk_no_overtime_active_source` (`tenant_id`,`active_source_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
