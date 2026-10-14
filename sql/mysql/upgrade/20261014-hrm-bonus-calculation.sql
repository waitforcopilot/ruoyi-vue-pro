-- Tenant catalogs must synchronize this protected earning source after installation.
INSERT INTO hrm_salary_option_template(code,parent_code,name,remark,system_flag,type,tax_enabled,visible,calculate_enabled)
SELECT 140101,140,'批准年终奖','依据独立复核奖金来源及年度政策计税，不允许人工调整或通用公式覆盖',1,1,0,1,1
WHERE NOT EXISTS (SELECT 1 FROM hrm_salary_option_template WHERE code=140101);
