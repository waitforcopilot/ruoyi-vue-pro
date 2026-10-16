SET NAMES utf8mb4;
SET @parent=(SELECT id FROM system_menu WHERE path='/hrm-payroll' AND deleted=0 LIMIT 1);
INSERT INTO system_menu(name,permission,type,parent_id,path,icon,component,component_name) SELECT '薪酬总览','hrm:payroll:overview:query',2,@parent,'overview','ep:data-board','hrm/payroll/overview/index','HrmPayrollOverview' WHERE @parent IS NOT NULL AND NOT EXISTS(SELECT 1 FROM system_menu WHERE permission='hrm:payroll:overview:query' AND deleted=0);
