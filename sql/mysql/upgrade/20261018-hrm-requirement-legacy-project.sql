SET NAMES utf8mb4;
-- F-01a: additive legacy ownership only. No project scope or authorization is implied.
CREATE TABLE IF NOT EXISTS `hrm_payroll_requirement_project` (
 `id` bigint NOT NULL AUTO_INCREMENT,
 `project_key` varchar(64) NOT NULL, `name` varchar(200) NOT NULL,
 `metadata_version` int NOT NULL DEFAULT 1, `project_scope_version` int DEFAULT NULL,
 `creator` varchar(64) DEFAULT '', `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `updater` varchar(64) DEFAULT '', `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `deleted` bit NOT NULL DEFAULT FALSE, `tenant_id` bigint NOT NULL DEFAULT 0,
 PRIMARY KEY (`id`), UNIQUE KEY `uk_requirement_project_tenant_key` (`tenant_id`,`project_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
SET @hrm_project_column_sql = IF(
 (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE()
  AND table_name='hrm_payroll_requirement' AND column_name='project_id')=0,
 'ALTER TABLE hrm_payroll_requirement ADD COLUMN project_id bigint DEFAULT NULL', 'SELECT 1');
PREPARE hrm_project_column_stmt FROM @hrm_project_column_sql;
EXECUTE hrm_project_column_stmt;
DEALLOCATE PREPARE hrm_project_column_stmt;
-- Do not change any old IDs, codes, versions, confirmations, timestamps or history JSON.
INSERT INTO hrm_payroll_requirement_project (project_key,name,metadata_version,tenant_id)
SELECT 'LEGACY','历史需求迁移项目',1,old.tenant_id
FROM (SELECT DISTINCT tenant_id FROM hrm_payroll_requirement) old
WHERE NOT EXISTS (SELECT 1 FROM hrm_payroll_requirement_project p
 WHERE p.tenant_id=old.tenant_id AND p.project_key='LEGACY');
UPDATE hrm_payroll_requirement r
JOIN hrm_payroll_requirement_project p ON p.tenant_id=r.tenant_id AND p.project_key='LEGACY'
SET r.project_id=p.id WHERE r.project_id IS NULL;
-- Keep the old tenant/code uniqueness until multi-project APIs and authorization are implemented.
