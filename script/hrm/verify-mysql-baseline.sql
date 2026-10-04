-- Test fixture: run only in the disposable container created by verify-mysql-baseline.sh.
DELIMITER $$
CREATE PROCEDURE assert_baseline(IN condition_ok BOOLEAN, IN error_message VARCHAR(255))
BEGIN
    IF condition_ok IS NULL OR NOT condition_ok THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = error_message;
    END IF;
END$$
DELIMITER ;

CALL assert_baseline((SELECT COUNT(*) FROM information_schema.tables
    WHERE table_schema = DATABASE() AND LEFT(table_name, 4) IN ('hrm_', 'bpm_')) = 58,
    'Expected 50 HRM and 8 BPM application tables');
CALL assert_baseline((SELECT COUNT(*) FROM information_schema.tables
    WHERE table_schema = DATABASE() AND engine = 'InnoDB') = 58,
    'All baseline tables must support transactions');
CALL assert_baseline((SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND column_name = 'tenant_id') = 57,
    'Every tenant business table requires a tenant column');
CALL assert_baseline((SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'hrm_salary_option_template'
        AND column_name = 'tenant_id') = 0,
    'Global salary templates must retain their existing TenantIgnore schema');
CALL assert_baseline((SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND column_name = 'id' AND extra = 'auto_increment') = 58,
    'Application primary keys must support generated IDs');
CALL assert_baseline((SELECT COUNT(*) FROM bpm_category
    WHERE name = 'baseline-repeatability-check' AND tenant_id = 1) = 1,
    'Reapplying the migration must preserve existing data');

START TRANSACTION;
INSERT INTO hrm_employee (name, job_number, birthday, tenant_id)
    VALUES ('迁移验证🧾', 'BASELINE-001', '1960-01-02 00:00:00', 1);
SET @employee_id = LAST_INSERT_ID();
INSERT INTO hrm_employee_contract (employee_id, end_time, tenant_id)
    VALUES (@employee_id, '2050-12-31 00:00:00', 1);
CALL assert_baseline((SELECT YEAR(birthday) FROM hrm_employee WHERE id = @employee_id) = 1960,
    'Employee history before 1970 must remain representable');
CALL assert_baseline((SELECT YEAR(end_time) FROM hrm_employee_contract
    WHERE employee_id = @employee_id) = 2050,
    'Contract dates after 2038 must remain representable');
CALL assert_baseline((SELECT name FROM hrm_employee WHERE id = @employee_id) = '迁移验证🧾',
    'UTF8MB4 names must round-trip');

INSERT INTO hrm_salary_month_record (year, month, tenant_id)
    VALUES (2026, 10, 1);
SET @month_record_id = LAST_INSERT_ID();
INSERT INTO hrm_salary_month_employee_record
    (month_record_id, employee_id, year, month, expected_pay_salary, personal_tax,
     real_pay_salary, option_values, tenant_id)
    VALUES (@month_record_id, @employee_id, 2026, 10, 10001.23, 1.23, 10000.00,
        JSON_ARRAY(JSON_OBJECT('code', 100, 'name', '薪资项', 'value', 10001.23,
            'description', REPEAT('🧾', 18000))), 1);
CALL assert_baseline((SELECT expected_pay_salary - personal_tax = real_pay_salary
    FROM hrm_salary_month_employee_record WHERE month_record_id = @month_record_id) = TRUE,
    'Decimal storage must preserve payroll reconciliation');
CALL assert_baseline((SELECT JSON_VALID(option_values) AND OCTET_LENGTH(option_values) > 65535
    FROM hrm_salary_month_employee_record WHERE month_record_id = @month_record_id) = TRUE,
    'Serialized salary snapshots must not be truncated to H2 fixture VARCHAR limits');

INSERT INTO bpm_form (name, status, conf, fields, tenant_id)
    VALUES ('审批表单', 0, JSON_OBJECT('description', REPEAT('🧾', 18000)),
        JSON_ARRAY('金额', '依据'), 1);
SET @form_id = LAST_INSERT_ID();
CALL assert_baseline((SELECT JSON_VALID(conf) AND OCTET_LENGTH(conf) > 65535
    FROM bpm_form WHERE id = @form_id) = TRUE,
    'BPM serialized configuration must remain readable');
UPDATE hrm_employee SET deleted = b'1' WHERE id = @employee_id;
CALL assert_baseline((SELECT COUNT(*) FROM hrm_employee
    WHERE id = @employee_id AND deleted = b'0') = 0,
    'Logical-delete flags must retain their boolean meaning');
ROLLBACK;
CALL assert_baseline((SELECT COUNT(*) FROM hrm_employee) = 0,
    'Verification transaction must roll back its fixtures');
DROP PROCEDURE assert_baseline;
