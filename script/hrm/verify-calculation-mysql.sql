DELIMITER //
CREATE PROCEDURE assert_calculation_migration()
BEGIN
 DECLARE duplicate_version BOOLEAN DEFAULT FALSE;
 IF (SELECT COUNT(*) FROM system_menu WHERE permission LIKE 'hrm:payroll:calculation:%' AND deleted=0) <> 3 THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Expected three calculation permissions without duplicate seeds';
 END IF;
 IF (SELECT COUNT(*) FROM hrm_payroll_calculation_definition WHERE title='保留规则😀' AND tenant_id=1
     AND JSON_UNQUOTE(JSON_EXTRACT(program_json,'$.explicitZero'))='0.00' AND JSON_EXTRACT(program_json,'$.scale')=0) <> 1 THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Migration modified retained definition or precision';
 END IF;
 BEGIN
  DECLARE CONTINUE HANDLER FOR 1062 SET duplicate_version=TRUE;
  INSERT INTO hrm_payroll_calculation_definition(code,title,scope_code,definition_version,schema_version,revision,program_json,input_count,item_count,case_count,tenant_id)
  SELECT code,'重复版本',scope_code,definition_version,schema_version,revision,program_json,input_count,item_count,case_count,tenant_id FROM hrm_payroll_calculation_definition LIMIT 1;
 END;
 IF NOT duplicate_version THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Calculation version uniqueness failed'; END IF;
 INSERT INTO hrm_payroll_calculation_definition(code,title,scope_code,definition_version,schema_version,revision,program_json,input_count,item_count,case_count,tenant_id)
 SELECT code,'租户 B',scope_code,1,1,1,program_json,input_count,item_count,case_count,999 FROM hrm_payroll_calculation_definition WHERE tenant_id=1 LIMIT 1;
 INSERT INTO hrm_payroll_calculation_definition(code,title,scope_code,definition_version,schema_version,revision,program_json,input_count,item_count,case_count,tenant_id)
 SELECT code,'第二版本',scope_code,2,1,1,program_json,input_count,item_count,case_count,1 FROM hrm_payroll_calculation_definition WHERE tenant_id=1 LIMIT 1;
 UPDATE hrm_payroll_calculation_definition SET reference=REPEAT('合成依据😀',1000),
  program_json=JSON_OBJECT('note','中文😀','amount','999999999999999999999999.12345678','negative','-0.01','scale',0) WHERE definition_version=1 AND tenant_id=1;
 IF NOT (SELECT JSON_VALID(program_json) AND LOCATE('😀',reference)>0
   AND JSON_UNQUOTE(JSON_EXTRACT(program_json,'$.amount'))='999999999999999999999999.12345678'
   AND JSON_UNQUOTE(JSON_EXTRACT(program_json,'$.negative'))='-0.01'
   FROM hrm_payroll_calculation_definition WHERE definition_version=1 AND tenant_id=1 LIMIT 1) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Program decimal text or Unicode preservation failed';
 END IF;
END//
DELIMITER ;
START TRANSACTION;
CALL assert_calculation_migration();
ROLLBACK;
DROP PROCEDURE assert_calculation_migration;
