-- Synthetic QA only; apply after review-role-fixtures/overview-role-fixtures in the isolated QA DB.
-- Add read access to configured versions, not scheme maintenance/review or business defaults.
INSERT INTO system_role_menu(role_id,menu_id,creator,updater,tenant_id)
SELECT r.id,m.id,'binding-verification','binding-verification',1
FROM system_role r JOIN system_menu m ON m.permission IN
 ('hrm:payroll:scheme:query','hrm:salary:group:query','hrm:salary:option:query','hrm:salary:tax-rule:query')
WHERE r.tenant_id=1 AND r.id IN (984000001,984000002,984000003,984000008,984000009,984000011)
 AND r.deleted=0 AND m.deleted=0
 AND NOT EXISTS(SELECT 1 FROM system_role_menu rm WHERE rm.role_id=r.id AND rm.menu_id=m.id AND rm.tenant_id=1 AND rm.deleted=0);
-- Negative identity has HR/core permissions and three policy reads, but cannot read tax configuration.
INSERT INTO system_role_menu(role_id,menu_id,creator,updater,tenant_id)
SELECT 984000007,m.id,'binding-verification','binding-verification',1 FROM system_menu m
WHERE m.permission IN ('hrm:payroll:scheme:query','hrm:salary:group:query','hrm:salary:option:query') AND m.deleted=0
 AND EXISTS(SELECT 1 FROM system_role r WHERE r.id=984000007 AND r.tenant_id=1 AND r.deleted=0)
 AND NOT EXISTS(SELECT 1 FROM system_role_menu rm WHERE rm.role_id=984000007 AND rm.menu_id=m.id AND rm.tenant_id=1 AND rm.deleted=0);
-- Yudao filters a leaf permission from frontend auth info when its menu ancestors are absent.
-- Grant only the actual ancestors of these read leaves; never grant sibling action buttons.
INSERT INTO system_role_menu(role_id,menu_id,creator,updater,tenant_id)
WITH RECURSIVE read_ancestors AS (
 SELECT rm.role_id,m.id,m.parent_id FROM system_role_menu rm JOIN system_menu m ON m.id=rm.menu_id
 WHERE rm.tenant_id=1 AND rm.role_id IN (984000001,984000002,984000003,984000007,984000008,984000009,984000011)
  AND rm.deleted=0 AND m.deleted=0 AND m.permission IN
   ('hrm:payroll:scheme:query','hrm:salary:group:query','hrm:salary:option:query','hrm:salary:tax-rule:query')
 UNION DISTINCT
 SELECT c.role_id,m.id,m.parent_id FROM read_ancestors c JOIN system_menu m ON m.id=c.parent_id WHERE m.deleted=0
)
SELECT c.role_id,c.id,'binding-verification','binding-verification',1 FROM read_ancestors c
WHERE NOT EXISTS(SELECT 1 FROM system_role_menu rm WHERE rm.role_id=c.role_id AND rm.menu_id=c.id AND rm.tenant_id=1 AND rm.deleted=0);
