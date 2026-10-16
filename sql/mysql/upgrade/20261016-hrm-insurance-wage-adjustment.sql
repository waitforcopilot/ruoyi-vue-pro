SET NAMES utf8mb4;
CREATE TABLE IF NOT EXISTS hrm_payroll_insurance_adjustment (
 id bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
 entity_id bigint NOT NULL,employee_id bigint NOT NULL,payment_id bigint NOT NULL,bill_id bigint NOT NULL,correction_id bigint,submitted_by bigint,reviewed_by bigint,confirmed_by bigint,
 personal_delta decimal(20,2) NOT NULL,corporate_delta decimal(20,2) NOT NULL,
 status varchar(32) NOT NULL,employee_acknowledgement varchar(1000),evidence varchar(1000),review_reason varchar(1000),receipt_reference varchar(100),receipt_evidence varchar(1000),request_key varchar(100) NOT NULL,source_digest varchar(64),source_snapshot LONGTEXT,request_snapshot LONGTEXT,
 value_date date,reviewed_time datetime,confirmed_time datetime,
 creator varchar(64) DEFAULT '',create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,updater varchar(64) DEFAULT '',update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,deleted bit NOT NULL DEFAULT FALSE,tenant_id bigint NOT NULL DEFAULT 0,
 UNIQUE KEY uk_ins_adjust_request(tenant_id,request_key),UNIQUE KEY uk_ins_adjust_receipt(tenant_id,receipt_reference),KEY idx_ins_adjust_source(tenant_id,payment_id,status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS hrm_payroll_insurance_adjustment_event (
 id bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,adjustment_id bigint,actor_id bigint,action varchar(32),reason varchar(1000),snapshot LONGTEXT,
 creator varchar(64) DEFAULT '',create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,updater varchar(64) DEFAULT '',update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,deleted bit NOT NULL DEFAULT FALSE,tenant_id bigint NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
SET @parent=(SELECT id FROM system_menu WHERE path='/hrm-payroll' AND deleted=0 LIMIT 1);
INSERT INTO system_menu(name,permission,type,parent_id,path,icon,component,component_name) SELECT '社保公积金工资差额','hrm:payroll:insurance-adjustment:query',2,@parent,'insurance-adjustment','ep:wallet','hrm/payroll/insurance-adjustment/index','HrmPayrollInsuranceAdjustment' WHERE @parent IS NOT NULL AND NOT EXISTS(SELECT 1 FROM system_menu WHERE permission='hrm:payroll:insurance-adjustment:query' AND deleted=0);
SET @adjustment=(SELECT id FROM system_menu WHERE permission='hrm:payroll:insurance-adjustment:query' AND deleted=0 LIMIT 1);
INSERT INTO system_menu(name,permission,type,parent_id,path,component,component_name) SELECT '提交缴费差额','hrm:payroll:insurance-adjustment:update',3,@adjustment,'','','' WHERE @adjustment IS NOT NULL AND NOT EXISTS(SELECT 1 FROM system_menu WHERE permission='hrm:payroll:insurance-adjustment:update' AND deleted=0);
INSERT INTO system_menu(name,permission,type,parent_id,path,component,component_name) SELECT '独立复核缴费差额','hrm:payroll:insurance-adjustment:review',3,@adjustment,'','','' WHERE @adjustment IS NOT NULL AND NOT EXISTS(SELECT 1 FROM system_menu WHERE permission='hrm:payroll:insurance-adjustment:review' AND deleted=0);
INSERT INTO system_menu(name,permission,type,parent_id,path,component,component_name) SELECT '核实收退及记账','hrm:payroll:insurance-adjustment:confirm',3,@adjustment,'','','' WHERE @adjustment IS NOT NULL AND NOT EXISTS(SELECT 1 FROM system_menu WHERE permission='hrm:payroll:insurance-adjustment:confirm' AND deleted=0);
