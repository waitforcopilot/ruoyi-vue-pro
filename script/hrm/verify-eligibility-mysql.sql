DELIMITER //
CREATE PROCEDURE assert_eligibility_migration()
BEGIN
 DECLARE duplicate_version BOOLEAN DEFAULT FALSE;
 IF (SELECT COUNT(*) FROM system_menu WHERE permission LIKE 'hrm:payroll:eligibility:%' AND deleted=0) <> 3 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Expected exactly three permissions'; END IF;
 IF (SELECT COUNT(*) FROM hrm_payroll_employee_eligibility WHERE entity_name='保留主体😀' AND employee_fingerprint=REPEAT('a',64) AND tenant_id=1) <> 1 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Migration changed retained snapshot'; END IF;
 BEGIN
  DECLARE CONTINUE HANDLER FOR 1062 SET duplicate_version=TRUE;
  INSERT INTO hrm_payroll_employee_eligibility(entity_code,entity_name,employee_id,eligibility_version,revision,snapshot_captured_at,employee_fingerprint,tenant_id)
  SELECT entity_code,'重复版本',employee_id,1,1,NOW(),employee_fingerprint,1 FROM hrm_payroll_employee_eligibility LIMIT 1;
 END;
 IF NOT duplicate_version THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Version uniqueness failed'; END IF;
 INSERT INTO hrm_payroll_employee_eligibility(entity_code,entity_name,employee_id,eligibility_version,revision,snapshot_captured_at,employee_fingerprint,tenant_id)
 SELECT entity_code,'租户 B',employee_id,1,1,NOW(),employee_fingerprint,999 FROM hrm_payroll_employee_eligibility WHERE tenant_id=1 LIMIT 1;
 INSERT INTO hrm_payroll_employee_eligibility(entity_code,entity_name,employee_id,eligibility_version,revision,snapshot_captured_at,employee_fingerprint,tenant_id)
 SELECT entity_code,'第二版本',employee_id,2,1,NOW(),employee_fingerprint,1 FROM hrm_payroll_employee_eligibility WHERE tenant_id=1 LIMIT 1;
 UPDATE hrm_payroll_employee_eligibility SET reason='排除😀',qualification='EXCLUDED',effective_from='2026-10-01',effective_to='2026-10-31' WHERE tenant_id=1 AND eligibility_version=2;
 IF (SELECT COUNT(*) FROM hrm_payroll_employee_eligibility) <> 3 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Tenant/version retention failed'; END IF;
END//
DELIMITER ;
START TRANSACTION;
CALL assert_eligibility_migration();
ROLLBACK;
DROP PROCEDURE assert_eligibility_migration;
