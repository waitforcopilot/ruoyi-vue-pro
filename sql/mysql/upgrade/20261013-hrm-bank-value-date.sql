-- Actual value dates are required for new successful receipts; old dates are not guessed.
SET @hrm_alter=IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='hrm_payroll_payment' AND COLUMN_NAME='value_date')=0, 'ALTER TABLE `hrm_payroll_payment` ADD COLUMN `value_date` date NULL', 'SELECT 1');
PREPARE hrm_stmt FROM @hrm_alter; EXECUTE hrm_stmt; DEALLOCATE PREPARE hrm_stmt;
