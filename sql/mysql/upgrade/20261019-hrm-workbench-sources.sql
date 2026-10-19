SET NAMES utf8mb4;
-- S3 incremental storage. Existing source text remains unverified inventory; no inferred sources/signatures.
CREATE TABLE IF NOT EXISTS hrm_payroll_workbench_record (
 id bigint NOT NULL AUTO_INCREMENT,
 project_id bigint NOT NULL,
 kind varchar(24) NOT NULL,
 version int NOT NULL,
 data_json longtext NOT NULL,
 creator varchar(64) DEFAULT '', create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updater varchar(64) DEFAULT '', update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 deleted bit(1) NOT NULL DEFAULT b'0', tenant_id bigint NOT NULL DEFAULT 0,
 PRIMARY KEY(id), KEY idx_project_kind(tenant_id,project_id,kind,deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @s3_ddl=IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='hrm_payroll_requirement_project_grant' AND column_name='source_ids_json')=0,'ALTER TABLE hrm_payroll_requirement_project_grant ADD COLUMN source_ids_json text NULL','SELECT 1');
PREPARE s3_stmt FROM @s3_ddl; EXECUTE s3_stmt; DEALLOCATE PREPARE s3_stmt;

SET @s3_ddl=IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='hrm_payroll_requirement' AND column_name='source_applicability')=0,'ALTER TABLE hrm_payroll_requirement ADD COLUMN source_applicability varchar(32) NULL','SELECT 1');
PREPARE s3_stmt FROM @s3_ddl; EXECUTE s3_stmt; DEALLOCATE PREPARE s3_stmt;

SET @s3_ddl=IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='hrm_payroll_requirement' AND column_name='applicability_reason')=0,'ALTER TABLE hrm_payroll_requirement ADD COLUMN applicability_reason text NULL','SELECT 1');
PREPARE s3_stmt FROM @s3_ddl; EXECUTE s3_stmt; DEALLOCATE PREPARE s3_stmt;

SET @s3_ddl=IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='hrm_payroll_requirement' AND column_name='rule_version')=0,'ALTER TABLE hrm_payroll_requirement ADD COLUMN rule_version varchar(1000) NULL','SELECT 1');
PREPARE s3_stmt FROM @s3_ddl; EXECUTE s3_stmt; DEALLOCATE PREPARE s3_stmt;

SET @s3_ddl=IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='hrm_payroll_requirement' AND column_name='semantic_version')=0,'ALTER TABLE hrm_payroll_requirement ADD COLUMN semantic_version int NULL','SELECT 1');
PREPARE s3_stmt FROM @s3_ddl; EXECUTE s3_stmt; DEALLOCATE PREPARE s3_stmt;

SET @s3_ddl=IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='hrm_payroll_requirement' AND column_name='confirmed_tuple_json')=0,'ALTER TABLE hrm_payroll_requirement ADD COLUMN confirmed_tuple_json longtext NULL','SELECT 1');
PREPARE s3_stmt FROM @s3_ddl; EXECUTE s3_stmt; DEALLOCATE PREPARE s3_stmt;

INSERT INTO system_menu(name,permission,type,sort,parent_id,status) SELECT '工作台来源维护','hrm:payroll:requirement:source-edit',3,90,id,0 FROM system_menu WHERE permission='hrm:payroll:requirement:query' AND deleted=0 AND NOT EXISTS(SELECT 1 FROM system_menu y WHERE y.permission='hrm:payroll:requirement:source-edit' AND y.deleted=0) LIMIT 1;

INSERT INTO system_menu(name,permission,type,sort,parent_id,status) SELECT '工作台来源核验','hrm:payroll:requirement:source-verify',3,90,id,0 FROM system_menu WHERE permission='hrm:payroll:requirement:query' AND deleted=0 AND NOT EXISTS(SELECT 1 FROM system_menu y WHERE y.permission='hrm:payroll:requirement:source-verify' AND y.deleted=0) LIMIT 1;

INSERT INTO system_menu(name,permission,type,sort,parent_id,status) SELECT '工作台问题管理','hrm:payroll:requirement:issue-manage',3,90,id,0 FROM system_menu WHERE permission='hrm:payroll:requirement:query' AND deleted=0 AND NOT EXISTS(SELECT 1 FROM system_menu y WHERE y.permission='hrm:payroll:requirement:issue-manage' AND y.deleted=0) LIMIT 1;

INSERT INTO system_menu(name,permission,type,sort,parent_id,status) SELECT '工作台问题处理','hrm:payroll:requirement:issue-handle',3,90,id,0 FROM system_menu WHERE permission='hrm:payroll:requirement:query' AND deleted=0 AND NOT EXISTS(SELECT 1 FROM system_menu y WHERE y.permission='hrm:payroll:requirement:issue-handle' AND y.deleted=0) LIMIT 1;

INSERT INTO system_menu(name,permission,type,sort,parent_id,status) SELECT '工作台问题复核','hrm:payroll:requirement:issue-review',3,90,id,0 FROM system_menu WHERE permission='hrm:payroll:requirement:query' AND deleted=0 AND NOT EXISTS(SELECT 1 FROM system_menu y WHERE y.permission='hrm:payroll:requirement:issue-review' AND y.deleted=0) LIMIT 1;

-- Repair only this stage's newly introduced menu placement, never role membership.
UPDATE system_menu child JOIN system_menu parent ON parent.permission='hrm:payroll:requirement:query' AND parent.deleted=0
SET child.parent_id=parent.id
WHERE child.permission IN ('hrm:payroll:requirement:source-edit','hrm:payroll:requirement:source-verify','hrm:payroll:requirement:issue-manage','hrm:payroll:requirement:issue-handle','hrm:payroll:requirement:issue-review') AND child.deleted=0 AND child.parent_id<>parent.id;
