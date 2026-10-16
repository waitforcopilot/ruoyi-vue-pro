SET NAMES utf8mb4;
SET @ddl=IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='hrm_salary_month_record' AND column_name='parent_batch_id')=0,'ALTER TABLE hrm_salary_month_record ADD COLUMN parent_batch_id bigint','SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
