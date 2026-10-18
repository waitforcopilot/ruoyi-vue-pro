SET NAMES utf8mb4;
-- Separate version confirmation from editing. No existing role gains permission.
-- Apply after 20261010-hrm-requirements.sql; safe to reapply.
SET @hrm_requirement_menu = (SELECT id FROM system_menu
 WHERE permission='hrm:payroll:requirement:query' AND deleted=0 LIMIT 1);
INSERT INTO system_menu (name, permission, type, parent_id)
SELECT '确认需求版本', 'hrm:payroll:requirement:confirm', 3, @hrm_requirement_menu
WHERE @hrm_requirement_menu IS NOT NULL AND NOT EXISTS (
 SELECT 1 FROM system_menu WHERE permission='hrm:payroll:requirement:confirm' AND deleted=0
);
