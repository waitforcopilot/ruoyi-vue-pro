-- Synthetic employees for an isolated verification database only. Never run against production.
-- Reserved IDs are not overwritten. Inspect any preexisting row before using this fixture.
INSERT INTO hrm_employee(id,name,job_number,tenant_id,creator)
SELECT 920000001,'接入回归员工（演示）','QAVR-INTAKE-KNOWN',1,'intake-verification' WHERE NOT EXISTS(SELECT 1 FROM hrm_employee WHERE id=920000001);
INSERT INTO hrm_employee(id,name,job_number,tenant_id,creator)
SELECT 920000002,'重复工号回归一（演示）','QAVR-INTAKE-DUP',1,'intake-verification' WHERE NOT EXISTS(SELECT 1 FROM hrm_employee WHERE id=920000002);
INSERT INTO hrm_employee(id,name,job_number,tenant_id,creator)
SELECT 920000003,'重复工号回归二（演示）','QAVR-INTAKE-DUP',1,'intake-verification' WHERE NOT EXISTS(SELECT 1 FROM hrm_employee WHERE id=920000003);
INSERT INTO hrm_employee(id,name,job_number,tenant_id,creator)
SELECT 920000004,'另一租户回归员工（演示）','QAVR-INTAKE-OTHER',999,'intake-verification' WHERE NOT EXISTS(SELECT 1 FROM hrm_employee WHERE id=920000004);
