DELIMITER //
CREATE PROCEDURE assert_intake_migration()
BEGIN
 IF (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name LIKE 'hrm_payroll_%') <> 6 THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Expected six payroll collection/intake tables';
 END IF;
 IF (SELECT COUNT(*) FROM system_menu WHERE permission LIKE 'hrm:payroll:intake:%' AND deleted=0) <> 4 THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Expected four intake permissions without duplicates';
 END IF;
 IF (SELECT COUNT(*) FROM hrm_payroll_source_contract WHERE title='保留草稿' AND tenant_id=1) <> 1 THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Migration changed existing contracts';
 END IF;
END//
DELIMITER ;
CALL assert_intake_migration();
DROP PROCEDURE assert_intake_migration;

DELIMITER //
CREATE PROCEDURE assert_intake_payload()
BEGIN
 DECLARE duplicate_version BOOLEAN DEFAULT FALSE;
 DECLARE duplicate_batch BOOLEAN DEFAULT FALSE;
 BEGIN
  DECLARE CONTINUE HANDLER FOR 1062 SET duplicate_version=TRUE;
  INSERT INTO hrm_payroll_source_contract(source_id,source_code,source_name,contract_version,title,schema_json,field_count,tenant_id)
   VALUES(1,'DS-TEST','隔离来源',1,'重复版本','{}',1,1);
 END;
 IF NOT duplicate_version THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Source version uniqueness failed'; END IF;
 INSERT INTO hrm_payroll_source_contract(source_id,source_code,source_name,contract_version,title,schema_json,field_count,tenant_id)
  VALUES(1,'DS-TEST','第二租户',1,'独立版本','{}',1,999);
 INSERT INTO hrm_payroll_import_batch(contract_id,source_code,contract_version,file_name,file_hash,idempotency_key,declared_scope,period_start,period_end,row_count,valid_count,error_count,status,created_by,snapshot,tenant_id)
  VALUES(1,'DS-TEST',1,'测试.csv',REPEAT('a',64),REPEAT('b',64),'测试主体','2026-10-01','2026-10-31',0,0,1,1,10,JSON_OBJECT('note',REPEAT('中文😀',20000)),1);
 IF NOT (SELECT OCTET_LENGTH(snapshot)>65535 AND JSON_VALID(snapshot) FROM hrm_payroll_import_batch WHERE id=LAST_INSERT_ID()) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Large Unicode batch snapshot failed';
 END IF;
 BEGIN
  DECLARE CONTINUE HANDLER FOR 1062 SET duplicate_batch=TRUE;
  INSERT INTO hrm_payroll_import_batch(contract_id,source_code,contract_version,file_name,file_hash,idempotency_key,declared_scope,period_start,period_end,row_count,valid_count,error_count,status,created_by,snapshot,tenant_id)
   VALUES(1,'DS-TEST',1,'重复.csv',REPEAT('a',64),REPEAT('b',64),'测试主体','2026-10-01','2026-10-31',0,0,1,1,10,'{}',1);
 END;
 IF NOT duplicate_batch THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Batch idempotency uniqueness failed'; END IF;
 INSERT INTO hrm_payroll_import_batch(contract_id,source_code,contract_version,file_name,file_hash,idempotency_key,declared_scope,period_start,period_end,row_count,valid_count,error_count,status,created_by,snapshot,tenant_id)
  VALUES(1,'DS-TEST',1,'另一提交人.csv',REPEAT('a',64),REPEAT('b',64),'测试主体','2026-10-01','2026-10-31',0,0,1,1,11,'{}',1);
 IF (SELECT COUNT(*) FROM hrm_payroll_import_batch) <> 2 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Owner uniqueness failed'; END IF;
END//
DELIMITER ;
START TRANSACTION;
CALL assert_intake_payload();
ROLLBACK;
DROP PROCEDURE assert_intake_payload;
