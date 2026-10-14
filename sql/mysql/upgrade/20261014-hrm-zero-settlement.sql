-- Zero net is an accounting settlement, not an automatically completed bank transfer.
SET @hrm_alter=IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='hrm_payroll_payment' AND COLUMN_NAME='zero_settlement_reference')=0, 'ALTER TABLE `hrm_payroll_payment` ADD COLUMN `zero_settlement_reference` varchar(100) NULL', 'SELECT 1');
PREPARE hrm_stmt FROM @hrm_alter; EXECUTE hrm_stmt; DEALLOCATE PREPARE hrm_stmt;
SET @hrm_alter=IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='hrm_payroll_payment' AND COLUMN_NAME='zero_settlement_evidence')=0, 'ALTER TABLE `hrm_payroll_payment` ADD COLUMN `zero_settlement_evidence` varchar(1000) NULL', 'SELECT 1');
PREPARE hrm_stmt FROM @hrm_alter; EXECUTE hrm_stmt; DEALLOCATE PREPARE hrm_stmt;
SET @hrm_alter=IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='hrm_payroll_payment' AND COLUMN_NAME='zero_settled_by')=0, 'ALTER TABLE `hrm_payroll_payment` ADD COLUMN `zero_settled_by` bigint NULL', 'SELECT 1');
PREPARE hrm_stmt FROM @hrm_alter; EXECUTE hrm_stmt; DEALLOCATE PREPARE hrm_stmt;
SET @hrm_alter=IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='hrm_payroll_payment' AND COLUMN_NAME='zero_settlement_time')=0, 'ALTER TABLE `hrm_payroll_payment` ADD COLUMN `zero_settlement_time` datetime NULL', 'SELECT 1');
PREPARE hrm_stmt FROM @hrm_alter; EXECUTE hrm_stmt; DEALLOCATE PREPARE hrm_stmt;

-- Preserve canceled preparation records while enforcing one live payment per batch/employee.
SET @hrm_alter=IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='hrm_payroll_payment' AND COLUMN_NAME='active_employee_id')=0, "ALTER TABLE `hrm_payroll_payment` ADD COLUMN `active_employee_id` bigint GENERATED ALWAYS AS (CASE WHEN `deleted`=b'0' THEN `employee_id` ELSE NULL END) STORED", 'SELECT 1');
PREPARE hrm_stmt FROM @hrm_alter; EXECUTE hrm_stmt; DEALLOCATE PREPARE hrm_stmt;
SET @hrm_alter=IF((SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='hrm_payroll_payment' AND INDEX_NAME='uk_payroll_payment_active')=0, 'ALTER TABLE `hrm_payroll_payment` ADD UNIQUE KEY `uk_payroll_payment_active` (`tenant_id`,`month_record_id`,`active_employee_id`)', 'SELECT 1');
PREPARE hrm_stmt FROM @hrm_alter; EXECUTE hrm_stmt; DEALLOCATE PREPARE hrm_stmt;
SET @hrm_old_index=(SELECT MIN(idx) FROM (SELECT INDEX_NAME AS idx,GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ',') AS cols FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='hrm_payroll_payment' AND NON_UNIQUE=0 AND INDEX_NAME<>'PRIMARY' GROUP BY INDEX_NAME) hrm_indexes WHERE cols='tenant_id,month_record_id,employee_id');
SET @hrm_alter=IF(@hrm_old_index IS NULL,'SELECT 1',CONCAT('ALTER TABLE `hrm_payroll_payment` DROP INDEX `',REPLACE(@hrm_old_index,'`','``'),'`'));
PREPARE hrm_stmt FROM @hrm_alter; EXECUTE hrm_stmt; DEALLOCATE PREPARE hrm_stmt;
