-- Evidence-based work versus schedule cases, with independent review and immutable events.
CREATE TABLE IF NOT EXISTS `hrm_payroll_work_variance` (
 `id` bigint NOT NULL AUTO_INCREMENT, `entity_id` bigint, `employee_id` bigint, `work_date` date,
 `request_key` varchar(100), `reason` varchar(1000), `source_snapshot` LONGTEXT,
 `owner_id` bigint, `reviewer_id` bigint, `due_time` datetime, `status` varchar(32),
 `submitted_by` bigint, `evidence` varchar(1000), `submitted_snapshot` LONGTEXT,
 `review_opinion` varchar(1000), `closed_time` datetime,
 `reminder_state` LONGTEXT, `reminder_error` varchar(1000),
 `creator` varchar(64) DEFAULT '', `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `updater` varchar(64) DEFAULT '', `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `deleted` bit NOT NULL DEFAULT FALSE, `tenant_id` bigint NOT NULL DEFAULT 0,
 `active_date_key` varchar(128) GENERATED ALWAYS AS (CASE WHEN `deleted`=b'0' AND `status` IN ('OPEN','SUBMITTED') THEN CONCAT(`entity_id`,':',`employee_id`,':',`work_date`) ELSE NULL END) STORED,
 PRIMARY KEY (`id`), UNIQUE KEY `uk_work_variance_request` (`tenant_id`,`request_key`),
 UNIQUE KEY `uk_work_variance_active` (`tenant_id`,`active_date_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS `hrm_payroll_work_variance_event` (
 `id` bigint NOT NULL AUTO_INCREMENT, `variance_id` bigint, `action` varchar(32), `actor_id` bigint,
 `reason` varchar(1000), `snapshot` LONGTEXT,
 `creator` varchar(64) DEFAULT '', `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `updater` varchar(64) DEFAULT '', `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `deleted` bit NOT NULL DEFAULT FALSE, `tenant_id` bigint NOT NULL DEFAULT 0,
 PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
SET @hrm_alter=IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='hrm_payroll_work_variance' AND COLUMN_NAME='reminder_state')=0, 'ALTER TABLE `hrm_payroll_work_variance` ADD COLUMN `reminder_state` LONGTEXT NULL', 'SELECT 1');
PREPARE hrm_stmt FROM @hrm_alter; EXECUTE hrm_stmt; DEALLOCATE PREPARE hrm_stmt;
SET @hrm_alter=IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='hrm_payroll_work_variance' AND COLUMN_NAME='reminder_error')=0, 'ALTER TABLE `hrm_payroll_work_variance` ADD COLUMN `reminder_error` varchar(1000) NULL', 'SELECT 1');
PREPARE hrm_stmt FROM @hrm_alter; EXECUTE hrm_stmt; DEALLOCATE PREPARE hrm_stmt;
INSERT INTO `system_notify_template` (`name`,`code`,`nickname`,`content`,`type`,`params`,`status`,`remark`)
SELECT '工时差异逾期提醒','hrm_work_variance_overdue','人力资源','工时差异工单 {caseId}（{workDate}）已超过处理期限 {dueTime}，请进入工时与排班差异核实。',2,'["caseId","workDate","dueTime"]',0,'仅包含工单编号及期限，不包含薪资和原证据'
WHERE NOT EXISTS (SELECT 1 FROM `system_notify_template` WHERE `code`='hrm_work_variance_overdue' AND `deleted`=b'0');
INSERT INTO `infra_job` (`name`,`status`,`handler_name`,`handler_param`,`cron_expression`,`retry_count`,`retry_interval`,`monitor_timeout`)
SELECT '工时差异逾期站内提醒',2,'hrmPayrollWorkVarianceReminderJob','','0 0 9 * * ?',0,0,0
WHERE NOT EXISTS (SELECT 1 FROM `infra_job` WHERE `handler_name`='hrmPayrollWorkVarianceReminderJob' AND `deleted`=b'0');
