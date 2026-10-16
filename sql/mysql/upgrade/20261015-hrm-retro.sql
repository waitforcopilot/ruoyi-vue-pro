SET NAMES utf8mb4;
CREATE TABLE IF NOT EXISTS hrm_payroll_retro (
 id bigint NOT NULL AUTO_INCREMENT, entity_id bigint, batch_id bigint, employee_id bigint, payment_id bigint, prior_retro_id bigint,
 version int, original_income decimal(20,2), corrected_income decimal(20,2), delta decimal(20,2),
 evidence varchar(1000), request_key varchar(100), request_snapshot LONGTEXT, status varchar(32), review_reason varchar(1000), original_digest varchar(64),
 submitted_by bigint, reviewed_by bigint, reviewed_time datetime,
 creator varchar(64) DEFAULT '', create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updater varchar(64) DEFAULT '', update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 deleted bit NOT NULL DEFAULT FALSE, tenant_id bigint NOT NULL DEFAULT 0,
 PRIMARY KEY(id), UNIQUE KEY uk_retro_request(tenant_id,request_key), UNIQUE KEY uk_retro_version(tenant_id,payment_id,version),
 KEY idx_retro_batch(tenant_id,batch_id,status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS hrm_payroll_retro_event (
 id bigint NOT NULL AUTO_INCREMENT, retro_id bigint, actor_id bigint, action varchar(32), reason varchar(1000), snapshot LONGTEXT,
 creator varchar(64) DEFAULT '', create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updater varchar(64) DEFAULT '', update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 deleted bit NOT NULL DEFAULT FALSE, tenant_id bigint NOT NULL DEFAULT 0, PRIMARY KEY(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
INSERT INTO hrm_salary_option_template(code,parent_code,name,remark,system_flag,type,tax_enabled,visible,calculate_enabled)
SELECT 10901,10,'批准工资补发/收回','关联原到账工资及独立复核差额，不允许人工或公式覆盖',1,1,1,1,1
WHERE NOT EXISTS (SELECT 1 FROM hrm_salary_option_template WHERE code=10901);
SET @parent=(SELECT id FROM system_menu WHERE permission='hrm:payroll:batch:query' AND deleted=0 LIMIT 1);
INSERT INTO system_menu(name,permission,type,parent_id,path,icon,component,component_name) SELECT '工资差额查询','hrm:payroll:retro:query',3,@parent,'','','','' WHERE @parent IS NOT NULL AND NOT EXISTS(SELECT 1 FROM system_menu WHERE permission='hrm:payroll:retro:query' AND deleted=0);
INSERT INTO system_menu(name,permission,type,parent_id,path,icon,component,component_name) SELECT '工资差额登记','hrm:payroll:retro:update',3,@parent,'','','','' WHERE @parent IS NOT NULL AND NOT EXISTS(SELECT 1 FROM system_menu WHERE permission='hrm:payroll:retro:update' AND deleted=0);
INSERT INTO system_menu(name,permission,type,parent_id,path,icon,component,component_name) SELECT '工资差额复核','hrm:payroll:retro:approve',3,@parent,'','','','' WHERE @parent IS NOT NULL AND NOT EXISTS(SELECT 1 FROM system_menu WHERE permission='hrm:payroll:retro:approve' AND deleted=0);
