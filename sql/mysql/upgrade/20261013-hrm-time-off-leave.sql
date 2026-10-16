SET NAMES utf8mb4;
-- Link approved leave processes to the existing time-off ledger. Legacy leaves remain unchanged.
SET @hrm_alter = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='hrm_attendance_leave' AND COLUMN_NAME='time_off_debit_id')=0, 'ALTER TABLE `hrm_attendance_leave` ADD COLUMN `time_off_debit_id` bigint NULL', 'SELECT 1');
PREPARE hrm_stmt FROM @hrm_alter; EXECUTE hrm_stmt; DEALLOCATE PREPARE hrm_stmt;
SET @hrm_alter = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='hrm_attendance_leave' AND COLUMN_NAME='time_off_minutes')=0, 'ALTER TABLE `hrm_attendance_leave` ADD COLUMN `time_off_minutes` int NULL', 'SELECT 1');
PREPARE hrm_stmt FROM @hrm_alter; EXECUTE hrm_stmt; DEALLOCATE PREPARE hrm_stmt;
SET @hrm_alter = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='hrm_attendance_leave' AND COLUMN_NAME='time_off_source_snapshot')=0, 'ALTER TABLE `hrm_attendance_leave` ADD COLUMN `time_off_source_snapshot` LONGTEXT NULL', 'SELECT 1');
PREPARE hrm_stmt FROM @hrm_alter; EXECUTE hrm_stmt; DEALLOCATE PREPARE hrm_stmt;
SET @hrm_alter = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='hrm_payroll_time_off' AND COLUMN_NAME='reserved_for_date')=0, 'ALTER TABLE `hrm_payroll_time_off` ADD COLUMN `reserved_for_date` date NULL', 'SELECT 1');
PREPARE hrm_stmt FROM @hrm_alter; EXECUTE hrm_stmt; DEALLOCATE PREPARE hrm_stmt;
