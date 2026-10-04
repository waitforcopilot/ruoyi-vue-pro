DELIMITER //
CREATE PROCEDURE assert_identity_migration()
BEGIN
 DECLARE duplicate_version BOOLEAN DEFAULT FALSE;
 IF (SELECT COUNT(*) FROM system_menu WHERE permission LIKE 'hrm:payroll:identity:%' AND deleted=0) <> 3 THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Expected three identity permissions without duplicate seeds';
 END IF;
 IF (SELECT COUNT(*) FROM hrm_payroll_employee_mapping WHERE snapshot_name='保留快照😀' AND tenant_id=1) <> 1 THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Migration modified an existing snapshot';
 END IF;
 BEGIN
  DECLARE CONTINUE HANDLER FOR 1062 SET duplicate_version=TRUE;
  INSERT INTO hrm_payroll_employee_mapping(identity_key,source_id,source_code,namespace,external_code,mapping_version,revision,employee_id,snapshot_captured_at,employee_fingerprint,tenant_id)
  VALUES(SHA2('keep',256),1,'DS01','QA','001',1,1,100,NOW(),SHA2('person',256),1);
 END;
 IF NOT duplicate_version THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Identity version uniqueness failed'; END IF;
 INSERT INTO hrm_payroll_employee_mapping(identity_key,source_id,source_code,namespace,external_code,mapping_version,revision,employee_id,snapshot_captured_at,employee_fingerprint,tenant_id)
 VALUES(SHA2('keep',256),1,'DS01','QA','001',1,1,100,NOW(),SHA2('person',256),999),
 (SHA2('Aa',256),1,'DS01','QA','Aa',1,1,100,NOW(),SHA2('person',256),1),
 (SHA2('aa',256),1,'DS01','QA','aa',1,1,100,NOW(),SHA2('person',256),1),
 (SHA2('1',256),1,'DS01','QA','1',1,1,100,NOW(),SHA2('person',256),1);
 IF (SELECT COUNT(*) FROM hrm_payroll_employee_mapping WHERE tenant_id=1 AND external_code='Aa') <> 1 OR
    (SELECT COUNT(*) FROM hrm_payroll_employee_mapping WHERE tenant_id=1 AND external_code='001') <> 1 THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Case sensitive or leading zero lookup failed';
 END IF;
 INSERT INTO hrm_payroll_employee_mapping(identity_key,source_id,source_code,namespace,external_code,mapping_version,revision,employee_id,snapshot_captured_at,employee_fingerprint,tenant_id)
 VALUES(SHA2('keep',256),1,'DS01','QA','001',2,1,100,NOW(),SHA2('person',256),1);
END//
DELIMITER ;
START TRANSACTION;
CALL assert_identity_migration();
ROLLBACK;
DROP PROCEDURE assert_identity_migration;
