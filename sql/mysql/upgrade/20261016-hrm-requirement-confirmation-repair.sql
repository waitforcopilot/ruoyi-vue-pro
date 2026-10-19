SET NAMES utf8mb4;
-- Reopen invalid legacy confirmations with an explicit system-authored history entry.
-- A currently disabled account does not, by itself, revoke a historical signature.
-- Once structured, version-bound confirmation tuples exist, legacy flat-field
-- heuristics must not reopen those signatures (including lawful source N/A).
-- Dynamic SQL also permits the original installation before S3 adds the column.
SET @hrm_legacy_confirmation_filter=IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='hrm_payroll_requirement' AND column_name='confirmed_tuple_json')=1,' AND r.confirmed_tuple_json IS NULL','');
START TRANSACTION;
SET @hrm_legacy_confirmation_sql=CONCAT('INSERT INTO hrm_payroll_requirement_history(requirement_id,version,actor_id,snapshot,tenant_id,creator,updater)
SELECT r.id,r.version+1,0,JSON_OBJECT(
 ''id'',r.id,''code'',r.code,''module'',r.module,''description'',r.description,''fieldMapping'',r.field_mapping,
 ''sourceSystem'',r.source_system,''sourceOwnerId'',r.source_owner_id,''reviewerId'',r.reviewer_id,
 ''priority'',r.priority,''status'',''REVIEWING'',''readiness'',r.readiness,''acceptance'',r.acceptance,''evidence'',r.evidence,
 ''version'',r.version+1,''confirmedBy'',NULL,''confirmedAt'',NULL,''previousConfirmedBy'',r.confirmed_by,
 ''previousConfirmedAt'',r.confirmed_at,''previousStatus'',r.status,
 ''validationReason'',''原确认缺字段映射/明确不适用理由，或责任人不存在，需重新评审''),
 r.tenant_id,''hrm-audit-f04'',''hrm-audit-f04''
FROM hrm_payroll_requirement r LEFT JOIN system_users owner ON owner.id=r.source_owner_id AND owner.tenant_id=r.tenant_id LEFT JOIN system_users reviewer ON reviewer.id=r.reviewer_id AND reviewer.tenant_id=r.tenant_id WHERE r.deleted=0 AND r.status=''CONFIRMED'' AND (r.field_mapping IS NULL OR TRIM(r.field_mapping)='''' OR UPPER(TRIM(r.field_mapping)) IN (''N/A'',''NA'',''不适用'') OR owner.id IS NULL OR reviewer.id IS NULL)',@hrm_legacy_confirmation_filter);
PREPARE hrm_legacy_stmt FROM @hrm_legacy_confirmation_sql; EXECUTE hrm_legacy_stmt; DEALLOCATE PREPARE hrm_legacy_stmt;
SET @hrm_legacy_confirmation_sql=CONCAT('UPDATE hrm_payroll_requirement r LEFT JOIN system_users owner ON owner.id=r.source_owner_id AND owner.tenant_id=r.tenant_id LEFT JOIN system_users reviewer ON reviewer.id=r.reviewer_id AND reviewer.tenant_id=r.tenant_id SET r.status=''REVIEWING'',r.version=r.version+1,r.confirmed_by=NULL,r.confirmed_at=NULL,
 r.updater=''hrm-audit-f04'',r.update_time=CURRENT_TIMESTAMP
WHERE r.deleted=0 AND r.status=''CONFIRMED'' AND (r.field_mapping IS NULL OR TRIM(r.field_mapping)='''' OR UPPER(TRIM(r.field_mapping)) IN (''N/A'',''NA'',''不适用'') OR owner.id IS NULL OR reviewer.id IS NULL)',@hrm_legacy_confirmation_filter);
PREPARE hrm_legacy_stmt FROM @hrm_legacy_confirmation_sql; EXECUTE hrm_legacy_stmt; DEALLOCATE PREPARE hrm_legacy_stmt;
COMMIT;
