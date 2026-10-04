DELIMITER //
CREATE PROCEDURE assert_collection_migration()
BEGIN
 IF (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name LIKE 'hrm_payroll_%') <> 4 THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Expected four collection tables';
 END IF;
 IF (SELECT COUNT(*) FROM system_menu WHERE permission LIKE 'hrm:payroll:%' AND deleted=0) <> 7 THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Expected seven permissions without duplicate seeds';
 END IF;
 IF (SELECT COUNT(*) FROM hrm_payroll_requirement WHERE code='CUSTOM-MIGRATION' AND tenant_id=1) <> 1 THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Migration changed existing collection data';
 END IF;
END//
DELIMITER ;
CALL assert_collection_migration();
DROP PROCEDURE assert_collection_migration;
DELIMITER //
CREATE PROCEDURE assert_collection_payload(IN baseline_id BIGINT)
BEGIN
 IF NOT (SELECT OCTET_LENGTH(snapshot)>65535 AND JSON_VALID(snapshot) FROM hrm_payroll_baseline WHERE id=baseline_id) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Large Unicode baseline snapshot failed';
 END IF;
 IF (SELECT COUNT(*) FROM hrm_payroll_requirement WHERE code='CUSTOM-MIGRATION') <> 2 THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Tenant-scoped unique codes failed';
 END IF;
END//
DELIMITER ;
START TRANSACTION;
INSERT INTO hrm_payroll_baseline(requirement_count,source_count,snapshot,tenant_id)
VALUES (46,12,JSON_OBJECT('note',REPEAT('中文验收记录😀',20000)),1);
SET @baseline_id=LAST_INSERT_ID();
SELECT (OCTET_LENGTH(snapshot)>65535 AND JSON_VALID(snapshot)) AS large_snapshot_ok FROM hrm_payroll_baseline WHERE id=@baseline_id;
INSERT INTO hrm_payroll_requirement(code,module_code,title,source_codes,tenant_id) VALUES ('CUSTOM-MIGRATION','req','第二租户独立记录','["DS-01"]',999);
SELECT COUNT(*)=2 AS tenant_code_isolation_ok FROM hrm_payroll_requirement WHERE code='CUSTOM-MIGRATION';
CALL assert_collection_payload(@baseline_id);
ROLLBACK;
DROP PROCEDURE assert_collection_payload;
