-- Durable salary-slip delivery. Configure the HTTPS employee portal URL and channel templates before enabling the job.
SET @hrm_ddl=IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='hrm_salary_slip_send_record' AND column_name='delivery_channels')=0,'ALTER TABLE hrm_salary_slip_send_record ADD COLUMN delivery_channels varchar(64) NULL','SELECT 1');
PREPARE hrm_stmt FROM @hrm_ddl; EXECUTE hrm_stmt; DEALLOCATE PREPARE hrm_stmt;
CREATE TABLE IF NOT EXISTS hrm_salary_slip_delivery (
 id bigint NOT NULL AUTO_INCREMENT,
 send_record_id bigint NOT NULL,
 slip_id bigint NOT NULL,
 user_id bigint NOT NULL,
 channel varchar(16) NOT NULL,
 status varchar(20) NOT NULL,
 attempts int NOT NULL DEFAULT 0,
 provider_log_id bigint NULL,
 failure_code varchar(120) NULL,
 last_attempt_at datetime NULL,
 next_check_at datetime NOT NULL,
 creator varchar(64) DEFAULT '',
 create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updater varchar(64) DEFAULT '',
 update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 deleted bit(1) NOT NULL DEFAULT b'0',
 tenant_id bigint NOT NULL DEFAULT 0,
 PRIMARY KEY(id), UNIQUE KEY uk_slip_delivery(tenant_id,slip_id,channel),
 KEY idx_slip_delivery_queue(tenant_id,status,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
INSERT INTO system_notify_template(name,code,nickname,content,type,params,status,remark)
SELECT '工资条发布通知','hrm_salary_slip_delivery','HRM','您的 {year} 年 {month} 月工资条已发布，请登录员工门户查看：{url}',2,'["year","month","url"]',0,'仅包含所属月份及登录入口，不含薪资明细'
WHERE NOT EXISTS(SELECT 1 FROM system_notify_template WHERE code='hrm_salary_slip_delivery' AND deleted=b'0');
INSERT INTO infra_job(name,status,handler_name,cron_expression,retry_count,retry_interval,monitor_timeout)
SELECT '工资条通知投递',2,'hrmSalarySlipDeliveryJob','0 * * * * ?',0,0,0
WHERE NOT EXISTS(SELECT 1 FROM infra_job WHERE handler_name='hrmSalarySlipDeliveryJob' AND deleted=b'0');

CREATE TABLE IF NOT EXISTS hrm_salary_slip_delivery_attempt (
 id bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
 delivery_id bigint NOT NULL, attempt_number int NOT NULL,
 status varchar(20) NOT NULL, provider_log_id bigint NULL, failure_code varchar(120) NULL,
 started_at datetime NOT NULL,
 creator varchar(64) DEFAULT '', create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updater varchar(64) DEFAULT '', update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 deleted bit(1) NOT NULL DEFAULT b'0', tenant_id bigint NOT NULL DEFAULT 0,
 UNIQUE KEY uk_slip_attempt(tenant_id,delivery_id,attempt_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
