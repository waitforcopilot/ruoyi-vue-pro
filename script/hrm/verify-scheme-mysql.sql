DELIMITER //
CREATE PROCEDURE assert_scheme_migration()
BEGIN
 DECLARE duplicate_version BOOLEAN DEFAULT FALSE;
 IF (SELECT COUNT(*) FROM system_menu WHERE permission LIKE 'hrm:payroll:scheme:%' AND deleted=0) <> 3 THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Expected three scheme permissions without duplicate seeds';
 END IF;
 IF (SELECT COUNT(*) FROM hrm_payroll_scheme_version WHERE title='保留快照😀' AND tenant_id=1) <> 1 THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Migration modified an existing snapshot';
 END IF;
 BEGIN
  DECLARE CONTINUE HANDLER FOR 1062 SET duplicate_version=TRUE;
  INSERT INTO hrm_payroll_scheme_version(group_id,group_name,title,scheme_version,revision,option_count,source_hash,snapshot_json,captured_at,tenant_id)
  VALUES(1,'保留组','重复版本',1,1,1,SHA2('keep',256),'{}',NOW(),1);
 END;
 IF NOT duplicate_version THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Scheme version uniqueness failed'; END IF;
 INSERT INTO hrm_payroll_scheme_version(group_id,group_name,title,scheme_version,revision,option_count,source_hash,snapshot_json,captured_at,tenant_id)
 VALUES(1,'租户 B 组','租户 B',1,1,1,SHA2('keep',256),'{}',NOW(),999),
       (1,'保留组','另一版本',2,1,1,SHA2('keep',256),'{}',NOW(),1);
 UPDATE hrm_payroll_scheme_version SET snapshot_json=JSON_OBJECT('note',REPEAT('中文😀',20000)) WHERE tenant_id=1 AND scheme_version=1;
 IF NOT (SELECT OCTET_LENGTH(snapshot_json)>65535 AND JSON_VALID(snapshot_json) FROM hrm_payroll_scheme_version WHERE tenant_id=1 AND scheme_version=1) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Unicode MEDIUMTEXT snapshot failed';
 END IF;
END//
DELIMITER ;
START TRANSACTION;
CALL assert_scheme_migration();
ROLLBACK;
DROP PROCEDURE assert_scheme_migration;
