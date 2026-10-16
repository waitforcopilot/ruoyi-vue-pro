SET NAMES utf8mb4;
CREATE TABLE IF NOT EXISTS `hrm_payroll_formula` (
 `id` bigint NOT NULL AUTO_INCREMENT,
 `salary_group_id` bigint, `option_code` int NOT NULL, `expression` varchar(1000) NOT NULL,
 `effective_from` date NOT NULL, `effective_to` date NOT NULL,
 `evidence` varchar(1000) NOT NULL, `approved_by` bigint, `status` varchar(32) NOT NULL,
 `creator` varchar(64) DEFAULT '', `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `updater` varchar(64) DEFAULT '', `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `deleted` bit NOT NULL DEFAULT FALSE, `tenant_id` bigint NOT NULL DEFAULT 0, PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @hrm_formula_ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='hrm_salary_month_employee_record' AND column_name='salary_group_id')=0, 'ALTER TABLE hrm_salary_month_employee_record ADD COLUMN salary_group_id bigint NULL', 'SELECT 1');
PREPARE hrm_formula_stmt FROM @hrm_formula_ddl;
EXECUTE hrm_formula_stmt;
DEALLOCATE PREPARE hrm_formula_stmt;

SET @hrm_formula_ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='hrm_salary_month_employee_record' AND column_name='formula_date')=0, 'ALTER TABLE hrm_salary_month_employee_record ADD COLUMN formula_date date NULL', 'SELECT 1');
PREPARE hrm_formula_stmt FROM @hrm_formula_ddl;
EXECUTE hrm_formula_stmt;
DEALLOCATE PREPARE hrm_formula_stmt;

SET @hrm_payroll_menu = (SELECT id FROM system_menu WHERE path='/hrm-payroll' AND deleted=0 LIMIT 1);
INSERT INTO system_menu (name,permission,type,sort,parent_id,path,icon,component,component_name)
SELECT '薪资公式','hrm:payroll:formula:query',2,40,@hrm_payroll_menu,'formula','ep:edit','hrm/payroll/formula/index','HrmPayrollFormula'
WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE permission='hrm:payroll:formula:query' AND deleted=0);
SET @hrm_formula_menu = (SELECT id FROM system_menu WHERE permission='hrm:payroll:formula:query' AND deleted=0 LIMIT 1);
INSERT INTO system_menu(name,permission,type,parent_id) SELECT '新增公式版本','hrm:payroll:formula:update',3,@hrm_formula_menu WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE permission='hrm:payroll:formula:update' AND deleted=0);
INSERT INTO system_menu(name,permission,type,parent_id) SELECT '审批公式版本','hrm:payroll:formula:approve',3,@hrm_formula_menu WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE permission='hrm:payroll:formula:approve' AND deleted=0);
