SET NAMES utf8mb4;
-- Planned payment dates are explicit business data, copied into each immutable run.
SET @hrm_alter=IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='hrm_salary_month_record' AND COLUMN_NAME='expected_payment_date')=0, 'ALTER TABLE `hrm_salary_month_record` ADD COLUMN `expected_payment_date` date NULL', 'SELECT 1');
PREPARE hrm_stmt FROM @hrm_alter; EXECUTE hrm_stmt; DEALLOCATE PREPARE hrm_stmt;
SET @hrm_alter=IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='hrm_payroll_run' AND COLUMN_NAME='expected_payment_date')=0, 'ALTER TABLE `hrm_payroll_run` ADD COLUMN `expected_payment_date` date NULL', 'SELECT 1');
PREPARE hrm_stmt FROM @hrm_alter; EXECUTE hrm_stmt; DEALLOCATE PREPARE hrm_stmt;
