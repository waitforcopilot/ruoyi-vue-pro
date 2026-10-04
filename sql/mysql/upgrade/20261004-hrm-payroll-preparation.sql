-- Read-only preparation overview. Requires the requirements/intake/rule migrations.
-- This menu alone grants no access to the underlying preparation records.
INSERT INTO system_menu(name,permission,type,sort,parent_id,path,icon,component,component_name)
SELECT '薪酬资料准备总览','hrm:payroll:preparation:query',2,89,root.id,'payroll-preparation','ep:data-analysis','hrm/payroll/preparation/index','HrmPayrollPreparation'
FROM system_menu root WHERE root.path='/hrm' AND root.parent_id=0 AND root.deleted=0
AND NOT EXISTS(SELECT 1 FROM system_menu WHERE component='hrm/payroll/preparation/index' AND deleted=0) LIMIT 1;
