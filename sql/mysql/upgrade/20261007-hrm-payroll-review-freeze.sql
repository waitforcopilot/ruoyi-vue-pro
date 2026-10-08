-- Version-bound review evidence and idempotent commands; amounts remain immutable trial results.
CREATE TABLE IF NOT EXISTS hrm_payroll_review_cycle (
 id bigint NOT NULL AUTO_INCREMENT,batch_id bigint NOT NULL,run_id bigint NOT NULL,cycle_version int NOT NULL,status int NOT NULL DEFAULT 0,source_hash char(64) NOT NULL,
 process_instance_id varchar(128) CHARACTER SET ascii COLLATE ascii_bin,process_definition_id varchar(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 started_by bigint NOT NULL,started_by_name varchar(120),submit_evidence text NOT NULL,started_at datetime NOT NULL,
 hr_reviewer_id bigint NOT NULL,hr_reviewer_name varchar(120),finance_reviewer_id bigint NOT NULL,finance_reviewer_name varchar(120),
 hr_reviewed_at datetime,hr_evidence text,finance_reviewed_at datetime,finance_evidence text,outcome varchar(32),finished_at datetime,
 frozen_by bigint,frozen_by_name varchar(120),freeze_evidence text,frozen_at datetime,
 unfrozen_by bigint,unfrozen_by_name varchar(120),unfreeze_evidence text,unfrozen_at datetime,
 creator varchar(64) DEFAULT '',create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updater varchar(64) DEFAULT '',update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 deleted bit NOT NULL DEFAULT 0,tenant_id bigint NOT NULL,
 PRIMARY KEY(id),UNIQUE KEY uk_review_cycle(tenant_id,batch_id,cycle_version),UNIQUE KEY uk_review_process(tenant_id,process_instance_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS hrm_payroll_review_command (
 id bigint NOT NULL AUTO_INCREMENT,batch_id bigint NOT NULL,request_key varchar(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 actor_id bigint NOT NULL,request_hash char(64) NOT NULL,response_json mediumtext NOT NULL,
 creator varchar(64) DEFAULT '',create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updater varchar(64) DEFAULT '',update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 deleted bit NOT NULL DEFAULT 0,tenant_id bigint NOT NULL,
 PRIMARY KEY(id),UNIQUE KEY uk_review_command(tenant_id,batch_id,request_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
SET @payroll_review_column_sql=IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='hrm_payroll_trial_batch' AND column_name='active_review_id'),'SELECT 1','ALTER TABLE hrm_payroll_trial_batch ADD COLUMN active_review_id bigint NULL');
PREPARE payroll_review_column_stmt FROM @payroll_review_column_sql;
EXECUTE payroll_review_column_stmt;
DEALLOCATE PREPARE payroll_review_column_stmt;
SET @payroll_review_column_sql=IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='hrm_payroll_trial_batch' AND column_name='frozen_run_id'),'SELECT 1','ALTER TABLE hrm_payroll_trial_batch ADD COLUMN frozen_run_id bigint NULL');
PREPARE payroll_review_column_stmt FROM @payroll_review_column_sql;
EXECUTE payroll_review_column_stmt;
DEALLOCATE PREPARE payroll_review_column_stmt;
INSERT INTO system_menu(name,permission,type,sort,parent_id,path,visible) SELECT '发起版本复核','hrm:payroll:trial:review-submit',3,3,m.id,'',0 FROM system_menu m WHERE m.component='hrm/payroll/trial/index' AND m.deleted=0 AND NOT EXISTS(SELECT 1 FROM system_menu WHERE permission='hrm:payroll:trial:review-submit' AND deleted=0) LIMIT 1;
INSERT INTO system_menu(name,permission,type,sort,parent_id,path,visible) SELECT 'HR 版本复核','hrm:payroll:trial:hr-review',3,4,m.id,'',0 FROM system_menu m WHERE m.component='hrm/payroll/trial/index' AND m.deleted=0 AND NOT EXISTS(SELECT 1 FROM system_menu WHERE permission='hrm:payroll:trial:hr-review' AND deleted=0) LIMIT 1;
INSERT INTO system_menu(name,permission,type,sort,parent_id,path,visible) SELECT '财务版本复核','hrm:payroll:trial:finance-review',3,5,m.id,'',0 FROM system_menu m WHERE m.component='hrm/payroll/trial/index' AND m.deleted=0 AND NOT EXISTS(SELECT 1 FROM system_menu WHERE permission='hrm:payroll:trial:finance-review' AND deleted=0) LIMIT 1;
INSERT INTO system_menu(name,permission,type,sort,parent_id,path,visible) SELECT '冻结已复核版本','hrm:payroll:trial:freeze',3,6,m.id,'',0 FROM system_menu m WHERE m.component='hrm/payroll/trial/index' AND m.deleted=0 AND NOT EXISTS(SELECT 1 FROM system_menu WHERE permission='hrm:payroll:trial:freeze' AND deleted=0) LIMIT 1;
INSERT INTO system_menu(name,permission,type,sort,parent_id,path,visible) SELECT '凭依据解冻版本','hrm:payroll:trial:unfreeze',3,7,m.id,'',0 FROM system_menu m WHERE m.component='hrm/payroll/trial/index' AND m.deleted=0 AND NOT EXISTS(SELECT 1 FROM system_menu WHERE permission='hrm:payroll:trial:unfreeze' AND deleted=0) LIMIT 1;

INSERT INTO system_menu(name,permission,type,sort,parent_id,path,visible) SELECT '管理撤销版本复核','hrm:payroll:trial:admin-cancel',3,8,m.id,'',0 FROM system_menu m WHERE m.component='hrm/payroll/trial/index' AND m.deleted=0 AND NOT EXISTS(SELECT 1 FROM system_menu WHERE permission='hrm:payroll:trial:admin-cancel' AND deleted=0) LIMIT 1;
