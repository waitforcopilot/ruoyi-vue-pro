DELIMITER //
CREATE PROCEDURE assert_insurance_policy_migration()
BEGIN
 DECLARE duplicate_version BOOLEAN DEFAULT FALSE;
 IF (SELECT COUNT(*) FROM system_menu WHERE permission LIKE 'hrm:payroll:insurance-policy:%' AND deleted=0) <> 3 THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Expected three policy permissions without duplicate seeds';
 END IF;
 IF (SELECT COUNT(*) FROM hrm_payroll_insurance_policy WHERE title='保留政策😀' AND tenant_id=1) <> 1 THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Migration modified an existing policy';
 END IF;
 BEGIN
  DECLARE CONTINUE HANDLER FOR 1062 SET duplicate_version=TRUE;
  INSERT INTO hrm_payroll_insurance_policy(identity_key,city_area_id,city_name,scope_code,project_type,project_code,project_name,title,policy_version,revision,config_schema_version,config_json,tenant_id)
  SELECT identity_key,city_area_id,city_name,scope_code,project_type,project_code,project_name,'重复版本',policy_version,revision,config_schema_version,config_json,tenant_id FROM hrm_payroll_insurance_policy LIMIT 1;
 END;
 IF NOT duplicate_version THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Policy version uniqueness failed'; END IF;
 INSERT INTO hrm_payroll_insurance_policy(identity_key,city_area_id,city_name,scope_code,project_type,project_code,project_name,title,policy_version,revision,config_schema_version,config_json,tenant_id)
 SELECT identity_key,city_area_id,city_name,scope_code,project_type,project_code,project_name,'租户 B',1,1,1,'{}',999 FROM hrm_payroll_insurance_policy WHERE tenant_id=1 LIMIT 1;
 UPDATE hrm_payroll_insurance_policy SET reference=REPEAT('合成政策😀',1000),config_json=JSON_OBJECT('note','中文😀','explicitZero','0.00') WHERE tenant_id=1;
 IF NOT (SELECT JSON_VALID(config_json) AND LOCATE('😀',reference)>0 FROM hrm_payroll_insurance_policy WHERE tenant_id=1 LIMIT 1) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Policy JSON Unicode or reference preservation failed';
 END IF;
END//
DELIMITER ;
START TRANSACTION;
CALL assert_insurance_policy_migration();
ROLLBACK;
DROP PROCEDURE assert_insurance_policy_migration;
