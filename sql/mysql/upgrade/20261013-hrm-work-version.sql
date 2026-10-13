-- Approved rule succession preserves original dates, parameters and evidence.
SET @hrm_alter=IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='hrm_payroll_work_rule' AND COLUMN_NAME='prior_rule_id')=0, 'ALTER TABLE `hrm_payroll_work_rule` ADD COLUMN `prior_rule_id` bigint NULL', 'SELECT 1');
PREPARE hrm_stmt FROM @hrm_alter; EXECUTE hrm_stmt; DEALLOCATE PREPARE hrm_stmt;
SET @hrm_alter=IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='hrm_payroll_work_rule' AND COLUMN_NAME='submitted_by')=0, 'ALTER TABLE `hrm_payroll_work_rule` ADD COLUMN `submitted_by` bigint NULL', 'SELECT 1');
PREPARE hrm_stmt FROM @hrm_alter; EXECUTE hrm_stmt; DEALLOCATE PREPARE hrm_stmt;
SET @hrm_alter=IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='hrm_payroll_work_rule' AND COLUMN_NAME='superseded_from')=0, 'ALTER TABLE `hrm_payroll_work_rule` ADD COLUMN `superseded_from` date NULL', 'SELECT 1');
PREPARE hrm_stmt FROM @hrm_alter; EXECUTE hrm_stmt; DEALLOCATE PREPARE hrm_stmt;
SET @hrm_alter=IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='hrm_payroll_work_rule' AND COLUMN_NAME='superseded_by')=0, 'ALTER TABLE `hrm_payroll_work_rule` ADD COLUMN `superseded_by` bigint NULL', 'SELECT 1');
PREPARE hrm_stmt FROM @hrm_alter; EXECUTE hrm_stmt; DEALLOCATE PREPARE hrm_stmt;
