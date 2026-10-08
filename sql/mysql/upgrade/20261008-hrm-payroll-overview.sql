-- Read-only overview entry. Business roles remain explicitly authorized by administrators.
INSERT INTO system_menu(name,permission,type,sort,parent_id,path,icon,component,component_name)
SELECT '薪酬批次概览','hrm:payroll:overview:query',2,100,m.id,'payroll-overview','ep:data-analysis','hrm/payroll/overview/index','HrmPayrollOverview'
FROM system_menu m WHERE m.path='/hrm' AND m.deleted=0
AND NOT EXISTS(SELECT 1 FROM system_menu WHERE permission='hrm:payroll:overview:query' AND deleted=0) LIMIT 1;
