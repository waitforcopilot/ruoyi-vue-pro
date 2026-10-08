-- Apply after trial batches, scheme versions and review/freeze. No business defaults or grants.
-- Nullable selection preserves the historical unbound trial contract; existing rows stay unbound.
SET @binding_column_exists = (SELECT COUNT(*) FROM information_schema.columns
 WHERE table_schema = DATABASE() AND table_name = 'hrm_payroll_trial_batch' AND column_name = 'scheme_id');
SET @binding_upgrade = IF(@binding_column_exists = 0,
 'ALTER TABLE hrm_payroll_trial_batch ADD COLUMN scheme_id bigint NULL COMMENT ''Explicit scheme version; source bindings live in configuration_json'' AFTER definition_id',
 'SELECT 1');
PREPARE hrm_binding_statement FROM @binding_upgrade;
EXECUTE hrm_binding_statement;
DEALLOCATE PREPARE hrm_binding_statement;
