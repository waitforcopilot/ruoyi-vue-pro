DELIMITER //
CREATE PROCEDURE assert_rules_migration()
BEGIN
 DECLARE duplicate_version BOOLEAN DEFAULT FALSE;
 IF (SELECT COUNT(*) FROM system_menu WHERE permission LIKE 'hrm:payroll:rule:%' AND deleted=0) <> 3 THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Expected three rule permissions without duplicate seeds';
 END IF;
 IF (SELECT COUNT(*) FROM hrm_payroll_rule WHERE title='保留草稿' AND tenant_id=1) <> 1 THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Migration changed existing rule data';
 END IF;
 BEGIN
  DECLARE CONTINUE HANDLER FOR 1062 SET duplicate_version=TRUE;
  INSERT INTO hrm_payroll_rule(code,title,category,rule_version,revision,parameters_json,cases_json,parameter_count,case_count,tenant_id)
   VALUES('RULE-CUSTOM-MIGRATION','重复版本','OTHER',1,1,'[]','[]',0,0,1);
 END;
 IF NOT duplicate_version THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Rule version uniqueness failed'; END IF;
 INSERT INTO hrm_payroll_rule(code,title,category,rule_version,revision,parameters_json,cases_json,parameter_count,case_count,tenant_id)
  VALUES('RULE-CUSTOM-MIGRATION','第二租户','OTHER',1,1,'[]','[]',0,0,999);
 UPDATE hrm_payroll_rule SET cases_json=JSON_ARRAY(JSON_OBJECT('note',REPEAT('中文😀',20000))) WHERE tenant_id=1;
 IF NOT (SELECT OCTET_LENGTH(cases_json)>65535 AND JSON_VALID(cases_json) FROM hrm_payroll_rule WHERE tenant_id=1) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Unicode MEDIUMTEXT case payload failed';
 END IF;
END//
DELIMITER ;
START TRANSACTION;
CALL assert_rules_migration();
ROLLBACK;
DROP PROCEDURE assert_rules_migration;
