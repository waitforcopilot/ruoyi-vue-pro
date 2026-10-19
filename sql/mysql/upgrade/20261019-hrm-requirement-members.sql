-- S2 additive authorization metadata. No role grants or business approval roots.
SET NAMES utf8mb4;
SET @ddl=IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='hrm_payroll_requirement_project_grant' AND column_name='parent_grant_id')=0,'ALTER TABLE hrm_payroll_requirement_project_grant ADD COLUMN parent_grant_id bigint NULL','SELECT 1');
PREPARE s2_stmt FROM @ddl; EXECUTE s2_stmt; DEALLOCATE PREPARE s2_stmt;
SET @ddl=IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='hrm_payroll_requirement_project_grant' AND column_name='grantor_id')=0,'ALTER TABLE hrm_payroll_requirement_project_grant ADD COLUMN grantor_id bigint NULL','SELECT 1');
PREPARE s2_stmt FROM @ddl; EXECUTE s2_stmt; DEALLOCATE PREPARE s2_stmt;
SET @ddl=IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='hrm_payroll_requirement_project_grant' AND column_name='grant_version')=0,'ALTER TABLE hrm_payroll_requirement_project_grant ADD COLUMN grant_version int NOT NULL DEFAULT 1','SELECT 1');
PREPARE s2_stmt FROM @ddl; EXECUTE s2_stmt; DEALLOCATE PREPARE s2_stmt;
SET @ddl=IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='hrm_payroll_requirement_project_grant' AND column_name='delegable_actions')=0,'ALTER TABLE hrm_payroll_requirement_project_grant ADD COLUMN delegable_actions varchar(200) NULL','SELECT 1');
PREPARE s2_stmt FROM @ddl; EXECUTE s2_stmt; DEALLOCATE PREPARE s2_stmt;
SET @ddl=IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='hrm_payroll_requirement_project_grant' AND column_name='modules_json')=0,'ALTER TABLE hrm_payroll_requirement_project_grant ADD COLUMN modules_json text NULL','SELECT 1');
PREPARE s2_stmt FROM @ddl; EXECUTE s2_stmt; DEALLOCATE PREPARE s2_stmt;
SET @ddl=IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='hrm_payroll_requirement_project_grant' AND column_name='requirement_ids_json')=0,'ALTER TABLE hrm_payroll_requirement_project_grant ADD COLUMN requirement_ids_json text NULL','SELECT 1');
PREPARE s2_stmt FROM @ddl; EXECUTE s2_stmt; DEALLOCATE PREPARE s2_stmt;
SET @ddl=IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='hrm_payroll_requirement_project_grant' AND column_name='full_project')=0,'ALTER TABLE hrm_payroll_requirement_project_grant ADD COLUMN full_project bit(1) NOT NULL DEFAULT 1','SELECT 1');
PREPARE s2_stmt FROM @ddl; EXECUTE s2_stmt; DEALLOCATE PREPARE s2_stmt;
SET @ddl=IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='hrm_payroll_requirement_project_grant' AND column_name='revoked_at')=0,'ALTER TABLE hrm_payroll_requirement_project_grant ADD COLUMN revoked_at datetime NULL','SELECT 1');
PREPARE s2_stmt FROM @ddl; EXECUTE s2_stmt; DEALLOCATE PREPARE s2_stmt;
SET @ddl=IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='hrm_payroll_requirement_project_grant' AND column_name='revoke_reason')=0,'ALTER TABLE hrm_payroll_requirement_project_grant ADD COLUMN revoke_reason varchar(2000) NULL','SELECT 1');
PREPARE s2_stmt FROM @ddl; EXECUTE s2_stmt; DEALLOCATE PREPARE s2_stmt;

-- Convert existing implicit creator draft access to a revocable record without business authority.
INSERT INTO hrm_payroll_requirement_project_grant(project_id,user_id,grantor_id,actions,full_project,grant_version,ceiling_json,valid_from,valid_to,evidence,tenant_id)
SELECT p.id,p.owner_id,p.owner_id,'READ,META,EDIT,ASSIGN,EXPORT',1,2,p.scope_json,DATE(p.create_time),'9999-12-31','PROJECT_CREATOR_ACCESS: preserved technical draft access only; no business confirmation, member or scope authority',p.tenant_id
FROM hrm_payroll_requirement_project p WHERE p.deleted=0 AND p.owner_id IS NOT NULL AND p.project_key<>'LEGACY'
AND NOT EXISTS(SELECT 1 FROM hrm_payroll_requirement_project_grant g WHERE g.project_id=p.id AND g.tenant_id=p.tenant_id AND g.user_id=p.owner_id AND g.evidence LIKE 'PROJECT_CREATOR_ACCESS:%');

-- One-time repair of an earlier S2 candidate: freeze current creator draft bounds.
-- Version 2 also seals UNKNOWN bounds, so a later rerun cannot grant future scope.
-- Only S2-generated active draft grants are touched, never business roots/signatures.
UPDATE hrm_payroll_requirement_project_grant g
JOIN hrm_payroll_requirement_project p ON p.id=g.project_id AND p.tenant_id=g.tenant_id
SET g.ceiling_json=p.scope_json,g.grant_version=2
WHERE g.evidence LIKE 'PROJECT_CREATOR_ACCESS:%' AND g.parent_grant_id IS NULL
AND g.ceiling_json IS NULL AND g.grant_version=1 AND g.revoked_at IS NULL
AND g.deleted=0 AND p.deleted=0 AND p.project_key<>'LEGACY';
