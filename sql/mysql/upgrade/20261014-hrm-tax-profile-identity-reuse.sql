SET NAMES utf8mb4;
-- Reused ciphertext comes from a verified identity of the same entity and employee.
SET @hrm_alter=IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='hrm_payroll_tax_profile' AND COLUMN_NAME='identity_source_profile_id')=0, 'ALTER TABLE `hrm_payroll_tax_profile` ADD COLUMN `identity_source_profile_id` bigint NULL', 'SELECT 1');
PREPARE hrm_stmt FROM @hrm_alter; EXECUTE hrm_stmt; DEALLOCATE PREPARE hrm_stmt;
