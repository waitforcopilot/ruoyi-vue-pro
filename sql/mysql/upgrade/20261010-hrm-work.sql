-- Approved work rules, verified overtime, expiring time-off lots and cycle settlements.
CREATE TABLE IF NOT EXISTS `hrm_payroll_work_rule` (
 `id` bigint NOT NULL AUTO_INCREMENT,
 `employee_id` bigint,
 `system_type` varchar(1000),
 `effective_from` date,
 `effective_to` date,
 `legal_basis` varchar(1000),
 `status` varchar(1000),
 `approved_by` bigint,
 `cycle_minutes` int,
 `cycle_months` int,
 `time_off_expiry_days` int,
 `limit_minutes` int,
 `hourly_base` decimal(18,4),
 `workday_rate` decimal(18,4),
 `restday_rate` decimal(18,4),
 `holiday_rate` decimal(18,4),
 `creator` varchar(64) DEFAULT '',
 `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `updater` varchar(64) DEFAULT '',
 `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `deleted` bit NOT NULL DEFAULT FALSE,
 `tenant_id` bigint NOT NULL DEFAULT 0,
 PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `hrm_payroll_time_entry` (
 `id` bigint NOT NULL AUTO_INCREMENT,
 `employee_id` bigint,
 `rule_id` bigint,
 `source_apply_id` bigint,
 `clock_in_id` bigint,
 `clock_out_id` bigint,
 `source_snapshot` LONGTEXT,
 `source_system` varchar(64),
 `source_record_id` varchar(128),
 `evidence` varchar(1000),
 `start_time` datetime,
 `end_time` datetime,
 `clock_start` datetime,
 `clock_end` datetime,
 `break_minutes` int,
 `recognized_minutes` int,
 `day_type` varchar(1000),
 `compensation` varchar(1000),
 `amount` decimal(18,4),
 `confirmed_by` bigint,
 `creator` varchar(64) DEFAULT '',
 `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `updater` varchar(64) DEFAULT '',
 `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `deleted` bit NOT NULL DEFAULT FALSE,
 `tenant_id` bigint NOT NULL DEFAULT 0,
 PRIMARY KEY (`id`),
 UNIQUE (`tenant_id`,`source_system`,`source_record_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `hrm_payroll_time_off` (
 `id` bigint NOT NULL AUTO_INCREMENT,
 `employee_id` bigint,
 `time_entry_id` bigint,
 `minutes` int,
 `request_key` varchar(128),
 `reason` varchar(1000),
 `actor_id` bigint,
 `status` varchar(1000),
 `expires_on` date,
 `allocations` LONGTEXT,
 `creator` varchar(64) DEFAULT '',
 `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `updater` varchar(64) DEFAULT '',
 `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `deleted` bit NOT NULL DEFAULT FALSE,
 `tenant_id` bigint NOT NULL DEFAULT 0,
 PRIMARY KEY (`id`),
 UNIQUE (`tenant_id`,`request_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `hrm_payroll_work_cycle` (
 `id` bigint NOT NULL AUTO_INCREMENT,
 `employee_id` bigint,
 `rule_id` bigint,
 `cycle_from` date,
 `cycle_to` date,
 `planned_minutes` int,
 `actual_minutes` int,
 `recognized_minutes` int,
 `payable_minutes` int,
 `amount` decimal(18,4),
 `source_snapshot` LONGTEXT,
 `settled_by` bigint,
 `creator` varchar(64) DEFAULT '',
 `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `updater` varchar(64) DEFAULT '',
 `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `deleted` bit NOT NULL DEFAULT FALSE,
 `tenant_id` bigint NOT NULL DEFAULT 0,
 PRIMARY KEY (`id`),
 UNIQUE (`tenant_id`,`rule_id`,`cycle_from`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
SET @hrm_payroll_menu=(SELECT id FROM system_menu WHERE path='/hrm-payroll' AND deleted=0 LIMIT 1);
INSERT INTO system_menu(name,permission,type,parent_id,path,component,component_name) SELECT '工时与加班','hrm:payroll:work:query',2,@hrm_payroll_menu,'work','hrm/payroll/work/index','HrmPayrollWork' WHERE NOT EXISTS(SELECT 1 FROM system_menu WHERE permission='hrm:payroll:work:query' AND deleted=0);
SET @hrm_work_menu=(SELECT id FROM system_menu WHERE permission='hrm:payroll:work:query' AND deleted=0 LIMIT 1);
INSERT INTO system_menu(name,permission,type,parent_id) SELECT '维护工时制度','hrm:payroll:work:update',3,@hrm_work_menu WHERE NOT EXISTS(SELECT 1 FROM system_menu WHERE permission='hrm:payroll:work:update' AND deleted=0);
INSERT INTO system_menu(name,permission,type,parent_id) SELECT '审批工时制度','hrm:payroll:work:approve',3,@hrm_work_menu WHERE NOT EXISTS(SELECT 1 FROM system_menu WHERE permission='hrm:payroll:work:approve' AND deleted=0);
INSERT INTO system_menu(name,permission,type,parent_id) SELECT '确认与结算工时','hrm:payroll:work:confirm',3,@hrm_work_menu WHERE NOT EXISTS(SELECT 1 FROM system_menu WHERE permission='hrm:payroll:work:confirm' AND deleted=0);
INSERT INTO system_dict_data(sort,label,value,dict_type,status) SELECT 3,'法定节假日','3','hrm_attendance_holiday_type',0 WHERE NOT EXISTS(SELECT 1 FROM system_dict_data WHERE dict_type='hrm_attendance_holiday_type' AND value='3' AND deleted=0);
