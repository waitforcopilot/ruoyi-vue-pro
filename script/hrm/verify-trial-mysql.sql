DELIMITER //
CREATE PROCEDURE assert_trial_migration()
BEGIN
 DECLARE duplicate_key BOOLEAN DEFAULT FALSE;
 IF (SELECT COUNT(*) FROM system_menu WHERE permission LIKE 'hrm:payroll:trial:%' AND deleted=0) <> 3 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Expected three trial permissions without duplicates'; END IF;
 IF (SELECT COUNT(*) FROM hrm_payroll_trial_batch WHERE title='保留试算😀' AND tenant_id=1 AND JSON_UNQUOTE(JSON_EXTRACT(configuration_json,'$.net'))='6200.00') <> 1 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Migration modified retained batch'; END IF;
 BEGIN
  DECLARE CONTINUE HANDLER FOR 1062 SET duplicate_key=TRUE;
  INSERT INTO hrm_payroll_trial_batch(code,title,entity_code,entity_name,period_type,period_start,period_end,definition_id,revision,person_count,configuration_json,tenant_id)
  SELECT code,title,entity_code,entity_name,period_type,period_start,period_end,definition_id,revision,person_count,configuration_json,tenant_id FROM hrm_payroll_trial_batch LIMIT 1;
 END;
 IF NOT duplicate_key THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Batch uniqueness failed'; END IF;
 INSERT INTO hrm_payroll_trial_batch(code,title,entity_code,entity_name,period_type,period_start,period_end,definition_id,revision,person_count,configuration_json,tenant_id)
 SELECT code,'租户 B',entity_code,entity_name,period_type,period_start,period_end,definition_id,revision,person_count,configuration_json,999 FROM hrm_payroll_trial_batch WHERE tenant_id=1 LIMIT 1;
 INSERT INTO hrm_payroll_trial_run(batch_id,run_version,request_key,expected_revision,source_hash,result_json,included_count,excluded_count,executed_by,executed_at,tenant_id)
 VALUES(1,1,'Case-Sensitive',1,REPEAT('a',64),JSON_OBJECT('net','6200.00','unicode','中文😀'),1,0,1,NOW(),1);
 SET duplicate_key=FALSE;
 BEGIN
  DECLARE CONTINUE HANDLER FOR 1062 SET duplicate_key=TRUE;
  INSERT INTO hrm_payroll_trial_run(batch_id,run_version,request_key,expected_revision,source_hash,result_json,included_count,excluded_count,executed_by,executed_at,tenant_id)
  VALUES(1,2,'Case-Sensitive',1,REPEAT('a',64),'{}',1,0,1,NOW(),1);
 END;
 IF NOT duplicate_key THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Request idempotency uniqueness failed'; END IF;
 INSERT INTO hrm_payroll_trial_run(batch_id,run_version,request_key,expected_revision,source_hash,result_json,included_count,excluded_count,executed_by,executed_at,tenant_id)
 VALUES(1,2,'case-sensitive',2,REPEAT('b',64),'{}',1,0,1,NOW(),1),(1,1,'Case-Sensitive',1,REPEAT('a',64),'{}',1,0,1,NOW(),999);
 SET duplicate_key=FALSE;
 BEGIN
  DECLARE CONTINUE HANDLER FOR 1062 SET duplicate_key=TRUE;
  INSERT INTO hrm_payroll_trial_run(batch_id,run_version,request_key,expected_revision,source_hash,result_json,included_count,excluded_count,executed_by,executed_at,tenant_id)
  VALUES(1,2,'different-key',2,REPEAT('b',64),'{}',1,0,1,NOW(),1);
 END;
 IF NOT duplicate_key THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Run version uniqueness failed'; END IF;
 IF NOT (SELECT JSON_VALID(result_json) AND LOCATE('😀',result_json)>0 AND JSON_UNQUOTE(JSON_EXTRACT(result_json,'$.net'))='6200.00' FROM hrm_payroll_trial_run WHERE run_version=1 AND tenant_id=1) THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Run decimal or Unicode was changed'; END IF;
 INSERT INTO hrm_payroll_trial_person(batch_id,employee_id,employee_fingerprint,tenant_id) VALUES(1,101,REPEAT('a',64),1),(1,101,REPEAT('b',64),1),(1,101,REPEAT('a',64),999);
 IF (SELECT COUNT(*) FROM hrm_payroll_trial_person)<>3 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Historical identity footprint failed'; END IF;
END//
DELIMITER ;
START TRANSACTION;
CALL assert_trial_migration();
ROLLBACK;
DROP PROCEDURE assert_trial_migration;
