-- Attendance source reconciliation and reviewed correction work orders.
CREATE TABLE IF NOT EXISTS `hrm_payroll_attendance_issue` (
 `id` bigint NOT NULL AUTO_INCREMENT,
 `employee_id` bigint,
 `attendance_date` date,
 `type` varchar(1000),
 `clock_type` int,
 `owner_id` bigint,
 `reviewer_id` bigint,
 `request_key` varchar(128),
 `status` varchar(1000),
 `initial_snapshot` LONGTEXT,
 `evidence` varchar(1000),
 `correction_time` datetime,
 `submitted_by` bigint,
 `correction_clock_id` bigint,
 `review_opinion` varchar(1000),
 `due_time` datetime,
 `closed_time` datetime,
 `creator` varchar(64) DEFAULT '',
 `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `updater` varchar(64) DEFAULT '',
 `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `deleted` bit NOT NULL DEFAULT FALSE,
 `tenant_id` bigint NOT NULL DEFAULT 0,
 PRIMARY KEY (`id`),
 UNIQUE (`tenant_id`,`request_key`),
 KEY `idx_attendance_issue_scope` (`tenant_id`,`employee_id`,`attendance_date`,`status`(32))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `hrm_payroll_attendance_issue_event` (
 `id` bigint NOT NULL AUTO_INCREMENT,
 `issue_id` bigint,
 `action` varchar(1000),
 `from_status` varchar(1000),
 `to_status` varchar(1000),
 `actor_id` bigint,
 `reason` varchar(1000),
 `snapshot` LONGTEXT,
 `creator` varchar(64) DEFAULT '',
 `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `updater` varchar(64) DEFAULT '',
 `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `deleted` bit NOT NULL DEFAULT FALSE,
 `tenant_id` bigint NOT NULL DEFAULT 0,
 PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
SET @hrm_payroll_menu=(SELECT id FROM system_menu WHERE path='/hrm-payroll' AND deleted=0 LIMIT 1);
INSERT INTO system_menu(name,permission,type,parent_id,path,component,component_name) SELECT '考勤归集与异常','hrm:payroll:attendance:query',2,@hrm_payroll_menu,'attendance','hrm/payroll/attendance/index','HrmPayrollAttendance' WHERE NOT EXISTS(SELECT 1 FROM system_menu WHERE permission='hrm:payroll:attendance:query' AND deleted=0);
SET @hrm_att_menu=(SELECT id FROM system_menu WHERE permission='hrm:payroll:attendance:query' AND deleted=0 LIMIT 1);
INSERT INTO system_menu(name,permission,type,parent_id) SELECT '创建及处理考勤工单','hrm:payroll:attendance:update',3,@hrm_att_menu WHERE NOT EXISTS(SELECT 1 FROM system_menu WHERE permission='hrm:payroll:attendance:update' AND deleted=0);
INSERT INTO system_menu(name,permission,type,parent_id) SELECT '复核及关闭考勤工单','hrm:payroll:attendance:review',3,@hrm_att_menu WHERE NOT EXISTS(SELECT 1 FROM system_menu WHERE permission='hrm:payroll:attendance:review' AND deleted=0);
SET @hrm_portal_menu=COALESCE((SELECT parent_id FROM system_menu WHERE component='hrm/portal/home/index' AND deleted=0 LIMIT 1),0);
INSERT INTO system_menu(name,permission,type,parent_id,path,component,component_name) SELECT '我的考勤工单','hrm:portal:query',2,@hrm_portal_menu,'attendance-issue','hrm/portal/attendance/issue/index','HrmPortalAttendanceIssue' WHERE NOT EXISTS(SELECT 1 FROM system_menu WHERE component='hrm/portal/attendance/issue/index' AND deleted=0);
INSERT INTO system_dict_data(sort,label,value,dict_type,status) SELECT 3,'异常工单批准补卡','3','hrm_attendance_clock_source',0 WHERE NOT EXISTS(SELECT 1 FROM system_dict_data WHERE dict_type='hrm_attendance_clock_source' AND value='3' AND deleted=0);
INSERT INTO system_dict_data(sort,label,value,dict_type,status) SELECT 100,'出差','BUSINESS_TRIP','hrm_attendance_leave_type',0 WHERE NOT EXISTS(SELECT 1 FROM system_dict_data WHERE dict_type='hrm_attendance_leave_type' AND value='BUSINESS_TRIP' AND deleted=0);
INSERT INTO system_dict_data(sort,label,value,dict_type,status) SELECT 101,'外出','OUTING','hrm_attendance_leave_type',0 WHERE NOT EXISTS(SELECT 1 FROM system_dict_data WHERE dict_type='hrm_attendance_leave_type' AND value='OUTING' AND deleted=0);
