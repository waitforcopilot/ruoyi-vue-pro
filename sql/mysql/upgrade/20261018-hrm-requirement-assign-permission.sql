SET NAMES utf8mb4;
-- Assignment is independent from content editing. Never auto-grant to roles.
-- Apply after 20261010-hrm-requirements.sql; safe to reapply.
SET @hrm_requirement_menu = (SELECT id FROM system_menu
 WHERE permission='hrm:payroll:requirement:query' AND deleted=0 LIMIT 1);
INSERT INTO system_menu (name, permission, type, parent_id)
SELECT '分派评审负责人', 'hrm:payroll:requirement:assign', 3, @hrm_requirement_menu
WHERE @hrm_requirement_menu IS NOT NULL AND NOT EXISTS (
 SELECT 1 FROM system_menu WHERE permission='hrm:payroll:requirement:assign' AND deleted=0
);
