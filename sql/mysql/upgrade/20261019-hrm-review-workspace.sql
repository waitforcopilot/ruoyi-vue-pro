SET NAMES utf8mb4;
-- S4 review-only fields, explicit scope decision action; no role expansion.
SET @s4_ddl=IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='hrm_payroll_requirement' AND column_name='business_context_json')=0,'ALTER TABLE hrm_payroll_requirement ADD COLUMN business_context_json longtext NULL','SELECT 1');
PREPARE s4_stmt FROM @s4_ddl; EXECUTE s4_stmt; DEALLOCATE PREPARE s4_stmt;
SET @s4_ddl=IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='hrm_payroll_requirement' AND column_name='scope_decision_json')=0,'ALTER TABLE hrm_payroll_requirement ADD COLUMN scope_decision_json longtext NULL','SELECT 1');
PREPARE s4_stmt FROM @s4_ddl; EXECUTE s4_stmt; DEALLOCATE PREPARE s4_stmt;
SET @s4_ddl=IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='hrm_payroll_requirement' AND column_name='implementation_json')=0,'ALTER TABLE hrm_payroll_requirement ADD COLUMN implementation_json longtext NULL','SELECT 1');
PREPARE s4_stmt FROM @s4_ddl; EXECUTE s4_stmt; DEALLOCATE PREPARE s4_stmt;
SET @s4_ddl=IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='hrm_payroll_requirement' AND column_name='collaboration_json')=0,'ALTER TABLE hrm_payroll_requirement ADD COLUMN collaboration_json longtext NULL','SELECT 1');
PREPARE s4_stmt FROM @s4_ddl; EXECUTE s4_stmt; DEALLOCATE PREPARE s4_stmt;
SET @s4_ddl=IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='hrm_payroll_requirement' AND column_name='legacy_review_status')=0,'ALTER TABLE hrm_payroll_requirement ADD COLUMN legacy_review_status varchar(32) NULL','SELECT 1');
PREPARE s4_stmt FROM @s4_ddl; EXECUTE s4_stmt; DEALLOCATE PREPARE s4_stmt;
UPDATE hrm_payroll_requirement SET legacy_review_status='DEFERRED',scope_decision='DEFERRED',status='REVIEWING',scope_decision_json=JSON_OBJECT('migrationPending',true,'legacyStatus','DEFERRED','missing','reason,responsibleId,reevaluate','migration','S4-20261019') WHERE status='DEFERRED' AND legacy_review_status IS NULL;
INSERT INTO system_menu(name,permission,type,sort,parent_id,status) SELECT '需求范围决策','hrm:payroll:requirement:scope-decide',3,91,id,0 FROM system_menu WHERE permission='hrm:payroll:requirement:query' AND deleted=0 AND NOT EXISTS(SELECT 1 FROM system_menu y WHERE y.permission='hrm:payroll:requirement:scope-decide' AND y.deleted=0) LIMIT 1;
