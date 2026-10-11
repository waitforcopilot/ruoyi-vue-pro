-- Immutable salary grades and position coefficients.
CREATE TABLE IF NOT EXISTS `hrm_payroll_grade_policy` (
 `id` bigint NOT NULL AUTO_INCREMENT,
 `code` varchar(64) NOT NULL,
 `name` varchar(100) NOT NULL,
 `version` int NOT NULL,
 `option_code` int NOT NULL,
 `effective_from` date NOT NULL,
 `effective_to` date NOT NULL,
 `grades` LONGTEXT NOT NULL,
 `post_coefficients` LONGTEXT NOT NULL,
 `evidence` varchar(1000) NOT NULL,
 `status` varchar(32) NOT NULL,
 `approved_by` bigint,
 `creator` varchar(64) DEFAULT '', `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `updater` varchar(64) DEFAULT '', `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `deleted` bit NOT NULL DEFAULT FALSE, `tenant_id` bigint NOT NULL DEFAULT 0, PRIMARY KEY (`id`), UNIQUE (`tenant_id`,`code`,`version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
SET @hrm_payroll_menu = (SELECT id FROM system_menu WHERE path='/hrm-payroll' AND deleted=0 LIMIT 1);
INSERT INTO system_menu(name,permission,type,parent_id,path,component,component_name) SELECT '薪级薪档','hrm:payroll:grade:query',2,@hrm_payroll_menu,'grade','hrm/payroll/grade/index','HrmPayrollGrade' WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE permission='hrm:payroll:grade:query' AND deleted=0);
SET @hrm_grade_menu = (SELECT id FROM system_menu WHERE permission='hrm:payroll:grade:query' AND deleted=0 LIMIT 1);
INSERT INTO system_menu(name,permission,type,parent_id) SELECT '维护与应用薪级','hrm:payroll:grade:update',3,@hrm_grade_menu WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE permission='hrm:payroll:grade:update' AND deleted=0);
INSERT INTO system_menu(name,permission,type,parent_id) SELECT '审批薪级方案','hrm:payroll:grade:approve',3,@hrm_grade_menu WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE permission='hrm:payroll:grade:approve' AND deleted=0);
