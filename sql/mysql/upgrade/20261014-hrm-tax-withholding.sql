-- Residency/first-income qualification is explicit; historical unknown flags stay NULL.
SET @hrm_alter=IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='hrm_payroll_tax_profile' AND COLUMN_NAME='resident_this_year')=0, 'ALTER TABLE `hrm_payroll_tax_profile` ADD COLUMN `resident_this_year` bit NULL', 'SELECT 1');
PREPARE hrm_stmt FROM @hrm_alter; EXECUTE hrm_stmt; DEALLOCATE PREPARE hrm_stmt;
SET @hrm_alter=IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='hrm_payroll_tax_profile' AND COLUMN_NAME='no_prior_continuous_remuneration_before_entry')=0, 'ALTER TABLE `hrm_payroll_tax_profile` ADD COLUMN `no_prior_continuous_remuneration_before_entry` bit NULL', 'SELECT 1');
PREPARE hrm_stmt FROM @hrm_alter; EXECUTE hrm_stmt; DEALLOCATE PREPARE hrm_stmt;
SET @hrm_alter=IF((SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='hrm_payroll_source_claim' AND INDEX_NAME='idx_hrm_claim_tax_pending')=0, 'ALTER TABLE `hrm_payroll_source_claim` ADD INDEX `idx_hrm_claim_tax_pending` (`tenant_id`,`kind`,`status`,`source_key`)', 'SELECT 1');
PREPARE hrm_stmt FROM @hrm_alter; EXECUTE hrm_stmt; DEALLOCATE PREPARE hrm_stmt;
