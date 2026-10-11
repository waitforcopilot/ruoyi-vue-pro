-- Immutable salary proposals and separate HR/finance approval.
CREATE TABLE IF NOT EXISTS `hrm_payroll_salary_proposal` (
 `id` bigint NOT NULL AUTO_INCREMENT,
 `employee_id` bigint NOT NULL,
 `request_snapshot` LONGTEXT NOT NULL,
 `before_snapshot` LONGTEXT,
 `status` varchar(32) NOT NULL,
 `proposed_by` bigint NOT NULL,
 `reviewed_by` bigint,
 `approved_by` bigint,
 `review_reason` varchar(1000),
 `approval_reason` varchar(1000),
 `change_record_id` bigint,
 `creator` varchar(64) DEFAULT '', `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `updater` varchar(64) DEFAULT '', `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `deleted` bit NOT NULL DEFAULT FALSE, `tenant_id` bigint NOT NULL DEFAULT 0, PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
SET @hrm_payroll_menu = (SELECT id FROM system_menu WHERE path='/hrm-payroll' AND deleted=0 LIMIT 1);
INSERT INTO system_menu(name,permission,type,parent_id,path,component,component_name) SELECT '定调薪审批','hrm:payroll:salary-proposal:query',2,@hrm_payroll_menu,'salary-proposal','hrm/payroll/salary-proposal/index','HrmPayrollSalaryProposal' WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE permission='hrm:payroll:salary-proposal:query' AND deleted=0);
SET @hrm_salary_proposal_menu = (SELECT id FROM system_menu WHERE permission='hrm:payroll:salary-proposal:query' AND deleted=0 LIMIT 1);
INSERT INTO system_menu(name,permission,type,parent_id,path,component,component_name) SELECT 'HR复核','hrm:payroll:salary-proposal:review',3,@hrm_salary_proposal_menu,'','','' WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE permission='hrm:payroll:salary-proposal:review' AND deleted=0);
INSERT INTO system_menu(name,permission,type,parent_id,path,component,component_name) SELECT '财务审批','hrm:payroll:salary-proposal:approve',3,@hrm_salary_proposal_menu,'','','' WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE permission='hrm:payroll:salary-proposal:approve' AND deleted=0);
INSERT INTO system_menu(name,permission,type,parent_id,path,component,component_name) SELECT '退回申请','hrm:payroll:salary-proposal:reject',3,@hrm_salary_proposal_menu,'','','' WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE permission='hrm:payroll:salary-proposal:reject' AND deleted=0);
