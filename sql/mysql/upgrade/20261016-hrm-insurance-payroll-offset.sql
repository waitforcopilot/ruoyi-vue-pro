SET NAMES utf8mb4;
INSERT INTO hrm_salary_option_template(code,parent_code,name,remark,system_flag,type,tax_enabled,visible,calculate_enabled) SELECT 150902,150,'批准缴费差额工资退还','原年度正式扣除更正后仅调整本批次税后净额，成功工资结算后核销',1,1,0,1,1 WHERE NOT EXISTS(SELECT 1 FROM hrm_salary_option_template WHERE code=150902);
INSERT INTO hrm_salary_option_template(code,parent_code,name,remark,system_flag,type,tax_enabled,visible,calculate_enabled) SELECT 160902,160,'批准缴费差额工资补扣','只结算已批准个人缴费差额，不重复扣减本期计税社保',1,2,0,1,1 WHERE NOT EXISTS(SELECT 1 FROM hrm_salary_option_template WHERE code=160902);
UPDATE system_menu SET name='工资收退差额查询' WHERE permission='hrm:payroll:offset:query' AND name='工资退补税查询';
UPDATE system_menu SET name='工资收退差额分配' WHERE permission='hrm:payroll:offset:update' AND name='工资退补税分配';
UPDATE system_menu SET name='工资收退差额复核' WHERE permission='hrm:payroll:offset:review' AND name='工资退补税复核';
