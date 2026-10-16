SET NAMES utf8mb4;
-- Explicit employer ownership; legacy unbound work sources are not auto-assigned.
SET @hrm_alter=IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='hrm_payroll_work_rule' AND COLUMN_NAME='entity_id')=0, 'ALTER TABLE `hrm_payroll_work_rule` ADD COLUMN `entity_id` bigint NULL', 'SELECT 1');
PREPARE hrm_stmt FROM @hrm_alter; EXECUTE hrm_stmt; DEALLOCATE PREPARE hrm_stmt;
SET @hrm_alter=IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='hrm_payroll_time_entry' AND COLUMN_NAME='entity_id')=0, 'ALTER TABLE `hrm_payroll_time_entry` ADD COLUMN `entity_id` bigint NULL', 'SELECT 1');
PREPARE hrm_stmt FROM @hrm_alter; EXECUTE hrm_stmt; DEALLOCATE PREPARE hrm_stmt;
SET @hrm_alter=IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='hrm_payroll_work_cycle' AND COLUMN_NAME='entity_id')=0, 'ALTER TABLE `hrm_payroll_work_cycle` ADD COLUMN `entity_id` bigint NULL', 'SELECT 1');
PREPARE hrm_stmt FROM @hrm_alter; EXECUTE hrm_stmt; DEALLOCATE PREPARE hrm_stmt;
SET @hrm_alter=IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='hrm_payroll_work_rule' AND COLUMN_NAME='rejected_by')=0, 'ALTER TABLE `hrm_payroll_work_rule` ADD COLUMN `rejected_by` bigint NULL', 'SELECT 1');
PREPARE hrm_stmt FROM @hrm_alter; EXECUTE hrm_stmt; DEALLOCATE PREPARE hrm_stmt;
SET @hrm_alter=IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='hrm_payroll_work_rule' AND COLUMN_NAME='rejection_reason')=0, 'ALTER TABLE `hrm_payroll_work_rule` ADD COLUMN `rejection_reason` varchar(1000) NULL', 'SELECT 1');
PREPARE hrm_stmt FROM @hrm_alter; EXECUTE hrm_stmt; DEALLOCATE PREPARE hrm_stmt;
