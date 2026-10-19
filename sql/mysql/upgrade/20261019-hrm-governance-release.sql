SET NAMES utf8mb4;
-- No formal trust anchors or existing role expansion are manufactured by migration.
CREATE TABLE IF NOT EXISTS hrm_payroll_governance_record (
 id bigint NOT NULL AUTO_INCREMENT, project_id bigint NOT NULL, kind varchar(32) NOT NULL,
 object_key varchar(128) NOT NULL, version int NOT NULL, data_json longtext NOT NULL,
 creator varchar(64) DEFAULT '', create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updater varchar(64) DEFAULT '', update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 deleted bit(1) NOT NULL DEFAULT b'0', tenant_id bigint NOT NULL DEFAULT 0,
 PRIMARY KEY(id), UNIQUE KEY uk_gov_object(tenant_id,project_id,kind,object_key,deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
INSERT INTO hrm_payroll_governance_record(project_id,kind,object_key,version,data_json,tenant_id)
 SELECT 0,'LOCK','TENANT',1,'{}',t.id FROM system_tenant t WHERE NOT EXISTS
 (SELECT 1 FROM hrm_payroll_governance_record g WHERE g.tenant_id=t.id AND g.kind='LOCK' AND g.deleted=0);
SET @s5_ddl=IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='hrm_payroll_requirement_project_grant' AND column_name='root_record_id')=0,'ALTER TABLE hrm_payroll_requirement_project_grant ADD COLUMN root_record_id bigint NULL','SELECT 1');
PREPARE s5_stmt FROM @s5_ddl; EXECUTE s5_stmt; DEALLOCATE PREPARE s5_stmt;

SET @s5_ddl=IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='hrm_payroll_requirement_project' AND column_name='archived')=0,'ALTER TABLE hrm_payroll_requirement_project ADD COLUMN archived bit(1) NOT NULL DEFAULT b\'0\'','SELECT 1');
PREPARE s5_stmt FROM @s5_ddl; EXECUTE s5_stmt; DEALLOCATE PREPARE s5_stmt;
INSERT INTO system_menu(name,permission,type,sort,parent_id,status) SELECT '登记业务授权根','hrm:payroll:requirement:root-register',3,92,id,0 FROM system_menu WHERE permission='hrm:payroll:requirement:query' AND deleted=0 AND NOT EXISTS(SELECT 1 FROM system_menu y WHERE y.permission='hrm:payroll:requirement:root-register' AND y.deleted=0) LIMIT 1;
INSERT INTO system_menu(name,permission,type,sort,parent_id,status) SELECT '独立签署业务授权根','hrm:payroll:requirement:root-sign',3,92,id,0 FROM system_menu WHERE permission='hrm:payroll:requirement:query' AND deleted=0 AND NOT EXISTS(SELECT 1 FROM system_menu y WHERE y.permission='hrm:payroll:requirement:root-sign' AND y.deleted=0) LIMIT 1;
INSERT INTO system_menu(name,permission,type,sort,parent_id,status) SELECT '起草业务决议','hrm:payroll:requirement:decision-edit',3,92,id,0 FROM system_menu WHERE permission='hrm:payroll:requirement:query' AND deleted=0 AND NOT EXISTS(SELECT 1 FROM system_menu y WHERE y.permission='hrm:payroll:requirement:decision-edit' AND y.deleted=0) LIMIT 1;
INSERT INTO system_menu(name,permission,type,sort,parent_id,status) SELECT '批准业务决议','hrm:payroll:requirement:decision-approve',3,92,id,0 FROM system_menu WHERE permission='hrm:payroll:requirement:query' AND deleted=0 AND NOT EXISTS(SELECT 1 FROM system_menu y WHERE y.permission='hrm:payroll:requirement:decision-approve' AND y.deleted=0) LIMIT 1;
INSERT INTO system_menu(name,permission,type,sort,parent_id,status) SELECT '读取业务决议','hrm:payroll:requirement:decision-read',3,92,id,0 FROM system_menu WHERE permission='hrm:payroll:requirement:query' AND deleted=0 AND NOT EXISTS(SELECT 1 FROM system_menu y WHERE y.permission='hrm:payroll:requirement:decision-read' AND y.deleted=0) LIMIT 1;
INSERT INTO system_menu(name,permission,type,sort,parent_id,status) SELECT '读取授权依据','hrm:payroll:requirement:grant-read',3,92,id,0 FROM system_menu WHERE permission='hrm:payroll:requirement:query' AND deleted=0 AND NOT EXISTS(SELECT 1 FROM system_menu y WHERE y.permission='hrm:payroll:requirement:grant-read' AND y.deleted=0) LIMIT 1;
INSERT INTO system_menu(name,permission,type,sort,parent_id,status) SELECT '发布需求基线','hrm:payroll:requirement:baseline-publish',3,92,id,0 FROM system_menu WHERE permission='hrm:payroll:requirement:query' AND deleted=0 AND NOT EXISTS(SELECT 1 FROM system_menu y WHERE y.permission='hrm:payroll:requirement:baseline-publish' AND y.deleted=0) LIMIT 1;

ALTER TABLE hrm_payroll_requirement_project_grant MODIFY COLUMN delegable_actions varchar(1000) NULL;

ALTER TABLE hrm_payroll_requirement_history MODIFY COLUMN snapshot longtext NOT NULL;
ALTER TABLE hrm_payroll_requirement_project_event MODIFY COLUMN snapshot longtext NOT NULL;

SET @s5_ddl=IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='hrm_payroll_requirement_project_grant' AND column_name='authority_facts_json')=0,'ALTER TABLE hrm_payroll_requirement_project_grant ADD COLUMN authority_facts_json longtext NULL','SELECT 1');
PREPARE s5_stmt FROM @s5_ddl; EXECUTE s5_stmt; DEALLOCATE PREPARE s5_stmt;
