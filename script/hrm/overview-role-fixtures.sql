-- Explicit synthetic QA roles only; execute solely in the isolated collection verification database.
INSERT INTO system_role_menu(role_id,menu_id,tenant_id)
SELECT r.id,m.id,1 FROM system_role r CROSS JOIN system_menu m
WHERE r.id BETWEEN 984000001 AND 984000012 AND r.tenant_id=1 AND r.deleted=0
AND m.permission='hrm:payroll:overview:query' AND m.deleted=0
AND NOT EXISTS(SELECT 1 FROM system_role_menu rm WHERE rm.role_id=r.id AND rm.menu_id=m.id AND rm.tenant_id=1 AND rm.deleted=0);
