DELIMITER //
CREATE PROCEDURE assert_review_migration()
BEGIN
 DECLARE duplicate_key BOOLEAN DEFAULT FALSE;
 IF (SELECT COUNT(*) FROM system_menu WHERE permission IN ('hrm:payroll:trial:review-submit','hrm:payroll:trial:hr-review','hrm:payroll:trial:finance-review','hrm:payroll:trial:freeze','hrm:payroll:trial:unfreeze','hrm:payroll:trial:admin-cancel') AND deleted=0) <> 6 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Expected six distinct review permissions'; END IF;
 IF (SELECT COUNT(*) FROM hrm_payroll_trial_batch WHERE title='保留试算😀' AND tenant_id=1 AND JSON_UNQUOTE(JSON_EXTRACT(configuration_json,'$.net'))='6200.00' AND active_review_id IS NULL AND frozen_run_id IS NULL) <> 1 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Migration modified existing trial'; END IF;
 INSERT INTO hrm_payroll_review_cycle(batch_id,run_id,cycle_version,source_hash,process_instance_id,process_definition_id,started_by,submit_evidence,started_at,hr_reviewer_id,finance_reviewer_id,tenant_id)
 VALUES(1,11,1,REPEAT('a',64),'Native-Process','pinned-deployment',10,'中文复核😀',NOW(),11,12,1);
 BEGIN
  DECLARE CONTINUE HANDLER FOR 1062 SET duplicate_key=TRUE;
  INSERT INTO hrm_payroll_review_cycle(batch_id,run_id,cycle_version,source_hash,process_instance_id,process_definition_id,started_by,submit_evidence,started_at,hr_reviewer_id,finance_reviewer_id,tenant_id)
  VALUES(1,11,1,REPEAT('a',64),'Another-Process','pinned-deployment',10,'dup',NOW(),11,12,1);
 END;
 IF NOT duplicate_key THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Cycle version uniqueness failed'; END IF;
 SET duplicate_key=FALSE;
 BEGIN
  DECLARE CONTINUE HANDLER FOR 1062 SET duplicate_key=TRUE;
  INSERT INTO hrm_payroll_review_cycle(batch_id,run_id,cycle_version,source_hash,process_instance_id,process_definition_id,started_by,submit_evidence,started_at,hr_reviewer_id,finance_reviewer_id,tenant_id)
  VALUES(1,11,2,REPEAT('a',64),'Native-Process','pinned-deployment',10,'dup',NOW(),11,12,1);
 END;
 IF NOT duplicate_key THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Native process uniqueness failed'; END IF;
 INSERT INTO hrm_payroll_review_cycle(batch_id,run_id,cycle_version,source_hash,process_instance_id,process_definition_id,started_by,submit_evidence,started_at,hr_reviewer_id,finance_reviewer_id,tenant_id)
 VALUES(1,11,1,REPEAT('a',64),'Native-Process','pinned-deployment',10,'租户 B',NOW(),11,12,999);
 INSERT INTO hrm_payroll_review_command(batch_id,request_key,actor_id,request_hash,response_json,tenant_id) VALUES(1,'Case-Sensitive',10,REPEAT('a',64),JSON_OBJECT('evidence','中文😀','net','6200.00'),1);
 SET duplicate_key=FALSE;
 BEGIN
  DECLARE CONTINUE HANDLER FOR 1062 SET duplicate_key=TRUE;
  INSERT INTO hrm_payroll_review_command(batch_id,request_key,actor_id,request_hash,response_json,tenant_id) VALUES(1,'Case-Sensitive',10,REPEAT('a',64),'{}',1);
 END;
 IF NOT duplicate_key THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Review request-key uniqueness failed'; END IF;
 INSERT INTO hrm_payroll_review_command(batch_id,request_key,actor_id,request_hash,response_json,tenant_id) VALUES(1,'case-sensitive',10,REPEAT('b',64),'{}',1),(1,'Case-Sensitive',10,REPEAT('a',64),'{}',999);
 IF (SELECT COUNT(*) FROM hrm_payroll_review_command)<>3 OR (SELECT COUNT(*) FROM hrm_payroll_review_cycle)<>2 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Tenant or request-case isolation failed'; END IF;
 IF (SELECT COUNT(*) FROM hrm_payroll_review_cycle WHERE submit_evidence='中文复核😀')<>1 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Review evidence Unicode failed'; END IF;
END//
DELIMITER ;
START TRANSACTION;
CALL assert_review_migration();
ROLLBACK;
DROP PROCEDURE assert_review_migration;
