-- Existing historical batches remain explicitly unbound until attributed with audit evidence.
SET @hrm_scope_ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='hrm_salary_month_record' AND column_name='entity_id')=0, 'ALTER TABLE hrm_salary_month_record ADD COLUMN entity_id bigint NULL', 'SELECT 1');
PREPARE hrm_scope_stmt FROM @hrm_scope_ddl; EXECUTE hrm_scope_stmt; DEALLOCATE PREPARE hrm_scope_stmt;
SET @hrm_scope_ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='hrm_salary_month_record' AND column_name='batch_key')=0, 'ALTER TABLE hrm_salary_month_record ADD COLUMN batch_key varchar(64) NOT NULL DEFAULT \'REGULAR\'', 'SELECT 1');
PREPARE hrm_scope_stmt FROM @hrm_scope_ddl; EXECUTE hrm_scope_stmt; DEALLOCATE PREPARE hrm_scope_stmt;
SET @hrm_scope_ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='hrm_salary_month_record' AND column_name='active_entity_id')=0, 'ALTER TABLE hrm_salary_month_record ADD COLUMN active_entity_id bigint GENERATED ALWAYS AS (IF(deleted=0,entity_id,NULL)) VIRTUAL', 'SELECT 1');
PREPARE hrm_scope_stmt FROM @hrm_scope_ddl; EXECUTE hrm_scope_stmt; DEALLOCATE PREPARE hrm_scope_stmt;
SET @hrm_scope_ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='hrm_salary_month_record' AND index_name='uk_payroll_subject_period_batch')=0, 'ALTER TABLE hrm_salary_month_record ADD UNIQUE KEY uk_payroll_subject_period_batch(tenant_id,active_entity_id,year,month,batch_key)', 'SELECT 1');
PREPARE hrm_scope_stmt FROM @hrm_scope_ddl; EXECUTE hrm_scope_stmt; DEALLOCATE PREPARE hrm_scope_stmt;
SET @hrm_scope_ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='hrm_salary_month_employee_record' AND column_name='entity_id')=0, 'ALTER TABLE hrm_salary_month_employee_record ADD COLUMN entity_id bigint NULL', 'SELECT 1');
PREPARE hrm_scope_stmt FROM @hrm_scope_ddl; EXECUTE hrm_scope_stmt; DEALLOCATE PREPARE hrm_scope_stmt;
SET @hrm_scope_ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='hrm_payroll_batch_event' AND column_name='snapshot')=0, 'ALTER TABLE hrm_payroll_batch_event ADD COLUMN snapshot LONGTEXT NULL', 'SELECT 1');
PREPARE hrm_scope_stmt FROM @hrm_scope_ddl; EXECUTE hrm_scope_stmt; DEALLOCATE PREPARE hrm_scope_stmt;
SET @hrm_scope_ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='hrm_payroll_batch_event' AND column_name='run_id' AND is_nullable='NO')>0, 'ALTER TABLE hrm_payroll_batch_event MODIFY COLUMN run_id bigint NULL', 'SELECT 1');
PREPARE hrm_scope_stmt FROM @hrm_scope_ddl; EXECUTE hrm_scope_stmt; DEALLOCATE PREPARE hrm_scope_stmt;
