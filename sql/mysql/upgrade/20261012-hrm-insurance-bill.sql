-- Monthly insurance statements, immutable differences and independently approved receipt settlements.
CREATE TABLE IF NOT EXISTS `hrm_payroll_insurance_bill` (
 `id` bigint NOT NULL AUTO_INCREMENT,
 `entity_id` bigint,
 `version` int,
 `replaces_id` bigint,
 `reviewed_by` bigint,
 `approved_by` bigint,
 `year` int,
 `month` int,
 `file_name` varchar(255),
 `digest` varchar(64),
 `reference` varchar(128),
 `evidence` varchar(1000),
 `sources` LONGTEXT,
 `rows_snapshot` LONGTEXT,
 `errors` LONGTEXT,
 `comparison` LONGTEXT,
 `status` varchar(32),
 `creator` varchar(64) DEFAULT '', `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `updater` varchar(64) DEFAULT '', `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `deleted` bit NOT NULL DEFAULT FALSE, `tenant_id` bigint NOT NULL DEFAULT 0,
 PRIMARY KEY (`id`),
 `approved_month` varchar(7) GENERATED ALWAYS AS (CASE WHEN `deleted`=b'0' AND `status` IN ('APPROVED','RECONCILED') THEN CONCAT(`year`,'-',LPAD(`month`,2,'0')) ELSE NULL END) STORED,
 UNIQUE KEY `uk_ins_bill_approved_period` (`tenant_id`,`entity_id`,`approved_month`),
 UNIQUE KEY `uk_ins_bill_content` (`tenant_id`,`entity_id`,`year`,`month`,`digest`,`version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS `hrm_payroll_insurance_bill_line` (
 `id` bigint NOT NULL AUTO_INCREMENT,
 `bill_id` bigint,
 `entity_id` bigint,
 `employee_id` bigint,
 `revision` bigint,
 `job_number` varchar(128),
 `employee_name` varchar(128),
 `project_code` varchar(32),
 `status` varchar(32),
 `project_name` varchar(64),
 `expected_personal` decimal(18,2),
 `expected_corporate` decimal(18,2),
 `original_personal` decimal(18,2),
 `original_corporate` decimal(18,2),
 `actual_personal` decimal(18,2),
 `actual_corporate` decimal(18,2),
 `creator` varchar(64) DEFAULT '', `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `updater` varchar(64) DEFAULT '', `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `deleted` bit NOT NULL DEFAULT FALSE, `tenant_id` bigint NOT NULL DEFAULT 0,
 PRIMARY KEY (`id`),
 UNIQUE KEY `uk_ins_bill_employee_project` (`tenant_id`,`bill_id`,`employee_id`,`project_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS `hrm_payroll_insurance_settlement` (
 `id` bigint NOT NULL AUTO_INCREMENT,
 `bill_id` bigint,
 `line_id` bigint,
 `entity_id` bigint,
 `line_revision` bigint,
 `proof_name` varchar(255),
 `proof_content` LONGTEXT,
 `reviewed_by` bigint,
 `approved_by` bigint,
 `mode` varchar(32),
 `status` varchar(32),
 `reference` varchar(128),
 `digest` varchar(64),
 `evidence` varchar(1000),
 `personal` decimal(18,2),
 `corporate` decimal(18,2),
 `creator` varchar(64) DEFAULT '', `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `updater` varchar(64) DEFAULT '', `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `deleted` bit NOT NULL DEFAULT FALSE, `tenant_id` bigint NOT NULL DEFAULT 0,
 PRIMARY KEY (`id`),
 UNIQUE KEY `uk_ins_settlement_receipt` (`tenant_id`,`line_id`,`digest`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
SET @hrm_payroll_menu=(SELECT id FROM system_menu WHERE path='/hrm-payroll' AND deleted=0 LIMIT 1);
INSERT INTO system_menu(name,permission,type,parent_id,path,component,component_name) SELECT '缴费实缴对账','hrm:payroll:insurance-bill:query',2,@hrm_payroll_menu,'insurance-bill','hrm/payroll/insurance-bill/index','HrmPayrollInsuranceBill' WHERE NOT EXISTS(SELECT 1 FROM system_menu WHERE permission='hrm:payroll:insurance-bill:query' AND deleted=0);
SET @hrm_bill_menu=(SELECT id FROM system_menu WHERE permission='hrm:payroll:insurance-bill:query' AND deleted=0 LIMIT 1);
INSERT INTO system_menu(name,permission,type,parent_id) SELECT '维护缴费账单及凭证','hrm:payroll:insurance-bill:update',3,@hrm_bill_menu WHERE NOT EXISTS(SELECT 1 FROM system_menu WHERE permission='hrm:payroll:insurance-bill:update' AND deleted=0);
INSERT INTO system_menu(name,permission,type,parent_id) SELECT '复核缴费账单及凭证','hrm:payroll:insurance-bill:review',3,@hrm_bill_menu WHERE NOT EXISTS(SELECT 1 FROM system_menu WHERE permission='hrm:payroll:insurance-bill:review' AND deleted=0);
INSERT INTO system_menu(name,permission,type,parent_id) SELECT '审批缴费账单及凭证','hrm:payroll:insurance-bill:approve',3,@hrm_bill_menu WHERE NOT EXISTS(SELECT 1 FROM system_menu WHERE permission='hrm:payroll:insurance-bill:approve' AND deleted=0);
INSERT INTO system_menu(name,permission,type,parent_id) SELECT '导出缴费对账','hrm:payroll:insurance-bill:export',3,@hrm_bill_menu WHERE NOT EXISTS(SELECT 1 FROM system_menu WHERE permission='hrm:payroll:insurance-bill:export' AND deleted=0);
